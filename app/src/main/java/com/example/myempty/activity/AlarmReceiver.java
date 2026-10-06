package com.example.myempty.activity2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "alarm_channel";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL);
        if (label == null) label = "时间到";

        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = null;
        if (pm != null) {
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyEmpty:alarm");
            wl.acquire(60 * 1000L);
        }

        try {
            Intent ring = new Intent(ctx, AlarmRingActivity.class);
            ring.putExtra(AlarmScheduler.EXTRA_LABEL, label);
            ring.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            ctx.startActivity(ring);

            notifyUser(ctx, label);
        } finally {
            if (wl != null && wl.isHeld()) wl.release();
        }
    }

    private void notifyUser(Context ctx, String label) {
        NotificationManager nm = (NotificationManager)
            ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "闹钟", NotificationManager.IMPORTANCE_HIGH);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(ctx, AlarmRingActivity.class);
        open.putExtra(AlarmScheduler.EXTRA_LABEL, label);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, flags);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(ctx, CHANNEL_ID);
        } else {
            b = new Notification.Builder(ctx);
        }

        b.setContentTitle("闹钟")
            .setContentText(label)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(Notification.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi);

        nm.notify(9001, b.build());
    }
}