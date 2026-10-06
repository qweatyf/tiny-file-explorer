package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmRingActivity extends Activity {

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
        if (label == null) label = "时间到";

        TextView tvLabel = findViewById(R.id.tv_ring_label);
        tvLabel.setText(label);

        findViewById(R.id.btn_ring_stop).setOnClickListener(v -> {
            stopAll();
            finish();
        });

        startRing();
        startVibrate();
        ui.post(clockTick);
    }

    private void startRing() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        String custom = sp.getString(KEY_RING_URI, null);

        if (custom != null) {
            try {
                player = new MediaPlayer();
                player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
                player.setDataSource(this, Uri.parse(custom));
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
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
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
        try {
            if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
        } catch (Exception ignored) {}
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
            }
        } catch (Exception ignored) {}
        try {
            if (vibrator != null) vibrator.cancel();
        } catch (Exception ignored) {}
        ringtone = null;
        player = null;
    }

    @Override
    protected void onDestroy() {
        stopAll();
        super.onDestroy();
    }
}