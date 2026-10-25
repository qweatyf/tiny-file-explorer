package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipActivity extends AppCompatActivity {
    private File zipFile;
    private ZipFile zip;
    private String currentPath = "";
    private List<String> entries = new ArrayList<>();
    private List<String> fullPaths = new ArrayList<>();
    private ListView lv;
    private TextView tvPath;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

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
            Toast.makeText(this,
                getString(R.string.zip_open_fail) + e.getMessage(),
                Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvPath = findViewById(R.id.tv_zip_path);
        lv = findViewById(R.id.lv_zip_files);

        showOptions();
    }

    private void showOptions() {
        String[] opts = {
            getString(R.string.zip_options_view),
            getString(R.string.zip_options_unzip),
            getString(R.string.zip_options_cancel)
        };
        new AlertDialog.Builder(this)
            .setTitle(zipFile.getName())
            .setItems(opts, (d, which) -> {
                if (which == 0) {
                    currentPath = "";
                    refresh();
                } else if (which == 1) {
                    unzip();
                } else finish();
            })
            .show();
    }

    private void refresh() {
        tvPath.setText(getString(R.string.zip_path_prefix) + currentPath);
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
            } else files.add(rest);
        }

        for (String d : dirs) {
            entries.add(getString(R.string.file_dir) + d);
            fullPaths.add(currentPath + d);
        }
        for (String f : files) {
            entries.add(getString(R.string.file_doc) + f);
            fullPaths.add(currentPath + f);
        }

        lv.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_list_item_1, entries));

        lv.setOnItemClickListener((parent, view, position, id) -> {
            String p = fullPaths.get(position);
            if (p.endsWith("/")) {
                currentPath = p;
                refresh();
            } else openEntry(p);
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
            Toast.makeText(this,
                getString(R.string.zip_read_fail) + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }

    private void unzip() {
        try {
            File outDir = new File(zipFile.getParent(),
                zipFile.getName() + getString(R.string.zip_dir_suffix));
            if (!outDir.exists()) outDir.mkdirs();

            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String entryName = e.getName();
                if (entryName.contains("..")) continue;
                File out = new File(outDir, entryName);
                if (e.isDirectory()) out.mkdirs();
                else {
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

            Toast.makeText(this, getString(R.string.zip_unzip_to) + outDir.getAbsolutePath(),
                Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this,
                getString(R.string.zip_unzip_fail) + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }
}