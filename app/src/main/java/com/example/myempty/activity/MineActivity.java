package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class MineActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mine);

        findViewById(R.id.btn_user).setOnClickListener(v ->
            startActivity(new Intent(this, UserActivity.class)));

        findViewById(R.id.btn_account).setOnClickListener(v ->
            startActivity(new Intent(this, AccountActivity.class)));

        findViewById(R.id.btn_memo).setOnClickListener(v ->
            startActivity(new Intent(this, MemoActivity.class)));

        findViewById(R.id.btn_vault).setOnClickListener(v ->
            startActivity(new Intent(this, VaultActivity.class)));

        findViewById(R.id.btn_mine_account_book).setOnClickListener(v ->
            startActivity(new Intent(this, AccountBookActivity.class)));

        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("注销账号")
                .setMessage("真要注销？注销完得重新输卡密。")
                .setPositiveButton("确定", (d, w) -> {
                    AccountManager.deleteAccount(this);
                    Toast.makeText(this, "注销了", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton("算了", null)
                .show();
        });
    }
}