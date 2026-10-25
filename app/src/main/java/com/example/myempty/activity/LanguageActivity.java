package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class LanguageActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_language);

        findViewById(R.id.btn_lang_zh).setOnClickListener(v -> switchTo(LocaleHelper.LANG_ZH));
        findViewById(R.id.btn_lang_en).setOnClickListener(v -> switchTo(LocaleHelper.LANG_EN));

        refreshMarks();
    }

    private void refreshMarks() {
        boolean en = LocaleHelper.isEnglish(this);
        android.widget.Button zh = findViewById(R.id.btn_lang_zh);
        android.widget.Button enBtn = findViewById(R.id.btn_lang_en);
        zh.setAlpha(en ? 0.5f : 1f);
        enBtn.setAlpha(en ? 1f : 0.5f);
    }

    private void switchTo(final String lang) {
        if (lang.equals(LocaleHelper.getLang(this))) return;
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.lang_confirm))
            .setPositiveButton(getString(R.string.lang_ok), (d, w) -> {
                LocaleHelper.setLang(this, lang);
                Intent i = new Intent(this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                finishAffinity();
            })
            .setNegativeButton(getString(R.string.lang_cancel), null)
            .show();
    }
}