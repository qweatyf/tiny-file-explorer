#include "BinaryOsCore.h"

void BinaryOsCore::setPixel(int x, int y, uint32_t c) {
    if (x >= 0 && x < width && y >= 0 && y < height)
        screenPixels[(size_t)y * width + x] = c;
}

void BinaryOsCore::mixPixel(int x, int y, uint32_t c, float alpha) {
    if (x < 0 || x >= width || y < 0 || y >= height) return;
    if (alpha <= 0.0f) return;
    if (alpha >= 1.0f) { screenPixels[(size_t)y * width + x] = c; return; }
    uint32_t bg = screenPixels[(size_t)y * width + x];
    int r = (int)(((c >> 16) & 0xFF) * alpha + ((bg >> 16) & 0xFF) * (1.0f - alpha));
    int g = (int)(((c >> 8) & 0xFF) * alpha + ((bg >> 8) & 0xFF) * (1.0f - alpha));
    int b = (int)((c & 0xFF) * alpha + (bg & 0xFF) * (1.0f - alpha));
    screenPixels[(size_t)y * width + x] = 0xFF000000u | ((uint32_t)r << 16) | ((uint32_t)g << 8) | (uint32_t)b;
}

void BinaryOsCore::blitChar(int x, int y, uint8_t ch, uint32_t color, int scale) {
    if (ch >= 128) return;
    if (ch == 0x20) return;
    const uint8_t* bm = BinaryOS::FONT_8X8[ch];
    for (int row = 0; row < 8; ++row) {
        uint8_t bits = bm[row];
        for (int col = 0; col < 8; ++col) {
            if (bits & (1 << col)) {
                for (int sy = 0; sy < scale; ++sy)
                    for (int sx = 0; sx < scale; ++sx)
                        setPixel(x + col * scale + sx, y + row * scale + sy, color);
            }
        }
    }
}

void BinaryOsCore::drawString(int x, int y, const std::string& s, uint32_t color, int scale) {
    int cx = x;
    size_t i = 0;
    while (i < s.size()) {
        uint8_t c = (uint8_t)s[i];
        if (c < 0x80) {
            blitChar(cx, y, c, color, scale);
            cx += 8 * scale;
            ++i;
        } else {
            size_t len = 1;
            if ((c & 0xE0) == 0xC0) len = 2;
            else if ((c & 0xF0) == 0xE0) len = 3;
            else if ((c & 0xF8) == 0xF0) len = 4;
            if (i + len > s.size()) break;
            for (size_t k = 0; k < len; ++k) blitChar(cx, y, (uint8_t)'?', color, scale);
            cx += 8 * scale;
            i += len;
        }
    }
}

void BinaryOsCore::drawStringBold(int x, int y, const std::string& s, uint32_t color, int scale) {
    drawString(x, y, s, color, scale);
    drawString(x + 1, y, s, color, scale);
}

void BinaryOsCore::fillRect(int rx, int ry, int rw, int rh, uint32_t c) {
    int x0 = std::max(0, rx);
    int y0 = std::max(0, ry);
    int x1 = std::min(width, rx + rw);
    int y1 = std::min(height, ry + rh);
    for (int y = y0; y < y1; ++y) {
        uint32_t* row = &screenPixels[(size_t)y * width];
        for (int x = x0; x < x1; ++x) row[x] = c;
    }
}

void BinaryOsCore::verticalGradient(int rx, int ry, int rw, int rh, uint32_t top, uint32_t bottom) {
    int rT = (top >> 16) & 0xFF, gT = (top >> 8) & 0xFF, bT = top & 0xFF;
    int rB = (bottom >> 16) & 0xFF, gB = (bottom >> 8) & 0xFF, bB = bottom & 0xFF;
    int span = std::max(1, rh - 1);
    for (int y = 0; y < rh; ++y) {
        int sy = ry + y;
        if (sy < 0 || sy >= height) continue;
        float t = (float)y / span;
        int r = (int)(rT + (rB - rT) * t);
        int g = (int)(gT + (gB - gT) * t);
        int b = (int)(bT + (bB - bT) * t);
        uint32_t c = 0xFF000000u | ((uint32_t)r << 16) | ((uint32_t)g << 8) | (uint32_t)b;
        int x0 = std::max(0, rx);
        int x1 = std::min(width, rx + rw);
        uint32_t* row = &screenPixels[(size_t)sy * width];
        for (int x = x0; x < x1; ++x) row[x] = c;
    }
}

void BinaryOsCore::roundRect(int rx, int ry, int rw, int rh, int radius, uint32_t c) {
    if (rw <= 0 || rh <= 0) return;
    int r = std::min(radius, std::min(rw, rh) / 2);
    fillRect(rx + r, ry, rw - 2 * r, rh, c);
    fillRect(rx, ry + r, r, rh - 2 * r, c);
    fillRect(rx + rw - r, ry + r, r, rh - 2 * r, c);
    for (int dy = -r; dy <= r; ++dy) {
        for (int dx = -r; dx <= r; ++dx) {
            int d2 = dx * dx + dy * dy;
            if (d2 > r * r) continue;
            float dist = std::sqrt((float)d2);
            float edge = (float)r - dist;
            float a = edge >= 1.0f ? 1.0f : (edge <= 0.0f ? 0.0f : edge);
            mixPixel(rx + r + dx, ry + r + dy, c, a);
            mixPixel(rx + rw - r + dx, ry + r + dy, c, a);
            mixPixel(rx + r + dx, ry + rh - r + dy, c, a);
            mixPixel(rx + rw - r + dx, ry + rh - r + dy, c, a);
        }
    }
}

void BinaryOsCore::roundRectTop(int rx, int ry, int rw, int rh, int radius, uint32_t c) {
    if (rw <= 0 || rh <= 0) return;
    int r = std::min(radius, std::min(rw, rh) / 2);
    fillRect(rx, ry + r, rw, rh - r, c);
    fillRect(rx + r, ry, rw - 2 * r, r, c);
    for (int dy = 0; dy <= r; ++dy) {
        for (int dx = -r; dx <= r; ++dx) {
            int d2 = dx * dx + (r - dy) * (r - dy);
            if (d2 > r * r) continue;
            float dist = std::sqrt((float)d2);
            float edge = (float)r - dist;
            float a = edge >= 1.0f ? 1.0f : (edge <= 0.0f ? 0.0f : edge);
            mixPixel(rx + r + dx, ry + dy, c, a);
            mixPixel(rx + rw - r + dx, ry + dy, c, a);
        }
    }
}

void BinaryOsCore::castShadow(int wx, int wy, int ww, int wh) {
    for (int i = 1; i <= 6; ++i) {
        float a = 0.12f * (7 - i) / 6.0f;
        int sx = wx - i;
        int sy = wy - i;
        int sw = ww + 2 * i;
        int sh = wh + 2 * i;
        for (int y = sy; y < sy + sh; ++y) {
            if (y < 0 || y >= height) continue;
            if (y >= wy && y < wy + wh) {
                mixPixel(sx, y, 0xFF000000, a);
                mixPixel(sx + sw - 1, y, 0xFF000000, a);
            } else {
                for (int x = sx; x < sx + sw; ++x) mixPixel(x, y, 0xFF000000, a);
            }
        }
    }
}

uint32_t BinaryOsCore::lerpColor(uint32_t a, uint32_t b, float t) {
    int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
    int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
    int r = (int)(ar + (br - ar) * t);
    int g = (int)(ag + (bg - ag) * t);
    int bl = (int)(ab + (bb - ab) * t);
    return 0xFF000000u | ((uint32_t)r << 16) | ((uint32_t)g << 8) | (uint32_t)bl;
}

void BinaryOsCore::drawIcon(int type, int x, int y, int size) {
    uint32_t folder = 0xFFFFC233;
    uint32_t folderEdge = 0xFFD89B1E;
    uint32_t paper = 0xFFFFFFFF;
    uint32_t paperLine = 0xFF8A8A8A;
    uint32_t disk = 0xFF3399FF;
    uint32_t exe = 0xFF66CC66;
    uint32_t img = 0xFFFF6699;
    uint32_t zip = 0xFF9966FF;
    if (type == 0) {
        roundRect(x, y + size / 4, size, size * 3 / 4, 2, folder);
        roundRect(x + size / 8, y + size / 8, size / 2, size / 4, 2, folder);
        fillRect(x, y + size / 4, size, 2, folderEdge);
    } else if (type == 1) {
        fillRect(x + size / 4, y + size / 8, size / 2, size * 3 / 4, paper);
        for (int i = 0; i < 4; ++i)
            fillRect(x + size / 4 + 3, y + size / 4 + i * (size / 8), size / 2 - 6, 1, paperLine);
        fillRect(x + size / 4, y + size / 8, size / 2, 1, 0xFFBBBBBB);
    } else if (type == 2) {
        roundRect(x + size / 8, y + size / 8, size * 3 / 4, size * 3 / 4, 3, img);
        fillRect(x + size / 3, y + size / 3, size / 5, size / 5, 0xFFFFFFFF);
    } else if (type == 5) {
        roundRect(x + size / 8, y + size / 8, size * 3 / 4, size * 3 / 4, 3, exe);
        fillRect(x + size / 3, y + size / 3, size / 4, size / 4, 0xFFFFFFFF);
    } else if (type == 6) {
        roundRect(x + size / 8, y + size / 8, size * 3 / 4, size * 3 / 4, 3, zip);
        fillRect(x + size / 3, y + size / 8, size / 6, size * 3 / 4, 0xFFFFFFFF);
    } else {
        roundRect(x + size / 8, y + size / 8, size * 3 / 4, size * 3 / 4, 3, disk);
        fillRect(x + size / 4, y + size / 4, size / 2, size / 6, 0xFFFFFFFF);
    }
}

void BinaryOsCore::drawCursor() {
    uint32_t fill = 0xFFFFFFFF;
    uint32_t edge = 0xFF000000;
    static const int mask[16][12] = {
        {2,0,0,0,0,0,0,0,0,0,0,0},
        {2,2,0,0,0,0,0,0,0,0,0,0},
        {2,1,2,0,0,0,0,0,0,0,0,0},
        {2,1,1,2,0,0,0,0,0,0,0,0},
        {2,1,1,1,2,0,0,0,0,0,0,0},
        {2,1,1,1,1,2,0,0,0,0,0,0},
        {2,1,1,1,1,1,2,0,0,0,0,0},
        {2,1,1,1,1,1,1,2,0,0,0,0},
        {2,1,1,1,1,1,1,1,2,0,0,0},
        {2,1,1,1,1,1,1,1,1,2,0,0},
        {2,1,1,1,1,1,2,2,2,2,2,0},
        {2,1,1,2,1,1,2,0,0,0,0,0},
        {2,1,2,0,2,1,1,2,0,0,0,0},
        {2,2,0,0,2,1,1,2,0,0,0,0},
        {2,0,0,0,0,2,1,1,2,0,0,0},
        {0,0,0,0,0,2,1,1,2,0,0,0}
    };
    for (int yy = 0; yy < 16; ++yy) {
        for (int xx = 0; xx < 12; ++xx) {
            int m = mask[yy][xx];
            if (m == 0) continue;
            int px = cursorX + xx;
            int py = cursorY + yy;
            if (px < 0 || px >= width || py < 0 || py >= height) continue;
            screenPixels[(size_t)py * width + px] = (m == 2) ? edge : fill;
        }
    }
}

void BinaryOsCore::drawRubberBand() {
    if (mouseState != MouseState::RIGHT_DRAG) return;
    int x0 = std::min(rbStartX, rbCurX);
    int y0 = std::min(rbStartY, rbCurY);
    int w = std::abs(rbCurX - rbStartX);
    int h = std::abs(rbCurY - rbStartY);
    for (int yy = y0; yy < y0 + h; ++yy)
        for (int xx = x0; xx < x0 + w; ++xx)
            mixPixel(xx, yy, 0xFF3A8EE6, 0.18f);
    for (int xx = x0; xx < x0 + w; ++xx) {
        if ((xx / 6) % 2 == 0) { setPixel(xx, y0, 0xFF3A8EE6); setPixel(xx, y0 + h - 1, 0xFF3A8EE6); }
    }
    for (int yy = y0; yy < y0 + h; ++yy) {
        if ((yy / 6) % 2 == 0) { setPixel(x0, yy, 0xFF3A8EE6); setPixel(x0 + w - 1, yy, 0xFF3A8EE6); }
    }
}