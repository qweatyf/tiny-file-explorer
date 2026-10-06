package com.example.myempty.activity2;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageViewActivity extends Activity {
    private ImageView iv;
    private Matrix matrix = new Matrix();
    private ScaleGestureDetector scaleDetector;
    private float lastX, lastY;
    private float scale = 1f;
    private float rotation = 0f;

    private List<File> images = new ArrayList<>();
    private int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        iv = findViewById(R.id.iv_image);
        iv.setScaleType(ImageView.ScaleType.MATRIX);

        String path = getIntent().getStringExtra("path");
        String dir = getIntent().getStringExtra("dir");
        if (path == null) { finish(); return; }

        if (dir != null) {
            File[] fs = new File(dir).listFiles();
            if (fs != null) {
                Arrays.sort(fs, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                for (File f : fs) {
                    String n = f.getName().toLowerCase();
                    if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                        || n.endsWith(".gif") || n.endsWith(".webp") || n.endsWith(".bmp")) {
                        images.add(f);
                    }
                }
            }
        }
        if (images.isEmpty()) images.add(new File(path));

        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getAbsolutePath().equals(path)) {
                index = i;
                break;
            }
        }

        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float f = detector.getScaleFactor();
                scale *= f;
                scale = Math.max(0.5f, Math.min(scale, 10f));
                applyTransform();
                return true;
            }
        });

        findViewById(R.id.btn_rotate).setOnClickListener(v -> {
            rotation += 90;
            rotation %= 360;
            applyTransform();
        });

        findViewById(R.id.btn_reset).setOnClickListener(v -> {
            scale = 1f;
            rotation = 0f;
            applyTransform();
        });

        loadImage();
    }

    private void loadImage() {
        if (images.isEmpty()) return;
        iv.setImageURI(android.net.Uri.fromFile(images.get(index)));
        scale = 1f;
        rotation = 0f;
        applyTransform();
    }

    private void applyTransform() {
        matrix.reset();
        Drawable d = iv.getDrawable();
        if (d != null) {
            float vw = iv.getWidth();
            float vh = iv.getHeight();
            float dw = d.getIntrinsicWidth();
            float dh = d.getIntrinsicHeight();

            matrix.postScale(scale, scale, vw / 2f, vh / 2f);
            matrix.postRotate(rotation, vw / 2f, vh / 2f);

            float tx = (vw - dw) / 2f;
            float ty = (vh - dh) / 2f;
            matrix.postTranslate(tx, ty);
        }
        iv.setImageMatrix(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress() && event.getPointerCount() == 1) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 100) {
                        if (dx > 0 && index > 0) { index--; loadImage(); }
                        else if (dx < 0 && index < images.size() - 1) { index++; loadImage(); }
                        lastX = event.getX();
                        lastY = event.getY();
                    } else {
                        matrix.postTranslate(dx, dy);
                        iv.setImageMatrix(matrix);
                        lastX = event.getX();
                        lastY = event.getY();
                    }
                }
                break;
        }
        return true;
    }
}