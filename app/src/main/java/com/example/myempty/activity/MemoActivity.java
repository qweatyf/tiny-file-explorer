package com.example.myempty.activity2;

import android.app.AlertDialog;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemoActivity extends AppCompatActivity {

    private EditText etSearch;
    private ListView lv;
    private MemoDb db;
    private final List<MemoDb.Memo> list = new ArrayList<>();
    private MemoAdapter adapter;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_memo);

        etSearch = findViewById(R.id.et_memo_search);
        lv = findViewById(R.id.lv_memo_list);

        db = new MemoDb(this);
        adapter = new MemoAdapter();
        lv.setAdapter(adapter);

        findViewById(R.id.btn_save_memo).setOnClickListener(v -> addNew());
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) { load(s.toString()); }
        });

        lv.setOnItemClickListener((p, v, pos, id) -> edit(list.get(pos)));
        lv.setOnItemLongClickListener((p, v, pos, id) -> {
            confirmDelete(list.get(pos));
            return true;
        });

        load("");
    }

    private void load(String kw) {
        list.clear();
        list.addAll(db.search(kw));
        adapter.notifyDataSetChanged();
    }

    private void addNew() {
        EditText et = new EditText(this);
        et.setHint(getString(R.string.memo_hint));
        et.setMinLines(5);
        et.setGravity(android.view.Gravity.TOP);

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.memo_title))
            .setView(et)
            .setPositiveButton(getString(R.string.memo_save), (d, w) -> {
                String txt = et.getText().toString().trim();
                if (!txt.isEmpty()) {
                    db.insert(txt);
                    load("");
                    Toast.makeText(this, getString(R.string.memo_saved), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(getString(R.string.file_rename_cancel), null)
            .show();
    }

    private void edit(final MemoDb.Memo m) {
        EditText et = new EditText(this);
        et.setText(m.content);
        et.setMinLines(5);
        et.setGravity(android.view.Gravity.TOP);

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.memo_title))
            .setView(et)
            .setPositiveButton(getString(R.string.memo_save), (d, w) -> {
                String txt = et.getText().toString().trim();
                if (!txt.isEmpty()) {
                    db.update(m.id, txt);
                    load(etSearch.getText().toString());
                    Toast.makeText(this, getString(R.string.memo_saved), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(getString(R.string.file_rename_cancel), null)
            .show();
    }

    private void confirmDelete(final MemoDb.Memo m) {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.file_del_title))
            .setMessage(getString(R.string.file_del_msg))
            .setPositiveButton(getString(R.string.file_del_ok), (d, w) -> {
                db.delete(m.id);
                load(etSearch.getText().toString());
            })
            .setNegativeButton(getString(R.string.file_del_cancel), null)
            .show();
    }

    class MemoAdapter extends BaseAdapter {
        @Override public int getCount() { return list.size(); }
        @Override public MemoDb.Memo getItem(int i) { return list.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(MemoActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);
            TextView t1 = v.findViewById(android.R.id.text1);
            TextView t2 = v.findViewById(android.R.id.text2);
            MemoDb.Memo m = getItem(pos);

            t1.setText(m.content);
            t1.setTextColor(0xFF333333);
            t1.setTextSize(15);
            t1.setMaxLines(2);
            t1.setEllipsize(android.text.TextUtils.TruncateAt.END);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            t2.setText(sdf.format(new Date(m.time)));
            t2.setTextColor(0xFF888888);
            t2.setTextSize(12);
            return v;
        }
    }
}