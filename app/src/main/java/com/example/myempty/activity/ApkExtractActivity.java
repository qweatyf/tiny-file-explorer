package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ApkExtractActivity extends Activity {

    private ListView lv;
    private EditText etSearch;
    private List<ApplicationInfo> all = new ArrayList<>();
    private List<ApplicationInfo> shown = new ArrayList<>();
    private Adapter adapter;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private PackageManager pm;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_apk_extract);

        pm = getPackageManager();
        lv = findViewById(R.id.lv_apk_list);
        etSearch = findViewById(R.id.et_apk_search);

        adapter = new Adapter();
        lv.setAdapter(adapter);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            ApplicationInfo ai = shown.get(position);
            showActions(ai);
        });

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                filter(s.toString());
            }
        });

        new Thread(this::loadApps).start();
    }

    private void loadApps() {
        List<ApplicationInfo> list = pm.getInstalledApplications(0);
        Collections.sort(list, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo a, ApplicationInfo b) {
                return pm.getApplicationLabel(a).toString()
                    .compareToIgnoreCase(pm.getApplicationLabel(b).toString());
            }
        });
        ui.post(() -> {
            all.clear();
            all.addAll(list);
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
            String pkg = ai.packageName.toLowerCase();
            if (name.contains(low) || pkg.contains(low)) {
                shown.add(ai);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showActions(final ApplicationInfo ai) {
        String label = pm.getApplicationLabel(ai).toString();
        String[] ops = {"提取 APK", "看详情"};

        new AlertDialog.Builder(this)
            .setTitle(label)
            .setItems(ops, (d, w) -> {
                if (w == 0) extractApk(ai);
                else showInfo(ai);
            })
            .show();
    }

    private void extractApk(final ApplicationInfo ai) {
        new Thread(() -> {
            try {
                File outDir = new File("/storage/emulated/0/Download/APK提取");
                if (!outDir.exists()) outDir.mkdirs();

                String label = pm.getApplicationLabel(ai).toString();
                String safeLabel = label.replaceAll("[\\\\/:*?\"<>|]", "_");

                File mainSrc = new File(ai.sourceDir);
                if (!mainSrc.exists() || !mainSrc.canRead()) {
                    ui.post(() -> Toast.makeText(this,
                        "读不了这个安装包", Toast.LENGTH_SHORT).show());
                    return;
                }

                File mainOut = new File(outDir, safeLabel + "_" + ai.packageName + ".apk");
                copyFile(mainSrc, mainOut);

                String[] splits = ai.splitSourceDirs;
                if (splits != null) {
                    for (int i = 0; i < splits.length; i++) {
                        File s = new File(splits[i]);
                        if (!s.exists()) continue;
                        File so = new File(outDir,
                            safeLabel + "_" + ai.packageName + "_split" + i + ".apk");
                        copyFile(s, so);
                    }
                }

                final File finalOut = mainOut;
                ui.post(() -> Toast.makeText(this,
                    "提好了: " + finalOut.getAbsolutePath(),
                    Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                ui.post(() -> Toast.makeText(this,
                    "提取失败: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void copyFile(File src, File dst) throws Exception {
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dst);
        byte[] buf = new byte[8192];
        int n;
        while ((n = fis.read(buf)) > 0) fos.write(buf, 0, n);
        fis.close();
        fos.close();
    }

    private void showInfo(ApplicationInfo ai) {
        try {
            String label = pm.getApplicationLabel(ai).toString();
            String ver = pm.getPackageInfo(ai.packageName, 0).versionName;
            long size = new File(ai.sourceDir).length();
            boolean isSys = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

            StringBuilder sb = new StringBuilder();
            sb.append("名字: ").append(label).append("\n");
            sb.append("包名: ").append(ai.packageName).append("\n");
            sb.append("版本: ").append(ver).append("\n");
            sb.append("大小: ").append(fmtSize(size)).append("\n");
            sb.append("类型: ").append(isSys ? "系统应用" : "普通应用").append("\n");
            sb.append("路径: ").append(ai.sourceDir).append("\n");

            new AlertDialog.Builder(this)
                .setTitle("应用详情")
                .setMessage(sb.toString())
                .setPositiveButton("知道了", null)
                .show();
        } catch (Exception e) {
            Toast.makeText(this, "读不了详情", Toast.LENGTH_SHORT).show();
        }
    }

    private String fmtSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) return String.format("%.1f MB", b / 1024.0 / 1024);
        return String.format("%.2f GB", b / 1024.0 / 1024 / 1024);
    }

    class Adapter extends BaseAdapter {
        @Override public int getCount() { return shown.size(); }
        @Override public ApplicationInfo getItem(int i) { return shown.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            View v = LayoutInflater.from(ApkExtractActivity.this)
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