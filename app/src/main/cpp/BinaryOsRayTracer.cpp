#include "BinaryOsCore.h"

RayTracer::RayTracer() {
    spheres.push_back({Vec3(-1.1f, 0.0f, 0.0f), 0.55f, 0xFFE74C3C});
    spheres.push_back({Vec3( 1.1f, 0.0f, 0.0f), 0.55f, 0xFF3498DB});
    spheres.push_back({Vec3( 0.0f, 1.1f, 0.4f), 0.45f, 0xFFF1C40F});
    floor.n = Vec3(0, 1, 0);
    floor.d = 1.0f;
    floor.color = 0xFF6B6B6B;
    light = Vec3(-3.0f, 4.0f, -2.0f);
}

bool RayTracer::raySphere(const Vec3& ro, const Vec3& rd, const Sphere& s, float& tOut) {
    Vec3 oc = ro.sub(s.c);
    float b = oc.dot(rd);
    float c = oc.dot(oc) - s.r * s.r;
    float disc = b * b - c;
    if (disc < 0.0f) return false;
    float sq = std::sqrt(disc);
    float t0 = -b - sq;
    float t1 = -b + sq;
    if (t0 > 1e-4f) { tOut = t0; return true; }
    if (t1 > 1e-4f) { tOut = t1; return true; }
    return false;
}

bool RayTracer::rayPlane(const Vec3& ro, const Vec3& rd, const Plane& p, float& tOut) {
    float denom = rd.dot(p.n);
    if (std::fabs(denom) < 1e-6f) return false;
    float t = -(ro.dot(p.n) + p.d) / denom;
    if (t <= 1e-4f) return false;
    tOut = t;
    return true;
}

bool RayTracer::inShadow(const Vec3& ro, const Vec3& rd) {
    for (auto& s : spheres) {
        float t;
        if (raySphere(ro, rd, s, t) && t > 1e-3f) return true;
    }
    return false;
}

RayTracer::Hit RayTracer::cast(const Vec3& ro, const Vec3& rd) {
    Hit best;
    float bestT = 1e30f;
    for (auto& s : spheres) {
        float t;
        if (raySphere(ro, rd, s, t) && t < bestT) {
            bestT = t;
            best.ok = true;
            best.t = t;
            best.p = ro.add(rd.mul(t));
            best.n = best.p.sub(s.c).unit();
            best.color = s.color;
        }
    }
    float tp;
    if (rayPlane(ro, rd, floor, tp) && tp < bestT) {
        bestT = tp;
        best.ok = true;
        best.t = tp;
        best.p = ro.add(rd.mul(tp));
        best.n = floor.n;
        float checker = (std::floor(best.p.x) + std::floor(best.p.z));
        bool odd = std::fmod(std::fabs(checker), 2.0f) > 0.5f;
        best.color = odd ? 0xFF444444 : 0xFF9A9A9A;
    }
    return best;
}

uint32_t RayTracer::shade(const Vec3& ro, const Vec3& rd, int depth) {
    Hit h = cast(ro, rd);
    if (!h.ok) {
        float g = 0.5f * (rd.y + 1.0f);
        if (g < 0.0f) g = 0.0f;
        if (g > 1.0f) g = 1.0f;
        int r = (int)(80 + 175 * g);
        int gr = (int)(120 + 120 * g);
        int b = (int)(200 + 55 * g);
        return 0xFF000000u | ((uint32_t)r << 16) | ((uint32_t)gr << 8) | (uint32_t)b;
    }
    Vec3 l = light.sub(h.p).unit();
    float diff = std::max(0.0f, h.n.dot(l));
    float shadow = inShadow(h.p.add(h.n.mul(0.001f)), l) ? 0.15f : 1.0f;
    Vec3 v = ro.sub(h.p).unit();
    Vec3 hvec = l.add(v).unit();
    float spec = std::pow(std::max(0.0f, h.n.dot(hvec)), 48.0f);
    int br = (h.color >> 16) & 0xFF;
    int bg = (h.color >> 8) & 0xFF;
    int bb = h.color & 0xFF;
    float ambient = 0.25f;
    int r = (int)(br * (ambient + 0.85f * diff * shadow) + 255.0f * spec * shadow);
    int g = (int)(bg * (ambient + 0.85f * diff * shadow) + 255.0f * spec * shadow);
    int b = (int)(bb * (ambient + 0.85f * diff * shadow) + 255.0f * spec * shadow);
    if (depth > 0 && spec > 0.4f) {
        Vec3 refl = rd.sub(h.n.mul(2.0f * rd.dot(h.n))).unit();
        uint32_t rc = shade(h.p.add(h.n.mul(0.001f)), refl, depth - 1);
        r = (r + (int)((rc >> 16) & 0xFF)) / 2;
        g = (g + (int)((rc >> 8) & 0xFF)) / 2;
        b = (b + (int)(rc & 0xFF)) / 2;
    }
    if (r > 255) r = 255;
    if (g > 255) g = 255;
    if (b > 255) b = 255;
    return 0xFF000000u | ((uint32_t)r << 16) | ((uint32_t)g << 8) | (uint32_t)b;
}

void RayTracer::render(std::vector<uint32_t>& out, int w, int h, float angle) {
    out.assign((size_t)w * h, 0xFF101018);
    float rad = angle * 3.14159265f / 180.0f;
    float cs = std::cos(rad);
    float sn = std::sin(rad);
    std::vector<Sphere> rotated = spheres;
    for (auto& s : rotated) {
        float nx = s.c.x * cs - s.c.z * sn;
        float nz = s.c.x * sn + s.c.z * cs;
        s.c.x = nx;
        s.c.z = nz;
    }
    std::vector<Sphere> saved = spheres;
    spheres = rotated;
    Vec3 eye(0.0f, 1.4f, 5.0f);
    Vec3 target(0.0f, 0.2f, 0.0f);
    Vec3 forward = target.sub(eye).unit();
    Vec3 worldUp(0.0f, 1.0f, 0.0f);
    Vec3 rightV(worldUp.y * forward.z - worldUp.z * forward.y,
                worldUp.z * forward.x - worldUp.x * forward.z,
                worldUp.x * forward.y - worldUp.y * forward.x);
    rightV = rightV.unit();
    Vec3 upV(rightV.y * forward.z - rightV.z * forward.y,
             rightV.z * forward.x - rightV.x * forward.z,
             rightV.x * forward.y - rightV.y * forward.x);
    upV = upV.unit();
    unsigned hw = std::max(1u, std::thread::hardware_concurrency());
    int threads = (int)std::min(4u, hw);
    if (threads < 1) threads = 1;
    std::vector<std::thread> pool;
    for (int t = 0; t < threads; ++t) {
        pool.emplace_back([&, t]() {
            int y0 = h * t / threads;
            int y1 = h * (t + 1) / threads;
            for (int y = y0; y < y1; ++y) {
                float v = 1.0f - 2.0f * (y + 0.5f) / h;
                for (int x = 0; x < w; ++x) {
                    float u = 2.0f * (x + 0.5f) / w - 1.0f;
                    Vec3 rd = forward.add(rightV.mul(u * ((float)w / h))).add(upV.mul(v)).unit();
                    out[(size_t)y * w + x] = shade(eye, rd, 2);
                }
            }
        });
    }
    for (auto& th : pool) th.join();
    spheres = saved;
}