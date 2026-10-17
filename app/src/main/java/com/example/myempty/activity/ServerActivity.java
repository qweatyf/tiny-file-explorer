package com.example.myempty.activity2;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ServerActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        TextView tvIp = findViewById(R.id.tv_ip);

        try {
            WifiManager wm = (WifiManager) getApplicationContext()
                .getSystemService(WIFI_SERVICE);
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            tvIp.setText(ip);
        } catch (Exception e) {
            tvIp.setText(getString(R.string.unspecified));
        }

        findViewById(R.id.btn_start).setOnClickListener(v -> {
            startForegroundService(new Intent(this, MyServerService.class));
            Toast.makeText(this, getString(R.string.server_started),
                Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_stop).setOnClickListener(v -> {
            stopService(new Intent(this, MyServerService.class));
            Toast.makeText(this, getString(R.string.server_stopped),
                Toast.LENGTH_SHORT).show();
        });
    }
}