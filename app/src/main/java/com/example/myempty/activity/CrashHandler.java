package com.example.myempty.activity2;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private final Context ctx;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public CrashHandler(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void install(Context ctx) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(ctx));
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();

            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()).format(new Date());

            String content = "时间: " + time + "\n"
                + "线程: " + t.getName() + "\n"
                + "异常: " + e.getClass().getName() + "\n\n"
                + sw.toString() + "\n\n";

            File outDir = new File("/storage/emulated/0/Download/崩溃日志");
            if (!outDir.exists()) outDir.mkdirs();
            File out = new File(outDir,
                "crash_" + System.currentTimeMillis() + ".txt");

            FileOutputStream fos = new FileOutputStream(out);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
        } catch (Throwable ignored) {}

        if (defaultHandler != null) {
            defaultHandler.uncaughtException(t, e);
        } else {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }
}