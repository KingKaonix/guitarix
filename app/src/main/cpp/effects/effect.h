#ifndef GUITARIX_EFFECTS_H
#define GUITARIX_EFFECTS_H

#include <cstdint>

class Effect {
public:
    virtual ~Effect() = default;
    virtual void process(const float* input, float* output, int32_t numFrames, int32_t numChannels) = 0;
    virtual void setParameter(int id, float value) = 0;
    virtual float getParameter(int id) = 0;
    virtual void reset() = 0;
    virtual int getParameterCount() const { return 0; }
};

#endif