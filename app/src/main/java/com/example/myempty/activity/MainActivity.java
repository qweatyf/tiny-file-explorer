package com.example.myempty.activity2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

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
        builder.setTitle(getString(R.string.help_agreement));
        builder.setMessage(AgreementUtils.getAgreementText(this));
        builder.setCancelable(false);

        builder.setPositiveButton(getString(R.string.lang_ok) + "（10）", null);
        builder.setNegativeButton(getString(R.string.lang_cancel), (d, w) -> finish());

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
                        .setText(getString(R.string.lang_ok) + "（" + count[0] + "）");
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    handler.postDelayed(this, 1000);
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setText(getString(R.string.lang_ok));
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
        input.setHint(getString(R.string.user_rename_hint));
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.user_rename_title))
            .setView(input)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.lang_ok), (d, w) -> {
                String username = input.getText().toString().trim();
                if (username.isEmpty()) username = getString(R.string.user_default_name);
                try {
                    AccountManager.saveAccount(this, username, "");
                    enterMain();
                } catch (Exception e) {
                    Toast.makeText(this, getString(R.string.file_fail),
                        Toast.LENGTH_SHORT).show();
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