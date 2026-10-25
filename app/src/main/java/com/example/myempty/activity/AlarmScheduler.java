package com.example.myempty.activity2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public class AlarmScheduler {

    public static final String ACTION_RING = "com.example.myempty.activity2.ACTION_RING";
    public static final String EXTRA_LABEL = "label";
    public static final String EXTRA_MUSIC = "music";
    public static final String EXTRA_ID = "id";

    private static final String SP = "alarm_store";
    private static final String KEY_LIST = "alarm_list";

    public static SharedPreferences sp(Context ctx) {
        return ctx.getSharedPreferences(SP, Context.MODE_PRIVATE);
    }

    public static List<AlarmData> loadAll(Context ctx) {
        return AlarmData.listFromJson(sp(ctx).getString(KEY_LIST, ""));
    }

    public static void saveAll(Context ctx, List<AlarmData> list) {
        sp(ctx).edit().putString(KEY_LIST, AlarmData.listToJson(list)).apply();
    }

    public static void add(Context ctx, AlarmData d) {
        List<AlarmData> list = loadAll(ctx);
        d.id = System.currentTimeMillis();
        d.triggerAt = d.nextTrigger(System.currentTimeMillis());
        list.add(d);
        saveAll(ctx, list);
        schedule(ctx, d);
    }

    public static void remove(Context ctx, long id) {
        List<AlarmData> list = loadAll(ctx);
        List<AlarmData> next = new ArrayList<>();
        for (AlarmData d : list) {
            if (d.id == id) {
                cancel(ctx, d);
            } else {
                next.add(d);
            }
        }
        saveAll(ctx, next);
    }

    public static void schedule(Context ctx, AlarmData d) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.setAction(ACTION_RING);
        i.putExtra(EXTRA_ID, d.id);
        i.putExtra(EXTRA_LABEL, d.label == null ? "" : d.label);
        i.putExtra(EXTRA_MUSIC, d.musicPath == null ? "" : d.musicPath);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(ctx,
            (int) (d.id % Integer.MAX_VALUE), i, flags);

        long at = d.triggerAt;
        if (at <= System.currentTimeMillis()) {
            at = d.nextTrigger(System.currentTimeMillis());
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException e) {
            am.set(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    public static void rescheduleAll(Context ctx) {
        List<AlarmData> list = loadAll(ctx);
        for (AlarmData d : list) {
            d.triggerAt = d.nextTrigger(System.currentTimeMillis());
            schedule(ctx, d);
        }
        saveAll(ctx, list);
    }

    public static void cancel(Context ctx, AlarmData d) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.setAction(ACTION_RING);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(ctx,
            (int) (d.id % Integer.MAX_VALUE), i, flags);
        am.cancel(pi);
    }
}