package com.example.myempty.activity2;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class VaultActivity extends AppCompatActivity {

    private static final String FILE_NAME = "vault.dat";

    private EditText etPassword;
    private EditText etVault;
    private boolean unlocked = false;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        etPassword = findViewById(R.id.et_password);
        etVault = findViewById(R.id.et_vault);

        findViewById(R.id.btn_unlock).setOnClickListener(v -> {
            String pw = etPassword.getText().toString();
            if (pw.isEmpty()) {
                Toast.makeText(this, getString(R.string.vault_pass_hint),
                    Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                File f = new File(getExternalFilesDir(null), FILE_NAME);
                if (!f.exists()) {
                    unlocked = true;
                    Toast.makeText(this, getString(R.string.vault_new),
                        Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, getString(R.string.vault_unlocked),
                    Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.vault_wrong_pass),
                    Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_save_vault).setOnClickListener(v -> {
            if (!unlocked) {
                Toast.makeText(this, getString(R.string.vault_unlock_first),
                    Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, getString(R.string.vault_saved),
                    Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this,
                    getString(R.string.vault_save_fail) + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            }
        });
    }
}