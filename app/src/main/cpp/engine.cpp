#include "engine.h"
#include "effects/distortion.h"
#include "effects/delay.h"
#include "effects/reverb.h"
#include "effects/eq.h"
#include "effects/ampsim.h"
#include "effects/chorus.h"
#include "effects/noise_gate.h"
#include "effects/compressor.h"
#include <oboe/Oboe.h>
#include <algorithm>
#include <cstring>
#include <cmath>
#include <android/log.h>
#include <cstdio>

#define LOG_TAG "GuitarixEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// WAV file writing helper
static bool writeWavHeader(FILE* f, int sampleRate, int numChannels, int bitsPerSample) {
    int dataSize = 0;
    int fileSize = 36 + dataSize;
    fwrite("RIFF", 1, 4, f);
    fwrite(&fileSize, 4, 1, f);
    fwrite("WAVE", 1, 4, f);
    fwrite("fmt ", 1, 4, f);
    int fmtSize = 16;
    short audioFmt = 3; // IEEE float
    short ch = (short)numChannels;
    int sr = sampleRate;
    short bps = (short)(numChannels * bitsPerSample / 8);
    int br = sr * bps;
    short ba = (short)(numChannels * bitsPerSample / 8);
    fwrite(&fmtSize, 4, 1, f);
    fwrite(&audioFmt, 2, 1, f);
    fwrite(&ch, 2, 1, f);
    fwrite(&sr, 4, 1, f);
    fwrite(&br, 4, 1, f);
    fwrite(&ba, 2, 1, f);
    fwrite(&bps, 2, 1, f);
    fwrite("data", 1, 4, f);
    fwrite(&dataSize, 4, 1, f);
    fflush(f);
    return true;
}

static bool finalizeWav(FILE* f) {
    if (!f) return false;
    int fileSize = (int)ftell(f);
    fseek(f, 4, SEEK_SET);
    int dataSize = fileSize - 44;
    int totalSize = fileSize - 8;
    fwrite(&totalSize, 4, 1, f);
    fseek(f, 40, SEEK_SET);
    fwrite(&dataSize, 4, 1, f);
    fclose(f);
    return true;
}

// Simple convolution for IR
static void applyConvolution(float* buffer, int numFrames, const float* ir, int irLen,
                              std::vector<float>& convBuf) {
    if (irLen == 0) return;
    int convLen = numFrames + irLen - 1;
    if ((int)convBuf.size() < convLen) convBuf.resize(convLen, 0.0f);
    // Convolve
    for (int n = 0; n < numFrames; ++n) {
        for (int k = 0; k < irLen; ++k) {
            convBuf[n + k] += buffer[n] * ir[k];
        }
    }
    // Copy output (first numFrames samples)
    for (int n = 0; n < numFrames && n < (int)convBuf.size(); ++n) {
        buffer[n] = convBuf[n];
    }
    // Shift remaining for next callback
    int remaining = convLen - numFrames;
    if (remaining > 0) {
        for (int i = 0; i < remaining; ++i) {
            convBuf[i] = convBuf[numFrames + i];
        }
        convBuf.resize(remaining);
    } else {
        convBuf.clear();
    }
}

enum EffectIndex {
    FX_DISTORTION = 0,
    FX_AMP_SIM,
    FX_EQ,
    FX_CHORUS,
    FX_NOISE_GATE,
    FX_COMPRESSOR,
    FX_DELAY,
    FX_REVERB,
    FX_COUNT
};

AudioEngine::AudioEngine()
    : currentPreset_(0), ringBuffer_(RING_BUFFER_CAPACITY, 0.0f),
      ringBufferWritePos_(0), ringBufferReadPos_(0) {
    effects_.resize(FX_COUNT);
    enabled_.resize(FX_COUNT, false);
    initEffects();
    tuner_ = std::make_unique<Tuner>();
    toneMatcher_ = std::make_unique<ToneMatcher>();
}

AudioEngine::~AudioEngine() {
    stop();
}

void AudioEngine::initEffects() {
    effects_[FX_DISTORTION] = std::make_unique<Distortion>();
    effects_[FX_AMP_SIM]    = std::make_unique<AmpSim>();
    effects_[FX_EQ]         = std::make_unique<EQ>();
    effects_[FX_CHORUS]     = std::make_unique<Chorus>();
    effects_[FX_DELAY]      = std::make_unique<Delay>();
    effects_[FX_REVERB]     = std::make_unique<Reverb>();

    enabled_[FX_NOISE_GATE] = true;
    enabled_[FX_COMPRESSOR] = true;
    enabled_[FX_AMP_SIM] = true;
}

bool AudioEngine::start() {
    if (outputStream_) return true;

    oboe::AudioStreamBuilder outBuilder;
    outBuilder.setDirection(oboe::Direction::Output);
    outBuilder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    outBuilder.setSharingMode(oboe::SharingMode::Exclusive);
    outBuilder.setFormat(oboe::AudioFormat::Float);
    outBuilder.setChannelCount(oboe::ChannelCount::Mono);
    outBuilder.setSampleRate(48000);
    outBuilder.setFramesPerDataCallback(256);
    outBuilder.setDataCallback(this);
    outBuilder.setErrorCallback(this);

    oboe::Result result = outBuilder.openStream(outputStream_);
    if (result != oboe::Result::OK) {
        LOGI("Failed to open output stream: %s", oboe::convertToText(result));
        return false;
    }

    // Try to open input stream
    oboe::AudioStreamBuilder inBuilder;
    inBuilder.setDirection(oboe::Direction::Input);
    inBuilder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    inBuilder.setSharingMode(oboe::SharingMode::Exclusive);
    inBuilder.setFormat(oboe::AudioFormat::Float);
    inBuilder.setChannelCount(oboe::ChannelCount::Mono);
    inBuilder.setSampleRate(48000);
    inBuilder.setFramesPerDataCallback(256);
    auto inputCb = std::make_shared<InputCallback>(this);
    inBuilder.setDataCallback(inputCb);
    inBuilder.setErrorCallback(this);

    oboe::Result inResult = inBuilder.openStream(inputStream_);
    if (inResult != oboe::Result::OK) {
        LOGI("Input stream not available (tuner will not get live audio): %s",
             oboe::convertToText(inResult));
        inputStream_.reset();
    } else {
        inResult = inputStream_->requestStart();
        if (inResult != oboe::Result::OK) {
            LOGI("Failed to start input stream: %s", oboe::convertToText(inResult));
            inputStream_.reset();
        }
    }

    result = outputStream_->requestStart();
    if (result != oboe::Result::OK) {
        LOGI("Failed to start output stream: %s", oboe::convertToText(result));
        outputStream_.reset();
        return false;
    }

    LOGI("Audio engine started: output %dch %dHz, input %s",
         outputStream_->getChannelCount(), outputStream_->getSampleRate(),
         inputStream_ ? "active" : "unavailable");
    return true;
}

void AudioEngine::stop() {
    if (inputStream_) {
        inputStream_->stop();
        inputStream_->close();
        inputStream_.reset();
    }
    if (outputStream_) {
        outputStream_->stop();
        outputStream_->close();
        outputStream_.reset();
    }
}

oboe::DataCallbackResult AudioEngine::onAudioReady(
    oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {

    int32_t numChannels = audioStream->getChannelCount();
    float *buffer = static_cast<float*>(audioData);

    // Default to silence
    memset(buffer, 0, numFrames * numChannels * sizeof(float));

    std::lock_guard<std::mutex> lock(mutex_);

    // Read input audio from ring buffer (if available)
    int samplesAvailable = 0;
    if (ringBufferWritePos_ > ringBufferReadPos_) {
        samplesAvailable = ringBufferWritePos_ - ringBufferReadPos_;
        int toCopy = (samplesAvailable < numFrames) ? samplesAvailable : numFrames;
        for (int i = 0; i < toCopy; ++i) {
            buffer[i] = ringBuffer_[(ringBufferReadPos_ + i) % RING_BUFFER_CAPACITY];
        }
        ringBufferReadPos_ += toCopy;
    }

    // If we have input audio, process through tuner
    if (samplesAvailable > 0 && tuner_) {
        tuner_->process(buffer, numFrames);
    }

    // Process effects chain on output
    buildSignalChain(buffer, numFrames, numChannels);

    // Apply IR convolution if loaded
    if (irLoaded_ && irLength_ > 0) {
        applyConvolution(buffer, numFrames, irData_.data(), irLength_, convBuffer_);
    }

    // Record if active
    if (recording_ && recordingFile_) {
        fwrite(buffer, sizeof(float), numFrames * numChannels, recordingFile_);
    }

    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::onErrorAfterClose(oboe::AudioStream *oboeStream, oboe::Result error) {
    LOGI("Audio stream error: %s", oboe::convertToText(error));
}

void AudioEngine::processInput(float* buffer, int32_t numFrames) {
    std::lock_guard<std::mutex> lock(mutex_);
    for (int i = 0; i < numFrames; ++i) {
        int pos = (ringBufferWritePos_ + i) % RING_BUFFER_CAPACITY;
        ringBuffer_[pos] = buffer[i];
    }
    ringBufferWritePos_ += numFrames;
    // Prevent overflow
    if (ringBufferWritePos_ - ringBufferReadPos_ > RING_BUFFER_CAPACITY) {
        ringBufferReadPos_ = ringBufferWritePos_ - RING_BUFFER_CAPACITY;
    }
}

void AudioEngine::buildSignalChain(float* buffer, int32_t numFrames, int32_t numChannels) {
    for (int i = 0; i < FX_COUNT; ++i) {
        if (enabled_[i] && effects_[i]) {
            effects_[i]->process(buffer, buffer, numFrames, numChannels);
        }
    }
}

void AudioEngine::setEffectEnabled(int index, bool enabled) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (index >= 0 && index < FX_COUNT) {
        enabled_[index] = enabled;
        if (!enabled && effects_[index]) {
            effects_[index]->reset();
        }
    }
}

bool AudioEngine::isEffectEnabled(int index) {
    if (index >= 0 && index < FX_COUNT) return enabled_[index];
    return false;
}

void AudioEngine::setEffectParameter(int index, int paramId, float value) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (index >= 0 && index < FX_COUNT && effects_[index]) {
        effects_[index]->setParameter(paramId, value);
    }
}

float AudioEngine::getEffectParameter(int index, int paramId) {
    if (index >= 0 && index < FX_COUNT && effects_[index]) {
        return effects_[index]->getParameter(paramId);
    }
    return 0.0f;
}

void AudioEngine::loadPreset(int preset) {
    std::lock_guard<std::mutex> lock(mutex_);
    currentPreset_ = preset;

    for (auto& effect : effects_) {
        if (effect) effect->reset();
    }

    switch (preset) {
        case 0: // Clean
            enabled_[FX_DISTORTION] = false;
            enabled_[FX_NOISE_GATE] = true;
    enabled_[FX_COMPRESSOR] = true;
    enabled_[FX_AMP_SIM] = true;
            enabled_[FX_EQ] = false;
            enabled_[FX_CHORUS] = false;
            enabled_[FX_DELAY] = true;
            enabled_[FX_REVERB] = true;
            effects_[FX_DELAY]->setParameter(0, 0.25f);
            effects_[FX_REVERB]->setParameter(0, 0.3f);
            effects_[FX_REVERB]->setParameter(1, 0.2f);
            effects_[FX_DELAY]->setParameter(1, 0.3f);
            effects_[FX_DELAY]->setParameter(2, 400.0f);
            effects_[FX_AMP_SIM]->setParameter(0, 0.3f);
            effects_[FX_AMP_SIM]->setParameter(1, 0.6f);
            break;
        case 1: // Crunch
            enabled_[FX_DISTORTION] = true;
            enabled_[FX_NOISE_GATE] = true;
    enabled_[FX_COMPRESSOR] = true;
    enabled_[FX_AMP_SIM] = true;
            enabled_[FX_EQ] = true;
            enabled_[FX_CHORUS] = false;
            enabled_[FX_DELAY] = false;
            enabled_[FX_REVERB] = true;
            effects_[FX_DISTORTION]->setParameter(0, 0.3f);
            effects_[FX_DISTORTION]->setParameter(1, 0.5f);
            effects_[FX_DISTORTION]->setParameter(2, 0.7f);
            effects_[FX_AMP_SIM]->setParameter(0, 0.5f);
            effects_[FX_AMP_SIM]->setParameter(1, 0.7f);
            effects_[FX_AMP_SIM]->setParameter(2, 0.5f);
            effects_[FX_EQ]->setParameter(0, 0.5f);
            effects_[FX_EQ]->setParameter(1, 0.4f);
            effects_[FX_EQ]->setParameter(2, 0.6f);
            effects_[FX_REVERB]->setParameter(0, 0.2f);
            effects_[FX_REVERB]->setParameter(1, 0.15f);
            break;
        case 2: // Lead
            enabled_[FX_DISTORTION] = true;
            enabled_[FX_NOISE_GATE] = true;
    enabled_[FX_COMPRESSOR] = true;
    enabled_[FX_AMP_SIM] = true;
            enabled_[FX_EQ] = true;
            enabled_[FX_CHORUS] = false;
            enabled_[FX_DELAY] = true;
            enabled_[FX_REVERB] = true;
            effects_[FX_DISTORTION]->setParameter(0, 0.7f);
            effects_[FX_DISTORTION]->setParameter(1, 0.6f);
            effects_[FX_DISTORTION]->setParameter(2, 0.85f);
            effects_[FX_AMP_SIM]->setParameter(0, 0.7f);
            effects_[FX_AMP_SIM]->setParameter(1, 0.5f);
            effects_[FX_EQ]->setParameter(0, 0.4f);
            effects_[FX_EQ]->setParameter(1, 0.3f);
            effects_[FX_EQ]->setParameter(2, 0.7f);
            effects_[FX_DELAY]->setParameter(0, 0.35f);
            effects_[FX_DELAY]->setParameter(1, 0.4f);
            effects_[FX_DELAY]->setParameter(2, 450.0f);
            effects_[FX_REVERB]->setParameter(0, 0.3f);
            effects_[FX_REVERB]->setParameter(1, 0.25f);
            break;
        case 3: // Metal
            enabled_[FX_DISTORTION] = true;
            enabled_[FX_NOISE_GATE] = true;
    enabled_[FX_COMPRESSOR] = true;
    enabled_[FX_AMP_SIM] = true;
            enabled_[FX_EQ] = true;
            enabled_[FX_CHORUS] = false;
            enabled_[FX_DELAY] = false;
            enabled_[FX_REVERB] = false;
            effects_[FX_NOISE_GATE]->setParameter(0, 0.75f);
            effects_[FX_NOISE_GATE]->setParameter(1, 0.5f);
            effects_[FX_NOISE_GATE]->setParameter(2, 0.5f);
            effects_[FX_COMPRESSOR]->setParameter(0, 0.67f);
            effects_[FX_COMPRESSOR]->setParameter(1, 0.16f);
            effects_[FX_COMPRESSOR]->setParameter(2, 0.5f);
            effects_[FX_COMPRESSOR]->setParameter(3, 0.5f);
            effects_[FX_DISTORTION]->setParameter(0, 0.95f);
            effects_[FX_DISTORTION]->setParameter(1, 0.4f);
            effects_[FX_DISTORTION]->setParameter(2, 0.9f);
            effects_[FX_AMP_SIM]->setParameter(0, 1.0f);
            effects_[FX_AMP_SIM]->setParameter(1, 0.4f);
            effects_[FX_EQ]->setParameter(0, 0.7f);
            effects_[FX_EQ]->setParameter(1, 0.2f);
            effects_[FX_EQ]->setParameter(2, 0.6f);
            break;
        case 4: // Ambient
            enabled_[FX_DISTORTION] = false;
            enabled_[FX_NOISE_GATE] = true;
    enabled_[FX_COMPRESSOR] = true;
    enabled_[FX_AMP_SIM] = true;
            enabled_[FX_EQ] = true;
            enabled_[FX_CHORUS] = true;
            enabled_[FX_DELAY] = true;
            enabled_[FX_REVERB] = true;
            effects_[FX_CHORUS]->setParameter(0, 0.5f);
            effects_[FX_CHORUS]->setParameter(1, 0.4f);
            effects_[FX_CHORUS]->setParameter(2, 0.4f);
            effects_[FX_DELAY]->setParameter(0, 0.4f);
            effects_[FX_DELAY]->setParameter(1, 0.3f);
            effects_[FX_DELAY]->setParameter(2, 600.0f);
            effects_[FX_REVERB]->setParameter(0, 0.8f);
            effects_[FX_REVERB]->setParameter(1, 0.5f);
            effects_[FX_AMP_SIM]->setParameter(0, 0.3f);
            effects_[FX_AMP_SIM]->setParameter(1, 0.8f);
            effects_[FX_EQ]->setParameter(0, 0.3f);
            effects_[FX_EQ]->setParameter(1, 0.5f);
            effects_[FX_EQ]->setParameter(2, 0.6f);
            break;
    }
}

// Recording implementation
bool AudioEngine::startRecording(const char* path) {
    if (recording_) return false;
    recordingFile_ = fopen(path, "wb");
    if (!recordingFile_) return false;
    writeWavHeader(recordingFile_, recordingSampleRate_, 1, 32);
    recording_ = true;
    LOGI("Recording started: %s", path);
    return true;
}

void AudioEngine::stopRecording() {
    if (!recording_) return;
    recording_ = false;
    if (recordingFile_) {
        finalizeWav(recordingFile_);
        recordingFile_ = nullptr;
    }
    LOGI("Recording stopped");
}

bool AudioEngine::loadImpulseResponse(const char* path) {
    FILE* f = fopen(path, "rb");
    if (!f) { irLoaded_ = false; return false; }
    char riff[4]; fread(riff, 1, 4, f); if (riff[0]!='R'||riff[1]!='I'||riff[2]!='F'||riff[3]!='F') { fclose(f); return false; }
    fseek(f, 40, SEEK_SET); // Go to data chunk size
    int dataSize; fread(&dataSize, 4, 1, f);
    int numSamples = dataSize / 4; // Assuming 32-bit float
    std::vector<float> temp(numSamples);
    fread(temp.data(), 4, numSamples, f);
    fclose(f);
    irData_ = std::move(temp);
    irLength_ = (int)irData_.size();
    irLoaded_ = true;
    convBuffer_.clear();
    LOGI("IR loaded: %d samples", irLength_);
    return true;
}

// Tuner interface implementations
void AudioEngine::loadAudioForTuner(const float* data, int32_t numFrames, int32_t numChannels) {
    if (tuner_) {
        tuner_->process(data, numFrames);
    }
}

float AudioEngine::getTunerFrequency() const {
    return tuner_ ? tuner_->getFrequency() : 0.0f;
}

int AudioEngine::getTunerNoteIndex() const {
    return tuner_ ? tuner_->getNoteIndex() : -1;
}

int AudioEngine::getTunerOctave() const {
    return tuner_ ? tuner_->getOctave() : 0;
}

float AudioEngine::getTunerCents() const {
    return tuner_ ? tuner_->getCents() : 0.0f;
}

bool AudioEngine::isTunerNoteDetected() const {
    return tuner_ ? tuner_->isNoteDetected() : false;
}

const char* AudioEngine::getTunerCurrentTuningName() const {
    return tuner_ ? tuner_->getTuningName() : "None";
}

const char* AudioEngine::getTunerNoteName(int index) const {
    return Tuner::noteName(index);
}

// Tone matcher interface implementations
void AudioEngine::loadAudioForToneMatcher(const float* data, int32_t numFrames, int32_t numChannels) {
    if (toneMatcher_) {
        toneMatcher_->loadSample(data, numFrames);
    }
}

bool AudioEngine::hasToneMatcherProfile() const {
    return toneMatcher_ ? toneMatcher_->hasProfile() : false;
}

float AudioEngine::getRecommendedDistortionDrive() const { return toneMatcher_ ? toneMatcher_->getRecommendedDistortionDrive() : 0.5f; }
float AudioEngine::getRecommendedDistortionTone() const { return toneMatcher_ ? toneMatcher_->getRecommendedDistortionTone() : 0.5f; }
float AudioEngine::getRecommendedDistortionLevel() const { return toneMatcher_ ? toneMatcher_->getRecommendedDistortionLevel() : 0.5f; }
float AudioEngine::getRecommendedAmpSimGain() const { return toneMatcher_ ? toneMatcher_->getRecommendedAmpSimGain() : 0.5f; }
float AudioEngine::getRecommendedAmpSimTone() const { return toneMatcher_ ? toneMatcher_->getRecommendedAmpSimTone() : 0.5f; }
float AudioEngine::getRecommendedAmpSimMaster() const { return toneMatcher_ ? toneMatcher_->getRecommendedAmpSimMaster() : 0.5f; }
float AudioEngine::getRecommendedEqBass() const { return toneMatcher_ ? toneMatcher_->getRecommendedEqBass() : 0.5f; }
float AudioEngine::getRecommendedEqMid() const { return toneMatcher_ ? toneMatcher_->getRecommendedEqMid() : 0.5f; }
float AudioEngine::getRecommendedEqTreble() const { return toneMatcher_ ? toneMatcher_->getRecommendedEqTreble() : 0.5f; }
float AudioEngine::getRecommendedChorusRate() const { return toneMatcher_ ? toneMatcher_->getRecommendedChorusRate() : 0.5f; }
float AudioEngine::getRecommendedChorusDepth() const { return toneMatcher_ ? toneMatcher_->getRecommendedChorusDepth() : 0.3f; }
float AudioEngine::getRecommendedChorusMix() const { return toneMatcher_ ? toneMatcher_->getRecommendedChorusMix() : 0.3f; }
float AudioEngine::getRecommendedDelayMix() const { return toneMatcher_ ? toneMatcher_->getRecommendedDelayMix() : 0.3f; }
float AudioEngine::getRecommendedDelayFeedback() const { return toneMatcher_ ? toneMatcher_->getRecommendedDelayFeedback() : 0.3f; }
float AudioEngine::getRecommendedDelayTime() const { return toneMatcher_ ? toneMatcher_->getRecommendedDelayTime() : 400.0f; }
float AudioEngine::getRecommendedReverbSize() const { return toneMatcher_ ? toneMatcher_->getRecommendedReverbSize() : 0.3f; }
float AudioEngine::getRecommendedReverbMix() const { return toneMatcher_ ? toneMatcher_->getRecommendedReverbMix() : 0.2f; }
