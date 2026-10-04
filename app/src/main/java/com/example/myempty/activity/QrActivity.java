package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class QrActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_qr);

        findViewById(R.id.btn_qr_gen).setOnClickListener(v ->
            startActivity(new Intent(this, QrGenActivity.class)));

        findViewById(R.id.btn_qr_scan).setOnClickListener(v ->
            startActivity(new Intent(this, QrScanActivity.class)));
    }
}