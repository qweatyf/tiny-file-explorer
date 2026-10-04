package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

public class BatteryMonitorActivity extends Activity {

    private TextView tvLevel;
    private TextView tvStatus;
    private TextView tvTemp;
    private TextView tvVolt;
    private TextView tvTech;
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
        setContentView(R.layout.activity_battery_monitor);

        tvLevel = findViewById(R.id.tv_bat_level);
        tvStatus = findViewById(R.id.tv_bat_status);
        tvTemp = findViewById(R.id.tv_bat_temp);
        tvVolt = findViewById(R.id.tv_bat_volt);
        tvTech = findViewById(R.id.tv_bat_tech);

        refresh();
        ui.postDelayed(tick, 1000);
    }

    private void refresh() {
        IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent i = registerReceiver(null, f);
        if (i == null) return;

        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        int volt = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        String tech = i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);

        float percent = level * 100f / scale;
        String statusStr;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) statusStr = "充电中";
        else if (status == BatteryManager.BATTERY_STATUS_FULL) statusStr = "已充满";
        else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) statusStr = "放电中";
        else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) statusStr = "未充电";
        else statusStr = "未知";

        tvLevel.setText(String.format("%.0f%%", percent));
        tvStatus.setText("状态: " + statusStr);
        tvTemp.setText("温度: " + (temp / 10f) + " °C");
        tvVolt.setText("电压: " + volt + " mV");
        tvTech.setText("类型: " + (tech == null ? "未知" : tech));
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(tick);
        super.onDestroy();
    }
}