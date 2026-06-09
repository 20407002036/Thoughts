# Architecture: The Local-First Sanctuary

This document details the architectural decisions that drive **Thoughts**, focusing on the transition from a cloud-dependent model to a local-first, AI-powered experience.

## 1. The Local-First Pivot

Originally conceived as a client-server application, Thoughts evolved into a local-first system to solve three primary problems:
1. **Privacy**: User reflections are deeply personal. Eliminating the backend removes the possibility of data breaches on the server side.
2. **Availability**: Journaling should not depend on a 5G signal.
3. **Latency**: LLM analysis of a 2-minute voice note can take several seconds over a network; on-device GPU acceleration makes this feel instantaneous.

### Data Sovereignty Model
We replaced the `AuthRepository` and `BackendService` with a local-first source of truth:
- **Room Database**: Stores all `JournalEntry`, `Draft`, and `UserProfile` data.
- **Encrypted Internal Storage**: Audio files are stored in the app's private directory, ensuring other apps cannot access the raw voice recordings.
- **Biometric Gatekeeper**: The app uses the Android Biometric API to ensure that the local database is only accessible after successful authentication.

---

## 2. The Engine Pattern

To prevent the application from becoming tightly coupled to a specific AI model or vendor, we implemented the **Engine Pattern**. This is a strategy where the business logic depends on abstract interfaces rather than concrete implementations.

### TranscriptionEngine
The `TranscriptionEngine` defines how the app converts audio to text.

```kotlin
interface TranscriptionEngine {
    fun startTranscription(callback: (String) -> Unit)
    fun stopTranscription()
    fun setLanguage(languageCode: String)
}
```

**Implementations:**
- `SystemTranscriptionEngine`: Uses the native Android `SpeechRecognizer`. Reliable and lightweight.
- `WhisperEngine`: Uses a local `whisper.cpp` implementation for superior accuracy.

### AnalysisEngine
The `AnalysisEngine` defines how the app extracts meaning from text.

```kotlin
interface AnalysisEngine {
    suspend fun analyze(text: String): AnalysisResult
    suspend fun initialize()
    suspend fun release()
}
```

**Implementation: MediaPipe Gemma Engine**
The current primary implementation uses the **MediaPipe LLM Inference API** to run **Gemma 2B**. 

#### The Prompting Strategy
Since LLMs can be unpredictable, we use a **Strict JSON Prompting** system. The engine wraps the user's text in a system prompt that forces the model to output a valid JSON object containing:
- `mood`: A single-word emotional label.
- `tags`: A list of 3-5 descriptive keywords.
- `takeaway`: A one-sentence summary of the insight.

---

## 3. Mobile AI Performance Engineering

Running a 2-billion parameter model on a mobile device introduces significant constraints. We address these via:

### GPU Acceleration
We configure MediaPipe to use **GPU delegates** (Vulkan/NNAPI). This shifts the heavy tensor operations from the CPU to the GPU, reducing inference time from ~30 seconds to < 5 seconds for typical journal entries.

### Memory Lifecycle Management
An LLM is a RAM hog. To prevent the OS from killing the app or slowing down the rest of the phone:
1. **Lazy Initialization**: The `AnalysisEngine` is only initialized when the user actually finishes a recording.
2. **Inactivity Unloader**: A timer tracks the last time the AI was used. If the app is idle for 5 minutes, the LLM is released from memory.
3. **RAM Monitoring**: The app monitors available system RAM. If the device has $< 8\text{GB}$ of RAM, the app provides a warning and may fallback to a lighter analysis mode.

---

## 4. Summary of Data Flow

1. **Capture**: Audio is recorded $\rightarrow$ `TranscriptionEngine` converts to text.
2. **Processing**: Text is passed to `AnalysisEngine` (Gemma 2B) $\rightarrow$ JSON result returned.
3. **Persistence**: Transcript + Analysis $\rightarrow$ Room Database.
4. **Observation**: UI observes the Room Database via `Flow` and updates the screen.
