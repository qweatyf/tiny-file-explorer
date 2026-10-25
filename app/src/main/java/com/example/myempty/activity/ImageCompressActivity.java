package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageCompressActivity extends AppCompatActivity {

    private final List<Uri> picked = new ArrayList<>();
    private TextView tvInfo;
    private SeekBar sbQuality;
    private EditText etMaxSize;
    private int quality = 80;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_image_compress);

        tvInfo = findViewById(R.id.tv_comp_info);
        sbQuality = findViewById(R.id.sb_comp_quality);
        etMaxSize = findViewById(R.id.et_comp_max_size);
        etMaxSize.setText("1920");

        sbQuality.setMax(100);
        sbQuality.setProgress(quality);
        sbQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int v, boolean u) { quality = v; updateInfo(); }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        findViewById(R.id.btn_comp_img_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_comp_img_do).setOnClickListener(v -> doCompress());
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
            updateInfo();
        }
    }

    private void updateInfo() {
        tvInfo.setText(getString(R.string.bp_selected, picked.size()) + "  |  " + quality);
    }

    private void doCompress() {
        if (picked.isEmpty()) {
            Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show();
            return;
        }
        int maxSize = 1920;
        try { maxSize = Integer.parseInt(etMaxSize.getText().toString().trim()); } catch (Exception ignored) {}
        if (maxSize <= 0) maxSize = 1920;

        final int fMax = maxSize;
        final int fQual = quality;

        new Thread(() -> {
            File outDir = new File("/storage/emulated/0/Download/压缩图片");
            if (!outDir.exists()) outDir.mkdirs();

            int ok = 0;
            for (Uri uri : picked) {
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    Bitmap src = BitmapFactory.decodeStream(is);
                    is.close();
                    if (src == null) continue;

                    int w = src.getWidth(), h = src.getHeight();
                    int max = Math.max(w, h);
                    if (max > fMax) {
                        float scale = (float) fMax / max;
                        src = Bitmap.createScaledBitmap(src, (int) (w * scale), (int) (h * scale), true);
                    }

                    File f = new File(outDir, "compressed_" + System.currentTimeMillis() + "_" + ok + ".jpg");
                    FileOutputStream fos = new FileOutputStream(f);
                    src.compress(Bitmap.CompressFormat.JPEG, fQual, fos);
                    fos.close();
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