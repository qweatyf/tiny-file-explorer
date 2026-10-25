package com.example.myempty.activity2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class MemoDb extends SQLiteOpenHelper {

    public static class Memo {
        public long id;
        public String content;
        public long time;
    }

    public MemoDb(Context ctx) {
        super(ctx, "memos.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE memo (id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT, time INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) {
        db.execSQL("DROP TABLE IF EXISTS memo");
        onCreate(db);
    }

    public long insert(String content) {
        ContentValues cv = new ContentValues();
        cv.put("content", content);
        cv.put("time", System.currentTimeMillis());
        return getWritableDatabase().insert("memo", null, cv);
    }

    public void update(long id, String content) {
        ContentValues cv = new ContentValues();
        cv.put("content", content);
        cv.put("time", System.currentTimeMillis());
        getWritableDatabase().update("memo", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void delete(long id) {
        getWritableDatabase().delete("memo", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Memo> search(String kw) {
        List<Memo> list = new ArrayList<>();
        String sql = "SELECT id,content,time FROM memo";
        String[] args = null;
        if (kw != null && !kw.trim().isEmpty()) {
            sql += " WHERE content LIKE ?";
            args = new String[]{"%" + kw.trim() + "%"};
        }
        sql += " ORDER BY time DESC";

        Cursor c = getReadableDatabase().rawQuery(sql, args);
        while (c.moveToNext()) {
            Memo m = new Memo();
            m.id = c.getLong(0);
            m.content = c.getString(1);
            m.time = c.getLong(2);
            list.add(m);
        }
        c.close();
        return list;
    }
}