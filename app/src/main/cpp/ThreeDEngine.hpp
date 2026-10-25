#ifndef THREE_D_ENGINE_HPP
#define THREE_D_ENGINE_HPP
#include <vector>
#include <cmath>
#include <algorithm>

class ThreeDEngine {
public:
    static void drawTriangle(std::vector<uint32_t>& pixels, int width, int height, 
                             int x0, int y0, int x1, int y1, int x2, int y2, uint32_t color) {
        int minX = std::max(0, std::min({x0, x1, x2}));
        int maxX = std::min(width - 1, std::max({x0, x1, x2}));
        int minY = std::max(0, std::min({y0, y1, y2}));
        int maxY = std::min(height - 1, std::max({y0, y1, y2}));

        auto edge = [](int ax, int ay, int bx, int by, int cx, int cy) {
            return (cx - ax) * (by - ay) - (cy - ay) * (bx - ax);
        };

        int area = edge(x0, y0, x1, y1, x2, y2);
        if (area == 0) return;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int w0 = edge(x1, y1, x2, y2, x, y);
                int w1 = edge(x2, y2, x0, y0, x, y);
                int w2 = edge(x0, y0, x1, y1, x, y);
                if ((w0 >= 0 && w1 >= 0 && w2 >= 0) || (w0 <= 0 && w1 <= 0 && w2 <= 0)) {
                    pixels[y * width + x] = color;
                }
            }
        }
    }

    static void drawCube(std::vector<uint32_t>& pixels, int width, int height, float angle, int cx, int cy) {
        float vertices[8][3] = {
            {-1,-1,-1}, {1,-1,-1}, {1,1,-1}, {-1,1,-1},
            {-1,-1, 1}, {1,-1, 1}, {1,1, 1}, {-1,1, 1}
        };
        int edges[12][2] = { {0,1},{1,2},{2,3},{3,0}, {4,5},{5,6},{6,7},{7,4}, {0,4},{1,5},{2,6},{3,7} };
        
        float rad = angle * 3.14159f / 180.0f;
        float cosA = cos(rad), sinA = sin(rad);
        float proj[8][2];

        for (int i = 0; i < 8; i++) {
            float x = vertices[i][0] * cosA - vertices[i][2] * sinA;
            float z = vertices[i][0] * sinA + vertices[i][2] * cosA;
            float y = vertices[i][1] * cosA - z * sinA;
            z = vertices[i][1] * sinA + z * cosA;

            float scale = 50.0f / (z + 4.0f); 
            proj[i][0] = cx + x * scale;
            proj[i][1] = cy + y * scale;
        }

        auto drawLine = [&](int x0, int y0, int x1, int y1, uint32_t color) {
            int dx = abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
            int dy = -abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
            int err = dx + dy, e2;
            while (true) {
                if (x0 >= 0 && x0 < width && y0 >= 0 && y0 < height) pixels[y0 * width + x0] = color;
                if (x0 == x1 && y0 == y1) break;
                e2 = 2 * err;
                if (e2 >= dy) { err += dy; x0 += sx; }
                if (e2 <= dx) { err += dx; y0 += sy; }
            }
        };

        for (int i = 0; i < 12; i++) {
            drawLine((int)proj[edges[i][0]][0], (int)proj[edges[i][0]][1], (int)proj[edges[i][1]][0], (int)proj[edges[i][1]][1], 0xFF00FFFF);
        }
    }
};
#endif