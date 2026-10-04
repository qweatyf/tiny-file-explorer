package com.example.myempty.activity2;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LocalShell {

    public interface OnOutput {
        void onLine(String text, int color);
    }

    public interface OnAsk {
        void ask(String question, Answer answer);
    }

    public interface Answer {
        void reply(String input);
    }

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

    public void setAsk(OnAsk a) {
        ask = a;
    }

    public void ask(String q, Answer a) {
        if (ask != null) ask.ask(q, a);
    }

    public void println(String s) {
        out.onLine(s, 0xFF000000);
    }

    public void printlnOk(String s) {
        out.onLine(s, 0xFF1B5E20);
    }

    public void printlnErr(String s) {
        out.onLine(s, 0xFFB71C1C);
    }

    public void printlnRaw(String s) {
        out.onLine(s, 0xFF000000);
    }

    public void printlnReplace(String s) {
        out.onLine("\u0001" + s, 0xFF1B5E20);
    }

    public String getCwd() {
        return cwd;
    }

    public void setCwd(String c) {
        cwd = c;
    }

    public Context getContext() {
        return ctx;
    }

    public List<String> getHistory() {
        return history;
    }

    public java.util.Map<String, String> getVars() {
        return vars;
    }

    public void exec(String line) {
        line = line.trim();
        if (line.isEmpty()) return;

        history.add(line);

        String[] parts = line.split("\\s+");
        String cmd = parts[0];

        try {
            if (cmd.equals("help") || cmd.equals("?")) {
                showHelp(parts);
                return;
            }
            if (cmd.equals("clear")) {
                println("\u000C");
                return;
            }
            if (cmd.equals("exit") || cmd.equals("quit")) {
                println("Bye.");
                return;
            }
            if (cmd.equals("history")) {
                for (int i = 0; i < history.size(); i++) {
                    println((i + 1) + "  " + history.get(i));
                }
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
                    for (java.util.Map.Entry<String, String> e : vars.entrySet()) {
                        println(e.getKey() + "=" + e.getValue());
                    }
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
                printlnOk("slept " + ms + "ms");
                return;
            }
            if (cmd.equals("true")) { printlnOk("true"); return; }
            if (cmd.equals("false")) { printlnErr("false"); return; }
            if (cmd.equals("which")) {
                if (parts.length < 2) { printlnErr("usage: which <cmd>"); return; }
                println("built-in: " + parts[1]);
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

            printlnErr("command not found: " + cmd);
            printlnErr("输入 help 查看所有命令");

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
            println("可用命令分类：");
            println("");
            println("help file      文件操作");
            println("help text      文本处理");
            println("help network   网络");
            println("help system    系统信息");
            println("help tool      工具");
            println("help extra     扩展工具");
            println("help clean     清理数据");
            println("help chat      聊天清理");
            println("help mirror    GitHub 镜像");
            println("help all       全部");
            println("");
            println("直接输入 help all 看所有命令");
            return;
        }

        if (topic.equals("file")) {
            println("文件操作命令：");
            println("");
            println("ls              列目录");
            println("ls -la          详细列表");
            println("cd              切换目录");
            println("pwd             看当前路径");
            println("cat             看文件内容");
            println("head            看前几行");
            println("tail            看后几行");
            println("mkdir           建目录");
            println("rm              删文件（要输 y 确认）");
            println("cp              复制");
            println("mv              移动/重命名");
            println("touch           建空文件");
            println("find            找文件");
            println("du              看目录大小");
            println("df              看磁盘占用");
            println("stat            看文件详情");
            println("tree            树形列目录");
            println("file            判文件类型");
            println("ln              建链接（不支持）");
            println("diff            比对两文件");
            println("split           切分文件");
            println("truncate        截断文件");
            println("basename        取文件名");
            println("dirname         取目录名");
            return;
        }

        if (topic.equals("text")) {
            println("文本处理命令：");
            println("");
            println("grep            搜内容");
            println("wc              统计行/词/字符");
            println("sort            排序");
            println("uniq            去连续重复");
            println("cut             按列切");
            println("tr              字符替换");
            println("sed             按规则替换");
            println("awk             按列取值");
            println("base64          Base64 编解码");
            println("sha256sum       算 SHA-256");
            println("md5sum          算 MD5");
            println("xxd             十六进制看");
            println("hexdump         同上");
            println("rev             每行倒序");
            println("tac             整文件倒序");
            return;
        }

        if (topic.equals("network")) {
            println("网络命令：");
            println("");
            println("wget <url>      下载文件（带进度）");
            println("wget -c <url>   断点续传");
            println("download        同上");
            println("fetch           同上");
            println("curl <url>      取网页源码");
            println("ping <host>     测连通");
            println("ip              看 WiFi IP");
            println("netstat         看网络状态");
            println("http <url>      看响应头");
            println("dns <host>      解析域名");
            println("url -e <text>   编码 URL");
            println("url -d <text>   解码 URL");
            println("imginfo <图片>   看图片信息");
            println("stopdl          取消当前下载");
            return;
        }

        if (topic.equals("system")) {
            println("系统信息命令：");
            println("");
            println("ps              看进程列表");
            println("uname           看系统信息");
            println("whoami          看当前用户");
            println("id              看 uid/pid");
            println("env             看环境变量");
            println("battery         看电池");
            println("cpu             看 CPU");
            println("mem             看内存");
            println("disk            看磁盘");
            println("net             看网络");
            println("app             看已装应用");
            println("uptime          看开机时长");
            println("hostname        看设备名");
            println("locale          看语言时区");
            println("export K=V      设变量");
            return;
        }

        if (topic.equals("tool")) {
            println("工具命令：");
            println("");
            println("clear           清屏");
            println("help            看帮助");
            println("history         看历史");
            println("exit            退出");
            println("sleep <ms>      睡一会");
            println("true            真");
            println("false           假");
            println("which <cmd>     查命令");
            println("alias           别名");
            println("set K V         设变量");
            println("unset K         删变量");
            println("export K=V      设环境变量");
            println("source <file>   跑脚本");
            println("bash <file>     同上");
            println("sh <file>       同上");
            println("calc <表达式>   算数");
            println("unit <n> <a> <b> 单位换算");
            println("rand <max>      随机数");
            println("uuid            生成 UUID");
            println("pass <len>      生成密码");
            println("json <file>     格式化 JSON");
            println("timestamp       时间戳互转");
            println("zip <out> <dir> 压缩");
            println("unzip <file>    解压");
            println("timer <sec>     倒计时");
            println("count <file>    算行数");
            println("sum <n...>      求和");
            println("avg <n...>      求平均");
            println("min <n...>      求最小");
            println("max <n...>      求最大");
            println("open <file>     用别的 App 打开");
            println("share <file>    分享");
            println("copy <text>     复制到剪贴板");
            println("toast <text>    弹提示");
            println("vibrate <ms>    震动");
            return;
        }

        if (topic.equals("extra")) {
            println("扩展工具命令：");
            println("");
            println("enc utf8 <文本>      UTF-8 编码");
            println("enc gbk <文本>       GBK 编码");
            println("enc unicode <文本>   Unicode 编码");
            println("enc hex <文本>       十六进制");
            println("enc base64 <文本>    Base64");
            println("dec hex <内容>       十六进制解码");
            println("dec base64 <内容>    Base64 解码");
            println("dec unicode <内容>   Unicode 解码");
            println("qrenc <文本> [文件]  生成二维码");
            println("qrdec <图片>         识别二维码");
            println("myip                 查公网 IP");
            println("portscan <IP>        端口扫描");
            println("repeat <n> <命令>    重复执行");
            println("after <秒> <命令>    延迟执行");
            println("at <HH:mm> <命令>    定时执行");
            return;
        }

        if (topic.equals("clean")) {
            println("清理数据：");
            println("");
            println("cleandata         列出要删的，问 y/n");
            println("cleandata -y      不问直接删");
            println("");
            println("会删：下载、二维码、音频、APKS、画画成果、快传接收");
            println("会留：备忘录、保险箱、账户、协议");
            return;
        }

        if (topic.equals("chat")) {
            println("聊天清理：");
            println("");
            println("chatclear         删聊天记录，问 y/n");
            println("chatclear -y      不问直接删");
            println("");
            println("会删：chat.db、聊天文件、头像、缓存");
            println("下次进聊天会自动重建");
            return;
        }

        if (topic.equals("mirror")) {
            println("GitHub 镜像命令：");
            println("");
            println("gh-mirror              看镜像状态");
            println("gh-mirror on           开启 GitHub 镜像");
            println("gh-mirror off          关闭 GitHub 镜像");
            println("gh-mirror list         列出内置镜像");
            println("gh-mirror use <编号>   切到第几个镜像");
            println("gh-mirror set <地址>   自定义镜像地址");
            println("gh-mirror reset        恢复默认");
            println("");
            println("开了镜像后，wget / curl / download / fetch / http");
            println("遇到 GitHub 链接会自动加镜像前缀");
            return;
        }

        if (topic.equals("all")) {
            println("全部命令：");
            println("");
            println("【文件】");
            println("ls  cd  pwd  cat  head  tail  mkdir  rm  cp  mv  touch");
            println("find  du  df  stat  tree  file  ln  diff  split  truncate");
            println("basename  dirname");
            println("");
            println("【文本】");
            println("grep  wc  sort  uniq  cut  tr  sed  awk  base64  sha256sum");
            println("md5sum  xxd  hexdump  rev  tac");
            println("");
            println("【网络】");
            println("wget  download  fetch  curl  ping  ip  netstat  http  dns  url");
            println("imginfo  stopdl");
            println("");
            println("【GitHub 镜像】");
            println("gh-mirror  gh-mirror on  gh-mirror off  gh-mirror list");
            println("gh-mirror use  gh-mirror set  gh-mirror reset");
            println("");
            println("【系统】");
            println("ps  uname  whoami  id  env  battery  cpu  mem  disk  net  app");
            println("uptime  hostname  locale  export");
            println("");
            println("【工具】");
            println("clear  help  history  exit  sleep  true  false  which  alias");
            println("set  unset  export  source  bash  sh  calc  unit  rand  uuid");
            println("pass  json  timestamp  zip  unzip  timer  count  sum  avg");
            println("min  max  open  share  copy  toast  vibrate");
            println("");
            println("【扩展】");
            println("enc  dec  qrenc  qrdec  myip  portscan  repeat  after  at");
            println("");
            println("【清理】");
            println("cleandata");
            println("");
            println("【聊天】");
            println("chatclear");
            return;
        }

        printlnErr("unknown help topic: " + topic);
        println("");
        println("可用分类：file / text / network / system / tool / extra / clean / chat / mirror / all");
    }

    public File resolve(String path) {
        if (path == null || path.isEmpty()) return new File(cwd);
        File f = new File(path);
        if (f.isAbsolute()) return f;
        return new File(cwd, path);
    }
}