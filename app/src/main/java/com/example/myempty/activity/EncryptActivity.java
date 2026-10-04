package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class EncryptActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypt);

        findViewById(R.id.btn_bin).setOnClickListener(v ->
            startActivity(new Intent(this, BinActivity.class)));

        findViewById(R.id.btn_base64).setOnClickListener(v -> startCrypto(CryptoActivity.MODE_BASE64));
        findViewById(R.id.btn_morse).setOnClickListener(v -> startCrypto(CryptoActivity.MODE_MORSE));
        findViewById(R.id.btn_seven).setOnClickListener(v -> startCrypto(CryptoActivity.MODE_SEVEN));
        findViewById(R.id.btn_pinyin).setOnClickListener(v -> startCrypto(CryptoActivity.MODE_PINYIN));
    }

    private void startCrypto(String mode) {
        Intent i = new Intent(this, CryptoActivity.class);
        i.putExtra("mode", mode);
        startActivity(i);
    }
}