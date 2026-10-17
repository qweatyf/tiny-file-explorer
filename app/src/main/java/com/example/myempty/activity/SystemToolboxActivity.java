package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SystemToolboxActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_system_toolbox);

        findViewById(R.id.btn_sys_cpu).setOnClickListener(v ->
            startActivity(new Intent(this, CpuMonitorActivity.class)));

        findViewById(R.id.btn_sys_mem).setOnClickListener(v ->
            startActivity(new Intent(this, MemMonitorActivity.class)));

        findViewById(R.id.btn_sys_battery).setOnClickListener(v ->
            startActivity(new Intent(this, BatteryMonitorActivity.class)));

        findViewById(R.id.btn_sys_net).setOnClickListener(v ->
            startActivity(new Intent(this, NetMonitorActivity.class)));

        findViewById(R.id.btn_sys_storage).setOnClickListener(v ->
            startActivity(new Intent(this, StorageAnalysisActivity.class)));

        findViewById(R.id.btn_sys_appdetail).setOnClickListener(v ->
            startActivity(new Intent(this, AppDetailActivity.class)));

        findViewById(R.id.btn_sys_uninstall).setOnClickListener(v ->
            startActivity(new Intent(this, AppUninstallActivity.class)));

        bindRoot(R.id.btn_sys_freeze, AppFreezeActivity.class);
        bindRoot(R.id.btn_sys_clean, AppCleanActivity.class);
        bindRoot(R.id.btn_sys_autostart, AppAutoStartActivity.class);
    }

    private void bindRoot(int id, Class<?> target) {
        Button btn = findViewById(id);
        if (btn == null) return;
        if (!RootUtils.isRooted()) {
            btn.setAlpha(0.4f);
            btn.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.root_tip_title))
                    .setMessage(getString(R.string.root_tip_msg))
                    .setPositiveButton(getString(R.string.lang_ok), null)
                    .show());
        } else {
            btn.setOnClickListener(v -> startActivity(new Intent(this, target)));
        }
    }
}