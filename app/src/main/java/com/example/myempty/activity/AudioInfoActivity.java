package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AudioInfoActivity extends AppCompatActivity {

    private Uri uri;
    private TextView tvInfo;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_audio_info);

        tvInfo = findViewById(R.id.tv_ai_info);

        findViewById(R.id.btn_ai_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("audio/*");
            startActivityForResult(i, 100);
        });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            uri = data.getData();
            tvInfo.setText(uri.toString());
            showInfo();
        }
    }

    private File copyToCache(Uri uri) throws Exception {
        File tmp = new File(getCacheDir(), "ai_" + System.currentTimeMillis() + ".tmp");
        InputStream is = getContentResolver().openInputStream(uri);
        FileOutputStream fos = new FileOutputStream(tmp);
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
        is.close();
        fos.close();
        return tmp;
    }

    private void showInfo() {
        new Thread(() -> {
            MediaExtractor ex = new MediaExtractor();
            File tmp = null;
            try {
                tmp = copyToCache(uri);
                ex.setDataSource(tmp.getAbsolutePath());
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < ex.getTrackCount(); i++) {
                    MediaFormat f = ex.getTrackFormat(i);
                    String mime = f.getString(MediaFormat.KEY_MIME);
                    if (mime == null || !mime.startsWith("audio/")) continue;

                    sb.append(mime).append("\n");
                    if (f.containsKey(MediaFormat.KEY_DURATION)) {
                        long us = f.getLong(MediaFormat.KEY_DURATION);
                        sb.append(us / 1000000).append(" s\n");
                    }
                    if (f.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sb.append(f.getInteger(MediaFormat.KEY_SAMPLE_RATE)).append(" Hz\n");
                    }
                    if (f.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        sb.append(f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)).append("\n");
                    }
                    if (f.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        sb.append(f.getInteger(MediaFormat.KEY_BIT_RATE) / 1000).append(" kbps\n");
                    }
                    sb.append("\n");
                }

                runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.title_audio_info))
                    .setMessage(sb.toString())
                    .setPositiveButton(getString(R.string.lang_ok), null).show());
            } catch (Throwable e) {
                runOnUiThread(() -> Toast.makeText(this,
                    e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                try { ex.release(); } catch (Exception ignored) {}
                if (tmp != null) tmp.delete();
            }
        }).start();
    }
}