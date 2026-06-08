# Project: Local-First Sanctuary
## Goal: Fully Offline User Experience

This document outlines the architectural transition from a client-server model to a fully local-first implementation. The primary objective is to maximize security and privacy by ensuring data never leaves the device, while maintaining the intelligence currently provided by the backend.

### 🏗️ Core Architecture: The Engine Pattern
To prevent vendor/model lock-in and allow for future upgrades (e.g., switching from Gemma to a newer model), the app will utilize an **Engine Interface** pattern.

- **`TranscriptionEngine`**: Abstract interface for turning audio into text.
- **`AnalysisEngine`**: Abstract interface for extracting tags, mood, and takeaways from text.

By coding to interfaces rather than specific models, we can swap the underlying AI without touching the UI or ViewModel logic.

---

### 🚀 Implementation Phases

#### Phase 1: Data Sovereignty (The Foundation)
*Goal: Eliminate all dependencies on the remote backend for data storage and retrieval.*

- [ ] **Local Database Migration**
    - Replace API calls with a **Room Database**.
    - Create entities for `JournalEntry`, `Draft`, `UserProfile`, and `Preferences`.
    - Update `JournalRepository` to use Room as the single source of truth.
- [ ] **Local Asset Management**
    - Update `AudioFileManager` to store audio files in encrypted internal storage.
    - Remove audio upload logic.
- [ ] **Security & Identity**
    - Remove `AuthRepository` and all backend authentication endpoints.
    - Implement **Android Biometric Prompt** (Fingerprint/FaceID) to lock access to the local database.

#### Phase 2: The "Ear" (On-Device Transcription)
*Goal: Replace WebSocket-based streaming with local Speech-to-Text (STT).*

- [ ] **Transcription Abstraction**
    - Implement the `TranscriptionEngine` interface.
- [ ] **Whisper Integration**
    - Integrate **whisper.cpp** or **MediaPipe STT**.
    - Implement a model downloader to fetch the Whisper `.bin` weights (e.g., Base or Small) upon first launch.
- [ ] **ViewModel Integration**
    - Route audio PCM chunks from `AudioRecorder` directly into the `TranscriptionEngine`.

#### Phase 3: The "Brain" (On-Device Analysis)
*Goal: Replace the backend LLM with a local Large Language Model.*

- [ ] **Analysis Abstraction**
    - Implement the `AnalysisEngine` interface.
- [ ] **Gemma Integration**
    - Integrate **MediaPipe LLM Inference API** to run Gemma 2B.
    - Develop a strict JSON-prompting system to ensure the model returns consistent `tags`, `mood`, and `takeaways`.
- [ ] **Processing Pipeline**
    - Implement a background worker via `WorkManager` to trigger analysis once a recording is finalized.

#### Phase 4: Resource & Power Management
*Goal: Mitigate the "Phone Heavy" impact on battery and performance.*

- [ ] **Hardware Acceleration**
    - Configure **Vulkan** and **NNAPI** support to offload inference to the GPU/NPU.
- [ ] **Memory Lifecycle Management**
    - Implement an automated loader/unloader for the LLM to prevent the Android OS from killing the app due to high RAM usage.
- [ ] **Tiered Intelligence**
    - (Optional) Implement a fallback to smaller models for devices with < 8GB RAM.

---

### 🛠️ Technical Stack
| Component | Technology |
| :--- | :--- |
| **Database** | Room Persistence Library |
| **STT Model** | OpenAI Whisper (Base/Small) |
| **LLM Model** | Gemma 2B (4-bit Quantized) |
| **AI Framework** | MediaPipe LLM Inference / whisper.cpp |
| **Acceleration** | Vulkan / NNAPI |
| **Security** | Android Biometric API |
| **Storage** | Encrypted Internal Storage |
