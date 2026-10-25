#ifndef ASSEMBLER_HPP
#define ASSEMBLER_HPP
#include <cstdint>
#include <string>
#include <sstream>
#include <cstring>

class Assembler {
public:
    static void assemble(const std::string& input, uint8_t* memory, uint64_t progBase) {
        std::istringstream stream(input);
        std::string line;
        uint64_t currentAddr = progBase;

        auto emitByte = [&](uint8_t b) {
            if (currentAddr < 256 * 1024 * 1024) memory[currentAddr++] = b;
        };
        auto emitLong = [&](uint64_t val) {
            for (int i = 7; i >= 0; i--) emitByte((val >> (i * 8)) & 0xFF);
        };
        auto parseReg = [](const std::string& s) -> uint64_t {
            if (s.empty()) return 0;
            try {
                if (s.size() > 2 && s[0] == '0' && (s[1] == 'x' || s[1] == 'X')) return std::stoull(s, nullptr, 16);
                return (uint64_t)std::stoll(s);
            } catch (...) { return 0; }
        };

        while (std::getline(stream, line)) {
            if (!line.empty() && line.back() == '\r') line.pop_back();
            if (line.empty()) continue;
            size_t sc = line.find(';'); if (sc != std::string::npos) line = line.substr(0, sc);
            size_t sl = line.find("//"); if (sl != std::string::npos) line = line.substr(0, sl);
            size_t b = line.find_first_not_of(" \t"); if (b == std::string::npos) continue;
            size_t e = line.find_last_not_of(" \t"); line = line.substr(b, e - b + 1);
            if (line.empty()) continue;
            
            for (size_t i = 0; i < line.length(); i++) if (line[i] == ',') line[i] = ' ';

            std::istringstream lineStream(line);
            std::string cmd;
            lineStream >> cmd;

            if (cmd == "WRITE") { std::string a1, a2; lineStream >> a1 >> a2; emitByte(0x01); emitLong(parseReg(a1)); emitByte(parseReg(a2) & 0xFF); }
            else if (cmd == "LOAD") { std::string a1; lineStream >> a1; emitByte(0x02); emitLong(parseReg(a1)); }
            else if (cmd == "ADD") { std::string a1; lineStream >> a1; emitByte(0x03); emitLong(parseReg(a1)); }
            else if (cmd == "JMP") { std::string a1; lineStream >> a1; emitByte(0x04); emitLong(parseReg(a1)); }
            else if (cmd == "JZ") { std::string a1; lineStream >> a1; emitByte(0x05); emitLong(parseReg(a1)); }
            else if (cmd == "OUT") { emitByte(0x06); }
            else if (cmd == "SUB") { std::string a1; lineStream >> a1; emitByte(0x07); emitLong(parseReg(a1)); }
            else if (cmd == "STORE") { std::string a1; lineStream >> a1; emitByte(0x08); emitLong(parseReg(a1)); }
            else if (cmd == "PUSH") { emitByte(0x09); }
            else if (cmd == "POP") { emitByte(0x0A); }
            else if (cmd == "STORE_IND") { emitByte(0x0E); }
            else if (cmd == "LOAD_IND") { emitByte(0x0F); }
            else if (cmd == "SET_ADDR") { emitByte(0x10); }
            else if (cmd == "VIDEO_ON") { emitByte(0x11); }
            else if (cmd == "SET_PIXEL") {
                std::string xStr, yStr, cStr; lineStream >> xStr >> yStr >> cStr;
                emitByte(0x12); emitLong(parseReg(yStr) * 640 + parseReg(xStr)); for (int i = 3; i >= 0; i--) emitByte((parseReg(cStr) >> (i * 8)) & 0xFF);
            } else if (cmd == "FILL_SCREEN") {
                std::string cStr; lineStream >> cStr; emitByte(0x13); for (int i = 3; i >= 0; i--) emitByte((parseReg(cStr) >> (i * 8)) & 0xFF);
            } else if (cmd == "VSYNC") { emitByte(0x15); }
            else if (cmd == "TOUCH_XY") {
                std::string xStr, yStr; lineStream >> xStr >> yStr; emitByte(0x18); emitLong(parseReg(xStr)); emitLong(parseReg(yStr));
            } else if (cmd == "DRAW_RECT") {
                std::string xStr, yStr, wStr, hStr, cStr; lineStream >> xStr >> yStr >> wStr >> hStr >> cStr; emitByte(0x17);
                emitLong(parseReg(xStr)); emitLong(parseReg(yStr)); emitLong(parseReg(wStr)); emitLong(parseReg(hStr)); for (int i = 3; i >= 0; i--) emitByte((parseReg(cStr) >> (i * 8)) & 0xFF);
            } else if (cmd == "DRAW_RECT_IND") {
                std::string xA, yA, wStr, hStr, cStr; lineStream >> xA >> yA >> wStr >> hStr >> cStr; emitByte(0x1B);
                emitLong(parseReg(xA)); emitLong(parseReg(yA)); emitLong(parseReg(wStr)); emitLong(parseReg(hStr)); for (int i = 3; i >= 0; i--) emitByte((parseReg(cStr) >> (i * 8)) & 0xFF);
            } else if (cmd == "LOAD_LONG") { std::string a1; lineStream >> a1; emitByte(0x19); emitLong(parseReg(a1)); }
            else if (cmd == "SUB_I") { std::string a1; lineStream >> a1; emitByte(0x1A); emitLong(parseReg(a1)); }
            else if (cmd == "PRINT_STR") {
                std::string xStr, yStr, addrStr, cStr; lineStream >> xStr >> yStr >> addrStr >> cStr; emitByte(0x1C);
                emitLong(parseReg(xStr)); emitLong(parseReg(yStr)); emitLong(parseReg(addrStr)); for (int i = 3; i >= 0; i--) emitByte((parseReg(cStr) >> (i * 8)) & 0xFF);
            } else if (cmd == "DRAW_CIRCLE") {
                std::string xStr, yStr, rStr, cStr; lineStream >> xStr >> yStr >> rStr >> cStr; emitByte(0x1E);
                emitLong(parseReg(xStr)); emitLong(parseReg(yStr)); emitLong(parseReg(rStr)); for (int i = 3; i >= 0; i--) emitByte((parseReg(cStr) >> (i * 8)) & 0xFF);
            } else if (cmd == "DRAW_LINE") {
                std::string x1, y1, x2, y2, cStr; lineStream >> x1 >> y1 >> x2 >> y2 >> cStr; emitByte(0x1F);
                emitLong(parseReg(x1)); emitLong(parseReg(y1)); emitLong(parseReg(x2)); emitLong(parseReg(y2)); for (int i = 3; i >= 0; i--) emitByte((parseReg(cStr) >> (i * 8)) & 0xFF);
            } else if (cmd == "MUL") { std::string a1; lineStream >> a1; emitByte(0x20); emitLong(parseReg(a1)); }
            else if (cmd == "DIV") { std::string a1; lineStream >> a1; emitByte(0x21); emitLong(parseReg(a1)); }
            else if (cmd == "MOD") { std::string a1; lineStream >> a1; emitByte(0x22); emitLong(parseReg(a1)); }
            else if (cmd == "AND") { std::string a1; lineStream >> a1; emitByte(0x23); emitLong(parseReg(a1)); }
            else if (cmd == "OR") { std::string a1; lineStream >> a1; emitByte(0x24); emitLong(parseReg(a1)); }
            else if (cmd == "XOR") { std::string a1; lineStream >> a1; emitByte(0x25); emitLong(parseReg(a1)); }
            else if (cmd == "SHL") { std::string a1; lineStream >> a1; emitByte(0x26); emitLong(parseReg(a1)); }
            else if (cmd == "SHR") { std::string a1; lineStream >> a1; emitByte(0x27); emitLong(parseReg(a1)); }
            else if (cmd == "RAND") { emitByte(0x28); }
            else if (cmd == "HANG") { emitByte(0x29); }
            else if (cmd == "MOUSE_EVENT") { std::string a1, a2; lineStream >> a1 >> a2; emitByte(0x2B); emitLong(parseReg(a1)); emitLong(parseReg(a2)); }
            else if (cmd == "GET_MOUSE") { std::string a1, a2, a3; lineStream >> a1 >> a2 >> a3; emitByte(0x2C); emitLong(parseReg(a1)); emitLong(parseReg(a2)); emitLong(parseReg(a3)); }
            else if (cmd == "IS_HIT") {
                std::string a1, a2, a3, a4, a5, a6; lineStream >> a1 >> a2 >> a3 >> a4 >> a5 >> a6;
                emitByte(0x2D); emitLong(parseReg(a1)); emitLong(parseReg(a2)); emitLong(parseReg(a3)); emitLong(parseReg(a4)); emitLong(parseReg(a5)); emitLong(parseReg(a6));
            }
            else if (cmd == "SLEEP") { std::string a1; lineStream >> a1; emitByte(0x2F); emitLong(parseReg(a1)); }
            else if (cmd == "DRAW_TEXT_STR") {
                std::string xStr, yStr, addrStr, cStr; lineStream >> xStr >> yStr >> addrStr >> cStr;
                emitByte(0x30); emitLong(parseReg(xStr)); emitLong(parseReg(yStr)); emitLong(parseReg(addrStr)); emitLong(parseReg(cStr));
            }
            else if (cmd == "BOOT") { emitByte(0x32); }
            else if (cmd == "DRAW_TRIANGLE") {
                std::string x0, y0, x1, y1, x2, y2, cStr; lineStream >> x0 >> y0 >> x1 >> y1 >> x2 >> y2 >> cStr; emitByte(0x33);
                emitLong(parseReg(x0)); emitLong(parseReg(y0)); emitLong(parseReg(x1)); emitLong(parseReg(y1)); emitLong(parseReg(x2)); emitLong(parseReg(y2)); emitLong(parseReg(cStr));
            }
            else if (cmd == "HALT") { emitByte(0x00); }
            else if (cmd == "PANIC") { emitByte(0xFF); }
        }
    }
};
#endif