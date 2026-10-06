package com.example.myempty.activity2;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class ChatServer extends NanoHTTPD {

    public interface OnMessage {
        void onNewMessage(ChatMsg m);
        void onUserJoin(String user);
        void onUserLeave(String user);
        void onAuthFail(String user);
    }

    private final Context ctx;
    private OnMessage listener;

    private static final String CHAT_DIR_NAME = "chat_files";
    private static final String AVATAR_DIR_NAME = "chat_avatars";

    private final Map<String, Long> onlineMap = new HashMap<>();
    private final Map<String, Boolean> muteMap = new HashMap<>();
    private final List<String> kickList = new ArrayList<>();

    private static final long ONLINE_TIMEOUT = 5000L;

    private String passHash = "";
    private byte[] sessionKey = null;

    public ChatServer(Context ctx) {
        super(8080);
        this.ctx = ctx;
    }

    public void setListener(OnMessage l) {
        listener = l;
    }

    public void setPassphrase(String pass) throws Exception {
        this.passHash = ChatCrypto.passHash(pass);
        this.sessionKey = ChatCrypto.sessionKeyFromPass(pass);
    }

    public String getPassHash() {
        return passHash;
    }

    public byte[] getSessionKey() {
        return sessionKey;
    }

    public boolean isMuted(String user) {
        return Boolean.TRUE.equals(muteMap.get(user));
    }

    public void setMuted(String user, boolean muted) {
        if (muted) muteMap.put(user, true);
        else muteMap.remove(user);
        pushSystemMsg("【系统】" + user + (muted ? " 被禁言" : " 解除禁言"));
    }

    public void kick(String user) {
        kickList.add(user);
        onlineMap.remove(user);
        pushSystemMsg("【系统】" + user + " 被请出房间");
    }

    public boolean isKicked(String user) {
        return kickList.contains(user);
    }

    public List<String> getOnlineUsers() {
        long now = System.currentTimeMillis();
        List<String> list = new ArrayList<>();
        List<String> dead = new ArrayList<>();
        for (Map.Entry<String, Long> e : onlineMap.entrySet()) {
            if (now - e.getValue() > ONLINE_TIMEOUT) {
                dead.add(e.getKey());
            } else {
                list.add(e.getKey());
            }
        }
        for (String d : dead) onlineMap.remove(d);
        return list;
    }

    private void pushSystemMsg(String text) {
        String enc = null;
        if (sessionKey != null) {
            enc = ChatCrypto.encrypt(text, sessionKey);
        }
        ChatMsg m = new ChatMsg(ChatMsg.TYPE_SYSTEM, "system",
            System.currentTimeMillis(), enc == null ? text : enc);
        m.self = true;
        ChatDb.get(ctx).insert(m);
        if (listener != null) listener.onNewMessage(m);
    }

    private File fileDir() {
        File d = new File(ctx.getExternalFilesDir(null), CHAT_DIR_NAME);
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public File getFileDir() {
        return fileDir();
    }

    private File avatarDir() {
        File d = new File(ctx.getExternalFilesDir(null), AVATAR_DIR_NAME);
        if (!d.exists()) d.mkdirs();
        return d;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> parms = session.getParms();

        try {
            if ("/chat/hello".equals(uri)) return hello(parms);
            if ("/chat/ping".equals(uri)) return ping(session, parms);
            if ("/chat/msgs".equals(uri)) return msgs(parms);
            if ("/chat/send".equals(uri)) return send(session, parms);
            if ("/chat/upload".equals(uri)) return upload(session, parms);
            if ("/chat/download".equals(uri)) return download(parms);
            if ("/chat/users".equals(uri)) return users();
            if ("/chat/kick".equals(uri)) return kickApi(parms);
            if ("/chat/mute".equals(uri)) return muteApi(parms);
            if ("/chat/avatar/upload".equals(uri)) return avatarUpload(session, parms);
            if ("/chat/avatar/get".equals(uri)) return avatarGet(parms);
            return json(Response.Status.NOT_FOUND, "{\"err\":\"no route\"}");
        } catch (Exception e) {
            return json(Response.Status.INTERNAL_ERROR,
                "{\"err\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private Response hello(Map<String, String> parms) {
        String user = parms.get("user");
        String hash = parms.get("hash");
        if (user == null || user.isEmpty()) user = "匿名";

        if (hash == null || !hash.equals(passHash)) {
            if (listener != null) listener.onAuthFail(user);
            pushSystemMsg("【安全】" + user + " 想进房，口令错了");
            return json(Response.Status.OK,
                "{\"ok\":false,\"err\":\"badpass\"}");
        }

        return json(Response.Status.OK,
            "{\"ok\":true,\"user\":\"" + esc(user) + "\"}");
    }

    private Response ping(IHTTPSession session, Map<String, String> parms) {
        String user = parms.get("user");
        if (user == null || user.isEmpty()) user = "匿名";
        if (isKicked(user)) {
            return json(Response.Status.OK, "{\"kicked\":true}");
        }
        onlineMap.put(user, System.currentTimeMillis());
        String muteFlag = isMuted(user) ? "true" : "false";
        return json(Response.Status.OK,
            "{\"kicked\":false,\"muted\":" + muteFlag + "}");
    }

    private Response msgs(Map<String, String> parms) {
        long since = 0;
        try {
            String s = parms.get("since");
            if (s != null) since = Long.parseLong(s);
        } catch (Exception ignored) {}

        List<ChatMsg> list = ChatDb.get(ctx).getSince(since);
        JSONArray arr = new JSONArray();
        for (ChatMsg m : list) {
            arr.put(toJson(m));
        }
        return json(Response.Status.OK, arr.toString());
    }

    private Response send(IHTTPSession session, Map<String, String> parms)
        throws Exception {
        String user = parms.get("user");
        String type = parms.get("type");
        String content = parms.get("content");
        if (user == null || user.isEmpty()) user = "匿名";
        if (type == null) type = ChatMsg.TYPE_TEXT;
        if (content == null) content = "";

        if (isKicked(user)) {
            return json(Response.Status.OK, "{\"ok\":false,\"kicked\":true}");
        }
        if (isMuted(user) && !ChatMsg.TYPE_SYSTEM.equals(type)) {
            return json(Response.Status.OK, "{\"ok\":false,\"muted\":true}");
        }

        String plain = null;
        if (sessionKey != null) {
            plain = ChatCrypto.decrypt(content, sessionKey);
            if (plain == null) {
                return json(Response.Status.OK, "{\"ok\":false,\"badcrypto\":true}");
            }
        } else {
            plain = content;
        }

        ChatMsg m = new ChatMsg(type, user, System.currentTimeMillis(), plain);
        long size = 0;
        try {
            String sz = parms.get("size");
            if (sz != null) size = Long.parseLong(sz);
        } catch (Exception ignored) {}
        m.size = size;

        if (ChatMsg.TYPE_IMAGE.equals(type) || ChatMsg.TYPE_FILE.equals(type)) {
            String encFile = parms.get("file");
            if (sessionKey != null && encFile != null) {
                String decFile = ChatCrypto.decrypt(encFile, sessionKey);
                m.filePath = decFile == null ? encFile : decFile;
            } else {
                m.filePath = encFile;
            }
        }

        String stored = content;
        if (sessionKey == null) stored = plain;
        m.content = stored;

        ChatDb.get(ctx).insert(m);
        if (listener != null) listener.onNewMessage(m);

        return json(Response.Status.OK, "{\"ok\":true,\"id\":" + m.id + "}");
    }

    private Response upload(IHTTPSession session, Map<String, String> parms)
        throws Exception {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String tmpPath = files.get("file");
        String name = parms.get("name");
        if (name == null) name = "upload_" + System.currentTimeMillis();
        name = name.replace("/", "_").replace("..", "_");

        if (tmpPath == null) {
            return json(Response.Status.BAD_REQUEST, "{\"err\":\"no file\"}");
        }

        File tmp = new File(tmpPath);
        byte[] raw = new byte[(int) tmp.length()];
        FileInputStream fis = new FileInputStream(tmp);
        int read = 0;
        while (read < raw.length) {
            int n = fis.read(raw, read, raw.length - read);
            if (n < 0) break;
            read += n;
        }
        fis.close();

        byte[] toStore;
        if (sessionKey != null) {
            byte[] dec = ChatCrypto.decryptBytes(raw, sessionKey);
            if (dec == null) {
                return json(Response.Status.OK, "{\"ok\":false,\"badcrypto\":true}");
            }
            toStore = dec;
        } else {
            toStore = raw;
        }

        File dst = new File(fileDir(), name);
        FileOutputStream fos = new FileOutputStream(dst);
        fos.write(toStore);
        fos.close();

        return json(Response.Status.OK,
            "{\"ok\":true,\"file\":\"" + esc(name) + "\"}");
    }

    private Response download(Map<String, String> parms) {
        String name = parms.get("name");
        if (name == null) {
            return json(Response.Status.BAD_REQUEST, "{\"err\":\"no name\"}");
        }
        name = name.replace("/", "_").replace("..", "_");

        File f = new File(fileDir(), name);
        if (!f.exists()) {
            return json(Response.Status.NOT_FOUND, "{\"err\":\"no file\"}");
        }

        try {
            if (sessionKey == null) {
                FileInputStream fis = new FileInputStream(f);
                return newFixedLengthResponse(Response.Status.OK,
                    "application/octet-stream", fis, f.length());
            }

            byte[] raw = new byte[(int) f.length()];
            FileInputStream fis = new FileInputStream(f);
            int read = 0;
            while (read < raw.length) {
                int n = fis.read(raw, read, raw.length - read);
                if (n < 0) break;
                read += n;
            }
            fis.close();

            byte[] enc = ChatCrypto.encryptBytes(raw, sessionKey);
            if (enc == null) {
                return json(Response.Status.INTERNAL_ERROR,
                    "{\"err\":\"enc fail\"}");
            }

            return newFixedLengthResponse(Response.Status.OK,
                "application/octet-stream",
                new java.io.ByteArrayInputStream(enc), enc.length);
        } catch (Exception e) {
            return json(Response.Status.INTERNAL_ERROR,
                "{\"err\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private Response avatarUpload(IHTTPSession session, Map<String, String> parms)
        throws Exception {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String tmpPath = files.get("file");
        String user = parms.get("user");
        if (user == null || user.isEmpty()) user = "匿名";
        user = user.replace("/", "_").replace("..", "_");

        if (tmpPath == null) {
            return json(Response.Status.BAD_REQUEST, "{\"err\":\"no file\"}");
        }

        File tmp = new File(tmpPath);
        byte[] raw = new byte[(int) tmp.length()];
        FileInputStream fis = new FileInputStream(tmp);
        int read = 0;
        while (read < raw.length) {
            int n = fis.read(raw, read, raw.length - read);
            if (n < 0) break;
            read += n;
        }
        fis.close();

        byte[] toStore;
        if (sessionKey != null) {
            byte[] dec = ChatCrypto.decryptBytes(raw, sessionKey);
            if (dec == null) {
                return json(Response.Status.OK, "{\"ok\":false,\"badcrypto\":true}");
            }
            toStore = dec;
        } else {
            toStore = raw;
        }

        File dst = new File(avatarDir(), user + ".png");
        FileOutputStream fos = new FileOutputStream(dst);
        fos.write(toStore);
        fos.close();

        return json(Response.Status.OK, "{\"ok\":true}");
    }

    private Response avatarGet(Map<String, String> parms) {
        String user = parms.get("user");
        if (user == null || user.isEmpty()) {
            return json(Response.Status.BAD_REQUEST, "{\"err\":\"no user\"}");
        }
        user = user.replace("/", "_").replace("..", "_");

        File f = new File(avatarDir(), user + ".png");
        if (!f.exists()) {
            return json(Response.Status.NOT_FOUND, "{\"err\":\"no avatar\"}");
        }

        try {
            if (sessionKey == null) {
                FileInputStream fis = new FileInputStream(f);
                return newFixedLengthResponse(Response.Status.OK,
                    "image/png", fis, f.length());
            }

            byte[] raw = new byte[(int) f.length()];
            FileInputStream fis = new FileInputStream(f);
            int read = 0;
            while (read < raw.length) {
                int n = fis.read(raw, read, raw.length - read);
                if (n < 0) break;
                read += n;
            }
            fis.close();

            byte[] enc = ChatCrypto.encryptBytes(raw, sessionKey);
            if (enc == null) {
                return json(Response.Status.INTERNAL_ERROR,
                    "{\"err\":\"enc fail\"}");
            }

            return newFixedLengthResponse(Response.Status.OK,
                "image/png",
                new java.io.ByteArrayInputStream(enc), enc.length);
        } catch (Exception e) {
            return json(Response.Status.INTERNAL_ERROR,
                "{\"err\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private Response users() {
        List<String> online = getOnlineUsers();
        JSONArray arr = new JSONArray();
        for (String u : online) {
            JSONObject o = new JSONObject();
            try {
                o.put("user", u);
                o.put("muted", isMuted(u));
            } catch (Exception ignored) {}
            arr.put(o);
        }
        return json(Response.Status.OK, arr.toString());
    }

    private Response kickApi(Map<String, String> parms) {
        String user = parms.get("user");
        if (user != null && !user.isEmpty()) kick(user);
        return json(Response.Status.OK, "{\"ok\":true}");
    }

    private Response muteApi(Map<String, String> parms) {
        String user = parms.get("user");
        String muted = parms.get("muted");
        if (user != null && !user.isEmpty()) {
            setMuted(user, "true".equals(muted));
        }
        return json(Response.Status.OK, "{\"ok\":true}");
    }

    private JSONObject toJson(ChatMsg m) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", m.id);
            o.put("type", m.type);
            o.put("user", m.user);
            o.put("time", m.time);
            o.put("content", m.content);
            o.put("size", m.size);
            o.put("file", m.filePath == null ? "" : m.filePath);
        } catch (Exception ignored) {}
        return o;
    }

    private Response json(Response.Status status, String text) {
        return newFixedLengthResponse(status, "application/json", text);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}