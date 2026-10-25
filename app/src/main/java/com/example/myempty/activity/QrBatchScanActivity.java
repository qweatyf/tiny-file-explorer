package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class QrBatchScanActivity extends AppCompatActivity {

    private TextView tvResult;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_qr_batch_scan);

        tvResult = findViewById(R.id.tv_qrs_status);

        findViewById(R.id.btn_qrs_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(i, 300);
        });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 300 && res == RESULT_OK && data != null) {
            List<Uri> uris = new ArrayList<>();
            if (data.getClipData() != null) {
                int n = data.getClipData().getItemCount();
                for (int i = 0; i < n; i++) uris.add(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) {
                uris.add(data.getData());
            }
            if (!uris.isEmpty()) decodeBatch(uris);
        }
    }

    private void decodeBatch(List<Uri> uris) {
        tvResult.setText(getString(R.string.file_search));
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            MultiFormatReader reader = new MultiFormatReader();
            int ok = 0;

            for (int i = 0; i < uris.size(); i++) {
                try {
                    InputStream is = getContentResolver().openInputStream(uris.get(i));
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();
                    if (bmp == null) continue;

                    int w = bmp.getWidth(), h = bmp.getHeight();
                    int[] px = new int[w * h];
                    bmp.getPixels(px, 0, w, 0, 0, w, h);
                    bmp.recycle();

                    LuminanceSource source = new RGBLuminanceSource(w, h, px);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                    Result result = reader.decode(bitmap);

                    sb.append("[").append(i + 1).append("] ").append(result.getText()).append("\n\n");
                    ok++;
                } catch (Exception e) {
                    sb.append("[").append(i + 1).append("] ").append(getString(R.string.file_fail)).append("\n\n");
                }
            }

            final String res = sb.toString();
            final int fok = ok;
            ui.post(() -> {
                tvResult.setText(res);
                if (fok > 0) {
                    new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.file_done))
                        .setMessage(getString(R.string.bp_selected, fok))
                        .setPositiveButton(getString(R.string.file_menu_copy), (d, w) -> {
                            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("qr", res));
                            Toast.makeText(this, getString(R.string.file_done), Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(getString(R.string.file_rename_cancel), null)
                        .show();
                }
            });
        }).start();
    }
}