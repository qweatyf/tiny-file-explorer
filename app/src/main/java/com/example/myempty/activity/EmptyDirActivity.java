package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EmptyDirActivity extends AppCompatActivity {

    private EditText etDir;
    private ListView lv;
    private final List<File> emptyDirs = new ArrayList<>();
    private Adapter adapter;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean scanning = false;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_empty_dir);

        etDir = findViewById(R.id.et_empty_dir);
        lv = findViewById(R.id.lv_empty_dirs);

        adapter = new Adapter();
        lv.setAdapter(adapter);

        findViewById(R.id.btn_empty_scan).setOnClickListener(v -> doScan());
        findViewById(R.id.btn_empty_delete).setOnClickListener(v -> doDelete());
    }

    private void doScan() {
        if (scanning) {
            Toast.makeText(this, getString(R.string.empty_scanning), Toast.LENGTH_SHORT).show();
            return;
        }
        String path = etDir.getText().toString().trim();
        if (path.isEmpty()) path = "/storage/emulated/0";
        File root = new File(path);
        if (!root.exists() || !root.isDirectory()) {
            Toast.makeText(this, getString(R.string.file_path_not_exist),
                Toast.LENGTH_SHORT).show();
            return;
        }

        scanning = true;
        emptyDirs.clear();
        adapter.notifyDataSetChanged();

        final File froot = root;
        new Thread(() -> {
            final List<File> found = new ArrayList<>();
            try { scan(froot, 0, found); } catch (Throwable ignored) {}
            ui.post(() -> {
                scanning = false;
                emptyDirs.clear();
                emptyDirs.addAll(found);
                adapter.notifyDataSetChanged();
                Toast.makeText(this, found.size() + "", Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private boolean scan(File dir, int depth, List<File> found) {
        if (depth > 8) return false;
        File[] fs = dir.listFiles();
        if (fs == null) return false;

        boolean empty = true;
        for (File f : fs) {
            if (f.isDirectory()) {
                scan(f, depth + 1, found);
            } else {
                empty = false;
            }
        }
        if (empty) { found.add(dir); return true; }
        return false;
    }

    private void doDelete() {
        if (emptyDirs.isEmpty()) {
            Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_empty_dir))
            .setMessage(emptyDirs.size() + "")
            .setPositiveButton(getString(R.string.file_del_ok), (d, w) -> {
                int ok = 0, err = 0;
                for (File f : new ArrayList<>(emptyDirs)) {
                    if (f.delete()) ok++; else err++;
                }
                Toast.makeText(this, ok + " / " + err, Toast.LENGTH_LONG).show();
                doScan();
            })
            .setNegativeButton(getString(R.string.file_del_cancel), null)
            .show();
    }

    class Adapter extends BaseAdapter {
        @Override public int getCount() { return emptyDirs.size(); }
        @Override public File getItem(int i) { return emptyDirs.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(EmptyDirActivity.this)
                .inflate(R.layout.item_apk, parent, false);
            File f;
            try { f = getItem(pos); } catch (Throwable t) { return v; }
            if (f == null) return v;

            android.widget.ImageView iv = v.findViewById(R.id.iv_apk_icon);
            TextView tvName = v.findViewById(R.id.tv_apk_name);
            TextView tvPkg = v.findViewById(R.id.tv_apk_pkg);

            try {
                iv.setImageResource(android.R.drawable.ic_menu_view);
                tvName.setText(f.getName());
                tvPkg.setText(f.getAbsolutePath());
            } catch (Throwable ignored) {}
            return v;
        }
    }
}