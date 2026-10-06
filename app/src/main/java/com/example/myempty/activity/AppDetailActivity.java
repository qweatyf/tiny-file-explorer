package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class AppDetailActivity extends Activity {

    private EditText etPkg;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_app_detail);

        etPkg = findViewById(R.id.et_app_pkg);
        tvResult = findViewById(R.id.tv_app_detail);

        findViewById(R.id.btn_app_detail_do).setOnClickListener(v -> show());
        findViewById(R.id.btn_app_detail_pick).setOnClickListener(v ->
            startActivityForResult(new Intent(Intent.ACTION_PICK)
                .setData(Uri.parse("package:")), 100));
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            String pkg = data.getDataString().replace("package:", "");
            etPkg.setText(pkg);
            show();
        }
    }

    private void show() {
        String pkg = etPkg.getText().toString().trim();
        if (pkg.isEmpty()) {
            Toast.makeText(this, "输包名", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS);
            ApplicationInfo ai = pi.applicationInfo;

            StringBuilder sb = new StringBuilder();
            sb.append("名称: ").append(pm.getApplicationLabel(ai)).append("\n\n");
            sb.append("包名: ").append(pi.packageName).append("\n\n");
            sb.append("版本: ").append(pi.versionName).append(" (").append(pi.versionCode).append(")\n\n");
            sb.append("大小: ").append(fmt(new File(ai.sourceDir).length())).append("\n\n");
            sb.append("路径: ").append(ai.sourceDir).append("\n\n");

            String sign = "无签名信息";
            try {
                android.content.pm.Signature[] sigs = pm.getPackageInfo(
                    pkg, PackageManager.GET_SIGNATURES).signatures;
                if (sigs != null && sigs.length > 0) {
                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
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
                for (String p : perms) {
                    sb.append("  ").append(p).append("\n");
                }
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
        if (b < 1024L * 1024 * 1024) return String.format("%.1f MB", b / 1024.0 / 1024);
        return String.format("%.2f GB", b / 1024.0 / 1024 / 1024);
    }
}