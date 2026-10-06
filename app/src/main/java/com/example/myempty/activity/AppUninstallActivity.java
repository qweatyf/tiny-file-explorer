package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppUninstallActivity extends Activity {

    private ListView lv;
    private EditText etSearch;
    private final List<ApplicationInfo> all = new ArrayList<>();
    private final List<ApplicationInfo> shown = new ArrayList<>();
    private Adapter adapter;
    private PackageManager pm;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_app_uninstall);

        pm = getPackageManager();
        lv = findViewById(R.id.lv_uninstall_list);
        etSearch = findViewById(R.id.et_uninstall_search);

        adapter = new Adapter();
        lv.setAdapter(adapter);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            ApplicationInfo ai = shown.get(position);
            confirmUninstall(ai);
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

    private void confirmUninstall(final ApplicationInfo ai) {
        String label = pm.getApplicationLabel(ai).toString();
        new AlertDialog.Builder(this)
            .setTitle("卸载")
            .setMessage("确定卸载 " + label + " 吗？")
            .setPositiveButton("卸载", (d, w) -> {
                try {
                    Intent i = new Intent(Intent.ACTION_DELETE,
                        Uri.parse("package:" + ai.packageName));
                    startActivity(i);
                } catch (Exception e) {
                    android.widget.Toast.makeText(this,
                        "打不开卸载界面: " + e.getMessage(),
                        android.widget.Toast.LENGTH_LONG).show();
                }
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
            View v = LayoutInflater.from(AppUninstallActivity.this)
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