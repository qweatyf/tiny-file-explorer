package com.example.myempty.activity2;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Vibrator;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class CmdTool {

    private final LocalShell sh;
    private final Context ctx;
    private final LocalShell.OnOutput out;

    public CmdTool(LocalShell sh, Context ctx, LocalShell.OnOutput out) {
        this.sh = sh;
        this.ctx = ctx;
        this.out = out;
    }

    public boolean exec(String[] p) {
        String c = p[0];
        if (c.equals("calc")) return calc(p);
        if (c.equals("unit")) return unit(p);
        if (c.equals("rand")) return rand(p);
        if (c.equals("uuid")) return uuid(p);
        if (c.equals("pass")) return pass(p);
        if (c.equals("json")) return json(p);
        if (c.equals("timestamp")) return timestamp(p);
        if (c.equals("zip")) return zip(p);
        if (c.equals("unzip")) return unzip(p);
        if (c.equals("timer")) return timer(p);
        if (c.equals("count")) return count(p);
        if (c.equals("sum")) return sum(p);
        if (c.equals("avg")) return avg(p);
        if (c.equals("min")) return min(p);
        if (c.equals("max")) return max(p);
        if (c.equals("open")) return open(p);
        if (c.equals("share")) return share(p);
        if (c.equals("copy")) return copy(p);
        if (c.equals("toast")) return toast(p);
        if (c.equals("vibrate")) return vibrate(p);
        if (c.equals("alias")) return alias(p);
        if (c.equals("source")) return source(p);
        if (c.equals("bash")) return source(p);
        if (c.equals("sh")) return source(p);
        return false;
    }

    private boolean calc(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: calc <表达式>      —— 算数，支持 + - * / % 和括号");
            return true;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < p.length; i++) {
            if (i > 1) sb.append(" ");
            sb.append(p[i]);
        }
        String expr = sb.toString();
        try {
            double result = eval(expr);
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                sh.println(String.valueOf((long) result));
            } else {
                sh.println(String.valueOf(result));
            }
        } catch (Exception e) {
            sh.printlnErr("calc 算不了: " + e.getMessage());
        }
        return true;
    }

    private double eval(String s) {
        return new Object() {
            int pos = -1, ch;
            void next() { ch = (++pos < s.length()) ? s.charAt(pos) : -1; }
            boolean eat(int c) {
                while (ch == ' ') next();
                if (ch == c) { next(); return true; }
                return false;
            }
            double parse() {
                next();
                double x = expr();
                if (pos < s.length()) throw new RuntimeException("多余的字符: " + (char) ch);
                return x;
            }
            double expr() {
                double x = term();
                for (;;) {
                    if (eat('+')) x += term();
                    else if (eat('-')) x -= term();
                    else return x;
                }
            }
            double term() {
                double x = factor();
                for (;;) {
                    if (eat('*')) x *= factor();
                    else if (eat('/')) x /= factor();
                    else if (eat('%')) x %= factor();
                    else return x;
                }
            }
            double factor() {
                if (eat('+')) return factor();
                if (eat('-')) return -factor();
                int start = pos;
                if (eat('(')) {
                    double x = expr();
                    eat(')');
                    return x;
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') next();
                    return Double.parseDouble(s.substring(start, pos));
                }
                throw new RuntimeException("不认识的字符");
            }
        }.parse();
    }

    private boolean unit(String[] p) {
        if (p.length < 4) {
            sh.printlnErr("用法: unit <数> <原单位> <目标单位>  —— 单位换算");
            sh.printlnErr("支持: b kb mb gb tb / mm cm m km");
            return true;
        }
        double n = Double.parseDouble(p[1]);
        String from = p[2].toLowerCase();
        String to = p[3].toLowerCase();

        double bytes = toBytes(n, from);
        double result = fromBytes(bytes, to);
        sh.println(n + " " + from + " = " + result + " " + to);
        return true;
    }

    private double toBytes(double n, String u) {
        switch (u) {
            case "b": return n;
            case "kb": return n * 1024;
            case "mb": return n * 1024 * 1024;
            case "gb": return n * 1024 * 1024 * 1024;
            case "tb": return n * 1024L * 1024 * 1024 * 1024;
            case "mm": return n / 1000;
            case "cm": return n / 100;
            case "m": return n;
            case "km": return n * 1000;
            default: throw new RuntimeException("不认识单位: " + u);
        }
    }

    private double fromBytes(double b, String u) {
        switch (u) {
            case "b": return b;
            case "kb": return b / 1024;
            case "mb": return b / 1024 / 1024;
            case "gb": return b / 1024 / 1024 / 1024;
            case "tb": return b / 1024L / 1024 / 1024 / 1024;
            case "mm": return b * 1000;
            case "cm": return b * 100;
            case "m": return b;
            case "km": return b / 1000;
            default: throw new RuntimeException("不认识单位: " + u);
        }
    }

    private boolean rand(String[] p) {
        int max = p.length >= 2 ? LocalShell.parseInt(p[1], 100) : 100;
        int n = new Random().nextInt(max + 1);
        sh.println(String.valueOf(n));
        return true;
    }

    private boolean uuid(String[] p) {
        sh.println(UUID.randomUUID().toString());
        return true;
    }

    private boolean pass(String[] p) {
        int len = p.length >= 2 ? LocalShell.parseInt(p[1], 16) : 16;
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*";
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        sh.println(sb.toString());
        return true;
    }

    private boolean json(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: json <文件>        —— 格式化 JSON");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("json: 文件不存在: " + p[1]);
            return true;
        }
        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            int read = 0;
            while (read < buf.length) {
                int n = fis.read(buf, read, buf.length - read);
                if (n < 0) break;
                read += n;
            }
            fis.close();
            String s = new String(buf, 0, read, "UTF-8");
            sh.println(prettyJson(s));
        } catch (Exception e) {
            sh.printlnErr("json 失败: " + e.getMessage());
        }
        return true;
    }

    private String prettyJson(String s) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean inStr = false;
        for (char c : s.toCharArray()) {
            if (c == '"') inStr = !inStr;
            if (!inStr) {
                if (c == '{' || c == '[') {
                    sb.append(c).append("\n");
                    indent++;
                    for (int i = 0; i < indent; i++) sb.append("  ");
                } else if (c == '}' || c == ']') {
                    sb.append("\n");
                    indent--;
                    for (int i = 0; i < indent; i++) sb.append("  ");
                    sb.append(c);
                } else if (c == ',') {
                    sb.append(c).append("\n");
                    for (int i = 0; i < indent; i++) sb.append("  ");
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean timestamp(String[] p) {
        if (p.length < 2) {
            long now = System.currentTimeMillis() / 1000;
            sh.println("当前时间戳: " + now);
            sh.println("毫秒: " + System.currentTimeMillis());
            return true;
        }
        try {
            long ts = Long.parseLong(p[1]);
            if (ts < 10000000000L) ts *= 1000;
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            sh.println(sdf.format(new java.util.Date(ts)));
        } catch (Exception e) {
            sh.printlnErr("timestamp 转换失败: " + e.getMessage());
        }
        return true;
    }

    private boolean zip(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: zip <输出.zip> <目录>  —— 压缩目录");
            return true;
        }
        File out = sh.resolve(p[1]);
        File src = sh.resolve(p[2]);
        if (!src.exists()) {
            sh.printlnErr("zip: 源目录不存在: " + p[2]);
            return true;
        }
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out));
            zipDir(src, src, zos);
            zos.close();
            sh.printlnOk("已压缩: " + out.getAbsolutePath());
        } catch (Exception e) {
            sh.printlnErr("zip 失败: " + e.getMessage());
        }
        return true;
    }

    private void zipDir(File root, File file, ZipOutputStream zos) throws Exception {
        if (file.isDirectory()) {
            File[] kids = file.listFiles();
            if (kids == null) return;
            for (File k : kids) zipDir(root, k, zos);
        } else {
            String path = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
            zos.putNextEntry(new ZipEntry(path));
            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) > 0) zos.write(buf, 0, n);
            fis.close();
            zos.closeEntry();
        }
    }

    private boolean unzip(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: unzip <文件.zip>   —— 解压");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("unzip: 文件不存在: " + p[1]);
            return true;
        }
        try {
            File outDir = new File(f.getParent(), f.getName().replace(".zip", ""));
            if (!outDir.exists()) outDir.mkdirs();
            ZipInputStream zis = new ZipInputStream(new FileInputStream(f));
            ZipEntry e;
            byte[] buf = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                if (e.getName().contains("..")) continue;
                File outFile = new File(outDir, e.getName());
                if (e.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(outFile);
                    int n;
                    while ((n = zis.read(buf)) > 0) fos.write(buf, 0, n);
                    fos.close();
                }
            }
            zis.close();
            sh.printlnOk("已解压到: " + outDir.getAbsolutePath());
        } catch (Exception e) {
            sh.printlnErr("unzip 失败: " + e.getMessage());
        }
        return true;
    }

    private boolean timer(String[] p) {
        int seconds = p.length >= 2 ? LocalShell.parseInt(p[1], 10) : 10;
        sh.printlnOk("倒计时 " + seconds + " 秒...");
        new Thread(() -> {
            try {
                Thread.sleep(seconds * 1000L);
                sh.printlnOk("时间到！");
                Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) v.vibrate(500);
            } catch (Exception ignored) {}
        }).start();
        return true;
    }

    private boolean count(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: count <文件>       —— 算行数");
            return true;
        }
        File f = sh.resolve(p[1]);
        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            int read = 0;
            while (read < buf.length) {
                int n = fis.read(buf, read, buf.length - read);
                if (n < 0) break;
                read += n;
            }
            fis.close();
            String s = new String(buf, 0, read, "UTF-8");
            sh.println("行数: " + s.split("\n").length);
        } catch (Exception e) {
            sh.printlnErr("count 失败: " + e.getMessage());
        }
        return true;
    }

    private boolean sum(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: sum <数字...>      —— 求和");
            return true;
        }
        double s = 0;
        for (int i = 1; i < p.length; i++) s += Double.parseDouble(p[i]);
        sh.println(String.valueOf(s));
        return true;
    }

    private boolean avg(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: avg <数字...>      —— 求平均");
            return true;
        }
        double s = 0;
        for (int i = 1; i < p.length; i++) s += Double.parseDouble(p[i]);
        sh.println(String.valueOf(s / (p.length - 1)));
        return true;
    }

    private boolean min(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: min <数字...>      —— 求最小");
            return true;
        }
        double m = Double.MAX_VALUE;
        for (int i = 1; i < p.length; i++) m = Math.min(m, Double.parseDouble(p[i]));
        sh.println(String.valueOf(m));
        return true;
    }

    private boolean max(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: max <数字...>      —— 求最大");
            return true;
        }
        double m = Double.MIN_VALUE;
        for (int i = 1; i < p.length; i++) m = Math.max(m, Double.parseDouble(p[i]));
        sh.println(String.valueOf(m));
        return true;
    }

    private boolean open(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: open <文件>        —— 用别的 App 打开");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("open: 文件不存在: " + p[1]);
            return true;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.fromFile(f), "*/*");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            sh.printlnOk("已打开");
        } catch (Exception e) {
            sh.printlnErr("open 失败: " + e.getMessage());
        }
        return true;
    }

    private boolean share(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: share <文件>       —— 分享文件");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("share: 文件不存在: " + p[1]);
            return true;
        }
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(Intent.createChooser(i, "分享").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            sh.printlnOk("已打开分享");
        } catch (Exception e) {
            sh.printlnErr("share 失败: " + e.getMessage());
        }
        return true;
    }

    private boolean copy(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: copy <文本>        —— 复制到剪贴板");
            return true;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < p.length; i++) {
            if (i > 1) sb.append(" ");
            sb.append(p[i]);
        }
        ClipboardManager cm = (ClipboardManager)
            ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("term", sb.toString()));
        sh.printlnOk("已复制到剪贴板");
        return true;
    }

    private boolean toast(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: toast <文本>       —— 弹提示");
            return true;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < p.length; i++) {
            if (i > 1) sb.append(" ");
            sb.append(p[i]);
        }
        Toast.makeText(ctx, sb.toString(), Toast.LENGTH_SHORT).show();
        return true;
    }

    private boolean vibrate(String[] p) {
        int ms = p.length >= 2 ? LocalShell.parseInt(p[1], 200) : 200;
        Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(ms);
            sh.printlnOk("已震动 " + ms + "ms");
        }
        return true;
    }

    private boolean alias(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: alias <名字> <命令>  —— 设别名");
            return true;
        }
        sh.getVars().put("alias_" + p[1], p[2]);
        sh.printlnOk("别名 " + p[1] + " = " + p[2]);
        return true;
    }

    private boolean source(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: " + p[0] + " <脚本.sh>  —— 跑脚本");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("source: 脚本不存在: " + p[1]);
            return true;
        }
        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            int read = 0;
            while (read < buf.length) {
                int n = fis.read(buf, read, buf.length - read);
                if (n < 0) break;
                read += n;
            }
            fis.close();
            String content = new String(buf, 0, read, "UTF-8");
            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                sh.println("> " + line);
                sh.exec(line);
            }
        } catch (Exception e) {
            sh.printlnErr("source 失败: " + e.getMessage());
        }
        return true;
    }
}