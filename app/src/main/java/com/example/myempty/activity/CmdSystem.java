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
import java.util.List;
import java.util.Locale;

public class CmdSystem {

    private final LocalShell sh;
    private final Context ctx;
    private final LocalShell.OnOutput out;

    public CmdSystem(LocalShell sh, Context ctx, LocalShell.OnOutput out) {
        this.sh = sh;
        this.ctx = ctx;
        this.out = out;
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
        return false;
    }

    private boolean ps(String[] p) {
        try {
            ActivityManager am = (ActivityManager)
                ctx.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> list = am.getRunningAppProcesses();
            sh.println("PID      进程名");
            if (list != null) {
                for (ActivityManager.RunningAppProcessInfo info : list) {
                    sh.println(info.pid + "      " + info.processName);
                }
            }
        } catch (Exception e) {
            sh.printlnErr("ps 失败: " + e.getMessage());
        }
        return true;
    }

    private boolean uname(String[] p) {
        sh.println("Android 版本: " + Build.VERSION.RELEASE);
        sh.println("SDK: " + Build.VERSION.SDK_INT);
        sh.println("CPU 架构: " + Build.SUPPORTED_ABIS[0]);
        sh.println("主板: " + Build.BOARD);
        sh.println("品牌: " + Build.BRAND);
        sh.println("设备名: " + Build.DEVICE);
        sh.println("型号: " + Build.MODEL);
        sh.println("厂商: " + Build.MANUFACTURER);
        return true;
    }

    private boolean whoami(String[] p) {
        sh.println("u0_a" + android.os.Process.myUid());
        return true;
    }

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
            IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent i = ctx.registerReceiver(null, f);
            if (i == null) {
                sh.printlnErr("battery: 拿不到电量信息");
                return true;
            }
            int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            int volt = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);

            float percent = level * 100f / scale;
            String statusStr;
            if (status == BatteryManager.BATTERY_STATUS_CHARGING) statusStr = "充电中";
            else if (status == BatteryManager.BATTERY_STATUS_FULL) statusStr = "已充满";
            else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) statusStr = "放电中";
            else statusStr = "未知";

            sh.println("电量: " + (int) percent + "%");
            sh.println("状态: " + statusStr);
            sh.println("温度: " + (temp / 10f) + "°C");
            sh.println("电压: " + volt + "mV");
        } catch (Exception e) {
            sh.printlnErr("battery 失败: " + e.getMessage());
        }
        return true;
    }

    private boolean cpu(String[] p) {
        try {
            File f = new File("/proc/cpuinfo");
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("processor")) count++;
                if (line.startsWith("Hardware") || line.startsWith("model name")) {
                    sh.println(line);
                }
            }
            br.close();
            sh.println("核心数: " + count);
            sh.println("架构: " + Build.SUPPORTED_ABIS[0]);
        } catch (Exception e) {
            sh.printlnErr("cpu 失败: " + e.getMessage());
        }
        return true;
    }

    private boolean mem(String[] p) {
        try {
            File f = new File("/proc/meminfo");
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("MemTotal") || line.startsWith("MemFree")
                    || line.startsWith("MemAvailable") || line.startsWith("Buffers")
                    || line.startsWith("Cached")) {
                    sh.println(line);
                }
            }
            br.close();
        } catch (Exception e) {
            sh.printlnErr("mem 失败: " + e.getMessage());
        }
        return true;
    }

    private boolean disk(String[] p) {
        try {
            File data = Environment.getDataDirectory();
            StatFs stat = new StatFs(data.getPath());
            long total = stat.getTotalBytes();
            long free = stat.getAvailableBytes();
            sh.println("数据分区：");
            sh.println("  总大小: " + (total / 1024 / 1024) + " MB");
            sh.println("  已用: " + ((total - free) / 1024 / 1024) + " MB");
            sh.println("  可用: " + (free / 1024 / 1024) + " MB");

            File ext = Environment.getExternalStorageDirectory();
            StatFs stat2 = new StatFs(ext.getPath());
            long total2 = stat2.getTotalBytes();
            long free2 = stat2.getAvailableBytes();
            sh.println("外部存储：");
            sh.println("  总大小: " + (total2 / 1024 / 1024) + " MB");
            sh.println("  可用: " + (free2 / 1024 / 1024) + " MB");
        } catch (Exception e) {
            sh.printlnErr("disk 失败: " + e.getMessage());
        }
        return true;
    }

    private boolean net(String[] p) {
        try {
            android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager)
                ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ipInt = wm.getConnectionInfo().getIpAddress();
            String ip = (ipInt & 0xFF) + "." + ((ipInt >> 8) & 0xFF)
                + "." + ((ipInt >> 16) & 0xFF) + "." + ((ipInt >> 24) & 0xFF);
            sh.println("WiFi IP: " + ip);

            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null) {
                sh.println("网络类型: " + ni.getTypeName());
                sh.println("已连接: " + ni.isConnected());
            }
        } catch (Exception e) {
            sh.printlnErr("net 失败: " + e.getMessage());
        }
        return true;
    }

    private boolean app(String[] p) {
        try {
            PackageManager pm = ctx.getPackageManager();
            List<ApplicationInfo> list = pm.getInstalledApplications(0);
            sh.println("已装应用共 " + list.size() + " 个：");
            for (ApplicationInfo ai : list) {
                boolean isSys = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                sh.println(ai.packageName + (isSys ? "  [系统]" : ""));
            }
        } catch (Exception e) {
            sh.printlnErr("app 失败: " + e.getMessage());
        }
        return true;
    }

    private boolean uptime(String[] p) {
        long up = android.os.SystemClock.elapsedRealtime() / 1000;
        long h = up / 3600;
        long m = (up % 3600) / 60;
        long s = up % 60;
        sh.println("开机时长: " + h + " 小时 " + m + " 分 " + s + " 秒");
        return true;
    }

    private boolean hostname(String[] p) {
        sh.println(Build.MODEL);
        return true;
    }

    private boolean locale(String[] p) {
        sh.println("语言: " + Locale.getDefault().getDisplayLanguage());
        sh.println("国家: " + Locale.getDefault().getCountry());
        sh.println("时区: " + java.util.TimeZone.getDefault().getID());
        return true;
    }

    private boolean export(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: export KEY=VALUE   —— 设环境变量");
            return true;
        }
        String kv = p[1];
        int eq = kv.indexOf('=');
        if (eq <= 0) {
            sh.printlnErr("export: 格式要写成 KEY=VALUE");
            return true;
        }
        sh.getVars().put(kv.substring(0, eq), kv.substring(eq + 1));
        sh.printlnOk("已设置: " + kv);
        return true;
    }
}