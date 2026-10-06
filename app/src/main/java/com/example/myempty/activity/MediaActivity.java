package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;

public class MediaActivity extends Activity {
    private VideoView vv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        vv = findViewById(R.id.vv_media);

        String path = getIntent().getStringExtra("path");
        if (path != null) {
            Uri uri = Uri.fromFile(new File(path));
            vv.setVideoURI(uri);
            vv.start();
        }

        findViewById(R.id.btn_pick_media).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_05).setOnClickListener(v -> setSpeed(0.5f));
        findViewById(R.id.btn_075).setOnClickListener(v -> setSpeed(0.75f));
        findViewById(R.id.btn_1).setOnClickListener(v -> setSpeed(1.0f));
        findViewById(R.id.btn_15).setOnClickListener(v -> setSpeed(1.5f));
        findViewById(R.id.btn_2).setOnClickListener(v -> setSpeed(2.0f));
        findViewById(R.id.btn_3).setOnClickListener(v -> setSpeed(3.0f));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            vv.setVideoURI(uri);
            vv.start();
        }
    }

    private void setSpeed(float speed) {
        try {
            android.media.MediaPlayer mp = (android.media.MediaPlayer)
                vv.getClass().getMethod("getMediaPlayer").invoke(vv);
            if (mp != null) {
                android.media.PlaybackParams params = mp.getPlaybackParams();
                params.setSpeed(speed);
                mp.setPlaybackParams(params);
            }
        } catch (Exception e) {
            Toast.makeText(this, "这机子不支持变速", Toast.LENGTH_SHORT).show();
        }
    }
}