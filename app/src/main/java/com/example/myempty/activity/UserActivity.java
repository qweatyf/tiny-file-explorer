package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;

public class UserActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private TextView tvName;
    private String myName = "用户";

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_user);

        myName = getString(R.string.user_default_name);

        ivAvatar = findViewById(R.id.iv_user_avatar);
        tvName = findViewById(R.id.tv_user_name);

        loadInfo();
        refreshAvatar();

        ivAvatar.setOnClickListener(v -> showAvatarMenu());

        findViewById(R.id.btn_user_rename).setOnClickListener(v -> rename());

        findViewById(R.id.btn_user_delete_avatar).setOnClickListener(v -> {
            if (!AvatarUtils.hasAvatar(this)) {
                Toast.makeText(this, getString(R.string.user_avatar_no_avatar),
                    Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.user_avatar_delete))
                .setMessage(getString(R.string.user_avatar_del_confirm))
                .setPositiveButton(getString(R.string.user_avatar_del_ok), (d, w) -> {
                    AvatarUtils.delete(this);
                    refreshAvatar();
                    Toast.makeText(this, getString(R.string.user_avatar_del_done),
                        Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.user_avatar_del_cancel), null)
                .show();
        });
    }

    private void loadInfo() {
        try {
            File f = new File(getFilesDir(), "account.txt");
            if (!f.exists()) return;
            java.io.FileInputStream fis = new java.io.FileInputStream(f);
            byte[] buf = new byte[fis.available()];
            fis.read(buf);
            fis.close();
            String s = new String(buf);
            for (String line : s.split("\n")) {
                if (line.startsWith("username=")) {
                    myName = line.substring(9).trim();
                }
            }
        } catch (Exception ignored) {}
        tvName.setText(myName);
    }

    private void refreshAvatar() {
        Bitmap av = AvatarUtils.load(this);
        if (av != null) {
            ivAvatar.setImageBitmap(AvatarUtils.makeCircle(av, dp(96)));
        } else {
            ivAvatar.setImageBitmap(AvatarUtils.makeDefault(myName, dp(96)));
        }
    }

    private void showAvatarMenu() {
        String[] ops = {
            getString(R.string.user_avatar_from_album),
            getString(R.string.user_avatar_camera),
            getString(R.string.user_avatar_delete)
        };
        new AlertDialog.Builder(this)
            .setItems(ops, (d, w) -> {
                if (w == 0) pickImage();
                else if (w == 1) Toast.makeText(this,
                    getString(R.string.user_avatar_no_camera), Toast.LENGTH_SHORT).show();
                else {
                    AvatarUtils.delete(this);
                    refreshAvatar();
                }
            })
            .show();
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        startActivityForResult(i, 500);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 500 && res == RESULT_OK && data != null) {
            android.net.Uri uri = data.getData();
            if (uri == null) return;
            if (AvatarUtils.saveFromUri(this, uri)) {
                refreshAvatar();
                Toast.makeText(this, getString(R.string.user_avatar_done),
                    Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.user_avatar_fail),
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void rename() {
        EditText et = new EditText(this);
        et.setText(myName);
        et.setSelection(myName.length());
        et.setHint(getString(R.string.user_rename_hint));

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.user_rename_title))
            .setView(et)
            .setPositiveButton(getString(R.string.user_rename_ok), (d, w) -> {
                String newName = et.getText().toString().trim();
                if (newName.isEmpty()) return;
                saveUsername(newName);
                myName = newName;
                tvName.setText(myName);
                refreshAvatar();
                Toast.makeText(this, getString(R.string.user_rename_done),
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(getString(R.string.user_rename_cancel), null)
            .show();
    }

    private void saveUsername(String name) {
        try {
            File f = new File(getFilesDir(), "account.txt");
            String hash = "";
            if (f.exists()) {
                java.io.FileInputStream fis = new java.io.FileInputStream(f);
                byte[] buf = new byte[fis.available()];
                fis.read(buf);
                fis.close();
                String s = new String(buf);
                for (String line : s.split("\n")) {
                    if (line.startsWith("cardHash=")) {
                        hash = line.substring(9).trim();
                        break;
                    }
                }
            }
            String content = "username=" + name + "\ncardHash=" + hash + "\n";
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(content.getBytes());
            fos.close();
        } catch (Exception ignored) {}
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}