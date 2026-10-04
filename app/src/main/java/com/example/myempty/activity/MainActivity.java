package com.example.myempty.activity2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CrashHandler.install(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        } else {
            requestPermissions(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
        }

        if (AgreementUtils.hasAgreed(this)) {
            if (AccountManager.hasAccount(this)) {
                enterMain();
            } else {
                askForUsername();
            }
        } else {
            showAgreement();
        }
    }

    private void showAgreement() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("用户协议");
        builder.setMessage(AgreementUtils.getAgreementText());
        builder.setCancelable(false);

        builder.setPositiveButton("同意（10）", null);
        builder.setNegativeButton("拒绝", (d, w) -> finish());

        AlertDialog dialog = builder.create();
        dialog.show();

        final int[] count = {10};
        Handler handler = new Handler();
        Runnable run = new Runnable() {
            @Override
            public void run() {
                count[0]--;
                if (count[0] > 0) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setText("同意（" + count[0] + "）");
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    handler.postDelayed(this, 1000);
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setText("同意");
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        };
        handler.postDelayed(run, 1000);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            AgreementUtils.markAgreed(this);
            dialog.dismiss();
            if (AccountManager.hasAccount(this)) {
                enterMain();
            } else {
                askForUsername();
            }
        });
    }

    private void askForUsername() {
        EditText input = new EditText(this);
        input.setHint("请输入你的名字");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
            .setTitle("设置用户名")
            .setMessage("这个名字会用在聊天和资料页")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("确定", (d, w) -> {
                String username = input.getText().toString().trim();
                if (username.isEmpty()) username = "用户";
                try {
                    AccountManager.saveAccount(this, username, "");
                    enterMain();
                } catch (Exception e) {
                    Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    private void enterMain() {
        setContentView(R.layout.activity_main);
        bindButtons();
    }

    private void bindButtons() {
        findViewById(R.id.btn_help).setOnClickListener(v ->
            startActivity(new Intent(this, HelpActivity.class)));

        findViewById(R.id.btn_func).setOnClickListener(v ->
            startActivity(new Intent(this, FunctionActivity.class)));

        findViewById(R.id.btn_encrypt).setOnClickListener(v ->
            startActivity(new Intent(this, EncryptActivity.class)));

        findViewById(R.id.btn_game).setOnClickListener(v ->
            startActivity(new Intent(this, GameActivity.class)));

        findViewById(R.id.btn_mine).setOnClickListener(v ->
            startActivity(new Intent(this, MineActivity.class)));
    }
}