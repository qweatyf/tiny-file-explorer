package com.example.myempty.activity2;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class MyServer extends NanoHTTPD {
    private final Context ctx;

    public MyServer(Context ctx) {
        super(8080);
        this.ctx = ctx;
    }

    @Override
    public Response serve(IHTTPSession session) {
        if ("/upload".equals(session.getUri())) {
            try {
                Map<String, String> files = new HashMap<>();
                session.parseBody(files);
                String hash = session.getParms().get("hash");
                String tmpPath = files.get("postData");
                if (tmpPath == null) {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                        "text/plain", "空的？");
                }

                File tmp = new File(tmpPath);
                byte[] enc = new byte[(int) tmp.length()];
                FileInputStream fis = new FileInputStream(tmp);
                int read = 0;
                while (read < enc.length) {
                    int n = fis.read(enc, read, enc.length - read);
                    if (n < 0) break;
                    read += n;
                }
                fis.close();

                String key = ctx.getString(R.string.transfer_key);
                byte[] dec = CryptoUtils.aesDecrypt(enc, key);

                String realHash = CryptoUtils.sha256(dec);
                if (hash != null && !hash.equals(realHash)) {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                        "text/plain", "hash 对不上");
                }

                File outDir = new File(ctx.getExternalFilesDir(null), "received");
                if (!outDir.exists()) outDir.mkdirs();
                File out = new File(outDir, "recv_" + System.currentTimeMillis() + ".bin");
                FileOutputStream fos = new FileOutputStream(out);
                fos.write(dec);
                fos.close();

                return newFixedLengthResponse(Response.Status.OK,
                    "text/plain", "ok: " + out.getName());
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                    "text/plain", "炸了：" + e.getMessage());
            }
        }

        String html = "<html><body><h1>服务器跑起来了</h1>"
            + "<p>收文件走 /upload</p></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }
}