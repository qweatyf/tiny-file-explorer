package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BatchRenameActivity extends Activity {

    private TextView tvPath;
    private ListView lv;
    private EditText etFind, etReplace;
    private File currentDir = new File("/storage/emulated/0/Download");
    private List<File> files = new ArrayList<>();
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_batch_rename);

        tvPath = findViewById(R.id.tv_rename_path);
        lv = findViewById(R.id.lv_rename_files);
        etFind = findViewById(R.id.et_rename_find);
        etReplace = findViewById(R.id.et_rename_replace);

        adapter = new Adapter();
        lv.setAdapter(adapter);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            File f = files.get(position);
            etFind.setText(f.getName());
            etFind.setSelection(etFind.getText().length());
            etReplace.requestFocus();
        });

        findViewById(R.id.btn_rename_up).setOnClickListener(v -> {
            File p = currentDir.getParentFile();
            if (p != null) {
                currentDir = p;
                refresh();
            }
        });

        findViewById(R.id.btn_rename_pick).setOnClickListener(v -> {
            final EditText et = new EditText(this);
            et.setText(currentDir.getAbsolutePath());
            new AlertDialog.Builder(this)
                .setTitle("输入目录")
                .setView(et)
                .setPositiveButton("进", (d, w) -> {
                    File f = new File(et.getText().toString().trim());
                    if (f.exists() && f.isDirectory()) {
                        currentDir = f;
                        refresh();
                    } else {
                        Toast.makeText(this, "目录不存在", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("算了", null)
                .show();
        });

        findViewById(R.id.btn_rename_preview).setOnClickListener(v -> preview());
        findViewById(R.id.btn_rename_do).setOnClickListener(v -> doRename());

        refresh();
    }

    private void refresh() {
        tvPath.setText(currentDir.getAbsolutePath());
        File[] fs = currentDir.listFiles();
        files.clear();
        if (fs != null) {
            Arrays.sort(fs, (a, b) ->
                a.getName().compareToIgnoreCase(b.getName()));
            files.addAll(Arrays.asList(fs));
        }
        adapter.notifyDataSetChanged();
    }

    private String newName(String oldName, String find, String replace) {
        if (find.isEmpty()) return oldName;
        return oldName.replace(find, replace);
    }

    private void preview() {
        String find = etFind.getText().toString();
        String replace = etReplace.getText().toString();
        if (find.isEmpty()) {
            Toast.makeText(this, "输要替换的内容", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (File f : files) {
            String nn = newName(f.getName(), find, replace);
            if (!nn.equals(f.getName())) {
                sb.append(f.getName()).append("  →  ").append(nn).append("\n");
                count++;
            }
        }

        if (count == 0) {
            new AlertDialog.Builder(this)
                .setTitle("预览")
                .setMessage("没有文件会被改")
                .setPositiveButton("知道了", null)
                .show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("预览（" + count + " 个文件会改）")
            .setMessage(sb.toString())
            .setPositiveButton("知道了", null)
            .show();
    }

    private void doRename() {
        String find = etFind.getText().toString();
        String replace = etReplace.getText().toString();
        if (find.isEmpty()) {
            Toast.makeText(this, "输要替换的内容", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("确定改名？")
            .setMessage("把文件名里的 \"" + find + "\" 换成 \"" + replace + "\"")
            .setPositiveButton("改", (d, w) -> {
                int ok = 0, err = 0;
                for (File f : files) {
                    String nn = newName(f.getName(), find, replace);
                    if (nn.equals(f.getName())) continue;
                    File target = new File(f.getParent(), nn);
                    if (target.exists()) {
                        err++;
                        continue;
                    }
                    if (f.renameTo(target)) ok++;
                    else err++;
                }
                Toast.makeText(this,
                    "改好了 " + ok + " 个，失败 " + err + " 个",
                    Toast.LENGTH_LONG).show();
                refresh();
            })
            .setNegativeButton("算了", null)
            .show();
    }

    class Adapter extends BaseAdapter {
        @Override public int getCount() { return files.size(); }
        @Override public File getItem(int i) { return files.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(BatchRenameActivity.this)
                .inflate(R.layout.item_apk, parent, false);

            File f = getItem(pos);
            android.widget.ImageView iv = v.findViewById(R.id.iv_apk_icon);
            TextView tvName = v.findViewById(R.id.tv_apk_name);
            TextView tvPkg = v.findViewById(R.id.tv_apk_pkg);

            iv.setImageResource(f.isDirectory()
                ? android.R.drawable.ic_menu_view
                : android.R.drawable.ic_menu_agenda);

            String find = etFind.getText().toString();
            String replace = etReplace.getText().toString();
            String newN = newName(f.getName(), find, replace);
            if (!newN.equals(f.getName())) {
                tvName.setText(f.getName() + "  →  " + newN);
                tvName.setTextColor(0xFFD87093);
            } else {
                tvName.setText(f.getName());
                tvName.setTextColor(0xFF333333);
            }

            tvPkg.setText(f.isDirectory() ? "目录" : (f.length() + " 字节"));
            return v;
        }
    }
}