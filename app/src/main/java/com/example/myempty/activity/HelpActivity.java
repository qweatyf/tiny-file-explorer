package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        findViewById(R.id.btn_update).setOnClickListener(v -> showUpdate());

        findViewById(R.id.btn_about).setOnClickListener(v ->
            showDialog(getString(R.string.help_about),
                getString(R.string.help_about_text)));

        findViewById(R.id.btn_copyright).setOnClickListener(v ->
            showDialog(getString(R.string.help_copyright),
                getString(R.string.help_copyright_text)));

        findViewById(R.id.btn_promo).setOnClickListener(v ->
            showDialog(getString(R.string.help_promo),
                getString(R.string.help_promo_text)));

        findViewById(R.id.btn_agreement).setOnClickListener(v ->
            showDialog(getString(R.string.help_agreement),
                AgreementUtils.getAgreementText(this)));

        findViewById(R.id.btn_language).setOnClickListener(v ->
            startActivity(new Intent(this, LanguageActivity.class)));
    }

    private void showUpdate() {
        showDialog(getString(R.string.help_update),
            getString(R.string.help_update_text));
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
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0xFFFFF0F5));
        }

        btnOk.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}