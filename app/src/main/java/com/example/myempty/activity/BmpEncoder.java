package com.example.myempty.activity2;

import android.graphics.Bitmap;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class BmpEncoder {

    public static boolean save(Bitmap bmp, String path, int quality) {
        if (bmp == null || path == null) return false;
        OutputStream os = null;
        try {
            os = new FileOutputStream(path);

            int w = bmp.getWidth();
            int h = bmp.getHeight();
            int rowSize = ((w * 24 + 31) / 32) * 4;
            int pixelArraySize = rowSize * h;
            int fileSize = 54 + pixelArraySize;

            writeShortLE(os, 0x4D42);
            writeIntLE(os, fileSize);
            writeShortLE(os, 0);
            writeShortLE(os, 0);
            writeIntLE(os, 54);

            writeIntLE(os, 40);
            writeIntLE(os, w);
            writeIntLE(os, h);
            writeShortLE(os, 1);
            writeShortLE(os, 24);
            writeIntLE(os, 0);
            writeIntLE(os, pixelArraySize);
            writeIntLE(os, 2835);
            writeIntLE(os, 2835);
            writeIntLE(os, 0);
            writeIntLE(os, 0);

            byte[] rowBuf = new byte[rowSize];
            int[] pixels = new int[w];
            int pad = rowSize - w * 3;

            for (int y = h - 1; y >= 0; y--) {
                bmp.getPixels(pixels, 0, w, 0, y, w, 1);
                int idx = 0;
                for (int x = 0; x < w; x++) {
                    int c = pixels[x];
                    rowBuf[idx++] = (byte) (c & 0xFF);
                    rowBuf[idx++] = (byte) ((c >> 8) & 0xFF);
                    rowBuf[idx++] = (byte) ((c >> 16) & 0xFF);
                }
                for (int i = 0; i < pad; i++) rowBuf[idx++] = 0;
                os.write(rowBuf);
            }

            os.flush();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try { if (os != null) os.close(); } catch (Exception ignored) {}
        }
    }

    private static void writeShortLE(OutputStream os, int v) throws Exception {
        os.write(v & 0xFF);
        os.write((v >> 8) & 0xFF);
    }

    private static void writeIntLE(OutputStream os, int v) throws Exception {
        os.write(v & 0xFF);
        os.write((v >> 8) & 0xFF);
        os.write((v >> 16) & 0xFF);
        os.write((v >> 24) & 0xFF);
    }
}