package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;

public class AccountActivity extends Activity {
    
    private static final boolean USE_SERVER = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        TextView tvUser = findViewById(R.id.tv_username);
        TextView tvHash = findViewById(R.id.tv_hash);
        TextView tvTime = findViewById(R.id.tv_time);

        String username = "user";
        String cardHash = "未知";
        String time = "未知";

        if (!USE_SERVER) {
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
        } else {
        }

        tvUser.setText("用户名：" + username);
        tvHash.setText("卡密哈希：" + cardHash);
        tvTime.setText("创建时间：" + time);
    }
}