package com.example.myempty.activity2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ChatDb extends SQLiteOpenHelper {

    private static final String DB_NAME = "chat.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "messages";
    private static final int MAX_ROWS = 8000;

    private static ChatDb instance;

    public static synchronized ChatDb get(Context ctx) {
        if (instance == null) {
            instance = new ChatDb(ctx.getApplicationContext());
        }
        return instance;
    }

    public static synchronized void closeDb() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }

    private ChatDb(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "type TEXT,"
            + "user TEXT,"
            + "time INTEGER,"
            + "content TEXT,"
            + "size INTEGER,"
            + "file_path TEXT"
            + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public synchronized long insert(ChatMsg m) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("type", m.type);
        cv.put("user", m.user);
        cv.put("time", m.time);
        cv.put("content", m.content);
        cv.put("size", m.size);
        cv.put("file_path", m.filePath);
        long id = db.insert(TABLE, null, cv);
        m.id = id;
        trim();
        return id;
    }

    public synchronized void trim() {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE, null);
        long count = 0;
        if (c.moveToFirst()) count = c.getLong(0);
        c.close();

        if (count > MAX_ROWS) {
            long over = count - MAX_ROWS;
            db.execSQL("DELETE FROM " + TABLE
                + " WHERE id IN (SELECT id FROM " + TABLE
                + " ORDER BY id ASC LIMIT " + over + ")");
        }
    }

    public synchronized List<ChatMsg> getAll() {
        List<ChatMsg> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,type,user,time,content,size,file_path"
            + " FROM " + TABLE + " ORDER BY id ASC", null);
        while (c.moveToNext()) {
            ChatMsg m = new ChatMsg();
            m.id = c.getLong(0);
            m.type = c.getString(1);
            m.user = c.getString(2);
            m.time = c.getLong(3);
            m.content = c.getString(4);
            m.size = c.getLong(5);
            m.filePath = c.getString(6);
            list.add(m);
        }
        c.close();
        return list;
    }

    public synchronized List<ChatMsg> getSince(long lastId) {
        List<ChatMsg> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,type,user,time,content,size,file_path"
            + " FROM " + TABLE + " WHERE id > ? ORDER BY id ASC",
            new String[]{String.valueOf(lastId)});
        while (c.moveToNext()) {
            ChatMsg m = new ChatMsg();
            m.id = c.getLong(0);
            m.type = c.getString(1);
            m.user = c.getString(2);
            m.time = c.getLong(3);
            m.content = c.getString(4);
            m.size = c.getLong(5);
            m.filePath = c.getString(6);
            list.add(m);
        }
        c.close();
        return list;
    }

    public synchronized long lastId() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT MAX(id) FROM " + TABLE, null);
        long id = 0;
        if (c.moveToFirst()) id = c.getLong(0);
        c.close();
        return id;
    }

    public synchronized void clear() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE);
    }
}