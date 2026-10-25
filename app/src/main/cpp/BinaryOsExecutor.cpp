#include "BinaryOsCore.h"

void BinaryOsCore::execute(uint32_t watchdogMs, bool& outHalted) {
    auto startTime = std::chrono::steady_clock::now();
    uint64_t pc = PROG_BASE;
    bool isRunning = true;
    outHalted = false;
    while (isRunning) {
        if (abortFlag.load()) { outHalted = false; break; }
        if (watchdogMs != 0) {
            auto el = std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::steady_clock::now() - startTime).count();
            if ((uint64_t)el > (uint64_t)watchdogMs) break;
        }
        uint8_t op = readMem(pc);
        if (op == 0x00) { isRunning = false; outHalted = true; break; }
        else if (op == 0x01) { uint64_t addr = readLong(pc + 1); uint8_t d = readMem(pc + 9); pc += 10; writeMem(addr, d); }
        else if (op == 0x02) { uint64_t addr = readLong(pc + 1); pc += 9; acc = readMem(addr); }
        else if (op == 0x03) { uint64_t addr = readLong(pc + 1); pc += 9; acc += readMem(addr); }
        else if (op == 0x04) { pc = readLong(pc + 1); }
        else if (op == 0x05) { uint64_t addr = readLong(pc + 1); if (acc == 0) pc = addr; else pc += 9; }
        else if (op == 0x06) { pc++; }
        else if (op == 0x07) { uint64_t addr = readLong(pc + 1); pc += 9; acc -= readMem(addr); }
        else if (op == 0x08) { uint64_t addr = readLong(pc + 1); pc += 9; writeLong(addr, acc); }
        else if (op == 0x09) { sp -= 8; writeLong(sp, acc); pc++; }
        else if (op == 0x0A) { acc = readLong(sp); sp += 8; pc++; }
        else if (op == 0x0E) { writeMem(regAddr, acc & 0xFF); pc++; }
        else if (op == 0x0F) { acc = readMem(regAddr); pc++; }
        else if (op == 0x10) { regAddr = acc; pc++; }
        else if (op == 0x11) { pc++; }
        else if (op == 0x12) {
            std::lock_guard<std::mutex> g(drawMutex);
            int64_t idx = (int64_t)readLong(pc + 1);
            uint32_t c = readInt(pc + 9);
            pc += 13;
            if (idx >= 0 && idx < pixelCount) screenPixels[(size_t)idx] = c;
        }
        else if (op == 0x13) {
            std::lock_guard<std::mutex> g(drawMutex);
            uint32_t c = readInt(pc + 1);
            pc += 5;
            for (int i = 0; i < pixelCount; ++i) screenPixels[i] = c;
        }
        else if (op == 0x15) {
            std::lock_guard<std::mutex> g(bufferMutex);
            memcpy(frontBuffer.data(), screenPixels.data(), (size_t)pixelCount * sizeof(uint32_t));
            frameReady.store(true);
            std::this_thread::sleep_for(std::chrono::milliseconds(16));
            pc++;
        }
        else if (op == 0x17) {
            std::lock_guard<std::mutex> g(drawMutex);
            int64_t rx = (int64_t)readLong(pc + 1), ry = (int64_t)readLong(pc + 9);
            int64_t rw = (int64_t)readLong(pc + 17), rh = (int64_t)readLong(pc + 25);
            uint32_t c = readInt(pc + 33);
            pc += 37;
            int64_t sx = rx < 0 ? 0 : rx;
            int64_t sy = ry < 0 ? 0 : ry;
            int64_t ex = rx + rw; if (ex > width) ex = width;
            int64_t ey = ry + rh; if (ey > height) ey = height;
            if (sx < ex && sy < ey) {
                for (int64_t y = sy; y < ey; ++y) {
                    uint32_t* row = &screenPixels[(size_t)y * width];
                    for (int64_t x = sx; x < ex; ++x) row[x] = c;
                }
            }
        }
        else if (op == 0x18) {
            uint64_t ax = readLong(pc + 1), ay = readLong(pc + 9);
            pc += 17;
            writeLong(ax, readLong(MMIO_INPUT));
            writeLong(ay, readLong(MMIO_INPUT + 8));
        }
        else if (op == 0x19) { uint64_t addr = readLong(pc + 1); pc += 9; acc = readLong(addr); }
        else if (op == 0x1A) { uint64_t imm = readLong(pc + 1); pc += 9; acc -= imm; }
        else if (op == 0x1B) {
            std::lock_guard<std::mutex> g(drawMutex);
            uint64_t ax = readLong(pc + 1), ay = readLong(pc + 9);
            int64_t rw = (int64_t)readLong(pc + 17), rh = (int64_t)readLong(pc + 25);
            uint32_t c = readInt(pc + 33);
            pc += 37;
            int64_t rx = (int64_t)readLong(ax), ry = (int64_t)readLong(ay);
            int64_t sx = rx < 0 ? 0 : rx;
            int64_t sy = ry < 0 ? 0 : ry;
            int64_t ex = rx + rw; if (ex > width) ex = width;
            int64_t ey = ry + rh; if (ey > height) ey = height;
            if (sx < ex && sy < ey) {
                for (int64_t y = sy; y < ey; ++y) {
                    uint32_t* row = &screenPixels[(size_t)y * width];
                    for (int64_t x = sx; x < ex; ++x) row[x] = c;
                }
            }
        }
        else if (op == 0x1C) {
            std::lock_guard<std::mutex> g(drawMutex);
            int64_t tx = (int64_t)readLong(pc + 1), ty = (int64_t)readLong(pc + 9);
            uint64_t strAddr = readLong(pc + 17);
            uint32_t c = readInt(pc + 25);
            pc += 29;
            int cx = (int)tx;
            while (true) {
                uint8_t ch = readMem(strAddr++);
                if (ch == 0) break;
                blitChar(cx, (int)ty, ch, c, 1);
                cx += 8;
            }
        }
        else if (op == 0x1E) {
            std::lock_guard<std::mutex> g(drawMutex);
            int64_t cx = (int64_t)readLong(pc + 1);
            int64_t cy = (int64_t)readLong(pc + 9);
            int64_t r = (int64_t)readLong(pc + 17);
            uint32_t c = readInt(pc + 25);
            pc += 29;
            if (r > 0) {
                int64_t x = 0, y = r, d = 3 - 2 * r;
                while (y >= x) {
                    setPixel((int)(cx + x), (int)(cy + y), c);
                    setPixel((int)(cx - x), (int)(cy + y), c);
                    setPixel((int)(cx + x), (int)(cy - y), c);
                    setPixel((int)(cx - x), (int)(cy - y), c);
                    setPixel((int)(cx + y), (int)(cy + x), c);
                    setPixel((int)(cx - y), (int)(cy + x), c);
                    setPixel((int)(cx + y), (int)(cy - x), c);
                    setPixel((int)(cx - y), (int)(cy - x), c);
                    x++;
                    if (d > 0) { y--; d = d + 4 * (x - y) + 10; }
                    else { d = d + 4 * x + 6; }
                }
            }
        }
        else if (op == 0x1F) {
            std::lock_guard<std::mutex> g(drawMutex);
            int64_t x0 = (int64_t)readLong(pc + 1), y0 = (int64_t)readLong(pc + 9);
            int64_t x1 = (int64_t)readLong(pc + 17), y1 = (int64_t)readLong(pc + 25);
            uint32_t c = readInt(pc + 33);
            pc += 37;
            int64_t dx = std::abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
            int64_t dy = -std::abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
            int64_t err = dx + dy;
            while (true) {
                setPixel((int)x0, (int)y0, c);
                if (x0 == x1 && y0 == y1) break;
                int64_t e2 = 2 * err;
                if (e2 >= dy) { err += dy; x0 += sx; }
                if (e2 <= dx) { err += dx; y0 += sy; }
            }
        }
        else if (op == 0x20) { uint64_t imm = readLong(pc + 1); pc += 9; acc *= imm; }
        else if (op == 0x21) { uint64_t imm = readLong(pc + 1); pc += 9; if (imm != 0) acc /= imm; }
        else if (op == 0x22) { uint64_t imm = readLong(pc + 1); pc += 9; if (imm != 0) acc %= imm; }
        else if (op == 0x23) { uint64_t imm = readLong(pc + 1); pc += 9; acc &= imm; }
        else if (op == 0x24) { uint64_t imm = readLong(pc + 1); pc += 9; acc |= imm; }
        else if (op == 0x25) { uint64_t imm = readLong(pc + 1); pc += 9; acc ^= imm; }
        else if (op == 0x26) { uint64_t imm = readLong(pc + 1); pc += 9; acc <<= imm; }
        else if (op == 0x27) { uint64_t imm = readLong(pc + 1); pc += 9; acc >>= imm; }
        else if (op == 0x28) { acc = (uint64_t)rand(); pc++; }
        else if (op == 0x29) { std::this_thread::sleep_for(std::chrono::milliseconds(16)); pc++; }
        else if (op == 0x2B) {
            uint64_t ax = readLong(pc + 1), ay = readLong(pc + 9);
            pc += 17;
            writeLong(ax, (uint64_t)((int64_t)cursorX));
            writeLong(ay, (uint64_t)((int64_t)cursorY));
        }
        else if (op == 0x2C) {
            uint64_t ax = readLong(pc + 1), ay = readLong(pc + 9), ap = readLong(pc + 17);
            pc += 25;
            writeLong(ax, readLong(MMIO_INPUT));
            writeLong(ay, readLong(MMIO_INPUT + 8));
            writeLong(ap, readLong(MMIO_INPUT + 16));
        }
        else if (op == 0x2D) {
            uint64_t mxA = readLong(pc + 1), myA = readLong(pc + 9);
            int64_t rx = (int64_t)readLong(pc + 17), ry = (int64_t)readLong(pc + 25);
            int64_t rw = (int64_t)readLong(pc + 33), rh = (int64_t)readLong(pc + 41);
            pc += 49;
            int64_t mx = (int64_t)readLong(mxA);
            int64_t my = (int64_t)readLong(myA);
            acc = (mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh) ? 1 : 0;
        }
        else if (op == 0x2F) {
            uint64_t ms = readLong(pc + 1);
            pc += 9;
            std::this_thread::sleep_for(std::chrono::milliseconds(ms));
        }
        else if (op == 0x30) {
            std::lock_guard<std::mutex> g(drawMutex);
            int64_t tx = (int64_t)readLong(pc + 1), ty = (int64_t)readLong(pc + 9);
            uint64_t strAddr = readLong(pc + 17);
            uint32_t c = (uint32_t)readLong(pc + 25);
            pc += 33;
            int cx = (int)tx;
            while (true) {
                uint8_t ch = readMem(strAddr++);
                if (ch == 0) break;
                blitChar(cx, (int)ty, ch, c, 1);
                cx += 8;
            }
        }
        else if (op == 0x32) {
            while (!abortFlag.load()) {
                if (watchdogMs != 0) {
                    auto el = std::chrono::duration_cast<std::chrono::milliseconds>(
                        std::chrono::steady_clock::now() - startTime).count();
                    if ((uint64_t)el > (uint64_t)watchdogMs) break;
                }
                {
                    std::lock_guard<std::mutex> g(drawMutex);
                    drawDesktop();
                    drawDesktopIcons();
                    auto procs = kernel->snapshot();
                    for (auto& p : procs) {
                        if (!p.is_running || !p.is_windowed) continue;
                        drawWindowFrame(p);
                        if (p.is_3d) drawSandbox3D(p);
                        else if (p.regedit) drawRegEditContents(p);
                        else if (p.is_notepad) drawNotepadContents(p);
                        else if (p.is_browser) drawBrowserContents(p);
                        else if (p.is_terminal) drawTerminalContents(p);
                        else if (p.is_taskmgr) drawTaskMgrContents(p);
                        else drawExplorerContents(p);
                    }
                    drawTaskbar();
                    drawStartMenu();
                    drawContextMenu();
                    drawRenameDialog();
                    drawTaskmgrConfirm();
                    drawRbMenu();
                    drawRubberBand();
                    drawCursor();
                }
                pushTextToJava();
                {
                    std::lock_guard<std::mutex> g(bufferMutex);
                    memcpy(frontBuffer.data(), screenPixels.data(), (size_t)pixelCount * sizeof(uint32_t));
                    frameReady.store(true);
                }
                auto now = std::chrono::steady_clock::now();
                if (taskmgrOpen && std::chrono::duration<float>(now - memSampleAt).count() > 0.5f) {
                    memSampleAt = now;
                    memHistory.push_back(kernel->totalMemory());
                    if (memHistory.size() > 200) memHistory.erase(memHistory.begin());
                }
                if (reg && ((++frameTick % 300) == 0)) reg->flush();
                std::this_thread::sleep_for(std::chrono::milliseconds(16));
            }
            if (reg) reg->flush();
            isRunning = false;
            break;
        }
        else if (op == 0x33) {
            std::lock_guard<std::mutex> g(drawMutex);
            int64_t x0 = (int64_t)readLong(pc + 1), y0 = (int64_t)readLong(pc + 9);
            int64_t x1 = (int64_t)readLong(pc + 17), y1 = (int64_t)readLong(pc + 25);
            int64_t x2 = (int64_t)readLong(pc + 33), y2 = (int64_t)readLong(pc + 41);
            uint32_t c = (uint32_t)readLong(pc + 49);
            pc += 57;
            int minX = (int)std::max<int64_t>(0, std::min({x0, x1, x2}));
            int maxX = (int)std::min<int64_t>(width - 1, std::max({x0, x1, x2}));
            int minY = (int)std::max<int64_t>(0, std::min({y0, y1, y2}));
            int maxY = (int)std::min<int64_t>(height - 1, std::max({y0, y1, y2}));
            auto edge = [](int64_t ax, int64_t ay, int64_t bx, int64_t by, int64_t cx, int64_t cy) {
                return (cx - ax) * (by - ay) - (cy - ay) * (bx - ax);
            };
            int64_t area = edge(x0, y0, x1, y1, x2, y2);
            if (area != 0) {
                for (int y = minY; y <= maxY; ++y) {
                    for (int x = minX; x <= maxX; ++x) {
                        int64_t w0 = edge(x1, y1, x2, y2, x, y);
                        int64_t w1 = edge(x2, y2, x0, y0, x, y);
                        int64_t w2 = edge(x0, y0, x1, y1, x, y);
                        if ((w0 >= 0 && w1 >= 0 && w2 >= 0) || (w0 <= 0 && w1 <= 0 && w2 <= 0)) {
                            setPixel(x, y, c);
                        }
                    }
                }
            }
        }
        else if (op == 0xFF) { isRunning = false; outHalted = false; break; }
        else { isRunning = false; outHalted = false; break; }
    }
}

void BinaryOsCore::startDesktop() {
    abortFlag.store(false);
    uint8_t* p = memory + PROG_BASE;
    p[0] = 0x32;
}