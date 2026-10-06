package com.example.myempty.activity2;

import android.util.Base64;

import java.nio.charset.StandardCharsets;

public class SevenLayer {
    public static String encode(String text) {
        String s = text;
        s = Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);

        StringBuilder hex = new StringBuilder();
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) hex.append(String.format("%02x", b));
        s = hex.toString();

        StringBuilder bin = new StringBuilder();
        for (char c : s.toCharArray()) {
            bin.append(String.format("%4s", Integer.toBinaryString(c)).replace(' ', '0'));
        }
        s = bin.toString();

        s = s.replace("0", ".").replace("1", "-");
        s = "SEVEN:" + s;
        s = Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
        s = new StringBuilder(s).reverse().toString();
        return s;
    }

    public static String decode(String text) {
        try {
            String s = new StringBuilder(text).reverse().toString();
            s = new String(Base64.decode(s, Base64.DEFAULT), StandardCharsets.UTF_8);
            s = s.replace("SEVEN:", "");
            s = s.replace(".", "0").replace("-", "1");

            StringBuilder hex = new StringBuilder();
            for (int i = 0; i + 4 <= s.length(); i += 4) {
                hex.append(Integer.toHexString(Integer.parseInt(s.substring(i, i + 4), 2)));
            }

            byte[] raw = new byte[hex.length() / 2];
            for (int i = 0; i < raw.length; i++) {
                raw[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            String b64 = new String(raw, StandardCharsets.UTF_8);
            return new String(Base64.decode(b64, Base64.DEFAULT), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "解密失败";
        }
    }
}