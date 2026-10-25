package com.example.myempty.activity2;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MediaToolboxActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_media_toolbox);

        findViewById(R.id.btn_mt_frame).setOnClickListener(v ->
            startActivity(new Intent(this, VideoFrameActivity.class)));

        findViewById(R.id.btn_mt_trim).setOnClickListener(v ->
            startActivity(new Intent(this, AudioTrimActivity.class)));

        findViewById(R.id.btn_mt_extract).setOnClickListener(v ->
            startActivity(new Intent(this, VideoToAudioActivity.class)));

        findViewById(R.id.btn_mt_info).setOnClickListener(v ->
            startActivity(new Intent(this, AudioInfoActivity.class)));
    }
}