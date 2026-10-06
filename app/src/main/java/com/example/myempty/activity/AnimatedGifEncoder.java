package com.example.myempty.activity2;

import android.graphics.Bitmap;

import java.io.IOException;
import java.io.OutputStream;

public class AnimatedGifEncoder {

    private OutputStream out;
    private int delay = 500;
    private int repeat = 0;
    private int quality = 10;
    private boolean started = false;
    private int width, height;
    private byte[] pixels;
    private int palSize = 7;
    private int colorDepth = 8;
    private boolean closeStream = false;
    private byte[] block = new byte[256];
    private int dispose = -1;
    private boolean firstFrame = true;

    public void setDelay(int ms) { delay = ms; }
    public void setRepeat(int n) { repeat = n; }
    public void setQuality(int q) { quality = q; }

    public boolean start(OutputStream os) {
        out = os;
        return start();
    }

    private boolean start() {
        try {
            writeString("GIF89a");
            started = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean addFrame(Bitmap bmp) {
        try {
            if (!started) return false;
            if (firstFrame) {
                width = bmp.getWidth();
                height = bmp.getHeight();
                writeLSD();
                writeNetscapeExt();
                firstFrame = false;
            }
            writeGraphicCtrlExt();
            writeImageDesc();
            writePixels(bmp);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean finish() {
        try {
            out.write(0x3b);
            out.flush();
            if (closeStream) out.close();
            out = null;
            started = false;
            firstFrame = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void writeGraphicCtrlExt() throws IOException {
        out.write(0x21);
        out.write(0xf9);
        out.write(4);
        int transp = 0;
        int disp = 2;
        if (dispose >= 0) disp = dispose & 7;
        disp <<= 2;
        out.write(0 | disp | 0 | transp);
        writeShort(delay);
        out.write(0);
    }

    private void writeImageDesc() throws IOException {
        out.write(0x2c);
        writeShort(0);
        writeShort(0);
        writeShort(width);
        writeShort(height);
        out.write(0);
    }

    private void writeLSD() throws IOException {
        writeShort(width);
        writeShort(height);
        out.write(0x80 | 0x70 | 0x00 | palSize);
        out.write(0);
        out.write(0);
    }

    private void writeNetscapeExt() throws IOException {
        out.write(0x21);
        out.write(0xff);
        out.write(11);
        writeString("NETSCAPE2.0");
        out.write(3);
        out.write(1);
        writeShort(repeat);
        out.write(0);
    }

    private void writePixels(Bitmap bmp) throws IOException {
        if (pixels == null || pixels.length < width * height) {
            pixels = new byte[width * height];
        }

        int[] argb = new int[width * height];
        bmp.getPixels(argb, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            int r = (argb[i] >> 16) & 0xff;
            int g = (argb[i] >> 8) & 0xff;
            int b = argb[i] & 0xff;
            int gray = (r + g + b) / 3;
            gray = gray * 7 / 8;
            pixels[i] = (byte) gray;
        }

        out.write(colorDepth);
        int n = pixels.length;
        int chunk = 250;
        int pos = 0;
        while (pos < n) {
            int len = Math.min(chunk, n - pos);
            out.write(len);
            out.write(pixels, pos, len);
            pos += len;
        }
        out.write(0);
    }

    private void writeShort(int v) throws IOException {
        out.write(v & 0xff);
        out.write((v >> 8) & 0xff);
    }

    private void writeString(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            out.write(s.charAt(i));
        }
    }
}