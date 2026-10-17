package com.example.myempty.activity2;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MemoActivity extends AppCompatActivity {

    private static final String FILE_NAME = "memo.txt";
    private EditText etMemo;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memo);

        etMemo = findViewById(R.id.et_memo);

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

        findViewById(R.id.btn_save_memo).setOnClickListener(v -> {
            try {
                File f = new File(getExternalFilesDir(null), FILE_NAME);
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(etMemo.getText().toString().getBytes());
                fos.close();
                Toast.makeText(this, getString(R.string.memo_saved),
                    Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this,
                    getString(R.string.memo_save_fail) + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            }
        });
    }
}