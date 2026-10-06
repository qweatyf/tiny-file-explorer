package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SystemToolboxActivity extends Activity {
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
        android.widget.Button btn = findViewById(id);
        if (btn == null) return;
        if (!RootUtils.isRooted()) {
            btn.setAlpha(0.4f);
            btn.setOnClickListener(v ->
                new android.app.AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("需要 Root 权限，请授权 Root。")
                    .setPositiveButton("知道了", null)
                    .show());
        } else {
            btn.setOnClickListener(v -> startActivity(new Intent(this, target)));
        }
    }
}