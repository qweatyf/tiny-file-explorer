package com.example.myempty.activity2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.io.IOException;

public class MyServerService extends Service {
    private MyServer server;

    @Override
    public void onCreate() {
        super.onCreate();

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "server_channel", getString(R.string.server_channel_name),
                NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            notification = new Notification.Builder(this, "server_channel")
                .setContentTitle(getString(R.string.server_running))
                .setContentText(getString(R.string.server_running_hint))
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .build();
        } else {
            notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.server_running))
                .setContentText(getString(R.string.server_running_hint))
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .build();
        }
        startForeground(1, notification);

        server = new MyServer(this);
        try {
            server.start();
            ServerState.isRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        if (server != null) server.stop();
        ServerState.isRunning = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}