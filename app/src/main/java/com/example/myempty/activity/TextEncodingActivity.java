package com.example.myempty.activity2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class TextEncodingActivity extends AppCompatActivity {

    private EditText etInput;
    private TextView tvOutput;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_text_encoding);

        etInput = findViewById(R.id.et_enc_input);
        tvOutput = findViewById(R.id.tv_enc_output);

        findViewById(R.id.btn_enc_to_utf8).setOnClickListener(v -> convertTo("UTF-8"));
        findViewById(R.id.btn_enc_to_gbk).setOnClickListener(v -> convertTo("GBK"));
        findViewById(R.id.btn_enc_to_unicode).setOnClickListener(v -> convertTo("Unicode"));
        findViewById(R.id.btn_enc_pick).setOnClickListener(v -> {
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
                etInput.setText(tryDecode(bos.toByteArray()));
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.file_enc_none),
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String tryDecode(byte[] data) {
        try {
            String s = new String(data, "UTF-8");
            if (!s.contains("\uFFFD")) return s;
        } catch (Exception ignored) {}
        try { return new String(data, "GBK"); } catch (Exception ignored) {}
        return new String(data);
    }

    private void convertTo(String target) {
        String src = etInput.getText().toString();
        if (src.isEmpty()) {
            Toast.makeText(this, getString(R.string.file_enc_none),
                Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            if (target.equals("UTF-8")) {
                byte[] b = src.getBytes("UTF-8");
                for (byte x : b) sb.append(String.format("%02x ", x & 0xFF));
                sb.append("\n\n").append(src);
            } else if (target.equals("GBK")) {
                byte[] b = src.getBytes("GBK");
                for (byte x : b) sb.append(String.format("%02x ", x & 0xFF));
                sb.append("\n\n").append(new String(b, "GBK"));
            } else if (target.equals("Unicode")) {
                for (char c : src.toCharArray()) {
                    sb.append("\\u").append(String.format("%04x", (int) c));
                }
            }
            tvOutput.setText(sb.toString());
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}