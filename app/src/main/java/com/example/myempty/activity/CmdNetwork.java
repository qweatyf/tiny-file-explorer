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
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

public class CmdNetwork {

    private final LocalShell sh;
    private final Context ctx;
    private final LocalShell.OnOutput out;

    private static volatile boolean cancelFlag = false;
    private static volatile String currentFile = null;

    private static final String[] MIRRORS = {
        "https://ghproxy.com/",
        "https://raw.gitmirror.com/",
        "https://ghproxy.net/",
        "https://mirror.ghproxy.com/",
        "https://gh.api.99988866.xyz/",
        "https://gh-proxy.com/"
    };

    private boolean mirrorOn = false;
    private int mirrorIdx = 0;
    private String customMirror = null;

    public CmdNetwork(LocalShell sh, Context ctx, LocalShell.OnOutput out) {
        this.sh = sh;
        this.ctx = ctx;
        this.out = out;
    }

    public static void cancelCurrent() {
        cancelFlag = true;
    }

    public static boolean isDownloading() {
        return currentFile != null;
    }

    public boolean exec(String[] p) {
        String c = p[0];
        if (c.equals("wget")) return download(p);
        if (c.equals("download")) return download(p);
        if (c.equals("fetch")) return download(p);
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
        return false;
    }

    private boolean stopDownload(String[] p) {
        if (currentFile == null) {
            sh.printlnErr("当前没有在下载");
            return true;
        }
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
        if (!mirrorOn) return url;
        if (url == null) return url;
        String low = url.toLowerCase();
        if (!low.contains("github.com") && !low.contains("githubusercontent.com")) {
            return url;
        }
        if (low.startsWith("https://ghproxy.com/")
            || low.startsWith("https://raw.gitmirror.com/")
            || low.startsWith("https://ghproxy.net/")
            || low.startsWith("https://mirror.ghproxy.com/")
            || low.startsWith("https://gh.api.99988866.xyz/")
            || low.startsWith("https://gh-proxy.com/")) {
            return url;
        }
        return getMirror() + url;
    }

    private boolean ghMirror(String[] p) {
        if (p.length < 2) {
            sh.println("GitHub 镜像状态: " + (mirrorOn ? "已开启" : "已关闭"));
            sh.println("当前镜像: " + getMirror());
            if (customMirror != null) {
                sh.println("(自定义镜像)");
            } else {
                sh.println("当前是第 " + (mirrorIdx + 1) + " 个内置镜像");
            }
            sh.println("");
            sh.println("用法:");
            sh.println("  gh-mirror on        开启镜像");
            sh.println("  gh-mirror off       关闭镜像");
            sh.println("  gh-mirror list      列出内置镜像");
            sh.println("  gh-mirror use 3     切到第 3 个镜像");
            sh.println("  gh-mirror set URL   自定义镜像地址");
            sh.println("  gh-mirror reset     恢复默认");
            return true;
        }

        String sub = p[1].toLowerCase();

        if (sub.equals("on")) {
            mirrorOn = true;
            sh.printlnOk("GitHub 镜像已开启");
            sh.println("当前镜像: " + getMirror());
            return true;
        }

        if (sub.equals("off")) {
            mirrorOn = false;
            sh.printlnOk("GitHub 镜像已关闭");
            return true;
        }

        if (sub.equals("list")) {
            sh.println("内置镜像列表:");
            for (int i = 0; i < MIRRORS.length; i++) {
                String mark = (i == mirrorIdx && customMirror == null) ? " *" : "";
                sh.println("  " + (i + 1) + ". " + MIRRORS[i] + mark);
            }
            return true;
        }

        if (sub.equals("use")) {
            if (p.length < 3) {
                sh.printlnErr("用法: gh-mirror use <编号>");
                return true;
            }
            int n = LocalShell.parseInt(p[2], -1);
            if (n < 1 || n > MIRRORS.length) {
                sh.printlnErr("编号要在 1~" + MIRRORS.length + " 之间");
                return true;
            }
            mirrorIdx = n - 1;
            customMirror = null;
            sh.printlnOk("已切到镜像 " + n + ": " + MIRRORS[mirrorIdx]);
            return true;
        }

        if (sub.equals("set")) {
            if (p.length < 3) {
                sh.printlnErr("用法: gh-mirror set <镜像地址>");
                return true;
            }
            String m = p[2];
            if (!m.endsWith("/")) m = m + "/";
            customMirror = m;
            sh.printlnOk("已设自定义镜像: " + customMirror);
            return true;
        }

        if (sub.equals("reset")) {
            customMirror = null;
            mirrorIdx = 0;
            sh.printlnOk("已恢复默认镜像");
            return true;
        }

        sh.printlnErr("未知子命令: " + sub);
        sh.printlnErr("输入 gh-mirror 看用法");
        return true;
    }

    private HttpURLConnection openWithRedirect(String urlStr, long rangeStart) throws Exception {
        String current = urlStr;
        int maxHop = 5;

        for (int hop = 0; hop < maxHop; hop++) {
            URL url = new URL(current);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setInstanceFollowRedirects(false);

            if (rangeStart > 0) {
                conn.setRequestProperty("Range", "bytes=" + rangeStart + "-");
            }

            conn.connect();

            int code = conn.getResponseCode();

            if (code == 301 || code == 302 || code == 303
                || code == 307 || code == 308) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                if (loc == null) {
                    throw new Exception("重定向但没给 Location");
                }
                if (loc.startsWith("/")) {
                    URL base = new URL(current);
                    String proto = base.getProtocol();
                    String host = base.getHost();
                    int port = base.getPort();
                    String portStr = port > 0 ? ":" + port : "";
                    loc = proto + "://" + host + portStr + loc;
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
            sh.printlnErr("用法: " + p[0] + " <链接> [文件名]  —— 下载文件到 Download");
            sh.printlnErr("      " + p[0] + " -c <链接> [文件名]  —— 断点续传");
            sh.printlnErr("      stopdl                  —— 取消当前下载");
            return true;
        }

        if (currentFile != null) {
            sh.printlnErr("已有下载在进行，先 stopdl 取消，或等它下完");
            return true;
        }

        boolean resume = false;
        int argStart = 1;
        for (int i = 1; i < p.length; i++) {
            if (p[i].equals("-c") || p[i].equals("--continue")) {
                resume = true;
                argStart = i + 1;
            } else {
                break;
            }
        }

        if (argStart >= p.length) {
            sh.printlnErr("没给链接");
            return true;
        }

        final String rawUrl = p[argStart];
        final String urlStr = applyMirror(rawUrl);
        final String nameArg = p.length > argStart + 1 ? p[argStart + 1] : null;
        final boolean doResume = resume;

        if (mirrorOn && !rawUrl.equals(urlStr)) {
            sh.println("走镜像: " + urlStr);
        }

        cancelFlag = false;

        new Thread(() -> {
            try {
                doDownload(urlStr, nameArg, doResume);
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg == null) msg = "";

                if (msg.contains("CANCELLED")) {
                    sh.printlnOk("下载已取消");
                    return;
                }

                boolean isCleartextErr = msg.contains("Cleartext")
                    || msg.contains("cleartext");

                if (isCleartextErr && urlStr.startsWith("https://")) {
                    final String httpUrl = "http://" + urlStr.substring(8);
                    sh.ask("https 走不通，要不要换 http 试试？\n" + httpUrl + "\n(y / n)",
                        input -> {
                            if (input != null && input.trim().equalsIgnoreCase("y")) {
                                sh.println("改用 http 重试: " + httpUrl);
                                new Thread(() -> {
                                    try {
                                        doDownload(httpUrl, nameArg, doResume);
                                    } catch (Exception e2) {
                                        String m2 = e2.getMessage();
                                        if (m2 != null && m2.contains("CANCELLED")) {
                                            sh.printlnOk("下载已取消");
                                        } else {
                                            sh.printlnErr("http 也失败: " + m2);
                                        }
                                    }
                                }).start();
                            } else {
                                sh.println("已取消");
                            }
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
            URL u = new URL(urlStr);
            String path = u.getPath();
            int slash = path.lastIndexOf('/');
            filename = slash >= 0 ? path.substring(slash + 1) : "download";
        }
        if (filename.isEmpty()) filename = "download";

        File outFile;
        if (doResume) {
            outFile = new File(outDir, filename);
        } else {
            outFile = buildUnique(outDir, filename);
        }

        long existing = 0;
        if (doResume && outFile.exists()) {
            existing = outFile.length();
        }

        HttpURLConnection conn = openWithRedirect(urlStr, existing);

        int code = conn.getResponseCode();
        boolean serverSupportsRange = (code == 206);

        if (code != 200 && code != 206) {
            sh.printlnErr("HTTP 状态码 " + code + "，下载失败");
            return;
        }

        if (existing > 0 && !serverSupportsRange) {
            sh.println("服务器不支持断点续传，从头下");
            existing = 0;
        }

        long totalLen;
        String lenHeader = conn.getHeaderField("Content-Length");
        if (lenHeader != null) {
            long l = Long.parseLong(lenHeader);
            totalLen = existing > 0 ? existing + l : l;
        } else {
            totalLen = -1;
        }

        currentFile = filename;

        sh.println("文件名: " + filename);
        sh.println("总大小: " + (totalLen > 0 ? fmtSize(totalLen) : "未知"));
        if (existing > 0) {
            sh.println("已下载: " + fmtSize(existing) + "（续传）");
        }
        sh.println("下载中，输 stopdl 可取消");
        sh.printlnRaw("");

        InputStream is = conn.getInputStream();
        FileOutputStream fos;
        if (existing > 0) {
            fos = new FileOutputStream(outFile, true);
        } else {
            fos = new FileOutputStream(outFile);
        }

        byte[] buf = new byte[8192];
        int n;
        long downloaded = existing;
        long lastTime = System.currentTimeMillis();
        long lastBytes = existing;
        long startTime = System.currentTimeMillis();
        boolean firstBar = true;

        try {
            while ((n = is.read(buf)) > 0) {
                if (cancelFlag) {
                    throw new Exception("CANCELLED");
                }

                fos.write(buf, 0, n);
                downloaded += n;

                long now = System.currentTimeMillis();
                if (now - lastTime >= 1000) {
                    long speed = (downloaded - lastBytes) * 1000 / (now - lastTime);
                    lastBytes = downloaded;
                    lastTime = now;

                    if (totalLen > 0) {
                        int pct = (int) (downloaded * 100 / totalLen);

                        long remainBytes = totalLen - downloaded;
                        String remainStr;
                        if (speed > 0) {
                            long remainSec = remainBytes / speed;
                            remainStr = fmtTime(remainSec);
                        } else {
                            remainStr = "未知";
                        }

                        int barLen = 20;
                        int filled = pct * barLen / 100;
                        StringBuilder bar = new StringBuilder();
                        bar.append("[");
                        for (int i = 0; i < barLen; i++) {
                            bar.append(i < filled ? "=" : " ");
                        }
                        bar.append("] ");

                        String line = bar.toString() + pct + "%  "
                            + fmtSize(downloaded) + " / " + fmtSize(totalLen)
                            + "  " + fmtSize(speed) + "/s"
                            + "  剩 " + remainStr;

                        if (firstBar) {
                            sh.printlnOk(line);
                            firstBar = false;
                        } else {
                            sh.printlnReplace(line);
                        }
                    } else {
                        String line = "已下 " + fmtSize(downloaded)
                            + "  " + fmtSize(speed) + "/s";
                        if (firstBar) {
                            sh.printlnOk(line);
                            firstBar = false;
                        } else {
                            sh.printlnReplace(line);
                        }
                    }
                }
            }
        } finally {
            try { is.close(); } catch (Exception ignored) {}
            try { fos.close(); } catch (Exception ignored) {}
            currentFile = null;
            cancelFlag = false;
        }

        long totalMs = System.currentTimeMillis() - startTime;
        long totalSec = totalMs / 1000;
        long avgSpeed = totalMs > 0 ? (downloaded - existing) * 1000 / totalMs : 0;

        String usedTime;
        if (totalMs < 1000) {
            usedTime = totalMs + "毫秒";
        } else {
            usedTime = fmtTime(totalSec);
        }

        sh.printlnRaw("");
        sh.printlnOk("下载完成: " + filename);
        sh.printlnOk("总大小: " + fmtSize(downloaded));
        sh.printlnOk("用时: " + usedTime);
        sh.printlnOk("平均速度: " + fmtSize(avgSpeed) + "/s");
        sh.printlnOk("保存到: " + outFile.getAbsolutePath());
    }

    private String fmtSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / 1024.0 / 1024);
        return String.format("%.2f GB", bytes / 1024.0 / 1024 / 1024);
    }

    private String fmtTime(long sec) {
        if (sec < 0) return "未知";
        if (sec < 60) return sec + "秒";
        if (sec < 3600) {
            long m = sec / 60;
            long s = sec % 60;
            return m + "分" + s + "秒";
        }
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        return h + "时" + m + "分" + s + "秒";
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
        if (p.length < 2) {
            sh.printlnErr("用法: imginfo <图片路径>  —— 看图片信息");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("文件不存在: " + p[1]);
            return true;
        }
        if (!f.isFile()) {
            sh.printlnErr("不是文件");
            return true;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), opts);

        if (opts.outWidth <= 0 || opts.outHeight <= 0) {
            sh.printlnErr("不认识的图片格式");
            return true;
        }

        long size = f.length();
        String format = detectFormat(opts.outMimeType, f.getName());
        double mp = (double) opts.outWidth * opts.outHeight / 1000000.0;

        sh.println("文件: " + f.getName());
        sh.println("路径: " + f.getAbsolutePath());
        sh.println("格式: " + format);
        sh.println("尺寸: " + opts.outWidth + " x " + opts.outHeight);
        sh.println("像素: " + String.format("%.2f", mp) + " MP");
        sh.println("大小: " + fmtSize(size));

        if (opts.outWidth > 0 && opts.outHeight > 0) {
            double ratio = (double) opts.outWidth / opts.outHeight;
            String ratioStr;
            if (Math.abs(ratio - 1.0) < 0.02) ratioStr = "1:1";
            else if (Math.abs(ratio - 4.0 / 3.0) < 0.02) ratioStr = "4:3";
            else if (Math.abs(ratio - 3.0 / 4.0) < 0.02) ratioStr = "3:4";
            else if (Math.abs(ratio - 16.0 / 9.0) < 0.02) ratioStr = "16:9";
            else if (Math.abs(ratio - 9.0 / 16.0) < 0.02) ratioStr = "9:16";
            else ratioStr = String.format("%.2f", ratio);
            sh.println("比例: " + ratioStr);
        }

        return true;
    }

    private String detectFormat(String mime, String name) {
        if (mime != null) {
            if (mime.contains("png")) return "PNG";
            if (mime.contains("jpeg") || mime.contains("jpg")) return "JPEG";
            if (mime.contains("gif")) return "GIF";
            if (mime.contains("webp")) return "WEBP";
            if (mime.contains("bmp")) return "BMP";
            if (mime.contains("heic") || mime.contains("heif")) return "HEIC";
            if (mime.contains("avif")) return "AVIF";
        }
        String low = name.toLowerCase();
        if (low.endsWith(".png")) return "PNG";
        if (low.endsWith(".jpg") || low.endsWith(".jpeg")) return "JPEG";
        if (low.endsWith(".gif")) return "GIF";
        if (low.endsWith(".webp")) return "WEBP";
        if (low.endsWith(".bmp")) return "BMP";
        if (low.endsWith(".heic") || low.endsWith(".heif")) return "HEIC";
        return "未知";
    }

    private boolean curl(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: curl <链接>        —— 取网页源码");
            return true;
        }
        final String rawUrl = p[1];
        final String urlStr = applyMirror(rawUrl);

        if (mirrorOn && !rawUrl.equals(urlStr)) {
            sh.println("走镜像: " + urlStr);
        }

        new Thread(() -> {
            try {
                HttpURLConnection conn = openWithRedirect(urlStr, 0);
                int code = conn.getResponseCode();
                if (code != 200) {
                    sh.printlnErr("HTTP 状态码 " + code);
                    return;
                }
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) sh.println(line);
                br.close();
            } catch (Exception e) {
                sh.printlnErr("curl 失败: " + e.getMessage());
            }
        }).start();
        return true;
    }

    private boolean ping(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: ping <域名或IP>    —— 测连通");
            return true;
        }
        final String host = p[1];
        new Thread(() -> {
            try {
                long start = System.currentTimeMillis();
                InetAddress addr = InetAddress.getByName(host);
                boolean reachable = addr.isReachable(3000);
                long ms = System.currentTimeMillis() - start;
                if (reachable) {
                    sh.printlnOk("ping " + host + ": 通，" + ms + "ms");
                } else {
                    sh.printlnErr("ping " + host + ": 不通");
                }
            } catch (Exception e) {
                sh.printlnErr("ping 失败: " + e.getMessage());
            }
        }).start();
        return true;
    }

    private boolean ip(String[] p) {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
            int ipInt = wm.getConnectionInfo().getIpAddress();
            String ip = (ipInt & 0xFF) + "." + ((ipInt >> 8) & 0xFF)
                + "." + ((ipInt >> 16) & 0xFF) + "." + ((ipInt >> 24) & 0xFF);
            sh.println("WiFi IP: " + ip);
        } catch (Exception e) {
            sh.printlnErr("ip: " + e.getMessage());
        }
        return true;
    }

    private boolean netstat(String[] p) {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null) {
                sh.println("网络类型: " + ni.getTypeName());
                sh.println("状态: " + ni.getState());
                sh.println("已连接: " + ni.isConnected());
            } else {
                sh.println("没网络");
            }
        } catch (Exception e) {
            sh.printlnErr("netstat: " + e.getMessage());
        }
        return true;
    }

    private boolean http(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: http <链接>        —— 看响应头");
            return true;
        }
        final String rawUrl = p[1];
        final String urlStr = applyMirror(rawUrl);

        new Thread(() -> {
            try {
                HttpURLConnection conn = openWithRedirect(urlStr, 0);
                int code = conn.getResponseCode();
                sh.println("状态码: " + code);
                for (Map.Entry<String, java.util.List<String>> e
                    : conn.getHeaderFields().entrySet()) {
                    if (e.getKey() == null) continue;
                    sh.println(e.getKey() + ": " + String.join(", ", e.getValue()));
                }
            } catch (Exception e) {
                sh.printlnErr("http 失败: " + e.getMessage());
            }
        }).start();
        return true;
    }

    private boolean dns(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: dns <域名>         —— 解析域名");
            return true;
        }
        final String host = p[1];
        new Thread(() -> {
            try {
                InetAddress[] addrs = InetAddress.getAllByName(host);
                for (InetAddress a : addrs) sh.println(a.getHostAddress());
            } catch (Exception e) {
                sh.printlnErr("dns 失败: " + e.getMessage());
            }
        }).start();
        return true;
    }

    private boolean url(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: url -e/-d <文本>   —— URL 编解码");
            return true;
        }
        boolean decode = p[1].equals("-d");
        String text = p[2];
        try {
            if (decode) sh.println(URLDecoder.decode(text, "UTF-8"));
            else sh.println(URLEncoder.encode(text, "UTF-8"));
        } catch (Exception e) {
            sh.printlnErr("url 失败: " + e.getMessage());
        }
        return true;
    }
}