package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

public class TransferActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        findViewById(R.id.btn_send).setOnClickListener(v ->
            startActivity(new Intent(this, SendActivity.class)));

        findViewById(R.id.btn_receive).setOnClickListener(v -> {
            if (!ServerState.isRunning) {
                new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("本地服务器还没开，去开一下吧。")
                    .setPositiveButton("去开", (d, w) ->
                        startActivity(new Intent(this, ServerActivity.class)))
                    .setNegativeButton("算了", null)
                    .show();
            } else {
                startActivity(new Intent(this, ReceiveActivity.class));
            }
        });
    }
}