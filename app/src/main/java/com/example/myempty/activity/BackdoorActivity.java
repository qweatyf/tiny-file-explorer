package com.example.myempty.activity2;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;

public class BackdoorActivity extends AppCompatActivity {
    private static final String[] SUSPICIOUS = {
        "DexClassLoader", "PathClassLoader", "Runtime.exec",
        "ProcessBuilder", "/system/bin/su", "/system/xbin/su",
        "nc -l", "busybox nc", "chmod 777 /data"
    };

    private int count = 0;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backdoor);

        TextView tv = findViewById(R.id.tv_result);

        findViewById(R.id.btn_scan).setOnClickListener(v -> {
            tv.setText(getString(R.string.backdoor_scanning));
            new Thread(() -> {
                StringBuilder sb = new StringBuilder();
                count = 0;
                scanDir(new File("/sdcard/"), sb);
                if (count == 0) {
                    sb.append(getString(R.string.backdoor_ok)).append("\n");
                } else {
                    sb.insert(0, getString(R.string.backdoor_warn, count) + "\n\n");
                }
                sb.append("\n").append(getString(R.string.backdoor_done));
                String r = sb.toString();
                runOnUiThread(() -> tv.setText(r));
            }).start();
        });
    }

    private void scanDir(File dir, StringBuilder sb) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) scanDir(f, sb);
            else if (f.length() < 2 * 1024 * 1024) {
                try {
                    FileInputStream fis = new FileInputStream(f);
                    byte[] buf = new byte[(int) f.length()];
                    int read = 0;
                    while (read < buf.length) {
                        int n = fis.read(buf, read, buf.length - read);
                        if (n < 0) break;
                        read += n;
                    }
                    fis.close();
                    String content = new String(buf, 0, read);
                    for (String s : SUSPICIOUS) {
                        if (content.contains(s)) {
                            sb.append(getString(R.string.backdoor_suspect,
                                f.getAbsolutePath(), s)).append("\n");
                            count++;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}