package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageWatermarkActivity extends AppCompatActivity {

    private final List<Uri> picked = new ArrayList<>();
    private TextView tvInfo;
    private EditText etText;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_image_watermark);

        tvInfo = findViewById(R.id.tv_wm_info);
        etText = findViewById(R.id.et_wm_text);
        etText.setText("MyEmptyActivity");

        findViewById(R.id.btn_wm_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_wm_do).setOnClickListener(v -> doWatermark());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            picked.clear();
            if (data.getClipData() != null) {
                int n = data.getClipData().getItemCount();
                for (int i = 0; i < n; i++) picked.add(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) {
                picked.add(data.getData());
            }
            tvInfo.setText(getString(R.string.bp_selected, picked.size()));
        }
    }

    private void doWatermark() {
        if (picked.isEmpty()) {
            Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show();
            return;
        }
        String text = etText.getText().toString();
        if (text.isEmpty()) return;

        final String ftext = text;
        new Thread(() -> {
            File outDir = new File("/storage/emulated/0/Download/水印图片");
            if (!outDir.exists()) outDir.mkdirs();

            int ok = 0;
            for (Uri uri : picked) {
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    Bitmap src = BitmapFactory.decodeStream(is);
                    is.close();
                    if (src == null) continue;

                    Bitmap out = src.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas cv = new Canvas(out);

                    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                    p.setColor(Color.WHITE);
                    p.setTextSize(Math.max(24, out.getWidth() / 30f));
                    p.setShadowLayer(4, 2, 2, Color.BLACK);

                    float pad = out.getWidth() * 0.03f;
                    float x = out.getWidth() - p.measureText(ftext) - pad;
                    float y = out.getHeight() - pad;
                    cv.drawText(ftext, x, y, p);

                    File f = new File(outDir, "watermark_" + System.currentTimeMillis() + "_" + ok + ".jpg");
                    FileOutputStream fos = new FileOutputStream(f);
                    out.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                    fos.close();
                    out.recycle();
                    src.recycle();
                    ok++;
                } catch (Exception ignored) {}
            }
            final int fok = ok;
            ui.post(() -> {
                tvInfo.setText(getString(R.string.file_done) + ": " + fok);
                new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.file_done))
                    .setMessage(outDir.getAbsolutePath())
                    .setPositiveButton(getString(R.string.lang_ok), null).show();
            });
        }).start();
    }
}