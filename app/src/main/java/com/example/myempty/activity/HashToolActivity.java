package com.example.myempty.activity2;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.security.MessageDigest;

public class HashToolActivity extends AppCompatActivity {

    private EditText etInput;
    private TextView tvOutput;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_hash_tool);

        etInput = findViewById(R.id.et_hash_input);
        tvOutput = findViewById(R.id.tv_hash_output);

        findViewById(R.id.btn_hash_calc).setOnClickListener(v -> calcAll());
        findViewById(R.id.btn_hash_clear).setOnClickListener(v -> {
            etInput.setText("");
            tvOutput.setText("");
        });
    }

    private void calcAll() {
        String text = etInput.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] data = text.getBytes("UTF-8");
            StringBuilder sb = new StringBuilder();
            sb.append("MD5:\n").append(hash(data, "MD5")).append("\n\n");
            sb.append("SHA-1:\n").append(hash(data, "SHA-1")).append("\n\n");
            sb.append("SHA-256:\n").append(hash(data, "SHA-256")).append("\n\n");
            sb.append("SHA-512:\n").append(hash(data, "SHA-512"));
            tvOutput.setText(sb.toString());
        } catch (Exception e) { tvOutput.setText(e.getMessage()); }
    }

    private String hash(byte[] data, String algo) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algo);
        byte[] h = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : h) sb.append(String.format("%02x", b & 0xFF));
        return sb.toString();
    }
}