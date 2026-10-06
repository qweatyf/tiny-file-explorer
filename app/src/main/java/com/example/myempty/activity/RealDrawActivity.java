package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class RealDrawActivity extends Activity {

    private static final int[] SIZES = {16, 32, 50, 100, 120, 350, 450, 500};
    private static final int[] BTNS = {
        R.id.btn_size_16, R.id.btn_size_32, R.id.btn_size_50, R.id.btn_size_100,
        R.id.btn_size_120, R.id.btn_size_350, R.id.btn_size_450, R.id.btn_size_500
    };

    ImageView iv;
    View colorDot;
    TextView tvSize;
    SeekBar sbSize;
    Button btnBrush, btnEraser;

    Bitmap bmp;
    Canvas cv;
    Paint p;

    float lx, ly;

    int color = Color.BLACK;
    int brushType = 2;
    int brushSize = 10;
    int size = 500;
    boolean eraser = false;

    Rect area = new Rect();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_draw);

        iv = findViewById(R.id.iv_canvas);
        colorDot = findViewById(R.id.view_current_color);
        tvSize = findViewById(R.id.tv_size);
        sbSize = findViewById(R.id.sb_size);
        btnBrush = findViewById(R.id.btn_pick_brush);
        btnEraser = findViewById(R.id.btn_eraser);

        p = new Paint();
        p.setAntiAlias(true);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setColor(color);

        colorDot.setBackgroundColor(color);

        for (int i = 0; i < BTNS.length; i++) {
            final int s = SIZES[i];
            findViewById(BTNS[i]).setOnClickListener(x -> changeSize(s));
        }

        sbSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b, int val, boolean u) {
                brushSize = val + 1;
                tvSize.setText("" + brushSize);
                if (eraser) {
                    p.setColor(Color.WHITE);
                    p.setStrokeWidth(brushSize * 1.5f);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar b) {}
            @Override public void onStopTrackingTouch(SeekBar b) {}
        });

        findViewById(R.id.btn_pick_color).setOnClickListener(x -> {
            new ColorPickerDialog(this, color, c -> {
                color = c;
                colorDot.setBackgroundColor(c);
                if (!eraser) apply();
            }).show();
        });

        btnBrush.setOnClickListener(x -> pickBrush());

        btnEraser.setOnClickListener(x -> {
            eraser = !eraser;
            btnEraser.setText(eraser ? "橡皮：开" : "橡皮");
            if (eraser) {
                p.setColor(Color.WHITE);
                p.setAlpha(255);
                p.setStrokeWidth(brushSize * 1.5f);
            } else {
                apply();
            }
        });

        findViewById(R.id.btn_clear).setOnClickListener(x -> {
            new AlertDialog.Builder(this)
                .setTitle("清空")
                .setMessage("清空后不能恢复，确定？")
                .setPositiveButton("确定", (d, w) -> {
                    cv.drawColor(Color.WHITE);
                    iv.invalidate();
                })
                .setNegativeButton("算了", null)
                .show();
        });

        findViewById(R.id.btn_import).setOnClickListener(x -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(i, 100);
        });

        iv.post(this::calcArea);

        iv.setOnTouchListener((v, e) -> {
            float[] pt = fix(e.getX(), e.getY());
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                lx = pt[0];
                ly = pt[1];
                if (eraser) cv.drawCircle(lx, ly, brushSize * 0.8f, p);
                iv.invalidate();
                return true;
            } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
                if (eraser) {
                    cv.drawLine(lx, ly, pt[0], pt[1], p);
                    cv.drawCircle(pt[0], pt[1], brushSize * 0.8f, p);
                } else {
                    stroke(lx, ly, pt[0], pt[1]);
                }
                iv.invalidate();
                lx = pt[0];
                ly = pt[1];
                return true;
            }
            return true;
        });

        findViewById(R.id.btn_save_draw).setOnClickListener(x -> save());

        reset(500);
        apply();
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent data) {
        super.onActivityResult(rc, res, data);
        if (rc == 100 && res == RESULT_OK && data != null) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                Bitmap src = BitmapFactory.decodeStream(is);
                is.close();
                if (src != null) drawImported(src);
            } catch (Exception e) {
                Toast.makeText(this, "读图失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    void drawImported(Bitmap src) {
        float sc = Math.min((float) size / src.getWidth(), (float) size / src.getHeight());
        int dw = (int) (src.getWidth() * sc);
        int dh = (int) (src.getHeight() * sc);
        int left = (size - dw) / 2;
        int top = (size - dh) / 2;
        RectF dst = new RectF(left, top, left + dw, top + dh);
        cv.drawBitmap(src, null, dst, null);
        iv.invalidate();
    }

    void changeSize(int s) {
        if (s == size) return;
        new AlertDialog.Builder(this)
            .setTitle("切尺寸")
            .setMessage("切成 " + s + "×" + s + " 会清空，确定？")
            .setPositiveButton("确定", (d, w) -> reset(s))
            .setNegativeButton("算了", null)
            .show();
    }

    void reset(int s) {
        size = s;
        bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        cv = new Canvas(bmp);
        cv.drawColor(Color.WHITE);
        iv.setImageBitmap(bmp);
        iv.post(this::calcArea);
    }

    void calcArea() {
        int vw = iv.getWidth(), vh = iv.getHeight();
        if (vw <= 0 || vh <= 0 || bmp == null) return;
        float sc = Math.min((float) vw / size, (float) vh / size);
        int dw = (int) (size * sc), dh = (int) (size * sc);
        int left = (vw - dw) / 2, top = (vh - dh) / 2;
        area.set(left, top, left + dw, top + dh);
    }

    float[] fix(float x, float y) {
        int dw = area.width(), dh = area.height();
        if (dw <= 0 || dh <= 0) return new float[]{x, y};
        float nx = (x - area.left) * size / dw;
        float ny = (y - area.top) * size / dh;
        if (nx < 0) nx = 0;
        if (nx > size) nx = size;
        if (ny < 0) ny = 0;
        if (ny > size) ny = size;
        return new float[]{nx, ny};
    }

    void apply() {
        p.setColor(color);
        if (brushType == 0) {
            p.setAlpha(110);
            p.setStrokeWidth(Math.max(1, brushSize * 0.5f));
        } else if (brushType == 1) {
            p.setAlpha(255);
            p.setStrokeWidth(brushSize);
        } else {
            p.setAlpha(210);
            p.setStrokeWidth(brushSize * 1.6f);
        }
    }

    void pickBrush() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_brush_picker, null);
        AlertDialog d = new AlertDialog.Builder(this).setView(v).create();

        v.findViewById(R.id.btn_pencil).setOnClickListener(x -> {
            brushType = 0;
            btnBrush.setText("笔：铅笔");
            eraser = false;
            btnEraser.setText("橡皮");
            apply();
            d.dismiss();
        });
        v.findViewById(R.id.btn_pen).setOnClickListener(x -> {
            brushType = 1;
            btnBrush.setText("笔：圆珠笔");
            eraser = false;
            btnEraser.setText("橡皮");
            apply();
            d.dismiss();
        });
        v.findViewById(R.id.btn_brush).setOnClickListener(x -> {
            brushType = 2;
            btnBrush.setText("笔：毛笔");
            eraser = false;
            btnEraser.setText("橡皮");
            apply();
            d.dismiss();
        });

        d.show();
    }

    void stroke(float x0, float y0, float x1, float y1) {
        if (bmp == null) return;
        if (brushType == 2) brush(x0, y0, x1, y1);
        else if (brushType == 0) pencil(x0, y0, x1, y1);
        else cv.drawLine(x0, y0, x1, y1, p);
    }

    void brush(float x0, float y0, float x1, float y1) {
        float dx = x1 - x0, dy = y1 - y0;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        int n = Math.max(1, (int) (d / 2));
        float w0 = brushSize * 1.6f;

        for (int i = 0; i <= n; i++) {
            float t = (float) i / n;
            float cx = x0 + dx * t;
            float cy = y0 + dy * t;
            float w = w0 * (1f - 0.5f * t);
            int a = (int) (210 * (1f - 0.4f * t));

            p.setStrokeWidth(Math.max(2, w));
            p.setAlpha(a);
            cv.drawPoint(cx, cy, p);

            p.setStrokeWidth(Math.max(1, w * 0.4f));
            p.setAlpha(a / 2);
            cv.drawPoint(cx + 1.5f, cy, p);
            cv.drawPoint(cx - 1.5f, cy, p);
        }
        p.setAlpha(210);
    }

    void pencil(float x0, float y0, float x1, float y1) {
        float dx = x1 - x0, dy = y1 - y0;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        int n = Math.max(1, (int) d);

        for (int i = 0; i <= n; i++) {
            float t = (float) i / n;
            cv.drawPoint(x0 + dx * t, y0 + dy * t, p);
        }
    }

    void save() {
        if (bmp == null) return;
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "画画成果/真正画画");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "画画成果.png");
            FileOutputStream fos = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Toast.makeText(this, "保存到：" + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}