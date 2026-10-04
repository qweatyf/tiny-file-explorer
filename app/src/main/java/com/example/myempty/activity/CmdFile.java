package com.example.myempty.activity2;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CmdFile {

    private final LocalShell sh;
    private final Context ctx;
    private final LocalShell.OnOutput out;

    public CmdFile(LocalShell sh, Context ctx, LocalShell.OnOutput out) {
        this.sh = sh;
        this.ctx = ctx;
        this.out = out;
    }

    public boolean exec(String[] p) {
        String c = p[0];

        if (c.equals("ls")) return ls(p);
        if (c.equals("cd")) return cd(p);
        if (c.equals("pwd")) return pwd(p);
        if (c.equals("cat")) return cat(p);
        if (c.equals("head")) return head(p);
        if (c.equals("tail")) return tail(p);
        if (c.equals("mkdir")) return mkdir(p);
        if (c.equals("rm")) return rm(p);
        if (c.equals("cp")) return cp(p);
        if (c.equals("mv")) return mv(p);
        if (c.equals("touch")) return touch(p);
        if (c.equals("find")) return find(p);
        if (c.equals("du")) return du(p);
        if (c.equals("df")) return df(p);
        if (c.equals("stat")) return stat(p);
        if (c.equals("tree")) return tree(p);
        if (c.equals("file")) return file(p);
        if (c.equals("ln")) return ln(p);
        if (c.equals("diff")) return diff(p);
        if (c.equals("split")) return split(p);
        if (c.equals("truncate")) return truncate(p);
        if (c.equals("basename")) return basename(p);
        if (c.equals("dirname")) return dirname(p);
        return false;
    }

    private boolean ls(String[] p) {
        File dir = sh.resolve(null);
        boolean longFmt = false;
        List<String> targets = new ArrayList<>();
        for (int i = 1; i < p.length; i++) {
            if (p[i].startsWith("-")) {
                if (p[i].contains("l") || p[i].contains("a")) longFmt = true;
            } else {
                targets.add(p[i]);
            }
        }
        if (!targets.isEmpty()) dir = sh.resolve(targets.get(0));

        if (!dir.exists()) {
            sh.printlnErr("ls: 路径不存在: " + dir.getAbsolutePath());
            return true;
        }
        if (!dir.isDirectory()) {
            sh.println(dir.getName());
            return true;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            sh.printlnErr("ls: 打不开目录（可能没权限）: " + dir.getAbsolutePath());
            return true;
        }
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        for (File f : files) {
            if (longFmt) {
                String perm = (f.isDirectory() ? "d" : "-")
                    + (f.canRead() ? "r" : "-")
                    + (f.canWrite() ? "w" : "-")
                    + "x";
                String size = f.isDirectory() ? "-" : String.valueOf(f.length());
                String time = sdf.format(new Date(f.lastModified()));
                sh.println(perm + "  " + size + "  " + time + "  " + f.getName()
                    + (f.isDirectory() ? "/" : ""));
            } else {
                sh.println(f.getName() + (f.isDirectory() ? "/" : ""));
            }
        }
        return true;
    }

    private boolean cd(String[] p) {
        String target = p.length >= 2 ? p[1] : "/storage/emulated/0/Download";
        File dir = sh.resolve(target);
        if (!dir.exists() || !dir.isDirectory()) {
            sh.printlnErr("cd: 没这个目录: " + target);
            return true;
        }
        sh.setCwd(dir.getAbsolutePath());
        return true;
    }

    private boolean pwd(String[] p) {
        sh.println(sh.getCwd());
        return true;
    }

    private boolean cat(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: cat <文件>        —— 看文件内容");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists() || !f.isFile()) {
            sh.printlnErr("cat: 文件不存在: " + p[1]);
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
            for (String line : content.split("\n")) sh.println(line);
        } catch (Exception e) {
            sh.printlnErr("cat: 读不了: " + e.getMessage());
        }
        return true;
    }

    private boolean head(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: head <文件> [行数]  —— 看前几行（默认10行）");
            return true;
        }
        int n = p.length >= 3 ? LocalShell.parseInt(p[2], 10) : 10;
        return headTail(p[1], n, true);
    }

    private boolean tail(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: tail <文件> [行数]  —— 看后几行（默认10行）");
            return true;
        }
        int n = p.length >= 3 ? LocalShell.parseInt(p[2], 10) : 10;
        return headTail(p[1], n, false);
    }

    private boolean headTail(String path, int n, boolean head) {
        File f = sh.resolve(path);
        if (!f.exists() || !f.isFile()) {
            sh.printlnErr("文件不存在: " + path);
            return true;
        }
        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            int read = 0;
            while (read < buf.length) {
                int k = fis.read(buf, read, buf.length - read);
                if (k < 0) break;
                read += k;
            }
            fis.close();
            String[] lines = new String(buf, 0, read, "UTF-8").split("\n");
            if (head) {
                for (int i = 0; i < Math.min(n, lines.length); i++) sh.println(lines[i]);
            } else {
                for (int i = Math.max(0, lines.length - n); i < lines.length; i++) sh.println(lines[i]);
            }
        } catch (Exception e) {
            sh.printlnErr("读不了: " + e.getMessage());
        }
        return true;
    }

    private boolean mkdir(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: mkdir <目录>       —— 建目录");
            return true;
        }
        File d = sh.resolve(p[1]);
        if (d.exists()) {
            sh.printlnErr("mkdir: 已存在: " + p[1]);
            return true;
        }
        if (d.mkdirs()) {
            sh.printlnOk("已创建: " + d.getAbsolutePath());
        } else {
            sh.printlnErr("mkdir: 建不了，可能没权限");
        }
        return true;
    }

    private boolean rm(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: rm <文件>         —— 删除（要输 y 确认）");
            return true;
        }
        boolean recursive = false;
        String path = null;
        for (int i = 1; i < p.length; i++) {
            if (p[i].equals("-r") || p[i].equals("-rf") || p[i].equals("-R")) recursive = true;
            else path = p[i];
        }
        if (path == null) {
            sh.printlnErr("用法: rm <文件>         —— 删除（要输 y 确认）");
            return true;
        }
        final File f = sh.resolve(path);
        if (!f.exists()) {
            sh.printlnErr("rm: 文件不存在: " + path);
            return true;
        }
        final boolean rec = recursive;
        sh.ask("rm: 删除 " + f.getAbsolutePath() + " ? (y/n)", input -> {
            if (input.equalsIgnoreCase("y")) {
                if (deleteRecursive(f, rec)) sh.printlnOk("已删除");
                else sh.printlnErr("rm: 删不掉");
            } else {
                sh.println("已取消");
            }
        });
        return true;
    }

    private boolean deleteRecursive(File f, boolean recursive) {
        if (f.isDirectory()) {
            if (!recursive) return false;
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursive(k, true);
        }
        return f.delete();
    }

    private boolean cp(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: cp <源> <目标>     —— 复制");
            return true;
        }
        File src = sh.resolve(p[1]);
        File dst = sh.resolve(p[2]);
        if (!src.exists()) {
            sh.printlnErr("cp: 源文件不存在: " + p[1]);
            return true;
        }
        try {
            if (src.isDirectory()) copyDir(src, dst);
            else copyFile(src, dst);
            sh.printlnOk("已复制");
        } catch (Exception e) {
            sh.printlnErr("cp: 复制失败: " + e.getMessage());
        }
        return true;
    }

    private void copyDir(File src, File dst) throws Exception {
        if (!dst.exists()) dst.mkdirs();
        File[] kids = src.listFiles();
        if (kids == null) return;
        for (File k : kids) {
            File d = new File(dst, k.getName());
            if (k.isDirectory()) copyDir(k, d);
            else copyFile(k, d);
        }
    }

    private void copyFile(File src, File dst) throws Exception {
        InputStream is = new FileInputStream(src);
        OutputStream os = new FileOutputStream(dst);
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
        is.close();
        os.close();
    }

    private boolean mv(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: mv <源> <目标>     —— 移动/重命名");
            return true;
        }
        File src = sh.resolve(p[1]);
        File dst = sh.resolve(p[2]);
        if (!src.exists()) {
            sh.printlnErr("mv: 源文件不存在: " + p[1]);
            return true;
        }
        if (src.renameTo(dst)) {
            sh.printlnOk("已移动");
        } else {
            sh.printlnErr("mv: 移不动，可能目标已存在");
        }
        return true;
    }

    private boolean touch(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: touch <文件>       —— 建空文件/更新时间");
            return true;
        }
        File f = sh.resolve(p[1]);
        try {
            if (f.exists()) {
                f.setLastModified(System.currentTimeMillis());
                sh.printlnOk("已更新时间");
            } else {
                f.getParentFile().mkdirs();
                if (f.createNewFile()) sh.printlnOk("已创建: " + f.getName());
                else sh.printlnErr("touch: 建不了");
            }
        } catch (Exception e) {
            sh.printlnErr("touch: " + e.getMessage());
        }
        return true;
    }

    private boolean find(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: find <目录> [关键词]  —— 找文件");
            return true;
        }
        File root = sh.resolve(p[1]);
        String name = p.length >= 3 ? p[2] : null;
        if (!root.exists()) {
            sh.printlnErr("find: 目录不存在: " + p[1]);
            return true;
        }
        findRec(root, name);
        return true;
    }

    private void findRec(File dir, String name) {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (name == null || f.getName().contains(name)) {
                sh.println(f.getAbsolutePath());
            }
            if (f.isDirectory()) findRec(f, name);
        }
    }

    private boolean du(String[] p) {
        File dir = p.length >= 2 ? sh.resolve(p[1]) : sh.resolve(null);
        if (!dir.exists()) {
            sh.printlnErr("du: 路径不存在: " + dir.getAbsolutePath());
            return true;
        }
        long size = duSize(dir);
        sh.println(size / 1024 + " KB\t" + dir.getAbsolutePath());
        return true;
    }

    private long duSize(File f) {
        if (f.isFile()) return f.length();
        long s = 0;
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) s += duSize(k);
        return s;
    }

    private boolean df(String[] p) {
        try {
            android.os.StatFs stat = new android.os.StatFs(sh.getCwd());
            long total = stat.getTotalBytes();
            long free = stat.getAvailableBytes();
            sh.println("文件系统          容量        已用       可用");
            sh.println("data          " + (total / 1024 / 1024) + "M   "
                + ((total - free) / 1024 / 1024) + "M   "
                + (free / 1024 / 1024) + "M");
        } catch (Exception e) {
            sh.printlnErr("df: " + e.getMessage());
        }
        return true;
    }

    private boolean stat(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: stat <文件>        —— 看文件详情");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("stat: 文件不存在: " + p[1]);
            return true;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sh.println("路径: " + f.getAbsolutePath());
        sh.println("大小: " + f.length() + " 字节");
        sh.println("类型: " + (f.isDirectory() ? "目录" : "文件"));
        sh.println("修改时间: " + sdf.format(new Date(f.lastModified())));
        sh.println("可读: " + f.canRead());
        sh.println("可写: " + f.canWrite());
        return true;
    }

    private boolean tree(String[] p) {
        File root = p.length >= 2 ? sh.resolve(p[1]) : sh.resolve(null);
        sh.println(root.getAbsolutePath());
        treeRec(root, "");
        return true;
    }

    private void treeRec(File dir, String prefix) {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        Arrays.sort(fs, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (int i = 0; i < fs.length; i++) {
            boolean last = i == fs.length - 1;
            sh.println(prefix + (last ? "└── " : "├── ") + fs[i].getName());
            if (fs[i].isDirectory()) {
                treeRec(fs[i], prefix + (last ? "    " : "│   "));
            }
        }
    }

    private boolean file(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: file <文件>        —— 判文件类型（文本/二进制）");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("file: 文件不存在: " + p[1]);
            return true;
        }
        if (f.isDirectory()) {
            sh.println(f.getName() + ": 目录");
            return true;
        }
        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] buf = new byte[16];
            int n = fis.read(buf);
            fis.close();
            if (n <= 0) {
                sh.println(f.getName() + ": 空文件");
                return true;
            }
            boolean text = true;
            for (int i = 0; i < n; i++) if (buf[i] == 0) { text = false; break; }
            sh.println(f.getName() + ": " + (text ? "文本文件" : "二进制文件"));
        } catch (Exception e) {
            sh.printlnErr("file: " + e.getMessage());
        }
        return true;
    }

    private boolean ln(String[] p) {
        sh.printlnErr("ln: 普通 App 建不了硬链接，只能建软链接");
        sh.printlnErr("（Android 上建软链接需要 Root）");
        return true;
    }

    private boolean diff(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: diff <文件1> <文件2>  —— 比对两文件");
            return true;
        }
        File f1 = sh.resolve(p[1]);
        File f2 = sh.resolve(p[2]);
        if (!f1.exists() || !f2.exists()) {
            sh.printlnErr("diff: 文件不存在");
            return true;
        }
        try {
            String s1 = readAll(f1);
            String s2 = readAll(f2);
            if (s1.equals(s2)) {
                sh.printlnOk("两文件一模一样");
            } else {
                String[] l1 = s1.split("\n");
                String[] l2 = s2.split("\n");
                int max = Math.max(l1.length, l2.length);
                for (int i = 0; i < max; i++) {
                    String a = i < l1.length ? l1[i] : "";
                    String b = i < l2.length ? l2[i] : "";
                    if (!a.equals(b)) {
                        sh.printlnErr("< " + a);
                        sh.printlnOk("> " + b);
                    }
                }
            }
        } catch (Exception e) {
            sh.printlnErr("diff: " + e.getMessage());
        }
        return true;
    }

    private String readAll(File f) throws Exception {
        FileInputStream fis = new FileInputStream(f);
        byte[] buf = new byte[(int) f.length()];
        int read = 0;
        while (read < buf.length) {
            int n = fis.read(buf, read, buf.length - read);
            if (n < 0) break;
            read += n;
        }
        fis.close();
        return new String(buf, 0, read, "UTF-8");
    }

    private boolean split(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: split <文件> <每片KB>  —— 切分文件");
            return true;
        }
        File f = sh.resolve(p[1]);
        int size = LocalShell.parseInt(p[2], 100) * 1024;
        if (!f.exists() || size <= 0) {
            sh.printlnErr("split: 参数不对");
            return true;
        }
        try {
            byte[] all = new byte[(int) f.length()];
            FileInputStream fis = new FileInputStream(f);
            int read = 0;
            while (read < all.length) {
                int n = fis.read(all, read, all.length - read);
                if (n < 0) break;
                read += n;
            }
            fis.close();
            int parts = (all.length + size - 1) / size;
            for (int i = 0; i < parts; i++) {
                int start = i * size;
                int end = Math.min(start + size, all.length);
                File out = new File(f.getParent(), f.getName() + ".part" + (i + 1));
                FileOutputStream fos = new FileOutputStream(out);
                fos.write(all, start, end - start);
                fos.close();
                sh.printlnOk("已生成: " + out.getName());
            }
        } catch (Exception e) {
            sh.printlnErr("split: " + e.getMessage());
        }
        return true;
    }

    private boolean truncate(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: truncate <文件> <字节数>  —— 截断文件");
            return true;
        }
        File f = sh.resolve(p[1]);
        int size = LocalShell.parseInt(p[2], -1);
        if (!f.exists() || size < 0) {
            sh.printlnErr("truncate: 参数不对");
            return true;
        }
        try {
            java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "rw");
            raf.setLength(size);
            raf.close();
            sh.printlnOk("已截到 " + size + " 字节");
        } catch (Exception e) {
            sh.printlnErr("truncate: " + e.getMessage());
        }
        return true;
    }

    private boolean basename(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: basename <路径>    —— 取文件名");
            return true;
        }
        String s = p[1];
        int slash = s.lastIndexOf('/');
        sh.println(slash >= 0 ? s.substring(slash + 1) : s);
        return true;
    }

    private boolean dirname(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: dirname <路径>     —— 取所在目录");
            return true;
        }
        String s = p[1];
        int slash = s.lastIndexOf('/');
        sh.println(slash >= 0 ? s.substring(0, slash) : ".");
        return true;
    }
}