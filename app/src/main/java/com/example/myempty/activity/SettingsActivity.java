package com.example.myempty.activity2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText etUrl, etToken;
    private TextView tvStatus;
    private Button btnTest;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

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

        if (ServerConfig.isConnected(this)) {
            tvStatus.setText(getString(R.string.settings_connected)
                + " " + ServerConfig.buildBaseUrl(this));
        } else {
            tvStatus.setText(getString(R.string.settings_not_connected));
        }

        findViewById(R.id.btn_settings_save).setOnClickListener(v -> save());

        findViewById(R.id.btn_settings_clear).setOnClickListener(v -> {
            etUrl.setText("");
            etToken.setText("");
            ServerConfig.clear(this);
            tvStatus.setText(getString(R.string.settings_not_connected));
            Toast.makeText(this, getString(R.string.settings_cleared),
                Toast.LENGTH_SHORT).show();
        });

        btnTest.setOnClickListener(v -> test());
    }

    private void save() {
        String url = etUrl.getText().toString().trim();
        String token = etToken.getText().toString().trim();

        if (url.isEmpty()) {
            Toast.makeText(this, getString(R.string.settings_no_url),
                Toast.LENGTH_SHORT).show();
            return;
        }

        ServerConfig.setUrl(this, url);
        ServerConfig.setToken(this, token);
        ServerConfig.setConnected(this, false);
        ServerConfig.setScheme(this, "");

        etUrl.setText(ServerConfig.getUrl(this));

        Toast.makeText(this, getString(R.string.settings_saved),
            Toast.LENGTH_SHORT).show();

        tvStatus.setText(getString(R.string.settings_not_connected));
    }

    private void test() {
        final String url = etUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, getString(R.string.settings_no_url),
                Toast.LENGTH_SHORT).show();
            return;
        }

        final String token = etToken.getText().toString().trim();

        ServerConfig.setUrl(this, url);
        ServerConfig.setToken(this, token);

        String host = ServerConfig.getUrl(this);

        tvStatus.setText(getString(R.string.settings_testing));
        btnTest.setEnabled(false);

        ServerClient.probe(this, host, token, (ok, scheme, error) -> runOnUiThread(() -> {
            btnTest.setEnabled(true);
            if (ok) {
                ServerConfig.setScheme(this, scheme);
                ServerConfig.setConnected(this, true);
                ServerConfig.setLastCheck(this, System.currentTimeMillis());
                tvStatus.setText(getString(R.string.settings_connected)
                    + " " + scheme + "://" + host);
                Toast.makeText(this, getString(R.string.settings_connected),
                    Toast.LENGTH_LONG).show();
            } else {
                ServerConfig.setConnected(this, false);
                ServerConfig.setScheme(this, "");
                tvStatus.setText(getString(R.string.settings_fail) + "\n" + error);
                Toast.makeText(this, getString(R.string.settings_fail),
                    Toast.LENGTH_LONG).show();
            }
        }));
    }
}