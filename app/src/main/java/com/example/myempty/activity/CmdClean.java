package com.example.myempty.activity2;

import android.content.Context;

import java.io.File;

public class CmdClean {

    private final LocalShell sh;
    private final Context ctx;
    private final LocalShell.OnOutput out;

    public CmdClean(LocalShell sh, Context ctx, LocalShell.OnOutput out) {
        this.sh = sh;
        this.ctx = ctx;
        this.out = out;
    }

    public boolean exec(String[] p) {
        String c = p[0];
        if (c.equals("cleandata")) return cleanData(p);
        if (c.equals("清理数据")) return cleanData(p);
        return false;
    }

    private boolean cleanData(String[] p) {
        boolean autoYes = false;
        for (int i = 1; i < p.length; i++) {
            if (p[i].equals("-y") || p[i].equals("--yes")) autoYes = true;
        }

        final File[] targets = {
            new File("/storage/emulated/0/Download/二维码存放地"),
            new File("/storage/emulated/0/Download/摩斯电码音频"),
            new File("/storage/emulated/0/Download/摩斯电码破译文件存放地"),
            new File("/storage/emulated/0/Download/合成APKS"),
            new File("/storage/emulated/0/Download/下载文件"),
            new File("/storage/emulated/0/Pictures/二维码存放地"),
            new File("/storage/emulated/0/画画成果/真正画画"),
            new File("/storage/emulated/0/画画成果/硬核画画"),
            new File(ctx.getExternalFilesDir(null), "Download"),
            new File(ctx.getExternalFilesDir(null), "received")
        };

        int existCount = 0;
        long totalSize = 0;

        sh.println("下面这些是要删的：");
        sh.println("");

        for (File f : targets) {
            if (f.exists()) {
                long size = dirSize(f);
                totalSize += size;
                existCount++;
                sh.println("  " + f.getAbsolutePath());
                sh.println("      " + fmtSize(size));
            }
        }

        if (existCount == 0) {
            sh.println("  （没东西删，都是空的）");
            return true;
        }

        sh.println("");
        sh.println("一共 " + existCount + " 个目录，"
            + fmtSize(totalSize));
        sh.println("");
        sh.println("注意：备忘录、保险箱、账户、协议 都会留着");
        sh.println("");

        if (autoYes) {
            doDelete(targets);
            return true;
        }

        sh.ask("确定要删这些吗？y 删 / n 算了", input -> {
            if (input != null && input.trim().equalsIgnoreCase("y")) {
                doDelete(targets);
            } else {
                sh.println("取消了");
            }
        });

        return true;
    }

    private void doDelete(File[] targets) {
        sh.println("");
        sh.println("开始清理...");
        int okCount = 0;
        int errCount = 0;

        for (File f : targets) {
            if (!f.exists()) continue;
            if (deleteRecursive(f)) {
                okCount++;
                sh.printlnOk("删了: " + f.getName());
            } else {
                errCount++;
                sh.printlnErr("删不掉: " + f.getAbsolutePath());
            }
        }

        sh.println("");
        sh.printlnOk("清理完成，共删 " + okCount + " 个目录");
        if (errCount > 0) {
            sh.printlnErr(errCount + " 个删不掉，可能被别的 App 占用了");
        }
    }

    private long dirSize(File f) {
        if (f.isFile()) return f.length();
        long s = 0;
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) s += dirSize(k);
        }
        return s;
    }

    private boolean deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File k : kids) deleteRecursive(k);
            }
        }
        return f.delete();
    }

    private String fmtSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.1f MB", bytes / 1024.0 / 1024);
        }
        return String.format("%.2f GB", bytes / 1024.0 / 1024 / 1024);
    }
}