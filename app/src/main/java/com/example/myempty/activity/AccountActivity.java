package com.example.myempty.activity2;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;

public class AccountActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        TextView tvUser = findViewById(R.id.tv_username);
        TextView tvHash = findViewById(R.id.tv_hash);
        TextView tvTime = findViewById(R.id.tv_time);

        String username = "user";
        String cardHash = getString(R.string.acc_unknown);
        String time = getString(R.string.acc_unknown);

        try {
            File f = new File(getFilesDir(), "account.txt");
            FileInputStream fis = new FileInputStream(f);
            byte[] buf = new byte[fis.available()];
            fis.read(buf);
            fis.close();
            String content = new String(buf);
            for (String line : content.split("\n")) {
                if (line.startsWith("username=")) username = line.substring(9);
                if (line.startsWith("cardHash=")) cardHash = line.substring(9);
            }
            time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                .format(new java.util.Date(f.lastModified()));
        } catch (Exception ignored) {}

        tvUser.setText(getString(R.string.acc_user) + username);
        tvHash.setText(getString(R.string.acc_hash) + cardHash);
        tvTime.setText(getString(R.string.acc_time) + time);
    }
}