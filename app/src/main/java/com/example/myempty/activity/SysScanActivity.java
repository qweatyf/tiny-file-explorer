package com.example.myempty.activity2;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SysScanActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sysscan);

        TextView tvMan = findViewById(R.id.tv_manufacturer);
        TextView tvModel = findViewById(R.id.tv_model);
        TextView tvVer = findViewById(R.id.tv_version);
        TextView tvBl = findViewById(R.id.tv_bl);
        TextView tvRoot = findViewById(R.id.tv_root);

        tvMan.setText(getString(R.string.sysscan_manufacturer)
            + getString(R.string.sysscan_colon) + Build.MANUFACTURER);
        tvModel.setText(getString(R.string.sysscan_model)
            + getString(R.string.sysscan_colon) + Build.MODEL);
        tvVer.setText(getString(R.string.sysscan_version)
            + getString(R.string.sysscan_colon) + Build.VERSION.RELEASE);
        tvBl.setText(getString(R.string.sysscan_bootloader)
            + getString(R.string.sysscan_colon) + getString(R.string.unspecified));
        tvRoot.setText(getString(R.string.sysscan_root)
            + getString(R.string.sysscan_colon)
            + (RootUtils.isRooted() ? "Y" : "N"));

        findViewById(R.id.btn_mem).setOnClickListener(v ->
            startActivity(new Intent(this, MemInfoActivity.class)));
    }
}