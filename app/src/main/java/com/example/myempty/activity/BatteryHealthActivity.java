package com.example.myempty.activity2;

import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileReader;

public class BatteryHealthActivity extends AppCompatActivity {

    private static final String SP = "battery_health_sp";
    private static final String KEY_DESIGN = "design";
    private static final String KEY_CURRENT = "current";

    private EditText etDesign, etCurrent;
    private TextView tvResult, tvCurrentHint;
    private int autoCurrent = 0;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_battery_health);

        etDesign = findViewById(R.id.et_health_design);
        etCurrent = findViewById(R.id.et_health_current);
        tvResult = findViewById(R.id.tv_health_result);
        tvCurrentHint = findViewById(R.id.tv_health_current_hint);

        autoCurrent = readCurrentCapacity();
        if (autoCurrent > 0) {
            etCurrent.setText(String.valueOf(autoCurrent));
            etCurrent.setEnabled(false);
            tvCurrentHint.setText(getString(R.string.battery_health_auto_hint, autoCurrent));
        } else {
            tvCurrentHint.setText(getString(R.string.battery_health_current_hint));
        }

        SharedPreferences sp = getSharedPreferences(SP, MODE_PRIVATE);
        int design = sp.getInt(KEY_DESIGN, 0);
        int current = sp.getInt(KEY_CURRENT, 0);
        if (design > 0) etDesign.setText(String.valueOf(design));
        if (current > 0 && autoCurrent <= 0) etCurrent.setText(String.valueOf(current));

        updateResult();

        findViewById(R.id.btn_health_save).setOnClickListener(v -> {
            int d = parseInt(etDesign.getText().toString());
            int c = parseInt(etCurrent.getText().toString());
            if (d <= 0 || c <= 0) {
                Toast.makeText(this, getString(R.string.file_enc_none),
                    Toast.LENGTH_SHORT).show();
                return;
            }
            sp.edit()
                .putInt(KEY_DESIGN, d)
                .putInt(KEY_CURRENT, c)
                .apply();
            updateResult();
            Toast.makeText(this, getString(R.string.battery_health_saved),
                Toast.LENGTH_SHORT).show();
        });
    }

    private int readCurrentCapacity() {
        int root = readFromSysfs();
        if (root > 0) return root;
        return readFromBatteryManager();
    }

    private int readFromSysfs() {
        String[] paths = {
            "/sys/class/power_supply/battery/charge_full",
            "/sys/class/power_supply/battery/charge_full_design"
        };
        for (String p : paths) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(p));
                String s = br.readLine();
                br.close();
                if (s == null) continue;
                int uah = Integer.parseInt(s.trim());
                if (uah <= 0) continue;
                return uah / 1000;
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private int readFromBatteryManager() {
        try {
            BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
            int counter = bm.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
            int capacity = bm.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY);
            if (counter <= 0 || capacity <= 0) return 0;
            int full = (int) (counter * 100L / capacity);
            return full / 1000;
        } catch (Exception e) {
            return 0;
        }
    }

    private void updateResult() {
        int d = parseInt(etDesign.getText().toString());
        int c = parseInt(etCurrent.getText().toString());
        if (d <= 0 || c <= 0) {
            tvResult.setText("-");
            return;
        }
        int pct = (int) (c * 100L / d);
        if (pct > 100) pct = 100;
        if (pct < 0) pct = 0;
        tvResult.setText(getString(R.string.battery_health_value, pct));
    }

    private int parseInt(String s) {
        if (TextUtils.isEmpty(s)) return 0;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}