package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class CryptoToolboxActivity extends Activity {
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