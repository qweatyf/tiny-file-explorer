package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileSplitActivity extends AppCompatActivity {

    private Uri fileUri;
    private TextView tvFile;
    private EditText etSize;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_file_split);

        tvFile = findViewById(R.id.tv_split_file);
        etSize = findViewById(R.id.et_split_size);

        findViewById(R.id.btn_split_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_split_do).setOnClickListener(v -> doSplit());
        findViewById(R.id.btn_split_merge).setOnClickListener(v -> doMerge());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            fileUri = data.getData();
            tvFile.setText(getString(R.string.file_enc_pick) + ": " + fileUri);
        }
    }

    private void doSplit() {
        if (fileUri == null) {
            Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show();
            return;
        }
        int kb = 1024;
        try { kb = Integer.parseInt(etSize.getText().toString().trim()) * 1024; }
        catch (Exception ignored) {}
        if (kb <= 0) kb = 1024 * 1024;

        final int chunk = kb;
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(fileUri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                is.close();
                byte[] raw = bos.toByteArray();

                File outDir = new File("/storage/emulated/0/Download/"
                    + getString(R.string.title_file_split) + "/"
                    + System.currentTimeMillis());
                if (!outDir.exists()) outDir.mkdirs();

                int parts = (raw.length + chunk - 1) / chunk;
                for (int i = 0; i < parts; i++) {
                    int start = i * chunk;
                    int end = Math.min(start + chunk, raw.length);
                    File out = new File(outDir, "part_" + (i + 1) + ".bin");
                    FileOutputStream fos = new FileOutputStream(out);
                    fos.write(raw, start, end - start);
                    fos.close();
                }

                runOnUiThread(() -> Toast.makeText(this,
                    outDir.getAbsolutePath() + "\n" + parts,
                    Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void doMerge() {
        final EditText et = new EditText(this);
        et.setHint(getString(R.string.split_merge_hint));

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.split_merge_title))
            .setView(et)
            .setPositiveButton(getString(R.string.lang_ok), (d, w) -> {
                String path = et.getText().toString().trim();
                File dir = new File(path);
                if (!dir.exists() || !dir.isDirectory()) {
                    Toast.makeText(this, getString(R.string.file_path_not_exist),
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(() -> {
                    try {
                        File[] parts = dir.listFiles((d2, n2) ->
                            n2.startsWith("part_") && n2.endsWith(".bin"));
                        if (parts == null || parts.length == 0) {
                            runOnUiThread(() -> Toast.makeText(this,
                                getString(R.string.file_enc_none),
                                Toast.LENGTH_SHORT).show());
                            return;
                        }
                        java.util.Arrays.sort(parts, (a, b) -> {
                            int na = Integer.parseInt(a.getName().replaceAll("\\D", ""));
                            int nb = Integer.parseInt(b.getName().replaceAll("\\D", ""));
                            return na - nb;
                        });

                        File out = new File(dir, "merged_" + System.currentTimeMillis() + ".bin");
                        FileOutputStream fos = new FileOutputStream(out);
                        for (File p : parts) {
                            FileInputStream fis = new FileInputStream(p);
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = fis.read(buf)) > 0) fos.write(buf, 0, n);
                            fis.close();
                        }
                        fos.close();

                        runOnUiThread(() -> Toast.makeText(this,
                            out.getAbsolutePath(), Toast.LENGTH_LONG).show());
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(this,
                            e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }).start();
            })
            .setNegativeButton(getString(R.string.file_rename_cancel), null)
            .show();
    }
}