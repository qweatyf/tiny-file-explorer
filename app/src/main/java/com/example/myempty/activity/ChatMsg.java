package com.example.myempty.activity2;

public class ChatMsg {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_FILE = "file";
    public static final String TYPE_SYSTEM = "system";

    public long id;
    public String type;
    public String user;
    public long time;
    public String content;
    public long size;
    public String filePath;
    public boolean self;
    public boolean tampered;

    public ChatMsg() {
    }

    public ChatMsg(String type, String user, long time, String content) {
        this.type = type;
        this.user = user;
        this.time = time;
        this.content = content;
    }

    public boolean isText() {
        return TYPE_TEXT.equals(type);
    }

    public boolean isImage() {
        return TYPE_IMAGE.equals(type);
    }

    public boolean isFile() {
        return TYPE_FILE.equals(type);
    }

    public boolean isSystem() {
        return TYPE_SYSTEM.equals(type);
    }
}