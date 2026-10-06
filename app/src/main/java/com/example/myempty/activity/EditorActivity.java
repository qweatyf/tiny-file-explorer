package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class EditorActivity extends Activity {
    private File srcFile;
    private EditText etContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        String path = getIntent().getStringExtra("file_path");
        if (path == null) { finish(); return; }
        srcFile = new File(path);

        TextView tvName = findViewById(R.id.tv_file_name);
        etContent = findViewById(R.id.et_content);
        tvName.setText(srcFile.getName());

        try {
            FileInputStream fis = new FileInputStream(srcFile);
            byte[] buf = new byte[(int) srcFile.length()];
            int read = 0;
            while (read < buf.length) {
                int n = fis.read(buf, read, buf.length - read);
                if (n < 0) break;
                read += n;
            }
            fis.close();
            etContent.setText(new String(buf, 0, read, "UTF-8"));
        } catch (Exception e) {
            Toast.makeText(this, "读不了：" + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        findViewById(R.id.btn_save).setOnClickListener(v -> save());
    }

    private void save() {
        try {
            String name = srcFile.getName();
            int dot = name.lastIndexOf('.');
            String newName = dot > 0
                ? name.substring(0, dot) + "_edited" + name.substring(dot)
                : name + "_edited";
            File newFile = new File(srcFile.getParent(), newName);

            FileOutputStream fos = new FileOutputStream(newFile);
            fos.write(etContent.getText().toString().getBytes("UTF-8"));
            fos.close();
            Toast.makeText(this, "另存成了：" + newFile.getName(), Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}