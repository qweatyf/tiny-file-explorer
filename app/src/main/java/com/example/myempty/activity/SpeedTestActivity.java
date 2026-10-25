package com.example.myempty.activity2;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SpeedTestActivity extends AppCompatActivity {

    private EditText etUrl;
    private TextView tvResult, tvStatus;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean testing = false;

    private static final String DEFAULT_URL =
        "https://mirrors.aliyun.com/deepin/releases/20.9/deepin-desktop-community-20.9-amd64.iso";

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_speed_test);

        etUrl = findViewById(R.id.et_speed_url);
        tvResult = findViewById(R.id.tv_speed_result);
        tvStatus = findViewById(R.id.tv_speed_status);

        etUrl.setText(DEFAULT_URL);

        findViewById(R.id.btn_speed_start).setOnClickListener(v -> start());
    }

    private void start() {
        if (testing) return;
        String url = etUrl.getText().toString().trim();
        if (url.isEmpty()) return;

        testing = true;
        tvResult.setText("");
        tvStatus.setText("...");

        final String finalUrl = url;
        new Thread(() -> {
            try {
                URL u = new URL(finalUrl);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "MiniFileManager/1.0");

                long t0 = System.currentTimeMillis();
                conn.connect();
                long connectMs = System.currentTimeMillis() - t0;
                int code = conn.getResponseCode();
                long contentLen = conn.getContentLength();

                InputStream is = conn.getInputStream();
                byte[] buf = new byte[16384];
                int n;
                long total = 0;
                long startTime = System.currentTimeMillis();
                long lastReport = startTime;
                long windowStart = startTime;
                long windowBytes = 0;

                final StringBuilder result = new StringBuilder();
                result.append(code).append("\n");
                result.append(connectMs).append(" ms\n");
                if (contentLen > 0) result.append(fmt(contentLen)).append("\n");
                result.append("\n");

                while ((n = is.read(buf)) > 0) {
                    total += n;
                    windowBytes += n;
                    long now = System.currentTimeMillis();
                    long totalElapsed = now - startTime;

                    if (now - lastReport >= 500) {
                        long windowMs = now - windowStart;
                        long windowSpeed = windowMs > 0 ? windowBytes * 1000 / windowMs : 0;
                        long avgSpeed = totalElapsed > 0 ? total * 1000 / totalElapsed : 0;
                        final long ft = total, fws = windowSpeed, fas = avgSpeed, fel = totalElapsed;
                        ui.post(() -> {
                            tvStatus.setText(fmt(ft) + "  " + (fel / 1000) + "s");
                            tvResult.setText(result.toString()
                                + fmtSpeed(fws) + "\n" + fmtSpeed(fas) + "\n");
                        });
                        lastReport = now;
                        windowStart = now;
                        windowBytes = 0;
                    }
                    if (totalElapsed > 20000) break;
                }
                is.close();
                conn.disconnect();

                long totalMs = System.currentTimeMillis() - startTime;
                long avgSpeed = totalMs > 0 ? total * 1000 / totalMs : 0;
                final String finalResult = result.toString()
                    + fmt(total) + "\n"
                    + (totalMs / 1000.0) + " s\n"
                    + fmtSpeed(avgSpeed) + "\n"
                    + (contentLen > 0 ? (total * 100 / contentLen) + "%\n" : "");
                ui.post(() -> {
                    testing = false;
                    tvResult.setText(finalResult);
                    tvStatus.setText(getString(R.string.file_done));
                });
            } catch (Exception e) {
                ui.post(() -> { testing = false; tvStatus.setText(e.getMessage()); });
            }
        }).start();
    }

    private String fmt(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) return String.format("%.1f MB", b / 1024.0 / 1024);
        return String.format("%.2f GB", b / 1024.0 / 1024 / 1024);
    }

    private String fmtSpeed(long bps) {
        if (bps < 1024) return bps + " B/s";
        if (bps < 1024 * 1024) return String.format("%.1f KB/s", bps / 1024.0);
        return String.format("%.2f MB/s", bps / 1024.0 / 1024);
    }
}