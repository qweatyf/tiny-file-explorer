package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MineGameActivity extends Activity {

    private MineView mineView;
    private TextView tvMineCount;
    private TextView tvTimer;
    private Button btnFlagMode;

    private MineGame game;
    private boolean flagMode = false;

    private int lastW = 9;
    private int lastH = 9;
    private int lastM = 10;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable timerTask = new Runnable() {
        @Override
        public void run() {
            updateTimer();
            if (game != null && game.isTiming()) {
                ui.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_mine_game);

        mineView = findViewById(R.id.mine_view);
        tvMineCount = findViewById(R.id.tv_mine_count);
        tvTimer = findViewById(R.id.tv_timer);
        btnFlagMode = findViewById(R.id.btn_flag_mode);

        mineView.setListener(new MineView.Listener() {
            @Override
            public void onFirstOpen() {
                startTimerRefresh();
            }

            @Override
            public void onChanged() {
                updateCounts();
                updateTimer();
            }

            @Override
            public void onGameEnd(boolean win) {
                stopTimerRefresh();
                updateCounts();
                updateTimer();
                Toast.makeText(MineGameActivity.this,
                    win ? "赢啦！" : "踩雷了", Toast.LENGTH_SHORT).show();
            }
        });

        btnFlagMode.setOnClickListener(v -> {
            flagMode = !flagMode;
            mineView.setFlagMode(flagMode);
            btnFlagMode.setText(flagMode ? "插旗：开" : "插旗：关");
        });

        findViewById(R.id.btn_restart).setOnClickListener(v -> {
            startGame(lastW, lastH, lastM);
        });

        findViewById(R.id.btn_new).setOnClickListener(v -> {
            showCustomDialog();
        });

        showCustomDialog();
    }

    private void startGame(int w, int h, int mines) {
        if (w < 5) w = 5;
        if (h < 5) h = 5;
        if (w > 30) w = 30;
        if (h > 30) h = 30;

        int max = w * h - 9;
        if (mines < 1) mines = 1;
        if (mines > max) mines = max;

        lastW = w;
        lastH = h;
        lastM = mines;

        game = new MineGame(w, h, mines);
        mineView.setGame(game);
        updateCounts();
        updateTimer();
        stopTimerRefresh();
    }

    private void startTimerRefresh() {
        ui.removeCallbacks(timerTask);
        ui.postDelayed(timerTask, 500);
    }

    private void stopTimerRefresh() {
        ui.removeCallbacks(timerTask);
    }

    private void updateTimer() {
        if (game == null) {
            tvTimer.setText("000");
            return;
        }
        long sec = game.getElapsedMs() / 1000;
        if (sec > 999) sec = 999;
        tvTimer.setText(String.format("%03d", sec));
    }

    private void updateCounts() {
        if (game == null) return;
        int remain = game.getRemainMines();
        if (remain < 0) remain = 0;
        if (remain > 999) remain = 999;
        tvMineCount.setText(String.format("%03d", remain));
    }

    private void showCustomDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_mine_custom, null);

        EditText etW = v.findViewById(R.id.et_width);
        EditText etH = v.findViewById(R.id.et_height);
        EditText etM = v.findViewById(R.id.et_mines);

        etW.setText(String.valueOf(lastW));
        etH.setText(String.valueOf(lastH));
        etM.setText(String.valueOf(lastM));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(v)
            .setCancelable(false)
            .create();

        v.findViewById(R.id.btn_cancel).setOnClickListener(x -> {
            dialog.dismiss();
            if (game == null) finish();
        });
        v.findViewById(R.id.btn_start).setOnClickListener(x -> {
            int w = parse(etW.getText().toString(), 9);
            int h = parse(etH.getText().toString(), 9);
            int m = parse(etM.getText().toString(), 10);

            if (w < 5 || w > 30 || h < 5 || h > 30) {
                Toast.makeText(this, "宽高要在 5~30 之间", Toast.LENGTH_SHORT).show();
                return;
            }
            int max = w * h - 9;
            if (m < 1 || m > max) {
                Toast.makeText(this, "雷数要在 1~" + max + " 之间", Toast.LENGTH_SHORT).show();
                return;
            }

            startGame(w, h, m);
            dialog.dismiss();
        });

        dialog.show();
    }

    private int parse(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (game != null && game.isTiming()) {
            startTimerRefresh();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimerRefresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimerRefresh();
        if (mineView != null) mineView.stopTick();
    }
}