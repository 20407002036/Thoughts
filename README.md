# Thoughts 💭

**Thoughts** is a modern, voice-first journaling application for Android. It's designed to help you capture your daily reflections effortlessly through audio, providing automatic transcription, AI-driven sentiment analysis, and a seamless offline-to-online experience.

## ✨ Features

- **🎙️ Voice-First Experience**: Record your entries naturally. The app handles high-quality audio capture and provides real-time transcription feedback.
- **🤖 AI-Powered Analysis**: 
    - **Mood Tracking**: Automatically detects your mood (label, score, and confidence).
    - **Smart Tagging**: Generates relevant tags for your entries.
    - **Contextual Insights**: Provides explanations for detected emotional states using on-device GenAI.
- **📊 Personalized Dashboard**: Stay engaged with daily journaling prompts, streak tracking, and summary statistics.
- **☁️ Offline-First & Sync**:
    - Full local persistence using **Room**.
    - Background synchronization with a remote backend via **WorkManager**.
    - Reliable audio uploads with state tracking (Pending -> Uploading -> Uploaded).
- **🔒 Security & Privacy**: 
    - **Biometric Authentication**: Secure your journal with your fingerprint or face.
    - **Encrypted Preferences**: Sensitive user settings are stored securely using `EncryptedSharedPreferences`.
- **🎨 Modern UI/UX**: Built entirely with **Jetpack Compose**, featuring a Material 3 design, smooth transitions, and support for both Light and Dark themes.

## 🛠 Tech Stack

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (BOM 2024.10.00)
- **Architecture**: MVVM with a robust Repository pattern.
- **Local Database**: [Room](https://developer.android.com/training/data-storage/room) (SQLite) for caching and offline support.
- **Preferences**: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for type-safe user settings.
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/) with [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization).
- **Background Work**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for media ingestion and syncing.
- **AI/ML**: [Google MediaPipe GenAI](https://developers.google.com/mediapipe/solutions/genai) for local intelligence.
- **Concurrency**: Kotlin Coroutines & Flow for reactive data streams.

## 📁 Project Structure

```text
app/src/main/java/com/example/thoughts/
├── ui/                 # UI components, themes, and common composables
├── data/               # Data layer
│   ├── local/          # Room Database, DAOs, and Entities
│   └── remote/         # Retrofit API definitions and models
├── work/               # WorkManager workers (e.g., UploadWorker)
├── MainActivity.kt     # App entry point and Navigation Compose setup
├── JournalRepository.kt # Main data orchestrator (Sync, Local, Remote)
└── AudioRecorder.kt    # Low-level audio capture implementation
```

## 🚀 Getting Started

### Prerequisites

- **Android Studio Koala** | 2024.1.1 or newer.
- **Android SDK Level 26+** (Android 8.0 Oreo).
- A backend server implementing the `BackendService` endpoints.

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/20407002036/thoughts.git
   ```
2. **Open in Android Studio**:
   Open the root `Thoughts` folder and allow Gradle to sync.
3. **Configure API Endpoints**:
   Update `BackendService.kt` with your server's base URL.
4. **Build & Run**:
   Connect an Android device or start an emulator and click the **Run** icon.

## 🏗 Architecture Details

The app follows the **Clean Architecture** principles and the **MVVM** pattern:
- **View**: Jetpack Compose screens that observe state from ViewModels.
- **ViewModel**: Manages UI state and interacts with the Repository.
- **Repository**: (e.g., `JournalRepository`) Acts as a single source of truth, mediating between the local Room database (cache) and the remote API.
- **Local Data Source**: Room provides an offline-first experience, ensuring the app is usable without an internet connection.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
