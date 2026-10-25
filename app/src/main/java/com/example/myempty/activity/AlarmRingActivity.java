package com.example.myempty.activity2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmRingActivity extends AppCompatActivity {

    public static final String SP_NAME = "alarm_sp";
    public static final String KEY_RING_URI = "ring_uri";

    private Ringtone ringtone;
    private MediaPlayer player;
    private Vibrator vibrator;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            TextView tv = findViewById(R.id.tv_ring_time);
            if (tv != null) {
                tv.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(new Date()));
            }
            ui.postDelayed(this, 500);
        }
    };

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        setContentView(R.layout.activity_alarm_ring);

        String label = getIntent().getStringExtra(AlarmScheduler.EXTRA_LABEL);
        if (label == null) label = getString(R.string.alarm_default_label);

        String music = getIntent().getStringExtra(AlarmScheduler.EXTRA_MUSIC);

        TextView tvLabel = findViewById(R.id.tv_ring_label);
        tvLabel.setText(label);

        findViewById(R.id.btn_ring_stop).setOnClickListener(v -> {
            stopAll();
            finish();
        });

        startRing(music);
        startVibrate();
        ui.post(clockTick);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL);
        if (label == null) label = getString(R.string.alarm_default_label);
        String music = intent.getStringExtra(AlarmScheduler.EXTRA_MUSIC);

        TextView tvLabel = findViewById(R.id.tv_ring_label);
        if (tvLabel != null) tvLabel.setText(label);

        stopAll();
        startRing(music);
        startVibrate();
        ui.removeCallbacks(clockTick);
        ui.post(clockTick);
    }

    private void startRing(String customPath) {
        if (customPath != null && !customPath.isEmpty()) {
            try {
                player = new MediaPlayer();
                player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
                
                if (customPath.startsWith("/")) {
                    player.setDataSource(customPath);
                } else {
                    player.setDataSource(this, Uri.parse(customPath));
                }
                
                player.setLooping(true);
                player.prepare();
                player.start();
                return;
            } catch (Exception ignored) {
                if (player != null) {
                    try { player.release(); } catch (Exception ignored2) {}
                    player = null;
                }
            }
        }

        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        String fallback = sp.getString(KEY_RING_URI, null);
        if (fallback != null) {
            try {
                player = new MediaPlayer();
                player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
                if (fallback.startsWith("/")) {
                    player.setDataSource(fallback);
                } else {
                    player.setDataSource(this, Uri.parse(fallback));
                }
                player.setLooping(true);
                player.prepare();
                player.start();
                return;
            } catch (Exception ignored) {
                if (player != null) {
                    try { player.release(); } catch (Exception ignored2) {}
                    player = null;
                }
            }
        }

        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (alarmUri != null) {
            ringtone = RingtoneManager.getRingtone(this, alarmUri);
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                }
                ringtone.play();
            }
        }
    }

    private void startVibrate() {
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 800, 600, 800, 600};
                vibrator.vibrate(pattern, 0);
            }
        } catch (Exception ignored) {}
    }

    private void stopAll() {
        ui.removeCallbacks(clockTick);
        try { if (ringtone != null && ringtone.isPlaying()) ringtone.stop(); } catch (Exception ignored) {}
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
            }
        } catch (Exception ignored) {}
        try { if (vibrator != null) vibrator.cancel(); } catch (Exception ignored) {}
        ringtone = null;
        player = null;
    }

    @Override
    protected void onDestroy() {
        stopAll();
        super.onDestroy();
    }
}