package com.kaonixx.guitarix

import android.content.Context
import android.net.Uri
import java.io.InputStream
import kotlin.math.pow

object WavLoader {

    data class WavResult(
        val sampleRate: Int,
        val numChannels: Int,
        val bitsPerSample: Int,
        val samples: FloatArray  // normalized -1.0 to 1.0, interleaved
    )

    fun load(context: Context, uri: Uri): WavResult? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                parseWav(stream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseWav(stream: InputStream): WavResult? {
        // RIFF header
        val riff = readString(stream, 4)       // "RIFF"
        if (riff != "RIFF") return null
        readIntLE(stream)                        // chunk size
        val wave = readString(stream, 4)        // "WAVE"
        if (wave != "WAVE") return null

        var sampleRate = 0
        var numChannels = 0
        var bitsPerSample = 0
        var pcmData: ByteArray? = null

        // Read chunks until we find "data"
        while (true) {
            val chunkId = peekString(stream, 4) ?: break
            when (chunkId) {
                "fmt " -> {
                    readString(stream, 4)         // consume "fmt "
                    val fmtSize = readIntLE(stream)
                    val audioFormat = readShortLE(stream).toInt() and 0xFFFF
                    numChannels = readShortLE(stream).toInt() and 0xFFFF
                    sampleRate = readIntLE(stream)
                    readIntLE(stream)              // byte rate
                    readShortLE(stream)            // block align
                    bitsPerSample = readShortLE(stream).toInt() and 0xFFFF
                    // Skip remaining fmt chunk
                    if (fmtSize > 16) stream.skip((fmtSize - 16).toLong())
                }
                "data" -> {
                    readString(stream, 4)         // consume "data"
                    val dataSize = readIntLE(stream)
                    pcmData = ByteArray(dataSize)
                    var offset = 0
                    while (offset < dataSize) {
                        val read = stream.read(pcmData, offset, dataSize - offset)
                        if (read == -1) break
                        offset += read
                    }
                    break
                }
                else -> {
                    // Skip unknown chunk
                    val chunkSize = readIntLE(stream)
                    stream.skip(chunkSize.toLong())
                }
            }
        }

        val data = pcmData ?: return null
        if (sampleRate == 0 || numChannels == 0 || bitsPerSample == 0) return null

        // Convert PCM bytes to normalized float samples
        val bytesPerSample = bitsPerSample / 8
        val numSamples = data.size / bytesPerSample
        val samples = FloatArray(numSamples)

        when (bitsPerSample) {
            16 -> {
                for (i in 0 until numSamples) {
                    val sample = (data[i * 2].toInt() and 0xFF) or
                            (data[i * 2 + 1].toInt() shl 8)
                    samples[i] = sample.toShort().toFloat() / 32768f
                }
            }
            24 -> {
                for (i in 0 until numSamples) {
                    val sample = (data[i * 3].toInt() and 0xFF) or
                            ((data[i * 3 + 1].toInt() and 0xFF) shl 8) or
                            (data[i * 3 + 2].toInt() shl 16)
                    samples[i] = (sample.toFloat()) / 8388608f
                }
            }
            32 -> {
                for (i in 0 until numSamples) {
                    val sample = (data[i * 4].toInt() and 0xFF).toLong() or
                            ((data[i * 4 + 1].toInt() and 0xFF).toLong() shl 8) or
                            ((data[i * 4 + 2].toInt() and 0xFF).toLong() shl 16) or
                            ((data[i * 4 + 3].toInt() and 0xFF).toLong() shl 24)
                    samples[i] = sample.toInt().toFloat() / 2147483648f
                }
            }
            8 -> {
                for (i in 0 until numSamples) {
                    samples[i] = ((data[i].toInt() and 0xFF) - 128) / 128f
                }
            }
        }

        return WavResult(sampleRate, numChannels, bitsPerSample, samples)
    }

    private fun readString(stream: InputStream, len: Int): String {
        val bytes = ByteArray(len)
        stream.read(bytes)
        return String(bytes, Charsets.US_ASCII)
    }

    private fun peekString(stream: InputStream, len: Int): String? {
        stream.mark(len + 1)
        val bytes = ByteArray(len)
        val read = stream.read(bytes)
        if (read < len) { stream.reset(); return null }
        stream.reset()
        return String(bytes, Charsets.US_ASCII)
    }

    private fun readIntLE(stream: InputStream): Int {
        return (stream.read() and 0xFF) or
                ((stream.read() and 0xFF) shl 8) or
                ((stream.read() and 0xFF) shl 16) or
                (stream.read() shl 24)
    }

    private fun readShortLE(stream: InputStream): Short {
        return ((stream.read() and 0xFF) or ((stream.read() and 0xFF) shl 8)).toShort()
    }
}
