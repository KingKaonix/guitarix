#ifndef GUITARIX_ENGINE_H
#define GUITARIX_ENGINE_H

#include <oboe/Oboe.h>
#include <memory>
#include <vector>
#include <mutex>
#include "effects/effect.h"
#include "effects/tuner.h"
#include "effects/tone_matcher.h"

class AudioEngine : public oboe::AudioStreamDataCallback,
                    public oboe::AudioStreamErrorCallback {
public:
    AudioEngine();
    ~AudioEngine();

    bool start();
    void stop();

    // Effect chain control
    void setEffectEnabled(int index, bool enabled);
    bool isEffectEnabled(int index);
    void setEffectParameter(int index, int paramId, float value);
    float getEffectParameter(int index, int paramId);

    // Preset management
    void loadPreset(int preset);
    int currentPreset() const { return currentPreset_; }

    // Tuner interface
    void loadAudioForTuner(const float* data, int32_t numFrames, int32_t numChannels);
    float getTunerFrequency() const;
    int getTunerNoteIndex() const;
    int getTunerOctave() const;
    float getTunerCents() const;
    bool isTunerNoteDetected() const;
    const char* getTunerCurrentTuningName() const;
    const char* getTunerNoteName(int index) const;

    // Tone matcher interface
    void loadAudioForToneMatcher(const float* data, int32_t numFrames, int32_t numChannels);
    bool hasToneMatcherProfile() const;
    float getRecommendedDistortionDrive() const;
    float getRecommendedDistortionTone() const;
    float getRecommendedDistortionLevel() const;
    float getRecommendedAmpSimGain() const;
    float getRecommendedAmpSimTone() const;
    float getRecommendedAmpSimMaster() const;
    float getRecommendedEqBass() const;
    float getRecommendedEqMid() const;
    float getRecommendedEqTreble() const;
    float getRecommendedChorusRate() const;
    float getRecommendedChorusDepth() const;
    float getRecommendedChorusMix() const;
    float getRecommendedDelayMix() const;
    float getRecommendedDelayFeedback() const;
    float getRecommendedDelayTime() const;
    float getRecommendedReverbSize() const;
    float getRecommendedReverbMix() const;

    // oboe::AudioStreamDataCallback
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream *audioStream,
        void *audioData,
        int32_t numFrames) override;

    // oboe::AudioStreamErrorCallback
    void onErrorAfterClose(oboe::AudioStream *oboeStream, oboe::Result error) override;

private:
    std::shared_ptr<oboe::AudioStream> inputStream_;
    std::shared_ptr<oboe::AudioStream> outputStream_;
    std::vector<float> ringBuffer_;
    int ringBufferWritePos_ = 0;
    int ringBufferReadPos_ = 0;
    std::vector<std::unique_ptr<Effect>> effects_;
    std::vector<bool> enabled_;
    int currentPreset_ = 0;

    // New audio analysis components
    std::unique_ptr<Tuner> tuner_;
    std::unique_ptr<ToneMatcher> toneMatcher_;

    std::mutex mutex_;

    void initEffects();
    void buildSignalChain(float* buffer, int32_t numFrames, int32_t numChannels);
    void processInput(float* buffer, int32_t numFrames);
    static constexpr int RING_BUFFER_CAPACITY = 16384; // ~85ms at 48kHz
};

// Separate callback class for input stream
class InputCallback : public oboe::AudioStreamDataCallback {
public:
    explicit InputCallback(AudioEngine* engine) : engine_(engine) {}
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* stream, void* audioData, int32_t numFrames) override {
        engine_->processInput(static_cast<float*>(audioData), numFrames);
        return oboe::DataCallbackResult::Continue;
    }
private:
    AudioEngine* engine_;
};

#endif
