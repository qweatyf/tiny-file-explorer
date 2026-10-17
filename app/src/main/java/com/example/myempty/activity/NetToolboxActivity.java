package com.example.myempty.activity2;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class NetToolboxActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_net_toolbox);

        findViewById(R.id.btn_net_http).setOnClickListener(v ->
            startActivity(new Intent(this, HttpRequestActivity.class)));

        findViewById(R.id.btn_net_lan).setOnClickListener(v ->
            startActivity(new Intent(this, LanScanActivity.class)));

        findViewById(R.id.btn_net_port).setOnClickListener(v ->
            startActivity(new Intent(this, PortScanActivity.class)));

        findViewById(R.id.btn_net_speed).setOnClickListener(v ->
            startActivity(new Intent(this, SpeedTestActivity.class)));
    }
}