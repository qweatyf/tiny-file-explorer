#ifndef REGISTRY_HPP
#define REGISTRY_HPP

#include <string>
#include <map>
#include <vector>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <mutex>

class Registry {
public:
    enum Type { TYPE_DWORD = 1, TYPE_SZ = 2, TYPE_BINARY = 3 };

    struct Value {
        Type type;
        uint32_t dword;
        std::string sz;
        std::vector<uint8_t> bin;
        Value() : type(TYPE_DWORD), dword(0) {}
    };

    Registry() : dirty_(false), loaded_(false) {}

    void setPath(const std::string& file) {
        std::lock_guard<std::mutex> g(mtx_);
        file_ = file;
    }

    bool load() {
        std::lock_guard<std::mutex> g(mtx_);
        if (file_.empty()) return false;
        FILE* f = fopen(file_.c_str(), "rb");
        if (!f) { loaded_ = true; return false; }
        char magic[4] = {0};
        if (fread(magic, 1, 4, f) != 4 || memcmp(magic, "BREG", 4) != 0) {
            fclose(f);
            loaded_ = true;
            return false;
        }
        uint32_t keyCount = 0;
        if (fread(&keyCount, 4, 1, f) != 1) { fclose(f); loaded_ = true; return false; }
        for (uint32_t k = 0; k < keyCount; ++k) {
            std::string keyPath = readStr(f);
            uint32_t valCount = 0;
            if (fread(&valCount, 4, 1, f) != 1) break;
            std::map<std::string, Value>& bucket = keys_[keyPath];
            for (uint32_t v = 0; v < valCount; ++v) {
                std::string name = readStr(f);
                uint8_t type = 0;
                if (fread(&type, 1, 1, f) != 1) break;
                Value val;
                val.type = (Type)type;
                if (val.type == TYPE_DWORD) {
                    if (fread(&val.dword, 4, 1, f) != 1) break;
                } else if (val.type == TYPE_SZ) {
                    val.sz = readStr(f);
                } else if (val.type == TYPE_BINARY) {
                    uint32_t blen = 0;
                    if (fread(&blen, 4, 1, f) != 1) break;
                    val.bin.resize(blen);
                    if (blen > 0 && fread(val.bin.data(), 1, blen, f) != blen) break;
                }
                bucket[name] = val;
            }
        }
        fclose(f);
        loaded_ = true;
        dirty_ = false;
        return true;
    }

    bool flush() {
        std::lock_guard<std::mutex> g(mtx_);
        if (file_.empty()) return false;
        if (!dirty_) return true;
        FILE* f = fopen(file_.c_str(), "wb");
        if (!f) return false;
        fwrite("BREG", 1, 4, f);
        uint32_t keyCount = (uint32_t)keys_.size();
        fwrite(&keyCount, 4, 1, f);
        for (auto& kv : keys_) {
            writeStr(f, kv.first);
            uint32_t valCount = (uint32_t)kv.second.size();
            fwrite(&valCount, 4, 1, f);
            for (auto& vv : kv.second) {
                writeStr(f, vv.first);
                uint8_t type = (uint8_t)vv.second.type;
                fwrite(&type, 1, 1, f);
                if (vv.second.type == TYPE_DWORD) {
                    fwrite(&vv.second.dword, 4, 1, f);
                } else if (vv.second.type == TYPE_SZ) {
                    writeStr(f, vv.second.sz);
                } else {
                    uint32_t blen = (uint32_t)vv.second.bin.size();
                    fwrite(&blen, 4, 1, f);
                    if (blen > 0) fwrite(vv.second.bin.data(), 1, blen, f);
                }
            }
        }
        fclose(f);
        dirty_ = false;
        return true;
    }

    void setDword(const std::string& key, const std::string& name, uint32_t v) {
        std::lock_guard<std::mutex> g(mtx_);
        Value val;
        val.type = TYPE_DWORD;
        val.dword = v;
        keys_[key][name] = val;
        dirty_ = true;
    }

    void setSz(const std::string& key, const std::string& name, const std::string& v) {
        std::lock_guard<std::mutex> g(mtx_);
        Value val;
        val.type = TYPE_SZ;
        val.sz = v;
        keys_[key][name] = val;
        dirty_ = true;
    }

    void setBin(const std::string& key, const std::string& name, const std::vector<uint8_t>& v) {
        std::lock_guard<std::mutex> g(mtx_);
        Value val;
        val.type = TYPE_BINARY;
        val.bin = v;
        keys_[key][name] = val;
        dirty_ = true;
    }

    uint32_t getDword(const std::string& key, const std::string& name, uint32_t def = 0) {
        std::lock_guard<std::mutex> g(mtx_);
        auto it = keys_.find(key);
        if (it == keys_.end()) return def;
        auto vt = it->second.find(name);
        if (vt == it->second.end()) return def;
        if (vt->second.type != TYPE_DWORD) return def;
        return vt->second.dword;
    }

    std::string getSz(const std::string& key, const std::string& name, const std::string& def = "") {
        std::lock_guard<std::mutex> g(mtx_);
        auto it = keys_.find(key);
        if (it == keys_.end()) return def;
        auto vt = it->second.find(name);
        if (vt == it->second.end()) return def;
        if (vt->second.type != TYPE_SZ) return def;
        return vt->second.sz;
    }

    std::vector<std::string> listKeys() {
        std::lock_guard<std::mutex> g(mtx_);
        std::vector<std::string> out;
        for (auto& kv : keys_) out.push_back(kv.first);
        return out;
    }

    std::vector<std::pair<std::string, Value>> listValues(const std::string& key) {
        std::lock_guard<std::mutex> g(mtx_);
        std::vector<std::pair<std::string, Value>> out;
        auto it = keys_.find(key);
        if (it == keys_.end()) return out;
        for (auto& kv : it->second) out.push_back({kv.first, kv.second});
        return out;
    }

private:
    std::map<std::string, std::map<std::string, Value>> keys_;
    std::string file_;
    bool dirty_;
    bool loaded_;
    std::mutex mtx_;

    static void writeStr(FILE* f, const std::string& s) {
        uint32_t n = (uint32_t)s.size();
        fwrite(&n, 4, 1, f);
        if (n > 0) fwrite(s.data(), 1, n, f);
    }

    static std::string readStr(FILE* f) {
        uint32_t n = 0;
        if (fread(&n, 4, 1, f) != 1) return "";
        if (n > 65535) { n = 65535; }
        std::string s;
        s.resize(n);
        if (n > 0 && fread(&s[0], 1, n, f) != n) s.clear();
        return s;
    }
};

#endif