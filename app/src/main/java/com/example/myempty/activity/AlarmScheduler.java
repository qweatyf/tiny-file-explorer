package com.example.myempty.activity2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmScheduler {

    public static final String ACTION_RING = "com.example.myempty.activity2.ACTION_RING";
    public static final String EXTRA_LABEL = "label";
    public static final String EXTRA_TIME = "time";

    public static void schedule(Context ctx, long triggerAt, String label) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.setAction(ACTION_RING);
        i.putExtra(EXTRA_LABEL, label);
        i.putExtra(EXTRA_TIME, triggerAt);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(ctx,
            (int) (triggerAt % Integer.MAX_VALUE), i, flags);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (SecurityException e) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void cancel(Context ctx, long triggerAt) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.setAction(ACTION_RING);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(ctx,
            (int) (triggerAt % Integer.MAX_VALUE), i, flags);
        am.cancel(pi);
    }
}