package com.example.myempty.activity2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CmdExtra {

    private final LocalShell sh;
    private final Context ctx;
    private final LocalShell.OnOutput out;

    public CmdExtra(LocalShell sh, Context ctx, LocalShell.OnOutput out) {
        this.sh = sh;
        this.ctx = ctx;
        this.out = out;
    }

    public boolean exec(String[] p) {
        String c = p[0];
        if (c.equals("enc")) return enc(p);
        if (c.equals("dec")) return dec(p);
        if (c.equals("qrenc")) return qrEnc(p);
        if (c.equals("qrdec")) return qrDec(p);
        if (c.equals("myip")) return myIp(p);
        if (c.equals("portscan")) return portScan(p);
        if (c.equals("repeat")) return repeat(p);
        if (c.equals("after")) return after(p);
        if (c.equals("at")) return at(p);
        return false;
    }

    private boolean enc(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: enc <utf8|gbk|unicode|hex|base64> <文本>");
            return true;
        }
        String type = p[1].toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < p.length; i++) {
            if (i > 2) sb.append(" ");
            sb.append(p[i]);
        }
        String text = sb.toString();

        try {
            if (type.equals("utf8")) {
                byte[] b = text.getBytes("UTF-8");
                StringBuilder h = new StringBuilder();
                for (byte x : b) h.append(String.format("%02x ", x & 0xFF));
                sh.println("UTF-8 字节: " + h.toString().trim());
            } else if (type.equals("gbk")) {
                byte[] b = text.getBytes("GBK");
                StringBuilder h = new StringBuilder();
                for (byte x : b) h.append(String.format("%02x ", x & 0xFF));
                sh.println("GBK 字节: " + h.toString().trim());
            } else if (type.equals("unicode")) {
                StringBuilder u = new StringBuilder();
                for (char ch : text.toCharArray()) {
                    u.append("\\u").append(String.format("%04x", (int) ch));
                }
                sh.println("Unicode: " + u);
            } else if (type.equals("hex")) {
                byte[] b = text.getBytes("UTF-8");
                StringBuilder h = new StringBuilder();
                for (byte x : b) h.append(String.format("%02x ", x & 0xFF));
                sh.println(h.toString().trim());
            } else if (type.equals("base64")) {
                sh.println(android.util.Base64.encodeToString(
                    text.getBytes("UTF-8"), android.util.Base64.NO_WRAP));
            } else {
                sh.printlnErr("不认识的编码: " + type);
            }
        } catch (Exception e) {
            sh.printlnErr("编码失败: " + e.getMessage());
        }
        return true;
    }

    private boolean dec(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: dec <utf8|gbk|unicode|hex|base64> <内容>");
            return true;
        }
        String type = p[1].toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < p.length; i++) {
            if (i > 2) sb.append(" ");
            sb.append(p[i]);
        }
        String text = sb.toString();

        try {
            if (type.equals("hex") || type.equals("utf8") || type.equals("gbk")) {
                String clean = text.replaceAll("\\s+", "");
                if (clean.length() % 2 != 0) {
                    sh.printlnErr("hex 长度不对");
                    return true;
                }
                byte[] b = new byte[clean.length() / 2];
                for (int i = 0; i < b.length; i++) {
                    b[i] = (byte) Integer.parseInt(
                        clean.substring(i * 2, i * 2 + 2), 16);
                }
                String cs = type.equals("gbk") ? "GBK" : "UTF-8";
                sh.println(new String(b, cs));
            } else if (type.equals("base64")) {
                byte[] b = android.util.Base64.decode(text, android.util.Base64.DEFAULT);
                sh.println(new String(b, "UTF-8"));
            } else if (type.equals("unicode")) {
                StringBuilder s = new StringBuilder();
                int i = 0;
                while (i < text.length()) {
                    if (text.charAt(i) == '\\' && i + 5 < text.length()
                        && text.charAt(i + 1) == 'u') {
                        String h = text.substring(i + 2, i + 6);
                        s.append((char) Integer.parseInt(h, 16));
                        i += 6;
                    } else {
                        s.append(text.charAt(i));
                        i++;
                    }
                }
                sh.println(s.toString());
            } else {
                sh.printlnErr("不认识的编码: " + type);
            }
        } catch (Exception e) {
            sh.printlnErr("解码失败: " + e.getMessage());
        }
        return true;
    }

    private boolean qrEnc(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: qrenc <文本> [文件名]");
            return true;
        }

        final String text;
        final String fname;

        if (p.length >= 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < p.length - 1; i++) {
                if (i > 1) sb.append(" ");
                sb.append(p[i]);
            }
            text = sb.toString();
            fname = p[p.length - 1];
        } else {
            text = p[1];
            fname = "qr_" + System.currentTimeMillis() + ".png";
        }

        new Thread(() -> {
            try {
                Map<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                hints.put(EncodeHintType.MARGIN, 1);

                BitMatrix matrix = new MultiFormatWriter().encode(
                    text, BarcodeFormat.QR_CODE, 512, 512, hints);

                int w = matrix.getWidth();
                int h = matrix.getHeight();
                int[] pixels = new int[w * h];
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        pixels[y * w + x] = matrix.get(x, y)
                            ? Color.BLACK : Color.WHITE;
                    }
                }

                Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                bmp.setPixels(pixels, 0, w, 0, 0, w, h);

                File outDir = new File("/storage/emulated/0/Download");
                if (!outDir.exists()) outDir.mkdirs();
                File outFile = new File(outDir, fname);

                FileOutputStream fos = new FileOutputStream(outFile);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                bmp.recycle();

                sh.printlnOk("二维码已生成: " + outFile.getAbsolutePath());
                sh.printlnOk("内容: " + text);
            } catch (Exception e) {
                sh.printlnErr("生成二维码失败: " + e.getMessage());
            }
        }).start();
        return true;
    }

    private boolean qrDec(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: qrdec <图片路径>");
            return true;
        }
        final File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("文件不存在: " + p[1]);
            return true;
        }

        new Thread(() -> {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (bmp == null) {
                    sh.printlnErr("读不了这张图");
                    return;
                }

                int w = bmp.getWidth();
                int h = bmp.getHeight();
                int[] pixels = new int[w * h];
                bmp.getPixels(pixels, 0, w, 0, 0, w, h);
                bmp.recycle();

                LuminanceSource source = new RGBLuminanceSource(w, h, pixels);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                Result result = new MultiFormatReader().decode(bitmap);
                sh.printlnOk("识别结果: " + result.getText());
                sh.println("格式: " + result.getBarcodeFormat());
            } catch (Exception e) {
                sh.printlnErr("识别二维码失败: " + e.getMessage());
            }
        }).start();
        return true;
    }

    private boolean myIp(String[] p) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.ipify.org");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
                String line = br.readLine();
                br.close();

                if (line != null && !line.isEmpty()) {
                    sh.printlnOk("公网 IP: " + line);
                } else {
                    sh.printlnErr("拿不到公网 IP");
                }
            } catch (Exception e) {
                try {
                    URL url = new URL("https://ifconfig.me/ip");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    String line = br.readLine();
                    br.close();

                    if (line != null && !line.isEmpty()) {
                        sh.printlnOk("公网 IP: " + line);
                    } else {
                        sh.printlnErr("拿不到公网 IP");
                    }
                } catch (Exception e2) {
                    sh.printlnErr("查询失败: " + e2.getMessage());
                }
            }
        }).start();
        return true;
    }

    private boolean portScan(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: portscan <IP或域名> [起始端口] [结束端口]");
            return true;
        }

        final String host = p[1];
        final int startPort = p.length >= 3 ? LocalShell.parseInt(p[2], 1) : 1;
        final int endPort = p.length >= 4 ? LocalShell.parseInt(p[3], 1024) : 1024;

        new Thread(() -> {
            try {
                InetAddress addr = InetAddress.getByName(host);
                sh.println("扫描 " + addr.getHostAddress()
                    + " 端口 " + startPort + "~" + endPort);

                List<Integer> open = new ArrayList<>();

                for (int port = startPort; port <= endPort; port++) {
                    if (port < 1 || port > 65535) continue;
                    try {
                        Socket s = new Socket();
                        s.connect(new InetSocketAddress(addr, port), 200);
                        s.close();
                        open.add(port);
                        sh.printlnOk("端口 " + port + " 开着");
                    } catch (Exception ignored) {}
                }

                if (open.isEmpty()) {
                    sh.println("没扫到开着的端口");
                } else {
                    sh.printlnOk("共 " + open.size() + " 个端口开着");
                }
            } catch (Exception e) {
                sh.printlnErr("扫描失败: " + e.getMessage());
            }
        }).start();
        return true;
    }

    private boolean repeat(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: repeat <次数> <命令>");
            return true;
        }
        int n = LocalShell.parseInt(p[1], 1);
        if (n < 1) n = 1;
        if (n > 100) n = 100;

        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < p.length; i++) {
            if (i > 2) sb.append(" ");
            sb.append(p[i]);
        }
        String cmd = sb.toString();

        for (int i = 0; i < n; i++) {
            sh.println("[" + (i + 1) + "/" + n + "] " + cmd);
            sh.exec(cmd);
        }
        return true;
    }

    private boolean after(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: after <秒数> <命令>");
            return true;
        }
        int sec = LocalShell.parseInt(p[1], 5);
        if (sec < 1) sec = 1;
        if (sec > 3600) sec = 3600;

        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < p.length; i++) {
            if (i > 2) sb.append(" ");
            sb.append(p[i]);
        }
        final String cmd = sb.toString();
        final int seconds = sec;

        sh.printlnOk("已设定 " + seconds + " 秒后执行: " + cmd);

        new Thread(() -> {
            try {
                Thread.sleep(seconds * 1000L);
                sh.printlnOk("[" + seconds + "秒到] 执行: " + cmd);
                sh.exec(cmd);
            } catch (Exception e) {
                sh.printlnErr("定时任务失败: " + e.getMessage());
            }
        }).start();
        return true;
    }

    private boolean at(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: at <HH:mm> <命令>");
            return true;
        }
        String timeStr = p[1];
        if (!timeStr.matches("\\d{1,2}:\\d{2}")) {
            sh.printlnErr("时间格式要 HH:mm，比如 08:30");
            return true;
        }

        String[] hm = timeStr.split(":");
        final int targetH = Integer.parseInt(hm[0]);
        final int targetM = Integer.parseInt(hm[1]);
        if (targetH < 0 || targetH > 23 || targetM < 0 || targetM > 59) {
            sh.printlnErr("时间不对");
            return true;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < p.length; i++) {
            if (i > 2) sb.append(" ");
            sb.append(p[i]);
        }
        final String cmd = sb.toString();

        sh.printlnOk("已设定每天 " + timeStr + " 执行: " + cmd);
        sh.println("（App 关了就没了）");

        new Thread(() -> {
            while (true) {
                try {
                    Calendar c = Calendar.getInstance();
                    int h = c.get(Calendar.HOUR_OF_DAY);
                    int m = c.get(Calendar.MINUTE);

                    if (h == targetH && m == targetM) {
                        sh.printlnOk("[" + timeStr + " 到] 执行: " + cmd);
                        sh.exec(cmd);
                        Thread.sleep(61 * 1000L);
                    } else {
                        Thread.sleep(30 * 1000L);
                    }
                } catch (Exception e) {
                    break;
                }
            }
        }).start();
        return true;
    }
}