package com.example.myempty.activity2;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class QrActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_qr);

        findViewById(R.id.btn_qr_gen).setOnClickListener(v ->
            startActivity(new Intent(this, QrGenActivity.class)));

        findViewById(R.id.btn_qr_scan).setOnClickListener(v ->
            startActivity(new Intent(this, QrScanActivity.class)));

        findViewById(R.id.btn_qr_batch_scan).setOnClickListener(v ->
            startActivity(new Intent(this, QrBatchScanActivity.class)));
    }
}