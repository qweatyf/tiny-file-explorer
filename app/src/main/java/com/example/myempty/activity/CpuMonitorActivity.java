package com.example.myempty.activity2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CpuMonitorActivity extends AppCompatActivity {

    private WebView web;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private long lastTotal = 0, lastIdle = 0;
    private boolean webReady = false;
    private boolean usingFallback = false;

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

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        CpuWebBridge bridge = new CpuWebBridge(this);
        bridge.setOnAppClicked((pkg, name) -> {
            Intent i = new Intent(this, CpuDetailActivity.class);
            i.putExtra("pkg", pkg);
            i.putExtra("name", name);
            startActivity(i);
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

        try {
            web.loadUrl("file:///android_asset/monitor.html");
        } catch (Throwable t) {
            showFallback();
        }

        ui.postDelayed(() -> {
            if (!webReady) showFallback();
        }, 3000);
    }

    private void showFallback() {
        if (usingFallback) return;
        usingFallback = true;
        ui.removeCallbacks(tick);
        try {
            String title = getString(R.string.monitor_unsupported);
            String msg = getString(R.string.monitor_unsupported_hint);
            web.loadUrl("file:///android_asset/fallback.html");
            web.evaluateJavascript("(function(){"
                + "var t=" + JSONObject.quote(title) + ";"
                + "var m=" + JSONObject.quote(msg) + ";"
                + "var e=document.getElementById('fb-title'); if(e)e.textContent=t;"
                + "var m2=document.getElementById('fb-msg'); if(m2)m2.innerHTML=m;"
                + "})()", null);
        } catch (Throwable ignored) {}
    }

    private void pushStrings() {
        try {
            JSONObject o = new JSONObject();
            o.put("cpu_title", getString(R.string.monitor_cpu_title));
            o.put("battery_title", getString(R.string.monitor_battery_title));
            o.put("cores", getString(R.string.monitor_cores));
            o.put("temp", getString(R.string.monitor_temp));
            o.put("current", getString(R.string.monitor_current));
            o.put("voltage", getString(R.string.monitor_voltage));
            o.put("no_data", getString(R.string.monitor_no_data));
            String js = "window.setStrings(" + JSONObject.quote(o.toString()) + ")";
            web.evaluateJavascript(js, null);
        } catch (Throwable ignored) {}
    }

    private void refresh() {
        if (usingFallback) return;
        new Thread(() -> {
            float usage = readCpuUsage();
            int cores = Runtime.getRuntime().availableProcessors();
            String temp = readTemp();
            List<AppInfo> apps = RootUtils.isRooted()
                ? readAppsRoot() : new ArrayList<AppInfo>();
            Collections.sort(apps, new Comparator<AppInfo>() {
                @Override
                public int compare(AppInfo a, AppInfo b) {
                    return Float.compare(b.value, a.value);
                }
            });
            if (apps.size() > 20) apps = apps.subList(0, 20);

            try {
                JSONObject o = new JSONObject();
                o.put("usage", usage);
                o.put("cores", cores);
                o.put("temp", temp);
                JSONArray arr = new JSONArray();
                for (AppInfo a : apps) {
                    JSONObject ao = new JSONObject();
                    ao.put("name", a.name);
                    ao.put("pkg", a.pkg);
                    ao.put("value", a.value);
                    arr.put(ao);
                }
                o.put("apps", arr);

                final String js = "window.updateCpu("
                    + JSONObject.quote(o.toString()) + ")";
                ui.post(() -> {
                    if (webReady) web.evaluateJavascript(js, null);
                });
            } catch (Throwable ignored) {}
        }).start();
    }

    private float readCpuUsage() {
        try {
            RandomAccessFile raf = new RandomAccessFile("/proc/stat", "r");
            String line = raf.readLine();
            raf.close();
            if (line == null || !line.startsWith("cpu ")) return 0;
            String[] parts = line.trim().split("\\s+");
            long total = 0;
            for (int i = 1; i < parts.length; i++) total += Long.parseLong(parts[i]);
            long idle = Long.parseLong(parts[4]) + Long.parseLong(parts[5]);
            if (lastTotal == 0) {
                lastTotal = total;
                lastIdle = idle;
                return 0;
            }
            long dTotal = total - lastTotal;
            long dIdle = idle - lastIdle;
            lastTotal = total;
            lastIdle = idle;
            if (dTotal <= 0) return 0;
            float u = (dTotal - dIdle) * 100f / dTotal;
            if (u < 0) u = 0;
            if (u > 100) u = 100;
            return u;
        } catch (Exception e) {
            return 0;
        }
    }

    private String readTemp() {
        try {
            BufferedReader br = new BufferedReader(
                new FileReader("/sys/class/thermal/thermal_zone0/temp"));
            String s = br.readLine();
            br.close();
            if (s == null) return "-";
            double t = Double.parseDouble(s.trim());
            if (t > 1000) t /= 1000;
            return String.format("%.1f", t);
        } catch (Exception e) {
            return "-";
        }
    }

    private List<AppInfo> readAppsRoot() {
        List<AppInfo> list = new ArrayList<>();
        try {
            Process p = Runtime.getRuntime().exec(
                new String[]{"su", "-c", "top -n 1 -b"});
            BufferedReader br = new BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            String line;
            boolean dataStart = false;
            while ((line = br.readLine()) != null) {
                if (line.trim().startsWith("PID")) { dataStart = true; continue; }
                if (!dataStart) continue;
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 9) continue;
                try {
                    float cpu = Float.parseFloat(parts[8].replace("%", ""));
                    String name = parts[parts.length - 1];
                    if (name.isEmpty() || name.startsWith("[")) continue;
                    AppInfo a = new AppInfo();
                    a.pkg = name;
                    a.name = name;
                    a.value = cpu;
                    list.add(a);
                } catch (Exception ignored) {}
            }
            br.close();
        } catch (Exception ignored) {}
        return list;
    }

    private static class AppInfo {
        String pkg;
        String name;
        float value;
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(tick);
        if (web != null) web.destroy();
        super.onDestroy();
    }
}