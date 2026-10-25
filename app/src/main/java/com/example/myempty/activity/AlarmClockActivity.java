package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AlarmClockActivity extends AppCompatActivity {

    private TextView tvTime;
    private EditText etLabel;
    private TextView tvMusic;
    private TextView tvType;
    private LinearLayout weekBox;
    private CheckBox[] weekChecks = new CheckBox[7];
    private ListView lvAlarms;

    private int hour = 7;
    private int min = 0;
    private int type = AlarmData.TYPE_DAILY;
    private String musicPath = "";
    private String musicName = "";

    private final List<AlarmData> alarms = new ArrayList<>();
    private AlarmAdapter adapter;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_alarm_clock);

        tvTime = findViewById(R.id.tv_ac_time);
        etLabel = findViewById(R.id.et_ac_label);
        tvMusic = findViewById(R.id.tv_ac_ring_path);
        tvType = findViewById(R.id.tv_ac_type);
        weekBox = findViewById(R.id.ll_ac_week);
        lvAlarms = findViewById(R.id.lv_ac_alarms);

        String[] weekIds = {"cb_w1", "cb_w2", "cb_w3", "cb_w4", "cb_w5", "cb_w6", "cb_w7"};
        for (int i = 0; i < 7; i++) {
            int id = getResources().getIdentifier(weekIds[i], "id", getPackageName());
            weekChecks[i] = findViewById(id);
        }

        adapter = new AlarmAdapter();
        lvAlarms.setAdapter(adapter);

        updateTimeText();
        updateTypeText();
        refreshMusicText();

        tvTime.setOnClickListener(v -> pickTime());
        tvType.setOnClickListener(v -> pickType());
        findViewById(R.id.btn_ac_pick_ring).setOnClickListener(v -> pickMusic());
        findViewById(R.id.btn_ac_reset_ring).setOnClickListener(v -> {
            musicPath = "";
            musicName = "";
            refreshMusicText();
        });
        findViewById(R.id.btn_ac_set).setOnClickListener(v -> setAlarm());

        lvAlarms.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmDelete(alarms.get(position));
            return true;
        });

        loadAlarms();
    }

    private void pickTime() {
        new TimePickerDialog(this, (TimePicker view, int h, int m) -> {
            hour = h;
            min = m;
            updateTimeText();
        }, hour, min, true).show();
    }

    private void pickType() {
        String[] ops = {
            getString(R.string.alarm_type_once),
            getString(R.string.alarm_type_daily),
            getString(R.string.alarm_type_weekly)
        };
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.alarm_type_title))
            .setItems(ops, (d, w) -> {
                type = w;
                updateTypeText();
            })
            .show();
    }

    private void updateTimeText() {
        tvTime.setText(String.format("%02d:%02d", hour, min));
    }

    private void updateTypeText() {
        if (type == AlarmData.TYPE_ONCE) {
            tvType.setText(getString(R.string.alarm_type_once));
            weekBox.setVisibility(View.GONE);
        } else if (type == AlarmData.TYPE_DAILY) {
            tvType.setText(getString(R.string.alarm_type_daily));
            weekBox.setVisibility(View.GONE);
        } else {
            tvType.setText(getString(R.string.alarm_type_weekly));
            weekBox.setVisibility(View.VISIBLE);
        }
    }

    private void refreshMusicText() {
        if (musicPath.isEmpty()) {
            tvMusic.setText(getString(R.string.alarm_default_ring));
        } else {
            tvMusic.setText(musicName);
        }
    }

    private void pickMusic() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("audio/*");
        startActivityForResult(i, 300);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 300 && res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            copyMusic(uri);
        }
    }

    private void copyMusic(final Uri uri) {
        new Thread(() -> {
            try {
                File dir = new File(getExternalFilesDir(null), "alarm_music");
                if (!dir.exists()) dir.mkdirs();

                String name = queryName(uri);
                if (name == null || name.isEmpty()) {
                    name = "music_" + System.currentTimeMillis() + ".mp3";
                }
                name = name.replace("/", "_");

                File out = new File(dir, System.currentTimeMillis() + "_" + name);

                InputStream is = getContentResolver().openInputStream(uri);
                FileOutputStream fos = new FileOutputStream(out);
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
                is.close();
                fos.close();

                final String path = out.getAbsolutePath();
                final String show = name;
                runOnUiThread(() -> {
                    musicPath = path;
                    musicName = show;
                    refreshMusicText();
                    Toast.makeText(this, getString(R.string.alarm_music_saved),
                        Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    getString(R.string.file_fail) + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String queryName(Uri uri) {
        try {
            android.database.Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String s = c.getString(idx);
                    c.close();
                    return s;
                }
                c.close();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void setAlarm() {
        String label = etLabel.getText().toString().trim();
        if (label.isEmpty()) {
            label = getString(R.string.alarm_default_ring);
        }

        AlarmData d = new AlarmData();
        d.hour = hour;
        d.minute = min;
        d.type = type;
        if (type == AlarmData.TYPE_WEEKLY) {
            boolean any = false;
            for (int i = 0; i < 7; i++) {
                d.days[i] = weekChecks[i].isChecked();
                if (d.days[i]) any = true;
            }
            if (!any) {
                Toast.makeText(this, getString(R.string.alarm_need_week),
                    Toast.LENGTH_SHORT).show();
                return;
            }
        }
        d.label = label;
        d.musicPath = musicPath;

        AlarmScheduler.add(this, d);

        String show = String.format(getString(R.string.alarm_set_fmt), hour, min);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_alarm_clock))
            .setMessage(show)
            .setPositiveButton(getString(R.string.lang_ok), null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager am =
                (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                builder.setNeutralButton(getString(R.string.alarm_grant_exact),
                    (d2, w) -> startActivity(new Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)));
            }
        }

        builder.show();

        etLabel.setText("");
        loadAlarms();
    }

    private void loadAlarms() {
        alarms.clear();
        alarms.addAll(AlarmScheduler.loadAll(this));
        adapter.notifyDataSetChanged();
    }

    private void confirmDelete(final AlarmData d) {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.alarm_delete_title))
            .setMessage(d.timeText() + "  " + d.label)
            .setPositiveButton(getString(R.string.file_del_ok), (dlg, w) -> {
                AlarmScheduler.remove(this, d.id);
                loadAlarms();
            })
            .setNegativeButton(getString(R.string.file_del_cancel), null)
            .show();
    }

    class AlarmAdapter extends BaseAdapter {
        @Override public int getCount() { return alarms.size(); }
        @Override public AlarmData getItem(int i) { return alarms.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(AlarmClockActivity.this)
                .inflate(R.layout.item_alarm, parent, false);
            AlarmData d = getItem(pos);

            TextView tvT = v.findViewById(R.id.tv_item_alarm_time);
            TextView tvL = v.findViewById(R.id.tv_item_alarm_label);
            TextView tvTy = v.findViewById(R.id.tv_item_alarm_type);

            tvT.setText(d.timeText());
            tvL.setText(d.label);

            String[] weekNames = {
                getString(R.string.week_mon),
                getString(R.string.week_tue),
                getString(R.string.week_wed),
                getString(R.string.week_thu),
                getString(R.string.week_fri),
                getString(R.string.week_sat),
                getString(R.string.week_sun)
            };
            tvTy.setText(d.typeText(weekNames));

            return v;
        }
    }
}