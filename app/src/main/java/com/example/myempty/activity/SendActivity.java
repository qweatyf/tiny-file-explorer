package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SendActivity extends Activity {
    private static final boolean USE_SERVER = false;

    private Uri selectedFile;
    private ProgressBar pb;
    private TextView tvProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        EditText etIp = findViewById(R.id.et_ip);
        TextView tvFile = findViewById(R.id.tv_file);
        pb = findViewById(R.id.pb_send);
        tvProgress = findViewById(R.id.tv_progress);

        findViewById(R.id.btn_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, 1);
        });

        findViewById(R.id.btn_send_now).setOnClickListener(v -> {
            if (selectedFile == null) {
                Toast.makeText(this, "先选文件啊兄弟", Toast.LENGTH_SHORT).show();
                return;
            }
            if (USE_SERVER) {
                Toast.makeText(this, "走服务器（还没写）", Toast.LENGTH_SHORT).show();
                return;
            }
            String ip = etIp.getText().toString().trim();
            if (ip.isEmpty()) {
                Toast.makeText(this, "IP 填一下呗", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> doSend(ip, selectedFile)).start();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            selectedFile = data.getData();
            ((TextView) findViewById(R.id.tv_file))
                .setText("已选：" + selectedFile.toString());
        }
    }

    private void doSend(String ip, Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            long total = 0;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
                total += n;
            }
            is.close();
            byte[] data = bos.toByteArray();

            runOnUiThread(() -> {
                pb.setVisibility(View.VISIBLE);
                tvProgress.setVisibility(View.VISIBLE);
                pb.setProgress(0);
                tvProgress.setText("加密中...");
            });

            String key = getString(R.string.transfer_key);
            byte[] encrypted = CryptoUtils.aesEncrypt(data, key);
            String hash = CryptoUtils.sha256(data);

            runOnUiThread(() -> {
                pb.setProgress(20);
                tvProgress.setText("发送中...");
            });

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
                    tvProgress.setText("发送中 " + p + "%");
                });
            }
            os.close();

            int code = conn.getResponseCode();
            runOnUiThread(() -> {
                pb.setProgress(100);
                tvProgress.setText("完成");
                Toast.makeText(this,
                    code == 200 ? "发过去了" : "发送失败，错误码 " + code,
                    Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this,
                "出错了：" + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }
}