#ifndef GUITARIX_COMPRESSOR_H
#define GUITARIX_COMPRESSOR_H

#include "effect.h"
#include <cmath>

class Compressor : public Effect {
public:
    Compressor();
    ~Compressor() override = default;

    void process(const float* input, float* output, int32_t numFrames, int32_t numChannels) override;
    void setParameter(int id, float value) override;
    float getParameter(int id) override;
    void reset() override;
    int getParameterCount() const override { return 4; }

    enum Params { PARAM_THRESHOLD = 0, PARAM_RATIO, PARAM_ATTACK, PARAM_RELEASE };

private:
    float threshold_;   // -60 to 0 dB
    float ratio_;       // 1:1 to 20:1
    float attack_;      // ms
    float release_;     // ms
    float envelope_;
    float attackCoeff_;
    float releaseCoeff_;
    float makeupGain_;
    void updateCoeffs();
};
#endif
