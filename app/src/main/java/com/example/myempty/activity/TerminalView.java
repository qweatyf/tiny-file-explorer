package com.example.myempty.activity2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.util.ArrayList;
import java.util.List;

public class TerminalView extends View {

    public static final int COLOR_NORMAL = 0xFF000000;
    public static final int COLOR_OK = 0xFF1B5E20;
    public static final int COLOR_ERR = 0xFFB71C1C;
    public static final int COLOR_USER = 0xFFD87093;

    public interface InputListener {
        void onInputChanged(String text);
        void onEnter(String text);
    }

    class Line {
        String text;
        int color;
        Line(String t, int c) { text = t; color = c; }
    }

    private final List<Line> lines = new ArrayList<>();
    private String input = "";
    private String composing = "";
    private Runnable onTap;
    private InputListener inputListener;

    private boolean selecting = false;
    private int selStart = -1, selEnd = -1;
    private float dot1X, dot1Y, dot2X, dot2Y;
    private int draggingDot = 0;

    private Paint paint;
    private Paint bgPaint;
    private Paint hlPaint;
    private Paint cursorPaint;
    private Paint composingPaint;

    private float fontSize = 40f;
    private float lineGap = 1.35f;
    private float pad = 28f;
    private float charW;

    private int scrollX = 0;
    private int maxScrollX = 0;

    private int scrollY = 0;
    private int maxScrollY = 0;
    private boolean autoScroll = true;

    private Bitmap bg;
    private int bgW = 0, bgH = 0;

    private boolean cursorOn = true;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable blink = new Runnable() {
        @Override public void run() {
            cursorOn = !cursorOn;
            invalidate();
            ui.postDelayed(this, 500);
        }
    };

    private ScaleGestureDetector scaleDetector;
    private GestureDetector tapDetector;

    private float[] lineTops;
    private int[] lineStarts;
    private String flatText = "";
    private float contentH = 0;

    private long lastLongPress = 0;

    private float downX = 0, downY = 0;
    private boolean dragging = false;
    private boolean movedFar = false;
    private long downTime = 0;
    private boolean multiTouch = false;
    private boolean isHorizontal = false;
    private boolean longPressed = false;

    public TerminalView(Context c) { super(c); init(); }
    public TerminalView(Context c, AttributeSet a) { super(c, a); init(); }
    public TerminalView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setColor(COLOR_NORMAL);
        paint.setTextSize(fontSize);
        paint.setStyle(Paint.Style.FILL);
        charW = paint.measureText("M");

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setFilterBitmap(true);

        hlPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hlPaint.setColor(0x66D87093);
        hlPaint.setStyle(Paint.Style.FILL);

        cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cursorPaint.setColor(COLOR_USER);
        cursorPaint.setStyle(Paint.Style.FILL);

        composingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        composingPaint.setTypeface(Typeface.MONOSPACE);
        composingPaint.setColor(0xFF888888);
        composingPaint.setTextSize(fontSize);
        composingPaint.setStyle(Paint.Style.FILL);
        composingPaint.setUnderlineText(true);

        setFocusable(true);
        setFocusableInTouchMode(true);

        loadBg();

        scaleDetector = new ScaleGestureDetector(getContext(),
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector d) {
                    float old = fontSize;
                    fontSize *= d.getScaleFactor();
                    if (fontSize < 20f) fontSize = 20f;
                    if (fontSize > 100f) fontSize = 100f;
                    paint.setTextSize(fontSize);
                    composingPaint.setTextSize(fontSize);
                    charW = paint.measureText("M");
                    if (old != fontSize) {
                        calcMaxScroll();
                        invalidate();
                    }
                    return true;
                }
            });

        tapDetector = new GestureDetector(getContext(),
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onLongPress(MotionEvent e) {
                    lastLongPress = System.currentTimeMillis();
                    longPressed = true;
                    startSelect(e.getX(), e.getY());
                }
            });

        ui.postDelayed(blink, 500);
    }

    private void loadBg() {
        try {
            Bitmap raw = BitmapFactory.decodeResource(
                getResources(), R.drawable.terminal_bg);
            if (raw == null) return;
            Matrix m = new Matrix();
            m.postRotate(90);
            bg = Bitmap.createBitmap(raw, 0, 0,
                raw.getWidth(), raw.getHeight(), m, true);
            if (bg != raw) raw.recycle();
            bgW = bg.getWidth();
            bgH = bg.getHeight();
        } catch (Exception ignored) {}
    }

    public void setInputListener(InputListener l) {
        inputListener = l;
    }

    public void setFontSize(float s) {
        fontSize = s;
        paint.setTextSize(s);
        composingPaint.setTextSize(s);
        charW = paint.measureText("M");
        calcMaxScroll();
        invalidate();
    }

    public void setOnTap(Runnable r) { onTap = r; }

    public void append(String text, int color) {
        if (text == null) return;
        String[] parts = text.split("\n", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i == parts.length - 1 && parts[i].isEmpty()) break;
            lines.add(new Line(parts[i], color));
        }
        if (lines.size() > 3000) {
            lines.subList(0, lines.size() - 3000).clear();
        }
        autoScroll = true;
        calcMaxScroll();
        invalidate();
    }

    public void replaceLastLine(String text, int color) {
        if (text == null) return;
        String[] parts = text.split("\n", -1);
        String first = parts[0];

        if (lines.isEmpty()) {
            lines.add(new Line(first, color));
        } else {
            Line last = lines.get(lines.size() - 1);
            last.text = first;
            last.color = color;
        }

        for (int i = 1; i < parts.length; i++) {
            if (i == parts.length - 1 && parts[i].isEmpty()) break;
            lines.add(new Line(parts[i], color));
        }

        if (lines.size() > 3000) {
            lines.subList(0, lines.size() - 3000).clear();
        }
        autoScroll = true;
        calcMaxScroll();
        invalidate();
    }

    public int lineCount() {
        return lines.size();
    }

    public void clear() {
        lines.clear();
        input = "";
        composing = "";
        selecting = false;
        selStart = selEnd = -1;
        scrollX = 0;
        scrollY = 0;
        autoScroll = true;
        calcMaxScroll();
        invalidate();
    }

    public void setInput(String s) {
        input = s == null ? "" : s;
        composing = "";
        autoScroll = true;
        calcMaxScroll();
        invalidate();
    }

    public String getInput() { return input; }

    public boolean isSelecting() { return selecting; }

    public void exitSelect() {
        selecting = false;
        selStart = selEnd = -1;
        invalidate();
    }

    public String getSelectedText() {
        if (!selecting || selStart < 0 || selEnd < 0) return "";
        int a = Math.min(selStart, selEnd);
        int b = Math.max(selStart, selEnd);
        if (a >= flatText.length()) return "";
        b = Math.min(b, flatText.length());
        if (a >= b) return "";
        return flatText.substring(a, b);
    }

    private void startSelect(float x, float y) {
        selecting = true;
        selStart = offsetFromXY(x, y);
        selEnd = selStart;
        updateDots();
        invalidate();
    }

    private void updateDots() {
        if (selStart < 0 || selEnd < 0) return;
        float[] p1 = xyFromOffset(Math.min(selStart, selEnd));
        float[] p2 = xyFromOffset(Math.max(selStart, selEnd));
        dot1X = p1[0]; dot1Y = p1[1];
        dot2X = p2[0]; dot2Y = p2[1];
    }

    private String getShown() {
        return input + composing;
    }

    private void calcMaxScroll() {
        buildLayout();
        float maxW = 0;
        for (Line l : lines) {
            float w = paint.measureText(l.text);
            if (w > maxW) maxW = w;
        }
        float inW = paint.measureText("> " + getShown());
        if (inW > maxW) maxW = inW;
        maxW += pad * 2;

        int vw = getWidth();
        int vh = getHeight();

        if (vw <= 0) maxScrollX = 0;
        else maxScrollX = (int) Math.max(0, maxW - vw);

        if (vh <= 0) maxScrollY = 0;
        else maxScrollY = (int) Math.max(0, contentH - vh);

        if (scrollX > maxScrollX) scrollX = maxScrollX;
        if (scrollX < 0) scrollX = 0;
        if (scrollY > maxScrollY) scrollY = maxScrollY;
        if (scrollY < 0) scrollY = 0;
    }

    private void buildLayout() {
        int n = lines.size() + 1;
        if (lineTops == null || lineTops.length < n) {
            lineTops = new float[n];
            lineStarts = new int[n];
        }
        float y = pad + fontSize;
        float lh = fontSize * lineGap;

        StringBuilder flat = new StringBuilder();

        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            lineTops[i] = y - fontSize;
            lineStarts[i] = flat.length();
            flat.append(l.text).append("\n");
            y += lh;
        }

        lineTops[lines.size()] = y - fontSize;
        lineStarts[lines.size()] = flat.length();
        flat.append("> ").append(getShown());
        contentH = y + fontSize + pad;

        flatText = flat.toString();
    }

    private int offsetFromXY(float x, float y) {
        float realX = x + scrollX;
        float realY = y + scrollY;
        if (lineTops == null) return 0;
        int idx = -1;
        float lh = fontSize * lineGap;
        for (int i = 0; i <= lines.size(); i++) {
            float top = lineTops[i];
            if (realY >= top && realY < top + lh) { idx = i; break; }
        }
        if (idx < 0) {
            if (realY < pad) idx = 0;
            else idx = lines.size();
        }

        String lineText;
        if (idx < lines.size()) lineText = lines.get(idx).text;
        else lineText = "> " + getShown();

        float relX = realX - pad;
        if (relX < 0) relX = 0;
        int charIdx = (int) (relX / Math.max(1f, charW));
        if (charIdx < 0) charIdx = 0;
        if (charIdx > lineText.length()) charIdx = lineText.length();

        return lineStarts[idx] + charIdx;
    }

    private float[] xyFromOffset(int offset) {
        if (lineTops == null || lineStarts == null) return new float[]{pad, pad};
        int idx = 0;
        for (int i = 0; i < lineStarts.length - 1; i++) {
            if (offset >= lineStarts[i] && offset < lineStarts[i + 1]) {
                idx = i;
                break;
            }
            if (offset >= lineStarts[lineStarts.length - 1]) {
                idx = lineStarts.length - 1;
            }
        }
        float top = lineTops[Math.min(idx, lineTops.length - 1)];
        int ci = offset - lineStarts[idx];
        float x = pad + ci * charW;
        float y = top + fontSize * 0.5f;
        return new float[]{x, y};
    }

    private void fireChange() {
        if (inputListener != null) inputListener.onInputChanged(input);
        autoScroll = true;
        calcMaxScroll();
        invalidate();
    }

    private void fireEnter(String text) {
        if (inputListener != null) inputListener.onEnter(text);
    }

    private void doEnter() {
        String done = input + composing;
        input = "";
        composing = "";
        fireChange();
        fireEnter(done);
    }

    private void doBackspace() {
        if (!composing.isEmpty()) {
            composing = composing.substring(0, composing.length() - 1);
            fireChange();
            return;
        }
        if (input.length() > 0) {
            input = input.substring(0, input.length() - 1);
            fireChange();
        }
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_SEND
            | EditorInfo.IME_FLAG_NO_FULLSCREEN;
        return new BaseInputConnection(this, true) {

            @Override
            public boolean commitText(CharSequence text, int newCursorPosition) {
                if (text == null) return true;
                String s = text.toString();
                if (s.isEmpty()) return true;

                composing = "";

                if (s.contains("\n")) {
                    String[] parts = s.split("\n", -1);
                    for (int i = 0; i < parts.length; i++) {
                        input += parts[i];
                        if (i < parts.length - 1) {
                            doEnter();
                        }
                    }
                    fireChange();
                    return true;
                }

                input += s;
                fireChange();
                return true;
            }

            @Override
            public boolean setComposingText(CharSequence text, int newCursorPosition) {
                if (text == null) return true;
                String s = text.toString();
                if (s.contains("\n")) {
                    return commitText(text, newCursorPosition);
                }
                composing = s;
                fireChange();
                return true;
            }

            @Override
            public boolean finishComposingText() {
                if (!composing.isEmpty()) {
                    input += composing;
                    composing = "";
                    fireChange();
                }
                return true;
            }

            @Override
            public boolean setComposingRegion(int start, int end) {
                return true;
            }

            @Override
            public boolean deleteSurroundingText(int before, int after) {
                if (!composing.isEmpty()) {
                    composing = "";
                    fireChange();
                    return true;
                }
                for (int i = 0; i < before; i++) {
                    if (input.length() > 0) {
                        input = input.substring(0, input.length() - 1);
                    }
                }
                fireChange();
                return true;
            }

            @Override
            public boolean sendKeyEvent(KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                        doBackspace();
                        return true;
                    }
                    if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        || event.getKeyCode() == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                        if (!composing.isEmpty()) {
                            input += composing;
                            composing = "";
                        }
                        doEnter();
                        return true;
                    }
                }
                return super.sendKeyEvent(event);
            }

            @Override
            public boolean performEditorAction(int actionCode) {
                if (!composing.isEmpty()) {
                    input += composing;
                    composing = "";
                }
                doEnter();
                return true;
            }
        };
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        calcMaxScroll();
    }

    @Override
    protected void onDraw(Canvas cv) {
        super.onDraw(cv);

        buildLayout();

        if (autoScroll) {
            scrollY = maxScrollY;
            autoScroll = false;
        }

        int viewW = getWidth();
        int viewH = getHeight();

        int save = cv.save();

        if (bg != null && bgW > 0 && bgH > 0) {
            cv.drawBitmap(bg, null, new Rect(0, 0, viewW, viewH), bgPaint);
        }

        cv.translate(-scrollX, -scrollY);

        float lh = fontSize * lineGap;

        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            paint.setColor(l.color);
            float top = lineTops[i];
            cv.drawText(l.text, pad, top + fontSize * 0.9f, paint);
        }

        paint.setColor(COLOR_USER);
        float inputTop = lineTops[lines.size()];
        String prefix = "> ";
        cv.drawText(prefix, pad, inputTop + fontSize * 0.9f, paint);

        float cursorBaseX = pad + paint.measureText(prefix);

        if (!input.isEmpty()) {
            paint.setColor(COLOR_NORMAL);
            cv.drawText(input, cursorBaseX, inputTop + fontSize * 0.9f, paint);
            cursorBaseX += paint.measureText(input);
        }

        if (!composing.isEmpty()) {
            composingPaint.setColor(0xFF888888);
            cv.drawText(composing, cursorBaseX, inputTop + fontSize * 0.9f, composingPaint);
            cursorBaseX += paint.measureText(composing);
        }

        if (cursorOn && !selecting) {
            cv.drawRect(cursorBaseX, inputTop + fontSize * 0.15f,
                cursorBaseX + charW, inputTop + fontSize * 0.95f, cursorPaint);
        }

        if (selecting && selStart >= 0 && selEnd >= 0) {
            drawSelection(cv, lh);
        }

        cv.restore();

        if (selecting) {
            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
            dot.setColor(0xFFD87093);
            dot.setStyle(Paint.Style.FILL);
            cv.drawCircle(dot1X - scrollX, dot1Y - scrollY, 14, dot);
            cv.drawCircle(dot2X - scrollX, dot2Y - scrollY, 14, dot);
            dot.setColor(Color.WHITE);
            dot.setStyle(Paint.Style.STROKE);
            dot.setStrokeWidth(3);
            cv.drawCircle(dot1X - scrollX, dot1Y - scrollY, 14, dot);
            cv.drawCircle(dot2X - scrollX, dot2Y - scrollY, 14, dot);
        }
    }

    private void drawSelection(Canvas cv, float lh) {
        int a = Math.min(selStart, selEnd);
        int b = Math.max(selStart, selEnd);
        if (a == b) return;

        for (int i = 0; i <= lines.size(); i++) {
            int s = lineStarts[i];
            int e = (i < lines.size()) ? s + lines.get(i).text.length()
                : s + ("> " + getShown()).length();
            if (b <= s || a > e) continue;

            float top = lineTops[i] + fontSize * 0.1f;
            float leftX;
            float rightX;
            if (i < lines.size()) {
                int ls = Math.max(a, s) - s;
                int le = Math.min(b, e) - s;
                leftX = pad + ls * charW;
                rightX = pad + le * charW;
                if (rightX < leftX) rightX = leftX + charW;
            } else {
                int ls = Math.max(a, s) - s;
                int le = Math.min(b, e) - s;
                leftX = pad + ls * charW;
                rightX = pad + le * charW;
                if (rightX < leftX) rightX = leftX + charW;
            }
            cv.drawRect(leftX, top, rightX, top + lh * 0.9f, hlPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int pointers = e.getPointerCount();

        if (pointers >= 2) {
            multiTouch = true;
            scaleDetector.onTouchEvent(e);
            return true;
        }

        if (multiTouch) {
            if (e.getActionMasked() == MotionEvent.ACTION_UP
                || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                multiTouch = false;
            }
            return true;
        }

        if (selecting) {
            handleSelectTouch(e);
            return true;
        }

        tapDetector.onTouchEvent(e);

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                downTime = System.currentTimeMillis();
                dragging = false;
                movedFar = false;
                isHorizontal = false;
                longPressed = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = e.getX() - downX;
                float dy = e.getY() - downY;

                if (!dragging) {
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                        dragging = true;
                        isHorizontal = Math.abs(dx) > Math.abs(dy);
                        downX = e.getX();
                        downY = e.getY();
                        return true;
                    }
                    return true;
                }

                if (isHorizontal) {
                    scrollX -= (int) dx;
                    if (scrollX < 0) scrollX = 0;
                    if (scrollX > maxScrollX) scrollX = maxScrollX;
                } else {
                    scrollY -= (int) dy;
                    if (scrollY < 0) scrollY = 0;
                    if (scrollY > maxScrollY) scrollY = maxScrollY;
                }
                if (Math.abs(dx) > 40 || Math.abs(dy) > 40) movedFar = true;
                downX = e.getX();
                downY = e.getY();
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                long dt = System.currentTimeMillis() - downTime;
                if (!dragging && !movedFar && !longPressed && !selecting
                    && dt < 400 && onTap != null) {
                    onTap.run();
                    return true;
                }
                longPressed = false;
                return true;
        }
        return super.onTouchEvent(e);
    }

    private void handleSelectTouch(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                float x = e.getX() + scrollX;
                float y = e.getY() + scrollY;
                float d1 = dist(x, y, dot1X, dot1Y);
                float d2 = dist(x, y, dot2X, dot2Y);
                if (d1 < 40 && d1 <= d2) draggingDot = 1;
                else if (d2 < 40) draggingDot = 2;
                else draggingDot = 0;
                return;

            case MotionEvent.ACTION_MOVE:
                if (draggingDot == 1) {
                    selStart = offsetFromXY(e.getX(), e.getY());
                } else if (draggingDot == 2) {
                    selEnd = offsetFromXY(e.getX(), e.getY());
                }
                updateDots();
                invalidate();
                return;

            case MotionEvent.ACTION_UP:
                draggingDot = 0;
                return;
        }
    }

    private float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    protected void onDetachedFromWindow() {
        ui.removeCallbacks(blink);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ui.removeCallbacks(blink);
        ui.postDelayed(blink, 500);
    }
}