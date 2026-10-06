package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileCompressActivity extends Activity {

    private Uri fileUri;
    private TextView tvFile;
    private EditText etName;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_file_compress);

        tvFile = findViewById(R.id.tv_comp_file);
        etName = findViewById(R.id.et_comp_name);

        findViewById(R.id.btn_comp_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_comp_do).setOnClickListener(v -> doCompress());
        findViewById(R.id.btn_uncomp_do).setOnClickListener(v -> doUncompress());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            fileUri = data.getData();
            tvFile.setText("已选: " + fileUri);
        }
    }

    private void doCompress() {
        if (fileUri == null) {
            Toast.makeText(this, "先选文件", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) name = "archive_" + System.currentTimeMillis();

        final String fname = name;
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(fileUri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                is.close();
                byte[] raw = bos.toByteArray();

                File outDir = new File("/storage/emulated/0/Download/压缩文件");
                if (!outDir.exists()) outDir.mkdirs();
                File out = new File(outDir, fname + ".zip");

                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out));
                ZipEntry e = new ZipEntry("data.bin");
                zos.putNextEntry(e);
                zos.write(raw);
                zos.closeEntry();
                zos.close();

                runOnUiThread(() -> Toast.makeText(this,
                    "压缩好了: " + out.getAbsolutePath(),
                    Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    "压缩失败: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void doUncompress() {
        if (fileUri == null) {
            Toast.makeText(this, "先选 zip 文件", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(fileUri);
                File outDir = new File("/storage/emulated/0/Download/解压文件/" +
                    System.currentTimeMillis());
                if (!outDir.exists()) outDir.mkdirs();

                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry e;
                byte[] buf = new byte[8192];
                int count = 0;
                while ((e = zis.getNextEntry()) != null) {
                    if (e.getName().contains("..")) continue;
                    File out = new File(outDir, e.getName());
                    if (e.isDirectory()) {
                        out.mkdirs();
                    } else {
                        out.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream(out);
                        int n;
                        while ((n = zis.read(buf)) > 0) fos.write(buf, 0, n);
                        fos.close();
                        count++;
                    }
                }
                zis.close();

                final int c = count;
                runOnUiThread(() -> Toast.makeText(this,
                    "解压好了，共 " + c + " 个文件\n" + outDir.getAbsolutePath(),
                    Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    "解压失败: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}