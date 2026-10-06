package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class UserActivity extends Activity {

    private ImageView ivAvatar;
    private TextView tvName;
    private String myName = "用户";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_user);

        ivAvatar = findViewById(R.id.iv_user_avatar);
        tvName = findViewById(R.id.tv_user_name);

        loadInfo();
        refreshAvatar();

        ivAvatar.setOnClickListener(v -> showAvatarMenu());

        findViewById(R.id.btn_user_rename).setOnClickListener(v -> rename());

        findViewById(R.id.btn_user_delete_avatar).setOnClickListener(v -> {
            if (!AvatarUtils.hasAvatar(this)) {
                Toast.makeText(this, "还没设头像呢", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                .setTitle("删头像")
                .setMessage("确定删掉当前头像吗？")
                .setPositiveButton("删", (d, w) -> {
                    AvatarUtils.delete(this);
                    refreshAvatar();
                    Toast.makeText(this, "删了", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("算了", null)
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
            ivAvatar.setImageBitmap(
                AvatarUtils.makeCircle(av, dp(96)));
        } else {
            ivAvatar.setImageBitmap(
                AvatarUtils.makeDefault(myName, dp(96)));
        }
    }

    private void showAvatarMenu() {
        String[] ops = {"从相册选", "拍照", "删掉头像"};
        new AlertDialog.Builder(this)
            .setItems(ops, (d, w) -> {
                if (w == 0) pickImage();
                else if (w == 1) {
                    Toast.makeText(this, "拍照暂不支持", Toast.LENGTH_SHORT).show();
                } else {
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
                Toast.makeText(this, "换好了", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "换不了，换张图试试", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void rename() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_info, null);
        EditText et = new EditText(this);
        et.setText(myName);
        et.setSelection(myName.length());

        new AlertDialog.Builder(this)
            .setTitle("改名字")
            .setView(et)
            .setPositiveButton("改", (d, w) -> {
                String newName = et.getText().toString().trim();
                if (newName.isEmpty()) return;
                saveUsername(newName);
                myName = newName;
                tvName.setText(myName);
                refreshAvatar();
                Toast.makeText(this, "改好了", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("算了", null)
            .show();
    }

    private void saveUsername(String name) {
        try {
            File f = new File(getFilesDir(), "account.txt");
            String content = "";
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
            content = "username=" + name + "\ncardHash=" + hash + "\n";
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(content.getBytes());
            fos.close();
        } catch (Exception ignored) {}
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}