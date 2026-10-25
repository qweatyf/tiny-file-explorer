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
        this.sh = sh; this.ctx = ctx; this.out = out;
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
        if (c.equals("xxd") || c.equals("hexdump")) return xxd(p);
        if (c.equals("rev")) return rev(p);
        if (c.equals("tac")) return tac(p);
        if (c.equals("join")) return join(p);
        if (c.equals("paste")) return paste(p);
        if (c.equals("nl")) return nl(p);
        if (c.equals("column")) return column(p);
        if (c.equals("fmt")) return fmt(p);
        if (c.equals("splitlines") || c.equals("splitl")) return splitLines(p);
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
            return new String(buf, 0, read, "UTF-8").split("\n");
        } catch (Exception e) { return new String[0]; }
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

    private boolean grep(String[] p) {
        if (p.length < 3) { sh.printlnErr("用法: grep <关键词> <文件>"); return true; }
        File f = sh.resolve(p[2]);
        if (!f.exists()) { sh.printlnErr("grep: 文件不存在"); return true; }
        int n = 0;
        for (String line : readLines(f)) if (line.contains(p[1])) { sh.println(line); n++; }
        if (n == 0) sh.println("（没有匹配）");
        return true;
    }

    private boolean wc(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: wc <文件>"); return true; }
        File f = sh.resolve(p[1]);
        if (!f.exists()) { sh.printlnErr("wc: 文件不存在"); return true; }
        String[] lines = readLines(f);
        int words = 0, chars = 0;
        for (String line : lines) {
            chars += line.length() + 1;
            for (String w : line.trim().split("\\s+")) if (!w.isEmpty()) words++;
        }
        sh.println("行数 词数 字符数 文件");
        sh.println(lines.length + "  " + words + "  " + chars + "  " + f.getName());
        return true;
    }

    private boolean sort(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: sort <文件>"); return true; }
        File f = sh.resolve(p[1]);
        if (!f.exists()) { sh.printlnErr("sort: 文件不存在"); return true; }
        String[] lines = readLines(f);
        Arrays.sort(lines);
        for (String line : lines) sh.println(line);
        return true;
    }

    private boolean uniq(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: uniq <文件>"); return true; }
        File f = sh.resolve(p[1]);
        if (!f.exists()) { sh.printlnErr("uniq: 文件不存在"); return true; }
        String last = null;
        for (String line : readLines(f)) {
            if (!line.equals(last)) sh.println(line);
            last = line;
        }
        return true;
    }

    private boolean cut(String[] p) {
        if (p.length < 4) { sh.printlnErr("用法: cut -d <分隔符> -f <列号> <文件>"); return true; }
        String delim = " ";
        int field = 1;
        String path = null;
        for (int i = 1; i < p.length; i++) {
            if (p[i].equals("-d") && i + 1 < p.length) delim = p[++i];
            else if (p[i].equals("-f") && i + 1 < p.length) field = LocalShell.parseInt(p[++i], 1);
            else path = p[i];
        }
        if (path == null) { sh.printlnErr("cut: 没指定文件"); return true; }
        for (String line : readLines(sh.resolve(path))) {
            String[] parts = line.split(java.util.regex.Pattern.quote(delim));
            if (field - 1 < parts.length) sh.println(parts[field - 1]);
        }
        return true;
    }

    private boolean tr(String[] p) {
        if (p.length < 3) { sh.printlnErr("用法: tr <原字符> <新字符>"); return true; }
        String from = p[1], to = p[2];
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
        if (p.length < 3) { sh.printlnErr("用法: sed s/旧/新/ <文件>"); return true; }
        String expr = p[1], path = p[2];
        if (!expr.startsWith("s/")) { sh.printlnErr("sed: 只支持 s/旧/新/"); return true; }
        String[] parts = expr.substring(2).split("/");
        if (parts.length < 2) { sh.printlnErr("sed: 表达式错"); return true; }
        for (String line : readLines(sh.resolve(path))) sh.println(line.replace(parts[0], parts[1]));
        return true;
    }

    private boolean awk(String[] p) {
        if (p.length < 3) { sh.printlnErr("用法: awk '{print $N}' <文件>"); return true; }
        String inner = p[1];
        if (inner.startsWith("{") && inner.endsWith("}"))
            inner = inner.substring(1, inner.length() - 1).trim();
        if (!inner.startsWith("print")) { sh.printlnErr("awk: 只支持 print"); return true; }
        String rest = inner.substring(5).trim();
        String[] lines = readLines(sh.resolve(p[2]));
        if (rest.equals("$0")) { for (String line : lines) sh.println(line); return true; }
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
        if (p.length < 2) { sh.printlnErr("用法: base64 -e/-d <文本>"); return true; }
        boolean decode = false;
        String text = null;
        for (int i = 1; i < p.length; i++) {
            if (p[i].equals("-d")) decode = true;
            else if (p[i].equals("-e")) decode = false;
            else text = p[i];
        }
        if (text == null) { sh.printlnErr("base64: 没输入"); return true; }
        try {
            if (decode) sh.println(new String(Base64.decode(text, Base64.DEFAULT), "UTF-8"));
            else sh.println(Base64.encodeToString(text.getBytes("UTF-8"), Base64.NO_WRAP));
        } catch (Exception e) { sh.printlnErr("base64: " + e.getMessage()); }
        return true;
    }

    private boolean sha256(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: sha256sum <文件>"); return true; }
        File f = sh.resolve(p[1]);
        if (!f.exists()) { sh.printlnErr("sha256sum: 文件不存在"); return true; }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(readBytes(f));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b & 0xFF));
            sh.println(sb + "  " + f.getName());
        } catch (Exception e) { sh.printlnErr("sha256sum: " + e.getMessage()); }
        return true;
    }

    private boolean md5(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: md5sum <文件>"); return true; }
        File f = sh.resolve(p[1]);
        if (!f.exists()) { sh.printlnErr("md5sum: 文件不存在"); return true; }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(readBytes(f));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b & 0xFF));
            sh.println(sb + "  " + f.getName());
        } catch (Exception e) { sh.printlnErr("md5sum: " + e.getMessage()); }
        return true;
    }

    private boolean xxd(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: xxd <文件>"); return true; }
        File f = sh.resolve(p[1]);
        if (!f.exists()) { sh.printlnErr("xxd: 文件不存在"); return true; }
        try {
            byte[] data = readBytes(f);
            for (int i = 0; i < data.length; i += 16) {
                StringBuilder hex = new StringBuilder(), ascii = new StringBuilder();
                for (int j = 0; j < 16; j++) {
                    if (i + j < data.length) {
                        int b = data[i + j] & 0xFF;
                        hex.append(String.format("%02x ", b));
                        ascii.append(b >= 32 && b < 127 ? (char) b : '.');
                    } else hex.append("   ");
                }
                sh.println(String.format("%08x  %s %s", i, hex.toString(), ascii.toString()));
            }
        } catch (Exception e) { sh.printlnErr("xxd: " + e.getMessage()); }
        return true;
    }

    private boolean rev(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: rev <文件>"); return true; }
        for (String line : readLines(sh.resolve(p[1])))
            sh.println(new StringBuilder(line).reverse().toString());
        return true;
    }

    private boolean tac(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: tac <文件>"); return true; }
        List<String> l = new ArrayList<>(Arrays.asList(readLines(sh.resolve(p[1]))));
        Collections.reverse(l);
        for (String s : l) sh.println(s);
        return true;
    }

    private boolean join(String[] p) {
        if (p.length < 3) { sh.printlnErr("用法: join <文件1> <文件2>"); return true; }
        String[] a = readLines(sh.resolve(p[1]));
        String[] b = readLines(sh.resolve(p[2]));
        int max = Math.max(a.length, b.length);
        for (int i = 0; i < max; i++)
            sh.println((i < a.length ? a[i] : "") + " " + (i < b.length ? b[i] : ""));
        return true;
    }

    private boolean paste(String[] p) {
        if (p.length < 3) { sh.printlnErr("用法: paste <文件1> <文件2>"); return true; }
        String[] a = readLines(sh.resolve(p[1]));
        String[] b = readLines(sh.resolve(p[2]));
        int max = Math.max(a.length, b.length);
        for (int i = 0; i < max; i++)
            sh.println((i < a.length ? a[i] : "") + "\t" + (i < b.length ? b[i] : ""));
        return true;
    }

    private boolean nl(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: nl <文件>"); return true; }
        String[] lines = readLines(sh.resolve(p[1]));
        for (int i = 0; i < lines.length; i++)
            sh.println(String.format("%6d\t%s", i + 1, lines[i]));
        return true;
    }

    private boolean column(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: column <文件>"); return true; }
        String[] lines = readLines(sh.resolve(p[1]));
        int maxWidth = 0;
        for (String line : lines) {
            for (String cell : line.split("\t")) if (cell.length() > maxWidth) maxWidth = cell.length();
        }
        for (String line : lines) {
            StringBuilder sb = new StringBuilder();
            for (String cell : line.split("\t")) sb.append(String.format("%-" + (maxWidth + 2) + "s", cell));
            sh.println(sb.toString());
        }
        return true;
    }

    private boolean fmt(String[] p) {
        if (p.length < 2) { sh.printlnErr("用法: fmt <文件>"); return true; }
        String[] lines = readLines(sh.resolve(p[1]));
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) {
                if (sb.length() > 0) { sh.println(sb.toString().trim()); sb.setLength(0); }
                sh.println("");
                continue;
            }
            if (sb.length() > 0) sb.append(" ");
            sb.append(t);
            if (sb.length() >= 70) { sh.println(sb.toString().trim()); sb.setLength(0); }
        }
        if (sb.length() > 0) sh.println(sb.toString().trim());
        return true;
    }

    private boolean splitLines(String[] p) {
        if (p.length < 3) { sh.printlnErr("用法: splitlines <文件> <每片行数>"); return true; }
        File f = sh.resolve(p[1]);
        int perFile = LocalShell.parseInt(p[2], 100);
        if (perFile < 1) perFile = 100;
        String[] lines = readLines(f);
        try {
            int part = 1;
            for (int i = 0; i < lines.length; i += perFile) {
                File out = new File(f.getParent(), f.getName() + ".part" + part);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
                for (int j = i; j < Math.min(i + perFile, lines.length); j++) {
                    fos.write(lines[j].getBytes("UTF-8"));
                    fos.write("\n".getBytes("UTF-8"));
                }
                fos.close();
                sh.printlnOk("已生成: " + out.getName());
                part++;
            }
        } catch (Exception e) { sh.printlnErr("splitlines: " + e.getMessage()); }
        return true;
    }
}