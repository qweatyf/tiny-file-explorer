package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StorageAnalysisActivity extends Activity {

    private LinearLayout box;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_storage_analysis);

        box = findViewById(R.id.ll_storage_box);

        findViewById(R.id.btn_storage_scan).setOnClickListener(v -> scan());
    }

    private void scan() {
        if (running) return;
        running = true;
        box.removeAllViews();

        new Thread(() -> {
            List<Item> items = new ArrayList<>();
            File root = Environment.getExternalStorageDirectory();

            addItem(items, new File(root, "Download"), "Download");
            addItem(items, new File(root, "Pictures"), "Pictures");
            addItem(items, new File(root, "DCIM"), "DCIM");
            addItem(items, new File(root, "Movies"), "Movies");
            addItem(items, new File(root, "Music"), "Music");
            addItem(items, new File(root, "Documents"), "Documents");
            addItem(items, new File(root, "Android"), "Android");

            Collections.sort(items, new Comparator<Item>() {
                @Override
                public int compare(Item a, Item b) {
                    return Long.compare(b.size, a.size);
                }
            });

            long max = 0;
            for (Item it : items) if (it.size > max) max = it.size;

            final List<Item> fitems = items;
            final long fmax = max;

            ui.post(() -> {
                running = false;
                render(fitems, fmax);
            });
        }).start();
    }

    private void addItem(List<Item> list, File dir, String name) {
        if (!dir.exists()) return;
        long size = calc(dir, 0);
        list.add(new Item(name, size));
    }

    private long calc(File f, int depth) {
        if (depth > 8) return 0;
        if (f.isFile()) return f.length();
        long s = 0;
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) s += calc(k, depth + 1);
        }
        return s;
    }

    private void render(List<Item> items, long max) {
        if (items.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("没扫到东西");
            tv.setTextColor(0xFF333333);
            tv.setPadding(20, 20, 20, 20);
            box.addView(tv);
            return;
        }

        int barMaxW = (int) (getResources().getDisplayMetrics().density * 200);

        for (Item it : items) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 12, 0, 12);

            TextView name = new TextView(this);
            name.setText(it.name + "  " + fmt(it.size));
            name.setTextColor(0xFF333333);
            name.setTextSize(15);
            row.addView(name);

            android.widget.ProgressBar bar = new android.widget.ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(1000);
            bar.setProgress(max > 0 ? (int) (it.size * 1000 / max) : 0);
            row.addView(bar);

            box.addView(row);
        }

        TextView tip = new TextView(this);
        tip.setText("条形越长占用越大");
        tip.setTextColor(0xFF888888);
        tip.setTextSize(12);
        tip.setPadding(0, 20, 0, 0);
        box.addView(tip);
    }

    private String fmt(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) return String.format("%.1f MB", b / 1024.0 / 1024);
        return String.format("%.2f GB", b / 1024.0 / 1024 / 1024);
    }

    static class Item {
        String name;
        long size;
        Item(String n, long s) { name = n; size = s; }
    }
}