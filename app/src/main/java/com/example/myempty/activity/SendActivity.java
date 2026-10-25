package com.example.myempty.activity2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SendActivity extends AppCompatActivity {

    private Uri selectedFile;
    private ProgressBar pb;
    private TextView tvProgress;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        EditText etIp = findViewById(R.id.et_ip);
        pb = findViewById(R.id.pb_send);
        tvProgress = findViewById(R.id.tv_progress);

        if (ServerConfig.isRemote(this)) {
            etIp.setHint(ServerConfig.buildBaseUrl(this));
            etIp.setText(ServerConfig.getUrl(this));
            etIp.setEnabled(false);
        }

        findViewById(R.id.btn_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, 1);
        });

        findViewById(R.id.btn_send_now).setOnClickListener(v -> {
            if (selectedFile == null) {
                Toast.makeText(this, getString(R.string.file_enc_none),
                    Toast.LENGTH_SHORT).show();
                return;
            }
            if (ServerConfig.isRemote(this)) {
                new Thread(() -> doSendRemote(selectedFile)).start();
                return;
            }
            String ip = etIp.getText().toString().trim();
            if (ip.isEmpty()) {
                Toast.makeText(this, getString(R.string.send_no_ip),
                    Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> doSendLocal(ip, selectedFile)).start();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            selectedFile = data.getData();
            ((TextView) findViewById(R.id.tv_file))
                .setText(selectedFile.toString());
        }
    }

    private void doSendLocal(String ip, Uri uri) {
        try {
            byte[] data = readUri(uri);
            runOnUiThread(() -> showProgress());

            String key = getString(R.string.transfer_key);
            byte[] encrypted = CryptoUtils.aesEncrypt(data, key);
            String hash = CryptoUtils.sha256(data);

            runOnUiThread(() -> pb.setProgress(20));

            URL url = new URL("http://" + ip + ":8080/upload?hash=" + hash);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(encrypted.length);
            OutputStream os = conn.getOutputStream();

            int sent = 0;
            int totalEnc = encrypted.length;
            int chunk = 8192;
            while (sent < totalEnc) {
                int len = Math.min(chunk, totalEnc - sent);
                os.write(encrypted, sent, len);
                sent += len;
                final int p = 20 + (int) (sent * 70L / totalEnc);
                runOnUiThread(() -> {
                    pb.setProgress(p);
                    tvProgress.setText(getString(R.string.send_progress, p));
                });
            }
            os.close();

            int code = conn.getResponseCode();
            runOnUiThread(() -> {
                pb.setProgress(100);
                tvProgress.setText(getString(R.string.file_done));
                Toast.makeText(this, code == 200 ? getString(R.string.file_done)
                    : ("HTTP " + code), Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this,
                e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void doSendRemote(Uri uri) {
        try {
            byte[] data = readUri(uri);
            runOnUiThread(() -> showProgress());

            String key = getString(R.string.transfer_key);
            byte[] encrypted = CryptoUtils.aesEncrypt(data, key);
            String hash = CryptoUtils.sha256(data);

            runOnUiThread(() -> pb.setProgress(20));

            String base = ServerConfig.buildBaseUrl(this);
            URL url = new URL(base + "/upload?hash=" + hash);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(encrypted.length);
            String token = ServerConfig.getToken(this);
            if (!token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            OutputStream os = conn.getOutputStream();

            int sent = 0;
            int totalEnc = encrypted.length;
            int chunk = 8192;
            while (sent < totalEnc) {
                int len = Math.min(chunk, totalEnc - sent);
                os.write(encrypted, sent, len);
                sent += len;
                final int p = 20 + (int) (sent * 70L / totalEnc);
                runOnUiThread(() -> {
                    pb.setProgress(p);
                    tvProgress.setText(getString(R.string.send_progress, p));
                });
            }
            os.close();

            int code = conn.getResponseCode();
            runOnUiThread(() -> {
                pb.setProgress(100);
                tvProgress.setText(getString(R.string.file_done));
                Toast.makeText(this, code == 200 ? getString(R.string.file_done)
                    : ("HTTP " + code), Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this,
                e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void showProgress() {
        pb.setVisibility(View.VISIBLE);
        tvProgress.setVisibility(View.VISIBLE);
        pb.setProgress(0);
        tvProgress.setText(getString(R.string.settings_testing));
    }

    private byte[] readUri(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        is.close();
        return bos.toByteArray();
    }
}