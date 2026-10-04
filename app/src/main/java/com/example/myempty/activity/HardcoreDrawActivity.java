package com.example.myempty.activity2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class HardcoreDrawActivity extends Activity {

    private static final int[] SIZES = {16, 32, 50, 100, 120, 350, 450, 500};
    private static final int[] BTNS = {
        R.id.btn_size_16, R.id.btn_size_32, R.id.btn_size_50, R.id.btn_size_100,
        R.id.btn_size_120, R.id.btn_size_350, R.id.btn_size_450, R.id.btn_size_500
    };
    private static final int BITS = 10;
    private static final int WHITE = 1023;

    EditText et;
    TextView err;
    ImageView iv;
    ScrollView svTable, svMain;
    LinearLayout llTable;
    Button btnGrid;

    int size = 16;
    boolean opened = false;
    boolean grid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hardcore_draw);

        et = findViewById(R.id.et_bin);
        err = findViewById(R.id.tv_err);
        iv = findViewById(R.id.iv_preview);
        svTable = findViewById(R.id.sv_color_table);
        svMain = findViewById(R.id.sv_main);
        llTable = findViewById(R.id.ll_color_table);
        btnGrid = findViewById(R.id.btn_grid);

        for (int i = 0; i < BTNS.length; i++) {
            final int s = SIZES[i];
            findViewById(BTNS[i]).setOnClickListener(x -> {
                size = s;
                draw();
            });
        }

        fillTable();

        findViewById(R.id.btn_drag_hint).setOnClickListener(x -> {
            opened = !opened;
            svTable.setVisibility(opened ? View.VISIBLE : View.GONE);
            svMain.setVisibility(opened ? View.GONE : View.VISIBLE);
        });

        btnGrid.setOnClickListener(x -> {
            grid = !grid;
            btnGrid.setText(grid ? "网格：开" : "网格：关");
            draw();
        });

        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                clean(s);
                draw();
            }
        });

        findViewById(R.id.btn_generate).setOnClickListener(x -> save());
    }

    void fillTable() {
        for (int i = 0; i < 1024; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(8, 2, 8, 2);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView t = new TextView(this);
            t.setText(bin(i));
            t.setTextSize(12);
            t.setWidth(120);
            row.addView(t);

            View b = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(80, 24);
            lp.setMargins(8, 0, 8, 0);
            b.setLayoutParams(lp);
            b.setBackgroundColor(color(i));
            row.addView(b);

            llTable.addView(row);
        }
    }

    String bin(int v) {
        StringBuilder sb = new StringBuilder();
        for (int i = BITS - 1; i >= 0; i--) sb.append((v >> i) & 1);
        return sb.toString();
    }

    void clean(Editable s) {
        String str = s.toString();
        StringBuilder sb = new StringBuilder();
        boolean bad = false;
        for (char c : str.toCharArray()) {
            if (c == '0' || c == '1') sb.append(c);
            else if (c == ' ' || c == '\n' || c == '\r' || c == '\t') sb.append(c);
            else bad = true;
        }
        err.setVisibility(bad ? View.VISIBLE : View.GONE);
        if (bad) err.setText("输入 0 和 1 之外的数将会被机器忽略");
        if (!sb.toString().equals(str)) s.replace(0, s.length(), sb.toString());
    }

    Bitmap build() {
        String raw = et.getText().toString().replaceAll("[^01]", "");
        int have = raw.length() / BITS;

        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        int idx = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int v = WHITE;
                if (idx < have) v = Integer.parseInt(raw.substring(idx * BITS, idx * BITS + BITS), 2);
                b.setPixel(x, y, color(v));
                idx++;
            }
        }

        if (!grid) return b;

        Bitmap g = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(g);
        cv.drawBitmap(b, 0, 0, null);

        float step = size / 16f;
        if (step < 4) step = 4;

        Paint p = new Paint();
        p.setColor(0xFFCCCCCC);
        p.setStrokeWidth(1);
        for (float i = 0; i <= size; i += step) {
            cv.drawLine(i, 0, i, size, p);
            cv.drawLine(0, i, size, i, p);
        }
        return g;
    }

    void draw() {
        iv.setImageBitmap(build());
    }

    int color(int v) {
        int r = (v >> 6) & 0x0F;
        int g = (v >> 3) & 0x07;
        int b = v & 0x07;
        return Color.rgb(r * 255 / 15, g * 255 / 7, b * 255 / 7);
    }

    void save() {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "画画成果/硬核画画");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "画画成果.png");
            FileOutputStream fos = new FileOutputStream(f);
            build().compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Toast.makeText(this, "保存到：" + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}