package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

public class AlarmClockActivity extends Activity {

    private TextView tvTime;
    private EditText etLabel;
    private TextView tvRingPath;
    private int hour = 7;
    private int min = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_alarm_clock);

        tvTime = findViewById(R.id.tv_ac_time);
        etLabel = findViewById(R.id.et_ac_label);
        tvRingPath = findViewById(R.id.tv_ac_ring_path);

        etLabel.setText("起床");

        updateTimeText();
        refreshRingPath();

        tvTime.setOnClickListener(v -> pickTime());
        findViewById(R.id.btn_ac_set).setOnClickListener(v -> setAlarm());
        findViewById(R.id.btn_ac_pick_ring).setOnClickListener(v -> pickRing());
        findViewById(R.id.btn_ac_reset_ring).setOnClickListener(v -> {
            SharedPreferences.Editor e =
                getSharedPreferences(AlarmRingActivity.SP_NAME, MODE_PRIVATE).edit();
            e.remove(AlarmRingActivity.KEY_RING_URI);
            e.apply();
            refreshRingPath();
            Toast.makeText(this, "已恢复系统默认铃声", Toast.LENGTH_SHORT).show();
        });
    }

    private void pickTime() {
        new TimePickerDialog(this, (TimePicker view, int h, int m) -> {
            hour = h;
            min = m;
            updateTimeText();
        }, hour, min, true).show();
    }

    private void updateTimeText() {
        tvTime.setText(String.format("%02d:%02d", hour, min));
    }

    private void setAlarm() {
        String label = etLabel.getText().toString().trim();
        if (label.isEmpty()) label = "闹钟";

        Calendar c = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, min);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (target.getTimeInMillis() <= c.getTimeInMillis()) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }

        long triggerAt = target.getTimeInMillis();
        AlarmScheduler.schedule(this, triggerAt, label);

        String show = String.format("已设定 %02d:%02d 的闹钟\n\n到点会响铃+通知，后台也生效。",
            hour, min);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle("闹钟")
            .setMessage(show)
            .setPositiveButton("知道了", null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                builder.setNeutralButton("授权精确闹钟", (d, w) -> {
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                });
            }
        }

        builder.show();
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
        startActivityForResult(i, 300);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 300 && res == RESULT_OK && data != null) {
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