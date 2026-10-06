package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipActivity extends Activity {
    private File zipFile;
    private ZipFile zip;
    private String currentPath = "";
    private List<String> entries = new ArrayList<>();
    private List<String> fullPaths = new ArrayList<>();
    private ListView lv;
    private TextView tvPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zip);

        String path = getIntent().getStringExtra("zip_path");
        if (path == null) { finish(); return; }
        zipFile = new File(path);

        try {
            zip = new ZipFile(zipFile);
        } catch (Exception e) {
            Toast.makeText(this, "打不开：" + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvPath = findViewById(R.id.tv_zip_path);
        lv = findViewById(R.id.lv_zip_files);

        showOptions();
    }

    private void showOptions() {
        new AlertDialog.Builder(this)
            .setTitle(zipFile.getName())
            .setItems(new String[]{"看看里面", "解压", "算了"}, (d, which) -> {
                if (which == 0) {
                    currentPath = "";
                    refresh();
                } else if (which == 1) {
                    unzip();
                } else {
                    finish();
                }
            })
            .show();
    }

    private void refresh() {
        tvPath.setText("压缩包内：" + currentPath);
        entries.clear();
        fullPaths.clear();

        Enumeration<? extends ZipEntry> en = zip.entries();
        List<String> dirs = new ArrayList<>();
        List<String> files = new ArrayList<>();

        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement();
            String name = e.getName();
            if (!name.startsWith(currentPath)) continue;
            String rest = name.substring(currentPath.length());
            if (rest.isEmpty() || rest.equals("/")) continue;
            int slash = rest.indexOf('/');
            if (slash >= 0) {
                String dir = rest.substring(0, slash + 1);
                if (!dirs.contains(dir)) dirs.add(dir);
            } else {
                files.add(rest);
            }
        }

        for (String d : dirs) { entries.add("[目录] " + d); fullPaths.add(currentPath + d); }
        for (String f : files) { entries.add("[文件] " + f); fullPaths.add(currentPath + f); }

        lv.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_list_item_1, entries));

        lv.setOnItemClickListener((parent, view, position, id) -> {
            String p = fullPaths.get(position);
            if (p.endsWith("/")) {
                currentPath = p;
                refresh();
            } else {
                openEntry(p);
            }
        });
    }

    private void openEntry(String path) {
        try {
            ZipEntry e = zip.getEntry(path);
            if (e == null) return;

            File tmp = new File(getCacheDir(),
                System.currentTimeMillis() + "_" + new File(path).getName());
            InputStream is = zip.getInputStream(e);
            FileOutputStream fos = new FileOutputStream(tmp);
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
            is.close();
            fos.close();

            Intent i = new Intent(this, EditorActivity.class);
            i.putExtra("file_path", tmp.getAbsolutePath());
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "读不了：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void unzip() {
        try {
            File outDir = new File(zipFile.getParent(), zipFile.getName() + "_解压");
            if (!outDir.exists()) outDir.mkdirs();

            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String entryName = e.getName();
                if (entryName.contains("..")) continue;
                File out = new File(outDir, entryName);
                if (e.isDirectory()) {
                    out.mkdirs();
                } else {
                    out.getParentFile().mkdirs();
                    InputStream is = zip.getInputStream(e);
                    FileOutputStream fos = new FileOutputStream(out);
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
                    is.close();
                    fos.close();
                }
            }

            Toast.makeText(this, "解压到：" + outDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "解压失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}