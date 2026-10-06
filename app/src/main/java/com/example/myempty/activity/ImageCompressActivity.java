package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageCompressActivity extends Activity {

    private Bitmap srcBmp;
    private ImageView ivPreview;
    private TextView tvInfo;
    private SeekBar sbQuality;
    private EditText etMaxSize;
    private int quality = 80;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_image_compress);

        ivPreview = findViewById(R.id.iv_comp_preview);
        tvInfo = findViewById(R.id.tv_comp_info);
        sbQuality = findViewById(R.id.sb_comp_quality);
        etMaxSize = findViewById(R.id.et_comp_max_size);

        etMaxSize.setText("1920");

        sbQuality.setMax(100);
        sbQuality.setProgress(quality);
        sbQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int v, boolean u) {
                quality = v;
                updateInfo();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        findViewById(R.id.btn_comp_img_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            i.setType("image/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_comp_img_do).setOnClickListener(v -> doCompress());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                srcBmp = BitmapFactory.decodeStream(is);
                is.close();
                if (srcBmp != null) {
                    ivPreview.setImageBitmap(srcBmp);
                    ivPreview.setVisibility(View.VISIBLE);
                    updateInfo();
                }
            } catch (Exception e) {
                Toast.makeText(this, "读图失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateInfo() {
        if (srcBmp == null) return;
        tvInfo.setText(srcBmp.getWidth() + " × " + srcBmp.getHeight()
            + "，质量 " + quality);
    }

    private void doCompress() {
        if (srcBmp == null) {
            Toast.makeText(this, "先选图", Toast.LENGTH_SHORT).show();
            return;
        }
        int maxSize = 1920;
        try { maxSize = Integer.parseInt(etMaxSize.getText().toString().trim()); }
        catch (Exception ignored) {}
        if (maxSize <= 0) maxSize = 1920;

        final int fMax = maxSize;
        new Thread(() -> {
            try {
                Bitmap out = srcBmp;
                int w = out.getWidth();
                int h = out.getHeight();
                int max = Math.max(w, h);
                if (max > fMax) {
                    float scale = (float) fMax / max;
                    out = Bitmap.createScaledBitmap(out,
                        (int) (w * scale), (int) (h * scale), true);
                }

                File outDir = new File("/storage/emulated/0/Download/压缩图片");
                if (!outDir.exists()) outDir.mkdirs();
                File f = new File(outDir, "compressed_" + System.currentTimeMillis() + ".jpg");
                FileOutputStream fos = new FileOutputStream(f);
                out.compress(Bitmap.CompressFormat.JPEG, quality, fos);
                fos.close();

                if (out != srcBmp) out.recycle();

                runOnUiThread(() -> Toast.makeText(this,
                    "压好了: " + f.getAbsolutePath() + "\n大小: " + (f.length() / 1024) + " KB",
                    Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    "压不了: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}