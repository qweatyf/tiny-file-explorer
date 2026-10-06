package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageColorActivity extends Activity {

    private Bitmap srcBmp;
    private ImageView ivPreview;
    private LinearLayout colorBox;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_image_color);

        ivPreview = findViewById(R.id.iv_color_preview);
        colorBox = findViewById(R.id.ll_color_box);

        findViewById(R.id.btn_color_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            i.setType("image/*");
            startActivityForResult(i, 100);
        });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                srcBmp = BitmapFactory.decodeStream(is);
                is.close();
                if (srcBmp != null) {
                    ivPreview.setImageBitmap(srcBmp);
                    ivPreview.setVisibility(ImageView.VISIBLE);
                    extract();
                }
            } catch (Exception e) {
                Toast.makeText(this, "读图失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void extract() {
        new Thread(() -> {
            try {
                int w = srcBmp.getWidth();
                int h = srcBmp.getHeight();
                Map<Integer, Integer> counter = new HashMap<>();

                int stepX = Math.max(1, w / 100);
                int stepY = Math.max(1, h / 100);

                for (int y = 0; y < h; y += stepY) {
                    for (int x = 0; x < w; x += stepX) {
                        int c = srcBmp.getPixel(x, y);
                        int r = (c >> 16) & 0xFF;
                        int g = (c >> 8) & 0xFF;
                        int b = c & 0xFF;
                        int quantR = r / 32 * 32;
                        int quantG = g / 32 * 32;
                        int quantB = b / 32 * 32;
                        int key = (quantR << 16) | (quantG << 8) | quantB;
                        counter.put(key, (counter.containsKey(key) ? counter.get(key) : 0) + 1);
                    }
                }

                List<Map.Entry<Integer, Integer>> list = new ArrayList<>(counter.entrySet());
                Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() {
                    @Override
                    public int compare(Map.Entry<Integer, Integer> a, Map.Entry<Integer, Integer> b) {
                        return b.getValue() - a.getValue();
                    }
                });

                final List<Integer> top = new ArrayList<>();
                for (int i = 0; i < Math.min(8, list.size()); i++) {
                    top.add(list.get(i).getKey());
                }

                runOnUiThread(() -> showColors(top));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    "取色失败: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void showColors(List<Integer> colors) {
        colorBox.removeAllViews();
        colorBox.setVisibility(LinearLayout.VISIBLE);

        for (final int c : colors) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            View box = new View(this);
            box.setBackgroundColor(c);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(60, 60);
            row.addView(box, lp);

            TextView tv = new TextView(this);
            final String hex = String.format("#%06X", 0xFFFFFF & c);
            tv.setText("  " + hex);
            tv.setTextSize(15);
            tv.setTextColor(0xFF333333);
            row.addView(tv);

            row.setOnClickListener(v -> {
                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("color", hex));
                Toast.makeText(this, "复制了: " + hex, Toast.LENGTH_SHORT).show();
            });

            colorBox.addView(row);
        }
    }
}