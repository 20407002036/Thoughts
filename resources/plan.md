## Plan: Backend-First Recording Flow (Revised)

## Status: ✅ IMPLEMENTATION COMPLETE

All 10 phases have been implemented. The app now follows a clean backend-first architecture:
- Audio recording works with AudioRecorder (M4A format)
- Audio files saved to app cache directory via AudioFileManager
- Upload to backend via Retrofit with multipart form data
- Backend processes audio and returns transcript, mood analysis, and tags
- ReviewScreen displays results or upload status/errors
- All SpeechRecognizer code removed from ViewModel
- No compilation errors detected

### What Changed
- **RecordScreen**: Now shows only timer + recording controls; no transcript preview
- **JournalViewModel**: Removed speech recognition; added upload orchestration + retry logic
- **Build.gradle.kts**: Added Retrofit, OkHttp, Kotlinx Serialization dependencies
- **AndroidManifest.xml**: Added INTERNET permission (RECORD_AUDIO already present)
- **ReviewScreen**: Added upload state handling with progress indicator and retry UI
- **JournalModels.kt**: Simplified RecordingSession; added @Serializable annotations; added IngestionResponse

---
Mobile app records audio only and uploads it to the backend for transcription and analysis. No on-device speech recognition.

**Key Change**: Remove live transcription from the phone; the backend handles all transcription, mood analysis, and tag generation.

**New Architecture**
- **RecordScreen**: Record audio → save to file → show timer
- **ReviewScreen**: Upload file to backend → wait for response → display results (transcript + analysis)
- **Backend owns**: transcription, mood analysis, tag generation
- **Mobile owns**: audio capture, local file management, upload orchestration

---

## Implementation Plan

### Phase 1: Data Model Updates
**Goal**: Simplify models to remove speech recognition coupling.

1. **Update JournalModels.kt**:
   - Remove `speechLocaleTag` and `recognitionError` from `RecordingSession` (no longer needed)
   - Keep `RecordingSession` minimal: `id`, `startedAtMillis`, `endedAtMillis`, `durationMs`, `status`
   - Keep `AudioAsset` for local file path and upload state
   - Add `uploadState` to track: `Local`, `Uploading`, `Processing`, `Uploaded`, `Failed`
   - Remove `Transcript.partialText` (no live updates)
   - Add `backendResponseError: String?` to handle upload/processing errors

2. **Review what to keep**:
   - `RecordingSession`, `AudioAsset`, `Transcript` — keep these
   - `TranscriptSegment`, `MoodAnalysis`, `JournalTag`, `JournalEntry` — keep; backend populates them
   - Remove: speech-specific error codes, language fallback logic

### Phase 2: Audio File Management
**Goal**: Capture audio and save it to a local file.

1. **Create AudioFileManager (new file)**:
   - Function: `fun createAudioFile(context: Context): File` — creates cache file with timestamp
   - Function: `fun getAudioDirectory(context: Context): File` — cache or app files directory
   - Function: `fun deleteAudioFile(file: File)` — cleanup after upload or discard
   - Store in `context.cacheDir` or `context.filesDir` (app-private)

2. **Update AudioRecorder.kt**:
   - Already has `start(outputFile)` and `stop()` — keep these
   - Ensure file is properly released after stop

### Phase 3: Backend Communication (HTTP Client)
**Goal**: Upload audio file with metadata to backend.

1. **Create BackendService.kt**:
   - Use Retrofit or OkHttp to POST audio as `multipart/form-data`
   - Endpoint: `POST /v1/journals/ingest` (as defined in ThoughtsApi)
   - Multipart fields:
     - `audio` (file)
     - `duration_ms` (long)
     - `locale` (optional, e.g., "en-US")
   - Response model: `IngestionResponse` or `BackendAnalysisResult` with `transcript`, `moodAnalysis`, `tags`

2. **Update ThoughtsApi.kt**:
   - Keep BASE_URL
   - Add actual response models (not just endpoint string)

3. **Add Retrofit dependency to build.gradle.kts**:
   - `com.squareup.retrofit2:retrofit`
   - `com.squareup.retrofit2:converter-kotlinx-serialization`
   - `com.squareup.okhttp3:okhttp`

### Phase 4: ViewModel Refactoring
**Goal**: Remove SpeechRecognizer, add upload orchestration.

1. **Update JournalViewModel.kt**:
   - **Remove**: `VoiceToTextParser`, `transcriptBuffer`, `transientSpeechErrorCount`, `hasTriedLanguageFallback`, all speech error handling
   - **Keep**: `RecordingSession` state, timer logic, draft state
   - **Add**:
     - `audioFile: File?` — local recording file
     - `uploadState: StateFlow<AudioUploadState>` — track upload progress
     - `backendResult: StateFlow<BackendAnalysisResult?>` — transcript + analysis from backend
     - `uploadError: StateFlow<String?>` — error message

2. **Implement new flow**:
   ```
   fun beginRecording()
     ├─ Create audio file via AudioFileManager
     ├─ Start AudioRecorder with file
     └─ Start timer
   
   fun finishRecording()
     ├─ Stop timer
     ├─ Stop AudioRecorder
     ├─ Transition to "uploading" state
     └─ Call uploadAudioToBackend()
   
   fun uploadAudioToBackend()
     ├─ Multipart POST audio + metadata
     ├─ Retry on network error (capped backoff)
     ├─ On success → parse response → update backendResult → emit Uploaded state
     └─ On error → emit Failed state + error message
   
   fun discardRecording()
     ├─ Stop timer
     ├─ Stop AudioRecorder
     ├─ Delete audio file
     └─ Reset state
   ```

3. **Remove init block** (SpeechRecognizer subscription)

### Phase 5: RecordScreen UI Updates
**Goal**: Show recording timer and status, not live transcript.

1. **Remove**:
   - Transcript preview (no live text)
   - SpeechRecognizer status text ("LIVE TRANSCRIPT ACTIVE" → "RECORDING")
   - Permission logic for RECORD_AUDIO is still needed, but simpler flow

2. **Keep**:
   - Timer display
   - Pulsing visualizer orb
   - Recording controls (stop, pause, resume, discard)

3. **Update status text**:
   - `Recording` → "Recording..."
   - `Paused` → "Paused"
   - `Finished` → Don't show; navigate to ReviewScreen

4. **Add upload indicator** (optional, between Record and Review):
   - Show "Uploading..." or "Processing..." with spinner
   - Block user from navigating until done

### Phase 6: ReviewScreen Integration
**Goal**: Receive backend results and display them.

1. **Update ReviewScreen.kt**:
   - Listen to `journalViewModel.backendResult`
   - If `uploadState` is `Processing` or `Uploading`, show spinner
   - If `backendResult` is loaded, display:
     - Full transcript (from backend)
     - Mood analysis (label + score)
     - Tags (generated list)
     - Takeaway placeholder (or from backend if available)
   - If error, show error message + "Retry" button

2. **Handle submission**:
   - "Save Entry" button submits to actual backend persistence (separate from ingest)
   - Or auto-save after successful ingest

### Phase 7: Manifest Permissions
**Goal**: Add necessary permissions.

1. **AndroidManifest.xml**:
   - Add `<uses-permission android:name="android.permission.RECORD_AUDIO" />`
   - Add `<uses-permission android:name="android.permission.INTERNET" />`
   - Runtime permission request for `RECORD_AUDIO` (already in RecordScreen)

### Phase 8: Error Handling & Retries
**Goal**: Graceful handling of network failures.

1. **Network error retry logic**:
   - On upload failure, show error + "Retry" button
   - Capped exponential backoff: 1s, 2s, 4s (max 3 retries)
   - Keep audio file until successful upload or user discards

2. **Timeout handling**:
   - Set timeout to 30s per upload attempt
   - Show "Upload taking longer than expected" if > 15s

3. **Local cleanup**:
   - Delete audio file after successful upload (optional, if backend doesn't need it)
   - Or keep for local playback/offline review

---

## Implementation Order

1. **Update JournalModels.kt** - remove speech fields
2. **Create AudioFileManager.kt** - file lifecycle
3. **Create BackendService.kt** - HTTP client
4. **Add deps to build.gradle.kts** - Retrofit, OkHttp
5. **Update AndroidManifest.xml** - permissions
6. **Refactor JournalViewModel.kt** - remove SpeechRecognizer, add upload logic
7. **Update RecordScreen.kt** - simplify UI, remove transcript preview
8. **Update ReviewScreen.kt** - wait for backend, display results
9. **Test end-to-end** - record, upload, view results

---

## Key Files to Modify

| File | Change |
|------|--------|
| `JournalModels.kt` | Remove speech fields, add upload state variants |
| `JournalViewModel.kt` | Remove VoiceToTextParser, add BackendService integration, upload orchestration |
| `RecordScreen.kt` | Remove transcript preview, simplify status display |
| `ReviewScreen.kt` | Wait for backend results, display transcript + analysis |
| `AndroidManifest.xml` | Add RECORD_AUDIO, INTERNET permissions |
| `build.gradle.kts` | Add Retrofit, OkHttp, Serialization |
| **NEW**: `AudioFileManager.kt` | Audio file lifecycle |
| **NEW**: `BackendService.kt` | HTTP client for ingest endpoint |

---

## Success Criteria

- ✅ RecordScreen shows only timer and recording controls (no transcript preview)
- ✅ Audio saves to local file on finish
- ✅ Audio uploads to backend with multipart form
- ✅ ReviewScreen receives backend results (transcript, mood, tags)
- ✅ Upload errors handled gracefully with retry option
- ✅ No SpeechRecognizer code in ViewModel
- ✅ Permissions granted at runtime
- ✅ End-to-end test: record → navigate to review → see transcript from backend
