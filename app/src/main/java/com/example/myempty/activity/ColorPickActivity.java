package com.example.myempty.activity2;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ColorPickActivity extends Activity {

    private View viewColor;
    private TextView tvValue;

    private int currentColor = 0xFFD87093;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_color_pick);

        viewColor = findViewById(R.id.view_cp_preview);
        tvValue = findViewById(R.id.tv_cp_value);

        viewColor.setBackgroundColor(currentColor);
        updateValue();

        findViewById(R.id.btn_cp_pick).setOnClickListener(v ->
            new ColorPickerDialog(this, currentColor, c -> {
                currentColor = c;
                viewColor.setBackgroundColor(c);
                updateValue();
            }).show());

        findViewById(R.id.btn_cp_copy).setOnClickListener(v -> {
            String hex = String.format("#%06X", 0xFFFFFF & currentColor);
            ClipboardManager cm = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("color", hex));
            Toast.makeText(this, "复制了: " + hex, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateValue() {
        int r = (currentColor >> 16) & 0xFF;
        int g = (currentColor >> 8) & 0xFF;
        int b = currentColor & 0xFF;
        String hex = String.format("#%06X", 0xFFFFFF & currentColor);
        tvValue.setText(hex + "\nRGB(" + r + ", " + g + ", " + b + ")");
    }
}