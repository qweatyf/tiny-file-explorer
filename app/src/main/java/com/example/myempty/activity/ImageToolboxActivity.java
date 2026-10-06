package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ImageToolboxActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_image_toolbox);

        findViewById(R.id.btn_img_compress).setOnClickListener(v ->
            startActivity(new Intent(this, ImageCompressActivity.class)));

        findViewById(R.id.btn_img_format).setOnClickListener(v ->
            startActivity(new Intent(this, ImageFormatActivity.class)));

        findViewById(R.id.btn_img_watermark).setOnClickListener(v ->
            startActivity(new Intent(this, ImageWatermarkActivity.class)));

        findViewById(R.id.btn_img_color).setOnClickListener(v ->
            startActivity(new Intent(this, ImageColorActivity.class)));

        findViewById(R.id.btn_gif_make).setOnClickListener(v ->
            startActivity(new Intent(this, GifMakeActivity.class)));

        findViewById(R.id.btn_gif_decode).setOnClickListener(v ->
            startActivity(new Intent(this, GifDecoderActivity.class)));
    }
}