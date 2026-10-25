package com.example.myempty.activity2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

public class GitHubTokenStore {

    private static final String SP = "github_sp";
    private static final String KEY_TOKEN = "token_enc";
    private static final String SECRET = "MyEmptyActivity_GH_v1_DoNotChange";

    public static void save(Context ctx, String token) {
        try {
            byte[] enc = CryptoUtils.aesEncrypt(token.getBytes("UTF-8"), SECRET);
            String b64 = Base64.encodeToString(enc, Base64.NO_WRAP);
            ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
                .edit().putString(KEY_TOKEN, b64).apply();
        } catch (Exception ignored) {}
    }

    public static String load(Context ctx) {
        try {
            String b64 = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
                .getString(KEY_TOKEN, "");
            if (b64 == null || b64.isEmpty()) return "";
            byte[] enc = Base64.decode(b64, Base64.DEFAULT);
            byte[] dec = CryptoUtils.aesDecrypt(enc, SECRET);
            return new String(dec, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    public static void clear(Context ctx) {
        ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
            .edit().remove(KEY_TOKEN).apply();
    }
}