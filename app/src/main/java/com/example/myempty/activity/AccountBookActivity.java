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

public class AccountBookActivity extends Activity {

    private EditText etAmount;
    private EditText etNote;
    private TextView tvDate;
    private TextView tvSummary;
    private ListView lv;
    private DB db;
    private long pickedTime;
    private String pickedDateStr;
    private final List<Item> items = new ArrayList<>();
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_account_book);

        etAmount = findViewById(R.id.et_ab_amount);
        etNote = findViewById(R.id.et_ab_note);
        tvDate = findViewById(R.id.tv_ab_date);
        tvSummary = findViewById(R.id.tv_ab_summary);
        lv = findViewById(R.id.lv_ab_list);

        db = new DB();
        adapter = new Adapter();
        lv.setAdapter(adapter);

        setToday();

        findViewById(R.id.btn_ab_pick_date).setOnClickListener(v -> pickDate());
        findViewById(R.id.btn_ab_add).setOnClickListener(v -> add());
        findViewById(R.id.btn_ab_income).setOnClickListener(v -> addQuick(1));
        lv.setOnItemLongClickListener((p, v, pos, id) -> {
            delete(items.get(pos));
            return true;
        });

        load();
    }

    private void setToday() {
        Calendar c = Calendar.getInstance();
        pickedTime = c.getTimeInMillis();
        pickedDateStr = c.get(Calendar.YEAR) + "-"
            + (c.get(Calendar.MONTH) + 1) + "-"
            + c.get(Calendar.DAY_OF_MONTH);
        tvDate.setText(pickedDateStr);
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar t = Calendar.getInstance();
            t.set(y, m, d, 0, 0, 0);
            t.set(Calendar.MILLISECOND, 0);
            pickedTime = t.getTimeInMillis();
            pickedDateStr = y + "-" + (m + 1) + "-" + d;
            tvDate.setText(pickedDateStr);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void add() {
        addQuick(0);
    }

    private void addQuick(int type) {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "输金额", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount;
        try { amount = Double.parseDouble(amountStr); }
        catch (Exception e) {
            Toast.makeText(this, "金额不对", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount <= 0) {
            Toast.makeText(this, "金额要正数", Toast.LENGTH_SHORT).show();
            return;
        }

        String note = etNote.getText().toString().trim();
        if (note.isEmpty()) note = type == 1 ? "收入" : "支出";

        ContentValues cv = new ContentValues();
        cv.put("amount", amount);
        cv.put("note", note);
        cv.put("time", pickedTime);
        cv.put("type", type);
        db.getWritableDatabase().insert("ab", null, cv);

        etAmount.setText("");
        etNote.setText("");
        load();
    }

    private void delete(Item it) {
        db.getWritableDatabase().delete("ab", "id=?",
            new String[]{String.valueOf(it.id)});
        load();
    }

    private void load() {
        items.clear();
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT id,amount,note,time,type FROM ab ORDER BY time DESC, id DESC", null);
        double income = 0, expense = 0;
        while (c.moveToNext()) {
            Item it = new Item();
            it.id = c.getLong(0);
            it.amount = c.getDouble(1);
            it.note = c.getString(2);
            it.time = c.getLong(3);
            it.type = c.getInt(4);
            if (it.type == 1) income += it.amount;
            else expense += it.amount;
            items.add(it);
        }
        c.close();
        adapter.notifyDataSetChanged();
        tvSummary.setText("收入 " + fmt(income) + "  |  支出 " + fmt(expense)
            + "  |  结余 " + fmt(income - expense));
    }

    private String fmt(double v) {
        return String.format("%.2f", v);
    }

    class Adapter extends BaseAdapter {
        @Override public int getCount() { return items.size(); }
        @Override public Item getItem(int i) { return items.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(AccountBookActivity.this)
                .inflate(android.R.layout.simple_list_item_2, parent, false);
            TextView t1 = v.findViewById(android.R.id.text1);
            TextView t2 = v.findViewById(android.R.id.text2);
            Item it = getItem(pos);

            String sign = it.type == 1 ? "+" : "-";
            t1.setText(sign + fmt(it.amount) + "  " + it.note);
            t1.setTextColor(it.type == 1 ? 0xFF1B5E20 : 0xFFB71C1C);
            t1.setTextSize(16);

            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(it.time);
            String date = c.get(Calendar.YEAR) + "-"
                + (c.get(Calendar.MONTH) + 1) + "-"
                + c.get(Calendar.DAY_OF_MONTH);
            t2.setText(date);
            t2.setTextColor(0xFF888888);
            return v;
        }
    }

    static class Item {
        long id;
        double amount;
        String note;
        long time;
        int type;
    }

    class DB extends SQLiteOpenHelper {
        DB() {
            super(AccountBookActivity.this, "account_book.db", null, 1);
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE ab ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "amount REAL,"
                + "note TEXT,"
                + "time INTEGER,"
                + "type INTEGER)");
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int o, int n) {
            db.execSQL("DROP TABLE IF EXISTS ab");
            onCreate(db);
        }
    }
}