package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileSplitActivity extends Activity {

    private Uri fileUri;
    private TextView tvFile;
    private EditText etSize;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_file_split);

        tvFile = findViewById(R.id.tv_split_file);
        etSize = findViewById(R.id.et_split_size);

        findViewById(R.id.btn_split_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_split_do).setOnClickListener(v -> doSplit());
        findViewById(R.id.btn_split_merge).setOnClickListener(v -> doMerge());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            fileUri = data.getData();
            tvFile.setText("已选: " + fileUri);
        }
    }

    private void doSplit() {
        if (fileUri == null) {
            Toast.makeText(this, "先选文件", Toast.LENGTH_SHORT).show();
            return;
        }
        int kb = 1024;
        try { kb = Integer.parseInt(etSize.getText().toString().trim()) * 1024; }
        catch (Exception ignored) {}
        if (kb <= 0) kb = 1024 * 1024;

        final int chunk = kb;
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(fileUri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                is.close();
                byte[] raw = bos.toByteArray();

                File outDir = new File("/storage/emulated/0/Download/切分文件/" +
                    System.currentTimeMillis());
                if (!outDir.exists()) outDir.mkdirs();

                int parts = (raw.length + chunk - 1) / chunk;
                for (int i = 0; i < parts; i++) {
                    int start = i * chunk;
                    int end = Math.min(start + chunk, raw.length);
                    File out = new File(outDir, "part_" + (i + 1) + ".bin");
                    FileOutputStream fos = new FileOutputStream(out);
                    fos.write(raw, start, end - start);
                    fos.close();
                }

                runOnUiThread(() -> Toast.makeText(this,
                    "切好了 " + parts + " 块\n" + outDir.getAbsolutePath(),
                    Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    "切不了: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void doMerge() {
        final EditText et = new EditText(this);
        et.setHint("/storage/emulated/0/Download/切分文件/xxx");

        new AlertDialog.Builder(this)
            .setTitle("合并目录")
            .setMessage("输切分文件所在目录")
            .setView(et)
            .setPositiveButton("合并", (d, w) -> {
                String path = et.getText().toString().trim();
                File dir = new File(path);
                if (!dir.exists() || !dir.isDirectory()) {
                    Toast.makeText(this, "目录不存在", Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(() -> {
                    try {
                        File[] parts = dir.listFiles((d2, n2) ->
                            n2.startsWith("part_") && n2.endsWith(".bin"));
                        if (parts == null || parts.length == 0) {
                            runOnUiThread(() -> Toast.makeText(this,
                                "没有 part_xxx.bin", Toast.LENGTH_SHORT).show());
                            return;
                        }
                        java.util.Arrays.sort(parts, (a, b) -> {
                            int na = Integer.parseInt(
                                a.getName().replaceAll("\\D", ""));
                            int nb = Integer.parseInt(
                                b.getName().replaceAll("\\D", ""));
                            return na - nb;
                        });

                        File out = new File(dir, "merged_" + System.currentTimeMillis() + ".bin");
                        FileOutputStream fos = new FileOutputStream(out);
                        for (File p : parts) {
                            FileInputStream fis = new FileInputStream(p);
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = fis.read(buf)) > 0) fos.write(buf, 0, n);
                            fis.close();
                        }
                        fos.close();

                        runOnUiThread(() -> Toast.makeText(this,
                            "合并好了: " + out.getAbsolutePath(),
                            Toast.LENGTH_LONG).show());
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(this,
                            "合并失败: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());
                    }
                }).start();
            })
            .setNegativeButton("算了", null)
            .show();
    }
}