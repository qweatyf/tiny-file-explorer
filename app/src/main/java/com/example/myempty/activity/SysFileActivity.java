package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SysFileActivity extends Activity {
    private File currentDir = new File("/system/");
    private List<File> items = new ArrayList<>();
    private TextView tvPath;
    private ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sysfile);

        tvPath = findViewById(R.id.tv_sys_path);
        lv = findViewById(R.id.lv_sys_files);
        refresh();

        lv.setOnItemClickListener((parent, view, position, id) -> {
            File f = items.get(position);
            if (f.isDirectory()) {
                currentDir = f;
                refresh();
            } else {
                Intent i = new Intent(this, EditorActivity.class);
                i.putExtra("file_path", f.getAbsolutePath());
                startActivity(i);
            }
        });
    }

    private void refresh() {
        tvPath.setText(currentDir.getAbsolutePath());
        File[] files = currentDir.listFiles();
        items.clear();
        if (files != null) {
            Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            items.addAll(Arrays.asList(files));
        }
        List<String> names = new ArrayList<>();
        for (File f : items) {
            names.add((f.isDirectory() ? "[目录] " : "[文件] ") + f.getName());
        }
        lv.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_list_item_1, names));
    }
}