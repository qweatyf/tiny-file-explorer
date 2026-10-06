package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;

public class CryptoActivity extends Activity {
    public static final String MODE_BASE64 = "base64";
    public static final String MODE_MORSE = "morse";
    public static final String MODE_SEVEN = "seven";
    public static final String MODE_PINYIN = "pinyin";

    private String mode;
    private EditText etInput;
    private TextView tvOutput;

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

        String name = "工具";
        String encodeText = "加密/转换";
        String decodeText = "解密/还原";

        if (MODE_BASE64.equals(mode)) {
            name = "BASE64 加密/解密";
        } else if (MODE_MORSE.equals(mode)) {
            name = "摩斯电码加密/解密";
        } else if (MODE_SEVEN.equals(mode)) {
            name = "七层加密/解密";
        } else if (MODE_PINYIN.equals(mode)) {
            name = "中文转拼音";
            encodeText = "中文转拼音";
            decodeText = "拼音转中文";
        }

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
            } else if (MODE_SEVEN.equals(mode)) {
                result = SevenLayer.encode(input);
            } else if (MODE_PINYIN.equals(mode)) {
                result = PinyinUtils.toPinyin(input);
            }
        } catch (Exception e) {
            result = "出错：" + e.getMessage();
        }
        tvOutput.setText(result);
    }

    private void decode() {
        String input = etInput.getText().toString();
        String result = "";
        try {
            if (MODE_BASE64.equals(mode)) {
                result = new String(Base64.decode(input, Base64.DEFAULT), StandardCharsets.UTF_8);
            } else if (MODE_MORSE.equals(mode)) {
                result = MorseUtils.fromMorse(input);
            } else if (MODE_SEVEN.equals(mode)) {
                result = SevenLayer.decode(input);
            } else if (MODE_PINYIN.equals(mode)) {
                try {
                    result = PinyinUtils.fromPinyin(input);
                } catch (Throwable t) {
                    result = "拼音转中文失败：" + t.getMessage();
                }
            }
        } catch (Exception e) {
            result = "出错：" + e.getMessage();
        }
        tvOutput.setText(result);
    }
}