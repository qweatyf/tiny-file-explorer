package com.example.myempty.activity2;

import android.content.Context;
import android.content.SharedPreferences;

public class ServerConfig {

    private static final String SP_NAME = "server_sp";
    private static final String KEY_URL = "server_url";
    private static final String KEY_TOKEN = "server_token";
    private static final String KEY_SCHEME = "server_scheme";
    private static final String KEY_CONNECTED = "server_connected";
    private static final String KEY_LAST_CHECK = "server_last_check";

    public static SharedPreferences sp(Context ctx) {
        return ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    public static String getUrl(Context ctx) {
        String u = sp(ctx).getString(KEY_URL, "");
        return u == null ? "" : u.trim();
    }

    public static void setUrl(Context ctx, String url) {
        if (url == null) url = "";
        url = url.trim();
        if (url.startsWith("https://")) url = url.substring(8);
        else if (url.startsWith("http://")) url = url.substring(7);
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        sp(ctx).edit().putString(KEY_URL, url).apply();
    }

    public static String getToken(Context ctx) {
        String t = sp(ctx).getString(KEY_TOKEN, "");
        return t == null ? "" : t;
    }

    public static void setToken(Context ctx, String token) {
        if (token == null) token = "";
        sp(ctx).edit().putString(KEY_TOKEN, token).apply();
    }

    public static String getScheme(Context ctx) {
        String s = sp(ctx).getString(KEY_SCHEME, "");
        return s == null ? "" : s;
    }

    public static void setScheme(Context ctx, String scheme) {
        if (scheme == null) scheme = "";
        sp(ctx).edit().putString(KEY_SCHEME, scheme).apply();
    }

    public static boolean isConnected(Context ctx) {
        return sp(ctx).getBoolean(KEY_CONNECTED, false);
    }

    public static void setConnected(Context ctx, boolean connected) {
        sp(ctx).edit().putBoolean(KEY_CONNECTED, connected).apply();
    }

    public static long getLastCheck(Context ctx) {
        return sp(ctx).getLong(KEY_LAST_CHECK, 0L);
    }

    public static void setLastCheck(Context ctx, long t) {
        sp(ctx).edit().putLong(KEY_LAST_CHECK, t).apply();
    }

    public static void clear(Context ctx) {
        sp(ctx).edit().clear().apply();
    }

    public static boolean isRemote(Context ctx) {
        return isConnected(ctx) && !getUrl(ctx).isEmpty();
    }

    public static boolean isLocal(Context ctx) {
        return !isRemote(ctx);
    }

    public static String buildBaseUrl(Context ctx) {
        String scheme = getScheme(ctx);
        if (scheme.isEmpty()) scheme = "https";
        return scheme + "://" + getUrl(ctx);
    }
}