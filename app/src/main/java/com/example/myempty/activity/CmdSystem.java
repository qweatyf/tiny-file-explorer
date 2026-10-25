package com.example.myempty.activity2;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CmdSystem {

    private final LocalShell sh;
    private final Context ctx;
    private final LocalShell.OnOutput out;

    public CmdSystem(LocalShell sh, Context ctx, LocalShell.OnOutput out) {
        this.sh = sh; this.ctx = ctx; this.out = out;
    }

    public boolean exec(String[] p) {
        String c = p[0];
        if (c.equals("ps")) return ps(p);
        if (c.equals("uname")) return uname(p);
        if (c.equals("whoami")) return whoami(p);
        if (c.equals("id")) return id(p);
        if (c.equals("env")) return env(p);
        if (c.equals("battery")) return battery(p);
        if (c.equals("cpu")) return cpu(p);
        if (c.equals("mem")) return mem(p);
        if (c.equals("disk")) return disk(p);
        if (c.equals("net")) return net(p);
        if (c.equals("app")) return app(p);
        if (c.equals("uptime")) return uptime(p);
        if (c.equals("hostname")) return hostname(p);
        if (c.equals("locale")) return locale(p);
        if (c.equals("export")) return export(p);
        if (c.equals("top")) return top(p);
        if (c.equals("sysinfo")) return sysInfo(p);
        if (c.equals("sensors")) return sensors(p);
        if (c.equals("ipaddr")) return ipaddr(p);
        if (c.equals("cal")) return cal(p);
        if (c.equals("dateu")) return dateU(p);
        if (c.equals("uptimep")) return uptimeP(p);
        if (c.equals("hostinfo")) return hostInfo(p);
        return false;
    }

    private boolean ps(String[] p) {
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> list = am.getRunningAppProcesses();
            sh.println("PID      进程名");
            if (list != null) for (ActivityManager.RunningAppProcessInfo info : list)
                sh.println(info.pid + "      " + info.processName);
        } catch (Exception e) { sh.printlnErr("ps: " + e.getMessage()); }
        return true;
    }

    private boolean uname(String[] p) {
        sh.println("Android: " + Build.VERSION.RELEASE);
        sh.println("SDK: " + Build.VERSION.SDK_INT);
        sh.println("架构: " + Build.SUPPORTED_ABIS[0]);
        sh.println("品牌: " + Build.BRAND);
        sh.println("型号: " + Build.MODEL);
        return true;
    }

    private boolean whoami(String[] p) { sh.println("u0_a" + android.os.Process.myUid()); return true; }

    private boolean id(String[] p) {
        sh.println("uid=" + android.os.Process.myUid());
        sh.println("pid=" + android.os.Process.myPid());
        sh.println("tid=" + android.os.Process.myTid());
        return true;
    }

    private boolean env(String[] p) {
        sh.println("PATH=" + System.getenv("PATH"));
        sh.println("HOME=" + System.getenv("HOME"));
        sh.println("当前目录=" + sh.getCwd());
        return true;
    }

    private boolean battery(String[] p) {
        try {
            Intent i = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (i == null) { sh.printlnErr("拿不到电量"); return true; }
            int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            int volt = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            String statusStr;
            if (status == BatteryManager.BATTERY_STATUS_CHARGING) statusStr = "充电中";
            else if (status == BatteryManager.BATTERY_STATUS_FULL) statusStr = "已满";
            else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) statusStr = "放电中";
            else statusStr = "未知";
            sh.println("电量: " + (level * 100 / scale) + "%");
            sh.println("状态: " + statusStr);
            sh.println("温度: " + (temp / 10f) + "°C");
            sh.println("电压: " + volt + "mV");
        } catch (Exception e) { sh.printlnErr("battery: " + e.getMessage()); }
        return true;
    }

    private boolean cpu(String[] p) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("processor")) count++;
                if (line.startsWith("Hardware") || line.startsWith("model name")) sh.println(line);
            }
            br.close();
            sh.println("核心数: " + count);
            sh.println("架构: " + Build.SUPPORTED_ABIS[0]);
        } catch (Exception e) { sh.printlnErr("cpu: " + e.getMessage()); }
        return true;
    }

    private boolean mem(String[] p) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("MemTotal") || line.startsWith("MemFree")
                    || line.startsWith("MemAvailable") || line.startsWith("Buffers")
                    || line.startsWith("Cached")) sh.println(line);
            }
            br.close();
        } catch (Exception e) { sh.printlnErr("mem: " + e.getMessage()); }
        return true;
    }

    private boolean disk(String[] p) {
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            long total = stat.getTotalBytes(), free = stat.getAvailableBytes();
            sh.println("数据分区：");
            sh.println("  总: " + (total / 1024 / 1024) + " MB");
            sh.println("  已用: " + ((total - free) / 1024 / 1024) + " MB");
            sh.println("  可用: " + (free / 1024 / 1024) + " MB");

            StatFs stat2 = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long total2 = stat2.getTotalBytes(), free2 = stat2.getAvailableBytes();
            sh.println("外部存储：");
            sh.println("  总: " + (total2 / 1024 / 1024) + " MB");
            sh.println("  可用: " + (free2 / 1024 / 1024) + " MB");
        } catch (Exception e) { sh.printlnErr("disk: " + e.getMessage()); }
        return true;
    }

    private boolean net(String[] p) {
        try {
            android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager)
                ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int i = wm.getConnectionInfo().getIpAddress();
            sh.println("WiFi IP: " + (i & 0xFF) + "." + ((i >> 8) & 0xFF)
                + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF));
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null) {
                sh.println("类型: " + ni.getTypeName());
                sh.println("已连接: " + ni.isConnected());
            }
        } catch (Exception e) { sh.printlnErr("net: " + e.getMessage()); }
        return true;
    }

    private boolean app(String[] p) {
        try {
            PackageManager pm = ctx.getPackageManager();
            List<ApplicationInfo> list = pm.getInstalledApplications(0);
            sh.println("已装 " + list.size() + " 个：");
            for (ApplicationInfo ai : list) {
                boolean isSys = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                sh.println(ai.packageName + (isSys ? "  [系统]" : ""));
            }
        } catch (Exception e) { sh.printlnErr("app: " + e.getMessage()); }
        return true;
    }

    private boolean uptime(String[] p) {
        long up = android.os.SystemClock.elapsedRealtime() / 1000;
        sh.println("开机时长: " + (up / 3600) + " 小时 "
            + ((up % 3600) / 60) + " 分 " + (up % 60) + " 秒");
        return true;
    }

    private boolean hostname(String[] p) { sh.println(Build.MODEL); return true; }

    private boolean locale(String[] p) {
        sh.println("语言: " + Locale.getDefault().getDisplayLanguage());
        sh.println("国家: " + Locale.getDefault().getCountry());
        sh.println("时区: " + TimeZone.getDefault().getID());
        return true;
    }

    private boolean export(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: export KEY=VALUE"); return true; }
        String kv = p[1];
        int eq = kv.indexOf('=');
        if (eq <= 0) { sh.printlnErr("export: 要 KEY=VALUE"); return true; }
        sh.getVars().put(kv.substring(0, eq), kv.substring(eq + 1));
        sh.printlnOk("已设: " + kv);
        return true;
    }

    private boolean top(String[] p) {
        int n = p.length >= 2 ? LocalShell.parseInt(p[1], 10) : 10;
        if (n < 1) n = 10;
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> list = am.getRunningAppProcesses();
            if (list == null) { sh.printlnErr("top: 拿不到进程"); return true; }
            List<ActivityManager.RunningAppProcessInfo> sorted =
                new ArrayList<>(list);
            Collections.sort(sorted, (a, b) -> {
                android.os.Debug.MemoryInfo[] mi1 = am.getProcessMemoryInfo(new int[]{a.pid});
                android.os.Debug.MemoryInfo[] mi2 = am.getProcessMemoryInfo(new int[]{b.pid});
                int s1 = mi1.length > 0 ? mi1[0].getTotalPss() : 0;
                int s2 = mi2.length > 0 ? mi2[0].getTotalPss() : 0;
                return s2 - s1;
            });
            sh.println("PSS      进程");
            for (int i = 0; i < Math.min(n, sorted.size()); i++) {
                ActivityManager.RunningAppProcessInfo info = sorted.get(i);
                android.os.Debug.MemoryInfo[] mi = am.getProcessMemoryInfo(new int[]{info.pid});
                int pss = mi.length > 0 ? mi[0].getTotalPss() : 0;
                sh.println(pss + " KB   " + info.processName);
            }
        } catch (Exception e) { sh.printlnErr("top: " + e.getMessage()); }
        return true;
    }

    private boolean sysInfo(String[] p) {
        sh.println("=== 系统 ===");
        sh.println("Android: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
        sh.println("架构: " + Build.SUPPORTED_ABIS[0]);
        sh.println("品牌: " + Build.BRAND);
        sh.println("型号: " + Build.MODEL);
        sh.println("厂商: " + Build.MANUFACTURER);
        sh.println("");

        sh.println("=== 内存 ===");
        try {
            RandomAccessFile raf = new RandomAccessFile("/proc/meminfo", "r");
            String line;
            while ((line = raf.readLine()) != null) {
                if (line.startsWith("MemTotal") || line.startsWith("MemAvailable")) sh.println(line);
            }
            raf.close();
        } catch (Exception ignored) {}
        sh.println("");

        sh.println("=== 存储 ===");
        try {
            StatFs st = new StatFs(Environment.getDataDirectory().getPath());
            sh.println("数据分区可用: " + (st.getAvailableBytes() / 1024 / 1024) + " MB");
        } catch (Exception ignored) {}

        sh.println("");
        sh.println("=== 电池 ===");
        try {
            Intent i = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (i != null) {
                int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                sh.println("电量: " + (level * 100 / scale) + "%");
            }
        } catch (Exception ignored) {}
        return true;
    }

    private boolean sensors(String[] p) {
        int found = 0;
        for (int i = 0; i < 30; i++) {
            String path = "/sys/class/thermal/thermal_zone" + i + "/temp";
            String type = "/sys/class/thermal/thermal_zone" + i + "/type";
            try {
                BufferedReader br = new BufferedReader(new FileReader(path));
                String v = br.readLine();
                br.close();
                String t = "unknown";
                try {
                    BufferedReader br2 = new BufferedReader(new FileReader(type));
                    t = br2.readLine();
                    br2.close();
                } catch (Exception ignored) {}
                if (v != null) {
                    double temp = Double.parseDouble(v.trim());
                    if (temp > 1000) temp /= 1000;
                    sh.println(t + ": " + String.format("%.1f", temp) + " °C");
                    found++;
                }
            } catch (Exception ignored) {}
        }
        if (found == 0) sh.println("这机子不给读温度");
        return true;
    }

    private boolean ipaddr(String[] p) {
        try {
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface nif : Collections.list(nifs)) {
                if (nif.isLoopback() || !nif.isUp()) continue;
                sh.println(nif.getName() + ":");
                for (InetAddress addr : Collections.list(nif.getInetAddresses()))
                    sh.println("  " + addr.getHostAddress());
            }
        } catch (Exception e) { sh.printlnErr("ipaddr: " + e.getMessage()); }
        return true;
    }

    private boolean cal(String[] p) {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int today = c.get(Calendar.DAY_OF_MONTH);

        Calendar first = Calendar.getInstance();
        first.set(year, month, 1);
        int firstDayOfWeek = first.get(Calendar.DAY_OF_WEEK);
        int maxDay = first.getActualMaximum(Calendar.DAY_OF_MONTH);

        sh.println(String.format("%d 年 %d 月", year, month + 1));
        sh.println("日 一 二 三 四 五 六");

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < firstDayOfWeek; i++) sb.append("   ");
        for (int d = 1; d <= maxDay; d++) {
            sb.append(String.format("%2d", d));
            if (d == today) sb.append("*");
            else sb.append(" ");
            if ((firstDayOfWeek - 1 + d) % 7 == 0) {
                sh.println(sb.toString());
                sb.setLength(0);
            }
        }
        if (sb.length() > 0) sh.println(sb.toString());
        return true;
    }

    private boolean dateU(String[] p) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        sh.println("UTC: " + sdf.format(new java.util.Date()));
        return true;
    }

    private boolean uptimeP(String[] p) {
        long up = android.os.SystemClock.elapsedRealtime() / 1000;
        long h = up / 3600, m = (up % 3600) / 60, s = up % 60;
        StringBuilder sb = new StringBuilder("开机 ");
        if (h > 0) sb.append(h).append(" 小时 ");
        if (m > 0) sb.append(m).append(" 分 ");
        sb.append(s).append(" 秒");
        sh.println(sb.toString());
        return true;
    }

    private boolean hostInfo(String[] p) {
        sh.println("品牌: " + Build.BRAND);
        sh.println("厂商: " + Build.MANUFACTURER);
        sh.println("型号: " + Build.MODEL);
        sh.println("设备: " + Build.DEVICE);
        sh.println("主板: " + Build.BOARD);
        sh.println("硬件: " + Build.HARDWARE);
        sh.println("指纹: " + Build.FINGERPRINT);
        sh.println("构建ID: " + Build.ID);
        sh.println("Android: " + Build.VERSION.RELEASE);
        sh.println("SDK: " + Build.VERSION.SDK_INT);
        sh.println("架构: " + Build.SUPPORTED_ABIS[0]);
        return true;
    }
}