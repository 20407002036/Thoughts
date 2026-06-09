# 🌿 Thoughts: A Local-First Sanctuary

**Thoughts** is a privacy-centric, AI-powered journaling application designed to be a "Local-First Sanctuary" for your mind. Unlike traditional journaling apps, Thoughts ensures that your most personal reflections never leave your device, combining the power of modern Large Language Models (LLMs) with absolute data sovereignty.

---

## 🌟 Core Philosophy: Local-First
In an era of cloud-dependency, **Thoughts** pivots away from the client-server model. We believe that privacy isn't a "feature"—it's a fundamental right. By moving the "brain" of the app from a remote server to the device itself, we achieve:
- **Absolute Privacy**: Your transcripts, moods, and reflections are stored only on your hardware.
- **Zero Latency**: No more waiting for network requests to process your thoughts.
- **Permanent Access**: Your journal is available regardless of your internet connection.
- **Data Sovereignty**: You own your data; there is no central database to leak or lose.

---

## ✨ Key Features

### 🎙️ Voice-First Journaling
Capture your thoughts naturally. Thoughts uses an abstracted transcription layer to convert speech to text with high accuracy, allowing you to focus on the reflection, not the typing.

### 🧠 On-Device Intelligence
Your entries are automatically analyzed by a local LLM to extract:
- **Mood Analysis**: Detecting the emotional tone of your entry.
- **Automatic Tagging**: Identifying recurring themes and topics.
- **Key Takeaways**: Summarizing the core essence of your reflection.

### 🛡️ Sovereign Security
- **Biometric Lock**: Integrated Android Biometric Prompt (Fingerprint/FaceID) ensures only you can access your sanctuary.
- **Encrypted Storage**: All audio assets and database records are stored in encrypted internal storage.

### ⚡ Mobile-Optimized AI
Running an LLM on a phone is challenging. Thoughts implements several optimizations to ensure a smooth experience:
- **GPU Acceleration**: Leveraging Vulkan and NNAPI via MediaPipe for fast inference.
- **Lazy Loading**: An intelligent "Lazy Loader" with a 5-minute inactivity timeout to prevent RAM exhaustion.
- **Tiered Intelligence**: Real-time RAM monitoring to ensure stability across different device tiers.

---

## 🏗️ Architecture

### The Engine Pattern
To avoid model lock-in, Thoughts utilizes an **Engine Interface** pattern. This allows the underlying AI models to be swapped without touching the UI or business logic.

- **`TranscriptionEngine`**: Abstract interface for Speech-to-Text. Current implementations include Android `SpeechRecognizer` and Whisper.
- **`AnalysisEngine`**: Abstract interface for text analysis. Powered by **Gemma 2B (4-bit Quantized)** via the MediaPipe LLM Inference API.

### Data Flow
`Voice Input` $\rightarrow$ `TranscriptionEngine` $\rightarrow$ `AnalysisEngine` $\rightarrow$ `Room Database` $\rightarrow$ `Jetpack Compose UI`

---

## 🛠️ Tech Stack

| Layer | Technology |
| :--- | :--- |
| **UI Framework** | Jetpack Compose |
| **Local Database** | Room Persistence Library |
| **LLM Model** | Gemma 2B (4-bit Quantized) |
| **AI Framework** | MediaPipe LLM Inference API |
| **STT** | OpenAI Whisper / Android SpeechRecognizer |
| **Acceleration** | Vulkan / NNAPI |
| **Security** | Android Biometric API |

---

## 📸 Screenshots

*(Add your screenshots here!)*

### Dashboard
![Dashboard Placeholder](https://via.placeholder.com/300x600?text=Dashboard+Screenshot)
*The calm entry point to your sanctuary.*

### Recording Experience
![Recording Placeholder](https://via.placeholder.com/300x600?text=Recording+Screenshot)
*Voice-first interface with real-time waveform.*

### Entry Detail & Analysis
![Detail Placeholder](https://via.placeholder.com/300x600?text=Detail+Screenshot)
*AI-generated tags, mood, and takeaways.*

---

## 🚀 Getting Started for Developers

### Prerequisites
- Android Studio Jellyfish or newer.
- A physical Android device with at least 8GB RAM (recommended for LLM performance).
- Android 12+ (API 31+).

### Setup
1. Clone the repository.
2. Sync Gradle.
3. **Model Assets**: The LLM model files are too large for Git. Please follow the setup guide in `docs/MODELS.md` to download and place the Gemma 2B `.bin` files in the correct internal storage directory.
4. Run the app.

---

## 🗺️ Roadmap
- [ ] **Deep Asset Linking**: Better relational mapping between audio files and draft entries.
- [ ] **Background Processing**: Utilizing `WorkManager` for deferred analysis of recordings.
- [ ] **Advanced Insights**: Longitudinal mood tracking and theme distribution dashboards.
- [ ] **Customizable Models**: Allowing users to choose their preferred local LLM.
