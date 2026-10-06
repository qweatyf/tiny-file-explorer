package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CountdownTimerActivity extends Activity {

    private EditText etHour;
    private EditText etMin;
    private EditText etSec;
    private TextView tvRingPath;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_countdown_timer);

        etHour = findViewById(R.id.et_cdt_hour);
        etMin = findViewById(R.id.et_cdt_min);
        etSec = findViewById(R.id.et_cdt_sec);
        tvRingPath = findViewById(R.id.tv_cdt_ring_path);

        etHour.setText("0");
        etMin.setText("5");
        etSec.setText("0");

        refreshRingPath();

        findViewById(R.id.btn_cdt_start).setOnClickListener(v -> start());
        findViewById(R.id.btn_cdt_pick_ring).setOnClickListener(v -> pickRing());
        findViewById(R.id.btn_cdt_reset_ring).setOnClickListener(v -> {
            SharedPreferences.Editor e =
                getSharedPreferences(AlarmRingActivity.SP_NAME, MODE_PRIVATE).edit();
            e.remove(AlarmRingActivity.KEY_RING_URI);
            e.apply();
            refreshRingPath();
            Toast.makeText(this, "已恢复系统默认铃声", Toast.LENGTH_SHORT).show();
        });
    }

    private void start() {
        int h = parse(etHour.getText().toString(), 0);
        int m = parse(etMin.getText().toString(), 0);
        int s = parse(etSec.getText().toString(), 0);
        long total = (h * 3600L + m * 60L + s) * 1000L;

        if (total <= 0) {
            Toast.makeText(this, "时间要大于 0", Toast.LENGTH_SHORT).show();
            return;
        }

        long triggerAt = System.currentTimeMillis() + total;
        AlarmScheduler.schedule(this, triggerAt, "倒计时结束");

        new AlertDialog.Builder(this)
            .setTitle("已设定")
            .setMessage("倒计时 " + h + " 时 " + m + " 分 " + s + " 秒\n\n"
                + "到点会响铃+通知，后台也生效。\n可以退出 App。")
            .setPositiveButton("知道了", null)
            .show();
    }

    private int parse(String s, int def) {
        try {
            int v = Integer.parseInt(s.trim());
            if (v < 0) return 0;
            return v;
        } catch (Exception e) {
            return def;
        }
    }

    private void pickRing() {
        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
            RingtoneManager.TYPE_ALARM);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择闹铃音");
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);

        String cur = getSharedPreferences(AlarmRingActivity.SP_NAME, MODE_PRIVATE)
            .getString(AlarmRingActivity.KEY_RING_URI, null);
        if (cur != null) {
            i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(cur));
        }
        startActivityForResult(i, 200);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 200 && res == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            SharedPreferences.Editor e =
                getSharedPreferences(AlarmRingActivity.SP_NAME, MODE_PRIVATE).edit();
            if (uri == null) {
                e.remove(AlarmRingActivity.KEY_RING_URI);
            } else {
                e.putString(AlarmRingActivity.KEY_RING_URI, uri.toString());
            }
            e.apply();
            refreshRingPath();
        }
    }

    private void refreshRingPath() {
        String cur = getSharedPreferences(AlarmRingActivity.SP_NAME, MODE_PRIVATE)
            .getString(AlarmRingActivity.KEY_RING_URI, null);
        if (cur == null) {
            tvRingPath.setText("铃声: 系统默认闹钟音");
        } else {
            tvRingPath.setText("铃声: " + cur);
        }
    }
}