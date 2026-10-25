package com.example.myempty.activity2;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class CpuDetailActivity extends AppCompatActivity {

    private WebView web;
    private String pkg, name;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean webReady = false;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refresh();
            ui.postDelayed(this, 1000);
        }
    };

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_monitor);

        web = findViewById(R.id.web_monitor);
        View btnHealth = findViewById(R.id.btn_battery_health);
        if (btnHealth != null) btnHealth.setVisibility(View.GONE);

        pkg = getIntent().getStringExtra("pkg");
        name = getIntent().getStringExtra("name");
        if (name == null) name = pkg;

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        CpuWebBridge bridge = new CpuWebBridge(this);
        bridge.setOnAppClicked((p, n) -> {
            pkg = p;
            name = n;
            pushStrings();
            refresh();
        });
        web.addJavascriptInterface(bridge, "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webReady = true;
                pushStrings();
                refresh();
                ui.removeCallbacks(tick);
                ui.post(tick);
            }
        });

        web.loadUrl("file:///android_asset/monitor.html");
    }

    private void pushStrings() {
        try {
            JSONObject o = new JSONObject();
            o.put("cpu_title", getString(R.string.cpu_detail_cpu));
            o.put("battery_title", getString(R.string.cpu_detail_title));
            o.put("cores", getString(R.string.cpu_detail_mem));
            o.put("temp", getString(R.string.monitor_temp));
            o.put("current", getString(R.string.monitor_current));
            o.put("voltage", getString(R.string.monitor_voltage));
            o.put("no_data", getString(R.string.monitor_no_data));
            String js = "window.setStrings(" + JSONObject.quote(o.toString()) + ")";
            web.evaluateJavascript(js, null);
        } catch (Throwable ignored) {}
    }

    private void refresh() {
        if (!webReady || name == null) return;
        new Thread(() -> {
            float cpu = readCpuRoot(name);
            int mem = readMemRoot(name);

            try {
                JSONObject o = new JSONObject();
                o.put("usage", cpu);
                o.put("cores", mem + " MB");
                o.put("temp", "-");
                o.put("apps", new org.json.JSONArray());

                final String js = "window.updateCpu("
                    + JSONObject.quote(o.toString()) + ")";
                ui.post(() -> {
                    if (webReady) web.evaluateJavascript(js, null);
                });
            } catch (Throwable ignored) {}
        }).start();
    }

    private float readCpuRoot(String target) {
        try {
            Process p = Runtime.getRuntime().exec(
                new String[]{"su", "-c", "top -n 1 -b"});
            java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            String line;
            boolean dataStart = false;
            while ((line = br.readLine()) != null) {
                if (line.trim().startsWith("PID")) { dataStart = true; continue; }
                if (!dataStart) continue;
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 9) continue;
                if (parts[parts.length - 1].equals(target)) {
                    br.close();
                    return Float.parseFloat(parts[8].replace("%", ""));
                }
            }
            br.close();
        } catch (Exception ignored) {}
        return 0;
    }

    private int readMemRoot(String target) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                "dumpsys meminfo " + target});
            java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("TOTAL")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        br.close();
                        return Integer.parseInt(parts[1]) / 1024;
                    }
                }
            }
            br.close();
        } catch (Exception ignored) {}
        return 0;
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(tick);
        if (web != null) web.destroy();
        super.onDestroy();
    }
}