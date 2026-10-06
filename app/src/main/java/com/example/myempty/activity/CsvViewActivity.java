package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CsvViewActivity extends Activity {

    private LinearLayout table;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_csv_view);

        table = findViewById(R.id.ll_csv_table);

        findViewById(R.id.btn_csv_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, 100);
        });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                is.close();
                String content = new String(bos.toByteArray(), "UTF-8");
                showTable(content);
            } catch (Exception e) {
                Toast.makeText(this, "读不了", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showTable(String content) {
        table.removeAllViews();
        String[] lines = content.split("\n");
        if (lines.length == 0) return;

        int cols = 0;
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String[] cells = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (int i = 0; i < cells.length; i++) {
                cells[i] = cells[i].replaceAll("^\"|\"$", "").trim();
            }
            rows.add(cells);
            if (cells.length > cols) cols = cells.length;
        }

        for (int r = 0; r < rows.size(); r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            String[] cells = rows.get(r);
            for (int c = 0; c < cols; c++) {
                TextView tv = new TextView(this);
                String txt = c < cells.length ? cells[c] : "";
                tv.setText(txt);
                tv.setPadding(16, 10, 16, 10);
                tv.setTextColor(0xFF333333);
                tv.setTextSize(13);

                if (r == 0) {
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                    tv.setBackgroundColor(0xFFF5C6D8);
                } else {
                    tv.setBackgroundColor(r % 2 == 0 ? 0xFFFFF5F8 : 0xFFFFFFFF);
                }

                tv.setMinWidth(120);
                row.addView(tv);
            }

            table.addView(row);
        }

        Toast.makeText(this, "共 " + rows.size() + " 行，" + cols + " 列",
            Toast.LENGTH_SHORT).show();
    }
}