package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

public class SysScanActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sysscan);

        TextView tvMan = findViewById(R.id.tv_manufacturer);
        TextView tvModel = findViewById(R.id.tv_model);
        TextView tvVer = findViewById(R.id.tv_version);
        TextView tvBl = findViewById(R.id.tv_bl);
        TextView tvRoot = findViewById(R.id.tv_root);

        tvMan.setText("厂商：" + Build.MANUFACTURER);
        tvModel.setText("型号：" + Build.MODEL);
        tvVer.setText("版本：" + Build.VERSION.RELEASE);
        tvBl.setText("BL 锁：拿不到");
        tvRoot.setText("Root：" + (RootUtils.isRooted() ? "有" : "没有"));

        findViewById(R.id.btn_mem).setOnClickListener(v ->
            startActivity(new Intent(this, MemInfoActivity.class)));
    }
}