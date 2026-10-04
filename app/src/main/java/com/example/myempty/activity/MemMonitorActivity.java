package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.io.RandomAccessFile;

public class MemMonitorActivity extends Activity {

    private TextView tvTotal;
    private TextView tvUsed;
    private TextView tvAvail;
    private TextView tvPercent;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refresh();
            ui.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_mem_monitor);

        tvTotal = findViewById(R.id.tv_mem_m_total);
        tvUsed = findViewById(R.id.tv_mem_m_used);
        tvAvail = findViewById(R.id.tv_mem_m_avail);
        tvPercent = findViewById(R.id.tv_mem_m_percent);

        refresh();
        ui.postDelayed(tick, 1000);
    }

    private void refresh() {
        new Thread(() -> {
            long total = 0, avail = 0, free = 0, buffers = 0, cached = 0;
            try {
                RandomAccessFile raf = new RandomAccessFile("/proc/meminfo", "r");
                String line;
                while ((line = raf.readLine()) != null) {
                    if (line.startsWith("MemTotal:")) {
                        total = parseKb(line);
                    } else if (line.startsWith("MemAvailable:")) {
                        avail = parseKb(line);
                    } else if (line.startsWith("MemFree:")) {
                        free = parseKb(line);
                    } else if (line.startsWith("Buffers:")) {
                        buffers = parseKb(line);
                    } else if (line.startsWith("Cached:")) {
                        cached = parseKb(line);
                    }
                }
                raf.close();
            } catch (Exception ignored) {}

            if (avail == 0) avail = free + buffers + cached;
            long used = total - avail;
            int percent = total > 0 ? (int) (used * 100 / total) : 0;

            final long ft = total;
            final long fu = used;
            final long fa = avail;
            final int fp = percent;

            ui.post(() -> {
                tvTotal.setText("总量: " + fmt(ft));
                tvUsed.setText("已用: " + fmt(fu));
                tvAvail.setText("可用: " + fmt(fa));
                tvPercent.setText("占用: " + fp + "%");
            });
        }).start();
    }

    private long parseKb(String line) {
        try {
            String[] parts = line.split("\\s+");
            return Long.parseLong(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    private String fmt(long kb) {
        if (kb < 1024) return kb + " KB";
        if (kb < 1024 * 1024) return String.format("%.1f MB", kb / 1024.0);
        return String.format("%.2f GB", kb / 1024.0 / 1024);
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(tick);
        super.onDestroy();
    }
}