package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class QrBatchActivity extends Activity {

    private EditText etInput;
    private EditText etPrefix;
    private TextView tvStatus;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_qr_batch);

        etInput = findViewById(R.id.et_qrb_input);
        etPrefix = findViewById(R.id.et_qrb_prefix);
        tvStatus = findViewById(R.id.tv_qrb_status);

        etPrefix.setText("qr");

        findViewById(R.id.btn_qrb_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("text/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_qrb_do).setOnClickListener(v -> doGen());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                is.close();
                etInput.setText(new String(bos.toByteArray(), "UTF-8"));
            } catch (Exception e) {
                Toast.makeText(this, "读不了", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void doGen() {
        String text = etInput.getText().toString();
        if (text.trim().isEmpty()) {
            Toast.makeText(this, "先输入或选文件", Toast.LENGTH_SHORT).show();
            return;
        }
        final String prefix = etPrefix.getText().toString().trim();
        final String finalPrefix = prefix.isEmpty() ? "qr" : prefix;

        final String[] lines = text.split("\n");

        tvStatus.setText("生成中...");

        new Thread(() -> {
            try {
                File dir = new File(Environment.getExternalStorageDirectory(),
                    "Download/二维码批量");
                if (!dir.exists()) dir.mkdirs();

                int ok = 0, skip = 0;
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty()) { skip++; continue; }

                    Map<EncodeHintType, Object> hints = new HashMap<>();
                    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                    hints.put(EncodeHintType.MARGIN, 1);

                    BitMatrix matrix = new MultiFormatWriter().encode(
                        line, BarcodeFormat.QR_CODE, 720, 720, hints);

                    int w = matrix.getWidth();
                    int h = matrix.getHeight();
                    int[] px = new int[w * h];
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            px[y * w + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                        }
                    }

                    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    bmp.setPixels(px, 0, w, 0, 0, w, h);

                    File f = new File(dir, finalPrefix + "_" + (i + 1) + ".png");
                    FileOutputStream fos = new FileOutputStream(f);
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                    bmp.recycle();
                    ok++;

                    final int cur = ok;
                    ui.post(() -> tvStatus.setText("已生成 " + cur + " 个"));
                }

                final int fok = ok, fskip = skip;
                ui.post(() -> tvStatus.setText("搞定，成功 " + fok + " 个，跳过空行 " + fskip + " 个\n"
                    + dir.getAbsolutePath()));
            } catch (Exception e) {
                ui.post(() -> tvStatus.setText("生成失败: " + e.getMessage()));
            }
        }).start();
    }
}