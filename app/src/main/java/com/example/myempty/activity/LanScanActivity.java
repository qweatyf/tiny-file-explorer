package com.example.myempty.activity2;

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

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LanScanActivity extends AppCompatActivity {

    private ListView lv;
    private TextView tvStatus;
    private final List<String> found = new ArrayList<>();
    private Adapter adapter;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean scanning = false;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

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
        tvStatus.setText("...");

        new Thread(() -> {
            String myIp = getMyIp();
            if (myIp == null) {
                ui.post(() -> { scanning = false; tvStatus.setText("-"); });
                return;
            }
            final String prefix = myIp.substring(0, myIp.lastIndexOf('.') + 1);
            ui.post(() -> tvStatus.setText(prefix + "0/24"));

            List<String> list = new ArrayList<>();
            for (String ip : readArp()) {
                if (ip.startsWith(prefix) && !ip.equals(myIp)) list.add(ip + " (ARP)");
            }

            for (int i = 1; i <= 254; i++) {
                final String ip = prefix + i;
                if (ip.equals(myIp)) continue;
                boolean in = false;
                for (String d : list) if (d.startsWith(ip + " ")) { in = true; break; }
                if (in) continue;
                try {
                    InetAddress addr = InetAddress.getByName(ip);
                    if (addr.isReachable(400)) {
                        list.add(ip + " (ping)");
                        final String ipFinal = ip;
                        ui.post(() -> {
                            found.add(ipFinal + " (ping)");
                            adapter.notifyDataSetChanged();
                            tvStatus.setText(found.size() + "");
                        });
                    }
                } catch (Exception ignored) {}
            }

            final List<String> fl = list;
            ui.post(() -> {
                scanning = false;
                found.clear();
                Collections.sort(fl);
                found.addAll(fl);
                adapter.notifyDataSetChanged();
                tvStatus.setText(found.size() + "");
            });
        }).start();
    }

    private String getMyIp() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ip = wm.getConnectionInfo().getIpAddress();
            if (ip == 0) return null;
            return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF)
                + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
        } catch (Exception e) { return null; }
    }

    private List<String> readArp() {
        List<String> list = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new java.io.InputStreamReader(
                new java.io.FileInputStream("/proc/net/arp")));
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 1) list.add(parts[0]);
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