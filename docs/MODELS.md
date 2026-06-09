# Model Setup Guide

Because AI model weights are large, they are not included in the git repository. Follow these steps to get the models onto your device for development.

## 1. Gemma 2B (Analysis Engine)

The app uses a 4-bit quantized version of **Gemma 2B** via the MediaPipe LLM Inference API.

### Download
You can download the compatible model from the [Kaggle Model Garden](https://www.kaggle.com/models/google/gemma) or the MediaPipe website. Ensure you download the `gemma-2b-it-cpu-int4` or equivalent GPU-optimized version.

### Installation
1. Connect your Android device via USB.
2. Push the `.bin` model file to the app's internal storage:
   ```bash
   adb push gemma-2b-it-cpu-int4.bin /data/local/tmp/thoughts_model.bin
   ```
3. In the app's **Developer Settings**, set the "Model Path" to:
   `/data/local/tmp/thoughts_model.bin`

---

## 2. Whisper (Transcription Engine)

If you are using the `WhisperEngine` instead of the system transcription:

### Download
Download the `base` or `small` Whisper weights (GGML format).

### Installation
Push the weights to the internal storage:
```bash
adb push ggml-base.en.bin /data/local/tmp/whisper_model.bin
```

---

## 🛠️ Troubleshooting

### "Model not found" Error
If the app crashes on initialization, verify that:
- The file was pushed to a directory the app has permission to read.
- The file name in the app settings matches the filename on the disk exactly.

### Performance Issues
If inference is extremely slow:
- Ensure you are using a **physical device**. The Android Emulator does not support Vulkan/GPU acceleration for MediaPipe LLMs.
- Check that your device has at least 8GB of RAM.
