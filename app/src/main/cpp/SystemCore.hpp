#ifndef SYSTEM_CORE_HPP
#define SYSTEM_CORE_HPP

#include <iostream>
#include <vector>
#include <string>
#include <mutex>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <cerrno>
#include <fstream>
#include <algorithm>
#include <dirent.h>
#include <sys/stat.h>
#include "Registry.hpp"

struct FileItem {
    std::string name;
    bool isDir;
    uint64_t size;
    uint64_t mtime;
    int iconType;
};

struct Process {
    int pid;
    std::string name;
    bool is_running;
    bool running;
    bool is_windowed;
    bool windowed;
    bool is_browser;
    bool browser;
    bool is_3d;
    bool threeD;
    bool regedit;
    bool is_notepad;
    bool notepad;
    bool is_terminal;
    bool terminal;
    bool is_taskmgr;
    bool taskmgr;
    bool is_folder;
    bool folder;
    bool maximized;
    int x, y, w, h;
    int win_x, win_y, win_w, win_h;
    int savedX, savedY, savedW, savedH;
    int zOrder;
    std::string path;
    std::string openPath;
    uint64_t mem_bytes;
    uint64_t cpu_ticks;
    int diskIndex;
    std::string subPath;
};

class FileSystem {
private:
    std::string paths[2];
    mutable std::mutex fs_mutex;
public:
    int lastErrno;
    FileSystem() : lastErrno(0) {}
    void setPaths(const std::string& c, const std::string& d) {
        std::lock_guard<std::mutex> lock(fs_mutex);
        paths[0] = c; paths[1] = d;
    }
    void mount(const std::string& c, const std::string& d) { setPaths(c, d); }
    std::string getPath(int disk) {
        std::lock_guard<std::mutex> lock(fs_mutex);
        if (disk >= 0 && disk < 2) return paths[disk];
        return "";
    }
    std::string root(int disk) { return getPath(disk); }
    static int iconFor(const std::string& name, bool isDir) {
        if (isDir) return 0;
        std::string n = name;
        for (auto& c : n) c = (char)tolower((unsigned char)c);
        auto ends = [&](const char* s) {
            size_t t = strlen(s);
            return n.size() >= t && n.compare(n.size() - t, t, s) == 0;
        };
        if (ends(".txt") || ends(".md") || ends(".log")) return 1;
        if (ends(".png") || ends(".jpg") || ends(".jpeg") || ends(".bmp") || ends(".gif")) return 2;
        if (ends(".mp3") || ends(".wav") || ends(".ogg")) return 3;
        if (ends(".mp4") || ends(".mkv") || ends(".avi")) return 4;
        if (ends(".exe") || ends(".apk") || ends(".so")) return 5;
        if (ends(".zip") || ends(".rar") || ends(".7z")) return 6;
        return 7;
    }
    std::vector<FileItem> list(int disk, const std::string& sub) { return listDir(disk, sub); }
    std::vector<FileItem> listDir(int disk, const std::string& subPath) {
        std::vector<FileItem> files;
        std::string rootPath = getPath(disk);
        if (rootPath.empty()) return files;
        std::string full = rootPath;
        if (!subPath.empty()) {
            if (full.back() != '/') full += '/';
            full += subPath;
        }
        DIR* dir = opendir(full.c_str());
        if (!dir) {
            std::lock_guard<std::mutex> lock(fs_mutex);
            lastErrno = errno;
            return files;
        }
        {
            std::lock_guard<std::mutex> lock(fs_mutex);
            lastErrno = 0;
        }
        struct dirent* ent;
        while ((ent = readdir(dir)) != nullptr) {
            std::string name = ent->d_name;
            if (name == "." || name == "..") continue;
            FileItem it;
            it.name = name;
            it.isDir = (ent->d_type == DT_DIR);
            it.size = 0;
            it.mtime = 0;
            std::string fp = full + "/" + name;
            struct stat st;
            if (stat(fp.c_str(), &st) == 0) {
                it.size = (uint64_t)st.st_size;
                it.mtime = (uint64_t)st.st_mtime;
                if (S_ISDIR(st.st_mode)) it.isDir = true;
            }
            it.iconType = iconFor(name, it.isDir);
            files.push_back(it);
            if (files.size() > 500) break;
        }
        closedir(dir);
        std::sort(files.begin(), files.end(), [](const FileItem& a, const FileItem& b) {
            if (a.isDir != b.isDir) return a.isDir > b.isDir;
            return a.name < b.name;
        });
        return files;
    }
    std::string readHead(const std::string& fullPath, size_t maxBytes) {
        std::ifstream f(fullPath, std::ios::binary);
        if (!f) return std::string();
        std::string out;
        out.resize(maxBytes);
        f.read(&out[0], (std::streamsize)maxBytes);
        out.resize((size_t)f.gcount());
        return out;
    }
    std::string readTextFile(const std::string& fullPath, size_t maxBytes) { return readHead(fullPath, maxBytes); }
    bool renameItem(int disk, const std::string& sub, const std::string& oldName, const std::string& newName) {
        std::string base = getPath(disk);
        if (base.empty()) return false;
        std::string full = base;
        if (!sub.empty()) { if (full.back() != '/') full += '/'; full += sub; }
        if (full.back() != '/') full += '/';
        std::string a = full + oldName;
        std::string b = full + newName;
        return rename(a.c_str(), b.c_str()) == 0;
    }
    bool copyItem(int disk, const std::string& sub, const std::string& name) {
        std::string base = getPath(disk);
        if (base.empty()) return false;
        std::string full = base;
        if (!sub.empty()) { if (full.back() != '/') full += '/'; full += sub; }
        if (full.back() != '/') full += '/';
        std::string src = full + name;
        std::string dst = full + name + ".copy";
        std::ifstream in(src, std::ios::binary);
        if (!in) return false;
        std::ofstream out(dst, std::ios::binary);
        if (!out) return false;
        out << in.rdbuf();
        return true;
    }
    bool moveItem(int disk, const std::string& sub, const std::string& name, const std::string& destSub) {
        std::string base = getPath(disk);
        if (base.empty()) return false;
        std::string srcDir = base;
        if (!sub.empty()) { if (srcDir.back() != '/') srcDir += '/'; srcDir += sub; }
        if (srcDir.back() != '/') srcDir += '/';
        std::string dstDir = base;
        if (!destSub.empty()) { if (dstDir.back() != '/') dstDir += '/'; dstDir += destSub; }
        if (dstDir.back() != '/') dstDir += '/';
        return rename((srcDir + name).c_str(), (dstDir + name).c_str()) == 0;
    }
};

class OSKernel {
private:
    mutable std::mutex os_mutex;
    int next_pid = 1001;
    int next_z = 1;
    std::vector<Process> processes;
public:
    OSKernel() {}
    int spawn(const std::string& name, bool windowed = false, bool browser = false, bool threeD = false,
              int x = 150, int y = 50, int w = 400, int h = 250, bool notepad = false,
              const std::string& path = std::string(), bool regedit = false,
              bool terminal = false, bool taskmgr = false, bool folder = false,
              int diskIndex = 0, const std::string& subPath = std::string()) {
        std::lock_guard<std::mutex> lock(os_mutex);
        int pid = next_pid++;
        Process p;
        p.pid = pid;
        p.name = name;
        p.is_running = true; p.running = true;
        p.is_windowed = windowed; p.windowed = windowed;
        p.is_browser = browser; p.browser = browser;
        p.is_3d = threeD; p.threeD = threeD;
        p.regedit = regedit;
        p.is_notepad = notepad; p.notepad = notepad;
        p.is_terminal = terminal; p.terminal = terminal;
        p.is_taskmgr = taskmgr; p.taskmgr = taskmgr;
        p.is_folder = folder; p.folder = folder;
        p.maximized = false;
        p.x = x; p.y = y; p.w = w; p.h = h;
        p.win_x = x; p.win_y = y; p.win_w = w; p.win_h = h;
        p.savedX = x; p.savedY = y; p.savedW = w; p.savedH = h;
        p.zOrder = next_z++;
        p.path = path;
        p.openPath = path;
        p.mem_bytes = 4096 + (uint64_t)(pid % 7) * 1024;
        p.cpu_ticks = 0;
        p.diskIndex = diskIndex;
        p.subPath = subPath;
        processes.push_back(p);
        return pid;
    }
    void kill(int pid) { killProcess(pid); }
    void killProcess(int pid) {
        std::lock_guard<std::mutex> lock(os_mutex);
        for (auto& p : processes) if (p.pid == pid) { p.is_running = false; p.running = false; }
    }
    void raise(int pid) { bringToFront(pid); }
    void bringToFront(int pid) {
        std::lock_guard<std::mutex> lock(os_mutex);
        for (auto& p : processes) if (p.pid == pid && p.is_running) p.zOrder = next_z++;
    }
    bool alive(const std::string& name) { return hasProcess(name); }
    bool hasProcess(const std::string& name) {
        std::lock_guard<std::mutex> lock(os_mutex);
        for (auto& p : processes) if (p.name == name && p.is_running) return true;
        return false;
    }
    void move(int pid, int nx, int ny) { moveWindow(pid, nx, ny); }
    void moveWindow(int pid, int nx, int ny) {
        std::lock_guard<std::mutex> lock(os_mutex);
        for (auto& p : processes) {
            if (p.pid == pid && p.is_running) {
                if (!p.maximized) { p.x = nx; p.y = ny; p.win_x = nx; p.win_y = ny; }
            }
        }
    }
    void resizeWindow(int pid, int nx, int ny, int nw, int nh) {
        std::lock_guard<std::mutex> lock(os_mutex);
        for (auto& p : processes) {
            if (p.pid == pid && p.is_running) {
                p.x = nx; p.y = ny; p.w = nw; p.h = nh;
                p.win_x = nx; p.win_y = ny; p.win_w = nw; p.win_h = nh;
            }
        }
    }
    void toggleMaximize(int pid, int fullW, int fullH) {
        std::lock_guard<std::mutex> lock(os_mutex);
        for (auto& p : processes) {
            if (p.pid != pid || !p.is_running) continue;
            if (p.maximized) {
                p.x = p.savedX; p.y = p.savedY; p.w = p.savedW; p.h = p.savedH;
                p.maximized = false;
            } else {
                p.savedX = p.x; p.savedY = p.y; p.savedW = p.w; p.savedH = p.h;
                p.x = 0; p.y = 0; p.w = fullW; p.h = fullH;
                p.maximized = true;
            }
            p.win_x = p.x; p.win_y = p.y; p.win_w = p.w; p.win_h = p.h;
        }
    }
    void setFolderState(int pid, int diskIndex, const std::string& sub) {
        std::lock_guard<std::mutex> lock(os_mutex);
        for (auto& p : processes) {
            if (p.pid == pid) { p.diskIndex = diskIndex; p.subPath = sub; }
        }
    }
    uint64_t totalMemory() {
        std::lock_guard<std::mutex> lock(os_mutex);
        uint64_t total = 0;
        for (auto& p : processes) if (p.is_running) total += p.mem_bytes;
        return total;
    }
    std::vector<Process> snapshot() { return getProcessList(); }
    std::vector<Process> getProcessList() {
        std::lock_guard<std::mutex> lock(os_mutex);
        std::vector<Process> out = processes;
        std::sort(out.begin(), out.end(), [](const Process& a, const Process& b) {
            return a.zOrder < b.zOrder;
        });
        return out;
    }
};

#endif