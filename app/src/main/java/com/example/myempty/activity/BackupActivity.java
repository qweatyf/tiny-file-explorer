package com.example.myempty.activity2;

import android.os.Bundle;
import android.os.StatFs;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class BackupActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        TextView tv = findViewById(R.id.tv_backup_result);

        findViewById(R.id.btn_backup).setOnClickListener(v -> {
            new Thread(() -> {
                StringBuilder sb = new StringBuilder();
                File srcDir = new File("/dev/block/by-name/");
                File outDir = new File("/sdcard/" + getString(R.string.title_backup) + "/");
                if (!outDir.exists()) outDir.mkdirs();

                File[] parts = srcDir.listFiles();
                if (parts == null) {
                    runOnUiThread(() -> tv.setText(getString(R.string.backup_no_root)));
                    return;
                }

                for (File p : parts) {
                    if ("userdata".equals(p.getName())) {
                        sb.append(getString(R.string.backup_skip_userdata)).append("\n");
                        continue;
                    }
                    StatFs stat = new StatFs(outDir.getAbsolutePath());
                    long free = stat.getAvailableBytes();
                    if (p.length() > free) {
                        sb.append(getString(R.string.backup_skip_nospace, p.getName()))
                            .append("\n");
                        continue;
                    }
                    try {
                        FileInputStream fis = new FileInputStream(p);
                        FileOutputStream fos = new FileOutputStream(
                            new File(outDir, p.getName() + ".img"));
                        byte[] buf = new byte[1024 * 1024];
                        int n;
                        while ((n = fis.read(buf)) > 0) fos.write(buf, 0, n);
                        fis.close();
                        fos.close();
                        sb.append(getString(R.string.backup_ok, p.getName())).append("\n");
                    } catch (Exception e) {
                        sb.append(getString(R.string.backup_fail, p.getName(),
                            e.getMessage())).append("\n");
                    }
                }
                String result = sb.toString();
                runOnUiThread(() -> tv.setText(result));
            }).start();
        });
    }
}