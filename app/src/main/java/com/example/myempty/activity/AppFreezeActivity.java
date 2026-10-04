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

public class AppFreezeActivity extends Activity {

    private ListView lv;
    private EditText etSearch;
    private final List<ApplicationInfo> all = new ArrayList<>();
    private final List<ApplicationInfo> shown = new ArrayList<>();
    private final Set<String> disabledSet = new HashSet<>();
    private Adapter adapter;
    private PackageManager pm;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_app_freeze);

        pm = getPackageManager();
        lv = findViewById(R.id.lv_freeze_list);
        etSearch = findViewById(R.id.et_freeze_search);

        adapter = new Adapter();
        lv.setAdapter(adapter);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            ApplicationInfo ai = shown.get(position);
            toggle(ai);
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
        loadDisabled();
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

    private void loadDisabled() {
        disabledSet.clear();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "pm list packages -d"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("package:")) {
                    disabledSet.add(line.substring(8).trim());
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

    private void toggle(final ApplicationInfo ai) {
        final boolean disabled = disabledSet.contains(ai.packageName);
        String label = pm.getApplicationLabel(ai).toString();
        String action = disabled ? "启用" : "冻结";
        new AlertDialog.Builder(this)
            .setTitle(action)
            .setMessage(action + " " + label + " ？")
            .setPositiveButton(action, (d, w) -> {
                new Thread(() -> {
                    String cmd = disabled
                        ? "pm enable " + ai.packageName
                        : "pm disable-user --user 0 " + ai.packageName;
                    try {
                        Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
                        p.waitFor();
                        runOnUiThread(() -> {
                            if (disabled) disabledSet.remove(ai.packageName);
                            else disabledSet.add(ai.packageName);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(this, action + "完成", Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(this,
                            action + "失败: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());
                    }
                }).start();
            })
            .setNegativeButton("算了", null)
            .show();
    }

    class Adapter extends BaseAdapter {
        @Override public int getCount() { return shown.size(); }
        @Override public ApplicationInfo getItem(int i) { return shown.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(AppFreezeActivity.this)
                .inflate(R.layout.item_apk, parent, false);
            ApplicationInfo ai = getItem(pos);
            ImageView iv = v.findViewById(R.id.iv_apk_icon);
            TextView tvName = v.findViewById(R.id.tv_apk_name);
            TextView tvPkg = v.findViewById(R.id.tv_apk_pkg);

            boolean disabled = disabledSet.contains(ai.packageName);
            iv.setImageDrawable(pm.getApplicationIcon(ai));
            tvName.setText(pm.getApplicationLabel(ai).toString() + (disabled ? "  [已冻结]" : ""));
            tvName.setTextColor(disabled ? 0xFF888888 : 0xFF333333);
            tvPkg.setText(ai.packageName);
            return v;
        }
    }
}