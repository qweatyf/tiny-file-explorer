package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class FileCompareActivity extends AppCompatActivity {

    private Uri uri1, uri2;
    private TextView tv1, tv2;
    private int pickSlot = 0;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_file_compare);

        tv1 = findViewById(R.id.tv_cmp_file1);
        tv2 = findViewById(R.id.tv_cmp_file2);

        findViewById(R.id.btn_cmp_pick1).setOnClickListener(v -> {
            pickSlot = 1;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_cmp_pick2).setOnClickListener(v -> {
            pickSlot = 2;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_cmp_do).setOnClickListener(v -> doCompare());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (pickSlot == 1) { uri1 = uri; tv1.setText("1: " + uri); }
            else if (pickSlot == 2) { uri2 = uri; tv2.setText("2: " + uri); }
        }
    }

    private void doCompare() {
        if (uri1 == null || uri2 == null) {
            Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                byte[] b1 = readAll(uri1);
                byte[] b2 = readAll(uri2);
                StringBuilder sb = new StringBuilder();
                if (b1.length != b2.length) {
                    sb.append("size diff");
                } else {
                    boolean same = true;
                    for (int i = 0; i < b1.length; i++) {
                        if (b1[i] != b2[i]) { same = false; break; }
                    }
                    sb.append(same ? "same" : "diff");
                }
                runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.file_menu_compare_title))
                    .setMessage(sb.toString())
                    .setPositiveButton(getString(R.string.lang_ok), null).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private byte[] readAll(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        is.close();
        return bos.toByteArray();
    }
}