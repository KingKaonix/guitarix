#include "compressor.h"
#include <cstring>

Compressor::Compressor()
    : threshold_(-20.0f), ratio_(4.0f), attack_(2.0f), release_(50.0f),
      envelope_(0.0f), attackCoeff_(0.0f), releaseCoeff_(0.0f), makeupGain_(1.0f) {
    updateCoeffs();
}

void Compressor::updateCoeffs() {
    float sr = 48000.0f;
    attackCoeff_ = expf(-1.0f / (sr * attack_ / 1000.0f));
    releaseCoeff_ = expf(-1.0f / (sr * release_ / 1000.0f));
    makeupGain_ = 1.0f + (ratio_ - 1.0f) / ratio_ * 0.5f;
}

void Compressor::process(const float* input, float* output, int32_t numFrames, int32_t numChannels) {
    float thresholdLin = powf(10.0f, threshold_ / 20.0f);
    float slope = 1.0f - 1.0f / ratio_;
    for (int i = 0; i < numFrames; ++i) {
        float absVal = fabsf(input[i * numChannels]);
        // Envelope follower
        if (absVal > envelope_) {
            envelope_ = attackCoeff_ * (envelope_ - absVal) + absVal;
        } else {
            envelope_ = releaseCoeff_ * (envelope_ - absVal) + absVal;
        }
        // Compute gain reduction
        float reduction = 1.0f;
        if (envelope_ > thresholdLin) {
            float envDb = 20.0f * log10f(envelope_);
            float targetDb = threshold_ + (envDb - threshold_) / ratio_;
            reduction = powf(10.0f, (targetDb - envDb) / 20.0f);
        }
        float gain = reduction * makeupGain_;
        for (int c = 0; c < numChannels; ++c) {
            output[i * numChannels + c] = input[i * numChannels + c] * gain;
        }
    }
}

void Compressor::setParameter(int id, float value) {
    switch (id) {
        case PARAM_THRESHOLD: threshold_ = -60.0f + value * 60.0f; break;
        case PARAM_RATIO:     ratio_ = 1.0f + value * 19.0f; updateCoeffs(); break;
        case PARAM_ATTACK:    attack_ = 0.1f + value * 30.0f; updateCoeffs(); break;
        case PARAM_RELEASE:   release_ = 5.0f + value * 300.0f; updateCoeffs(); break;
    }
}

float Compressor::getParameter(int id) {
    switch (id) {
        case PARAM_THRESHOLD: return (threshold_ + 60.0f) / 60.0f;
        case PARAM_RATIO:     return (ratio_ - 1.0f) / 19.0f;
        case PARAM_ATTACK:    return (attack_ - 0.1f) / 30.0f;
        case PARAM_RELEASE:   return (release_ - 5.0f) / 300.0f;
        default: return 0.5f;
    }
}

void Compressor::reset() { envelope_ = 0.0f; }
