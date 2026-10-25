#include "BinaryOsCore.h"
#include <ctime>

void BinaryOsCore::drawDesktop() {
    verticalGradient(0, 0, width, height, 0xFF1B4F8A, 0xFF0B1E3A);
    for (int i = 0; i < 60; ++i) {
        int sx = (int)((i * 137 + 31) % width);
        int sy = (int)((i * 89 + 17) % (height / 2));
        mixPixel(sx, sy, 0xFFFFFFFF, 0.35f);
    }
}

void BinaryOsCore::drawDesktopIcons() {
    int ix = 24;
    int iy = 24;
    int sz = 48;
    int step = 88;
    drawIcon(7, ix, iy, sz);
    drawString(ix, iy + sz + 4, "C:", 0xFFFFFFFF, 1);
    drawIcon(7, ix, iy + step, sz);
    drawString(ix, iy + step + sz + 4, "D:", 0xFFFFFFFF, 1);
    drawIcon(5, ix, iy + 2 * step, sz);
    drawString(ix, iy + 2 * step + sz + 4, "Terminal", 0xFFFFFFFF, 1);
    drawIcon(1, ix, iy + 3 * step, sz);
    drawString(ix, iy + 3 * step + sz + 4, "Readme", 0xFFFFFFFF, 1);
}

void BinaryOsCore::drawTaskbar() {
    int tbH = 48;
    int tbY = height - tbH;
    fillRect(0, tbY, width, tbH, 0xFF2C2C2C);
    fillRect(0, tbY, width, 1, 0xFF3A3A3A);
    uint32_t startColor = startOpen ? 0xFF1E5EA8 : 0xFF3A8EE6;
    roundRect(8, tbY + 8, 100, tbH - 16, 4, startColor);
    drawStringBold(20, tbY + 16, "START", 0xFFFFFFFF, 1);
    roundRect(114, tbY + 8, 80, tbH - 16, 4, taskmgrOpen ? 0xFF1E5EA8 : 0xFF3A8EE6);
    drawStringBold(122, tbY + 16, "TASKMGR", 0xFFFFFFFF, 1);
    auto procs = kernel->snapshot();
    int bx = 200;
    for (auto& p : procs) {
        if (!p.is_running || !p.is_windowed) continue;
        int bw = 150;
        if (bx + bw > width - 100) break;
        roundRect(bx, tbY + 8, bw, tbH - 16, 4, 0xFF2A2A2A);
        drawString(bx + 10, tbY + 16, p.name.substr(0, 12), 0xFFFFFFFF, 1);
        bx += bw + 6;
    }
    time_t t = time(nullptr);
    struct tm* lt = localtime(&t);
    char clock[16];
    snprintf(clock, sizeof(clock), "%02d:%02d", lt ? lt->tm_hour : 0, lt ? lt->tm_min : 0);
    drawString(width - 60, tbY + 16, clock, 0xFFFFFFFF, 1);
}

void BinaryOsCore::drawStartMenu() {
    if (!startOpen) return;
    int mw = 240;
    int mh = 280;
    int mx = 8;
    int my = height - 48 - mh - 8;
    castShadow(mx, my, mw, mh);
    roundRect(mx, my, mw, mh, 6, 0xFF202020);
    fillRect(mx, my, mw, 36, 0xFF2D7DD2);
    drawStringBold(mx + 12, my + 12, "Programs", 0xFFFFFFFF, 1);
    const char* items[] = {"C: Drive", "D: Drive", "Terminal", "TaskMgr", "Readme", "Shutdown"};
    int iy = my + 48;
    for (int i = 0; i < 6; ++i) {
        if (iy + 30 > my + mh - 8) break;
        roundRect(mx + 8, iy, mw - 16, 26, 3, 0xFF2E2E2E);
        drawString(mx + 18, iy + 8, items[i], 0xFFFFFFFF, 1);
        iy += 30;
    }
}

void BinaryOsCore::drawWindowFrame(const Process& p) {
    castShadow(p.x, p.y, p.w, p.h);
    fillRect(p.x, p.y, p.w, p.h, 0xFFF0F0F0);
    int titleH = 30;
    roundRectTop(p.x, p.y, p.w, titleH, 8, 0xFF2D7DD2);
    drawStringBold(p.x + 12, p.y + 10, p.name, 0xFFFFFFFF, 1);
    int closeX = p.x + p.w - 28;
    int closeY = p.y + 6;
    roundRect(closeX, closeY, 22, 18, 3, 0xFFE74C3C);
    fillRect(closeX + 6, closeY + 8, 10, 2, 0xFFFFFFFF);
    fillRect(closeX + 10, closeY + 4, 2, 10, 0xFFFFFFFF);
    int maxX = p.x + p.w - 54;
    int maxY = p.y + 6;
    roundRect(maxX, maxY, 22, 18, 3, p.maximized ? 0xFF27AE60 : 0xFF3498DB);
    if (p.maximized) {
        fillRect(maxX + 5, maxY + 4, 8, 8, 0xFFFFFFFF);
        fillRect(maxX + 8, maxY + 7, 8, 8, 0xFF3498DB);
        fillRect(maxX + 8, maxY + 7, 8, 1, 0xFFFFFFFF);
        fillRect(maxX + 8, maxY + 14, 8, 1, 0xFFFFFFFF);
        fillRect(maxX + 8, maxY + 7, 1, 8, 0xFFFFFFFF);
        fillRect(maxX + 15, maxY + 7, 1, 8, 0xFFFFFFFF);
    } else {
        fillRect(maxX + 6, maxY + 5, 10, 8, 0xFFFFFFFF);
        fillRect(maxX + 6, maxY + 5, 10, 1, 0xFF3498DB);
        fillRect(maxX + 6, maxY + 12, 10, 1, 0xFF3498DB);
        fillRect(maxX + 6, maxY + 5, 1, 8, 0xFF3498DB);
        fillRect(maxX + 15, maxY + 5, 1, 8, 0xFF3498DB);
    }
}

void BinaryOsCore::drawExplorerContents(const Process& p) {
    int bx = p.x + 8;
    int by = p.y + 38;
    int bw = p.w - 16;
    int bh = p.h - 46;
    fillRect(bx, by, bw, bh, 0xFFFFFFFF);
    int hdrH = 24;
    fillRect(bx, by, bw, hdrH, 0xFFE8E8E8);
    fillRect(bx, by, bw, 1, 0xFFBBBBBB);
    roundRect(bx + 4, by + 4, 16, 16, 2, 0xFF3A8EE6);
    drawString(bx + 6, by + 8, "<", 0xFFFFFFFF, 1);
    std::string label = "C:\\";
    if (p.diskIndex == 1) label = "D:\\";
    if (!p.subPath.empty()) label += p.subPath;
    drawString(bx + 28, by + 8, label, 0xFF333333, 1);
    auto files = fs->list(p.diskIndex, p.subPath);
    int rowH = 24;
    int rowY = by + hdrH + 4 - scrollY[p.diskIndex];
    int shown = 0;
    int idx = 0;
    for (auto& f : files) {
        if (rowY + rowH > by + hdrH && rowY < by + bh) {
            bool hover = (cursorX >= bx && cursorX < bx + bw && cursorY >= rowY && cursorY < rowY + rowH);
            bool selected = (explorerSelPid == p.pid && explorerSelRow == idx);
            uint32_t rowBg = (shown % 2 == 0) ? 0xFFFFFFFF : 0xFFF5F5F5;
            if (hover) rowBg = 0xFFE0ECFF;
            if (selected) rowBg = 0xFFCCE4FF;
            fillRect(bx + 2, rowY, bw - 4, rowH, rowBg);
            drawIcon(f.iconType, bx + 8, rowY + 4, 16);
            drawString(bx + 32, rowY + 8, f.name, (hover || selected) ? 0xFF0078D7 : 0xFF222222, 1);
            char sizeBuf[32];
            if (f.isDir) snprintf(sizeBuf, sizeof(sizeBuf), "<DIR>");
            else if (f.size < 1024) snprintf(sizeBuf, sizeof(sizeBuf), "%llu B", (unsigned long long)f.size);
            else if (f.size < 1024ull * 1024ull) snprintf(sizeBuf, sizeof(sizeBuf), "%.1f KB", f.size / 1024.0);
            else snprintf(sizeBuf, sizeof(sizeBuf), "%.1f MB", f.size / (1024.0 * 1024.0));
            drawString(bx + bw - 160, rowY + 8, sizeBuf, (hover || selected) ? 0xFF0078D7 : 0xFF666666, 1);
        }
        rowY += rowH;
        ++shown;
        ++idx;
    }
    if (files.empty()) {
        int err = fs->lastErrno;
        if (err != 0) {
            char buf[128];
            snprintf(buf, sizeof(buf), "Permission denied (errno %d)", err);
            drawString(bx + 12, by + 60, buf, 0xFFCC2222, 1);
        } else {
            drawString(bx + 12, by + 60, "Empty folder", 0xFF666666, 1);
        }
    }
}

void BinaryOsCore::drawRegEditContents(const Process& p) {
    int bx = p.x + 8;
    int by = p.y + 38;
    int bw = p.w - 16;
    int bh = p.h - 46;
    if (bw <= 0 || bh <= 0) return;
    fillRect(bx, by, bw, bh, 0xFFFFFFFF);
    int leftW = bw / 3;
    fillRect(bx, by, leftW, bh, 0xFFF7F7F7);
    fillRect(bx + leftW, by, 1, bh, 0xFFDDDDDD);
    auto keys = reg->listKeys();
    int ky = by + 8;
    for (auto& k : keys) {
        if (ky + 18 > by + bh - 4) break;
        bool selected = (k == regEditKey);
        if (selected) fillRect(bx + 2, ky - 2, leftW - 4, 18, 0xFFCFE4FF);
        drawString(bx + 8, ky, k, selected ? 0xFF0A3D91 : 0xFF222222, 1);
        ky += 18;
    }
    int vx = bx + leftW + 8;
    int vy = by + 8;
    drawStringBold(vx, vy, regEditKey, 0xFF0A3D91, 1);
    vy += 20;
    fillRect(vx, vy, bw - leftW - 16, 1, 0xFFDDDDDD);
    vy += 6;
    drawString(vx, vy, "Name", 0xFF555555, 1);
    drawString(vx + 192, vy, "Type", 0xFF555555, 1);
    drawString(vx + 272, vy, "Data", 0xFF555555, 1);
    vy += 16;
    fillRect(vx, vy, bw - leftW - 16, 1, 0xFFDDDDDD);
    vy += 4;
    auto values = reg->listValues(regEditKey);
    for (auto& kv : values) {
        if (vy + 16 > by + bh - 4) break;
        const Registry::Value& v = kv.second;
        const char* typeName = (v.type == Registry::TYPE_DWORD) ? "DWORD" : (v.type == Registry::TYPE_SZ) ? "SZ" : "BINARY";
        char dataBuf[128];
        if (v.type == Registry::TYPE_DWORD) snprintf(dataBuf, sizeof(dataBuf), "0x%08X (%u)", v.dword, v.dword);
        else if (v.type == Registry::TYPE_SZ) snprintf(dataBuf, sizeof(dataBuf), "%s", v.sz.c_str());
        else snprintf(dataBuf, sizeof(dataBuf), "<%u bytes>", (unsigned)v.bin.size());
        drawString(vx, vy, kv.first, 0xFF222222, 1);
        drawString(vx + 192, vy, typeName, 0xFF666666, 1);
        drawString(vx + 272, vy, dataBuf, 0xFF222222, 1);
        vy += 16;
    }
}

void BinaryOsCore::drawNotepadContents(const Process& p) {
    int bx = p.x + 8;
    int by = p.y + 38;
    int bw = p.w - 16;
    int bh = p.h - 46;
    fillRect(bx, by, bw, bh, 0xFFFFFFFF);
    fillRect(bx, by, bw, 1, 0xFFBBBBBB);
    std::string content = fs->readHead(p.path, 16384);
    int lineH = 14;
    int y = by + 8;
    int x = bx + 8;
    size_t i = 0;
    while (i < content.size() && y + lineH < by + bh) {
        char c = content[i];
        if (c == '\n') { x = bx + 8; y += lineH; ++i; continue; }
        if (c == '\r') { ++i; continue; }
        if (c >= 32 && c < 127) {
            blitChar(x, y, (uint8_t)c, 0xFF222222, 1);
            x += 8;
            if (x + 8 > bx + bw - 8) { x = bx + 8; y += lineH; }
        } else {
            size_t len = 1;
            uint8_t u = (uint8_t)c;
            if ((u & 0xE0) == 0xC0) len = 2;
            else if ((u & 0xF0) == 0xE0) len = 3;
            else if ((u & 0xF8) == 0xF0) len = 4;
            if (i + len > content.size()) break;
            blitChar(x, y, (uint8_t)'?', 0xFF222222, 1);
            x += 8;
            i += len;
            if (x + 8 > bx + bw - 8) { x = bx + 8; y += lineH; }
            continue;
        }
        ++i;
    }
    if (content.empty()) drawString(bx + 8, by + 8, "(empty)", 0xFF888888, 1);
}

void BinaryOsCore::drawBrowserContents(const Process& p) {
    int bx = p.x + 8;
    int by = p.y + 38;
    int bw = p.w - 16;
    int bh = p.h - 46;
    fillRect(bx, by, bw, bh, 0xFFFFFFFF);
    fillRect(bx, by, bw, 24, 0xFFDDDDDD);
    drawString(bx + 8, by + 8, "https://example.com", 0xFF333333, 1);
    drawString(bx + 8, by + 36, "Network Connected.", 0xFF009933, 1);
}

void BinaryOsCore::drawSandbox3D(const Process& p) {
    int bx = p.x + 8;
    int by = p.y + 38;
    int bw = p.w - 16;
    int bh = p.h - 46;
    if (bw <= 0 || bh <= 0) return;
    auto now = std::chrono::steady_clock::now();
    float dt = std::chrono::duration<float>(now - rtLast).count();
    rtLast = now;
    rtAngle += dt * 30.0f;
    if (rtAngle > 360.0f) rtAngle -= 360.0f;
    if (rtW <= 0 || rtH <= 0) return;
    tracer->render(rtBuffer, rtW, rtH, rtAngle);
    for (int y = 0; y < bh; ++y) {
        int sy = by + y;
        if (sy < 0 || sy >= height) continue;
        float fy = (float)y / bh * rtH;
        int iy0 = (int)fy;
        int iy1 = std::min(rtH - 1, iy0 + 1);
        float wy = fy - iy0;
        for (int x = 0; x < bw; ++x) {
            int sx = bx + x;
            if (sx < 0 || sx >= width) continue;
            float fx = (float)x / bw * rtW;
            int ix0 = (int)fx;
            int ix1 = std::min(rtW - 1, ix0 + 1);
            float wx = fx - ix0;
            uint32_t c00 = rtBuffer[(size_t)iy0 * rtW + ix0];
            uint32_t c10 = rtBuffer[(size_t)iy0 * rtW + ix1];
            uint32_t c01 = rtBuffer[(size_t)iy1 * rtW + ix0];
            uint32_t c11 = rtBuffer[(size_t)iy1 * rtW + ix1];
            uint32_t top = lerpColor(c00, c10, wx);
            uint32_t bot = lerpColor(c01, c11, wx);
            screenPixels[(size_t)sy * width + sx] = lerpColor(top, bot, wy);
        }
    }
}

void BinaryOsCore::drawTaskMgrContents(const Process& p) {
    int bx = p.x + 8;
    int by = p.y + 38;
    int bw = p.w - 16;
    int bh = p.h - 46;
    fillRect(bx, by, bw, bh, 0xFF1E1E1E);
    drawStringBold(bx + 8, by + 6, "PID   PROCESS          MEM", 0xFF00FF66, 1);
    fillRect(bx + 8, by + 20, bw - 16, 1, 0xFF00AA33);
    auto procs = kernel->snapshot();
    bool selAlive = false;
    for (auto& q : procs) if (q.is_running && q.pid == taskmgrSelectedPid) { selAlive = true; break; }
    if (!selAlive) taskmgrSelectedPid = -1;
    int ry = by + 28;
    int rowH = 20;
    int maxRows = (bh - 160) / rowH;
    if (maxRows < 1) maxRows = 1;
    int shown = 0;
    for (auto& q : procs) {
        if (!q.is_running) continue;
        if (shown >= maxRows) break;
        bool selected = (q.pid == taskmgrSelectedPid);
        bool hover = (cursorX >= bx && cursorX < bx + bw && cursorY >= ry && cursorY < ry + rowH);
        if (selected) fillRect(bx + 4, ry - 2, bw - 8, rowH, 0xFF2D5A2D);
        else if (hover) fillRect(bx + 4, ry - 2, bw - 8, rowH, 0xFF2A3A2A);
        int cbX = bx + 8;
        int cbY = ry + 3;
        fillRect(cbX, cbY, 12, 12, 0xFF000000);
        fillRect(cbX, cbY, 12, 1, 0xFF00AA33);
        fillRect(cbX, cbY + 11, 12, 1, 0xFF00AA33);
        fillRect(cbX, cbY, 1, 12, 0xFF00AA33);
        fillRect(cbX + 11, cbY, 1, 12, 0xFF00AA33);
        if (selected) fillRect(cbX + 3, cbY + 3, 6, 6, 0xFF00FF66);
        char line[96];
        snprintf(line, sizeof(line), "%-5d %-16s %6lluK", q.pid, q.name.substr(0, 16).c_str(),
                 (unsigned long long)(q.mem_bytes / 1024));
        uint32_t rowColor = selected ? 0xFF00FF66 : (hover ? 0xFF0078D7 : 0xFFCCFFCC);
        drawString(bx + 28, ry, line, rowColor, 1);
        ry += rowH;
        ++shown;
    }
    int btnW = 120;
    int btnH = 24;
    int btnX = bx + 8;
    int btnY = by + bh - btnH - 8;
    bool btnHover = (cursorX >= btnX && cursorX < btnX + btnW && cursorY >= btnY && cursorY < btnY + btnH);
    bool btnEnabled = (taskmgrSelectedPid >= 0);
    uint32_t btnColor = !btnEnabled ? 0xFF3A3A3A : (btnHover ? 0xFFE74C3C : 0xFFC0392B);
    roundRect(btnX, btnY, btnW, btnH, 3, btnColor);
    drawString(btnX + 12, btnY + 8, "End Process", btnEnabled ? 0xFFFFFFFF : 0xFF777777, 1);
    int graphH = 80;
    int graphY = by + bh - graphH - btnH - 16;
    int graphX = bx + 8;
    int graphW = bw - 16;
    fillRect(graphX, graphY, graphW, graphH, 0xFF000000);
    fillRect(graphX, graphY, graphW, 1, 0xFF00AA33);
    fillRect(graphX, graphY + graphH - 1, graphW, 1, 0xFF00AA33);
    fillRect(graphX, graphY, 1, graphH, 0xFF00AA33);
    fillRect(graphX + graphW - 1, graphY, 1, graphH, 0xFF00AA33);
    uint64_t maxMem = 1;
    for (auto v : memHistory) if (v > maxMem) maxMem = v;
    int n = (int)memHistory.size();
    if (n > graphW) n = graphW;
    int start = (int)memHistory.size() - n;
    if (start < 0) start = 0;
    int prevX = graphX;
    int prevY = graphY + graphH - 4;
    for (int i = 0; i < n; ++i) {
        uint64_t v = memHistory[start + i];
        int px = graphX + i * graphW / std::max(1, n - 1);
        int py = graphY + graphH - 4 - (int)((v * (graphH - 8)) / maxMem);
        if (py < graphY + 2) py = graphY + 2;
        if (i > 0) {
            int ddx = std::abs(px - prevX);
            int ddy = std::abs(py - prevY);
            int steps = std::max(ddx, ddy);
            for (int s = 0; s <= steps; ++s) {
                float t = steps > 0 ? (float)s / steps : 0.0f;
                int lx = (int)(prevX + (px - prevX) * t);
                int ly = (int)(prevY + (py - prevY) * t);
                if (lx >= graphX && lx < graphX + graphW && ly >= graphY && ly < graphY + graphH)
                    screenPixels[(size_t)ly * width + lx] = 0xFF00FF66;
            }
        }
        prevX = px;
        prevY = py;
    }
    char memBuf[64];
    snprintf(memBuf, sizeof(memBuf), "MEM %lluK", (unsigned long long)(kernel->totalMemory() / 1024));
    drawString(graphX + 4, graphY + 4, memBuf, 0xFF00FF66, 1);
}

void BinaryOsCore::drawTerminalContents(const Process& p) {
    int bx = p.x + 8;
    int by = p.y + 38;
    int bw = p.w - 16;
    int bh = p.h - 46;
    fillRect(bx, by, bw, bh, 0xFF0C0C0C);
    int lineH = 16;
    int maxLines = (bh - lineH - 8) / lineH;
    if (maxLines < 1) maxLines = 1;
    int total = (int)terminalHistory.size() + 1;
    int startIdx = total - maxLines;
    if (startIdx < 0) startIdx = 0;
    int y = by + 6;
    for (int i = startIdx; i < total; ++i) {
        std::string cur;
        const std::string* linePtr = nullptr;
        if (i < (int)terminalHistory.size()) linePtr = &terminalHistory[i];
        else { cur = "C:\\> " + terminalInput; linePtr = &cur; }
        drawString(bx + 6, y, *linePtr, 0xFF66FF66, 1);
        y += lineH;
    }
    int cursorBlink = (int)((frameTick / 30) % 2);
    if (cursorBlink) {
        int cx = bx + 6 + (int)(terminalInput.size() + 5) * 8;
        int cy = by + 6 + (int)(total - startIdx - 1) * lineH;
        fillRect(cx, cy, 6, 12, 0xFF66FF66);
    }
}

void BinaryOsCore::drawContextMenu() {
    if (!contextMenuOpen) return;
    const char* items[] = {"Rename", "Copy", "Move", "SysMirror"};
    int iw = 140;
    int ih = 4 * 24 + 8;
    int ix = ctxX;
    int iy = ctxY;
    if (ix + iw > width) ix = width - iw;
    if (iy + ih > height) iy = height - ih;
    castShadow(ix, iy, iw, ih);
    roundRect(ix, iy, iw, ih, 4, 0xFFF7F7F7);
    fillRect(ix + 1, iy + 1, iw - 2, 1, 0xFFFFFFFF);
    for (int i = 0; i < 4; ++i) {
        int ry = iy + 4 + i * 24;
        bool hover = (cursorX >= ix && cursorX < ix + iw && cursorY >= ry && cursorY < ry + 24);
        if (hover) fillRect(ix + 2, ry, iw - 4, 24, 0xFFCFE4FF);
        drawString(ix + 8, ry + 8, items[i], hover ? 0xFF0A3D91 : 0xFF222222, 1);
    }
}

void BinaryOsCore::drawRenameDialog() {
    if (!renameMode) return;
    int dw = 280;
    int dh = 100;
    int dx = (width - dw) / 2;
    int dy = (height - dh) / 2;
    castShadow(dx, dy, dw, dh);
    roundRect(dx, dy, dw, dh, 6, 0xFFF7F7F7);
    roundRectTop(dx, dy, dw, 28, 6, 0xFF2D7DD2);
    drawStringBold(dx + 10, dy + 10, "Rename", 0xFFFFFFFF, 1);
    fillRect(dx + 12, dy + 40, dw - 24, 22, 0xFFFFFFFF);
    fillRect(dx + 12, dy + 40, dw - 24, 1, 0xFF888888);
    fillRect(dx + 12, dy + 61, dw - 24, 1, 0xFF888888);
    drawString(dx + 16, dy + 46, renameBuf + "_", 0xFF222222, 1);
    roundRect(dx + dw - 140, dy + dh - 28, 60, 20, 3, 0xFF3A8EE6);
    drawString(dx + dw - 130, dy + dh - 22, "OK", 0xFFFFFFFF, 1);
    roundRect(dx + dw - 70, dy + dh - 28, 60, 20, 3, 0xFFB0B0B0);
    drawString(dx + dw - 60, dy + dh - 22, "Cancel", 0xFFFFFFFF, 1);
}

void BinaryOsCore::drawTaskmgrConfirm() {
    if (!taskmgrConfirmOpen) return;
    int dw = 300;
    int dh = 120;
    int dx = (width - dw) / 2;
    int dy = (height - dh) / 2;
    castShadow(dx, dy, dw, dh);
    roundRect(dx, dy, dw, dh, 6, 0xFFF7F7F7);
    roundRectTop(dx, dy, dw, 28, 6, 0xFF2D7DD2);
    drawStringBold(dx + 10, dy + 10, "Task Manager", 0xFFFFFFFF, 1);
    char msg[96];
    const char* pname = "process";
    auto procs = kernel->snapshot();
    for (auto& q : procs) {
        if (q.pid == taskmgrConfirmPid) { pname = q.name.c_str(); break; }
    }
    snprintf(msg, sizeof(msg), "End process %s (PID %d)?", pname, taskmgrConfirmPid);
    drawString(dx + 16, dy + 48, msg, 0xFF222222, 1);
    drawString(dx + 16, dy + 68, "Unsaved data may be lost.", 0xFF666666, 1);
    roundRect(dx + dw - 140, dy + dh - 32, 60, 24, 3, 0xFF3A8EE6);
    drawString(dx + dw - 126, dy + dh - 26, "Yes", 0xFFFFFFFF, 1);
    roundRect(dx + dw - 72, dy + dh - 32, 60, 24, 3, 0xFFB0B0B0);
    drawString(dx + dw - 60, dy + dh - 26, "No", 0xFFFFFFFF, 1);
}

void BinaryOsCore::drawRbMenu() {
    if (!rbMenuOpen) return;
    const char* items[] = {"New Explorer", "New Terminal", "New TaskMgr", "Cancel"};
    int iw = 150;
    int ih = 4 * 24 + 8;
    int ix = rbMenuX - iw / 2;
    int iy = rbMenuY - ih / 2;
    if (ix < 0) ix = 0;
    if (iy < 0) iy = 0;
    if (ix + iw > width) ix = width - iw;
    if (iy + ih > height) iy = height - ih;
    castShadow(ix, iy, iw, ih);
    roundRect(ix, iy, iw, ih, 4, 0xFFF7F7F7);
    fillRect(ix + 1, iy + 1, iw - 2, 1, 0xFFFFFFFF);
    for (int i = 0; i < 4; ++i) {
        int ry = iy + 4 + i * 24;
        bool hover = (cursorX >= ix && cursorX < ix + iw && cursorY >= ry && cursorY < ry + 24);
        if (hover) fillRect(ix + 2, ry, iw - 4, 24, 0xFFCFE4FF);
        drawString(ix + 8, ry + 8, items[i], hover ? 0xFF0A3D91 : 0xFF222222, 1);
    }
}