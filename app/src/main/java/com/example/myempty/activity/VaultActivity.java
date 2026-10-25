package com.example.myempty.activity2;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class VaultActivity extends AppCompatActivity {

    private static final String FILE_NAME = "vault_items.dat";

    private EditText etPassword, etVault;
    private TextView tvList;
    private boolean unlocked = false;
    private String currentPass = "";
    private String pendingPass = "";
    private final List<String> items = new ArrayList<>();

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
        tvList = findViewById(R.id.tv_vault_list);

        findViewById(R.id.btn_unlock).setOnClickListener(v -> {
            String pw = etPassword.getText().toString();
            if (pw.isEmpty()) {
                Toast.makeText(this, getString(R.string.vault_pass_hint), Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                File f = new File(getExternalFilesDir(null), FILE_NAME);
                if (!f.exists()) {
                    if (pendingPass.isEmpty()) {
                        pendingPass = pw;
                        etPassword.setText("");
                        etPassword.setHint(getString(R.string.vault_confirm_hint));
                        Toast.makeText(this, getString(R.string.vault_confirm_toast), Toast.LENGTH_SHORT).show();
                    } else {
                        if (pendingPass.equals(pw)) {
                            f.createNewFile();
                            unlocked = true;
                            currentPass = pw;
                            pendingPass = "";
                            Toast.makeText(this, getString(R.string.vault_new), Toast.LENGTH_LONG).show();
                        } else {
                            pendingPass = "";
                            etPassword.setText("");
                            etPassword.setHint(getString(R.string.vault_pass_hint));
                            Toast.makeText(this, getString(R.string.vault_wrong_pass), Toast.LENGTH_SHORT).show();
                        }
                    }
                    return;
                }
                
                if (!unlocked) {
                    FileInputStream fis = new FileInputStream(f);
                    byte[] enc = new byte[(int) f.length()];
                    int read = 0;
                    while (read < enc.length) {
                        int n = fis.read(enc, read, enc.length - read);
                        if (n < 0) break;
                        read += n;
                    }
                    fis.close();
                    
                    if (enc.length == 0) {
                         unlocked = true;
                         currentPass = pw;
                         Toast.makeText(this, getString(R.string.vault_new), Toast.LENGTH_LONG).show();
                         return;
                    }

                    byte[] dec = CryptoUtils.aesDecrypt(enc, pw);
                    String all = new String(dec, "UTF-8");
                    items.clear();
                    if (!all.trim().isEmpty()) {
                        for (String line : all.split("\n---\n")) {
                            if (!line.trim().isEmpty()) items.add(line);
                        }
                    }
                    unlocked = true;
                    currentPass = pw;
                    Toast.makeText(this, getString(R.string.vault_unlocked), Toast.LENGTH_SHORT).show();
                    updateList();
                }
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.vault_wrong_pass), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_save_vault).setOnClickListener(v -> {
            if (!unlocked) {
                Toast.makeText(this, getString(R.string.vault_unlock_first), Toast.LENGTH_SHORT).show();
                return;
            }
            String newText = etVault.getText().toString().trim();
            if (!newText.isEmpty()) {
                items.add(0, newText);
                saveToDisk();
                etVault.setText("");
                updateList();
                Toast.makeText(this, getString(R.string.vault_saved), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveToDisk() {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                sb.append(items.get(i));
                if (i < items.size() - 1) sb.append("\n---\n");
            }
            byte[] enc = CryptoUtils.aesEncrypt(sb.toString().getBytes("UTF-8"), currentPass);
            File f = new File(getExternalFilesDir(null), FILE_NAME);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(enc);
            fos.close();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.vault_save_fail) + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateList() {
        if (items.isEmpty()) {
            tvList.setText(getString(R.string.vault_unlock_first));
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                sb.append("【").append(i + 1).append("】 ").append(items.get(i)).append("\n\n");
            }
            tvList.setText(sb.toString().trim());
        }
    }
}