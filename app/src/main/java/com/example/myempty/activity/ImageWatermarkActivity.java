package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageWatermarkActivity extends Activity {

    private Bitmap srcBmp;
    private ImageView ivPreview;
    private EditText etText;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_image_watermark);

        ivPreview = findViewById(R.id.iv_wm_preview);
        etText = findViewById(R.id.et_wm_text);
        etText.setText("MyEmptyActivity");

        findViewById(R.id.btn_wm_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            i.setType("image/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_wm_do).setOnClickListener(v -> doWatermark());
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
                    ivPreview.setVisibility(ImageView.VISIBLE);
                }
            } catch (Exception e) {
                Toast.makeText(this, "读图失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void doWatermark() {
        if (srcBmp == null) {
            Toast.makeText(this, "先选图", Toast.LENGTH_SHORT).show();
            return;
        }
        String text = etText.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "输水印文字", Toast.LENGTH_SHORT).show();
            return;
        }

        final String ftext = text;
        new Thread(() -> {
            try {
                Bitmap out = srcBmp.copy(Bitmap.Config.ARGB_8888, true);
                Canvas cv = new Canvas(out);

                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(Color.WHITE);
                p.setTextSize(Math.max(24, out.getWidth() / 30f));
                p.setShadowLayer(4, 2, 2, Color.BLACK);

                float pad = out.getWidth() * 0.03f;
                float x = out.getWidth() - p.measureText(ftext) - pad;
                float y = out.getHeight() - pad;

                cv.drawText(ftext, x, y, p);

                File outDir = new File("/storage/emulated/0/Download/水印图片");
                if (!outDir.exists()) outDir.mkdirs();
                File f = new File(outDir, "watermark_" + System.currentTimeMillis() + ".jpg");
                FileOutputStream fos = new FileOutputStream(f);
                out.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                fos.close();
                out.recycle();

                runOnUiThread(() -> Toast.makeText(this,
                    "加好了: " + f.getAbsolutePath(),
                    Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    "加失败: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}