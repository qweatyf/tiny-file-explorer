package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ApksActivity extends Activity {
    private final List<Uri> selected = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apks);

        findViewById(R.id.btn_pick_apks).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_merge).setOnClickListener(v -> {
            if (selected.isEmpty()) {
                Toast.makeText(this, "先选文件啊兄弟", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> {
                try {
                    File outDir = new File(getExternalFilesDir(null), "merged_temp");
                    if (outDir.exists()) deleteDir(outDir);
                    outDir.mkdirs();

                    for (Uri uri : selected) unzip(uri, outDir);

                    File outApk = new File(getExternalFilesDir(null),
                        "合成结果_" + System.currentTimeMillis() + ".apk");
                    zipDir(outDir, outApk);

                    runOnUiThread(() -> showSignedDialog(outApk.getAbsolutePath()));
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this,
                        "合成挂了：" + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        });
    }

    private void showSignedDialog(String path) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_signed, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create();

        Button btn = view.findViewById(R.id.btn_ok);
        btn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            selected.clear();
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    selected.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                selected.add(data.getData());
            }
            Toast.makeText(this, "选了 " + selected.size() + " 个文件", Toast.LENGTH_SHORT).show();
        }
    }

    private void unzip(Uri uri, File outDir) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry;
        byte[] buf = new byte[4096];
        while ((entry = zis.getNextEntry()) != null) {
            String name = entry.getName();
            if (name.contains("..")) continue;
            File out = new File(outDir, name);
            if (entry.isDirectory()) {
                out.mkdirs();
            } else {
                out.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(out);
                int n;
                while ((n = zis.read(buf)) > 0) fos.write(buf, 0, n);
                fos.close();
            }
        }
        zis.close();
    }

    private void zipDir(File dir, File outApk) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outApk));
        zipFile(dir, dir, zos);
        zos.close();
    }

    private void zipFile(File root, File file, ZipOutputStream zos) throws Exception {
        if (file.isDirectory()) {
            File[] kids = file.listFiles();
            if (kids == null) return;
            for (File f : kids) zipFile(root, f, zos);
        } else {
            String path = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
            zos.putNextEntry(new ZipEntry(path));
            InputStream is = new java.io.FileInputStream(file);
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) zos.write(buf, 0, n);
            is.close();
            zos.closeEntry();
        }
    }

    private void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] kids = dir.listFiles();
            if (kids != null) {
                for (File f : kids) deleteDir(f);
            }
        }
        dir.delete();
    }
}