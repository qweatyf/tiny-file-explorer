package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class GifDecoderActivity extends Activity {

    private Uri gifUri;
    private TextView tvInfo;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_gif_decoder);

        tvInfo = findViewById(R.id.tv_gifd_info);

        findViewById(R.id.btn_gifd_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_gifd_decode).setOnClickListener(v -> askRange());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            gifUri = data.getData();
            tvInfo.setText("已选: " + gifUri);
        }
    }

    private void askRange() {
        if (gifUri == null) {
            Toast.makeText(this, "先选 GIF", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] ops = {"拆全部", "拆指定范围"};
        new AlertDialog.Builder(this)
            .setItems(ops, (d, w) -> {
                if (w == 0) doDecode(0, -1);
                else {
                    android.widget.LinearLayout box = new android.widget.LinearLayout(this);
                    box.setOrientation(android.widget.LinearLayout.VERTICAL);
                    box.setPadding(40, 20, 40, 0);

                    final EditText etS = new EditText(this);
                    etS.setHint("起始帧（从 0 开始）");
                    etS.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                    box.addView(etS);

                    final EditText etE = new EditText(this);
                    etE.setHint("结束帧（不含）");
                    etE.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                    box.addView(etE);

                    new AlertDialog.Builder(this)
                        .setTitle("范围")
                        .setView(box)
                        .setPositiveButton("拆", (dd, ww) -> {
                            int s = parseInt(etS.getText().toString(), 0);
                            int e = parseInt(etE.getText().toString(), -1);
                            doDecode(s, e);
                        })
                        .setNegativeButton("算了", null)
                        .show();
                }
            })
            .show();
    }

    private void doDecode(final int start, final int end) {
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(gifUri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                is.close();
                byte[] raw = bos.toByteArray();

                android.graphics.Movie movie =
                    android.graphics.Movie.decodeByteArray(raw, 0, raw.length);
                if (movie == null) {
                    ui.post(() -> Toast.makeText(this,
                        "这不是 GIF 或者读不了", Toast.LENGTH_SHORT).show());
                    return;
                }

                int duration = movie.duration();
                if (duration <= 0) duration = 500;
                int totalFrames = Math.max(1, duration / 50);

                int from = Math.max(0, start);
                int to = (end < 0 || end > totalFrames) ? totalFrames : end;
                if (to <= from) to = totalFrames;

                File outDir = new File("/storage/emulated/0/Download/GIF帧/"
                    + System.currentTimeMillis());
                if (!outDir.exists()) outDir.mkdirs();

                int w = movie.width();
                int h = movie.height();
                Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                android.graphics.Canvas cv = new android.graphics.Canvas(bmp);

                int saved = 0;
                for (int i = from; i < to; i++) {
                    int time = i * 50;
                    movie.setTime(time);
                    cv.drawColor(0x00000000);
                    movie.draw(cv, 0, 0);

                    File f = new File(outDir, "frame_" + i + ".png");
                    FileOutputStream fos = new FileOutputStream(f);
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                    saved++;
                }

                bmp.recycle();

                final int s = saved;
                final File fd = outDir;
                ui.post(() -> Toast.makeText(this,
                    "拆了 " + s + " 帧\n" + fd.getAbsolutePath(),
                    Toast.LENGTH_LONG).show());
            } catch (Throwable e) {
                ui.post(() -> Toast.makeText(this,
                    "拆失败: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}