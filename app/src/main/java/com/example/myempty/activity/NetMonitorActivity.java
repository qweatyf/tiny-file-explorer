package com.example.myempty.activity2;

import android.app.Activity;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

public class NetMonitorActivity extends Activity {

    private TextView tvRx;
    private TextView tvTx;
    private TextView tvRxSpeed;
    private TextView tvTxSpeed;
    private TextView tvTotal;

    private long lastRx = 0;
    private long lastTx = 0;
    private long lastTime = 0;

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
        setContentView(R.layout.activity_net_monitor);

        tvRx = findViewById(R.id.tv_net_rx);
        tvTx = findViewById(R.id.tv_net_tx);
        tvRxSpeed = findViewById(R.id.tv_net_rx_speed);
        tvTxSpeed = findViewById(R.id.tv_net_tx_speed);
        tvTotal = findViewById(R.id.tv_net_total);

        long rx = TrafficStats.getTotalRxBytes();
        long tx = TrafficStats.getTotalTxBytes();
        lastRx = rx;
        lastTx = tx;
        lastTime = System.currentTimeMillis();

        refresh();
        ui.postDelayed(tick, 1000);
    }

    private void refresh() {
        long rx = TrafficStats.getTotalRxBytes();
        long tx = TrafficStats.getTotalTxBytes();
        long now = System.currentTimeMillis();

        if (rx < 0) rx = 0;
        if (tx < 0) tx = 0;

        long dt = now - lastTime;
        long rxSpeed = 0;
        long txSpeed = 0;
        if (dt > 0) {
            rxSpeed = (rx - lastRx) * 1000 / dt;
            txSpeed = (tx - lastTx) * 1000 / dt;
        }
        if (rxSpeed < 0) rxSpeed = 0;
        if (txSpeed < 0) txSpeed = 0;

        lastRx = rx;
        lastTx = tx;
        lastTime = now;

        tvRx.setText("累计收: " + fmt(rx));
        tvTx.setText("累计发: " + fmt(tx));
        tvRxSpeed.setText("下载: " + fmtSpeed(rxSpeed));
        tvTxSpeed.setText("上传: " + fmtSpeed(txSpeed));
        tvTotal.setText("总共: " + fmt(rx + tx));
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

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(tick);
        super.onDestroy();
    }
}