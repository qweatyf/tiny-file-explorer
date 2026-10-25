package com.example.myempty.activity2;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class BatteryWebBridge {

    private final Activity activity;

    public BatteryWebBridge(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void onWebGLError(final String msg) {
        activity.runOnUiThread(() ->
            Toast.makeText(activity, "WebGL: " + msg, Toast.LENGTH_SHORT).show());
    }
}