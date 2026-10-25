#include "BinaryOsCore.h"

JavaVM* gVm = nullptr;
jobject gActivity = nullptr;
jmethodID gPushText = nullptr;
jmethodID gRequestKb = nullptr;
jmethodID gShutdown = nullptr;

BinaryOsCore::BinaryOsCore() {
    memory = (uint8_t*)malloc(MEM_SIZE);
    if (memory) memset(memory, 0, MEM_SIZE);
    screenPixels.resize(width * height, 0);
    frontBuffer.resize(width * height, 0);
    pixelCount = width * height;
    sp = MEM_SIZE - 1024;
    kernel = new OSKernel();
    fs = new FileSystem();
    reg = new Registry();
    tracer = new RayTracer();
    rtLast = std::chrono::steady_clock::now();
    memSampleAt = std::chrono::steady_clock::now();
    lastClickAt = std::chrono::steady_clock::now() - std::chrono::seconds(10);
}

BinaryOsCore::~BinaryOsCore() {
    delete kernel;
    delete fs;
    delete reg;
    delete tracer;
    if (memory) free(memory);
}

int BinaryOsCore::S(int v) {
    return v < 1 ? 1 : v;
}

uint8_t BinaryOsCore::readMem(uint64_t addr) { return addr < (uint64_t)MEM_SIZE ? memory[addr] : 0; }
void BinaryOsCore::writeMem(uint64_t addr, uint8_t data) { if (addr < (uint64_t)MEM_SIZE) memory[addr] = data; }
void BinaryOsCore::writeLong(uint64_t addr, uint64_t v) {
    for (int i = 0; i < 8; ++i) writeMem(addr + i, (uint8_t)((v >> ((7 - i) * 8)) & 0xFF));
}
uint64_t BinaryOsCore::readLong(uint64_t addr) {
    uint64_t v = 0;
    for (int i = 0; i < 8; ++i) v = (v << 8) | readMem(addr + i);
    return v;
}
uint32_t BinaryOsCore::readInt(uint64_t addr) {
    return ((uint32_t)readMem(addr) << 24) | ((uint32_t)readMem(addr + 1) << 16)
         | ((uint32_t)readMem(addr + 2) << 8) | (uint32_t)readMem(addr + 3);
}

void BinaryOsCore::flushRegistry() { if (reg) reg->flush(); }

void BinaryOsCore::setPaths(const std::string& c, const std::string& d) {
    if (fs) fs->mount(c, d);
    if (reg) {
        reg->setPath(c + "/registry.dat");
        reg->load();
        reg->setSz("HKLM\\SYSTEM\\MountPoints", "C", c);
        reg->setSz("HKLM\\SYSTEM\\MountPoints", "D", d);
        if (reg->getDword("HKLM\\SOFTWARE\\BinaryOS", "Version", 0) == 0)
            reg->setDword("HKLM\\SOFTWARE\\BinaryOS", "Version", 0x00010000);
    }
}

void BinaryOsCore::setResolution(int w, int h) {
    std::lock_guard<std::mutex> g(drawMutex);
    width = w;
    height = h;
    pixelCount = width * height;
    screenPixels.assign((size_t)pixelCount, 0);
    frontBuffer.assign((size_t)pixelCount, 0);
    cursorX = width / 2;
    cursorY = height / 2;
    uiScale = 1.0f;
    rtW = 160;
    rtH = 120;
}

void BinaryOsCore::clearMem() {
    std::lock_guard<std::mutex> g(drawMutex);
    memset(memory, 0, MEM_SIZE);
    abortFlag.store(false);
    frameReady.store(false);
}

void BinaryOsCore::forceAbort() { abortFlag.store(true); }
void BinaryOsCore::assemble(const std::string& code) { if (memory) Assembler::assemble(code, memory, PROG_BASE); }

void BinaryOsCore::copyPixels(uint32_t* out) {
    std::lock_guard<std::mutex> g(bufferMutex);
    if (frameReady.load()) memcpy(out, frontBuffer.data(), (size_t)pixelCount * sizeof(uint32_t));
}

int BinaryOsCore::getWidth() { return width; }
int BinaryOsCore::getHeight() { return height; }
uint8_t* BinaryOsCore::getMemory() { return memory; }

void BinaryOsCore::setTerminalInput(const std::string& s) { terminalInput = s; }
std::string BinaryOsCore::getTerminalInput() { return terminalInput; }
void BinaryOsCore::clearTerminalInput() { terminalInput.clear(); }
void BinaryOsCore::submitTerminal() {
    std::string cmd = terminalInput;
    terminalInput.clear();
    parseTerminalCommand(cmd);
}

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void*) {
    gVm = vm;
    return JNI_VERSION_1_6;
}

static BinaryOsCore* osCore = nullptr;

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeInit(JNIEnv* env, jobject thiz) {
    gActivity = env->NewGlobalRef(thiz);
    jclass cls = env->GetObjectClass(thiz);
    gPushText = env->GetMethodID(cls, "onNativePushText", "(IILjava/lang/String;II)V");
    gRequestKb = env->GetMethodID(cls, "onNativeRequestKeyboard", "()V");
    gShutdown = env->GetMethodID(cls, "onNativeShutdown", "()V");
    if (!osCore) osCore = new BinaryOsCore();
}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeSetPaths(JNIEnv* env, jobject, jstring c, jstring d) {
    if (!osCore) return;
    const char* pc = env->GetStringUTFChars(c, nullptr);
    const char* pd = env->GetStringUTFChars(d, nullptr);
    osCore->setPaths(pc, pd);
    env->ReleaseStringUTFChars(c, pc);
    env->ReleaseStringUTFChars(d, pd);
}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeSetResolution(JNIEnv*, jobject, jint w, jint h) {
    if (osCore) osCore->setResolution(w, h);
}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeClear(JNIEnv*, jobject) {
    if (osCore) osCore->clearMem();
}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeOpenLog(JNIEnv*, jobject, jstring) {}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeTouch(JNIEnv*, jobject, jint x, jint y, jboolean p, jint pc) {
    if (osCore) osCore->publicSetTouch(x, y, p, pc);
}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeClearTouch(JNIEnv*, jobject) {
    if (osCore) osCore->publicClearTouch();
}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeAbort(JNIEnv*, jobject) {
    if (osCore) osCore->forceAbort();
}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeSendKey(JNIEnv*, jobject, jstring) {}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeTerminalInput(JNIEnv* env, jobject, jstring s) {
    if (!osCore || !s) return;
    const char* cs = env->GetStringUTFChars(s, nullptr);
    osCore->setTerminalInput(cs);
    env->ReleaseStringUTFChars(s, cs);
}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeTerminalSubmit(JNIEnv*, jobject) {
    if (osCore) osCore->submitTerminal();
}

JNIEXPORT jboolean JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeRunAsm(JNIEnv* env, jobject, jstring code, jint watchdog) {
    if (!osCore || !code) return JNI_FALSE;
    const char* s = env->GetStringUTFChars(code, nullptr);
    osCore->assemble(s);
    env->ReleaseStringUTFChars(code, s);
    bool halted = false;
    osCore->execute((uint32_t)watchdog, halted);
    return halted ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeStartDesktop(JNIEnv*, jobject) {
    if (osCore) osCore->startDesktop();
}

JNIEXPORT jboolean JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeGetPixels(JNIEnv* env, jobject, jintArray out) {
    if (!osCore || !out) return JNI_FALSE;
    int w = osCore->getWidth();
    int h = osCore->getHeight();
    jsize len = env->GetArrayLength(out);
    if (len < w * h) return JNI_FALSE;
    jint* p = env->GetIntArrayElements(out, nullptr);
    if (!p) return JNI_FALSE;
    osCore->copyPixels((uint32_t*)p);
    env->ReleaseIntArrayElements(out, p, 0);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeSaveFile(JNIEnv*, jobject, jstring) {}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeLoadFile(JNIEnv*, jobject, jstring) {}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeLoadRegistry(JNIEnv*, jobject, jstring) {}

JNIEXPORT void JNICALL
Java_com_example_myempty_activity2_BinaryOsActivity_nativeFlushRegistry(JNIEnv*, jobject) {
    if (osCore) osCore->flushRegistry();
}

}