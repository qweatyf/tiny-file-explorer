package com.example.myempty.activity2;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

public class ChatDiscovery {

    public interface OnHostFound {
        void onFound(String ip, String user);
    }

    public interface OnClientFound {
        void onFound(String ip, String user);
    }

    private static final int PORT = 8900;
    private static final String MAGIC = "MYEMPTYCHAT_V1";
    private static final String MULTICAST_ADDR = "239.255.42.99";

    private MulticastSocket socket;
    private WifiManager.MulticastLock lock;
    private Thread thread;
    private boolean running = false;

    private boolean isHost;
    private String myName;
    private String myIp;

    public void startAsHost(Context ctx, String name, String ip) {
        isHost = true;
        myName = name;
        myIp = ip;
        start(ctx, null, null);
    }

    public void startAsClient(Context ctx, OnHostFound cb) {
        isHost = false;
        start(ctx, cb, null);
    }

    private void start(Context ctx, OnHostFound hostCb, OnClientFound clientCb) {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
            lock = wm.createMulticastLock("chat_discovery");
            lock.setReferenceCounted(true);
            lock.acquire();

            socket = new MulticastSocket(PORT);
            socket.setReuseAddress(true);
            socket.setTimeToLive(4);
            InetAddress group = InetAddress.getByName(MULTICAST_ADDR);
            socket.joinGroup(group);

            running = true;

            thread = new Thread(() -> loop(hostCb, clientCb));
            thread.start();
        } catch (Exception e) {
            running = false;
        }
    }

    private void loop(OnHostFound hostCb, OnClientFound clientCb) {
        byte[] buf = new byte[512];

        while (running) {
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.setSoTimeout(2000);
                socket.receive(pkt);

                String msg = new String(pkt.getData(), 0, pkt.getLength(), "UTF-8");
                String fromIp = pkt.getAddress().getHostAddress();

                if (isHost) {
                    if (msg.startsWith(MAGIC + "|FIND")) {
                        String reply = MAGIC + "|HERE|" + myName + "|" + myIp;
                        byte[] out = reply.getBytes("UTF-8");
                        DatagramPacket p = new DatagramPacket(out, out.length,
                            pkt.getAddress(), pkt.getPort());
                        socket.send(p);
                    }
                } else {
                    if (msg.startsWith(MAGIC + "|HERE|")) {
                        String[] parts = msg.split("\\|");
                        if (parts.length >= 4) {
                            String user = parts[2];
                            String ip = parts[3];
                            if (hostCb != null && !fromIp.equals(getMyIpQuick())) {
                                hostCb.onFound(ip, user);
                            }
                        }
                    }
                }
            } catch (SocketTimeoutException te) {
                if (!isHost) {
                    try {
                        String ask = MAGIC + "|FIND";
                        byte[] out = ask.getBytes("UTF-8");
                        DatagramPacket p = new DatagramPacket(out, out.length,
                            InetAddress.getByName(MULTICAST_ADDR), PORT);
                        socket.send(p);
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                if (!running) break;
            }
        }
    }

    private String getMyIpQuick() {
        return myIp != null ? myIp : "";
    }

    public void stop() {
        running = false;
        try {
            if (thread != null) thread.interrupt();
        } catch (Exception ignored) {}
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
        try {
            if (lock != null && lock.isHeld()) lock.release();
        } catch (Exception ignored) {}
        socket = null;
        thread = null;
    }

    public boolean isRunning() {
        return running;
    }
}