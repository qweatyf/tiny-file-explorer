package com.example.myempty.activity2;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {

    private static final String SP_NAME = "locale_sp";
    private static final String KEY_LANG = "lang";

    public static final String LANG_ZH = "zh";
    public static final String LANG_EN = "en";

    public static String getLang(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_LANG, LANG_ZH);
    }

    public static void setLang(Context ctx, String lang) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_LANG, lang).apply();
    }

    public static Context wrap(Context ctx) {
        String lang = getLang(ctx);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(ctx.getResources().getConfiguration());
        config.setLocale(locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return ctx.createConfigurationContext(config);
        } else {
            ctx.getResources().updateConfiguration(config, ctx.getResources().getDisplayMetrics());
            return ctx;
        }
    }

    public static boolean isEnglish(Context ctx) {
        return LANG_EN.equals(getLang(ctx));
    }
}