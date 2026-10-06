package com.example.myempty.activity2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class MineView extends View {

    public interface Listener {
        void onFirstOpen();
        void onChanged();
        void onGameEnd(boolean win);
    }

    private MineGame game;
    private Listener listener;

    private float cellSize = 80f;
    private float pad = 0f;
    private boolean flagMode = false;

    private Paint bgPaint;
    private Paint closedPaint;
    private Paint closedTopPaint;
    private Paint openedPaint;
    private Paint minePaint;
    private Paint textPaint;
    private Paint explodePaint;
    private Paint wrongPaint;

    private final Handler ui = new Handler(Looper.getMainLooper());

    private float touchStartX, touchStartY;
    private boolean moved = false;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (listener != null) listener.onChanged();
            if (game != null && game.isTiming()) {
                ui.postDelayed(this, 500);
            }
        }
    };

    public MineView(Context c) { super(c); init(); }
    public MineView(Context c, AttributeSet a) { super(c, a); init(); }
    public MineView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(0xFFF7E6EE);

        closedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        closedPaint.setColor(0xFFF5C6D8);

        closedTopPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        closedTopPaint.setColor(0xFFFBE0EA);

        openedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        openedPaint.setColor(0xFFFFF5F8);

        minePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        minePaint.setColor(0xFF333333);

        explodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        explodePaint.setColor(0xFFB71C1C);

        wrongPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wrongPaint.setColor(0xFFFF0000);
        wrongPaint.setStyle(Paint.Style.STROKE);
        wrongPaint.setStrokeWidth(4f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);

        setFocusable(true);
    }

    public void setGame(MineGame g) {
        game = g;
        ui.removeCallbacks(tick);
        if (g != null && g.isTiming()) ui.postDelayed(tick, 500);
        requestLayout();
        invalidate();
    }

    public MineGame getGame() { return game; }

    public void setListener(Listener l) { listener = l; }

    public void setFlagMode(boolean on) { flagMode = on; }

    public boolean isFlagMode() { return flagMode; }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        if (game == null) {
            super.onMeasure(wSpec, hSpec);
            return;
        }

        int wMode = MeasureSpec.getMode(wSpec);
        int hMode = MeasureSpec.getMode(hSpec);
        int wSize = MeasureSpec.getSize(wSpec);
        int hSize = MeasureSpec.getSize(hSpec);

        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;

        if (wMode == MeasureSpec.UNSPECIFIED) {
            wSize = screenW;
        }
        if (hMode == MeasureSpec.UNSPECIFIED) {
            hSize = (int) (screenH * 1.2f);
        }

        float ideal = 90f;
        float minSize = 16f;
        float maxSize = 120f;

        float csW = (wSize - pad * 2) / game.width;
        float csH = (hSize - pad * 2) / game.height;
        float cs = Math.min(csW, csH);
        if (cs > ideal) cs = ideal;
        if (cs > maxSize) cs = maxSize;
        if (cs < minSize) cs = minSize;

        cellSize = cs;

        int needW = (int) (cs * game.width + pad * 2);
        int needH = (int) (cs * game.height + pad * 2);

        if (wMode == MeasureSpec.EXACTLY) needW = wSize;
        if (hMode == MeasureSpec.EXACTLY) needH = hSize;

        setMeasuredDimension(needW, needH);
    }

    @Override
    protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        if (game == null) return;

        int w = getWidth();
        int h = getHeight();
        cv.drawColor(bgPaint.getColor());

        float totalW = cellSize * game.width;
        float totalH = cellSize * game.height;
        float offX = (w - totalW) / 2f;
        float offY = (h - totalH) / 2f;

        for (int y = 0; y < game.height; y++) {
            for (int x = 0; x < game.width; x++) {
                MineCell c = game.get(x, y);
                if (c == null) continue;
                float left = offX + x * cellSize;
                float top = offY + y * cellSize;
                drawCell(cv, c, left, top, cellSize);
            }
        }
    }

    private void drawCell(Canvas cv, MineCell c, float left, float top, float size) {
        float inset = size * 0.04f;
        RectF box = new RectF(left + inset, top + inset,
            left + size - inset, top + size - inset);
        float r = size * 0.14f;

        if (c.isOpened()) {
            if (c.exploded) {
                cv.drawRoundRect(box, r, r, explodePaint);
            } else {
                cv.drawRoundRect(box, r, r, openedPaint);
            }

            if (c.isMine) {
                drawMine(cv, left, top, size);
            } else if (c.value > 0) {
                textPaint.setColor(numColor(c.value));
                textPaint.setTextSize(size * 0.6f);
                float cx = left + size / 2f;
                float cy = top + size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
                cv.drawText(String.valueOf(c.value), cx, cy, textPaint);
            }

            if (c.wrongFlag) {
                float cx = left + size / 2f;
                float cy = top + size / 2f;
                float rr = size * 0.35f;
                cv.drawCircle(cx, cy, rr, wrongPaint);
                cv.drawLine(cx - rr * 0.7f, cy - rr * 0.7f,
                    cx + rr * 0.7f, cy + rr * 0.7f, wrongPaint);
                cv.drawLine(cx + rr * 0.7f, cy - rr * 0.7f,
                    cx - rr * 0.7f, cy + rr * 0.7f, wrongPaint);
            }
        } else {
            cv.drawRoundRect(box, r, r, closedPaint);

            RectF shine = new RectF(box.left + 2, box.top + 2,
                box.right - 2, box.top + size * 0.35f);
            cv.drawRoundRect(shine, r, r, closedTopPaint);

            if (c.state == MineCell.STATE_FLAG) {
                drawFlag(cv, left, top, size);
            } else if (c.state == MineCell.STATE_QUESTION) {
                textPaint.setColor(0xFF666666);
                textPaint.setTextSize(size * 0.6f);
                float cx = left + size / 2f;
                float cy = top + size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
                cv.drawText("?", cx, cy, textPaint);
            }
        }
    }

    private void drawMine(Canvas cv, float left, float top, float size) {
        float cx = left + size / 2f;
        float cy = top + size / 2f;
        float r = size * 0.22f;

        cv.drawCircle(cx, cy, r, minePaint);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF333333);
        p.setStrokeWidth(Math.max(2, size * 0.06f));
        p.setStrokeCap(Paint.Cap.ROUND);
        cv.drawLine(cx, cy - r * 1.6f, cx, cy + r * 1.6f, p);
        cv.drawLine(cx - r * 1.6f, cy, cx + r * 1.6f, cy, p);
        cv.drawLine(cx - r * 1.1f, cy - r * 1.1f, cx + r * 1.1f, cy + r * 1.1f, p);
        cv.drawLine(cx + r * 1.1f, cy - r * 1.1f, cx - r * 1.1f, cy + r * 1.1f, p);

        Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
        white.setColor(0xFFFFFFFF);
        cv.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.25f, white);
    }

    private void drawFlag(Canvas cv, float left, float top, float size) {
        float cx = left + size / 2f;
        float cy = top + size / 2f;

        Paint pole = new Paint(Paint.ANTI_ALIAS_FLAG);
        pole.setColor(0xFF333333);
        pole.setStrokeWidth(Math.max(2, size * 0.06f));
        pole.setStrokeCap(Paint.Cap.ROUND);
        cv.drawLine(cx, cy - size * 0.28f, cx, cy + size * 0.28f, pole);

        android.graphics.Path flag = new android.graphics.Path();
        flag.moveTo(cx, cy - size * 0.28f);
        flag.lineTo(cx + size * 0.25f, cy - size * 0.12f);
        flag.lineTo(cx, cy + size * 0.02f);
        flag.close();

        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setColor(0xFFD87093);
        cv.drawPath(flag, fp);

        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setColor(0xFF333333);
        RectF b = new RectF(cx - size * 0.22f, cy + size * 0.24f,
            cx + size * 0.22f, cy + size * 0.32f);
        cv.drawRect(b, base);
    }

    private int numColor(int n) {
        switch (n) {
            case 1: return 0xFF1565C0;
            case 2: return 0xFF2E7D32;
            case 3: return 0xFFC62828;
            case 4: return 0xFF283593;
            case 5: return 0xFF6D4C41;
            case 6: return 0xFF00838F;
            case 7: return 0xFF212121;
            case 8: return 0xFF757575;
            default: return 0xFF000000;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (game == null) return false;

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = e.getX();
                touchStartY = e.getY();
                moved = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (Math.abs(e.getX() - touchStartX) > 20
                    || Math.abs(e.getY() - touchStartY) > 20) {
                    moved = true;
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (moved) return true;
                handleTap(e.getX(), e.getY());
                return true;
        }
        return super.onTouchEvent(e);
    }

    private void handleTap(float px, float py) {
        if (game == null || game.isGameOver()) return;

        int w = getWidth();
        int h = getHeight();
        float totalW = cellSize * game.width;
        float totalH = cellSize * game.height;
        float offX = (w - totalW) / 2f;
        float offY = (h - totalH) / 2f;

        int x = (int) ((px - offX) / cellSize);
        int y = (int) ((py - offY) / cellSize);
        if (x < 0 || x >= game.width || y < 0 || y >= game.height) return;

        if (flagMode) {
            game.toggleFlag(x, y);
            if (listener != null) listener.onChanged();
            invalidate();
            return;
        }

        MineCell c = game.get(x, y);
        if (c == null) return;
        if (c.isFlagged() || c.state == MineCell.STATE_QUESTION) return;

        boolean wasFirst = !game.isTiming() && game.getOpenedCount() == 0;

        int r = game.open(x, y);

        if (wasFirst && listener != null) {
            listener.onFirstOpen();
            ui.removeCallbacks(tick);
            ui.postDelayed(tick, 500);
        }

        if (r == MineGame.RESULT_WIN) {
            ui.removeCallbacks(tick);
            if (listener != null) {
                listener.onChanged();
                listener.onGameEnd(true);
            }
        } else if (r == MineGame.RESULT_LOSE) {
            ui.removeCallbacks(tick);
            if (listener != null) {
                listener.onChanged();
                listener.onGameEnd(false);
            }
        } else {
            if (listener != null) listener.onChanged();
        }

        invalidate();
    }

    public void stopTick() {
        ui.removeCallbacks(tick);
    }

    public void startTick() {
        ui.removeCallbacks(tick);
        ui.postDelayed(tick, 500);
    }
}