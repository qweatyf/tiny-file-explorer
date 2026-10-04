package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageFormatActivity extends Activity {

    private Bitmap srcBmp;
    private ImageView ivPreview;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_image_format);

        ivPreview = findViewById(R.id.iv_fmt_preview);

        findViewById(R.id.btn_fmt_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            i.setType("image/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_fmt_do).setOnClickListener(v -> pickFormat());
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

    private void pickFormat() {
        if (srcBmp == null) {
            Toast.makeText(this, "先选图", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] fmts = {"PNG", "JPEG", "WEBP"};
        new AlertDialog.Builder(this)
            .setTitle("转成哪种")
            .setItems(fmts, (d, w) -> doConvert(fmts[w]))
            .show();
    }

    private void doConvert(String fmt) {
        new Thread(() -> {
            try {
                File outDir = new File("/storage/emulated/0/Download/转格式图片");
                if (!outDir.exists()) outDir.mkdirs();
                String ext = fmt.toLowerCase();
                if (ext.equals("jpeg")) ext = "jpg";
                File f = new File(outDir, "converted_" + System.currentTimeMillis() + "." + ext);
                FileOutputStream fos = new FileOutputStream(f);

                Bitmap.CompressFormat cf;
                if (fmt.equals("PNG")) cf = Bitmap.CompressFormat.PNG;
                else if (fmt.equals("WEBP")) cf = Bitmap.CompressFormat.WEBP;
                else cf = Bitmap.CompressFormat.JPEG;

                srcBmp.compress(cf, 95, fos);
                fos.close();

                runOnUiThread(() -> Toast.makeText(this,
                    "转好了: " + f.getAbsolutePath(),
                    Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    "转失败: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}