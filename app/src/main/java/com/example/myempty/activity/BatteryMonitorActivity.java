package com.example.myempty.activity2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class BatteryMonitorActivity extends AppCompatActivity {

    private WebView web;
    private Button btnHealth;
    private final Handler ui = new Handler(Looper.getMainLooper());
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
        btnHealth = findViewById(R.id.btn_battery_health);
        btnHealth.setVisibility(android.view.View.VISIBLE);
        btnHealth.setOnClickListener(v ->
            startActivity(new Intent(this, BatteryHealthActivity.class)));

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        BatteryWebBridge bridge = new BatteryWebBridge(this);
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
        IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent i = registerReceiver(null, f);
        if (i == null) return;

        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int volt = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);

        float percent = level * 100f / scale;
        int current = readCurrentMa();

        try {
            JSONObject o = new JSONObject();
            o.put("level", (int) percent);
            o.put("current", current);
            o.put("voltage", volt);
            final String js = "window.updateBattery("
                + JSONObject.quote(o.toString()) + ")";
            if (webReady) web.evaluateJavascript(js, null);
        } catch (Throwable ignored) {}
    }

    private int readCurrentMa() {
        try {
            BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
            int ua = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            if (ua == Integer.MIN_VALUE || ua == 0) return 0;
            return ua / 1000;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(tick);
        if (web != null) web.destroy();
        super.onDestroy();
    }
}