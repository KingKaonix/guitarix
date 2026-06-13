#ifndef GUITARIX_NOISE_GATE_H
#define GUITARIX_NOISE_GATE_H

#include "effect.h"
#include <cmath>

class NoiseGate : public Effect {
public:
    NoiseGate();
    ~NoiseGate() override = default;

    void process(const float* input, float* output, int32_t numFrames, int32_t numChannels) override;
    void setParameter(int id, float value) override;
    float getParameter(int id) override;
    void reset() override;
    int getParameterCount() const override { return 3; }

    enum Params { PARAM_THRESHOLD = 0, PARAM_ATTACK, PARAM_RELEASE };

private:
    float threshold_;   // -80 to 0 dB
    float attack_;      // ms
    float release_;     // ms
    float envelope_;
    float attackCoeff_;
    float releaseCoeff_;
    void updateCoeffs();
};
#endif
