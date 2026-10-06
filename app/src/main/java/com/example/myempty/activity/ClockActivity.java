package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockActivity extends Activity {

    private TextView tvTime;
    private TextView tvDate;
    private TextView tvWeek;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat weekFmt = new SimpleDateFormat("EEEE", Locale.getDefault());

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refresh();
            long delay = 1000 - (System.currentTimeMillis() % 1000);
            ui.postDelayed(this, delay);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_clock);

        tvTime = findViewById(R.id.tv_clock_time);
        tvDate = findViewById(R.id.tv_clock_date);
        tvWeek = findViewById(R.id.tv_clock_week);

        refresh();
        ui.post(tick);
    }

    private void refresh() {
        Date now = new Date();
        tvTime.setText(timeFmt.format(now));
        tvDate.setText(dateFmt.format(now));
        tvWeek.setText(weekFmt.format(now));
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(tick);
        super.onDestroy();
    }
}