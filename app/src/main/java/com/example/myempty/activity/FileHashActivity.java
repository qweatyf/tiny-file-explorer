package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;

public class FileHashActivity extends Activity {

    private Uri fileUri;
    private TextView tvFile;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_file_hash);

        tvFile = findViewById(R.id.tv_hash_file);

        findViewById(R.id.btn_hash_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_hash_do).setOnClickListener(v -> doHash());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            fileUri = data.getData();
            tvFile.setText("已选: " + fileUri);
        }
    }

    private void doHash() {
        if (fileUri == null) {
            Toast.makeText(this, "先选文件", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(fileUri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                is.close();
                byte[] raw = bos.toByteArray();

                String md5 = hash(raw, "MD5");
                String sha1 = hash(raw, "SHA-1");
                String sha256 = hash(raw, "SHA-256");

                StringBuilder sb = new StringBuilder();
                sb.append("大小: ").append(raw.length).append(" 字节\n\n");
                sb.append("MD5:\n").append(md5).append("\n\n");
                sb.append("SHA-1:\n").append(sha1).append("\n\n");
                sb.append("SHA-256:\n").append(sha256);

                runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("哈希值")
                    .setMessage(sb.toString())
                    .setPositiveButton("知道了", null)
                    .show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    "算不了: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String hash(byte[] data, String algo) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algo);
        byte[] h = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : h) sb.append(String.format("%02x", b & 0xFF));
        return sb.toString();
    }
}