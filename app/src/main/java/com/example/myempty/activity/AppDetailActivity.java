package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppDetailActivity extends Activity {

    private EditText etPkg;
    private TextView tvResult;
    private PackageManager pm;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_app_detail);

        pm = getPackageManager();
        etPkg = findViewById(R.id.et_app_pkg);
        tvResult = findViewById(R.id.tv_app_detail);

        findViewById(R.id.btn_app_detail_do).setOnClickListener(v -> show());
        findViewById(R.id.btn_app_detail_pick).setOnClickListener(v -> showPicker());
    }

    private void showPicker() {
        final List<ApplicationInfo> list = pm.getInstalledApplications(0);
        Collections.sort(list, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo a, ApplicationInfo b) {
                return pm.getApplicationLabel(a).toString()
                    .compareToIgnoreCase(pm.getApplicationLabel(b).toString());
            }
        });

        ListView lv = new ListView(this);
        lv.setAdapter(new PickerAdapter(list));
        lv.setBackgroundColor(0xFFFFFFFF);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("选应用")
            .setView(lv)
            .setNegativeButton("取消", null)
            .create();

        lv.setOnItemClickListener((p, v, pos, id) -> {
            ApplicationInfo ai = list.get(pos);
            etPkg.setText(ai.packageName);
            dialog.dismiss();
            show();
        });

        dialog.show();
    }

    private void show() {
        String pkg = etPkg.getText().toString().trim();
        if (pkg.isEmpty()) {
            Toast.makeText(this, "输包名", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            PackageInfo pi = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS);
            ApplicationInfo ai = pi.applicationInfo;

            StringBuilder sb = new StringBuilder();
            sb.append("名称: ").append(pm.getApplicationLabel(ai)).append("\n\n");
            sb.append("包名: ").append(pi.packageName).append("\n\n");
            sb.append("版本: ").append(pi.versionName)
              .append(" (").append(pi.versionCode).append(")\n\n");
            sb.append("大小: ").append(fmt(new File(ai.sourceDir).length())).append("\n\n");
            sb.append("路径: ").append(ai.sourceDir).append("\n\n");

            String sign = "无签名信息";
            try {
                android.content.pm.Signature[] sigs = pm.getPackageInfo(
                    pkg, PackageManager.GET_SIGNATURES).signatures;
                if (sigs != null && sigs.length > 0) {
                    java.security.MessageDigest md =
                        java.security.MessageDigest.getInstance("SHA-256");
                    byte[] h = md.digest(sigs[0].toByteArray());
                    StringBuilder sh = new StringBuilder();
                    for (byte x : h) sh.append(String.format("%02x", x & 0xFF));
                    sign = sh.toString();
                }
            } catch (Exception ignored) {}
            sb.append("签名 SHA-256:\n").append(sign).append("\n\n");

            String[] perms = pi.requestedPermissions;
            if (perms != null && perms.length > 0) {
                sb.append("权限 (").append(perms.length).append("):\n");
                for (String p : perms) sb.append("  ").append(p).append("\n");
            } else {
                sb.append("无声明权限");
            }

            tvResult.setText(sb.toString());
        } catch (Exception e) {
            tvResult.setText("读不到: " + e.getMessage());
        }
    }

    private String fmt(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) {
            return String.format("%.1f MB", b / 1024.0 / 1024);
        }
        return String.format("%.2f GB", b / 1024.0 / 1024 / 1024);
    }

    class PickerAdapter extends BaseAdapter {
        private final List<ApplicationInfo> data;

        PickerAdapter(List<ApplicationInfo> d) {
            data = d;
        }

        @Override public int getCount() { return data.size(); }
        @Override public ApplicationInfo getItem(int i) { return data.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = getLayoutInflater().inflate(
                android.R.layout.simple_list_item_1, parent, false);
            TextView tv = v.findViewById(android.R.id.text1);
            ApplicationInfo ai = getItem(pos);
            tv.setText(pm.getApplicationLabel(ai).toString()
                + "\n" + ai.packageName);
            tv.setTextColor(0xFF333333);
            tv.setTextSize(14);
            tv.setPadding(30, 20, 30, 20);
            return v;
        }
    }
}