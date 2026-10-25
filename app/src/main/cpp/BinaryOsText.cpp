#include "BinaryOsCore.h"

extern JavaVM* gVm;
extern jobject gActivity;
extern jmethodID gPushText;
extern jmethodID gRequestKb;
extern jmethodID gShutdown;

void BinaryOsCore::pushTextToJava() {
    std::vector<TextCommand> pending;
    {
        std::lock_guard<std::mutex> g(textMutex);
        pending.swap(textQueue);
    }
    if (pending.empty()) return;
    if (!gVm || !gActivity || !gPushText) return;
    JNIEnv* env = nullptr;
    bool attached = false;
    if (gVm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        if (gVm->AttachCurrentThread(&env, nullptr) != JNI_OK) return;
        attached = true;
    }
    for (auto& t : pending) {
        jstring js = env->NewStringUTF(t.text.c_str());
        if (!js) continue;
        env->CallVoidMethod(gActivity, gPushText, (jint)t.x, (jint)t.y, js, (jint)t.color, (jint)t.size);
        env->DeleteLocalRef(js);
        if (env->ExceptionCheck()) env->ExceptionClear();
    }
    if (attached) gVm->DetachCurrentThread();
}

void BinaryOsCore::requestKeyboard() {
    if (!gVm || !gActivity || !gRequestKb) return;
    JNIEnv* env = nullptr;
    bool attached = false;
    if (gVm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        if (gVm->AttachCurrentThread(&env, nullptr) != JNI_OK) return;
        attached = true;
    }
    env->CallVoidMethod(gActivity, gRequestKb);
    if (env->ExceptionCheck()) env->ExceptionClear();
    if (attached) gVm->DetachCurrentThread();
}

void BinaryOsCore::requestShutdown() {
    if (!gVm || !gActivity || !gShutdown) return;
    JNIEnv* env = nullptr;
    bool attached = false;
    if (gVm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        if (gVm->AttachCurrentThread(&env, nullptr) != JNI_OK) return;
        attached = true;
    }
    env->CallVoidMethod(gActivity, gShutdown);
    if (env->ExceptionCheck()) env->ExceptionClear();
    if (attached) gVm->DetachCurrentThread();
}