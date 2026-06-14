package com.kaonixx.guitarix

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SystemAudioCapture(private val activity: Activity) {

    private var mediaProjection: MediaProjection? = null
    private var isCapturing = false

    data class CaptureResult(
        val samples: FloatArray,
        val sampleRate: Int,
        val numChannels: Int,
        val durationMs: Int
    )

    fun createCaptureIntent(): Intent {
        val mpm = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return mpm.createScreenCaptureIntent()
    }

    fun setup(resultCode: Int, data: Intent) {
        val mpm = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(resultCode, data)
    }

    suspend fun capture(durationMs: Int = 30000): CaptureResult? = withContext(Dispatchers.IO) {
        val proj = mediaProjection ?: return@withContext null

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

        try {
            val audioRecord = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                proj.createAudioRecord(
                    AudioRecord.AUDIO_SOURCE_DEFAULT,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            } else {
                // Fallback to mic recording for older devices
                AudioRecord(
                    android.media.MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            }

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext null
            }

            val totalSamples = sampleRate * durationMs / 1000
            val buffer = ShortArray(bufferSize / 2)
            val allSamples = mutableListOf<Float>()

            audioRecord.startRecording()
            isCapturing = true

            var collected = 0
            while (collected < totalSamples && isCapturing) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    for (i in 0 until read) {
                        allSamples.add(buffer[i].toFloat() / 32768f)
                    }
                    collected += read
                }
            }

            audioRecord.stop()
            audioRecord.release()

            if (allSamples.isEmpty()) return@withContext null

            CaptureResult(
                samples = allSamples.toFloatArray(),
                sampleRate = sampleRate,
                numChannels = 1,
                durationMs = (allSamples.size * 1000 / sampleRate).coerceAtMost(durationMs)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            isCapturing = false
        }
    }

    fun release() {
        isCapturing = false
        try { mediaProjection?.stop() } catch (_: Exception) {}
        mediaProjection = null
    }

    companion object {
        const val REQUEST_CODE = 1001
    }
}
