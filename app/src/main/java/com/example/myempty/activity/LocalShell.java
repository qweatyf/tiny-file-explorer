package com.example.myempty.activity2;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LocalShell {

    public interface OnOutput { void onLine(String text, int color); }
    public interface OnAsk { void ask(String question, Answer answer); }
    public interface Answer { void reply(String input); }

    private final Context ctx;
    private final OnOutput out;
    private OnAsk ask;

    private String cwd = "/storage/emulated/0/Download";
    private final List<String> history = new ArrayList<>();
    private final java.util.Map<String, String> vars = new java.util.HashMap<>();

    public CmdFile file;
    public CmdText text;
    public CmdNetwork network;
    public CmdSystem system;
    public CmdTool tool;
    public CmdExtra extra;
    public CmdClean clean;
    public CmdChat chat;

    public LocalShell(Context ctx, OnOutput out) {
        this.ctx = ctx;
        this.out = out;
        this.file = new CmdFile(this, ctx, out);
        this.text = new CmdText(this, ctx, out);
        this.network = new CmdNetwork(this, ctx, out);
        this.system = new CmdSystem(this, ctx, out);
        this.tool = new CmdTool(this, ctx, out);
        this.extra = new CmdExtra(this, ctx, out);
        this.clean = new CmdClean(this, ctx, out);
        this.chat = new CmdChat(this, ctx, out);
    }

    public void setAsk(OnAsk a) { ask = a; }
    public void ask(String q, Answer a) { if (ask != null) ask.ask(q, a); }

    public void println(String s) { out.onLine(s, 0xFF000000); }
    public void printlnOk(String s) { out.onLine(s, 0xFF1B5E20); }
    public void printlnErr(String s) { out.onLine(s, 0xFFB71C1C); }
    public void printlnRaw(String s) { out.onLine(s, 0xFF000000); }
    public void printlnReplace(String s) { out.onLine("\u0001" + s, 0xFF1B5E20); }

    public String getCwd() { return cwd; }
    public void setCwd(String c) { cwd = c; }
    public Context getContext() { return ctx; }
    public List<String> getHistory() { return history; }
    public java.util.Map<String, String> getVars() { return vars; }

    public void exec(String line) {
        line = line.trim();
        if (line.isEmpty()) return;
        history.add(line);

        String[] parts = line.split("\\s+");
        String cmd = parts[0];

        try {
            if (cmd.equals("help") || cmd.equals("?")) { showHelp(parts); return; }
            if (cmd.equals("clear")) { println("\u000C"); return; }
            if (cmd.equals("exit") || cmd.equals("quit")) {
                println(ctx.getString(R.string.shell_bye) + ".");
                return;
            }
            if (cmd.equals("history")) {
                for (int i = 0; i < history.size(); i++) println((i + 1) + "  " + history.get(i));
                return;
            }
            if (cmd.equals("echo")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    if (i > 1) sb.append(" ");
                    sb.append(parts[i]);
                }
                println(sb.toString());
                return;
            }
            if (cmd.equals("set")) {
                if (parts.length >= 3) {
                    vars.put(parts[1], parts[2]);
                    printlnOk(parts[1] + "=" + parts[2]);
                } else {
                    for (java.util.Map.Entry<String, String> e : vars.entrySet())
                        println(e.getKey() + "=" + e.getValue());
                }
                return;
            }
            if (cmd.equals("unset")) {
                if (parts.length >= 2) vars.remove(parts[1]);
                return;
            }
            if (cmd.equals("sleep")) {
                int ms = parts.length >= 2 ? parseInt(parts[1], 1000) : 1000;
                try { Thread.sleep(ms); } catch (Exception ignored) {}
                printlnOk(ctx.getString(R.string.shell_slept, ms));
                return;
            }
            if (cmd.equals("true")) { printlnOk("true"); return; }
            if (cmd.equals("false")) { printlnErr("false"); return; }
            if (cmd.equals("which")) {
                if (parts.length >= 2) println(ctx.getString(R.string.shell_builtin, parts[1]));
                return;
            }
            if (cmd.equals("date")) {
                println(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.getDefault()).format(new java.util.Date()));
                return;
            }

            if (file.exec(parts)) return;
            if (text.exec(parts)) return;
            if (network.exec(parts)) return;
            if (system.exec(parts)) return;
            if (tool.exec(parts)) return;
            if (extra.exec(parts)) return;
            if (clean.exec(parts)) return;
            if (chat.exec(parts)) return;

            printlnErr(ctx.getString(R.string.shell_not_found, cmd));
            printlnErr(ctx.getString(R.string.shell_help_hint2));
        } catch (Throwable e) {
            printlnErr("error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    public void showHelp(String[] parts) {
        String topic = parts.length >= 2 ? parts[1].toLowerCase() : "";

        if (topic.isEmpty()) {
            println(ctx.getString(R.string.shell_help_title));
            println(ctx.getString(R.string.shell_help_hint));
            return;
        }

        if (topic.equals("file")) {
            println(ctx.getString(R.string.shell_file));
            println(ctx.getString(R.string.shell_file_new));
            return;
        }
        if (topic.equals("text")) {
            println(ctx.getString(R.string.shell_text));
            println(ctx.getString(R.string.shell_text_new));
            return;
        }
        if (topic.equals("network")) {
            println(ctx.getString(R.string.shell_network_dl));
            println(ctx.getString(R.string.shell_network_dns));
            println(ctx.getString(R.string.shell_network_ip));
            println(ctx.getString(R.string.shell_network_conn));
            println(ctx.getString(R.string.shell_network_other));
            return;
        }
        if (topic.equals("system")) {
            println(ctx.getString(R.string.shell_system));
            println(ctx.getString(R.string.shell_system_new));
            return;
        }
        if (topic.equals("tool")) {
            println(ctx.getString(R.string.shell_tool));
            return;
        }
        if (topic.equals("extra")) {
            println(ctx.getString(R.string.shell_extra));
            return;
        }
        if (topic.equals("clean")) {
            println(ctx.getString(R.string.shell_clean));
            return;
        }
        if (topic.equals("chat")) {
            println(ctx.getString(R.string.shell_chat));
            return;
        }
        if (topic.equals("mirror")) {
            println(ctx.getString(R.string.shell_mirror));
            return;
        }
        if (topic.equals("all")) {
            println(ctx.getString(R.string.shell_all_file));
            println(ctx.getString(R.string.shell_all_base) + ctx.getString(R.string.shell_file));
            println(ctx.getString(R.string.shell_all_new) + ctx.getString(R.string.shell_file_new));
            println("");
            println(ctx.getString(R.string.shell_all_text));
            println(ctx.getString(R.string.shell_all_base) + ctx.getString(R.string.shell_text));
            println(ctx.getString(R.string.shell_all_new) + ctx.getString(R.string.shell_text_new));
            println("");
            println(ctx.getString(R.string.shell_all_net_dl));
            println(ctx.getString(R.string.shell_network_dl));
            println("");
            println(ctx.getString(R.string.shell_all_net_dns));
            println(ctx.getString(R.string.shell_network_dns));
            println("");
            println(ctx.getString(R.string.shell_all_net_ip));
            println(ctx.getString(R.string.shell_network_ip));
            println("");
            println(ctx.getString(R.string.shell_all_net_conn));
            println(ctx.getString(R.string.shell_network_conn));
            println("");
            println(ctx.getString(R.string.shell_all_net_other));
            println(ctx.getString(R.string.shell_network_other));
            println("");
            println(ctx.getString(R.string.shell_all_sys));
            println(ctx.getString(R.string.shell_all_base) + ctx.getString(R.string.shell_system));
            println(ctx.getString(R.string.shell_all_new) + ctx.getString(R.string.shell_system_new));
            println("");
            println(ctx.getString(R.string.shell_all_tool));
            println(ctx.getString(R.string.shell_tool));
            println("");
            println(ctx.getString(R.string.shell_all_extra));
            println(ctx.getString(R.string.shell_extra));
            println("");
            println(ctx.getString(R.string.shell_all_clean));
            println(ctx.getString(R.string.shell_clean));
            println("");
            println(ctx.getString(R.string.shell_all_chat));
            println(ctx.getString(R.string.shell_chat));
            return;
        }
        printlnErr(ctx.getString(R.string.shell_unknown) + topic);
    }

    public File resolve(String path) {
        if (path == null || path.isEmpty()) return new File(cwd);
        File f = new File(path);
        return f.isAbsolute() ? f : new File(cwd, path);
    }
}