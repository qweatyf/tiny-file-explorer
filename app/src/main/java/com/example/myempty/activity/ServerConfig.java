package com.example.myempty.activity2;

import android.content.Context;
import android.content.SharedPreferences;

public class ServerConfig {

    private static final String SP_NAME = "server_sp";
    private static final String KEY_URL = "server_url";
    private static final String KEY_TOKEN = "server_token";

    public static SharedPreferences sp(Context ctx) {
        return ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    public static String getUrl(Context ctx) {
        String u = sp(ctx).getString(KEY_URL, "");
        if (u == null) return "";
        return u.trim();
    }

    public static void setUrl(Context ctx, String url) {
        if (url == null) url = "";
        sp(ctx).edit().putString(KEY_URL, url.trim()).apply();
    }

    public static String getToken(Context ctx) {
        String t = sp(ctx).getString(KEY_TOKEN, "");
        if (t == null) return "";
        return t;
    }

    public static void setToken(Context ctx, String token) {
        if (token == null) token = "";
        sp(ctx).edit().putString(KEY_TOKEN, token).apply();
    }

    public static boolean isRemote(Context ctx) {
        return !getUrl(ctx).isEmpty();
    }

    public static boolean isLocal(Context ctx) {
        return getUrl(ctx).isEmpty();
    }
}