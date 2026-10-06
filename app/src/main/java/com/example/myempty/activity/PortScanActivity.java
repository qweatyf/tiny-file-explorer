package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class PortScanActivity extends Activity {

    private EditText etHost;
    private EditText etStart;
    private EditText etEnd;
    private ListView lv;
    private TextView tvStatus;
    private final List<String> openPorts = new ArrayList<>();
    private Adapter adapter;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean scanning = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_port_scan);

        etHost = findViewById(R.id.et_port_host);
        etStart = findViewById(R.id.et_port_start);
        etEnd = findViewById(R.id.et_port_end);
        lv = findViewById(R.id.lv_port_result);
        tvStatus = findViewById(R.id.tv_port_status);

        etStart.setText("1");
        etEnd.setText("1024");

        adapter = new Adapter();
        lv.setAdapter(adapter);

        findViewById(R.id.btn_port_scan).setOnClickListener(v -> scan());
    }

    private void scan() {
        if (scanning) {
            Toast.makeText(this, "还在扫", Toast.LENGTH_SHORT).show();
            return;
        }
        final String host = etHost.getText().toString().trim();
        if (host.isEmpty()) {
            Toast.makeText(this, "填 IP 或域名", Toast.LENGTH_SHORT).show();
            return;
        }
        int start, end;
        try { start = Integer.parseInt(etStart.getText().toString().trim()); }
        catch (Exception e) { start = 1; }
        try { end = Integer.parseInt(etEnd.getText().toString().trim()); }
        catch (Exception e) { end = 1024; }
        if (start < 1) start = 1;
        if (end > 65535) end = 65535;
        if (end < start) end = start;
        final int fs = start, fe = end;

        scanning = true;
        openPorts.clear();
        adapter.notifyDataSetChanged();
        tvStatus.setText("扫描中...");

        new Thread(() -> {
            try {
                final InetAddress addr = InetAddress.getByName(host);
                ui.post(() -> tvStatus.setText("扫描 " + addr.getHostAddress()
                    + " 端口 " + fs + "~" + fe));

                for (int port = fs; port <= fe; port++) {
                    try {
                        Socket s = new Socket();
                        s.connect(new InetSocketAddress(addr, port), 200);
                        s.close();
                        final int p = port;
                        ui.post(() -> {
                            openPorts.add("端口 " + p + " 开着");
                            adapter.notifyDataSetChanged();
                        });
                    } catch (Exception ignored) {}
                }

                ui.post(() -> {
                    scanning = false;
                    tvStatus.setText(openPorts.isEmpty()
                        ? "没扫到开着的端口"
                        : "共 " + openPorts.size() + " 个开着");
                });
            } catch (Exception e) {
                ui.post(() -> {
                    scanning = false;
                    tvStatus.setText("扫描失败: " + e.getMessage());
                });
            }
        }).start();
    }

    class Adapter extends BaseAdapter {
        @Override public int getCount() { return openPorts.size(); }
        @Override public String getItem(int i) { return openPorts.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(PortScanActivity.this)
                .inflate(android.R.layout.simple_list_item_1, parent, false);
            TextView tv = v.findViewById(android.R.id.text1);
            tv.setText(getItem(pos));
            tv.setTextColor(0xFF1B5E20);
            tv.setPadding(24, 24, 24, 24);
            return v;
        }
    }
}