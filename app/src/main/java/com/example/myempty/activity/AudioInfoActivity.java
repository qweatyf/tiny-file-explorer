package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AudioInfoActivity extends Activity {

    private Uri uri;
    private TextView tvInfo;

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
            tvInfo.setText("已选: " + uri);
            showInfo();
        }
    }

    private File copyToCache(Uri uri) throws Exception {
        File tmp = new File(getCacheDir(),
            "ai_" + System.currentTimeMillis() + ".tmp");
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
                sb.append("文件: ").append(uri.getLastPathSegment()).append("\n\n");

                for (int i = 0; i < ex.getTrackCount(); i++) {
                    MediaFormat f = ex.getTrackFormat(i);
                    String mime = f.getString(MediaFormat.KEY_MIME);
                    if (mime == null || !mime.startsWith("audio/")) continue;

                    sb.append("轨道 ").append(i).append("\n");
                    sb.append("  格式: ").append(mime).append("\n");
                    if (f.containsKey(MediaFormat.KEY_DURATION)) {
                        long us = f.getLong(MediaFormat.KEY_DURATION);
                        sb.append("  时长: ").append(us / 1000000).append(" 秒\n");
                    }
                    if (f.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sb.append("  采样率: ").append(f.getInteger(MediaFormat.KEY_SAMPLE_RATE)).append(" Hz\n");
                    }
                    if (f.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        sb.append("  声道: ").append(f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)).append("\n");
                    }
                    if (f.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        sb.append("  码率: ").append(f.getInteger(MediaFormat.KEY_BIT_RATE) / 1000).append(" kbps\n");
                    }
                    sb.append("\n");
                }

                runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("音频信息")
                    .setMessage(sb.toString())
                    .setPositiveButton("知道了", null)
                    .show());
            } catch (Throwable e) {
                runOnUiThread(() -> Toast.makeText(this,
                    "读不了: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            } finally {
                try { ex.release(); } catch (Exception ignored) {}
                if (tmp != null) tmp.delete();
            }
        }).start();
    }
}