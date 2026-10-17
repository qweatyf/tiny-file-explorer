package com.example.myempty.activity2;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class CpuWebBridge {

    private final Activity activity;

    public interface OnAppClicked {
        void onApp(String pkg, String name);
    }

    private OnAppClicked callback;

    public CpuWebBridge(Activity activity) {
        this.activity = activity;
    }

    public void setOnAppClicked(OnAppClicked cb) {
        this.callback = cb;
    }

    @JavascriptInterface
    public void onAppClicked(final String pkg, final String name) {
        activity.runOnUiThread(() -> {
            if (callback != null) callback.onApp(pkg, name);
        });
    }

    @JavascriptInterface
    public void onWebGLError(final String msg) {
        activity.runOnUiThread(() ->
            Toast.makeText(activity, "WebGL: " + msg, Toast.LENGTH_SHORT).show());
    }
}