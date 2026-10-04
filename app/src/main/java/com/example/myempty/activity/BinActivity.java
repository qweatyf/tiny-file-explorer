package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;

public class BinActivity extends Activity {
    private EditText input;
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bin);

        input = findViewById(R.id.et_bin_input);
        output = findViewById(R.id.tv_bin_output);

        findViewById(R.id.btn_to_bin).setOnClickListener(v -> {
            String s = input.getText().toString();
            StringBuilder sb = new StringBuilder();
            for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
                sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF))
                    .replace(' ', '0')).append(" ");
            }
            output.setText(sb.toString().trim());
        });

        findViewById(R.id.btn_from_bin).setOnClickListener(v -> {
            try {
                String[] parts = input.getText().toString().trim().split("\\s+");
                byte[] data = new byte[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    data[i] = (byte) Integer.parseInt(parts[i], 2);
                }
                output.setText(new String(data, StandardCharsets.UTF_8));
            } catch (Exception e) {
                output.setText("转不了：" + e.getMessage());
            }
        });
    }
}