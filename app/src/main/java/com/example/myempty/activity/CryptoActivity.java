package com.example.myempty.activity2;

import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;

public class CryptoActivity extends AppCompatActivity {
    public static final String MODE_BASE64 = "base64";
    public static final String MODE_MORSE = "morse";
    public static final String MODE_PINYIN = "pinyin";

    private String mode;
    private EditText etInput;
    private TextView tvOutput;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crypto);

        mode = getIntent().getStringExtra("mode");
        etInput = findViewById(R.id.et_input);
        tvOutput = findViewById(R.id.tv_output);

        TextView title = findViewById(R.id.tv_title);
        Button btnEncode = findViewById(R.id.btn_encode);
        Button btnDecode = findViewById(R.id.btn_decode);

        String name = "Crypto";
        String encodeText = getString(R.string.file_enc_do);
        String decodeText = getString(R.string.file_enc_dec);

        if (MODE_BASE64.equals(mode)) name = "BASE64";
        else if (MODE_MORSE.equals(mode)) name = "Morse";
        else if (MODE_PINYIN.equals(mode)) name = "Pinyin";

        title.setText(name);
        btnEncode.setText(encodeText);
        btnDecode.setText(decodeText);

        btnEncode.setOnClickListener(v -> encode());
        btnDecode.setOnClickListener(v -> decode());
    }

    private void encode() {
        String input = etInput.getText().toString();
        String result = "";
        try {
            if (MODE_BASE64.equals(mode)) {
                result = Base64.encodeToString(input.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
            } else if (MODE_MORSE.equals(mode)) {
                result = MorseUtils.toMorse(input);
            } else if (MODE_PINYIN.equals(mode)) {
                result = PinyinUtils.toPinyin(input);
            }
        } catch (Exception e) {
            result = e.getMessage();
        }
        tvOutput.setText(result);
    }

    private void decode() {
        String input = etInput.getText().toString();
        if (input.isEmpty()) return;

        if (MODE_PINYIN.equals(mode)) {
            tvOutput.setText("查询中...");
            PinyinUtils.fromPinyinAsync(input, result -> {
                runOnUiThread(() -> tvOutput.setText(result));
            });
            return;
        }

        String result = "";
        try {
            if (MODE_BASE64.equals(mode)) {
                result = new String(Base64.decode(input, Base64.DEFAULT), StandardCharsets.UTF_8);
            } else if (MODE_MORSE.equals(mode)) {
                result = MorseUtils.fromMorse(input);
            }
        } catch (Exception e) {
            result = e.getMessage();
        }
        tvOutput.setText(result);
    }
}