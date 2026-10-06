package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MemoActivity extends Activity {
    private static final boolean USE_SERVER = false;
    private static final String FILE_NAME = "memo.txt";

    private EditText etMemo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memo);

        etMemo = findViewById(R.id.et_memo);

        if (!USE_SERVER) {
            try {
                File f = new File(getExternalFilesDir(null), FILE_NAME);
                if (f.exists()) {
                    FileInputStream fis = new FileInputStream(f);
                    byte[] buf = new byte[fis.available()];
                    fis.read(buf);
                    fis.close();
                    etMemo.setText(new String(buf));
                }
            } catch (Exception ignored) {}
        } else {
        }

        findViewById(R.id.btn_save_memo).setOnClickListener(v -> {
            if (!USE_SERVER) {
                try {
                    File f = new File(getExternalFilesDir(null), FILE_NAME);
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(etMemo.getText().toString().getBytes());
                    fos.close();
                    Toast.makeText(this, "存本地了", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "同步到服务器了", Toast.LENGTH_SHORT).show();
            }
        });
    }
}