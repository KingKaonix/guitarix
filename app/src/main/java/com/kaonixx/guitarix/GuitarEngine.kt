package com.kaonixx.guitarix

class GuitarEngine {
    companion object {
        init { System.loadLibrary("guitarix_engine") }

        // Effect indices — must match engine.h enum EffectIndex
        const val FX_DISTORTION = 0
        const val FX_AMP_SIM    = 1
        const val FX_EQ         = 2
        const val FX_CHORUS     = 3
        const val FX_DELAY      = 4
        const val FX_NOISE_GATE = 5
        const val FX_COMPRESSOR = 6
        const val FX_REVERB     = 7
        const val FX_TUNER      = 8
        const val FX_TONE_MATCHER = 9

        // Distortion params
        const val PARAM_DRIVE = 0
        const val PARAM_TONE  = 1
        const val PARAM_LEVEL = 2

        // AmpSim params
        const val PARAM_GAIN   = 0
        const val PARAM_TONE2  = 1
        const val PARAM_MASTER = 2

        // EQ params
        const val PARAM_BASS   = 0
        const val PARAM_MID    = 1
        const val PARAM_TREBLE = 2

        // Noise Gate params
        const val PARAM_THRESHOLD2 = 0
        const val PARAM_ATTACK2 = 1
        const val PARAM_RELEASE2 = 2

        // Compressor params
        const val PARAM_THRESHOLD3 = 0
        const val PARAM_RATIO = 1
        const val PARAM_ATTACK3 = 2
        const val PARAM_RELEASE3 = 3

        // Chorus params
        const val PARAM_RATE  = 0
        const val PARAM_DEPTH = 1
        const val PARAM_MIX   = 2

        // Delay params
        const val PARAM_MIX2      = 0
        const val PARAM_FEEDBACK  = 1
        const val PARAM_DELAY_MS  = 2

        // Reverb params
        const val PARAM_ROOM_SIZE = 0
        const val PARAM_MIX3      = 1

        val effectNames = listOf("Distortion", "Amp Sim", "EQ", "Chorus", "Noise Gate", "Compressor", "Delay", "Reverb", "Tuner", "Tone Matcher")
        val presetNames = listOf("Clean", "Crunch", "Lead", "Metal", "Ambient")
    }

    private var nativePtr: Long = 0

    init { nativePtr = nativeCreate() }

    protected fun finalize() {
        if (nativePtr != 0L) nativeDestroy(nativePtr)
    }

    fun start(): Boolean = nativeStart(nativePtr)
    fun stop() = nativeStop(nativePtr)

    fun setEffectEnabled(index: Int, enabled: Boolean) =
        nativeSetEffectEnabled(nativePtr, index, enabled)
    fun isEffectEnabled(index: Int): Boolean =
        nativeIsEffectEnabled(nativePtr, index)

    fun setEffectParameter(index: Int, paramId: Int, value: Float) =
        nativeSetEffectParameter(nativePtr, index, paramId, value)
    fun getEffectParameter(index: Int, paramId: Int): Float =
        nativeGetEffectParameter(nativePtr, index, paramId)

    fun loadPreset(preset: Int) = nativeLoadPreset(nativePtr, preset)

    // Recording
    fun startRecording(filePath: String) = nativeStartRecording(nativePtr, filePath)
    fun stopRecording() = nativeStopRecording(nativePtr)
    fun isRecording(): Boolean = nativeIsRecording(nativePtr)
    fun loadImpulseResponse(path: String): Boolean = nativeLoadImpulseResponse(nativePtr, path)

    // Tuner operations
    fun loadAudioForTuner(data: FloatArray, numFrames: Int, numChannels: Int) =
        nativeLoadAudioForTuner(nativePtr, data, numFrames, numChannels)
    fun getTunerFrequency(): Float = nativeGetTunerFrequency(nativePtr)
    fun getTunerNoteIndex(): Int = nativeGetTunerNoteIndex(nativePtr)
    fun getTunerOctave(): Int = nativeGetTunerOctave(nativePtr)
    fun getTunerCents(): Float = nativeGetTunerCents(nativePtr)
    fun isTunerNoteDetected(): Boolean = nativeIsTunerNoteDetected(nativePtr)
    fun getTunerCurrentTuningName(): String = nativeGetTunerCurrentTuningName(nativePtr)
    fun getTunerNoteName(index: Int): String = nativeGetTunerNoteName(nativePtr, index)

    // Tone matcher operations
    fun loadAudioForToneMatcher(data: FloatArray, numFrames: Int, numChannels: Int) =
        nativeLoadAudioForToneMatcher(nativePtr, data, numFrames, numChannels)
    fun hasToneMatcherProfile(): Boolean = nativeHasToneMatcherProfile(nativePtr)
    fun getRecommendedDistortionDrive(): Float = nativeGetRecommendedDistortionDrive(nativePtr)
    fun getRecommendedDistortionTone(): Float = nativeGetRecommendedDistortionTone(nativePtr)
    fun getRecommendedDistortionLevel(): Float = nativeGetRecommendedDistortionLevel(nativePtr)
    fun getRecommendedAmpSimGain(): Float = nativeGetRecommendedAmpSimGain(nativePtr)
    fun getRecommendedAmpSimTone(): Float = nativeGetRecommendedAmpSimTone(nativePtr)
    fun getRecommendedAmpSimMaster(): Float = nativeGetRecommendedAmpSimMaster(nativePtr)
    fun getRecommendedEqBass(): Float = nativeGetRecommendedEqBass(nativePtr)
    fun getRecommendedEqMid(): Float = nativeGetRecommendedEqMid(nativePtr)
    fun getRecommendedEqTreble(): Float = nativeGetRecommendedEqTreble(nativePtr)
    fun getRecommendedChorusRate(): Float = nativeGetRecommendedChorusRate(nativePtr)
    fun getRecommendedChorusDepth(): Float = nativeGetRecommendedChorusDepth(nativePtr)
    fun getRecommendedChorusMix(): Float = nativeGetRecommendedChorusMix(nativePtr)
    fun getRecommendedDelayMix(): Float = nativeGetRecommendedDelayMix(nativePtr)
    fun getRecommendedDelayFeedback(): Float = nativeGetRecommendedDelayFeedback(nativePtr)
    fun getRecommendedDelayTime(): Float = nativeGetRecommendedDelayTime(nativePtr)
    fun getRecommendedReverbSize(): Float = nativeGetRecommendedReverbSize(nativePtr)
    fun getRecommendedReverbMix(): Float = nativeGetRecommendedReverbMix(nativePtr)

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(ptr: Long)
    private external fun nativeStart(ptr: Long): Boolean
    private external fun nativeStop(ptr: Long)
    private external fun nativeSetEffectEnabled(ptr: Long, index: Int, enabled: Boolean)
    private external fun nativeIsEffectEnabled(ptr: Long, index: Int): Boolean
    private external fun nativeSetEffectParameter(ptr: Long, index: Int, paramId: Int, value: Float)
    private external fun nativeGetEffectParameter(ptr: Long, index: Int, paramId: Int): Float
    private external fun nativeLoadPreset(ptr: Long, preset: Int)

    // Recording and IR
    private external fun nativeStartRecording(ptr: Long, filePath: String)
    private external fun nativeStopRecording(ptr: Long)
    private external fun nativeIsRecording(ptr: Long): Boolean
    private external fun nativeLoadImpulseResponse(ptr: Long, path: String): Boolean

    // Tuner JNI methods
    private external fun nativeLoadAudioForTuner(ptr: Long, data: FloatArray, numFrames: Int, numChannels: Int)
    private external fun nativeGetTunerFrequency(ptr: Long): Float
    private external fun nativeGetTunerNoteIndex(ptr: Long): Int
    private external fun nativeGetTunerOctave(ptr: Long): Int
    private external fun nativeGetTunerCents(ptr: Long): Float
    private external fun nativeIsTunerNoteDetected(ptr: Long): Boolean
    private external fun nativeGetTunerCurrentTuningName(ptr: Long): String
    private external fun nativeGetTunerNoteName(ptr: Long, index: Int): String

    // Tone matcher JNI methods
    private external fun nativeLoadAudioForToneMatcher(ptr: Long, data: FloatArray, numFrames: Int, numChannels: Int)
    private external fun nativeHasToneMatcherProfile(ptr: Long): Boolean
    private external fun nativeGetRecommendedDistortionDrive(ptr: Long): Float
    private external fun nativeGetRecommendedDistortionTone(ptr: Long): Float
    private external fun nativeGetRecommendedDistortionLevel(ptr: Long): Float
    private external fun nativeGetRecommendedAmpSimGain(ptr: Long): Float
    private external fun nativeGetRecommendedAmpSimTone(ptr: Long): Float
    private external fun nativeGetRecommendedAmpSimMaster(ptr: Long): Float
    private external fun nativeGetRecommendedEqBass(ptr: Long): Float
    private external fun nativeGetRecommendedEqMid(ptr: Long): Float
    private external fun nativeGetRecommendedEqTreble(ptr: Long): Float
    private external fun nativeGetRecommendedChorusRate(ptr: Long): Float
    private external fun nativeGetRecommendedChorusDepth(ptr: Long): Float
    private external fun nativeGetRecommendedChorusMix(ptr: Long): Float
    private external fun nativeGetRecommendedDelayMix(ptr: Long): Float
    private external fun nativeGetRecommendedDelayFeedback(ptr: Long): Float
    private external fun nativeGetRecommendedDelayTime(ptr: Long): Float
    private external fun nativeGetRecommendedReverbSize(ptr: Long): Float
    private external fun nativeGetRecommendedReverbMix(ptr: Long): Float
}
