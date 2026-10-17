package com.example.myempty.activity2;

import android.os.Bundle;
import android.util.Base64;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AesToolActivity extends AppCompatActivity {

    private EditText etInput, etPass;
    private TextView tvOutput;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_aes_tool);

        etInput = findViewById(R.id.et_aes_input);
        etPass = findViewById(R.id.et_aes_pass);
        tvOutput = findViewById(R.id.tv_aes_output);

        findViewById(R.id.btn_aes_encrypt).setOnClickListener(v -> encrypt());
        findViewById(R.id.btn_aes_decrypt).setOnClickListener(v -> decrypt());
        findViewById(R.id.btn_aes_clear).setOnClickListener(v -> {
            etInput.setText("");
            etPass.setText("");
            tvOutput.setText("");
        });
    }

    private void encrypt() {
        String text = etInput.getText().toString();
        String pass = etPass.getText().toString();
        if (text.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] enc = CryptoUtils.aesEncrypt(text.getBytes("UTF-8"), pass);
            tvOutput.setText(Base64.encodeToString(enc, Base64.NO_WRAP));
        } catch (Exception e) { tvOutput.setText(e.getMessage()); }
    }

    private void decrypt() {
        String text = etInput.getText().toString().trim();
        String pass = etPass.getText().toString();
        if (text.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] enc = Base64.decode(text, Base64.DEFAULT);
            byte[] dec = CryptoUtils.aesDecrypt(enc, pass);
            tvOutput.setText(new String(dec, "UTF-8"));
        } catch (Exception e) { tvOutput.setText(getString(R.string.file_fail)); }
    }
}