#include <jni.h>
#include "engine.h"
#include <vector>

static AudioEngine* getEngine(jlong ptr) {
    return reinterpret_cast<AudioEngine*>(static_cast<intptr_t>(ptr));
}

extern "C" {

// Existing JNI methods (copied from original)
JNIEXPORT jlong JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeCreate(JNIEnv*, jobject) {
    auto* engine = new AudioEngine();
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeDestroy(JNIEnv*, jobject, jlong ptr) {
    delete getEngine(ptr);
}

JNIEXPORT jboolean JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeStart(JNIEnv*, jobject, jlong ptr) {
    return getEngine(ptr)->start() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeStop(JNIEnv*, jobject, jlong ptr) {
    getEngine(ptr)->stop();
}

JNIEXPORT void JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeSetEffectEnabled(JNIEnv*, jobject,
    jlong ptr, jint index, jboolean enabled) {
    getEngine(ptr)->setEffectEnabled(index, enabled);
}

JNIEXPORT jboolean JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeIsEffectEnabled(JNIEnv*, jobject,
    jlong ptr, jint index) {
    return getEngine(ptr)->isEffectEnabled(index) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeSetEffectParameter(JNIEnv*, jobject,
    jlong ptr, jint index, jint paramId, jfloat value) {
    getEngine(ptr)->setEffectParameter(index, paramId, value);
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetEffectParameter(JNIEnv*, jobject,
    jlong ptr, jint index, jint paramId) {
    return getEngine(ptr)->getEffectParameter(index, paramId);
}

JNIEXPORT void JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeLoadPreset(JNIEnv*, jobject,
    jlong ptr, jint preset) {
    getEngine(ptr)->loadPreset(preset);
}

// Tuner JNI methods
JNIEXPORT void JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeLoadAudioForTuner(JNIEnv* env, jobject,
    jlong ptr, jfloatArray data, jint numFrames, jint numChannels) {
    auto* engine = getEngine(ptr);
    if (!engine || !data) return;

    jint len = env->GetArrayLength(data);
    jfloat* arr = env->GetFloatArrayElements(data, nullptr);

    std::vector<float> buffer(arr, arr + len);
    env->ReleaseFloatArrayElements(data, arr, 0); // JNI_ABORT

    engine->loadAudioForTuner(buffer.data(), numFrames, numChannels);
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetTunerFrequency(JNIEnv*, jobject,
    jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getTunerFrequency() : 0.0f;
}

JNIEXPORT jint JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetTunerNoteIndex(JNIEnv*, jobject,
    jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getTunerNoteIndex() : -1;
}

JNIEXPORT jint JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetTunerOctave(JNIEnv*, jobject,
    jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getTunerOctave() : 0;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetTunerCents(JNIEnv*, jobject,
    jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getTunerCents() : 0.0f;
}

JNIEXPORT jboolean JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeIsTunerNoteDetected(JNIEnv*, jobject,
    jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->isTunerNoteDetected() : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetTunerCurrentTuningName(JNIEnv* env, jobject,
    jlong ptr) {
    auto* engine = getEngine(ptr);
    if (!engine) return nullptr;
    const char* name = engine->getTunerCurrentTuningName();
    return env->NewStringUTF(name);
}

JNIEXPORT jstring JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetTunerNoteName(JNIEnv* env, jobject,
    jlong ptr, jint index) {
    auto* engine = getEngine(ptr);
    if (!engine) return nullptr;
    const char* noteName = engine->getTunerNoteName(index);
    return env->NewStringUTF(noteName);
}

// Tone matcher JNI methods
JNIEXPORT void JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeLoadAudioForToneMatcher(JNIEnv* env, jobject,
    jlong ptr, jfloatArray data, jint numFrames, jint numChannels) {
    auto* engine = getEngine(ptr);
    if (!engine || !data) return;

    jint len = env->GetArrayLength(data);
    jfloat* arr = env->GetFloatArrayElements(data, nullptr);

    std::vector<float> buffer(arr, arr + len);
    env->ReleaseFloatArrayElements(data, arr, 0);

    engine->loadAudioForToneMatcher(buffer.data(), numFrames, numChannels);
}

JNIEXPORT jboolean JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeHasToneMatcherProfile(JNIEnv*, jobject,
    jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->hasToneMatcherProfile() : JNI_FALSE;
}

// All other nativeGetRecommended* functions...
JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedDistortionDrive(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedDistortionDrive() : 0.5f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedDistortionTone(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedDistortionTone() : 0.5f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedDistortionLevel(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedDistortionLevel() : 0.5f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedAmpSimGain(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedAmpSimGain() : 0.5f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedAmpSimTone(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedAmpSimTone() : 0.5f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedAmpSimMaster(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedAmpSimMaster() : 0.5f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedEqBass(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedEqBass() : 0.5f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedEqMid(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedEqMid() : 0.5f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedEqTreble(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedEqTreble() : 0.5f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedChorusRate(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedChorusRate() : 0.5f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedChorusDepth(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedChorusDepth() : 0.3f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedChorusMix(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedChorusMix() : 0.3f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedDelayMix(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedDelayMix() : 0.3f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedDelayFeedback(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedDelayFeedback() : 0.3f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedDelayTime(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedDelayTime() : 400.0f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedReverbSize(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedReverbSize() : 0.3f;
}

JNIEXPORT jfloat JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeGetRecommendedReverbMix(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? engine->getRecommendedReverbMix() : 0.2f;
}

// Recording JNI
JNIEXPORT void JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeStartRecording(JNIEnv* env, jobject,
    jlong ptr, jstring filePath) {
    auto* engine = getEngine(ptr);
    if (!engine) return;
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    engine->startRecording(path);
    env->ReleaseStringUTFChars(filePath, path);
}

JNIEXPORT void JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeStopRecording(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    if (engine) engine->stopRecording();
}

JNIEXPORT jboolean JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeIsRecording(JNIEnv*, jobject, jlong ptr) {
    auto* engine = getEngine(ptr);
    return engine ? (engine->isRecording() ? JNI_TRUE : JNI_FALSE) : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_kaonixx_guitarix_GuitarEngine_nativeLoadImpulseResponse(JNIEnv* env, jobject,
    jlong ptr, jstring path) {
    auto* engine = getEngine(ptr);
    if (!engine) return JNI_FALSE;
    const char* cpath = env->GetStringUTFChars(path, nullptr);
    bool result = engine->loadImpulseResponse(cpath);
    env->ReleaseStringUTFChars(path, cpath);
    return result ? JNI_TRUE : JNI_FALSE;
}

}  // extern "C"
