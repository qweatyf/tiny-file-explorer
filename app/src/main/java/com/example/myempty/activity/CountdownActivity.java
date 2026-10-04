package com.example.myempty.activity2;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CountdownActivity extends Activity {

    private EditText etName;
    private TextView tvDate;
    private ListView lv;
    private DB db;
    private long pickedTime = 0;
    private final List<Item> items = new ArrayList<>();
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_countdown);

        etName = findViewById(R.id.et_cd_name);
        tvDate = findViewById(R.id.tv_cd_date);
        lv = findViewById(R.id.lv_cd_list);

        db = new DB();

        adapter = new Adapter();
        lv.setAdapter(adapter);

        findViewById(R.id.btn_cd_pick_date).setOnClickListener(v -> pickDate());
        findViewById(R.id.btn_cd_add).setOnClickListener(v -> add());
        lv.setOnItemLongClickListener((p, v, pos, id) -> {
            delete(items.get(pos));
            return true;
        });

        load();
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar t = Calendar.getInstance();
            t.set(y, m, d, 0, 0, 0);
            t.set(Calendar.MILLISECOND, 0);
            pickedTime = t.getTimeInMillis();
            tvDate.setText(y + "-" + (m + 1) + "-" + d);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void add() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty() || pickedTime == 0) {
            Toast.makeText(this, "名字和日期都要", Toast.LENGTH_SHORT).show();
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("time", pickedTime);
        db.getWritableDatabase().insert("cd", null, cv);
        etName.setText("");
        tvDate.setText("未选日期");
        pickedTime = 0;
        load();
    }

    private void delete(Item it) {
        db.getWritableDatabase().delete("cd", "id=?",
            new String[]{String.valueOf(it.id)});
        load();
    }

    private void load() {
        items.clear();
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT id,name,time FROM cd ORDER BY time ASC", null);
        while (c.moveToNext()) {
            Item it = new Item();
            it.id = c.getLong(0);
            it.name = c.getString(1);
            it.time = c.getLong(2);
            items.add(it);
        }
        c.close();
        adapter.notifyDataSetChanged();
    }

    class Adapter extends BaseAdapter {
        @Override public int getCount() { return items.size(); }
        @Override public Item getItem(int i) { return items.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(CountdownActivity.this)
                .inflate(android.R.layout.simple_list_item_2, parent, false);
            TextView t1 = v.findViewById(android.R.id.text1);
            TextView t2 = v.findViewById(android.R.id.text2);
            Item it = getItem(pos);

            long diff = it.time - System.currentTimeMillis();
            long days = diff / (1000L * 60 * 60 * 24);

            t1.setText(it.name);
            t1.setTextColor(0xFF333333);
            t1.setTextSize(16);

            if (days > 0) t2.setText("还有 " + days + " 天");
            else if (days < 0) t2.setText("已经过了 " + (-days) + " 天");
            else t2.setText("就是今天");
            t2.setTextColor(0xFFD87093);
            return v;
        }
    }

    static class Item {
        long id;
        String name;
        long time;
    }

    class DB extends SQLiteOpenHelper {
        DB() {
            super(CountdownActivity.this, "countdown.db", null, 1);
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE cd ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT,"
                + "time INTEGER)");
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int o, int n) {
            db.execSQL("DROP TABLE IF EXISTS cd");
            onCreate(db);
        }
    }
}