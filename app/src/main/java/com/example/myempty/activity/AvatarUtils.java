package com.example.myempty.activity2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AvatarUtils {

    private static final String FILE_NAME = "avatar.png";

    public static File avatarFile(Context ctx) {
        return new File(ctx.getFilesDir(), FILE_NAME);
    }

    public static boolean hasAvatar(Context ctx) {
        return avatarFile(ctx).exists();
    }

    public static Bitmap load(Context ctx) {
        try {
            File f = avatarFile(ctx);
            if (!f.exists()) return null;
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean saveFromUri(Context ctx, android.net.Uri uri) {
        try {
            InputStream is = ctx.getContentResolver().openInputStream(uri);
            if (is == null) return false;

            Bitmap raw = BitmapFactory.decodeStream(is);
            is.close();
            if (raw == null) return false;

            Bitmap square = cropSquare(raw);
            Bitmap small = Bitmap.createScaledBitmap(square, 256, 256, true);

            FileOutputStream fos = new FileOutputStream(avatarFile(ctx));
            small.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            if (small != square) square.recycle();
            if (square != raw) raw.recycle();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean delete(Context ctx) {
        return avatarFile(ctx).delete();
    }

    private static Bitmap cropSquare(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int size = Math.min(w, h);
        int x = (w - size) / 2;
        int y = (h - size) / 2;
        return Bitmap.createBitmap(src, x, y, size, size);
    }

    public static Bitmap makeDefault(String name, int sizePx) {
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);

        int[] palette = {
            0xFFD87093, 0xFF6A9BD8, 0xFF7DBE77, 0xFFE0A458,
            0xFF9B7DBE, 0xFFE07A5F, 0xFF4ECDC4, 0xFFB56576
        };
        int color = palette[Math.abs(safeHash(name)) % palette.length];

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        cv.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, p);

        String ch = firstChar(name);
        if (!ch.isEmpty()) {
            Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(Color.WHITE);
            tp.setTypeface(Typeface.DEFAULT_BOLD);
            tp.setTextSize(sizePx * 0.45f);
            tp.setTextAlign(Paint.Align.CENTER);
            Rect r = new Rect();
            tp.getTextBounds(ch, 0, ch.length(), r);
            float cy = sizePx / 2f - (r.top + r.bottom) / 2f;
            cv.drawText(ch, sizePx / 2f, cy, tp);
        }
        return bmp;
    }

    public static Bitmap makeCircle(Bitmap src, int sizePx) {
        if (src == null) return null;
        Bitmap out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Bitmap scaled = Bitmap.createScaledBitmap(src, sizePx, sizePx, true);
        p.setShader(new android.graphics.BitmapShader(scaled,
            android.graphics.Shader.TileMode.CLAMP,
            android.graphics.Shader.TileMode.CLAMP));
        cv.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, p);

        if (scaled != src) scaled.recycle();
        return out;
    }

    private static int safeHash(String s) {
        if (s == null) return 0;
        int h = 0;
        for (int i = 0; i < s.length(); i++) {
            h = h * 31 + s.charAt(i);
        }
        return h;
    }

    private static String firstChar(String name) {
        if (name == null || name.isEmpty()) return "?";
        return String.valueOf(name.charAt(0));
    }
}