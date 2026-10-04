package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class FunctionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_function);

        findViewById(R.id.btn_file).setOnClickListener(v ->
            startActivity(new Intent(this, FileActivity.class)));

        findViewById(R.id.btn_file_toolbox).setOnClickListener(v ->
            startActivity(new Intent(this, FileToolboxActivity.class)));

        findViewById(R.id.btn_apks).setOnClickListener(v ->
            startActivity(new Intent(this, ApksActivity.class)));

        findViewById(R.id.btn_server).setOnClickListener(v ->
            startActivity(new Intent(this, ServerActivity.class)));

        findViewById(R.id.btn_transfer).setOnClickListener(v ->
            startActivity(new Intent(this, TransferActivity.class)));

        findViewById(R.id.btn_links).setOnClickListener(v ->
            startActivity(new Intent(this, LinksActivity.class)));

        findViewById(R.id.btn_net_toolbox).setOnClickListener(v ->
            startActivity(new Intent(this, NetToolboxActivity.class)));

        findViewById(R.id.btn_real_draw).setOnClickListener(v ->
            startActivity(new Intent(this, RealDrawActivity.class)));

        findViewById(R.id.btn_hardcore_draw).setOnClickListener(v ->
            startActivity(new Intent(this, HardcoreDrawActivity.class)));

        findViewById(R.id.btn_morse_audio).setOnClickListener(v ->
            startActivity(new Intent(this, MorseAudioActivity.class)));

        findViewById(R.id.btn_morse_terminal).setOnClickListener(v ->
            startActivity(new Intent(this, MorseTerminalActivity.class)));

        findViewById(R.id.btn_real_terminal).setOnClickListener(v ->
            startActivity(new Intent(this, RealTerminalActivity.class)));

        findViewById(R.id.btn_qr).setOnClickListener(v ->
            startActivity(new Intent(this, QrActivity.class)));

        findViewById(R.id.btn_chat).setOnClickListener(v ->
            startActivity(new Intent(this, ChatActivity.class)));

        findViewById(R.id.btn_apk_extract).setOnClickListener(v ->
            startActivity(new Intent(this, ApkExtractActivity.class)));

        findViewById(R.id.btn_image_toolbox).setOnClickListener(v ->
            startActivity(new Intent(this, ImageToolboxActivity.class)));

        findViewById(R.id.btn_doc_toolbox).setOnClickListener(v ->
            startActivity(new Intent(this, DocToolboxActivity.class)));

        findViewById(R.id.btn_media_toolbox).setOnClickListener(v ->
            startActivity(new Intent(this, MediaToolboxActivity.class)));

        findViewById(R.id.btn_crypto_toolbox).setOnClickListener(v ->
            startActivity(new Intent(this, CryptoToolboxActivity.class)));

        findViewById(R.id.btn_unit_convert).setOnClickListener(v ->
            startActivity(new Intent(this, UnitConvertActivity.class)));

        findViewById(R.id.btn_color_pick).setOnClickListener(v ->
            startActivity(new Intent(this, ColorPickActivity.class)));

        findViewById(R.id.btn_qr_batch).setOnClickListener(v ->
            startActivity(new Intent(this, QrBatchActivity.class)));

        findViewById(R.id.btn_clock).setOnClickListener(v ->
            startActivity(new Intent(this, ClockActivity.class)));

        findViewById(R.id.btn_countdown).setOnClickListener(v ->
            startActivity(new Intent(this, CountdownActivity.class)));

        findViewById(R.id.btn_media).setOnClickListener(v ->
            startActivity(new Intent(this, MediaActivity.class)));

        findViewById(R.id.btn_sys_toolbox).setOnClickListener(v ->
            startActivity(new Intent(this, SystemToolboxActivity.class)));

        findViewById(R.id.btn_sysscan).setOnClickListener(v ->
            startActivity(new Intent(this, SysScanActivity.class)));

        bindRootBtn(R.id.btn_backdoor, BackdoorActivity.class);
        bindRootBtn(R.id.btn_backup, BackupActivity.class);
        bindRootBtn(R.id.btn_sysfile, SysFileActivity.class);
    }

    private void bindRootBtn(int id, Class<?> target) {
        Button btn = findViewById(id);
        if (btn == null) return;
        if (!RootUtils.isRooted()) {
            btn.setAlpha(0.4f);
            btn.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("需要 Root 权限，请授权 Root。")
                    .setPositiveButton("知道了", null)
                    .show());
        } else {
            btn.setOnClickListener(v -> startActivity(new Intent(this, target)));
        }
    }
}