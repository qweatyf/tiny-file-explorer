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
import java.util.List;

public class AppCleanActivity extends Activity {

    private ListView lv;
    private EditText etSearch;
    private final List<ApplicationInfo> all = new ArrayList<>();
    private final List<ApplicationInfo> shown = new ArrayList<>();
    private Adapter adapter;
    private PackageManager pm;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_app_clean);

        pm = getPackageManager();
        lv = findViewById(R.id.lv_clean_list);
        etSearch = findViewById(R.id.et_clean_search);

        adapter = new Adapter();
        lv.setAdapter(adapter);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            ApplicationInfo ai = shown.get(position);
            showActions(ai);
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
        List<ApplicationInfo> list = pm.getInstalledApplications(0);
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

    private void showActions(final ApplicationInfo ai) {
        String label = pm.getApplicationLabel(ai).toString();
        String[] ops = {"清缓存", "清数据", "两个都清"};
        new AlertDialog.Builder(this)
            .setTitle(label)
            .setItems(ops, (d, w) -> {
                if (w == 0) doClean(ai, false);
                else if (w == 1) doClean(ai, true);
                else doClean(ai, true, false);
            })
            .show();
    }

    private void doClean(ApplicationInfo ai, boolean clearData) {
        doClean(ai, true, clearData);
    }

    private void doClean(final ApplicationInfo ai, final boolean clearCache, final boolean clearData) {
        new AlertDialog.Builder(this)
            .setTitle("确定？")
            .setMessage("清完后 App 可能被重置")
            .setPositiveButton("清", (d, w) -> {
                new Thread(() -> {
                    try {
                        if (clearCache) {
                            runRoot("pm clear --cache-only " + ai.packageName);
                        }
                        if (clearData) {
                            runRoot("pm clear " + ai.packageName);
                        }
                        runOnUiThread(() -> Toast.makeText(this,
                            "清完了", Toast.LENGTH_SHORT).show());
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(this,
                            "失败: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());
                    }
                }).start();
            })
            .setNegativeButton("算了", null)
            .show();
    }

    private void runRoot(String cmd) throws Exception {
        Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {}
        br.close();
        p.waitFor();
    }

    class Adapter extends BaseAdapter {
        @Override public int getCount() { return shown.size(); }
        @Override public ApplicationInfo getItem(int i) { return shown.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(AppCleanActivity.this)
                .inflate(R.layout.item_apk, parent, false);
            ApplicationInfo ai = getItem(pos);
            ImageView iv = v.findViewById(R.id.iv_apk_icon);
            TextView tvName = v.findViewById(R.id.tv_apk_name);
            TextView tvPkg = v.findViewById(R.id.tv_apk_pkg);
            iv.setImageDrawable(pm.getApplicationIcon(ai));
            tvName.setText(pm.getApplicationLabel(ai).toString());
            tvPkg.setText(ai.packageName);
            return v;
        }
    }
}