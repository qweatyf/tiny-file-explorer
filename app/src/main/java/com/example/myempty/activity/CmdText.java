package com.example.myempty.activity2;

import android.content.Context;
import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CmdText {

    private final LocalShell sh;
    private final Context ctx;
    private final LocalShell.OnOutput out;

    public CmdText(LocalShell sh, Context ctx, LocalShell.OnOutput out) {
        this.sh = sh;
        this.ctx = ctx;
        this.out = out;
    }

    public boolean exec(String[] p) {
        String c = p[0];
        if (c.equals("grep")) return grep(p);
        if (c.equals("wc")) return wc(p);
        if (c.equals("sort")) return sort(p);
        if (c.equals("uniq")) return uniq(p);
        if (c.equals("cut")) return cut(p);
        if (c.equals("tr")) return tr(p);
        if (c.equals("sed")) return sed(p);
        if (c.equals("awk")) return awk(p);
        if (c.equals("base64")) return base64(p);
        if (c.equals("sha256sum")) return sha256(p);
        if (c.equals("md5sum")) return md5(p);
        if (c.equals("xxd")) return xxd(p);
        if (c.equals("hexdump")) return xxd(p);
        if (c.equals("rev")) return rev(p);
        if (c.equals("tac")) return tac(p);
        return false;
    }

    private String[] readLines(File f) {
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
            return content.split("\n");
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean grep(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: grep <关键词> <文件>  —— 搜内容");
            return true;
        }
        String pattern = p[1];
        File f = sh.resolve(p[2]);
        if (!f.exists()) {
            sh.printlnErr("grep: 文件不存在: " + p[2]);
            return true;
        }
        String[] lines = readLines(f);
        int n = 0;
        for (String line : lines) {
            if (line.contains(pattern)) {
                sh.println(line);
                n++;
            }
        }
        if (n == 0) sh.println("（没有匹配）");
        return true;
    }

    private boolean wc(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: wc <文件>          —— 统计行/词/字符");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("wc: 文件不存在: " + p[1]);
            return true;
        }
        String[] lines = readLines(f);
        int lineCount = lines.length;
        int wordCount = 0;
        int charCount = 0;
        for (String line : lines) {
            charCount += line.length() + 1;
            String[] words = line.trim().split("\\s+");
            for (String w : words) if (!w.isEmpty()) wordCount++;
        }
        sh.println("行数 词数 字符数 文件");
        sh.println(lineCount + "  " + wordCount + "  " + charCount + "  " + f.getName());
        return true;
    }

    private boolean sort(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: sort <文件>        —— 按行排序");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("sort: 文件不存在: " + p[1]);
            return true;
        }
        String[] lines = readLines(f);
        Arrays.sort(lines);
        for (String line : lines) sh.println(line);
        return true;
    }

    private boolean uniq(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: uniq <文件>        —— 去连续重复行");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("uniq: 文件不存在: " + p[1]);
            return true;
        }
        String[] lines = readLines(f);
        String last = null;
        for (String line : lines) {
            if (!line.equals(last)) sh.println(line);
            last = line;
        }
        return true;
    }

    private boolean cut(String[] p) {
        if (p.length < 4) {
            sh.printlnErr("用法: cut -d <分隔符> -f <列号> <文件>  —— 按列切");
            return true;
        }
        String delim = " ";
        int field = 1;
        String path = null;
        for (int i = 1; i < p.length; i++) {
            if (p[i].equals("-d") && i + 1 < p.length) {
                delim = p[++i];
            } else if (p[i].equals("-f") && i + 1 < p.length) {
                field = LocalShell.parseInt(p[++i], 1);
            } else {
                path = p[i];
            }
        }
        if (path == null) {
            sh.printlnErr("cut: 没指定文件");
            return true;
        }
        File f = sh.resolve(path);
        String[] lines = readLines(f);
        for (String line : lines) {
            String[] parts = line.split(java.util.regex.Pattern.quote(delim));
            if (field - 1 < parts.length) sh.println(parts[field - 1]);
        }
        return true;
    }

    private boolean tr(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: tr <原字符> <新字符>  —— 字符替换");
            return true;
        }
        String from = p[1];
        String to = p[2];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < from.length(); i++) {
            char c = from.charAt(i);
            int idx = to.indexOf(c);
            sb.append(idx >= 0 ? to.charAt(idx) : c);
        }
        sh.println(sb.toString());
        return true;
    }

    private boolean sed(String[] p) {
        if (p.length < 4) {
            sh.printlnErr("用法: sed s/旧/新/ <文件>  —— 按规则替换");
            return true;
        }
        String expr = p[1];
        String path = p[2];
        if (!expr.startsWith("s/")) {
            sh.printlnErr("sed: 只支持 s/旧/新/ 格式");
            return true;
        }
        String[] parts = expr.substring(2).split("/");
        if (parts.length < 2) {
            sh.printlnErr("sed: 表达式写错了");
            return true;
        }
        File f = sh.resolve(path);
        String[] lines = readLines(f);
        for (String line : lines) sh.println(line.replace(parts[0], parts[1]));
        return true;
    }

    private boolean awk(String[] p) {
        if (p.length < 3) {
            sh.printlnErr("用法: awk '{print $N}' <文件>  —— 按列取值");
            return true;
        }
        String expr = p[1];
        File f = sh.resolve(p[2]);
        String[] lines = readLines(f);

        String inner = expr;
        if (inner.startsWith("{") && inner.endsWith("}")) {
            inner = inner.substring(1, inner.length() - 1).trim();
        }
        if (!inner.startsWith("print")) {
            sh.printlnErr("awk: 只支持 print");
            return true;
        }
        String rest = inner.substring(5).trim();

        if (rest.equals("$0")) {
            for (String line : lines) sh.println(line);
            return true;
        }
        if (rest.startsWith("$")) {
            int n = LocalShell.parseInt(rest.substring(1), 1);
            for (String line : lines) {
                String[] cols = line.trim().split("\\s+");
                if (n - 1 < cols.length) sh.println(cols[n - 1]);
            }
            return true;
        }
        sh.printlnErr("awk: 不支持这个表达式");
        return true;
    }

    private boolean base64(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: base64 -e/-d <文本>  —— Base64 编解码");
            return true;
        }
        boolean decode = false;
        String text = null;
        for (int i = 1; i < p.length; i++) {
            if (p[i].equals("-d")) decode = true;
            else if (p[i].equals("-e")) decode = false;
            else text = p[i];
        }
        if (text == null) {
            sh.printlnErr("base64: 没输入内容");
            return true;
        }
        try {
            if (decode) {
                byte[] b = Base64.decode(text, Base64.DEFAULT);
                sh.println(new String(b, "UTF-8"));
            } else {
                sh.println(Base64.encodeToString(text.getBytes("UTF-8"), Base64.NO_WRAP));
            }
        } catch (Exception e) {
            sh.printlnErr("base64: " + e.getMessage());
        }
        return true;
    }

    private boolean sha256(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: sha256sum <文件>   —— 算 SHA-256");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("sha256sum: 文件不存在: " + p[1]);
            return true;
        }
        try {
            byte[] data = readBytes(f);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b & 0xFF));
            sh.println(sb + "  " + f.getName());
        } catch (Exception e) {
            sh.printlnErr("sha256sum: " + e.getMessage());
        }
        return true;
    }

    private boolean md5(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: md5sum <文件>      —— 算 MD5");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("md5sum: 文件不存在: " + p[1]);
            return true;
        }
        try {
            byte[] data = readBytes(f);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b & 0xFF));
            sh.println(sb + "  " + f.getName());
        } catch (Exception e) {
            sh.printlnErr("md5sum: " + e.getMessage());
        }
        return true;
    }

    private byte[] readBytes(File f) throws Exception {
        FileInputStream fis = new FileInputStream(f);
        byte[] buf = new byte[(int) f.length()];
        int read = 0;
        while (read < buf.length) {
            int n = fis.read(buf, read, buf.length - read);
            if (n < 0) break;
            read += n;
        }
        fis.close();
        return buf;
    }

    private boolean xxd(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: xxd <文件>         —— 十六进制查看");
            return true;
        }
        File f = sh.resolve(p[1]);
        if (!f.exists()) {
            sh.printlnErr("xxd: 文件不存在: " + p[1]);
            return true;
        }
        try {
            byte[] data = readBytes(f);
            for (int i = 0; i < data.length; i += 16) {
                StringBuilder hex = new StringBuilder();
                StringBuilder ascii = new StringBuilder();
                for (int j = 0; j < 16; j++) {
                    if (i + j < data.length) {
                        int b = data[i + j] & 0xFF;
                        hex.append(String.format("%02x ", b));
                        ascii.append(b >= 32 && b < 127 ? (char) b : '.');
                    } else {
                        hex.append("   ");
                    }
                }
                sh.println(String.format("%08x  %s %s", i, hex.toString(), ascii.toString()));
            }
        } catch (Exception e) {
            sh.printlnErr("xxd: " + e.getMessage());
        }
        return true;
    }

    private boolean rev(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: rev <文件>         —— 每行倒序");
            return true;
        }
        File f = sh.resolve(p[1]);
        for (String line : readLines(f)) {
            sh.println(new StringBuilder(line).reverse().toString());
        }
        return true;
    }

    private boolean tac(String[] p) {
        if (p.length < 2) {
            sh.printlnErr("用法: tac <文件>         —— 整文件倒序");
            return true;
        }
        File f = sh.resolve(p[1]);
        String[] lines = readLines(f);
        List<String> l = new ArrayList<>(Arrays.asList(lines));
        Collections.reverse(l);
        for (String s : l) sh.println(s);
        return true;
    }
}