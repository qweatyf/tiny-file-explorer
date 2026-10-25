package com.example.myempty.activity2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileEncryptActivity extends AppCompatActivity {

    private Uri fileUri;
    private TextView tvFile;
    private EditText etPass;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_file_encrypt);

        tvFile = findViewById(R.id.tv_enc_file);
        etPass = findViewById(R.id.et_enc_pass);

        findViewById(R.id.btn_enc_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_enc_do).setOnClickListener(v -> doEncrypt());
        findViewById(R.id.btn_dec_do).setOnClickListener(v -> doDecrypt());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            fileUri = data.getData();
            tvFile.setText(getString(R.string.file_enc_pick) + ": " + fileUri);
        }
    }

    private void doEncrypt() {
        if (fileUri == null) {
            Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show();
            return;
        }
        String pw = etPass.getText().toString();
        if (pw.isEmpty()) {
            Toast.makeText(this, getString(R.string.file_enc_pass_hint), Toast.LENGTH_SHORT).show();
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

                byte[] enc = CryptoUtils.aesEncrypt(raw, pw);

                File outDir = new File("/storage/emulated/0/Download/加密文件");
                if (!outDir.exists()) outDir.mkdirs();
                File out = new File(outDir, "enc_" + System.currentTimeMillis() + ".dat");
                FileOutputStream fos = new FileOutputStream(out);
                fos.write(enc);
                fos.close();

                runOnUiThread(() -> Toast.makeText(this,
                    out.getAbsolutePath(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    getString(R.string.file_fail) + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void doDecrypt() {
        if (fileUri == null) {
            Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show();
            return;
        }
        String pw = etPass.getText().toString();
        if (pw.isEmpty()) {
            Toast.makeText(this, getString(R.string.file_enc_pass_hint), Toast.LENGTH_SHORT).show();
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

                byte[] dec = CryptoUtils.aesDecrypt(raw, pw);

                File outDir = new File("/storage/emulated/0/Download/解密文件");
                if (!outDir.exists()) outDir.mkdirs();
                File out = new File(outDir, "dec_" + System.currentTimeMillis() + ".bin");
                FileOutputStream fos = new FileOutputStream(out);
                fos.write(dec);
                fos.close();

                runOnUiThread(() -> Toast.makeText(this,
                    out.getAbsolutePath(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    getString(R.string.file_fail) + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}