package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class FileToolboxActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_file_toolbox);

        findViewById(R.id.btn_tool_encrypt).setOnClickListener(v ->
            startActivity(new Intent(this, FileEncryptActivity.class)));

        findViewById(R.id.btn_tool_compress).setOnClickListener(v ->
            startActivity(new Intent(this, FileCompressActivity.class)));

        findViewById(R.id.btn_tool_rename).setOnClickListener(v ->
            startActivity(new Intent(this, BatchRenameActivity.class)));

        findViewById(R.id.btn_tool_search).setOnClickListener(v ->
            startActivity(new Intent(this, FileSearchActivity.class)));

        findViewById(R.id.btn_tool_compare).setOnClickListener(v ->
            startActivity(new Intent(this, FileCompareActivity.class)));

        findViewById(R.id.btn_tool_hash).setOnClickListener(v ->
            startActivity(new Intent(this, FileHashActivity.class)));

        findViewById(R.id.btn_tool_split).setOnClickListener(v ->
            startActivity(new Intent(this, FileSplitActivity.class)));

        findViewById(R.id.btn_tool_empty).setOnClickListener(v ->
            startActivity(new Intent(this, EmptyDirActivity.class)));
    }
}