package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MineActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

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
                .setTitle(getString(R.string.logout_title))
                .setMessage(getString(R.string.logout_msg))
                .setPositiveButton(getString(R.string.logout_confirm), (d, w) -> {
                    AccountManager.deleteAccount(this);
                    Toast.makeText(this, getString(R.string.logout_done),
                        Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton(getString(R.string.logout_cancel), null)
                .show();
        });
    }
}