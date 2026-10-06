package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends Activity {

    private EditText etUrl;
    private EditText etToken;
    private TextView tvStatus;
    private Button btnTest;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);

        etUrl = findViewById(R.id.et_settings_url);
        etToken = findViewById(R.id.et_settings_token);
        tvStatus = findViewById(R.id.tv_settings_status);
        btnTest = findViewById(R.id.btn_settings_test);

        etUrl.setText(ServerConfig.getUrl(this));
        etToken.setText(ServerConfig.getToken(this));

        findViewById(R.id.btn_settings_save).setOnClickListener(v -> save());
        findViewById(R.id.btn_settings_clear).setOnClickListener(v -> {
            etUrl.setText("");
            etToken.setText("");
            ServerConfig.setUrl(this, "");
            ServerConfig.setToken(this, "");
            Toast.makeText(this, "已清空，本地模式", Toast.LENGTH_SHORT).show();
        });

        btnTest.setOnClickListener(v -> test());
    }

    private void save() {
        String url = etUrl.getText().toString().trim();
        String token = etToken.getText().toString().trim();

        if (!url.isEmpty()
            && !url.startsWith("http://")
            && !url.startsWith("https://")) {
            url = "http://" + url;
            etUrl.setText(url);
        }

        ServerConfig.setUrl(this, url);
        ServerConfig.setToken(this, token);

        if (url.isEmpty()) {
            Toast.makeText(this, "已切到本地模式", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "已存: " + url, Toast.LENGTH_SHORT).show();
        }
    }

    private void test() {
        String url = etUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "先填地址", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
            etUrl.setText(url);
        }

        ServerConfig.setUrl(this, url);
        ServerConfig.setToken(this, etToken.getText().toString().trim());

        tvStatus.setText("测试中...");
        btnTest.setEnabled(false);

        Map<String, String> params = new HashMap<>();
        params.put("t", String.valueOf(System.currentTimeMillis()));

        ServerClient.get(this, "/ping", params, new ServerClient.Callback() {
            @Override
            public void onSuccess(String body) {
                runOnUiThread(() -> {
                    tvStatus.setText("通了。返回: "
                        + (body.length() > 100 ? body.substring(0, 100) + "..." : body));
                    btnTest.setEnabled(true);
                });
            }

            @Override
            public void onFail(String error) {
                runOnUiThread(() -> {
                    tvStatus.setText("连不上: " + error);
                    btnTest.setEnabled(true);
                });
            }
        });
    }
}