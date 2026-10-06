package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class VaultActivity extends Activity {
    private static final boolean USE_SERVER = false;
    private static final String FILE_NAME = "vault.dat";

    private EditText etPassword;
    private EditText etVault;
    private boolean unlocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        etPassword = findViewById(R.id.et_password);
        etVault = findViewById(R.id.et_vault);

        findViewById(R.id.btn_unlock).setOnClickListener(v -> {
            String pw = etPassword.getText().toString();
            if (pw.isEmpty()) {
                Toast.makeText(this, "密码填一下呗", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                File f = new File(getExternalFilesDir(null), FILE_NAME);
                if (!f.exists()) {
                    unlocked = true;
                    Toast.makeText(this, "新保险箱，密码记住了，写点东西点保存", Toast.LENGTH_LONG).show();
                    return;
                }
                FileInputStream fis = new FileInputStream(f);
                byte[] enc = new byte[(int) f.length()];
                int read = 0;
                while (read < enc.length) {
                    int n = fis.read(enc, read, enc.length - read);
                    if (n < 0) break;
                    read += n;
                }
                fis.close();

                byte[] dec = CryptoUtils.aesDecrypt(enc, pw);
                etVault.setText(new String(dec, "UTF-8"));
                unlocked = true;
                Toast.makeText(this, "解锁成功", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "密码不对吧", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_save_vault).setOnClickListener(v -> {
            if (!unlocked) {
                Toast.makeText(this, "先解锁啊兄弟", Toast.LENGTH_SHORT).show();
                return;
            }
            String pw = etPassword.getText().toString();
            try {
                byte[] enc = CryptoUtils.aesEncrypt(
                    etVault.getText().toString().getBytes("UTF-8"), pw);
                File f = new File(getExternalFilesDir(null), FILE_NAME);
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(enc);
                fos.close();
                Toast.makeText(this, "加密保存好了", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}