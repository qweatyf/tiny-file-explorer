package com.example.myempty.activity2;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class QrGenActivity extends Activity {

    private EditText etInput;
    private ImageView ivQr;
    private Button btnGen;
    private Button btnSave;
    private Button btnCopy;

    private Bitmap currentQr;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_qr_gen);

        etInput = findViewById(R.id.et_qr_text);
        ivQr = findViewById(R.id.iv_qr);
        btnGen = findViewById(R.id.btn_qr_gen_do);
        btnSave = findViewById(R.id.btn_qr_save);
        btnCopy = findViewById(R.id.btn_qr_copy);

        btnSave.setEnabled(false);
        btnCopy.setEnabled(false);

        btnGen.setOnClickListener(v -> doGen());

        btnSave.setOnClickListener(v -> {
            if (currentQr != null) saveQr();
        });

        btnCopy.setOnClickListener(v -> {
            String s = etInput.getText().toString();
            if (s.isEmpty()) return;
            ClipboardManager cm = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("qr", s));
            Toast.makeText(this, "内容已经拷贝了", Toast.LENGTH_SHORT).show();
        });
    }

    private void doGen() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "好歹打点东西进去啊", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGen.setEnabled(false);
        btnGen.setText("正在生成…");

        final String content = text;
        new Thread(() -> {
            try {
                Map<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                hints.put(EncodeHintType.MARGIN, 1);

                BitMatrix m = new MultiFormatWriter().encode(
                    content, BarcodeFormat.QR_CODE, 720, 720, hints);

                int w = m.getWidth();
                int h = m.getHeight();
                int[] px = new int[w * h];
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        px[y * w + x] = m.get(x, y) ? Color.BLACK : Color.WHITE;
                    }
                }

                final Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                bmp.setPixels(px, 0, w, 0, 0, w, h);

                ui.post(() -> {
                    if (currentQr != null && !currentQr.isRecycled()) {
                        currentQr.recycle();
                    }
                    currentQr = bmp;
                    ivQr.setImageBitmap(bmp);
                    ivQr.setVisibility(View.VISIBLE);
                    btnGen.setEnabled(true);
                    btnGen.setText("生成二维码");
                    btnSave.setEnabled(true);
                    btnCopy.setEnabled(true);
                });
            } catch (Exception e) {
                ui.post(() -> {
                    btnGen.setEnabled(true);
                    btnGen.setText("生成二维码");
                    Toast.makeText(this, "生成失败：" + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void saveQr() {
        try {
            String fname = "qr_" + System.currentTimeMillis() + ".png";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Images.Media.DISPLAY_NAME, fname);
                cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                cv.put(MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/二维码存放地");

                Uri uri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);

                if (uri == null) throw new Exception("插 MediaStore 失败");

                OutputStream os = getContentResolver().openOutputStream(uri);
                currentQr.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.close();

                Toast.makeText(this, "存好了，去相册看吧", Toast.LENGTH_LONG).show();
            } else {
                File dir = new File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES),
                    "二维码存放地");
                if (!dir.exists()) dir.mkdirs();

                File f = new File(dir, fname);
                FileOutputStream fos = new FileOutputStream(f);
                currentQr.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();

                MediaScannerConnection.scanFile(this,
                    new String[]{f.getAbsolutePath()}, null, null);

                Toast.makeText(this, "存好了：" + f.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "存不了：" + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentQr != null && !currentQr.isRecycled()) {
            currentQr.recycle();
            currentQr = null;
        }
    }
}