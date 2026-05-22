package com.example.thoughts

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "AudioRecorder"

class AudioRecorder(
    private val context: Context,
    private val onPcmChunk: ((bytes: ByteArray, length: Int) -> Unit)? = null,
) {

    private val sampleRateHz = 16_000
    private val channelCount = 1
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO

    private var audioRecord: AudioRecord? = null
    private var recordThread: Thread? = null
    private var wavFile: File? = null
    private var wavWriter: RandomAccessFile? = null
    private var pcmDataLengthBytes: Long = 0L

    private val isRunning = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    fun start(outputFile: File) {
        if (isRunning.get()) return

        try {
            wavFile = outputFile
            pcmDataLengthBytes = 0L

            // Open file and write placeholder WAV header.
            wavWriter = RandomAccessFile(outputFile, "rw").apply {
                setLength(0)
                write(buildWavHeader(dataLengthBytes = 0L))
            }

            val minBuffer = AudioRecord.getMinBufferSize(sampleRateHz, channelConfig, audioFormat)
            val bufferSize = (minBuffer.coerceAtLeast(sampleRateHz / 10 * 2) * 2).coerceAtLeast(4096)

            audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRateHz)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("AudioRecord failed to initialize")
            }

            isPaused.set(false)
            isRunning.set(true)

            audioRecord?.startRecording()
            recordThread = Thread(
                {
                    readLoop(bufferSize)
                },
                "pcm-audio-record"
            ).apply { start() }

            Log.d(TAG, "Started PCM recording to WAV: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start PCM recording", e)
            safeStopInternal(finalizeWav = true)
        }
    }

    fun pause() {
        if (!isRunning.get()) return
        if (isPaused.getAndSet(true)) return

        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause AudioRecord", e)
        }
        Log.d(TAG, "Paused PCM recording")
    }

    fun resume() {
        if (!isRunning.get()) return
        if (!isPaused.getAndSet(false)) return

        try {
            audioRecord?.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume AudioRecord", e)
        }
        Log.d(TAG, "Resumed PCM recording")
    }

    fun stop() {
        safeStopInternal(finalizeWav = true)
    }

    private fun readLoop(bufferSize: Int) {
        val localRecord = audioRecord ?: return
        val buffer = ByteArray(bufferSize)

        while (isRunning.get()) {
            if (isPaused.get()) {
                try {
                    Thread.sleep(50)
                } catch (_: InterruptedException) {
                    // ignore
                }
                continue
            }

            val read = try {
                localRecord.read(buffer, 0, buffer.size)
            } catch (e: Exception) {
                Log.e(TAG, "AudioRecord.read failed", e)
                AudioRecord.ERROR
            }

            if (read < 0) {
                Log.e(TAG, "AudioRecord.read returned error code: $read")
                isRunning.set(false)
                break
            }

            if (read == 0) {
                try {
                    Thread.sleep(10)
                } catch (_: InterruptedException) {
                    // ignore
                }
                continue
            }

            try {
                wavWriter?.write(buffer, 0, read)
                pcmDataLengthBytes += read.toLong()
            } catch (e: Exception) {
                Log.e(TAG, "Failed writing WAV data", e)
            }

            try {
                // Copy to avoid mutation while OkHttp sends.
                val chunk = ByteArray(read)
                System.arraycopy(buffer, 0, chunk, 0, read)
                onPcmChunk?.invoke(chunk, read)
            } catch (e: Exception) {
                Log.e(TAG, "Failed delivering PCM chunk", e)
            }
        }
    }

    private fun safeStopInternal(finalizeWav: Boolean) {
        if (!isRunning.getAndSet(false)) return

        isPaused.set(false)

        try {
            audioRecord?.let {
                try {
                    it.stop()
                } catch (_: Exception) {
                    // ignore
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop/release AudioRecord", e)
        } finally {
            audioRecord = null
        }

        try {
            recordThread?.join(300)
        } catch (_: Exception) {
            // ignore
        } finally {
            recordThread = null
        }

        if (finalizeWav) {
            finalizeWavHeaderSafely()
        }
    }

    private fun finalizeWavHeaderSafely() {
        val writer = wavWriter
        val file = wavFile
        try {
            if (writer != null && file != null) {
                writer.seek(0)
                writer.write(buildWavHeader(dataLengthBytes = pcmDataLengthBytes))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to finalize WAV header", e)
        } finally {
            try {
                wavWriter?.close()
            } catch (_: Exception) {
                // ignore
            }
            wavWriter = null
            wavFile = null
        }
    }

    private fun buildWavHeader(dataLengthBytes: Long): ByteArray {
        val byteRate = sampleRateHz * channelCount * (16 / 8)
        val blockAlign = (channelCount * (16 / 8)).toShort()
        val riffChunkSize = 36L + dataLengthBytes

        val buffer = ByteBuffer.allocate(44)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(riffChunkSize.toInt())
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))

        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1) // AudioFormat PCM
        buffer.putShort(channelCount.toShort())
        buffer.putInt(sampleRateHz)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign)
        buffer.putShort(16) // bitsPerSample

        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataLengthBytes.toInt())

        return buffer.array()
    }
}
