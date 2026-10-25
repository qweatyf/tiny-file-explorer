package com.example.myempty.activity2;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class DocToolboxActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_doc_toolbox);

        findViewById(R.id.btn_doc_md).setOnClickListener(v ->
            startActivity(new Intent(this, MarkdownViewActivity.class)));

        findViewById(R.id.btn_doc_csv).setOnClickListener(v ->
            startActivity(new Intent(this, CsvViewActivity.class)));

        findViewById(R.id.btn_doc_enc).setOnClickListener(v ->
            startActivity(new Intent(this, TextEncodingActivity.class)));

        findViewById(R.id.btn_doc_pdf).setOnClickListener(v ->
            startActivity(new Intent(this, PdfMergeActivity.class)));
    }
}