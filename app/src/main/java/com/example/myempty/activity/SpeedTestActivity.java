package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SpeedTestActivity extends Activity {

    private EditText etUrl;
    private TextView tvResult;
    private TextView tvStatus;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean testing = false;

    private static final String DEFAULT_URL =
        "https://mirrors.aliyun.com/deepin/releases/20.9/deepin-desktop-community-20.9-amd64.iso";

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
        if (testing) {
            Toast.makeText(this, "还在测", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = etUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "URL 填一下", Toast.LENGTH_SHORT).show();
            return;
        }

        testing = true;
        tvResult.setText("");
        tvStatus.setText("连接中...");

        final String finalUrl = url;
        new Thread(() -> {
            try {
                URL u = new URL(finalUrl);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "MyEmptyActivity/1.0");

                long t0 = System.currentTimeMillis();
                conn.connect();
                long connectMs = System.currentTimeMillis() - t0;

                int code = conn.getResponseCode();
                long contentLen = conn.getContentLength();

                ui.post(() -> tvStatus.setText("开始下载测速..."));

                InputStream is = conn.getInputStream();
                byte[] buf = new byte[16384];
                long total = 0;
                long startTime = System.currentTimeMillis();
                long lastReport = startTime;
                long windowStart = startTime;
                long windowBytes = 0;

                final StringBuilder result = new StringBuilder();
                result.append("状态码: ").append(code).append("\n");
                result.append("连接耗时: ").append(connectMs).append(" ms\n");
                if (contentLen > 0) {
                    result.append("文件大小: ").append(fmt(contentLen)).append("\n");
                }
                result.append("\n");

                int n;
                long maxTime = 20000;
                while ((n = is.read(buf)) > 0) {
                    total += n;
                    windowBytes += n;
                    long now = System.currentTimeMillis();
                    long totalElapsed = now - startTime;

                    if (now - lastReport >= 500) {
                        long windowMs = now - windowStart;
                        long windowSpeed = windowMs > 0
                            ? windowBytes * 1000 / windowMs : 0;
                        long avgSpeed = totalElapsed > 0
                            ? total * 1000 / totalElapsed : 0;

                        final long ft = total;
                        final long fws = windowSpeed;
                        final long fas = avgSpeed;
                        final long fel = totalElapsed;

                        ui.post(() -> {
                            tvStatus.setText("已下载 " + fmt(ft)
                                + "  用时 " + (fel / 1000) + "s");
                            tvResult.setText(result.toString()
                                + "即时速度: " + fmtSpeed(fws) + "\n"
                                + "平均速度: " + fmtSpeed(fas) + "\n");
                        });

                        lastReport = now;
                        windowStart = now;
                        windowBytes = 0;
                    }

                    if (totalElapsed > maxTime) break;
                }
                is.close();
                conn.disconnect();

                long totalMs = System.currentTimeMillis() - startTime;
                long avgSpeed = totalMs > 0 ? total * 1000 / totalMs : 0;

                final String finalResult = result.toString()
                    + "已下载: " + fmt(total) + "\n"
                    + "总用时: " + (totalMs / 1000.0) + " s\n"
                    + "平均速度: " + fmtSpeed(avgSpeed) + "\n"
                    + (contentLen > 0 ? "完成度: " + (total * 100 / contentLen) + "%\n" : "");
                ui.post(() -> {
                    testing = false;
                    tvResult.setText(finalResult);
                    tvStatus.setText("测速完成");
                });
            } catch (Exception e) {
                ui.post(() -> {
                    testing = false;
                    tvStatus.setText("测速失败: " + e.getMessage());
                });
            }
        }).start();
    }

    private String fmt(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) return String.format("%.1f MB", b / 1024.0 / 1024);
        return String.format("%.2f GB", b / 1024.0 / 1024 / 1024);
    }

    private String fmtSpeed(long bytesPerSec) {
        if (bytesPerSec < 1024) return bytesPerSec + " B/s";
        if (bytesPerSec < 1024 * 1024) {
            return String.format("%.1f KB/s", bytesPerSec / 1024.0);
        }
        return String.format("%.2f MB/s", bytesPerSec / 1024.0 / 1024);
    }
}