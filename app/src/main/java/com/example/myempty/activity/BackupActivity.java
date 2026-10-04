package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.os.StatFs;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class BackupActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        TextView tv = findViewById(R.id.tv_backup_result);

        findViewById(R.id.btn_backup).setOnClickListener(v -> {
            new Thread(() -> {
                StringBuilder sb = new StringBuilder();
                File srcDir = new File("/dev/block/by-name/");
                File outDir = new File("/sdcard/分区备份/");
                if (!outDir.exists()) outDir.mkdirs();

                File[] parts = srcDir.listFiles();
                if (parts == null) {
                    runOnUiThread(() -> tv.setText("读不到分区目录，估计没 Root。"));
                    return;
                }

                for (File p : parts) {
                    if ("userdata".equals(p.getName())) {
                        sb.append("跳过 userdata（太大）\n");
                        continue;
                    }

                    StatFs stat = new StatFs(outDir.getAbsolutePath());
                    long free = stat.getAvailableBytes();
                    if (p.length() > free) {
                        sb.append("跳过了：").append(p.getName())
                          .append("（空间不够，需要 ")
                          .append(p.length() / 1024 / 1024)
                          .append(" MB）\n");
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
                        sb.append("备份好了：").append(p.getName()).append("\n");
                    } catch (Exception e) {
                        sb.append("失败了：").append(p.getName())
                          .append(" - ").append(e.getMessage()).append("\n");
                    }
                }
                String result = sb.toString();
                runOnUiThread(() -> tv.setText(result));
            }).start();
        });
    }
}