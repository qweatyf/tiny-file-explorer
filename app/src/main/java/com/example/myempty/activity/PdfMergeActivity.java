package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PdfMergeActivity extends Activity {

    private final List<Uri> pdfs = new ArrayList<>();
    private TextView tvInfo;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_pdf_merge);

        tvInfo = findViewById(R.id.tv_pdf_info);

        findViewById(R.id.btn_pdf_add).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("application/pdf");
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_pdf_clear).setOnClickListener(v -> {
            pdfs.clear();
            updateInfo();
        });

        findViewById(R.id.btn_pdf_merge).setOnClickListener(v -> doMerge());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int n = data.getClipData().getItemCount();
                for (int i = 0; i < n; i++) {
                    pdfs.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                pdfs.add(data.getData());
            }
            updateInfo();
        }
    }

    private void updateInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("已选 ").append(pdfs.size()).append(" 个 PDF\n\n");
        for (int i = 0; i < pdfs.size(); i++) {
            sb.append(i + 1).append(". ").append(pdfs.get(i).getLastPathSegment()).append("\n");
        }
        tvInfo.setText(sb.toString());
    }

    private void doMerge() {
        if (pdfs.size() < 2) {
            Toast.makeText(this, "至少选两个 PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                PDFMergerUtility merger = new PDFMergerUtility();

                List<File> tempFiles = new ArrayList<>();
                for (int i = 0; i < pdfs.size(); i++) {
                    InputStream is = getContentResolver().openInputStream(pdfs.get(i));
                    File temp = new File(getCacheDir(), "pdf_" + i + ".pdf");
                    OutputStream os = new java.io.FileOutputStream(temp);
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
                    is.close();
                    os.close();
                    tempFiles.add(temp);
                    merger.addSource(temp);
                }

                File outDir = new File("/storage/emulated/0/Download/合并PDF");
                if (!outDir.exists()) outDir.mkdirs();
                File out = new File(outDir, "merged_" + System.currentTimeMillis() + ".pdf");

                merger.setDestinationFileName(out.getAbsolutePath());
                merger.mergeDocuments(null);

                for (File f : tempFiles) f.delete();

                ui.post(() -> Toast.makeText(this,
                    "合并好了: " + out.getAbsolutePath(),
                    Toast.LENGTH_LONG).show());
            } catch (Throwable e) {
                ui.post(() -> Toast.makeText(this,
                    "合并失败: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}