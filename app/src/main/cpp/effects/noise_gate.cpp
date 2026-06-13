#include "noise_gate.h"
#include <cstring>

NoiseGate::NoiseGate()
    : threshold_(-60.0f), attack_(1.0f), release_(20.0f),
      envelope_(0.0f), attackCoeff_(0.0f), releaseCoeff_(0.0f) {
    updateCoeffs();
}

void NoiseGate::updateCoeffs() {
    float sr = 48000.0f;
    attackCoeff_ = expf(-1.0f / (sr * attack_ / 1000.0f));
    releaseCoeff_ = expf(-1.0f / (sr * release_ / 1000.0f));
}

void NoiseGate::process(const float* input, float* output, int32_t numFrames, int32_t numChannels) {
    float thresholdLin = powf(10.0f, threshold_ / 20.0f);
    for (int i = 0; i < numFrames; ++i) {
        float absVal = fabsf(input[i * numChannels]);
        // Envelope follower
        if (absVal > envelope_) {
            envelope_ = attackCoeff_ * (envelope_ - absVal) + absVal;
        } else {
            envelope_ = releaseCoeff_ * (envelope_ - absVal) + absVal;
        }
        float gain = (envelope_ > thresholdLin) ? 1.0f : 0.0f;
        for (int c = 0; c < numChannels; ++c) {
            output[i * numChannels + c] = input[i * numChannels + c] * gain;
        }
    }
}

void NoiseGate::setParameter(int id, float value) {
    switch (id) {
        case PARAM_THRESHOLD: threshold_ = -80.0f + value * 80.0f; break;
        case PARAM_ATTACK:    attack_ = 0.1f + value * 20.0f; updateCoeffs(); break;
        case PARAM_RELEASE:   release_ = 1.0f + value * 200.0f; updateCoeffs(); break;
    }
}

float NoiseGate::getParameter(int id) {
    switch (id) {
        case PARAM_THRESHOLD: return (threshold_ + 80.0f) / 80.0f;
        case PARAM_ATTACK:    return (attack_ - 0.1f) / 20.0f;
        case PARAM_RELEASE:   return (release_ - 1.0f) / 200.0f;
        default: return 0.5f;
    }
}

void NoiseGate::reset() { envelope_ = 0.0f; }
