package com.example.myempty.activity2;

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

import androidx.appcompat.app.AppCompatActivity;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class PortScanActivity extends AppCompatActivity {

    private EditText etHost, etStart, etEnd;
    private ListView lv;
    private TextView tvStatus;
    private final List<String> openPorts = new ArrayList<>();
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
        if (scanning) return;
        final String host = etHost.getText().toString().trim();
        if (host.isEmpty()) {
            Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show();
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
        tvStatus.setText("...");

        new Thread(() -> {
            try {
                final InetAddress addr = InetAddress.getByName(host);
                ui.post(() -> tvStatus.setText(addr.getHostAddress() + " " + fs + "-" + fe));
                for (int port = fs; port <= fe; port++) {
                    try {
                        Socket s = new Socket();
                        s.connect(new InetSocketAddress(addr, port), 200);
                        s.close();
                        final int p = port;
                        ui.post(() -> { openPorts.add(String.valueOf(p)); adapter.notifyDataSetChanged(); });
                    } catch (Exception ignored) {}
                }
                ui.post(() -> {
                    scanning = false;
                    tvStatus.setText(openPorts.size() + "");
                });
            } catch (Exception e) {
                ui.post(() -> { scanning = false; tvStatus.setText(e.getMessage()); });
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