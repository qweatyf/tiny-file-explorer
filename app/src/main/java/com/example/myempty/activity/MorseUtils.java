package com.example.myempty.activity2;

public class MorseUtils {
    private static final String[] CODE = {
        ".-","-...","-.-.","-..",".","..-.","--.","....","..",".---",
        "-.-",".-..","--","-.","---",".--.","--.-",".-.","...","-",
        "..-","...-",".--","-..-","-.--","--.."
    };

    private static final String[] NUM_CODE = {
        "-----",".----","..---","...--","....-",".....","-....","--...","---..","----."
    };

    public static String toMorse(String text) {
        String src = text;
        if (containsChinese(src)) src = PinyinUtils.toPinyin(src);

        StringBuilder sb = new StringBuilder();
        for (char c : src.toUpperCase().toCharArray()) {
            if (c >= 'A' && c <= 'Z') sb.append(CODE[c - 'A']).append(" ");
            else if (c == ' ') sb.append("/ ");
            else if (c >= '0' && c <= '9') {
                sb.append(NUM_CODE[c - '0']).append(" ");
            }
        }
        return sb.toString();
    }

    public static String fromMorse(String morse) {
        StringBuilder sb = new StringBuilder();
        for (String code : morse.trim().split("\\s+")) {
            if (code.equals("/")) { sb.append(" "); continue; }
            
            boolean found = false;
            for (int i = 0; i < CODE.length; i++) {
                if (CODE[i].equals(code)) { sb.append((char)('A' + i)); found = true; break; }
            }
            if (found) continue;
            
            for (int i = 0; i < NUM_CODE.length; i++) {
                if (NUM_CODE[i].equals(code)) { sb.append((char)('0' + i)); found = true; break; }
            }
            if (found) continue;
        }
        return sb.toString();
    }

    private static boolean containsChinese(String s) {
        for (char c : s.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FA5) return true;
        }
        return false;
    }
}