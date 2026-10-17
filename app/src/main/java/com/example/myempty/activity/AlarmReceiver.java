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

import java.util.List;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "alarm_channel";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        long id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, 0);
        String label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL);
        String music = intent.getStringExtra(AlarmScheduler.EXTRA_MUSIC);
        if (label == null) label = ctx.getString(R.string.alarm_default_label);

        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = null;
        if (pm != null) {
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyEmpty:alarm");
            wl.acquire(60 * 1000L);
        }

        try {
            Intent ring = new Intent(ctx, AlarmRingActivity.class);
            ring.putExtra(AlarmScheduler.EXTRA_LABEL, label);
            ring.putExtra(AlarmScheduler.EXTRA_MUSIC, music);
            ring.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            ctx.startActivity(ring);

            notifyUser(ctx, label, id);

            if (id > 0) {
                List<AlarmData> list = AlarmScheduler.loadAll(ctx);
                for (AlarmData d : list) {
                    if (d.id == id) {
                        if (d.type == AlarmData.TYPE_ONCE) {
                            list.remove(d);
                            AlarmScheduler.saveAll(ctx, list);
                        } else {
                            d.triggerAt = d.nextTrigger(System.currentTimeMillis());
                            AlarmScheduler.schedule(ctx, d);
                            AlarmScheduler.saveAll(ctx, list);
                        }
                        break;
                    }
                }
            }
        } finally {
            if (wl != null && wl.isHeld()) wl.release();
        }
    }

    private void notifyUser(Context ctx, String label, long id) {
        NotificationManager nm = (NotificationManager)
            ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, ctx.getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
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

        b.setContentTitle(ctx.getString(R.string.title_alarm_clock))
            .setContentText(label)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(Notification.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi);

        nm.notify(9001, b.build());
    }
}