package com.example.myempty.activity2;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;

public class PasswordGenActivity extends Activity {

    private EditText etLength;
    private SeekBar sbLength;
    private TextView tvLength;
    private CheckBox cbUpper;
    private CheckBox cbLower;
    private CheckBox cbDigit;
    private CheckBox cbSymbol;
    private TextView tvOutput;

    private final SecureRandom rng = new SecureRandom();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_password_gen);

        etLength = findViewById(R.id.et_pass_len);
        sbLength = findViewById(R.id.sb_pass_len);
        tvLength = findViewById(R.id.tv_pass_len);
        cbUpper = findViewById(R.id.cb_pass_upper);
        cbLower = findViewById(R.id.cb_pass_lower);
        cbDigit = findViewById(R.id.cb_pass_digit);
        cbSymbol = findViewById(R.id.cb_pass_symbol);
        tvOutput = findViewById(R.id.tv_pass_output);

        cbUpper.setChecked(true);
        cbLower.setChecked(true);
        cbDigit.setChecked(true);
        cbSymbol.setChecked(false);

        etLength.setText("16");
        sbLength.setMax(47);
        sbLength.setProgress(15);
        tvLength.setText("16");

        sbLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int v, boolean u) {
                int len = v + 1;
                tvLength.setText(String.valueOf(len));
                etLength.setText(String.valueOf(len));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        findViewById(R.id.btn_pass_gen).setOnClickListener(v -> gen());
        findViewById(R.id.btn_pass_copy).setOnClickListener(v -> {
            String s = tvOutput.getText().toString();
            if (s.isEmpty()) return;
            ClipboardManager cm = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("pass", s));
            Toast.makeText(this, "复制了", Toast.LENGTH_SHORT).show();
        });
    }

    private void gen() {
        int len;
        try { len = Integer.parseInt(etLength.getText().toString().trim()); }
        catch (Exception e) { len = 16; }
        if (len < 1) len = 1;
        if (len > 128) len = 128;

        StringBuilder pool = new StringBuilder();
        if (cbUpper.isChecked()) pool.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        if (cbLower.isChecked()) pool.append("abcdefghijklmnopqrstuvwxyz");
        if (cbDigit.isChecked()) pool.append("0123456789");
        if (cbSymbol.isChecked()) pool.append("!@#$%^&*()-_=+[]{};:,.<>?");

        if (pool.length() == 0) {
            Toast.makeText(this, "至少勾一个字符集", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(pool.charAt(rng.nextInt(pool.length())));
        }
        tvOutput.setText(sb.toString());
    }
}