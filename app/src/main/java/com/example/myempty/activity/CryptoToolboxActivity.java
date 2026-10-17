package com.example.myempty.activity2;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class CryptoToolboxActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_crypto_toolbox);

        findViewById(R.id.btn_crypto_aes).setOnClickListener(v ->
            startActivity(new Intent(this, AesToolActivity.class)));

        findViewById(R.id.btn_crypto_hash).setOnClickListener(v ->
            startActivity(new Intent(this, HashToolActivity.class)));

        findViewById(R.id.btn_crypto_pass).setOnClickListener(v ->
            startActivity(new Intent(this, PasswordGenActivity.class)));
    }
}