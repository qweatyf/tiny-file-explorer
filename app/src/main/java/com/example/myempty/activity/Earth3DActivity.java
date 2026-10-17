package com.example.myempty.activity2;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Earth3DActivity extends AppCompatActivity {

    private WebView web;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_earth_3d);

        web = findViewById(R.id.web_earth);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pushStrings();
                pushLocate();
                pushLabels();
            }
        });

        web.loadUrl("file:///android_asset/earth.html");
    }

    private class Bridge {
        @JavascriptInterface
        public String getLabels() {
            return readLabels();
        }
    }

    private void pushStrings() {
        try {
            JSONObject o = new JSONObject();
            o.put("lat", getString(R.string.earth_lat));
            o.put("lon", getString(R.string.earth_lon));
            o.put("zoom", getString(R.string.earth_zoom));
            o.put("hint", getString(R.string.earth_hint));
            o.put("btn_lat", getString(R.string.earth_btn_lat));
            o.put("btn_lon", getString(R.string.earth_btn_lon));
            o.put("btn_mag", getString(R.string.earth_btn_mag));
            String js = "window.setStrings(" + JSONObject.quote(o.toString()) + ")";
            web.evaluateJavascript(js, null);
        } catch (Throwable ignored) {}
    }

    private void pushLocate() {
        try {
            double lat = getIntent().getDoubleExtra("lat", Double.NaN);
            double lon = getIntent().getDoubleExtra("lon", Double.NaN);
            if (Double.isNaN(lat) || Double.isNaN(lon)) return;
            JSONObject o = new JSONObject();
            o.put("lat", lat);
            o.put("lon", lon);
            String js = "window.locate(" + JSONObject.quote(o.toString()) + ")";
            web.evaluateJavascript(js, null);
        } catch (Throwable ignored) {}
    }

    private void pushLabels() {
        try {
            String json = readLabels();
            String js = "window.loadLabels(" + JSONObject.quote(json) + ")";
            web.evaluateJavascript(js, null);
        } catch (Throwable ignored) {}
    }

    private String readLabels() {
        InputStream is = null;
        BufferedReader br = null;
        try {
            is = getAssets().open("earth_labels.json");
            br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            String content = sb.toString();

            boolean en = LocaleHelper.isEnglish(this);

            JSONObject root = new JSONObject(content);
            JSONArray countries = root.optJSONArray("countries");
            JSONArray oceans = root.optJSONArray("oceans");
            JSONArray outCountries = new JSONArray();
            JSONArray outOceans = new JSONArray();

            if (countries != null) {
                for (int i = 0; i < countries.length(); i++) {
                    JSONObject c = countries.getJSONObject(i);
                    JSONObject n = new JSONObject();
                    n.put("name", en ? c.optString("en", c.optString("name"))
                                     : c.optString("name"));
                    n.put("lat", c.optDouble("lat"));
                    n.put("lon", c.optDouble("lon"));
                    outCountries.put(n);
                }
            }

            if (oceans != null) {
                for (int i = 0; i < oceans.length(); i++) {
                    JSONObject c = oceans.getJSONObject(i);
                    JSONObject n = new JSONObject();
                    n.put("name", en ? c.optString("en", c.optString("name"))
                                     : c.optString("name"));
                    n.put("lat", c.optDouble("lat"));
                    n.put("lon", c.optDouble("lon"));
                    outOceans.put(n);
                }
            }

            JSONObject out = new JSONObject();
            out.put("countries", outCountries);
            out.put("oceans", outOceans);
            return out.toString();
        } catch (Throwable e) {
            return "{\"countries\":[],\"oceans\":[],\"err\":\"read\"}";
        } finally {
            try { if (br != null) br.close(); } catch (Throwable ignored) {}
            try { if (is != null) is.close(); } catch (Throwable ignored) {}
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.removeJavascriptInterface("AndroidBridge");
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}