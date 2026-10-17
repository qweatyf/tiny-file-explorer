package com.example.myempty.activity2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class ChatServerRemote extends NanoHTTPD {

    private final File dataDir;
    private final String token;
    private final String transferKey;

    private final List<Map<String, Object>> msgs = new ArrayList<>();
    private long nextId = 1;

    private final Map<String, Long> online = new HashMap<>();
    private final Map<String, Boolean> muted = new HashMap<>();
    private final List<String> kicked = new ArrayList<>();
    private static final long ONLINE_TIMEOUT = 10000L;

    public ChatServerRemote(int port, String dataDir, String token, String transferKey) {
        super(port);
        this.dataDir = new File(dataDir);
        this.token = token == null ? "" : token;
        this.transferKey = transferKey == null ? "" : transferKey;
        if (!this.dataDir.exists()) this.dataDir.mkdirs();
    }

    private boolean authed(IHTTPSession session) {
        if (token.isEmpty()) return true;
        String h = session.getHeaders().get("authorization");
        if (h == null) return false;
        return h.equals("Bearer " + token);
    }

    private Response json(Response.Status s, String body) {
        return newFixedLengthResponse(s, "application/json", body);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> parms = session.getParms();

        if (!authed(session)) {
            return json(Response.Status.UNAUTHORIZED, "{\"err\":\"unauthorized\"}");
        }

        try {
            if ("/ping".equals(uri)) return json(Response.Status.OK, "{\"ok\":true}");
            if ("/chat/hello".equals(uri)) return hello(parms);
            if ("/chat/ping".equals(uri)) return ping(parms);
            if ("/chat/msgs".equals(uri)) return msgs(parms);
            if ("/chat/send".equals(uri)) return send(parms);
            if ("/chat/upload".equals(uri)) return upload(session, parms);
            if ("/chat/download".equals(uri)) return download(parms);
            if ("/chat/avatar/upload".equals(uri)) return avatarUpload(session, parms);
            if ("/chat/avatar/get".equals(uri)) return avatarGet(parms);
            if ("/upload".equals(uri)) return fileUpload(session, parms);
            return json(Response.Status.NOT_FOUND, "{\"err\":\"no route\"}");
        } catch (Exception e) {
            return json(Response.Status.INTERNAL_ERROR,
                "{\"err\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private Response hello(Map<String, String> p) {
        String user = p.get("user");
        String hash = p.get("hash");
        if (user == null || user.isEmpty()) user = "user";
        if (hash == null) return json(Response.Status.OK, "{\"ok\":false}");
        return json(Response.Status.OK,
            "{\"ok\":true,\"user\":\"" + esc(user) + "\"}");
    }

    private Response ping(Map<String, String> p) {
        String user = p.get("user");
        if (user == null || user.isEmpty()) user = "user";
        if (kicked.contains(user)) {
            return json(Response.Status.OK, "{\"kicked\":true}");
        }
        online.put(user, System.currentTimeMillis());
        cleanOnline();
        return json(Response.Status.OK, "{\"kicked\":false}");
    }

    private void cleanOnline() {
        long now = System.currentTimeMillis();
        List<String> dead = new ArrayList<>();
        for (Map.Entry<String, Long> e : online.entrySet()) {
            if (now - e.getValue() > ONLINE_TIMEOUT) dead.add(e.getKey());
        }
        for (String d : dead) online.remove(d);
    }

    private Response msgs(Map<String, String> p) {
        long since = 0;
        try {
            String s = p.get("since");
            if (s != null) since = Long.parseLong(s);
        } catch (Exception ignored) {}

        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map<String, Object> m : msgs) {
            long id = (Long) m.get("id");
            if (id <= since) continue;
            if (!first) sb.append(",");
            first = false;
            sb.append("{");
            sb.append("\"id\":").append(id).append(",");
            sb.append("\"type\":\"").append(esc((String) m.get("type"))).append("\",");
            sb.append("\"user\":\"").append(esc((String) m.get("user"))).append("\",");
            sb.append("\"time\":").append(m.get("time")).append(",");
            sb.append("\"content\":\"").append(esc((String) m.get("content"))).append("\",");
            sb.append("\"size\":").append(m.get("size")).append(",");
            sb.append("\"file\":\"").append(esc((String) m.get("file"))).append("\"");
            sb.append("}");
        }
        sb.append("]");
        return json(Response.Status.OK, sb.toString());
    }

    private Response send(Map<String, String> p) {
        String user = p.get("user");
        String type = p.get("type");
        String content = p.get("content");
        String file = p.get("file");
        String size = p.get("size");
        if (user == null) user = "user";
        if (type == null) type = "text";
        if (content == null) content = "";

        Map<String, Object> m = new HashMap<>();
        m.put("id", nextId++);
        m.put("type", type);
        m.put("user", user);
        m.put("time", System.currentTimeMillis());
        m.put("content", content);
        m.put("file", file == null ? "" : file);
        try { m.put("size", size == null ? 0L : Long.parseLong(size)); }
        catch (Exception e) { m.put("size", 0L); }
        msgs.add(m);

        if (msgs.size() > 5000) {
            msgs.subList(0, msgs.size() - 5000).clear();
        }

        return json(Response.Status.OK, "{\"ok\":true,\"id\":" + m.get("id") + "}");
    }

    private Response upload(IHTTPSession session, Map<String, String> p) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String tmpPath = files.get("file");
            String name = p.get("name");
            if (name == null) name = "upload_" + System.currentTimeMillis();
            name = name.replace("/", "_").replace("..", "_");

            if (tmpPath == null) {
                return json(Response.Status.BAD_REQUEST, "{\"err\":\"no file\"}");
            }
            File tmp = new File(tmpPath);
            File dst = new File(new File(dataDir, "chat_files"), name);
            dst.getParentFile().mkdirs();
            copy(tmp, dst);
            return json(Response.Status.OK, "{\"ok\":true}");
        } catch (Exception e) {
            return json(Response.Status.INTERNAL_ERROR,
                "{\"err\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private Response download(Map<String, String> p) {
        String name = p.get("name");
        if (name == null) return json(Response.Status.BAD_REQUEST, "{\"err\":\"no name\"}");
        name = name.replace("/", "_").replace("..", "_");
        File f = new File(new File(dataDir, "chat_files"), name);
        if (!f.exists()) return json(Response.Status.NOT_FOUND, "{\"err\":\"no file\"}");
        try {
            FileInputStream fis = new FileInputStream(f);
            return newFixedLengthResponse(Response.Status.OK,
                "application/octet-stream", fis, f.length());
        } catch (Exception e) {
            return json(Response.Status.INTERNAL_ERROR,
                "{\"err\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private Response avatarUpload(IHTTPSession session, Map<String, String> p) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String tmpPath = files.get("file");
            String user = p.get("user");
            if (user == null || user.isEmpty()) user = "user";
            user = user.replace("/", "_").replace("..", "_");
            if (tmpPath == null) {
                return json(Response.Status.BAD_REQUEST, "{\"err\":\"no file\"}");
            }
            File tmp = new File(tmpPath);
            File dst = new File(new File(dataDir, "chat_avatars"), user + ".png");
            dst.getParentFile().mkdirs();
            copy(tmp, dst);
            return json(Response.Status.OK, "{\"ok\":true}");
        } catch (Exception e) {
            return json(Response.Status.INTERNAL_ERROR,
                "{\"err\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private Response avatarGet(Map<String, String> p) {
        String user = p.get("user");
        if (user == null) return json(Response.Status.BAD_REQUEST, "{\"err\":\"no user\"}");
        user = user.replace("/", "_").replace("..", "_");
        File f = new File(new File(dataDir, "chat_avatars"), user + ".png");
        if (!f.exists()) return json(Response.Status.NOT_FOUND, "{\"err\":\"no avatar\"}");
        try {
            FileInputStream fis = new FileInputStream(f);
            return newFixedLengthResponse(Response.Status.OK,
                "image/png", fis, f.length());
        } catch (Exception e) {
            return json(Response.Status.INTERNAL_ERROR,
                "{\"err\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private Response fileUpload(IHTTPSession session, Map<String, String> p) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String tmpPath = files.get("postData");
            String hash = p.get("hash");
            if (tmpPath == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                    "text/plain", "empty");
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

            byte[] dec = CryptoUtils.aesDecrypt(enc, transferKey);
            String realHash = CryptoUtils.sha256(dec);
            if (hash != null && !hash.equals(realHash)) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                    "text/plain", "hash mismatch");
            }

            File outDir = new File(dataDir, "received");
            if (!outDir.exists()) outDir.mkdirs();
            File out = new File(outDir, "recv_" + System.currentTimeMillis() + ".bin");
            FileOutputStream fos = new FileOutputStream(out);
            fos.write(dec);
            fos.close();

            return newFixedLengthResponse(Response.Status.OK,
                "text/plain", "ok: " + out.getName());
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                "text/plain", "err: " + e.getMessage());
        }
    }

    private void copy(File src, File dst) throws Exception {
        InputStream is = new FileInputStream(src);
        FileOutputStream os = new FileOutputStream(dst);
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
        is.close();
        os.close();
    }

    public static void main(String[] args) {
        int port = 8080;
        String dir = "./data";
        String token = "";
        String transferKey = "CHANGE_ME_BEFORE_RELEASE_2024";

        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) port = Integer.parseInt(args[++i]);
            else if ("--dir".equals(args[i]) && i + 1 < args.length) dir = args[++i];
            else if ("--token".equals(args[i]) && i + 1 < args.length) token = args[++i];
            else if ("--key".equals(args[i]) && i + 1 < args.length) transferKey = args[++i];
        }

        try {
            ChatServerRemote server = new ChatServerRemote(port, dir, token, transferKey);
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("MyEmpty remote server started on port " + port);
            System.out.println("data dir: " + new File(dir).getAbsolutePath());
            System.out.println("token: " + (token.isEmpty() ? "(none)" : token));
        } catch (Exception e) {
            System.err.println("Failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}