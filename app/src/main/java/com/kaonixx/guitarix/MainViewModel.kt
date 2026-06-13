package com.kaonixx.guitarix

import android.app.Application

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.kaonixx.guitarix.GuitarEngine.Companion.FX_AMP_SIM
import com.kaonixx.guitarix.GuitarEngine.Companion.FX_CHORUS
import com.kaonixx.guitarix.GuitarEngine.Companion.FX_DELAY
import com.kaonixx.guitarix.GuitarEngine.Companion.FX_DISTORTION
import com.kaonixx.guitarix.GuitarEngine.Companion.FX_EQ
import com.kaonixx.guitarix.GuitarEngine.Companion.FX_REVERB
import com.kaonixx.guitarix.GuitarEngine.Companion.FX_NOISE_GATE
import com.kaonixx.guitarix.GuitarEngine.Companion.FX_COMPRESSOR
import com.kaonixx.guitarix.GuitarEngine.Companion.FX_TONE_MATCHER

// Data class for tablature notes from transcription
data class TabNoteData(val stringNum: Int, val fret: Int, val startTime: Float, val duration: Float)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    val engine = GuitarEngine()
    private var tunerPollJob: Job? = null

    // --- Observed state ---
    var isRunning by mutableStateOf(false); private set
    var currentPresetIndex by mutableIntStateOf(0); private set

    // UI state - current selected tab
    var currentTab by mutableIntStateOf(0); private set  // 0=Effects, 1=Tuner, 2=Tone Match

    // Per-effect enable state
    var distortionOn by mutableStateOf(false); private set
    var ampSimOn by mutableStateOf(true); private set
    var noiseGateOn by mutableStateOf(true); private set
    var compressorOn by mutableStateOf(true); private set
    var eqOn by mutableStateOf(false); private set
    var chorusOn by mutableStateOf(false); private set
    var delayOn by mutableStateOf(true); private set
    var reverbOn by mutableStateOf(true); private set

    // Distortion params
    var distortionDrive by mutableFloatStateOf(0.5f); private set
    var distortionTone by mutableFloatStateOf(0.5f); private set
    var distortionLevel by mutableFloatStateOf(0.7f); private set

    var ampSimGain by mutableFloatStateOf(0.5f); private set
    var ampSimTone by mutableFloatStateOf(0.5f); private set
    var ampSimMaster by mutableFloatStateOf(0.7f); private set

    // Noise Gate params
    var noiseGateThreshold by mutableFloatStateOf(0.75f); private set  // -60dB default (maps to -60 + 0.75*80 = 0dB)
    var noiseGateAttack by mutableFloatStateOf(0.5f); private set
    var noiseGateRelease by mutableFloatStateOf(0.5f); private set

    // Compressor params
    var compressorThreshold by mutableFloatStateOf(0.67f); private set  // -20dB default
    var compressorRatio by mutableFloatStateOf(0.16f); private set       // 4:1 default
    var compressorAttack by mutableFloatStateOf(0.5f); private set
    var compressorRelease by mutableFloatStateOf(0.5f); private set

    var eqBass by mutableFloatStateOf(0.5f); private set
    var eqMid by mutableFloatStateOf(0.5f); private set
    var eqTreble by mutableFloatStateOf(0.5f); private set

    var chorusRate by mutableFloatStateOf(0.5f); private set
    var chorusDepth by mutableFloatStateOf(0.3f); private set
    var chorusMix by mutableFloatStateOf(0.3f); private set

    var delayMix by mutableFloatStateOf(0.3f); private set
    var delayFeedback by mutableFloatStateOf(0.3f); private set
    var delayMs by mutableFloatStateOf(400f); private set

    var reverbRoomSize by mutableFloatStateOf(0.3f); private set
    var reverbMix by mutableFloatStateOf(0.2f); private set

    // Tuner state
    var tunerFrequency by mutableFloatStateOf(0.0f); private set
    var tunerNoteIndex by mutableIntStateOf(-1); private set
    var tunerOctave by mutableIntStateOf(0); private set
    var tunerCents by mutableFloatStateOf(0.0f); private set
    var isTunerNoteDetected by mutableStateOf(false); private set
    var tunerCurrentTuning by mutableIntStateOf(0); private set  // 0=Standard, 1=Drop D, etc.

    // Tone matcher state
    var toneMatcherHasProfile by mutableStateOf(false); private set
    var toneMatcherRecommendedDistortionDrive by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedDistortionTone by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedDistortionLevel by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedAmpSimGain by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedAmpSimTone by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedAmpSimMaster by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedEqBass by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedEqMid by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedEqTreble by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedChorusRate by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedChorusDepth by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedChorusMix by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedDelayMix by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedDelayFeedback by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedDelayTime by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedReverbSize by mutableFloatStateOf(0.5f); private set
    var toneMatcherRecommendedReverbMix by mutableFloatStateOf(0.5f); private set

    // Transcription state
    var transcribeProgress by mutableFloatStateOf(0.0f); private set
    var transcribeHasResult by mutableStateOf(false); private set
    var transcribeNumMeasures by mutableIntStateOf(0); private set
    var transcribeNotes by mutableStateOf<List<TabNoteData>>(emptyList()); private set

    val presetNames = GuitarEngine.presetNames
    val effectNames = GuitarEngine.effectNames

    init {
        // Load current recommendations when tone matcher has a profile
        updateToneMatcherRecommendations()
    }

    fun toggleEngine() {
        if (isRunning) {
            engine.stop()
            isRunning = false
            stopTunerPolling()
        } else {
            isRunning = engine.start()
            if (isRunning) startTunerPolling()
        }
    }

    private fun startTunerPolling() {
        tunerPollJob = viewModelScope.launch {
            while (isActive) {
                updateTunerState()
                delay(50)
            }
        }
    }

    private fun stopTunerPolling() {
        tunerPollJob?.cancel()
        tunerPollJob = null
    }

    fun toggleAmpSim() {
        ampSimOn = !ampSimOn
        engine.setEffectEnabled(FX_AMP_SIM, ampSimOn)
    }

    fun toggleDelay() {
        delayOn = !delayOn
        engine.setEffectEnabled(FX_DELAY, delayOn)
    }

    fun toggleReverb() {
        reverbOn = !reverbOn
        engine.setEffectEnabled(FX_REVERB, reverbOn)
    }

    fun toggleNoiseGate() {
        noiseGateOn = !noiseGateOn
        engine.setEffectEnabled(FX_NOISE_GATE, noiseGateOn)
    }

    fun toggleCompressor() {
        compressorOn = !compressorOn
        engine.setEffectEnabled(FX_COMPRESSOR, compressorOn)
    }

    fun toggleEq() {
        eqOn = !eqOn
        engine.setEffectEnabled(FX_EQ, eqOn)
    }

    fun toggleDistortion() {
        distortionOn = !distortionOn
        engine.setEffectEnabled(FX_DISTORTION, distortionOn)
    }

    fun toggleChorus() {
        chorusOn = !chorusOn
        engine.setEffectEnabled(FX_CHORUS, chorusOn)
    }

    fun setTab(tab: Int) { currentTab = tab }

    fun loadPreset(index: Int) {
        currentPresetIndex = index
        engine.loadPreset(index)
        syncParamsFromPreset(index)
    }

    fun syncParamsFromPreset(index: Int) {
        distortionOn  = engine.isEffectEnabled(FX_DISTORTION)
        ampSimOn      = engine.isEffectEnabled(FX_AMP_SIM)
        eqOn          = engine.isEffectEnabled(FX_EQ)
        chorusOn      = engine.isEffectEnabled(FX_CHORUS)
        delayOn       = engine.isEffectEnabled(FX_DELAY)
        reverbOn      = engine.isEffectEnabled(FX_REVERB)

        distortionDrive = engine.getEffectParameter(FX_DISTORTION, GuitarEngine.PARAM_DRIVE)
        distortionTone  = engine.getEffectParameter(FX_DISTORTION, GuitarEngine.PARAM_TONE)
        distortionLevel = engine.getEffectParameter(FX_DISTORTION, GuitarEngine.PARAM_LEVEL)

        ampSimGain   = engine.getEffectParameter(FX_AMP_SIM, GuitarEngine.PARAM_GAIN)
        ampSimTone   = engine.getEffectParameter(FX_AMP_SIM, GuitarEngine.PARAM_TONE2)
        ampSimMaster = engine.getEffectParameter(FX_AMP_SIM, GuitarEngine.PARAM_MASTER)

        eqBass   = engine.getEffectParameter(FX_EQ, GuitarEngine.PARAM_BASS)
        eqMid    = engine.getEffectParameter(FX_EQ, GuitarEngine.PARAM_MID)
        eqTreble = engine.getEffectParameter(FX_EQ, GuitarEngine.PARAM_TREBLE)

        chorusRate  = engine.getEffectParameter(FX_CHORUS, GuitarEngine.PARAM_RATE)
        chorusDepth = engine.getEffectParameter(FX_CHORUS, GuitarEngine.PARAM_DEPTH)
        chorusMix   = engine.getEffectParameter(FX_CHORUS, GuitarEngine.PARAM_MIX)

        delayMix     = engine.getEffectParameter(FX_DELAY, GuitarEngine.PARAM_MIX2)
        delayFeedback = engine.getEffectParameter(FX_DELAY, GuitarEngine.PARAM_FEEDBACK)
        delayMs      = engine.getEffectParameter(FX_DELAY, GuitarEngine.PARAM_DELAY_MS)

        reverbRoomSize = engine.getEffectParameter(FX_REVERB, GuitarEngine.PARAM_ROOM_SIZE)
        reverbMix      = engine.getEffectParameter(FX_REVERB, GuitarEngine.PARAM_MIX3)
    }

    fun updateDistortionDrive(v: Float) { distortionDrive = v; engine.setEffectParameter(FX_DISTORTION, GuitarEngine.PARAM_DRIVE, v) }
    fun updateDistortionTone(v: Float) { distortionTone = v; engine.setEffectParameter(FX_DISTORTION, GuitarEngine.PARAM_TONE, v) }
    fun updateDistortionLevel(v: Float) { distortionLevel = v; engine.setEffectParameter(FX_DISTORTION, GuitarEngine.PARAM_LEVEL, v) }

    fun updateAmpSimGain(v: Float) { ampSimGain = v; engine.setEffectParameter(FX_AMP_SIM, GuitarEngine.PARAM_GAIN, v) }
    fun updateAmpSimTone(v: Float) { ampSimTone = v; engine.setEffectParameter(FX_AMP_SIM, GuitarEngine.PARAM_TONE2, v) }
    fun updateAmpSimMaster(v: Float) { ampSimMaster = v; engine.setEffectParameter(FX_AMP_SIM, GuitarEngine.PARAM_MASTER, v) }

    fun updateNoiseGateThreshold(v: Float) { noiseGateThreshold = v; engine.setEffectParameter(FX_NOISE_GATE, GuitarEngine.PARAM_THRESHOLD2, v) }
    fun updateNoiseGateAttack(v: Float) { noiseGateAttack = v; engine.setEffectParameter(FX_NOISE_GATE, GuitarEngine.PARAM_ATTACK2, v) }
    fun updateNoiseGateRelease(v: Float) { noiseGateRelease = v; engine.setEffectParameter(FX_NOISE_GATE, GuitarEngine.PARAM_RELEASE2, v) }

    fun updateCompressorThreshold(v: Float) { compressorThreshold = v; engine.setEffectParameter(FX_COMPRESSOR, GuitarEngine.PARAM_THRESHOLD3, v) }
    fun updateCompressorRatio(v: Float) { compressorRatio = v; engine.setEffectParameter(FX_COMPRESSOR, GuitarEngine.PARAM_RATIO, v) }
    fun updateCompressorAttack(v: Float) { compressorAttack = v; engine.setEffectParameter(FX_COMPRESSOR, GuitarEngine.PARAM_ATTACK3, v) }
    fun updateCompressorRelease(v: Float) { compressorRelease = v; engine.setEffectParameter(FX_COMPRESSOR, GuitarEngine.PARAM_RELEASE3, v) }

    fun updateEqBass(v: Float) { eqBass = v; engine.setEffectParameter(FX_EQ, GuitarEngine.PARAM_BASS, v) }
    fun updateEqMid(v: Float) { eqMid = v; engine.setEffectParameter(FX_EQ, GuitarEngine.PARAM_MID, v) }
    fun updateEqTreble(v: Float) { eqTreble = v; engine.setEffectParameter(FX_EQ, GuitarEngine.PARAM_TREBLE, v) }

    fun updateChorusRate(v: Float) { chorusRate = v; engine.setEffectParameter(FX_CHORUS, GuitarEngine.PARAM_RATE, v) }
    fun updateChorusDepth(v: Float) { chorusDepth = v; engine.setEffectParameter(FX_CHORUS, GuitarEngine.PARAM_DEPTH, v) }
    fun updateChorusMix(v: Float) { chorusMix = v; engine.setEffectParameter(FX_CHORUS, GuitarEngine.PARAM_MIX, v) }

    fun updateDelayMix(v: Float) { delayMix = v; engine.setEffectParameter(FX_DELAY, GuitarEngine.PARAM_MIX2, v) }
    fun updateDelayFeedback(v: Float) { delayFeedback = v; engine.setEffectParameter(FX_DELAY, GuitarEngine.PARAM_FEEDBACK, v) }
    fun updateDelayMs(v: Float) { delayMs = v; engine.setEffectParameter(FX_DELAY, GuitarEngine.PARAM_DELAY_MS, v) }

    fun updateReverbRoomSize(v: Float) { reverbRoomSize = v; engine.setEffectParameter(FX_REVERB, GuitarEngine.PARAM_ROOM_SIZE, v) }
    fun updateReverbMix(v: Float) { reverbMix = v; engine.setEffectParameter(FX_REVERB, GuitarEngine.PARAM_MIX3, v) }

    // Tuner operations
    fun loadAudioForTuner(data: FloatArray, numFrames: Int, numChannels: Int) {
        engine.loadAudioForTuner(data, numFrames, numChannels)
        updateTunerState()
    }

    fun setTunerTuning(tuningIndex: Int) {
        tunerCurrentTuning = tuningIndex
    }

    fun updateTunerState() {
        tunerFrequency = engine.getTunerFrequency()
        tunerNoteIndex = engine.getTunerNoteIndex()
        tunerOctave = engine.getTunerOctave()
        tunerCents = engine.getTunerCents()
        isTunerNoteDetected = engine.isTunerNoteDetected()
    }

    // Tone matcher operations
    fun loadAudioForToneMatcher(data: FloatArray, numFrames: Int, numChannels: Int) {
        engine.loadAudioForToneMatcher(data, numFrames, numChannels)
        updateToneMatcherRecommendations()
    }

    fun transcribeAudio(data: FloatArray, sampleRate: Int) {
        val result = engine.transcribeAudio(data, data.size, sampleRate)
        transcribeHasResult = result
        if (result) {
            transcribeNumMeasures = engine.getNumMeasures()
            transcribeProgress = engine.getTranscriptionProgress()
            loadTabNotes()
        }
    }

    private fun loadTabNotes() {
        val numMeasures = engine.getNumMeasures()
        if (numMeasures <= 0) return
        // Estimate max notes (20 per measure)
        val maxNotes = numMeasures * 20
        val strings = IntArray(maxNotes)
        val frets = IntArray(maxNotes)
        val times = FloatArray(maxNotes)
        val durations = FloatArray(maxNotes)
        engine.getTabData(strings, frets, times, durations, maxNotes)
        val notes = mutableListOf<TabNoteData>()
        for (i in 0 until maxNotes) {
            if (strings[i] < 0) break
            notes.add(TabNoteData(strings[i], frets[i], times[i], durations[i]))
        }
        transcribeNotes = notes
    }

    fun updateToneMatcherRecommendations() {
        toneMatcherHasProfile = engine.hasToneMatcherProfile()
        toneMatcherRecommendedDistortionDrive = engine.getRecommendedDistortionDrive()
        toneMatcherRecommendedDistortionTone = engine.getRecommendedDistortionTone()
        toneMatcherRecommendedDistortionLevel = engine.getRecommendedDistortionLevel()
        toneMatcherRecommendedAmpSimGain = engine.getRecommendedAmpSimGain()
        toneMatcherRecommendedAmpSimTone = engine.getRecommendedAmpSimTone()
        toneMatcherRecommendedAmpSimMaster = engine.getRecommendedAmpSimMaster()
        toneMatcherRecommendedEqBass = engine.getRecommendedEqBass()
        toneMatcherRecommendedEqMid = engine.getRecommendedEqMid()
        toneMatcherRecommendedEqTreble = engine.getRecommendedEqTreble()
        toneMatcherRecommendedChorusRate = engine.getRecommendedChorusRate()
        toneMatcherRecommendedChorusDepth = engine.getRecommendedChorusDepth()
        toneMatcherRecommendedChorusMix = engine.getRecommendedChorusMix()
        toneMatcherRecommendedDelayMix = engine.getRecommendedDelayMix()
        toneMatcherRecommendedDelayFeedback = engine.getRecommendedDelayFeedback()
        toneMatcherRecommendedDelayTime = engine.getRecommendedDelayTime()
        toneMatcherRecommendedReverbSize = engine.getRecommendedReverbSize()
        toneMatcherRecommendedReverbMix = engine.getRecommendedReverbMix()
    }

    override fun onCleared() {
        stopTunerPolling()
        if (isRunning) engine.stop()
        super.onCleared()
    }
}
