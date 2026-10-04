package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.RandomAccessFile;

public class CpuMonitorActivity extends Activity {

    private TextView tvFreq;
    private TextView tvTemp;
    private TextView tvCores;
    private TextView tvLoad;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private long lastTotal = 0;
    private long lastIdle = 0;

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
        setContentView(R.layout.activity_cpu_monitor);

        tvFreq = findViewById(R.id.tv_cpu_freq);
        tvTemp = findViewById(R.id.tv_cpu_temp);
        tvCores = findViewById(R.id.tv_cpu_cores);
        tvLoad = findViewById(R.id.tv_cpu_load);

        refresh();
        ui.postDelayed(tick, 1000);
    }

    private void refresh() {
        new Thread(() -> {
            StringBuilder cores = new StringBuilder();
            int cnt = 0;
            for (int i = 0; i < 16; i++) {
                String f = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq";
                String freq = readOne(f);
                if (freq == null) break;
                try {
                    long khz = Long.parseLong(freq.trim());
                    cores.append("CPU").append(i).append(": ")
                        .append(khz / 1000).append(" MHz\n");
                } catch (Exception e) {
                    cores.append("CPU").append(i).append(": 读不到\n");
                }
                cnt++;
            }
            if (cnt == 0) cores.append("这机子不给读频率\n");

            String temp = readOne("/sys/class/thermal/thermal_zone0/temp");
            String tempStr;
            if (temp != null) {
                try {
                    double t = Double.parseDouble(temp.trim());
                    if (t > 1000) t /= 1000;
                    tempStr = String.format("%.1f °C", t);
                } catch (Exception e) {
                    tempStr = "读不到";
                }
            } else {
                tempStr = "读不到";
            }

            int totalCores = Runtime.getRuntime().availableProcessors();

            long[] cpu = readCpuStat();
            String loadStr = "计算中...";
            if (cpu != null && lastTotal > 0) {
                long dTotal = cpu[0] - lastTotal;
                long dIdle = cpu[1] - lastIdle;
                if (dTotal > 0) {
                    int usage = (int) ((dTotal - dIdle) * 100 / dTotal);
                    if (usage < 0) usage = 0;
                    if (usage > 100) usage = 100;
                    loadStr = usage + " %";
                }
            }
            if (cpu != null) {
                lastTotal = cpu[0];
                lastIdle = cpu[1];
            }

            final String fc = cores.toString();
            final String ft = tempStr;
            final int fn = totalCores;
            final String fl = loadStr;

            ui.post(() -> {
                tvFreq.setText(fc);
                tvTemp.setText("温度: " + ft);
                tvCores.setText("核心数: " + fn);
                tvLoad.setText("总占用: " + fl);
            });
        }).start();
    }

    private long[] readCpuStat() {
        try {
            RandomAccessFile raf = new RandomAccessFile("/proc/stat", "r");
            String line = raf.readLine();
            raf.close();
            if (line == null || !line.startsWith("cpu ")) return null;
            String[] parts = line.trim().split("\\s+");
            long total = 0;
            for (int i = 1; i < parts.length; i++) {
                total += Long.parseLong(parts[i]);
            }
            long idle = Long.parseLong(parts[4]) + Long.parseLong(parts[5]);
            return new long[]{total, idle};
        } catch (Exception e) {
            return null;
        }
    }

    private String readOne(String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String s = br.readLine();
            br.close();
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(tick);
        super.onDestroy();
    }
}