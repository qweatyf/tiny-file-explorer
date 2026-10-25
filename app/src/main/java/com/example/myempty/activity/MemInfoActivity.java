package com.example.myempty.activity2;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class MemInfoActivity extends AppCompatActivity {
    private TextView tvTotal, tvAvail;
    private ListView lv;
    private final Handler handler = new Handler();
    private final List<String> useful = new ArrayList<>();
    private final List<String> useless = new ArrayList<>();

    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            loadMem();
            handler.postDelayed(this, 10000);
        }
    };

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meminfo);

        tvTotal = findViewById(R.id.tv_mem_total);
        tvAvail = findViewById(R.id.tv_mem_avail);
        lv = findViewById(R.id.lv_procs);

        findViewById(R.id.btn_useless).setOnClickListener(v -> {
            UselessProcData.list = useless;
            startActivity(new Intent(this, UselessActivity.class));
        });

        loadMem();
        handler.postDelayed(refreshTask, 10000);
    }

    private void loadMem() {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile("/proc/meminfo", "r");
            String line, total = "", avail = "";
            while ((line = raf.readLine()) != null) {
                if (line.startsWith("MemTotal:")) total = line;
                if (line.startsWith("MemAvailable:")) avail = line;
                if (!total.isEmpty() && !avail.isEmpty()) break;
            }
            tvTotal.setText(total);
            tvAvail.setText(avail);
        } catch (Exception e) {
            tvTotal.setText("-");
            tvAvail.setText("-");
        } finally {
            if (raf != null) try { raf.close(); } catch (Exception ignored) {}
        }

        useful.clear();
        useless.clear();

        if (RootUtils.isRooted()) {
            try {
                Process p = Runtime.getRuntime().exec("ps -A -o PID,RSS,NAME");
                BufferedReader br = new BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
                String line;
                br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length < 3) continue;
                    long rss = 0;
                    try { rss = Long.parseLong(parts[1]); } catch (Exception ignored) {}
                    String name = parts[2];
                    String type = "?";
                    try {
                        ApplicationInfo ai = getPackageManager()
                            .getApplicationInfo(name, 0);
                        type = ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                            ? "SYS" : "USER";
                    } catch (Exception ignored) {}

                    String item = name + "  |  " + String.format("%.1f MB", rss / 1024.0)
                        + "  |  " + type;
                    if (type.equals("?")) useless.add(item);
                    else useful.add(item);
                }
                br.close();
            } catch (Exception e) {
                useful.add(e.getMessage());
            }
        } else {
            try {
                ApplicationInfo ai = getApplicationInfo();
                boolean isSys = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                android.os.Debug.MemoryInfo mi = new android.os.Debug.MemoryInfo();
                android.os.Debug.getMemoryInfo(mi);
                useful.add(ai.packageName + "  |  " + (mi.getTotalPss() / 1024) + " MB  |  "
                    + (isSys ? "SYS" : "USER"));
            } catch (Exception e) {
                useful.add(e.getMessage());
            }
        }

        lv.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_list_item_1, useful));
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(refreshTask);
        super.onDestroy();
    }
}