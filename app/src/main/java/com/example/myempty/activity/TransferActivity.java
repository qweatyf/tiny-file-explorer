package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class TransferActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        findViewById(R.id.btn_send).setOnClickListener(v ->
            startActivity(new Intent(this, SendActivity.class)));

        findViewById(R.id.btn_receive).setOnClickListener(v -> {
            if (!ServerState.isRunning) {
                new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.title_server))
                    .setMessage(getString(R.string.transfer_need_server))
                    .setPositiveButton(getString(R.string.lang_ok), (d, w) ->
                        startActivity(new Intent(this, ServerActivity.class)))
                    .setNegativeButton(getString(R.string.file_rename_cancel), null)
                    .show();
            } else {
                startActivity(new Intent(this, ReceiveActivity.class));
            }
        });
    }
}