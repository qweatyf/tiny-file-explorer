package com.example.myempty.activity2;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CountdownTimerActivity extends AppCompatActivity {

    private EditText etName;
    private TextView tvDate;
    private ListView lv;
    private long pickedTime = 0;
    private final List<Item> items = new ArrayList<>();
    private Adapter adapter;

    private static final String SP_NAME = "countdown_timer_sp";

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_countdown);

        etName = findViewById(R.id.et_cd_name);
        tvDate = findViewById(R.id.tv_cd_date);
        lv = findViewById(R.id.lv_cd_list);

        adapter = new Adapter();
        lv.setAdapter(adapter);

        findViewById(R.id.btn_cd_pick_date).setOnClickListener(v -> pickDate());
        findViewById(R.id.btn_cd_add).setOnClickListener(v -> add());
        lv.setOnItemLongClickListener((p, v, pos, id) -> {
            items.remove(pos);
            saveData();
            adapter.notifyDataSetChanged();
            return true;
        });

        loadData();
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar t = Calendar.getInstance();
            t.set(y, m, d, 0, 0, 0);
            t.set(Calendar.MILLISECOND, 0);
            pickedTime = t.getTimeInMillis();
            tvDate.setText(y + "-" + (m + 1) + "-" + d);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void add() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty() || pickedTime == 0) {
            Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show();
            return;
        }
        Item it = new Item();
        it.id = System.currentTimeMillis();
        it.name = name;
        it.time = pickedTime;
        items.add(it);
        
        etName.setText("");
        tvDate.setText("");
        pickedTime = 0;
        
        saveData();
        loadData();
    }

    private void loadData() {
        items.clear();
        SharedPreferences sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String json = sp.getString("list", "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Item it = new Item();
                it.id = o.getLong("id");
                it.name = o.getString("name");
                it.time = o.getLong("time");
                items.add(it);
            }
        } catch (Exception ignored) {}

        java.util.Collections.sort(items, (a, b) -> Long.compare(a.time, b.time));
        adapter.notifyDataSetChanged();
    }

    private void saveData() {
        JSONArray arr = new JSONArray();
        for (Item it : items) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", it.id);
                o.put("name", it.name);
                o.put("time", it.time);
                arr.put(o);
            } catch (Exception ignored) {}
        }
        getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit()
            .putString("list", arr.toString()).apply();
    }

    class Adapter extends BaseAdapter {
        @Override public int getCount() { return items.size(); }
        @Override public Item getItem(int i) { return items.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(CountdownTimerActivity.this)
                .inflate(android.R.layout.simple_list_item_2, parent, false);
            TextView t1 = v.findViewById(android.R.id.text1);
            TextView t2 = v.findViewById(android.R.id.text2);
            Item it = getItem(pos);

            long diff = it.time - System.currentTimeMillis();
            long days = diff / (1000L * 60 * 60 * 24);

            t1.setText(it.name);
            t1.setTextColor(0xFF333333);
            t1.setTextSize(16);

            if (days > 0) t2.setText("+" + days);
            else if (days < 0) t2.setText("" + days);
            else t2.setText("0");
            t2.setTextColor(0xFFD87093);
            return v;
        }
    }

    static class Item {
        long id;
        String name;
        long time;
    }
}