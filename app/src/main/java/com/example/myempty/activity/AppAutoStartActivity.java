package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppAutoStartActivity extends Activity {

    private ListView lv;
    private EditText etSearch;
    private final List<ApplicationInfo> all = new ArrayList<>();
    private final List<ApplicationInfo> shown = new ArrayList<>();
    private final Set<String> bootSet = new HashSet<>();
    private Adapter adapter;
    private PackageManager pm;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_app_auto_start);

        pm = getPackageManager();
        lv = findViewById(R.id.lv_autostart_list);
        etSearch = findViewById(R.id.et_autostart_search);

        adapter = new Adapter();
        lv.setAdapter(adapter);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            ApplicationInfo ai = shown.get(position);
            showDetail(ai);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });

        new Thread(this::load).start();
    }

    private void load() {
        loadBootReceivers();
        List<ApplicationInfo> list = pm.getInstalledApplications(PackageManager.GET_RECEIVERS);
        List<ApplicationInfo> user = new ArrayList<>();
        for (ApplicationInfo ai : list) {
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                user.add(ai);
            }
        }
        Collections.sort(user, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo a, ApplicationInfo b) {
                return pm.getApplicationLabel(a).toString()
                    .compareToIgnoreCase(pm.getApplicationLabel(b).toString());
            }
        });
        runOnUiThread(() -> {
            all.clear();
            all.addAll(user);
            filter("");
        });
    }

    private void loadBootReceivers() {
        bootSet.clear();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                "dumpsys package r"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            String currentPkg = null;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.startsWith("Receiver #")) {
                    currentPkg = null;
                }
                if (t.contains("packageName=")) {
                    int idx = t.indexOf("packageName=");
                    String rest = t.substring(idx + 12);
                    int end = rest.indexOf(' ');
                    currentPkg = end > 0 ? rest.substring(0, end) : rest;
                }
                if (currentPkg != null && t.contains("android.intent.action.BOOT_COMPLETED")) {
                    bootSet.add(currentPkg);
                }
            }
            br.close();
        } catch (Exception ignored) {}
    }

    private void filter(String kw) {
        shown.clear();
        String low = kw == null ? "" : kw.toLowerCase().trim();
        for (ApplicationInfo ai : all) {
            if (low.isEmpty()) {
                shown.add(ai);
                continue;
            }
            String name = pm.getApplicationLabel(ai).toString().toLowerCase();
            if (name.contains(low) || ai.packageName.toLowerCase().contains(low)) {
                shown.add(ai);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showDetail(final ApplicationInfo ai) {
        String label = pm.getApplicationLabel(ai).toString();
        boolean hasBoot = bootSet.contains(ai.packageName);
        new AlertDialog.Builder(this)
            .setTitle(label)
            .setMessage("包名: " + ai.packageName
                + "\n\n自启: " + (hasBoot ? "有" : "没检测到")
                + "\n\n（改自启要 Root，不同 ROM 路径不一样，这里只读）")
            .setPositiveButton("知道了", null)
            .show();
    }

    class Adapter extends BaseAdapter {
        @Override public int getCount() { return shown.size(); }
        @Override public ApplicationInfo getItem(int i) { return shown.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(AppAutoStartActivity.this)
                .inflate(R.layout.item_apk, parent, false);
            ApplicationInfo ai = getItem(pos);
            ImageView iv = v.findViewById(R.id.iv_apk_icon);
            TextView tvName = v.findViewById(R.id.tv_apk_name);
            TextView tvPkg = v.findViewById(R.id.tv_apk_pkg);

            boolean hasBoot = bootSet.contains(ai.packageName);
            iv.setImageDrawable(pm.getApplicationIcon(ai));
            tvName.setText(pm.getApplicationLabel(ai).toString() + (hasBoot ? "  [有自启]" : ""));
            tvPkg.setText(ai.packageName);
            return v;
        }
    }
}