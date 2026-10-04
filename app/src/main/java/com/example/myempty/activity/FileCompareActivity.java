package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class FileCompareActivity extends Activity {

    private Uri uri1, uri2;
    private TextView tv1, tv2;
    private int pickSlot = 0;

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
            if (pickSlot == 1) {
                uri1 = uri;
                tv1.setText("文件1: " + uri);
            } else if (pickSlot == 2) {
                uri2 = uri;
                tv2.setText("文件2: " + uri);
            }
        }
    }

    private void doCompare() {
        if (uri1 == null || uri2 == null) {
            Toast.makeText(this, "两个都选一下", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                byte[] b1 = readAll(uri1);
                byte[] b2 = readAll(uri2);

                StringBuilder sb = new StringBuilder();
                sb.append("文件1 大小: ").append(b1.length).append(" 字节\n");
                sb.append("文件2 大小: ").append(b2.length).append(" 字节\n\n");

                if (b1.length != b2.length) {
                    sb.append("结论: 大小不一样，肯定不同");
                } else {
                    boolean same = true;
                    int firstDiff = -1;
                    for (int i = 0; i < b1.length; i++) {
                        if (b1[i] != b2[i]) {
                            same = false;
                            firstDiff = i;
                            break;
                        }
                    }
                    if (same) {
                        sb.append("结论: 一模一样");
                    } else {
                        sb.append("结论: 不同\n");
                        sb.append("第一个不同的位置: 第 ").append(firstDiff).append(" 字节\n");
                        int show = Math.min(16, b1.length - firstDiff);
                        sb.append("文件1: ");
                        for (int i = 0; i < show; i++) {
                            sb.append(String.format("%02x ", b1[firstDiff + i] & 0xFF));
                        }
                        sb.append("\n文件2: ");
                        for (int i = 0; i < show; i++) {
                            sb.append(String.format("%02x ", b2[firstDiff + i] & 0xFF));
                        }
                    }
                }

                runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("对比结果")
                    .setMessage(sb.toString())
                    .setPositiveButton("知道了", null)
                    .show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    "读不了: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
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