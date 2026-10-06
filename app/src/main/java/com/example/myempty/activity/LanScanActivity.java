package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LanScanActivity extends Activity {

    private ListView lv;
    private TextView tvStatus;
    private final List<String> found = new ArrayList<>();
    private Adapter adapter;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean scanning = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_lan_scan);

        lv = findViewById(R.id.lv_lan_result);
        tvStatus = findViewById(R.id.tv_lan_status);

        adapter = new Adapter();
        lv.setAdapter(adapter);

        findViewById(R.id.btn_lan_scan).setOnClickListener(v -> scan());
    }

    private void scan() {
        if (scanning) return;
        scanning = true;
        found.clear();
        adapter.notifyDataSetChanged();
        tvStatus.setText("正在获取本机 IP...");

        new Thread(() -> {
            String myIp = getMyIp();
            if (myIp == null) {
                ui.post(() -> {
                    scanning = false;
                    tvStatus.setText("连不上 WiFi");
                });
                return;
            }

            final String prefix = myIp.substring(0, myIp.lastIndexOf('.') + 1);
            ui.post(() -> tvStatus.setText("网段: " + prefix + "0/24"));

            List<String> fromArp = readArp();
            List<String> deviceList = new ArrayList<>();

            for (String ip : fromArp) {
                if (ip.startsWith(prefix) && !ip.equals(myIp)) {
                    deviceList.add(ip + "  (ARP)");
                }
            }

            for (int i = 1; i <= 254; i++) {
                final String ip = prefix + i;
                if (ip.equals(myIp)) continue;
                boolean isInList = false;
                for (String d : deviceList) if (d.startsWith(ip + " ")) { isInList = true; break; }
                if (isInList) continue;

                try {
                    InetAddress addr = InetAddress.getByName(ip);
                    if (addr.isReachable(400)) {
                        deviceList.add(ip + "  (ping)");
                        final String ipFinal = ip;
                        ui.post(() -> {
                            found.add(ipFinal + "  (ping)");
                            adapter.notifyDataSetChanged();
                            tvStatus.setText("已发现 " + found.size() + " 台");
                        });
                    }
                } catch (Exception ignored) {}
            }

            final List<String> finalList = deviceList;
            ui.post(() -> {
                scanning = false;
                found.clear();
                Collections.sort(finalList);
                found.addAll(finalList);
                adapter.notifyDataSetChanged();
                tvStatus.setText("扫完了，共 " + found.size() + " 台设备");
            });
        }).start();
    }

    private String getMyIp() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
            int ip = wm.getConnectionInfo().getIpAddress();
            if (ip == 0) return null;
            return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF)
                + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> readArp() {
        List<String> list = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                new java.io.FileInputStream("/proc/net/arp")));
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 1) {
                    list.add(parts[0]);
                }
            }
            br.close();
        } catch (Exception ignored) {}
        return list;
    }

    class Adapter extends BaseAdapter {
        @Override public int getCount() { return found.size(); }
        @Override public String getItem(int i) { return found.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(LanScanActivity.this)
                .inflate(android.R.layout.simple_list_item_1, parent, false);
            TextView tv = v.findViewById(android.R.id.text1);
            tv.setText(getItem(pos));
            tv.setTextColor(0xFF333333);
            tv.setPadding(24, 24, 24, 24);
            return v;
        }
    }
}