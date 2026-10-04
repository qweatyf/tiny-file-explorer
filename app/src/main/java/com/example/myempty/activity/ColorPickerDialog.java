package com.example.myempty.activity2;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

public class ColorPickerDialog extends Dialog {

    public interface OnColorPicked {
        void onPicked(int color);
    }

    private float h = 0f, s = 1f, v = 1f;

    private SvView svView;
    private HueView hueView;
    private View preview;
    private TextView txt;
    private OnColorPicked cb;

    public ColorPickerDialog(Context ctx, int initColor, OnColorPicked l) {
        super(ctx);
        this.cb = l;

        float[] hsv = new float[3];
        Color.colorToHSV(initColor, hsv);
        h = hsv[0];
        s = hsv[1];
        v = hsv[2];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_color_picker);

        FrameLayout box1 = findViewById(R.id.view_sv_container);
        FrameLayout box2 = findViewById(R.id.view_hue_container);

        svView = new SvView(getContext());
        hueView = new HueView(getContext());

        box1.addView(svView, new FrameLayout.LayoutParams(-1, -1));
        box2.addView(hueView, new FrameLayout.LayoutParams(-1, -1));

        preview = findViewById(R.id.view_preview);
        txt = findViewById(R.id.tv_color_value);

        findViewById(R.id.btn_cancel).setOnClickListener(x -> dismiss());

        findViewById(R.id.btn_ok).setOnClickListener(x -> {
            if (cb != null) cb.onPicked(Color.HSVToColor(new float[]{h, s, v}));
            dismiss();
        });

        updatePreview();
    }

    private void updatePreview() {
        int c = Color.HSVToColor(new float[]{h, s, v});
        preview.setBackgroundColor(c);
        txt.setText(String.format("#%06X", 0xFFFFFF & c));
    }

    private float clamp(float x) {
        return x < 0 ? 0 : (x > 1 ? 1 : x);
    }

    class SvView extends View {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        Shader shader;
        boolean drag = false;

        SvView(Context c) {
            super(c);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(3);
            ring.setColor(Color.WHITE);
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            if (w > 0 && h > 0) {
                int c = Color.HSVToColor(new float[]{ColorPickerDialog.this.h, 1f, 1f});
                Shader sat = new LinearGradient(0, 0, w, 0, Color.WHITE, c, Shader.TileMode.CLAMP);
                Shader val = new LinearGradient(0, 0, 0, h, Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP);
                shader = new ComposeShader(sat, val, PorterDuff.Mode.MULTIPLY);
            }
        }

        @Override
        protected void onDraw(Canvas cv) {
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0 || shader == null) return;
            p.setShader(shader);
            cv.drawRect(0, 0, w, h, p);
            cv.drawCircle(ColorPickerDialog.this.s * w, (1 - ColorPickerDialog.this.v) * h, 12, ring);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    drag = true;
                    setPos(e.getX(), e.getY());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (drag) setPos(e.getX(), e.getY());
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    drag = false;
                    return true;
            }
            return false;
        }

        void setPos(float x, float y) {
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            ColorPickerDialog.this.s = clamp(x / w);
            ColorPickerDialog.this.v = 1f - clamp(y / h);
            updatePreview();
            invalidate();
        }

        void refreshHue() {
            invalidate();
        }
    }

    class HueView extends View {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        Shader shader;
        boolean drag = false;

        HueView(Context c) {
            super(c);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(3);
            ring.setColor(Color.WHITE);
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            int[] cs = {Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED};
            shader = new LinearGradient(0, 0, 0, h, cs, null, Shader.TileMode.CLAMP);
        }

        @Override
        protected void onDraw(Canvas cv) {
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0 || shader == null) return;
            p.setShader(shader);
            cv.drawRect(0, 0, w, h, p);
            float cy = ColorPickerDialog.this.h / 360f * h;
            cv.drawRect(new RectF(0, cy - 6, w, cy + 6), ring);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    drag = true;
                    setPos(e.getY());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (drag) setPos(e.getY());
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    drag = false;
                    return true;
            }
            return false;
        }

        void setPos(float y) {
            int h = getHeight();
            if (h <= 0) return;
            ColorPickerDialog.this.h = clamp(y / h) * 360f;
            updatePreview();
            invalidate();
            if (svView != null) svView.refreshHue();
        }
    }
}