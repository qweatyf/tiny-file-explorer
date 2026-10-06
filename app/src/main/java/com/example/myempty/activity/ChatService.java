package com.example.myempty.activity2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class ChatService extends Service {

    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_USER = "user";
    public static final String EXTRA_IP = "ip";

    public static final String MODE_HOST = "host";
    public static final String MODE_CLIENT = "client";

    private ChatServer server;
    private ChatDiscovery discovery;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(2, buildNotification("聊天服务跑着"));

        if (intent != null) {
            String mode = intent.getStringExtra(EXTRA_MODE);
            String user = intent.getStringExtra(EXTRA_USER);
            String ip = intent.getStringExtra(EXTRA_IP);

            if (MODE_HOST.equals(mode)) {
                startHost(user, ip);
            }
        }

        return START_STICKY;
    }

    private void startHost(String user, String ip) {
        if (server != null) return;
        try {
            server = new ChatServer(this);
            server.start();

            discovery = new ChatDiscovery();
            discovery.startAsHost(this, user, ip);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Notification buildNotification(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                "chat_channel", "聊天服务", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
            return new Notification.Builder(this, "chat_channel")
                .setContentTitle("本地聊天")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .build();
        } else {
            return new Notification.Builder(this)
                .setContentTitle("本地聊天")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .build();
        }
    }

    @Override
    public void onDestroy() {
        if (server != null) {
            server.stop();
            server = null;
        }
        if (discovery != null) {
            discovery.stop();
            discovery = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}