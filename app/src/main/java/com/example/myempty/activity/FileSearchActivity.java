package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileSearchActivity extends Activity {

    private EditText etKeyword, etDir;
    private ListView lv;
    private final List<File> results = new ArrayList<>();
    private Adapter adapter;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean searching = false;
    private volatile boolean stopFlag = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_file_search);

        etKeyword = findViewById(R.id.et_search_kw);
        etDir = findViewById(R.id.et_search_dir);
        lv = findViewById(R.id.lv_search_results);

        adapter = new Adapter();
        lv.setAdapter(adapter);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            try {
                File f = results.get(position);
                Toast.makeText(this, f.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (Throwable ignored) {}
        });

        lv.setOnItemLongClickListener((parent, view, position, id) -> {
            try {
                File f = results.get(position);
                showItemMenu(f);
            } catch (Throwable ignored) {}
            return true;
        });

        findViewById(R.id.btn_search_do).setOnClickListener(v -> doSearch());
    }

    private void showItemMenu(final File f) {
        String[] ops = {"定位文件位置", "复制路径", "复制文件名", "复制整个文件"};

        new AlertDialog.Builder(this)
            .setTitle(f.getName())
            .setItems(ops, (d, w) -> {
                if (w == 0) openInFileManager(f);
                else if (w == 1) copyToClipboard(f.getAbsolutePath());
                else if (w == 2) copyToClipboard(f.getName());
                else if (w == 3) copyFileToDownload(f);
            })
            .show();
    }

    private void openInFileManager(File f) {
        try {
            File dir = f.isDirectory() ? f : f.getParentFile();
            if (dir == null) {
                Toast.makeText(this, "找不到目录", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(this, FileActivity.class);
            i.putExtra("start_dir", dir.getAbsolutePath());
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "打不开: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager cm = (ClipboardManager)
            getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("path", text));
        Toast.makeText(this, "复制好了: " + text, Toast.LENGTH_SHORT).show();
    }

    private void copyFileToDownload(final File src) {
        new Thread(() -> {
            try {
                File outDir = new File("/storage/emulated/0/Download/搜索结果");
                if (!outDir.exists()) outDir.mkdirs();
                File dst = new File(outDir, src.getName());
                if (dst.exists()) {
                    dst = new File(outDir,
                        System.currentTimeMillis() + "_" + src.getName());
                }

                java.io.FileInputStream fis = new java.io.FileInputStream(src);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(dst);
                byte[] buf = new byte[8192];
                int n;
                while ((n = fis.read(buf)) > 0) fos.write(buf, 0, n);
                fis.close();
                fos.close();

                final File fdst = dst;
                ui.post(() -> Toast.makeText(this,
                    "复制好了: " + fdst.getAbsolutePath(),
                    Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                ui.post(() -> Toast.makeText(this,
                    "复制失败: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void doSearch() {
        if (searching) {
            Toast.makeText(this, "还在搜呢", Toast.LENGTH_SHORT).show();
            return;
        }
        String kw = etKeyword.getText().toString().trim();
        if (kw.isEmpty()) {
            Toast.makeText(this, "输关键词", Toast.LENGTH_SHORT).show();
            return;
        }
        String path = etDir.getText().toString().trim();
        if (path.isEmpty()) path = "/storage/emulated/0";
        File root = new File(path);
        if (!root.exists() || !root.isDirectory()) {
            Toast.makeText(this, "目录不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        final String fkw = kw.toLowerCase();
        final File froot = root;

        searching = true;
        stopFlag = false;
        results.clear();
        adapter.notifyDataSetChanged();

        new Thread(() -> {
            final List<File> found = new ArrayList<>();
            try {
                searchToList(froot, fkw, 0, found);
            } catch (Throwable t) {
                ui.post(() -> Toast.makeText(this,
                    "搜索出错: " + t.getMessage(),
                    Toast.LENGTH_LONG).show());
            } finally {
                searching = false;
                ui.post(() -> {
                    results.clear();
                    results.addAll(found);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this,
                        "搜完了，共 " + found.size() + " 个结果",
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void searchToList(File dir, String kw, int depth, List<File> found) {
        if (stopFlag) return;
        if (depth > 6) return;
        if (found.size() > 200) return;

        File[] fs;
        try {
            fs = dir.listFiles();
        } catch (Throwable t) {
            return;
        }
        if (fs == null) return;

        for (File f : fs) {
            if (stopFlag) return;
            if (found.size() > 200) return;
            try {
                if (f.getName().toLowerCase().contains(kw)) {
                    found.add(f);
                }
                if (f.isDirectory() && !f.getName().startsWith(".")) {
                    searchToList(f, kw, depth + 1, found);
                }
            } catch (Throwable ignored) {}
        }
    }

    @Override
    protected void onDestroy() {
        stopFlag = true;
        super.onDestroy();
    }

    class Adapter extends BaseAdapter {
        @Override public int getCount() { return results.size(); }
        @Override public File getItem(int i) { return results.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(FileSearchActivity.this)
                .inflate(R.layout.item_apk, parent, false);

            File f;
            try {
                f = getItem(pos);
            } catch (Throwable t) {
                return v;
            }
            if (f == null) return v;

            android.widget.ImageView iv = v.findViewById(R.id.iv_apk_icon);
            TextView tvName = v.findViewById(R.id.tv_apk_name);
            TextView tvPkg = v.findViewById(R.id.tv_apk_pkg);

            try {
                iv.setImageResource(f.isDirectory()
                    ? android.R.drawable.ic_menu_view
                    : android.R.drawable.ic_menu_agenda);
                tvName.setText(f.getName());
                tvPkg.setText(f.getAbsolutePath());
            } catch (Throwable ignored) {}
            return v;
        }
    }
}