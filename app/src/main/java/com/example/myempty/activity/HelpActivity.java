package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class HelpActivity extends Activity {
    private static final boolean USE_SERVER = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        findViewById(R.id.btn_update).setOnClickListener(v -> showUpdate());

        findViewById(R.id.btn_about).setOnClickListener(v ->
            showDialog("关于应用",
                "小型文件管理器\n\n" +
                "版本：2.0\n\n" +
                "文件管理、加密、快传，功能齐全。"));

        findViewById(R.id.btn_copyright).setOnClickListener(v ->
            showDialog("版权说明",
                "本项目为开源工具，仅供学习与个人使用。\n\n" +
                "若相关内容涉及版权问题，请联系处理。"));

        findViewById(R.id.btn_promo).setOnClickListener(v ->
            showDialog("开源说明",
                "本项目源码公开，可自由使用与修改。\n\n" +
                "欢迎提出建议与反馈。"));

        findViewById(R.id.btn_agreement).setOnClickListener(v ->
            showDialog("用户协议", AgreementUtils.getAgreementText()));
    }

    private void showUpdate() {
        if (USE_SERVER) {
            showDialog("应用更新", "（服务器版本还没接）");
        } else {
            showDialog("应用更新",
                "当前版本：2.0\n\n" +
                "更新内容：\n" +
                "1. 新增系统工具箱\n" +
                "2. 新增网络工具箱\n" +
                "3. 新增加密工具箱\n" +
                "4. 优化整体体验");
        }
    }

    private void showDialog(String title, String msg) {
        View view = LayoutInflater.from(this)
            .inflate(R.layout.dialog_info, null);

        TextView tvTitle = view.findViewById(R.id.dlg_title);
        TextView tvMsg = view.findViewById(R.id.dlg_msg);
        Button btnOk = view.findViewById(R.id.dlg_ok);

        tvTitle.setText(title);
        tvMsg.setText(msg);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(0xFFFFF0F5));
        }

        btnOk.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}