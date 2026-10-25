#include "BinaryOsCore.h"

void BinaryOsCore::spawnExplorer(int disk, const std::string& sub) {
    std::string name = (disk == 0) ? "Explorer C:" : "Explorer D:";
    if (kernel->alive(name)) {
        auto procs = kernel->snapshot();
        for (auto& p : procs) {
            if (p.is_running && p.name == name) {
                kernel->raise(p.pid);
                kernel->setFolderState(p.pid, disk, sub);
                break;
            }
        }
        return;
    }
    kernel->spawn(name, true, false, false, 150, 60, 500, 380, false, "", false, false, false, true, disk, sub);
}

void BinaryOsCore::spawnTerminal() {
    if (kernel->alive("Terminal")) {
        auto procs = kernel->snapshot();
        for (auto& p : procs) {
            if (p.is_running && p.name == "Terminal") { kernel->raise(p.pid); break; }
        }
        terminalOpen = true;
        return;
    }
    int pid = kernel->spawn("Terminal", true, false, false, 120, 100, 520, 300, false, "", false, true, false, false);
    terminalPid = pid;
    terminalOpen = true;
    terminalHistory.clear();
    terminalHistory.push_back("BinaryOS Terminal v1.0");
    terminalHistory.push_back("Type 'help' for commands.");
    terminalInput.clear();
}

void BinaryOsCore::spawnTaskMgr() {
    if (kernel->alive("TaskMgr")) {
        auto procs = kernel->snapshot();
        for (auto& p : procs) {
            if (p.is_running && p.name == "TaskMgr") { kernel->raise(p.pid); break; }
        }
        taskmgrOpen = true;
        return;
    }
    int pid = kernel->spawn("TaskMgr", true, false, false, 200, 80, 480, 380, false, "", false, false, true, false);
    taskmgrPid = pid;
    taskmgrOpen = true;
    memHistory.clear();
    memSampleAt = std::chrono::steady_clock::now();
}

void BinaryOsCore::parseTerminalCommand(const std::string& raw) {
    if (raw.empty()) return;
    terminalHistory.push_back("C:\\> " + raw);
    std::string line = raw;
    std::string cmd;
    std::string rest;
    size_t sp = line.find(' ');
    if (sp == std::string::npos) cmd = line;
    else { cmd = line.substr(0, sp); rest = line.substr(sp + 1); }
    for (auto& c : cmd) c = (char)tolower((unsigned char)c);
    if (cmd == "help") {
        terminalHistory.push_back("help        - list commands");
        terminalHistory.push_back("cls         - clear screen");
        terminalHistory.push_back("dir         - list current folder");
        terminalHistory.push_back("cd <dir>    - change folder");
        terminalHistory.push_back("type <file> - show text file");
        terminalHistory.push_back("start <app> - launch app");
        terminalHistory.push_back("tasklist    - list processes");
        terminalHistory.push_back("kill <pid>  - end process");
        terminalHistory.push_back("echo <text> - print text");
        terminalHistory.push_back("asm <code>  - run assembly code");
        terminalHistory.push_back("reboot      - restart desktop");
        terminalHistory.push_back("shutdown    - exit BinaryOS");
    } else if (cmd == "cls") {
        terminalHistory.clear();
        terminalHistory.push_back("BinaryOS Terminal v1.0");
    } else if (cmd == "dir") {
        auto files = fs->list(activeDisk, activeSub[activeDisk]);
        for (auto& f : files) {
            char buf[128];
            snprintf(buf, sizeof(buf), "%-32s %s", f.name.c_str(), f.isDir ? "<DIR>" : "file");
            terminalHistory.push_back(buf);
        }
        if (files.empty()) terminalHistory.push_back("(empty)");
    } else if (cmd == "cd") {
        if (rest == "..") {
            size_t p = activeSub[activeDisk].rfind('/');
            if (p == std::string::npos) activeSub[activeDisk].clear();
            else activeSub[activeDisk] = activeSub[activeDisk].substr(0, p);
        } else if (!rest.empty()) {
            if (activeSub[activeDisk].empty()) activeSub[activeDisk] = rest;
            else activeSub[activeDisk] += "/" + rest;
        } else activeSub[activeDisk].clear();
    } else if (cmd == "type") {
        std::string full = fs->root(activeDisk);
        if (!activeSub[activeDisk].empty()) full += "/" + activeSub[activeDisk];
        if (full.back() != '/') full += '/';
        full += rest;
        std::string txt = fs->readHead(full, 8192);
        if (txt.empty()) terminalHistory.push_back("(empty or missing)");
        else {
            size_t s = 0;
            while (s < txt.size()) {
                size_t e = txt.find('\n', s);
                if (e == std::string::npos) e = txt.size();
                terminalHistory.push_back(txt.substr(s, e - s));
                s = e + 1;
            }
        }
    } else if (cmd == "echo") {
        terminalHistory.push_back(rest);
    } else if (cmd == "tasklist") {
        auto procs = kernel->snapshot();
        for (auto& p : procs) {
            if (!p.is_running) continue;
            char buf[96];
            snprintf(buf, sizeof(buf), "%-5d %-16s %lluK", p.pid, p.name.c_str(), (unsigned long long)(p.mem_bytes / 1024));
            terminalHistory.push_back(buf);
        }
    } else if (cmd == "kill") {
        try {
            int pid = std::stoi(rest);
            kernel->kill(pid);
            char buf[64];
            snprintf(buf, sizeof(buf), "Ended PID %d", pid);
            terminalHistory.push_back(buf);
        } catch (...) { terminalHistory.push_back("Invalid PID"); }
    } else if (cmd == "start") {
        std::string app = rest;
        for (auto& c : app) c = (char)tolower((unsigned char)c);
        if (app == "explorer" || app == "c" || app == "c:") spawnExplorer(0, "");
        else if (app == "d" || app == "d:") spawnExplorer(1, "");
        else if (app == "taskmgr") spawnTaskMgr();
        else if (app == "terminal") spawnTerminal();
        else if (app == "regedit") kernel->spawn("RegEdit.exe", true, false, false, 260, 100, 640, 400, false, "", true);
        else if (app == "sandbox") kernel->spawn("Sandbox3D", true, false, true, width - 420, 80, 400, 300, false, "");
        else terminalHistory.push_back("Unknown app: " + rest);
    } else if (cmd == "asm") {
        if (rest.empty()) terminalHistory.push_back("Usage: asm <assembly code>");
        else {
            terminalHistory.push_back("Assembling...");
            Assembler::assemble(rest, memory, PROG_BASE);
            bool halted = false;
            execute(2000, halted);
            terminalHistory.push_back(halted ? "HALTED" : "ERROR");
        }
    } else if (cmd == "reboot") {
        terminalHistory.push_back("Rebooting desktop...");
    } else if (cmd == "shutdown") {
        terminalHistory.push_back("Shutting down...");
        requestShutdown();
    } else {
        terminalHistory.push_back("Unknown command: " + cmd);
    }
    if (terminalHistory.size() > 2000)
        terminalHistory.erase(terminalHistory.begin(), terminalHistory.begin() + 1000);
    terminalHistoryScroll = 0;
}

void BinaryOsCore::applyRbMenuChoice(int idx) {
    int rx = rbRectX, ry = rbRectY;
    rbMenuOpen = false;
    if (idx == 3) return;
    if (idx == 0) spawnExplorer(0, "");
    else if (idx == 1) spawnTerminal();
    else if (idx == 2) spawnTaskMgr();
    auto procs = kernel->snapshot();
    for (auto& p : procs) {
        if (!p.is_running || !p.is_windowed) continue;
        if ((idx == 0 && p.name.find("Explorer") != std::string::npos) ||
            (idx == 1 && p.name == "Terminal") ||
            (idx == 2 && p.name == "TaskMgr")) {
            kernel->moveWindow(p.pid, rx, ry);
            break;
        }
    }
}

void BinaryOsCore::executeExe(const std::string& fullPath) {
    std::string content = fs->readHead(fullPath, 65536);
    if (content.empty()) return;
    bool looksAsm = true;
    int printable = 0;
    for (size_t i = 0; i < content.size() && i < 512; ++i) {
        unsigned char c = (unsigned char)content[i];
        if (c == '\n' || c == '\r' || c == '\t' || (c >= 32 && c < 127)) printable++;
        else { looksAsm = false; break; }
    }
    if (!looksAsm || printable == 0) {
        kernel->spawn("Error", true, false, false, 200, 140, 400, 220, true, fullPath);
        return;
    }
    Assembler::assemble(content, memory, PROG_BASE);
    int pid = kernel->spawn(fullPath.substr(fullPath.find_last_of('/') + 1), true, false, false, 100, 60, width - 200, height - 150, false, "", false, false, false, false);
    (void)pid;
    bool halted = false;
    execute(0, halted);
}

void BinaryOsCore::openFileEntry(int disk, const std::string& sub, const FileItem& item) {
    if (item.isDir) {
        std::string newSub = sub;
        if (!newSub.empty()) newSub += "/";
        newSub += item.name;
        activeSub[disk] = newSub;
        activeDisk = disk;
        scrollY[disk] = 0;
        explorerSelRow = -1;
        explorerSelPid = -1;
        auto procs = kernel->snapshot();
        for (auto& p : procs) {
            if (p.is_running && p.diskIndex == disk && (p.is_folder || p.name.find("Explorer") != std::string::npos)) {
                kernel->setFolderState(p.pid, disk, newSub);
            }
        }
        return;
    }
    std::string base = fs->root(disk);
    std::string full = base;
    if (!sub.empty()) full += "/" + sub;
    if (full.back() != '/') full += '/';
    full += item.name;
    std::string lower = item.name;
    for (auto& c : lower) c = (char)tolower((unsigned char)c);
    auto ends = [&](const char* s) {
        size_t t = strlen(s);
        return lower.size() >= t && lower.compare(lower.size() - t, t, s) == 0;
    };
    if (ends(".exe")) {
        executeExe(full);
    } else if (ends(".apk")) {
        kernel->spawn("Error", true, false, false, 200, 140, 400, 220, true, full);
    } else {
        kernel->spawn("Notepad", true, false, false, 220, 140, 480, 360, true, full);
    }
}

void BinaryOsCore::handleDoubleClick(int mx, int my) {
    auto procs = kernel->snapshot();
    for (int i = (int)procs.size() - 1; i >= 0; --i) {
        auto& p = procs[i];
        if (!p.is_running || !p.is_windowed) continue;
        if (mx < p.x || mx >= p.x + p.w || my < p.y || my >= p.y + p.h) continue;
        if (p.is_folder || p.name.find("Explorer") != std::string::npos) {
            int bx = p.x + 8;
            int by = p.y + 38;
            int bw = p.w - 16;
            int bh = p.h - 46;
            int hdrH = 24;
            if (mx < bx || mx >= bx + bw || my < by + hdrH || my >= by + bh) return;
            int rowH = 24;
            int rowY = by + hdrH + 4 - scrollY[p.diskIndex];
            auto files = fs->list(p.diskIndex, p.subPath);
            for (auto& f : files) {
                if (my >= rowY && my < rowY + rowH) {
                    openFileEntry(p.diskIndex, p.subPath, f);
                    return;
                }
                rowY += rowH;
            }
            return;
        }
        if (p.is_terminal) {
            requestKeyboard();
            return;
        }
    }
}

void BinaryOsCore::handleMouseDown(int mx, int my, bool fromDrag) {
    leftDown = true;
    auto now = std::chrono::steady_clock::now();
    float dt = std::chrono::duration<float>(now - lastClickAt).count();
    int dx = mx - lastClickX;
    int dy = my - lastClickY;
    bool isDouble = (dt < 0.4f) && (dx * dx + dy * dy < 100);

    if (rbMenuOpen) {
        int iw = 150, ih = 4 * 24 + 8;
        int ix = rbMenuX - iw / 2, iy = rbMenuY - ih / 2;
        if (ix < 0) ix = 0;
        if (iy < 0) iy = 0;
        if (ix + iw > width) ix = width - iw;
        if (iy + ih > height) iy = height - ih;
        for (int i = 0; i < 4; ++i) {
            int ry = iy + 4 + i * 24;
            if (mx >= ix && mx < ix + iw && my >= ry && my < ry + 24) {
                applyRbMenuChoice(i);
                return;
            }
        }
        rbMenuOpen = false;
        return;
    }

    if (taskmgrConfirmOpen) {
        int dw = 300, dh = 120;
        int dx2 = (width - dw) / 2, dy2 = (height - dh) / 2;
        if (mx >= dx2 + dw - 140 && mx < dx2 + dw - 80 && my >= dy2 + dh - 32 && my < dy2 + dh - 8) {
            if (taskmgrConfirmPid >= 0) {
                kernel->kill(taskmgrConfirmPid);
                if (taskmgrConfirmPid == terminalPid) { terminalPid = -1; terminalOpen = false; }
                if (taskmgrConfirmPid == taskmgrPid) { taskmgrPid = -1; taskmgrOpen = false; }
                if (taskmgrConfirmPid == taskmgrSelectedPid) taskmgrSelectedPid = -1;
            }
            taskmgrConfirmOpen = false;
            taskmgrConfirmPid = -1;
            return;
        }
        if (mx >= dx2 + dw - 72 && mx < dx2 + dw - 12 && my >= dy2 + dh - 32 && my < dy2 + dh - 8) {
            taskmgrConfirmOpen = false;
            taskmgrConfirmPid = -1;
            return;
        }
        return;
    }

    if (contextMenuOpen) {
        int iw = 140, ih = 4 * 24 + 8;
        int ix = ctxX, iy = ctxY;
        if (ix + iw > width) ix = width - iw;
        if (iy + ih > height) iy = height - ih;
        for (int i = 0; i < 4; ++i) {
            int ry = iy + 4 + i * 24;
            if (mx >= ix && mx < ix + iw && my >= ry && my < ry + 24) {
                if (i == 0) {
                    renameMode = true;
                    renameOld = ctxName;
                    renameBuf = ctxName;
                    renameDisk = ctxDisk;
                    renameSub = ctxSub;
                } else if (i == 1) {
                    fs->copyItem(ctxDisk, ctxSub, ctxName);
                } else if (i == 2) {
                    fs->moveItem(ctxDisk, ctxSub, ctxName, "");
                } else if (i == 3) {
                    if (reg) {
                        std::string full = fs->root(ctxDisk);
                        if (!ctxSub.empty()) full += "/" + ctxSub;
                        if (full.back() != '/') full += '/';
                        full += ctxName;
                        reg->setSz("HKLM\\SYSTEM\\SysMirror", "Image", full);
                        reg->flush();
                    }
                }
                contextMenuOpen = false;
                return;
            }
        }
        contextMenuOpen = false;
        return;
    }

    if (renameMode) {
        int dw = 280, dh = 100;
        int dx2 = (width - dw) / 2, dy2 = (height - dh) / 2;
        if (mx >= dx2 + dw - 140 && mx < dx2 + dw - 80 && my >= dy2 + dh - 28 && my < dy2 + dh - 8) {
            if (!renameBuf.empty() && renameBuf != renameOld) fs->renameItem(renameDisk, renameSub, renameOld, renameBuf);
            renameMode = false;
            return;
        }
        if (mx >= dx2 + dw - 70 && mx < dx2 + dw - 10 && my >= dy2 + dh - 28 && my < dy2 + dh - 8) {
            renameMode = false;
            return;
        }
        return;
    }

    int tbY = height - 48;
    if (my >= tbY) {
        if (mx >= 8 && mx < 108 && my >= tbY + 8 && my < tbY + 40) { startOpen = !startOpen; return; }
        if (mx >= 114 && mx < 194 && my >= tbY + 8 && my < tbY + 40) { spawnTaskMgr(); return; }
        auto procs = kernel->snapshot();
        int bx = 200;
        for (auto& p : procs) {
            if (!p.is_running || !p.is_windowed) continue;
            int bw = 150;
            if (bx + bw > width - 100) break;
            if (mx >= bx && mx < bx + bw && my >= tbY + 8 && my < tbY + 40) {
                kernel->raise(p.pid);
                if (p.name == "Terminal") terminalPid = p.pid;
                if (p.name == "TaskMgr") taskmgrPid = p.pid;
                return;
            }
            bx += bw + 6;
        }
        return;
    }

    if (startOpen) {
        int mw = 240, mh = 280, mx2 = 8, my2 = height - 48 - mh - 8;
        if (mx >= mx2 && mx < mx2 + mw && my >= my2 + 48 && my < my2 + mh) {
            int idx = (my - (my2 + 48)) / 30;
            if (idx == 0) spawnExplorer(0, "");
            else if (idx == 1) spawnExplorer(1, "");
            else if (idx == 2) spawnTerminal();
            else if (idx == 3) spawnTaskMgr();
            else if (idx == 4) kernel->spawn("Readme", true, false, false, 220, 140, 480, 360, true, "");
            else if (idx == 5) { startOpen = false; requestShutdown(); return; }
        }
        startOpen = false;
        return;
    }

    if (isDouble && !fromDrag) {
        handleDoubleClick(mx, my);
        lastClickAt = now - std::chrono::seconds(2);
        return;
    }

    auto procs = kernel->snapshot();

    for (int i = (int)procs.size() - 1; i >= 0; --i) {
        auto& p = procs[i];
        if (!p.is_running || !p.is_windowed) continue;
        int closeX = p.x + p.w - 28;
        int closeY = p.y + 6;
        if (mx >= closeX && mx < closeX + 22 && my >= closeY && my < closeY + 18) {
            kernel->kill(p.pid);
            if (p.pid == terminalPid) { terminalPid = -1; terminalOpen = false; }
            if (p.pid == taskmgrPid) { taskmgrPid = -1; taskmgrOpen = false; }
            return;
        }
        int maxX = p.x + p.w - 54;
        int maxY = p.y + 6;
        if (mx >= maxX && mx < maxX + 22 && my >= maxY && my < maxY + 18) {
            kernel->toggleMaximize(p.pid, width, height - 48);
            return;
        }
    }

    for (int i = (int)procs.size() - 1; i >= 0; --i) {
        auto& p = procs[i];
        if (!p.is_running || !p.is_windowed) continue;
        if (mx >= p.x && mx < p.x + p.w && my >= p.y && my < p.y + 30) {
            kernel->raise(p.pid);
            if (!fromDrag) {
                dragPid = p.pid;
                dragOffX = mx - p.x;
                dragOffY = my - p.y;
            }
            return;
        }
    }

    lastClickAt = now;
    lastClickX = mx;
    lastClickY = my;

    for (int i = (int)procs.size() - 1; i >= 0; --i) {
        auto& p = procs[i];
        if (!p.is_running || !p.is_windowed) continue;
        if (mx >= p.x && mx < p.x + p.w && my >= p.y + 30 && my < p.y + p.h) {
            if (p.is_terminal) {
                terminalPid = p.pid;
                terminalOpen = true;
                requestKeyboard();
                return;
            }
            if (p.is_taskmgr) {
                taskmgrPid = p.pid;
                taskmgrOpen = true;
                int bx = p.x + 8;
                int by = p.y + 38;
                int bw = p.w - 16;
                int bh = p.h - 46;
                int rowY = by + 28;
                int rowH = 20;
                int maxRows = (bh - 160) / rowH;
                if (maxRows < 1) maxRows = 1;
                int btnW = 120, btnH = 24;
                int btnX = bx + 8;
                int btnY = by + bh - btnH - 8;
                if (mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH) {
                    if (taskmgrSelectedPid >= 0) {
                        taskmgrConfirmOpen = true;
                        taskmgrConfirmPid = taskmgrSelectedPid;
                    }
                    return;
                }
                if (my >= rowY && my < rowY + maxRows * rowH) {
                    int idx = (my - rowY) / rowH;
                    auto procs2 = kernel->snapshot();
                    int cur = 0;
                    for (auto& q : procs2) {
                        if (!q.is_running) continue;
                        if (cur == idx) {
                            if (taskmgrSelectedPid == q.pid) taskmgrSelectedPid = -1;
                            else taskmgrSelectedPid = q.pid;
                            break;
                        }
                        cur++;
                    }
                }
                return;
            }
            if (p.is_folder || p.name.find("Explorer") != std::string::npos) {
                scrollPid = p.pid;
                activeDisk = p.diskIndex;
                activeSub[p.diskIndex] = p.subPath;
                int bx = p.x + 8;
                int by = p.y + 38;
                int bw = p.w - 16;
                int bh = p.h - 46;
                int hdrH = 24;
                if (mx >= bx + 4 && mx < bx + 20 && my >= by + 4 && my < by + 20) {
                    size_t pos = p.subPath.rfind('/');
                    std::string up = (pos == std::string::npos) ? "" : p.subPath.substr(0, pos);
                    kernel->setFolderState(p.pid, p.diskIndex, up);
                    activeSub[p.diskIndex] = up;
                    scrollY[p.diskIndex] = 0;
                    explorerSelRow = -1;
                    explorerSelPid = -1;
                    return;
                }
                if (mx >= bx && mx < bx + bw && my >= by + hdrH && my < by + bh) {
                    int rowH = 24;
                    int rowY = by + hdrH + 4 - scrollY[p.diskIndex];
                    auto files = fs->list(p.diskIndex, p.subPath);
                    int idx = 0;
                    for (auto& f : files) {
                        if (my >= rowY && my < rowY + rowH) {
                            explorerSelPid = p.pid;
                            explorerSelRow = idx;
                            scrollWinPid = p.pid;
                            scrollWinAccum = 0;
                            return;
                        }
                        rowY += rowH;
                        ++idx;
                    }
                }
                return;
            }
            return;
        }
    }

    int ix = 24, sz = 48, step = 88;
    if (mx >= ix && mx < ix + sz && my >= 24 && my < 24 + sz) { spawnExplorer(0, ""); return; }
    if (mx >= ix && mx < ix + sz && my >= 24 + step && my < 24 + step + sz) { spawnExplorer(1, ""); return; }
    if (mx >= ix && mx < ix + sz && my >= 24 + 2 * step && my < 24 + 2 * step + sz) { spawnTerminal(); return; }
    if (mx >= ix && mx < ix + sz && my >= 24 + 3 * step && my < 24 + 3 * step + sz) {
        kernel->spawn("Readme", true, false, false, 220, 140, 480, 360, true, "");
        return;
    }
}

void BinaryOsCore::handleMouseUp(int mx, int my) {
    (void)mx; (void)my;
    dragPid = -1;
    scrollPid = -1;
    scrollWinPid = -1;
}

void BinaryOsCore::handleRightClick(int mx, int my) {
    (void)mx; (void)my;
    contextMenuOpen = false;
    startOpen = false;
    rbMenuOpen = false;
}

void BinaryOsCore::setTouch(int x, int y, bool pressed, int pointerCount) {
    writeLong(MMIO_INPUT, (uint64_t)x);
    writeLong(MMIO_INPUT + 8, (uint64_t)y);
    uint64_t btn = 0;
    if (pressed) btn = (pointerCount >= 2) ? 2 : 1;
    writeLong(MMIO_INPUT + 16, btn);
    if (!pressed) return;
    auto now = std::chrono::steady_clock::now();

    if (mouseState == MouseState::IDLE) {
        if (pointerCount >= 2) {
            mouseState = MouseState::RIGHT_PENDING;
            rightDownAt = now;
            rightDownX = x; rightDownY = y;
            touchLastX = x; touchLastY = y;
            rbStartX = x; rbStartY = y;
            rbCurX = x; rbCurY = y;
        } else {
            mouseState = MouseState::LEFT_PENDING;
            leftDownAt = now;
            leftDownX = x; leftDownY = y;
            touchLastX = x; touchLastY = y;
        }
        return;
    }

    if (mouseState == MouseState::LEFT_PENDING) {
        int mdx = x - leftDownX;
        int mdy = y - leftDownY;
        if (mdx * mdx + mdy * mdy > 400) {
            mouseState = MouseState::LEFT_DRAG;
            leftButtonDown = true;
            handleMouseDown(cursorX, cursorY, true);
            touchLastX = x; touchLastY = y;
        }
        return;
    }

    if (mouseState == MouseState::LEFT_DRAG) {
        int dx = x - touchLastX;
        int dy = y - touchLastY;
        if (scrollWinPid >= 0) {
            scrollWinAccum += dy;
            int lines = scrollWinAccum / 12;
            if (lines != 0) {
                auto procs = kernel->snapshot();
                for (auto& p : procs) {
                    if (p.pid == scrollWinPid && p.is_running) {
                        scrollY[p.diskIndex] -= lines * 24;
                        if (scrollY[p.diskIndex] < 0) scrollY[p.diskIndex] = 0;
                        if (scrollY[p.diskIndex] > 4000) scrollY[p.diskIndex] = 4000;
                        break;
                    }
                }
                scrollWinAccum -= lines * 12;
            }
            touchLastX = x; touchLastY = y;
            return;
        }
        float fdx = (float)dx, fdy = (float)dy;
        velX = velX * 0.7f + fdx * 0.3f;
        velY = velY * 0.7f + fdy * 0.3f;
        int nx = cursorX + (int)(velX * 1.5f);
        int ny = cursorY + (int)(velY * 1.5f);
        if (nx < 0) { nx = 0; velX = 0; }
        if (nx >= width) { nx = width - 1; velX = 0; }
        if (ny < 0) { ny = 0; velY = 0; }
        if (ny >= height) { ny = height - 1; velY = 0; }
        cursorX = nx; cursorY = ny;
        if (dragPid >= 0) kernel->moveWindow(dragPid, cursorX - dragOffX, cursorY - dragOffY);
        touchLastX = x; touchLastY = y;
        return;
    }

    if (mouseState == MouseState::RIGHT_PENDING) {
        float held = std::chrono::duration<float>(now - rightDownAt).count();
        int mdx = x - rightDownX;
        int mdy = y - rightDownY;
        if (held >= 0.6f && mdx * mdx + mdy * mdy < 400) {
            mouseState = MouseState::RIGHT_DRAG;
            rightButtonDown = true;
            rbCurX = x; rbCurY = y;
            touchLastX = x; touchLastY = y;
            return;
        }
        if (mdx * mdx + mdy * mdy >= 400) {
            mouseState = MouseState::SCROLL;
            touchLastX = x; touchLastY = y;
        }
        return;
    }

    if (mouseState == MouseState::RIGHT_DRAG) {
        rbCurX = x; rbCurY = y;
        touchLastX = x; touchLastY = y;
        return;
    }

    if (mouseState == MouseState::SCROLL) {
        int dy = y - touchLastY;
        scrollAccum += dy;
        int lines = scrollAccum / 15;
        if (lines != 0) {
            scrollY[activeDisk] -= lines * 24;
            if (scrollY[activeDisk] < 0) scrollY[activeDisk] = 0;
            if (scrollY[activeDisk] > 4000) scrollY[activeDisk] = 4000;
            scrollAccum -= lines * 15;
        }
        touchLastX = x; touchLastY = y;
        return;
    }
}

void BinaryOsCore::publicSetTouch(int x, int y, bool pressed, int pointerCount) {
    if (pressed) { setTouch(x, y, pressed, pointerCount); return; }
    auto now = std::chrono::steady_clock::now();
    if (mouseState == MouseState::LEFT_PENDING) {
        float dt = std::chrono::duration<float>(now - leftDownAt).count();
        if (dt < 0.4f) {
            handleMouseDown(cursorX, cursorY, false);
            handleMouseUp(cursorX, cursorY);
        }
    } else if (mouseState == MouseState::LEFT_DRAG) {
        handleMouseUp(cursorX, cursorY);
    } else if (mouseState == MouseState::RIGHT_PENDING) {
        float dt = std::chrono::duration<float>(now - rightDownAt).count();
        int mdx = x - rightDownX;
        int mdy = y - rightDownY;
        if (dt < 0.3f && mdx * mdx + mdy * mdy < 400) handleRightClick(cursorX, cursorY);
    } else if (mouseState == MouseState::RIGHT_DRAG) {
        int x0 = std::min(rbStartX, rbCurX);
        int y0 = std::min(rbStartY, rbCurY);
        int w = std::abs(rbCurX - rbStartX);
        int h = std::abs(rbCurY - rbStartY);
        if (w >= 40 && h >= 40) {
            rbMenuOpen = true;
            rbMenuX = x0 + w / 2;
            rbMenuY = y0 + h / 2;
            rbRectX = x0; rbRectY = y0; rbRectW = w; rbRectH = h;
        }
    }
    mouseState = MouseState::IDLE;
    writeLong(MMIO_INPUT + 16, 0);
    leftButtonDown = false;
    rightButtonDown = false;
    dragPid = -1;
    scrollPid = -1;
    scrollWinPid = -1;
    leftDown = false;
    rightDown = false;
    velX = 0; velY = 0;
    scrollAccum = 0;
    scrollWinAccum = 0;
}

void BinaryOsCore::publicClearTouch() { clearTouch(); }

void BinaryOsCore::clearTouch() {
    writeLong(MMIO_INPUT + 16, 0);
    mouseState = MouseState::IDLE;
    dragPid = -1;
    scrollPid = -1;
    scrollWinPid = -1;
    leftDown = false;
    rightDown = false;
    leftButtonDown = false;
    rightButtonDown = false;
    velX = 0; velY = 0;
    scrollAccum = 0;
    scrollWinAccum = 0;
}