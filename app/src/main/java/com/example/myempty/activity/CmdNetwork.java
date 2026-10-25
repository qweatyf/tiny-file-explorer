package com.example.myempty.activity2;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

public class CmdNetwork {

    private final LocalShell sh;
    private final Context ctx;
    private final LocalShell.OnOutput out;

    private static volatile boolean cancelFlag = false;
    private static volatile String currentFile = null;

    private static final String[] MIRRORS = {
        "https://ghproxy.com/", "https://raw.gitmirror.com/",
        "https://ghproxy.net/", "https://mirror.ghproxy.com/",
        "https://gh.api.99988866.xyz/", "https://gh-proxy.com/"
    };

    private boolean mirrorOn = false;
    private int mirrorIdx = 0;
    private String customMirror = null;

    public CmdNetwork(LocalShell sh, Context ctx, LocalShell.OnOutput out) {
        this.sh = sh; this.ctx = ctx; this.out = out;
    }

    public static void cancelCurrent() { cancelFlag = true; }
    public static boolean isDownloading() { return currentFile != null; }

    public boolean exec(String[] p) {
        String c = p[0];
        if (c.equals("wget") || c.equals("download") || c.equals("fetch")) return download(p);
        if (c.equals("curl")) return curl(p);
        if (c.equals("ping")) return ping(p);
        if (c.equals("ip")) return ip(p);
        if (c.equals("netstat")) return netstat(p);
        if (c.equals("http")) return http(p);
        if (c.equals("dns")) return dns(p);
        if (c.equals("url")) return url(p);
        if (c.equals("gh-mirror") || c.equals("ghmirror")) return ghMirror(p);
        if (c.equals("imginfo")) return imgInfo(p);
        if (c.equals("stopdl") || c.equals("stopdownload")) return stopDownload(p);
        if (c.equals("whois")) return whois(p);
        if (c.equals("ipinfo")) return ipInfo(p);
        if (c.equals("myip4")) return myIp4(p);
        if (c.equals("myip6")) return myIp6(p);
        if (c.equals("dnsq")) return dnsQuery(p);
        if (c.equals("mx")) return dnsType(p, "MX");
        if (c.equals("txt")) return dnsType(p, "TXT");
        if (c.equals("ns")) return dnsType(p, "NS");
        if (c.equals("portcheck")) return portCheck(p);
        if (c.equals("httpcode")) return httpCode(p);
        if (c.equals("httptime")) return httpTime(p);
        if (c.equals("headers")) return headers(p);
        if (c.equals("redir")) return redir(p);
        if (c.equals("speedtest")) return speedTest(p);
        if (c.equals("trace")) return trace(p);
        if (c.equals("checkonline")) return checkOnline(p);
        if (c.equals("iplocal")) return ipLocal(p);
        if (c.equals("mac")) return mac(p);
        if (c.equals("gateway")) return gateway(p);
        if (c.equals("netinfo")) return netInfo(p);
        return false;
    }

    private boolean stopDownload(String[] p) {
        if (currentFile == null) { sh.printlnErr("当前没有在下载"); return true; }
        cancelFlag = true;
        sh.printlnOk("已发送取消信号，正在中断...");
        return true;
    }

    private String getMirror() {
        if (customMirror != null && !customMirror.isEmpty()) return customMirror;
        if (mirrorIdx < 0 || mirrorIdx >= MIRRORS.length) mirrorIdx = 0;
        return MIRRORS[mirrorIdx];
    }

    private String applyMirror(String url) {
        if (!mirrorOn || url == null) return url;
        String low = url.toLowerCase();
        if (!low.contains("github.com") && !low.contains("githubusercontent.com")) return url;
        for (String m : MIRRORS) if (low.startsWith(m)) return url;
        if (customMirror != null && low.startsWith(customMirror)) return url;
        return getMirror() + url;
    }

    private boolean ghMirror(String[] p) {
        if (p.length < 2) {
            sh.println("GitHub 镜像状态: " + (mirrorOn ? "已开启" : "已关闭"));
            sh.println("当前镜像: " + getMirror());
            sh.println("用法: gh-mirror on/off/list/use <n>/set <url>/reset");
            return true;
        }
        String sub = p[1].toLowerCase();
        if (sub.equals("on")) {
            mirrorOn = true;
            sh.printlnOk("GitHub 镜像已开启: " + getMirror());
            return true;
        }
        if (sub.equals("off")) { mirrorOn = false; sh.printlnOk("已关闭"); return true; }
        if (sub.equals("list")) {
            for (int i = 0; i < MIRRORS.length; i++)
                sh.println("  " + (i + 1) + ". " + MIRRORS[i] + ((i == mirrorIdx && customMirror == null) ? " *" : ""));
            return true;
        }
        if (sub.equals("use")) {
            if (p.length < 3) { sh.printlnErr("用法: gh-mirror use <编号>"); return true; }
            int n = LocalShell.parseInt(p[2], -1);
            if (n < 1 || n > MIRRORS.length) { sh.printlnErr("编号 1~" + MIRRORS.length); return true; }
            mirrorIdx = n - 1; customMirror = null;
            sh.printlnOk("已切到 " + MIRRORS[mirrorIdx]);
            return true;
        }
        if (sub.equals("set")) {
            if (p.length < 3) { sh.printlnErr("用法: gh-mirror set <url>"); return true; }
            String m = p[2];
            if (!m.endsWith("/")) m += "/";
            customMirror = m;
            sh.printlnOk("已设 " + m);
            return true;
        }
        if (sub.equals("reset")) {
            customMirror = null; mirrorIdx = 0;
            sh.printlnOk("已恢复默认");
            return true;
        }
        sh.printlnErr("未知子命令: " + sub);
        return true;
    }

    private HttpURLConnection openWithRedirect(String urlStr, long rangeStart) throws Exception {
        String current = urlStr;
        for (int hop = 0; hop < 5; hop++) {
            URL url = new URL(current);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setInstanceFollowRedirects(false);
            if (rangeStart > 0) conn.setRequestProperty("Range", "bytes=" + rangeStart + "-");
            conn.connect();
            int code = conn.getResponseCode();
            if (code >= 300 && code < 400) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                if (loc == null) throw new Exception("重定向没给 Location");
                if (loc.startsWith("/")) {
                    URL base = new URL(current);
                    loc = base.getProtocol() + "://" + base.getHost()
                        + (base.getPort() > 0 ? ":" + base.getPort() : "") + loc;
                }
                sh.println("跳转 " + (hop + 1) + ": " + loc);
                current = loc;
                continue;
            }
            return conn;
        }
        throw new Exception("重定向次数太多");
    }

    private boolean download(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: " + p[0] + " [-c] <链接> [文件名]");
            return true;
        }
        if (currentFile != null) { sh.printlnErr("已有下载进行中"); return true; }

        boolean resume = false;
        int argStart = 1;
        if (p[1].equals("-c") || p[1].equals("--continue")) { resume = true; argStart = 2; }
        if (argStart >= p.length) { sh.printlnErr("没给链接"); return true; }

        final String rawUrl = p[argStart];
        final String urlStr = applyMirror(rawUrl);
        final String nameArg = p.length > argStart + 1 ? p[argStart + 1] : null;
        final boolean doResume = resume;

        if (mirrorOn && !rawUrl.equals(urlStr)) sh.println("走镜像: " + urlStr);
        cancelFlag = false;

        new Thread(() -> {
            try { doDownload(urlStr, nameArg, doResume); }
            catch (Exception e) {
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (msg.contains("CANCELLED")) { sh.printlnOk("下载已取消"); return; }
                if ((msg.contains("Cleartext") || msg.contains("cleartext"))
                    && urlStr.startsWith("https://")) {
                    final String httpUrl = "http://" + urlStr.substring(8);
                    sh.ask("https 走不通，换 http 试试？\n" + httpUrl + "\n(y/n)", input -> {
                        if (input != null && input.trim().equalsIgnoreCase("y")) {
                            sh.println("改用 " + httpUrl);
                            new Thread(() -> {
                                try { doDownload(httpUrl, nameArg, doResume); }
                                catch (Exception e2) {
                                    String m2 = e2.getMessage();
                                    if (m2 != null && m2.contains("CANCELLED")) sh.printlnOk("已取消");
                                    else sh.printlnErr("http 也失败: " + m2);
                                }
                            }).start();
                        } else sh.println("已取消");
                    });
                    return;
                }
                sh.printlnErr("下载失败: " + e.getMessage());
            }
        }).start();
        return true;
    }

    private void doDownload(String urlStr, String nameArg, boolean doResume) throws Exception {
        File outDir = new File("/storage/emulated/0/Download");
        if (!outDir.exists()) outDir.mkdirs();

        String filename = nameArg;
        if (filename == null) {
            String path = new URL(urlStr).getPath();
            int slash = path.lastIndexOf('/');
            filename = slash >= 0 ? path.substring(slash + 1) : "download";
        }
        if (filename.isEmpty()) filename = "download";

        File outFile = doResume ? new File(outDir, filename) : buildUnique(outDir, filename);
        long existing = (doResume && outFile.exists()) ? outFile.length() : 0;

        HttpURLConnection conn = openWithRedirect(urlStr, existing);
        int code = conn.getResponseCode();
        if (code != 200 && code != 206) { sh.printlnErr("HTTP " + code + "，失败"); return; }

        boolean rangeOk = (code == 206);
        if (existing > 0 && !rangeOk) {
            sh.println("服务器不支持续传，从头下");
            existing = 0;
        }

        long totalLen;
        String lenHeader = conn.getHeaderField("Content-Length");
        totalLen = lenHeader != null
            ? (existing > 0 ? existing + Long.parseLong(lenHeader) : Long.parseLong(lenHeader))
            : -1;

        currentFile = filename;
        sh.println("文件名: " + filename);
        sh.println("总大小: " + (totalLen > 0 ? fmtSize(totalLen) : "未知"));
        if (existing > 0) sh.println("续传自: " + fmtSize(existing));
        sh.println("输 stopdl 取消");
        sh.printlnRaw("");

        InputStream is = conn.getInputStream();
        FileOutputStream fos = new FileOutputStream(outFile, existing > 0);

        byte[] buf = new byte[8192];
        int n;
        long downloaded = existing;
        long lastTime = System.currentTimeMillis();
        long lastBytes = existing;
        long startTime = System.currentTimeMillis();
        boolean firstBar = true;

        try {
            while ((n = is.read(buf)) > 0) {
                if (cancelFlag) throw new Exception("CANCELLED");
                fos.write(buf, 0, n);
                downloaded += n;
                long now = System.currentTimeMillis();
                if (now - lastTime >= 1000) {
                    long speed = (downloaded - lastBytes) * 1000 / (now - lastTime);
                    lastBytes = downloaded; lastTime = now;
                    String line;
                    if (totalLen > 0) {
                        int pct = (int) (downloaded * 100 / totalLen);
                        long remain = totalLen - downloaded;
                        int filled = pct * 20 / 100;
                        StringBuilder bar = new StringBuilder("[");
                        for (int i = 0; i < 20; i++) bar.append(i < filled ? "=" : " ");
                        bar.append("] ");
                        line = bar.toString() + pct + "%  " + fmtSize(downloaded)
                            + " / " + fmtSize(totalLen) + "  " + fmtSize(speed) + "/s"
                            + "  剩 " + (speed > 0 ? fmtTime(remain / speed) : "未知");
                    } else {
                        line = "已下 " + fmtSize(downloaded) + "  " + fmtSize(speed) + "/s";
                    }
                    if (firstBar) { sh.printlnOk(line); firstBar = false; }
                    else sh.printlnReplace(line);
                }
            }
        } finally {
            try { is.close(); } catch (Exception ignored) {}
            try { fos.close(); } catch (Exception ignored) {}
            currentFile = null;
            cancelFlag = false;
        }

        long totalMs = System.currentTimeMillis() - startTime;
        long avgSpeed = totalMs > 0 ? (downloaded - existing) * 1000 / totalMs : 0;

        sh.printlnRaw("");
        sh.printlnOk("完成: " + filename);
        sh.printlnOk("总大小: " + fmtSize(downloaded));
        sh.printlnOk("用时: " + (totalMs < 1000 ? totalMs + "毫秒" : fmtTime(totalMs / 1000)));
        sh.printlnOk("平均: " + fmtSize(avgSpeed) + "/s");
        sh.printlnOk("存到: " + outFile.getAbsolutePath());
    }

    private String fmtSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) return String.format("%.1f MB", b / 1024.0 / 1024);
        return String.format("%.2f GB", b / 1024.0 / 1024 / 1024);
    }

    private String fmtTime(long sec) {
        if (sec < 0) return "未知";
        if (sec < 60) return sec + "秒";
        if (sec < 3600) return (sec / 60) + "分" + (sec % 60) + "秒";
        return (sec / 3600) + "时" + ((sec % 3600) / 60) + "分" + (sec % 60) + "秒";
    }

    private File buildUnique(File dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) return f;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        int i = 1;
        while (true) {
            File g = new File(dir, base + "(" + i + ")" + ext);
            if (!g.exists()) return g;
            i++;
        }
    }

    private boolean imgInfo(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: imginfo <图片>"); return true; }
        File f = sh.resolve(p[1]);
        if (!f.exists() || !f.isFile()) { sh.printlnErr("文件不存在"); return true; }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
        if (opts.outWidth <= 0 || opts.outHeight <= 0) { sh.printlnErr("不认识的图片"); return true; }

        sh.println("文件: " + f.getName());
        sh.println("格式: " + detectFormat(opts.outMimeType, f.getName()));
        sh.println("尺寸: " + opts.outWidth + " x " + opts.outHeight);
        sh.println("大小: " + fmtSize(f.length()));
        return true;
    }

    private String detectFormat(String mime, String name) {
        if (mime != null) {
            if (mime.contains("png")) return "PNG";
            if (mime.contains("jpeg") || mime.contains("jpg")) return "JPEG";
            if (mime.contains("gif")) return "GIF";
            if (mime.contains("webp")) return "WEBP";
            if (mime.contains("bmp")) return "BMP";
        }
        String low = name.toLowerCase();
        if (low.endsWith(".png")) return "PNG";
        if (low.endsWith(".jpg") || low.endsWith(".jpeg")) return "JPEG";
        if (low.endsWith(".gif")) return "GIF";
        if (low.endsWith(".webp")) return "WEBP";
        return "未知";
    }

    private boolean curl(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: curl <url>"); return true; }
        final String urlStr = applyMirror(p[1]);
        new Thread(() -> {
            try {
                HttpURLConnection conn = openWithRedirect(urlStr, 0);
                if (conn.getResponseCode() != 200) { sh.printlnErr("HTTP " + conn.getResponseCode()); return; }
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) sh.println(line);
                br.close();
            } catch (Exception e) { sh.printlnErr("curl: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean ping(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: ping <host>"); return true; }
        final String host = p[1];
        new Thread(() -> {
            try {
                long start = System.currentTimeMillis();
                InetAddress addr = InetAddress.getByName(host);
                boolean ok = addr.isReachable(3000);
                long ms = System.currentTimeMillis() - start;
                if (ok) sh.printlnOk("ping " + host + ": 通，" + ms + "ms");
                else sh.printlnErr("ping " + host + ": 不通");
            } catch (Exception e) { sh.printlnErr("ping: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean ip(String[] p) {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int i = wm.getConnectionInfo().getIpAddress();
            sh.println("WiFi IP: " + (i & 0xFF) + "." + ((i >> 8) & 0xFF)
                + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF));
        } catch (Exception e) { sh.printlnErr("ip: " + e.getMessage()); }
        return true;
    }

    private boolean netstat(String[] p) {
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null) {
                sh.println("类型: " + ni.getTypeName());
                sh.println("状态: " + ni.getState());
                sh.println("已连接: " + ni.isConnected());
            } else sh.println("没网络");
        } catch (Exception e) { sh.printlnErr("netstat: " + e.getMessage()); }
        return true;
    }

    private boolean http(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: http <url>"); return true; }
        final String urlStr = applyMirror(p[1]);
        new Thread(() -> {
            try {
                HttpURLConnection conn = openWithRedirect(urlStr, 0);
                sh.println("状态码: " + conn.getResponseCode());
                for (Map.Entry<String, java.util.List<String>> e : conn.getHeaderFields().entrySet()) {
                    if (e.getKey() == null) continue;
                    sh.println(e.getKey() + ": " + String.join(", ", e.getValue()));
                }
            } catch (Exception e) { sh.printlnErr("http: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean dns(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: dns <域名>"); return true; }
        final String host = p[1];
        new Thread(() -> {
            try {
                for (InetAddress a : InetAddress.getAllByName(host)) sh.println(a.getHostAddress());
            } catch (Exception e) { sh.printlnErr("dns: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean url(String[] p) {
        if (p.length < 3) { sh.printlnErr("用法: url -e/-d <文本>"); return true; }
        boolean decode = p[1].equals("-d");
        try {
            if (decode) sh.println(URLDecoder.decode(p[2], "UTF-8"));
            else sh.println(URLEncoder.encode(p[2], "UTF-8"));
        } catch (Exception e) { sh.printlnErr("url: " + e.getMessage()); }
        return true;
    }

    private boolean whois(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: whois <域名>"); return true; }
        final String domain = p[1];
        new Thread(() -> {
            try {
                String json = httpGet("https://rdap.org/domain/" + domain);
                sh.println("域名: " + domain);
                sh.println(json.length() > 500 ? json.substring(0, 500) + "..." : json);
            } catch (Exception e) { sh.printlnErr("whois: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean ipInfo(String[] p) {
        final String ip = p.length >= 2 ? p[1] : "";
        new Thread(() -> {
            try { sh.println(httpGet("http://ip-api.com/json/" + ip)); }
            catch (Exception e) { sh.printlnErr("ipinfo: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean myIp4(String[] p) {
        new Thread(() -> {
            try { sh.printlnOk("公网 IPv4: " + httpGet("https://api.ipify.org?format=text")); }
            catch (Exception e) { sh.printlnErr("myip4: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean myIp6(String[] p) {
        new Thread(() -> {
            try { sh.printlnOk("公网 IP: " + httpGet("https://api64.ipify.org?format=text")); }
            catch (Exception e) { sh.printlnErr("myip6: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean dnsQuery(String[] p) {
        if (p.length < 3) { sh.printlnErr("用法: dnsq <dns> <域名>"); return true; }
        final String server = p[1], domain = p[2];
        new Thread(() -> {
            try {
                sh.println("查询 " + domain + " @ " + server);
                sh.println(httpGet("https://dns.google/resolve?name=" + domain
                    + "&type=A&edns_client_subnet=" + server));
            } catch (Exception e) { sh.printlnErr("dnsq: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean dnsType(String[] p, String type) {
        if (p.length < 2) { sh.printlnErr("用法: " + p[0] + " <域名>"); return true; }
        final String domain = p[1];
        final String t = type;
        new Thread(() -> {
            try {
                sh.println("查询 " + domain + " 的 " + t + " 记录:");
                sh.println(httpGet("https://dns.google/resolve?name=" + domain + "&type=" + t));
            } catch (Exception e) { sh.printlnErr(p[0] + ": " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean portCheck(String[] p) {
        if (p.length < 3) { sh.printlnErr("用法: portcheck <host> <port>"); return true; }
        final String host = p[1];
        final int port = LocalShell.parseInt(p[2], -1);
        if (port < 1 || port > 65535) { sh.printlnErr("端口 1~65535"); return true; }
        new Thread(() -> {
            try {
                Socket s = new Socket();
                long start = System.currentTimeMillis();
                s.connect(new java.net.InetSocketAddress(host, port), 3000);
                long ms = System.currentTimeMillis() - start;
                s.close();
                sh.printlnOk(host + ":" + port + " 开着，" + ms + "ms");
            } catch (Exception e) { sh.printlnErr(host + ":" + port + " 关了或不通"); }
        }).start();
        return true;
    }

    private boolean httpCode(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: httpcode <url>"); return true; }
        final String urlStr = applyMirror(p[1]);
        new Thread(() -> {
            try {
                HttpURLConnection conn = openWithRedirect(urlStr, 0);
                sh.printlnOk("状态码: " + conn.getResponseCode());
            } catch (Exception e) { sh.printlnErr("httpcode: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean httpTime(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: httptime <url>"); return true; }
        final String urlStr = applyMirror(p[1]);
        new Thread(() -> {
            try {
                long t0 = System.currentTimeMillis();
                openWithRedirect(urlStr, 0).getResponseCode();
                sh.printlnOk("耗时: " + (System.currentTimeMillis() - t0) + " ms");
            } catch (Exception e) { sh.printlnErr("httptime: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean headers(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: headers <url>"); return true; }
        final String urlStr = applyMirror(p[1]);
        new Thread(() -> {
            try {
                HttpURLConnection conn = openWithRedirect(urlStr, 0);
                conn.getResponseCode();
                for (Map.Entry<String, java.util.List<String>> e : conn.getHeaderFields().entrySet()) {
                    if (e.getKey() == null) continue;
                    sh.println(e.getKey() + ": " + String.join(", ", e.getValue()));
                }
            } catch (Exception e) { sh.printlnErr("headers: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean redir(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: redir <url>"); return true; }
        final String urlStr = applyMirror(p[1]);
        new Thread(() -> {
            try {
                String current = urlStr;
                for (int hop = 0; hop < 10; hop++) {
                    URL u = new URL(current);
                    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setRequestProperty("User-Agent", "MiniFileManager/1.0");
                    conn.setInstanceFollowRedirects(false);
                    int code = conn.getResponseCode();
                    sh.println("[" + hop + "] " + code + " " + current);
                    if (code >= 300 && code < 400) {
                        String loc = conn.getHeaderField("Location");
                        conn.disconnect();
                        if (loc == null) break;
                        if (loc.startsWith("/")) loc = u.getProtocol() + "://" + u.getHost() + loc;
                        current = loc;
                    } else { conn.disconnect(); break; }
                }
            } catch (Exception e) { sh.printlnErr("redir: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean speedTest(String[] p) {
        final String urlStr = p.length >= 2
            ? applyMirror(p[1])
            : "https://mirrors.aliyun.com/deepin/releases/20.9/deepin-desktop-community-20.9-amd64.iso";
        new Thread(() -> {
            try {
                URL u = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "MiniFileManager/1.0");
                if (conn.getResponseCode() != 200) { sh.printlnErr("HTTP " + conn.getResponseCode()); return; }
                sh.println("20 秒测速...");
                InputStream is = conn.getInputStream();
                byte[] buf = new byte[16384];
                int n;
                long total = 0;
                long start = System.currentTimeMillis();
                while ((n = is.read(buf)) > 0) {
                    total += n;
                    long elapsed = System.currentTimeMillis() - start;
                    if (elapsed > 20000) break;
                    if (elapsed > 1000 && elapsed % 2000 < 30) {
                        sh.printlnReplace("已下 " + fmtSize(total) + "  "
                            + fmtSize(total * 1000 / elapsed) + "/s");
                    }
                }
                is.close();
                long totalMs = System.currentTimeMillis() - start;
                sh.printlnRaw("");
                sh.printlnOk("共下 " + fmtSize(total) + "，平均 "
                    + fmtSize(totalMs > 0 ? total * 1000 / totalMs : 0) + "/s");
            } catch (Exception e) { sh.printlnErr("speedtest: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean trace(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: trace <host>"); return true; }
        final String host = p[1];
        new Thread(() -> {
            try {
                InetAddress target = InetAddress.getByName(host);
                sh.println("traceroute to " + host + " (" + target.getHostAddress() + ")");
                for (int ttl = 1; ttl <= 15; ttl++) {
                    long t0 = System.currentTimeMillis();
                    boolean ok = target.isReachable(1000);
                    long ms = System.currentTimeMillis() - t0;
                    if (ok) { sh.println(ttl + "  " + target.getHostAddress() + "  " + ms + " ms"); break; }
                    else sh.println(ttl + "  *  *  *");
                }
                sh.printlnOk("完事");
            } catch (Exception e) { sh.printlnErr("trace: " + e.getMessage()); }
        }).start();
        return true;
    }

    private boolean checkOnline(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: checkonline <url...>"); return true; }
        final String[] urls = new String[p.length - 1];
        System.arraycopy(p, 1, urls, 0, p.length - 1);
        new Thread(() -> {
            for (String u : urls) {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestMethod("HEAD");
                    sh.printlnOk(u + "  →  " + conn.getResponseCode());
                } catch (Exception e) { sh.printlnErr(u + "  →  不通"); }
            }
        }).start();
        return true;
    }

    private boolean ipLocal(String[] p) {
        try {
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface nif : Collections.list(nifs)) {
                if (nif.isLoopback() || !nif.isUp()) continue;
                for (InetAddress addr : Collections.list(nif.getInetAddresses()))
                    sh.println(nif.getName() + ": " + addr.getHostAddress());
            }
        } catch (Exception e) { sh.printlnErr("iplocal: " + e.getMessage()); }
        return true;
    }

    private boolean mac(String[] p) {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            String m = wm.getConnectionInfo().getMacAddress();
            if (m == null) sh.printlnErr("拿不到（Android 6+ 限制）");
            else sh.println("MAC: " + m);
        } catch (Exception e) { sh.printlnErr("mac: " + e.getMessage()); }
        return true;
    }

    private boolean gateway(String[] p) {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int g = wm.getDhcpInfo().gateway;
            sh.println("网关: " + (g & 0xFF) + "." + ((g >> 8) & 0xFF)
                + "." + ((g >> 16) & 0xFF) + "." + ((g >> 24) & 0xFF));
        } catch (Exception e) { sh.printlnErr("gateway: " + e.getMessage()); }
        return true;
    }

    private boolean netInfo(String[] p) {
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni == null) { sh.println("没网络"); return true; }
            sh.println("类型: " + ni.getTypeName());
            sh.println("状态: " + ni.getState());

            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int i = wm.getConnectionInfo().getIpAddress();
            sh.println("本机 IP: " + (i & 0xFF) + "." + ((i >> 8) & 0xFF)
                + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF));
            int g = wm.getDhcpInfo().gateway;
            sh.println("网关: " + (g & 0xFF) + "." + ((g >> 8) & 0xFF)
                + "." + ((g >> 16) & 0xFF) + "." + ((g >> 24) & 0xFF));
            String mac = wm.getConnectionInfo().getMacAddress();
            sh.println("MAC: " + (mac == null ? "拿不到" : mac));

            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface nif : Collections.list(nifs)) {
                if (nif.isLoopback() || !nif.isUp()) continue;
                sh.println(nif.getName() + ":");
                for (InetAddress addr : Collections.list(nif.getInetAddresses()))
                    sh.println("  " + addr.getHostAddress());
            }
        } catch (Exception e) { sh.printlnErr("netinfo: " + e.getMessage()); }
        return true;
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "MiniFileManager/1.0");
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }
}