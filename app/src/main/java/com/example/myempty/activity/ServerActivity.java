package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.TextView;
import android.widget.Toast;

public class ServerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        TextView tvIp = findViewById(R.id.tv_ip);

        try {
            WifiManager wm = (WifiManager) getApplicationContext()
                .getSystemService(WIFI_SERVICE);
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            tvIp.setText("你的 IP：" + ip);
        } catch (Exception e) {
            tvIp.setText("你的 IP：拿不到");
        }

        findViewById(R.id.btn_start).setOnClickListener(v -> {
            startForegroundService(new Intent(this, MyServerService.class));
            Toast.makeText(this, "服务器开起来了", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_stop).setOnClickListener(v -> {
            stopService(new Intent(this, MyServerService.class));
            Toast.makeText(this, "服务器关了", Toast.LENGTH_SHORT).show();
        });
    }
}