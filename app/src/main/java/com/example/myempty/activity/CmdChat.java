package com.example.myempty.activity2;

import android.content.Context;

import java.io.File;

public class CmdChat {

    private final LocalShell sh;
    private final Context ctx;
    private final LocalShell.OnOutput out;

    public CmdChat(LocalShell sh, Context ctx, LocalShell.OnOutput out) {
        this.sh = sh;
        this.ctx = ctx;
        this.out = out;
    }

    public boolean exec(String[] p) {
        String c = p[0];
        if (c.equals("chatclear")) return chatClear(p);
        if (c.equals("清空聊天")) return chatClear(p);
        return false;
    }

    private boolean chatClear(String[] p) {
        boolean autoYes = false;
        for (int i = 1; i < p.length; i++) {
            if (p[i].equals("-y") || p[i].equals("--yes")) autoYes = true;
        }

        File dbDir = ctx.getDatabasePath("chat.db").getParentFile();
        File db = new File(dbDir, "chat.db");
        File dbJournal = new File(dbDir, "chat.db-journal");
        File dbWal = new File(dbDir, "chat.db-wal");
        File dbShm = new File(dbDir, "chat.db-shm");

        File chatFiles = new File(ctx.getExternalFilesDir(null), "chat_files");
        File chatAvatars = new File(ctx.getExternalFilesDir(null), "chat_avatars");
        File chatCache = new File(ctx.getExternalFilesDir(null), "chat_cache");
        File avatarCache = new File(ctx.getExternalFilesDir(null), "chat_avatar_cache");

        long totalSize = 0;
        int count = 0;

        File[] all = {db, dbJournal, dbWal, dbShm, chatFiles, chatAvatars, chatCache, avatarCache};

        sh.println("要删的：");
        sh.println("");

        for (File f : all) {
            if (f.exists()) {
                long s = size(f);
                totalSize += s;
                count++;
                sh.println("  " + f.getAbsolutePath());
                sh.println("      " + fmtSize(s));
            }
        }

        if (count == 0) {
            sh.println("  （没东西删）");
            return true;
        }

        sh.println("");
        sh.println("共 " + count + " 项，" + fmtSize(totalSize));
        sh.println("");

        if (autoYes) {
            doDelete(all);
            return true;
        }

        sh.ask("确定清空聊天记录吗？y 删 / n 算了", input -> {
            if (input != null && input.trim().equalsIgnoreCase("y")) {
                doDelete(all);
            } else {
                sh.println("取消了");
            }
        });

        return true;
    }

    private void doDelete(File[] targets) {
        sh.println("");
        sh.println("开始清理...");

        try {
            ChatDb.closeDb();
        } catch (Throwable ignored) {}

        int ok = 0;
        int err = 0;

        for (File f : targets) {
            if (!f.exists()) continue;
            if (deleteRecursive(f)) {
                ok++;
                sh.printlnOk("删了: " + f.getName());
            } else {
                err++;
                sh.printlnErr("删不掉: " + f.getAbsolutePath());
            }
        }

        sh.println("");
        sh.printlnOk("清理完成，共删 " + ok + " 项");
        if (err > 0) {
            sh.printlnErr(err + " 项删不掉，可能被占了");
        }
        sh.println("下次进聊天会重建");
    }

    private long size(File f) {
        if (f.isFile()) return f.length();
        long s = 0;
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) s += size(k);
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