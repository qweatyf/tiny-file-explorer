#ifndef BINARY_OS_CORE_H
#define BINARY_OS_CORE_H

#include <jni.h>
#include <vector>
#include <cstdint>
#include <cstring>
#include <string>
#include <chrono>
#include <thread>
#include <cstdlib>
#include <cstdio>
#include <mutex>
#include <atomic>
#include <random>
#include <cmath>
#include <algorithm>
#include "Font.hpp"
#include "Assembler.hpp"
#include "SystemCore.hpp"

struct TextCommand {
    int x, y, size;
    uint32_t color;
    std::string text;
};

class RayTracer {
public:
    struct Vec3 {
        float x, y, z;
        Vec3() : x(0), y(0), z(0) {}
        Vec3(float a, float b, float c) : x(a), y(b), z(c) {}
        Vec3 add(const Vec3& o) const { return Vec3(x + o.x, y + o.y, z + o.z); }
        Vec3 sub(const Vec3& o) const { return Vec3(x - o.x, y - o.y, z - o.z); }
        Vec3 mul(float s) const { return Vec3(x * s, y * s, z * s); }
        float dot(const Vec3& o) const { return x * o.x + y * o.y + z * o.z; }
        Vec3 unit() const {
            float l = std::sqrt(dot(*this));
            return l > 1e-6f ? mul(1.0f / l) : Vec3();
        }
    };
    struct Sphere { Vec3 c; float r; uint32_t color; };
    struct Plane { Vec3 n; float d; uint32_t color; };
    struct Hit { bool ok; float t; Vec3 p; Vec3 n; uint32_t color; Hit() : ok(false), t(0), color(0) {} };

    Vec3 light;
    std::vector<Sphere> spheres;
    Plane floor;

    RayTracer();
    bool raySphere(const Vec3& ro, const Vec3& rd, const Sphere& s, float& tOut);
    bool rayPlane(const Vec3& ro, const Vec3& rd, const Plane& p, float& tOut);
    bool inShadow(const Vec3& ro, const Vec3& rd);
    Hit cast(const Vec3& ro, const Vec3& rd);
    uint32_t shade(const Vec3& ro, const Vec3& rd, int depth);
    void render(std::vector<uint32_t>& out, int w, int h, float angle);
};

class BinaryOsCore {
public:
    enum class MouseState {
        IDLE,
        LEFT_PENDING,
        LEFT_DRAG,
        RIGHT_PENDING,
        RIGHT_DRAG,
        SCROLL
    };

    static const int MEM_SIZE = 256 * 1024 * 1024;
    uint8_t* memory = nullptr;

    uint64_t acc = 0, regAddr = 0, sp = 0;
    const uint64_t PROG_BASE = 512;
    const uint64_t MMIO_INPUT = 0x800000;

    int width = 640, height = 360, pixelCount = 0;
    float uiScale = 1.0f;
    std::vector<uint32_t> screenPixels, frontBuffer;
    std::atomic<bool> abortFlag{false}, frameReady{false};
    std::mutex drawMutex, bufferMutex;

    OSKernel* kernel = nullptr;
    FileSystem* fs = nullptr;
    Registry* reg = nullptr;
    RayTracer* tracer = nullptr;

    int activeDisk = 0;
    std::string activeSub[2];
    int scrollY[2] = {0, 0};
    int explorerSelRow = -1;
    int explorerSelPid = -1;
    bool startOpen = false;
    std::string regEditKey = "HKLM\\SOFTWARE\\BinaryOS";
    uint64_t frameTick = 0;

    float rtAngle = 0.0f;
    std::vector<uint32_t> rtBuffer;
    int rtW = 200, rtH = 150;
    std::chrono::steady_clock::time_point rtLast;

    int cursorX = 320;
    int cursorY = 180;
    float velX = 0.0f;
    float velY = 0.0f;
    bool leftDown = false;
    bool rightDown = false;
    std::chrono::steady_clock::time_point lastClickAt;
    int lastClickX = 0;
    int lastClickY = 0;

    MouseState mouseState = MouseState::IDLE;
    std::chrono::steady_clock::time_point leftDownAt, rightDownAt;
    int leftDownX = 0, leftDownY = 0;
    int rightDownX = 0, rightDownY = 0;
    bool leftButtonDown = false, rightButtonDown = false;
    int touchLastX = 0, touchLastY = 0;

    bool rbMenuOpen = false;
    int rbMenuX = 0, rbMenuY = 0;
    int rbRectX = 0, rbRectY = 0, rbRectW = 0, rbRectH = 0;
    int rbStartX = 0, rbStartY = 0;
    int rbCurX = 0, rbCurY = 0;

    int dragPid = -1;
    int dragOffX = 0;
    int dragOffY = 0;
    int scrollPid = -1;
    int scrollAccum = 0;
    int scrollWinPid = -1;
    int scrollWinAccum = 0;

    bool contextMenuOpen = false;
    int ctxX = 0, ctxY = 0, ctxDisk = 0;
    std::string ctxSub, ctxName;
    bool ctxIsDir = false;
    int ctxPid = -1;

    bool renameMode = false;
    std::string renameOld, renameBuf;
    int renameDisk = 0;
    std::string renameSub;

    bool taskmgrOpen = false;
    int taskmgrPid = -1;
    int taskmgrSelectedPid = -1;
    bool taskmgrConfirmOpen = false;
    int taskmgrConfirmPid = -1;
    std::vector<uint64_t> memHistory;
    std::chrono::steady_clock::time_point memSampleAt;

    bool terminalOpen = false;
    int terminalPid = -1;
    std::string terminalInput;
    std::vector<std::string> terminalHistory;
    int terminalHistoryScroll = 0;

    std::mutex textMutex;
    std::vector<TextCommand> textQueue;

    BinaryOsCore();
    ~BinaryOsCore();

    uint8_t readMem(uint64_t addr);
    void writeMem(uint64_t addr, uint8_t data);
    void writeLong(uint64_t addr, uint64_t v);
    uint64_t readLong(uint64_t addr);
    uint32_t readInt(uint64_t addr);

    void setPixel(int x, int y, uint32_t c);
    void mixPixel(int x, int y, uint32_t c, float alpha);
    void blitChar(int x, int y, uint8_t ch, uint32_t color, int scale);
    void drawString(int x, int y, const std::string& s, uint32_t color, int scale);
    void drawStringBold(int x, int y, const std::string& s, uint32_t color, int scale);
    void fillRect(int rx, int ry, int rw, int rh, uint32_t c);
    void verticalGradient(int rx, int ry, int rw, int rh, uint32_t top, uint32_t bottom);
    void roundRect(int rx, int ry, int rw, int rh, int radius, uint32_t c);
    void roundRectTop(int rx, int ry, int rw, int rh, int radius, uint32_t c);
    void castShadow(int wx, int wy, int ww, int wh);
    uint32_t lerpColor(uint32_t a, uint32_t b, float t);
    void drawIcon(int type, int x, int y, int size);
    void drawCursor();
    void drawRubberBand();

    void drawDesktop();
    void drawDesktopIcons();
    void drawTaskbar();
    void drawStartMenu();
    void drawWindowFrame(const Process& p);
    void drawExplorerContents(const Process& p);
    void drawRegEditContents(const Process& p);
    void drawNotepadContents(const Process& p);
    void drawBrowserContents(const Process& p);
    void drawSandbox3D(const Process& p);
    void drawTaskMgrContents(const Process& p);
    void drawTerminalContents(const Process& p);
    void drawContextMenu();
    void drawRenameDialog();
    void drawTaskmgrConfirm();
    void drawRbMenu();

    void spawnExplorer(int disk, const std::string& sub);
    void spawnTerminal();
    void spawnTaskMgr();
    void parseTerminalCommand(const std::string& raw);

    void handleMouseDown(int mx, int my, bool fromDrag = false);
    void handleMouseUp(int mx, int my);
    void handleRightClick(int mx, int my);
    void handleDoubleClick(int mx, int my);
    void openFileEntry(int disk, const std::string& sub, const FileItem& item);
    void executeExe(const std::string& fullPath);
    void applyRbMenuChoice(int idx);

    void setTouch(int x, int y, bool pressed, int pointerCount);
    void clearTouch();
    void publicSetTouch(int x, int y, bool pressed, int pointerCount);
    void publicClearTouch();

    void pushTextToJava();
    void requestKeyboard();
    void requestShutdown();

    void flushRegistry();
    void setPaths(const std::string& c, const std::string& d);
    void setResolution(int w, int h);
    void clearMem();
    void forceAbort();
    void assemble(const std::string& code);
    void copyPixels(uint32_t* out);
    int getWidth();
    int getHeight();
    uint8_t* getMemory();
    void setTerminalInput(const std::string& s);
    std::string getTerminalInput();
    void clearTerminalInput();
    void submitTerminal();
    void execute(uint32_t watchdogMs, bool& outHalted);
    void startDesktop();
    int S(int v);
};

#endif