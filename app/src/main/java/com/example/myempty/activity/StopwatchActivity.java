package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StopwatchActivity extends Activity {

    private TextView tvTime;
    private Button btnStart;
    private ListView lvLaps;
    private TextView tvLapEmpty;

    private long startTime = 0;
    private long accTime = 0;
    private boolean running = false;
    private long lastLapTime = 0;
    private int lapCount = 0;
    private final List<String> laps = new ArrayList<>();
    private LapAdapter adapter;

    private final Handler ui = new Handler(Looper.getMainLooper());

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (running) {
                tvTime.setText(format(accTime + (System.currentTimeMillis() - startTime)));
                ui.postDelayed(this, 30);
            }
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_stopwatch);

        tvTime = findViewById(R.id.tv_sw_time);
        btnStart = findViewById(R.id.btn_sw_start);
        lvLaps = findViewById(R.id.lv_sw_laps);
        tvLapEmpty = findViewById(R.id.tv_sw_lap_empty);

        adapter = new LapAdapter();
        lvLaps.setAdapter(adapter);

        btnStart.setOnClickListener(v -> toggle());
        findViewById(R.id.btn_sw_lap).setOnClickListener(v -> lap());
        findViewById(R.id.btn_sw_reset).setOnClickListener(v -> reset());

        tvTime.setText(format(0));
        refreshEmpty();
    }

    private void toggle() {
        if (running) {
            accTime += System.currentTimeMillis() - startTime;
            running = false;
            ui.removeCallbacks(tick);
            btnStart.setText("继续");
            tvTime.setText(format(accTime));
        } else {
            startTime = System.currentTimeMillis();
            running = true;
            btnStart.setText("暂停");
            ui.post(tick);
        }
    }

    private void lap() {
        if (!running && accTime == 0) return;
        long total = running
            ? accTime + (System.currentTimeMillis() - startTime)
            : accTime;
        long lapTime = total - lastLapTime;
        lastLapTime = total;
        lapCount++;

        String line = String.format(Locale.getDefault(),
            "第 %d 次  %s  (+%s)", lapCount, format(total), format(lapTime));
        laps.add(0, line);
        adapter.notifyDataSetChanged();
        refreshEmpty();
    }

    private void reset() {
        running = false;
        accTime = 0;
        startTime = 0;
        lastLapTime = 0;
        lapCount = 0;
        laps.clear();
        adapter.notifyDataSetChanged();
        ui.removeCallbacks(tick);
        btnStart.setText("开始");
        tvTime.setText(format(0));
        refreshEmpty();
    }

    private void refreshEmpty() {
        tvLapEmpty.setVisibility(laps.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String format(long ms) {
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        long cs = (ms % 1000) / 10;
        return String.format(Locale.getDefault(),
            "%02d:%02d:%02d.%02d", h, m, s, cs);
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(tick);
        super.onDestroy();
    }

    class LapAdapter extends BaseAdapter {
        @Override public int getCount() { return laps.size(); }
        @Override public String getItem(int i) { return laps.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(StopwatchActivity.this)
                .inflate(android.R.layout.simple_list_item_1, parent, false);
            TextView tv = v.findViewById(android.R.id.text1);
            tv.setText(getItem(pos));
            tv.setTextColor(0xFF333333);
            tv.setTextSize(14);
            tv.setPadding(24, 24, 24, 24);
            return v;
        }
    }
}