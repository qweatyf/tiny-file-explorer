package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BatchPipelineActivity extends AppCompatActivity {

    private final List<Uri> selectedFiles = new ArrayList<>();
    private TextView tvStatus;
    private CheckBox cbRename, cbCompress, cbHash, cbEncrypt;
    private EditText etRenameFind, etRenameReplace, etPassword;
    private Button btnStart;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_batch_pipeline);

        tvStatus = findViewById(R.id.tv_bp_status);
        cbRename = findViewById(R.id.cb_bp_rename);
        cbCompress = findViewById(R.id.cb_bp_compress);
        cbHash = findViewById(R.id.cb_bp_hash);
        cbEncrypt = findViewById(R.id.cb_bp_encrypt);

        etRenameFind = findViewById(R.id.et_bp_rename_find);
        etRenameReplace = findViewById(R.id.et_bp_rename_replace);
        etPassword = findViewById(R.id.et_bp_password);

        btnStart = findViewById(R.id.btn_bp_start);

        findViewById(R.id.btn_bp_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(i, 100);
        });

        btnStart.setOnClickListener(v -> startPipeline());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            selectedFiles.clear();
            if (data.getClipData() != null) {
                int n = data.getClipData().getItemCount();
                for (int i = 0; i < n; i++) selectedFiles.add(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) {
                selectedFiles.add(data.getData());
            }
            tvStatus.setText(getString(R.string.bp_selected, selectedFiles.size()));
        }
    }

    private void startPipeline() {
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!cbRename.isChecked() && !cbCompress.isChecked() && !cbHash.isChecked() && !cbEncrypt.isChecked()) {
            Toast.makeText(this, getString(R.string.bp_pick_step), Toast.LENGTH_SHORT).show();
            return;
        }
        
        final String pass = etPassword.getText().toString();
        if (cbEncrypt.isChecked() && pass.isEmpty()) {
            Toast.makeText(this, getString(R.string.file_enc_pass_hint), Toast.LENGTH_SHORT).show();
            return;
        }

        btnStart.setEnabled(false);
        final String rFind = etRenameFind.getText().toString();
        final String rRep = etRenameReplace.getText().toString();

        new Thread(() -> {
            int ok = 0, fail = 0;
            File outDir = new File("/storage/emulated/0/Download/批处理输出");
            if (!outDir.exists()) outDir.mkdirs();

            for (int idx = 0; idx < selectedFiles.size(); idx++) {
                final int cur = idx + 1;
                final int total = selectedFiles.size();
                ui.post(() -> tvStatus.setText(getString(R.string.bp_processing, cur, total)));

                try {
                    Uri uri = selectedFiles.get(idx);
                    String name = queryDisplayName(uri);
                    if (name == null || name.isEmpty()) name = "file_" + System.currentTimeMillis();
                    
                    int dot = name.lastIndexOf('.');
                    String baseName = dot > 0 ? name.substring(0, dot) : name;
                    String ext = dot > 0 ? name.substring(dot) : "";

                    if (cbRename.isChecked() && !rFind.isEmpty()) {
                        baseName = baseName.replace(rFind, rRep);
                    }

                    byte[] data = readUri(uri);
                    File currentTempFile = null;

                    if (cbCompress.isChecked()) {
                        currentTempFile = new File(outDir, baseName + ".zip");
                        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(currentTempFile));
                        ZipEntry entry = new ZipEntry(baseName + ext);
                        zos.putNextEntry(entry);
                        zos.write(data);
                        zos.closeEntry();
                        zos.close();
                        
                        FileInputStream fis = new FileInputStream(currentTempFile);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = fis.read(buf)) > 0) bos.write(buf, 0, n);
                        fis.close();
                        data = bos.toByteArray();
                        name = baseName + ".zip";
                    } else if (cbEncrypt.isChecked()) {
                        data = CryptoUtils.aesEncrypt(data, pass);
                        name = baseName + ".dat";
                    } else {
                        name = baseName + ext;
                    }

                    File finalFile = new File(outDir, name);
                    FileOutputStream fos = new FileOutputStream(finalFile);
                    fos.write(data);
                    fos.close();

                    if (cbHash.isChecked()) {
                        String hash = CryptoUtils.sha256(data);
                        File hFile = new File(outDir, name + ".sha256");
                        FileOutputStream hfos = new FileOutputStream(hFile);
                        hfos.write(hash.getBytes());
                        hfos.close();
                    }

                    if (currentTempFile != null) {
                        currentTempFile.delete();
                    }

                    ok++;
                } catch (Exception e) {
                    fail++;
                }
            }
            final int fok = ok, ffail = fail;
            ui.post(() -> {
                btnStart.setEnabled(true);
                tvStatus.setText(getString(R.string.bp_done, fok, ffail, outDir.getAbsolutePath()));
                new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.file_done))
                    .setMessage(outDir.getAbsolutePath())
                    .setPositiveButton(getString(R.string.lang_ok), null)
                    .show();
            });
        }).start();
    }

    private String queryDisplayName(Uri uri) {
        try {
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String s = c.getString(idx);
                    c.close();
                    return s;
                }
                c.close();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private byte[] readUri(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        is.close();
        return bos.toByteArray();
    }
}