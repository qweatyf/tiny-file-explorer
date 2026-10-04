package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatActivity extends Activity {

    private static final String SP_NAME = "chat_sp";
    private static final String KEY_LAST_IP = "last_ip";

    private ListView lvMsgs;
    private EditText etInput;
    private Button btnSend;
    private Button btnPlus;
    private TextView tvTitle;
    private TextView tvStatus;
    private TextView tvPass;
    private LinearLayout topBar;

    private ChatServer server;
    private boolean isHost = false;
    private String hostIp = "";
    private String myName = "我";
    private Bitmap myAvatar;

    private String myPass = "";
    private byte[] sessionKey = null;

    private final List<ChatMsg> msgs = new ArrayList<>();
    private MsgAdapter adapter;

    private long lastId = 0;
    private boolean polling = false;
    private volatile boolean kicked = false;
    private volatile boolean askingHostLost = false;

    private ChatDiscovery discovery;
    private ChatDiscovery clientDiscovery;

    private int failCount = 0;

    private final Handler ui = new Handler(Looper.getMainLooper());

    private final SimpleDateFormat sdf =
        new SimpleDateFormat("HH:mm", Locale.getDefault());

    private final Map<String, Bitmap> imageCache = new ConcurrentHashMap<>();
    private final Map<String, Bitmap> avatarCache = new ConcurrentHashMap<>();

    private File cacheDir() {
        File d = new File(getExternalFilesDir(null), "chat_cache");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private File avatarCacheDir() {
        File d = new File(getExternalFilesDir(null), "chat_avatar_cache");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private File downloadDir() {
        File d = new File("/storage/emulated/0/Download/本地聊天");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_chat);

        lvMsgs = findViewById(R.id.lv_chat_msgs);
        etInput = findViewById(R.id.et_chat_input);
        btnSend = findViewById(R.id.btn_chat_send);
        btnPlus = findViewById(R.id.btn_chat_plus);
        tvTitle = findViewById(R.id.tv_chat_title);
        tvStatus = findViewById(R.id.tv_chat_status);
        tvPass = findViewById(R.id.tv_chat_pass);
        topBar = findViewById(R.id.chat_top_bar);

        String uname = readUsername();
        if (uname != null && !uname.isEmpty()) myName = uname;

        Bitmap av = AvatarUtils.load(this);
        if (av != null) {
            myAvatar = AvatarUtils.makeCircle(av, dp(40));
        } else {
            myAvatar = AvatarUtils.makeDefault(myName, dp(40));
        }

        adapter = new MsgAdapter();
        lvMsgs.setAdapter(adapter);

        loadHistory();

        btnSend.setOnClickListener(v -> sendText());
        btnPlus.setOnClickListener(v -> showPlusMenu());
        topBar.setOnClickListener(v -> showUsersDialog());

        startDiscovery();
    }

    private void startDiscovery() {
        tvTitle.setText("正在找房间...");
        tvStatus.setText("扫描局域网");

        discovery = new ChatDiscovery();
        discovery.startAsClient(this, (ip, user) -> ui.post(() -> {
            if (isFinishing()) return;
            if (discovery != null) {
                discovery.stop();
                discovery = null;
            }

            new AlertDialog.Builder(this)
                .setTitle("发现房间")
                .setMessage(user + " 的房间\nIP: " + ip + "\n\n要进吗？")
                .setPositiveButton("进", (d, w) -> showJoinDialog(ip))
                .setNegativeButton("不要，我自己开", (d, w) -> askRole())
                .setCancelable(false)
                .show();
        }));

        ui.postDelayed(() -> {
            if (discovery != null && discovery.isRunning()) {
                discovery.stop();
                discovery = null;
                askRole();
            }
        }, 4000);
    }

    private void askRole() {
        String[] roles = {"当房主（开服务器）", "当客人（手动输 IP）"};
        new AlertDialog.Builder(this)
            .setTitle("选个身份")
            .setItems(roles, (d, w) -> {
                if (w == 0) askPassThenHost();
                else {
                    SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
                    showJoinDialog(sp.getString(KEY_LAST_IP, ""));
                }
            })
            .setCancelable(false)
            .show();
    }

    private void askPassThenHost() {
        EditText et = new EditText(this);
        et.setHint("至少 6 位，越乱越好");

        new AlertDialog.Builder(this)
            .setTitle("设个房间口令")
            .setMessage("告诉要进房的人，让他们输这个")
            .setView(et)
            .setCancelable(false)
            .setPositiveButton("开房", (d, w) -> {
                String pass = et.getText().toString().trim();
                if (pass.length() < 6) {
                    Toast.makeText(this, "至少 6 位", Toast.LENGTH_SHORT).show();
                    askPassThenHost();
                    return;
                }
                startAsHost(pass);
            })
            .setNegativeButton("算了", (d, w) -> finish())
            .show();
    }

    private void startAsHost(String pass) {
        isHost = true;
        hostIp = "";
        failCount = 0;
        myPass = pass;

        try {
            sessionKey = ChatCrypto.sessionKeyFromPass(pass);
        } catch (Exception e) {
            Toast.makeText(this, "密钥生成失败：" + e.getMessage(),
                Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvTitle.setText("房主模式");
        tvStatus.setText("本机 IP：" + getWifiIp() + "  在线：0");
        tvPass.setVisibility(View.VISIBLE);
        tvPass.setText("口令：" + pass);

        try {
            server = new ChatServer(this);
            server.setPassphrase(pass);
            server.setListener(new ChatServer.OnMessage() {
                @Override
                public void onNewMessage(ChatMsg m) {
                    ui.post(() -> addLocalMsg(m));
                }

                @Override
                public void onUserJoin(String user) {
                    ui.post(() -> refreshStatus());
                }

                @Override
                public void onUserLeave(String user) {
                    ui.post(() -> refreshStatus());
                }

                @Override
                public void onAuthFail(String user) {
                    ui.post(() -> refreshStatus());
                }
            });
            server.start();

            Intent svc = new Intent(this, ChatService.class);
            svc.putExtra(ChatService.EXTRA_MODE, ChatService.MODE_HOST);
            svc.putExtra(ChatService.EXTRA_USER, myName);
            svc.putExtra(ChatService.EXTRA_IP, getWifiIp());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }

            pushSystemLocal("【系统】房间已开，IP: " + getWifiIp() + "，口令: " + pass);
        } catch (Exception e) {
            Toast.makeText(this, "服务器开不了：" + e.getMessage(),
                Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void showJoinDialog(String defIp) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(8), pad, 0);

        EditText etIp = new EditText(this);
        etIp.setHint("192.168.1.100");
        etIp.setText(defIp);
        box.addView(etIp);

        EditText etPass = new EditText(this);
        etPass.setHint("房间口令");
        box.addView(etPass);

        new AlertDialog.Builder(this)
            .setTitle("进房间")
            .setView(box)
            .setCancelable(false)
            .setPositiveButton("进", (d, w) -> {
                String ip = etIp.getText().toString().trim();
                String pass = etPass.getText().toString().trim();
                if (ip.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(this, "IP 和口令都填一下", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                doJoin(ip, pass);
            })
            .setNegativeButton("算了", (d, w) -> finish())
            .show();
    }

    private void doJoin(String ip, String pass) {
        tvTitle.setText("连接中...");
        tvStatus.setText(ip);

        new Thread(() -> {
            try {
                String hash = ChatCrypto.passHash(pass);
                String url = "http://" + ip + ":8080/chat/hello"
                    + "?user=" + URLEncoder.encode(myName, "UTF-8")
                    + "&hash=" + hash;

                String r = httpGet(url);
                if (r == null) {
                    ui.post(() -> {
                        Toast.makeText(this, "连不上房主", Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }

                if (r.contains("\"ok\":false")) {
                    ui.post(() -> {
                        new AlertDialog.Builder(this)
                            .setTitle("进不去")
                            .setMessage("口令不对，或者被房主拒了")
                            .setCancelable(false)
                            .setPositiveButton("知道了", (d, w) -> finish())
                            .show();
                    });
                    return;
                }

                ui.post(() -> startAsClient(ip, pass));
            } catch (Exception e) {
                ui.post(() -> {
                    Toast.makeText(this, "出错：" + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void startAsClient(String ip, String pass) {
        isHost = false;
        hostIp = ip;
        myPass = pass;
        failCount = 0;
        kicked = false;

        try {
            sessionKey = ChatCrypto.sessionKeyFromPass(pass);
        } catch (Exception e) {
            Toast.makeText(this, "密钥生成失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle.setText("连 " + ip);
        tvStatus.setText("已连上");
        tvPass.setVisibility(View.GONE);

        SharedPreferences.Editor e = getSharedPreferences(SP_NAME, MODE_PRIVATE).edit();
        e.putString(KEY_LAST_IP, ip);
        e.apply();

        new Thread(this::uploadMyAvatar).start();

        polling = true;
        new Thread(this::pollLoop).start();

        if (clientDiscovery == null) {
            clientDiscovery = new ChatDiscovery();
            clientDiscovery.startAsClient(this, (newIp, user) -> ui.post(() -> {
                if (newIp.equals(hostIp)) return;
                if (isFinishing()) return;
                if (kicked) return;

                new AlertDialog.Builder(this)
                    .setTitle("房主换了")
                    .setMessage(user + " 接手了房间\n新 IP: " + newIp
                        + "\n\n要切过去吗？")
                    .setPositiveButton("切", (d, w) -> {
                        stopPolling();
                        startAsClient(newIp, myPass);
                    })
                    .setNegativeButton("不切", null)
                    .setCancelable(false)
                    .show();
            }));
        }
    }

    private void stopPolling() {
        polling = false;
        try { Thread.sleep(200); } catch (Exception ignored) {}
    }

    private void uploadMyAvatar() {
        try {
            File av = AvatarUtils.avatarFile(this);
            if (!av.exists()) return;
            if (sessionKey == null) return;

            byte[] bytes = new byte[(int) av.length()];
            java.io.FileInputStream fis = new java.io.FileInputStream(av);
            int read = 0;
            while (read < bytes.length) {
                int n = fis.read(bytes, read, bytes.length - read);
                if (n < 0) break;
                read += n;
            }
            fis.close();

            byte[] enc = ChatCrypto.encryptBytes(bytes, sessionKey);
            if (enc == null) return;

            String boundary = "----AvatarBoundary" + System.currentTimeMillis();
            String encodedUser = URLEncoder.encode(myName, "UTF-8");
            URL url = new URL("http://" + hostIp + ":8080/chat/avatar/upload?user="
                + encodedUser);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);

            OutputStream os = conn.getOutputStream();
            os.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"avatar.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n").getBytes("UTF-8"));
            os.write(enc);
            os.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
            os.close();

            conn.getResponseCode();
        } catch (Exception ignored) {}
    }

    private String readUsername() {
        try {
            File f = new File(getFilesDir(), "account.txt");
            if (!f.exists()) return null;
            InputStream is = new java.io.FileInputStream(f);
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            String s = new String(buf);
            for (String line : s.split("\n")) {
                if (line.startsWith("username=")) return line.substring(9).trim();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getWifiIp() {
        try {
            android.net.wifi.WifiManager wm =
                (android.net.wifi.WifiManager) getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            int ip = wm.getConnectionInfo().getIpAddress();
            return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF)
                + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
        } catch (Exception e) {
            return "拿不到";
        }
    }

    private void loadHistory() {
        new Thread(() -> {
            List<ChatMsg> all = ChatDb.get(this).getAll();
            ui.post(() -> {
                msgs.clear();
                msgs.addAll(all);
                if (!msgs.isEmpty()) lastId = msgs.get(msgs.size() - 1).id;
                adapter.notifyDataSetChanged();
                scrollBottom();
            });
        }).start();
    }

    private void addLocalMsg(ChatMsg m) {
        if (m.id > lastId) lastId = m.id;
        m.self = true;
        msgs.add(m);
        adapter.notifyDataSetChanged();
        scrollBottom();
    }

    private void pushSystemLocal(String text) {
        String enc = null;
        if (sessionKey != null) enc = ChatCrypto.encrypt(text, sessionKey);
        ChatMsg m = new ChatMsg(ChatMsg.TYPE_SYSTEM, "system",
            System.currentTimeMillis(), enc == null ? text : enc);
        m.self = true;
        ChatDb.get(this).insert(m);
        addLocalMsg(m);
    }

    private void sendText() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;

        if (isHost) {
            if (server != null && server.isMuted(myName)) {
                Toast.makeText(this, "你被禁言了", Toast.LENGTH_SHORT).show();
                return;
            }
            etInput.setText("");
            String enc = ChatCrypto.encrypt(text, sessionKey);
            ChatMsg m = new ChatMsg(ChatMsg.TYPE_TEXT, myName,
                System.currentTimeMillis(), enc);
            ChatDb.get(this).insert(m);
            addLocalMsg(m);
        } else {
            etInput.setText("");
            new Thread(() -> {
                try {
                    String enc = ChatCrypto.encrypt(text, sessionKey);
                    if (enc == null) return;
                    String url = "http://" + hostIp + ":8080/chat/send"
                        + "?user=" + URLEncoder.encode(myName, "UTF-8")
                        + "&type=text"
                        + "&content=" + URLEncoder.encode(enc, "UTF-8");
                    httpGet(url);
                } catch (Exception ignored) {}
            }).start();
        }
    }

    private void showPlusMenu() {
        String[] ops = {"发图片", "发文件"};
        new AlertDialog.Builder(this)
            .setItems(ops, (d, w) -> {
                if (w == 0) pickImage();
                else pickFile();
            })
            .show();
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        startActivityForResult(i, 401);
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, 400);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            if (req == 401) uploadMedia(uri, true);
            else if (req == 400) uploadMedia(uri, false);
        }
    }

    private void uploadMedia(Uri uri, boolean isImage) {
        new Thread(() -> {
            try {
                String name;
                if (isImage) {
                    String ext = guessExt(uri);
                    name = "img_" + System.currentTimeMillis() + ext;
                } else {
                    name = "file_" + System.currentTimeMillis() + ".bin";
                    String display = queryDisplayName(uri);
                    if (display != null && !display.isEmpty()) {
                        name = display.replace("/", "_");
                    }
                }

                InputStream is = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                is.close();
                byte[] bytes = bos.toByteArray();

                if (isHost) {
                    File dst = new File(cacheDir(), name);
                    FileOutputStream fos = new FileOutputStream(dst);
                    fos.write(bytes);
                    fos.close();

                    String encName = ChatCrypto.encrypt(name, sessionKey);
                    ChatMsg m = new ChatMsg(
                        isImage ? ChatMsg.TYPE_IMAGE : ChatMsg.TYPE_FILE,
                        myName, System.currentTimeMillis(),
                        encName == null ? name : encName);
                    m.size = bytes.length;
                    m.filePath = name;
                    ChatDb.get(this).insert(m);
                    ui.post(() -> addLocalMsg(m));
                    return;
                }

                byte[] encBytes = ChatCrypto.encryptBytes(bytes, sessionKey);
                if (encBytes == null) return;
                String encName = ChatCrypto.encrypt(name, sessionKey);
                if (encName == null) return;

                String boundary = "----ChatBoundary" + System.currentTimeMillis();
                URL url = new URL("http://" + hostIp + ":8080/chat/upload?name="
                    + URLEncoder.encode(name, "UTF-8"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + boundary);

                OutputStream os = conn.getOutputStream();
                os.write(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\""
                    + name + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n")
                    .getBytes("UTF-8"));
                os.write(encBytes);
                os.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
                os.close();

                int code = conn.getResponseCode();
                if (code == 200) {
                    String type = isImage ? ChatMsg.TYPE_IMAGE : ChatMsg.TYPE_FILE;
                    String sendUrl = "http://" + hostIp + ":8080/chat/send"
                        + "?user=" + URLEncoder.encode(myName, "UTF-8")
                        + "&type=" + type
                        + "&content=" + URLEncoder.encode(encName, "UTF-8")
                        + "&size=" + bytes.length
                        + "&file=" + URLEncoder.encode(encName, "UTF-8");
                    httpGet(sendUrl);
                }
            } catch (Exception e) {
                ui.post(() -> Toast.makeText(this,
                    "传失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String guessExt(Uri uri) {
        String mime = getContentResolver().getType(uri);
        if (mime == null) return ".png";
        if (mime.contains("png")) return ".png";
        if (mime.contains("jpeg") || mime.contains("jpg")) return ".jpg";
        if (mime.contains("gif")) return ".gif";
        if (mime.contains("webp")) return ".webp";
        return ".png";
    }

    private String queryDisplayName(Uri uri) {
        try {
            android.database.Cursor c = getContentResolver().query(uri,
                null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String s = c.getString(idx);
                    c.close();
                    return s;
                }
                c.close();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void pollLoop() {
        while (polling) {
            try {
                String pingUrl = "http://" + hostIp + ":8080/chat/ping"
                    + "?user=" + URLEncoder.encode(myName, "UTF-8");
                String pingR = httpGet(pingUrl);

                if (pingR == null) {
                    failCount++;
                    if (failCount >= 3 && !askingHostLost) {
                        ui.post(this::onHostLost);
                        return;
                    }
                } else {
                    failCount = 0;
                    if (pingR.contains("\"kicked\":true")) {
                        ui.post(this::onKicked);
                        return;
                    }

                    String msgsUrl = "http://" + hostIp + ":8080/chat/msgs"
                        + "?since=" + lastId;
                    String r = httpGet(msgsUrl);
                    if (r != null && r.startsWith("[")) {
                        org.json.JSONArray arr = new org.json.JSONArray(r);
                        for (int i = 0; i < arr.length(); i++) {
                            org.json.JSONObject o = arr.getJSONObject(i);
                            ChatMsg m = new ChatMsg();
                            m.id = o.getLong("id");
                            m.type = o.getString("type");
                            m.user = o.getString("user");
                            m.time = o.getLong("time");
                            m.content = o.optString("content", "");
                            m.size = o.optLong("size", 0);
                            m.filePath = o.optString("file", "");
                            m.self = myName.equals(m.user);

                            ChatDb.get(this).insert(m);

                            final ChatMsg fm = m;
                            ui.post(() -> {
                                if (fm.id > lastId) lastId = fm.id;
                                msgs.add(fm);
                                adapter.notifyDataSetChanged();
                                scrollBottom();
                            });
                        }
                    }
                }
            } catch (Exception ignored) {}

            try { Thread.sleep(2000); } catch (Exception ignored) {}
        }
    }

    private void onHostLost() {
        if (askingHostLost) return;
        if (kicked) return;
        askingHostLost = true;
        stopPolling();

        new AlertDialog.Builder(this)
            .setTitle("房主好像下线了")
            .setMessage("要接手当房主吗？\n接手后别人会自动找到你")
            .setPositiveButton("接手", (d, w) -> {
                askingHostLost = false;
                tvStatus.setText("切换中...");
                startAsHost(myPass);
            })
            .setNegativeButton("不接了", (d, w) -> {
                askingHostLost = false;
                Toast.makeText(this, "退回主界面", Toast.LENGTH_SHORT).show();
                finish();
            })
            .setCancelable(false)
            .show();
    }

    private void onKicked() {
        if (kicked) return;
        kicked = true;
        polling = false;
        new AlertDialog.Builder(this)
            .setTitle("你已经被移出房间")
            .setCancelable(false)
            .setPositiveButton("知道了", (d, w) -> finish())
            .show();
    }

    private void refreshStatus() {
        if (isHost && server != null) {
            int n = server.getOnlineUsers().size();
            tvStatus.setText("本机 IP：" + getWifiIp() + "  在线：" + n);
        }
    }

    private void showUsersDialog() {
        if (!isHost || server == null) return;
        List<String> online = server.getOnlineUsers();
        if (online.isEmpty()) {
            Toast.makeText(this, "现在没人连进来", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] arr = new String[online.size()];
        for (int i = 0; i < online.size(); i++) {
            String u = online.get(i);
            String flag = server.isMuted(u) ? " [已禁言]" : "";
            arr[i] = "● " + u + flag;
        }
        new AlertDialog.Builder(this)
            .setTitle("在线用户")
            .setItems(arr, (d, w) -> showUserActions(online.get(w)))
            .setNegativeButton("关闭", null)
            .show();
    }

    private void showUserActions(String user) {
        boolean muted = server.isMuted(user);
        String[] ops = {muted ? "解除禁言" : "禁言", "移出房间"};
        new AlertDialog.Builder(this)
            .setTitle(user)
            .setItems(ops, (d, w) -> {
                if (w == 0) server.setMuted(user, !muted);
                else if (w == 1) {
                    new AlertDialog.Builder(this)
                        .setTitle("移出房间")
                        .setMessage("确定把 " + user + " 请出去吗？")
                        .setPositiveButton("移出", (dd, ww) -> server.kick(user))
                        .setNegativeButton("算了", null)
                        .show();
                }
            })
            .show();
    }

    private String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            if (code != 200) return null;
            java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void scrollBottom() {
        lvMsgs.post(() -> lvMsgs.setSelection(msgs.size() - 1));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        polling = false;
        if (server != null) {
            server.stop();
            server = null;
        }
        if (discovery != null) {
            discovery.stop();
            discovery = null;
        }
        if (clientDiscovery != null) {
            clientDiscovery.stop();
            clientDiscovery = null;
        }
        stopService(new Intent(this, ChatService.class));
    }

    private ChatMsg decodeForShow(ChatMsg raw) {
        ChatMsg m = new ChatMsg();
        m.id = raw.id;
        m.user = raw.user;
        m.time = raw.time;
        m.size = raw.size;
        m.self = raw.self;
        m.filePath = raw.filePath;

        if (raw.isSystem()) {
            m.type = ChatMsg.TYPE_SYSTEM;
            if (sessionKey != null) {
                String dec = ChatCrypto.decrypt(raw.content, sessionKey);
                if (dec == null) {
                    m.content = "⚠ 疑似被修改";
                    m.tampered = true;
                } else {
                    m.content = dec;
                }
            } else {
                m.content = raw.content;
            }
        } else if (raw.isText()) {
            m.type = ChatMsg.TYPE_TEXT;
            if (sessionKey != null) {
                String dec = ChatCrypto.decrypt(raw.content, sessionKey);
                if (dec == null) {
                    m.content = "⚠ 疑似被修改";
                    m.tampered = true;
                } else {
                    m.content = dec;
                }
            } else {
                m.content = raw.content;
            }
        } else if (raw.isImage() || raw.isFile()) {
            m.type = raw.type;
            if (sessionKey != null) {
                String dec = ChatCrypto.decrypt(raw.content, sessionKey);
                if (dec == null) {
                    m.content = "⚠ 疑似被修改";
                    m.tampered = true;
                } else {
                    m.content = dec;
                }
            } else {
                m.content = raw.content;
            }
        } else {
            m.type = raw.type;
            m.content = raw.content;
        }
        return m;
    }

    private Bitmap loadImage(String name) {
        if (name == null || name.isEmpty()) return null;
        Bitmap cached = imageCache.get(name);
        if (cached != null) return cached;

        File local = new File(cacheDir(), name);
        if (local.exists()) {
            Bitmap b = BitmapFactory.decodeFile(local.getAbsolutePath());
            if (b != null) {
                imageCache.put(name, b);
                return b;
            }
        }

        if (isHost) {
            File f = new File(server.getFileDir(), name);
            if (f.exists()) {
                Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (b != null) {
                    imageCache.put(name, b);
                    return b;
                }
            }
            return null;
        }

        try {
            URL url = new URL("http://" + hostIp + ":8080/chat/download?name="
                + URLEncoder.encode(name, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(15000);
            InputStream is = conn.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            is.close();
            byte[] enc = bos.toByteArray();

            byte[] raw = ChatCrypto.decryptBytes(enc, sessionKey);
            if (raw == null) return null;

            FileOutputStream fos = new FileOutputStream(local);
            fos.write(raw);
            fos.close();

            Bitmap b = BitmapFactory.decodeFile(local.getAbsolutePath());
            if (b != null) imageCache.put(name, b);
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap loadAvatar(String user) {
        if (user == null || user.isEmpty()) return null;

        if (myName.equals(user) && myAvatar != null) return myAvatar;

        Bitmap cached = avatarCache.get(user);
        if (cached != null) return cached;

        if (isHost) {
            File f = new File(new File(getExternalFilesDir(null), "chat_avatars"),
                user + ".png");
            if (f.exists()) {
                Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (b != null) {
                    Bitmap c = AvatarUtils.makeCircle(b, dp(40));
                    avatarCache.put(user, c);
                    return c;
                }
            }
        } else {
            File local = new File(avatarCacheDir(), user + ".png");
            if (!local.exists()) {
                try {
                    URL url = new URL("http://" + hostIp
                        + ":8080/chat/avatar/get?user="
                        + URLEncoder.encode(user, "UTF-8"));
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(5000);
                    if (conn.getResponseCode() == 200) {
                        InputStream is = conn.getInputStream();
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                        is.close();

                        byte[] raw = ChatCrypto.decryptBytes(bos.toByteArray(), sessionKey);
                        if (raw != null) {
                            FileOutputStream fos = new FileOutputStream(local);
                            fos.write(raw);
                            fos.close();
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (local.exists()) {
                Bitmap b = BitmapFactory.decodeFile(local.getAbsolutePath());
                if (b != null) {
                    Bitmap c = AvatarUtils.makeCircle(b, dp(40));
                    avatarCache.put(user, c);
                    return c;
                }
            }
        }

        Bitmap d = AvatarUtils.makeDefault(user, dp(40));
        avatarCache.put(user, d);
        return d;
    }

    private void downloadFile(ChatMsg m) {
        final String showName = m.content;
        new Thread(() -> {
            try {
                File local = new File(cacheDir(), m.filePath);
                if (!local.exists()) {
                    if (isHost) {
                        File f = new File(server.getFileDir(), m.filePath);
                        if (!f.exists()) {
                            ui.post(() -> Toast.makeText(this, "房主那边也没这文件",
                                Toast.LENGTH_SHORT).show());
                            return;
                        }
                        InputStream is = new java.io.FileInputStream(f);
                        FileOutputStream fos = new FileOutputStream(local);
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
                        is.close();
                        fos.close();
                    } else {
                        URL url = new URL("http://" + hostIp
                            + ":8080/chat/download?name="
                            + URLEncoder.encode(m.filePath, "UTF-8"));
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(60000);
                        InputStream is = conn.getInputStream();
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                        is.close();

                        byte[] raw = ChatCrypto.decryptBytes(bos.toByteArray(), sessionKey);
                        if (raw == null) {
                            ui.post(() -> Toast.makeText(this,
                                "⚠ 文件可能被改，下载中止",
                                Toast.LENGTH_LONG).show());
                            return;
                        }

                        FileOutputStream fos = new FileOutputStream(local);
                        fos.write(raw);
                        fos.close();
                    }
                }

                File dst = new File(downloadDir(), showName);
                InputStream is = new java.io.FileInputStream(local);
                FileOutputStream fos = new FileOutputStream(dst);
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
                is.close();
                fos.close();

                if (m.isImage()) {
                    android.media.MediaScannerConnection.scanFile(this,
                        new String[]{dst.getAbsolutePath()}, null, null);
                }

                ui.post(() -> Toast.makeText(this,
                    "已存到：" + dst.getAbsolutePath(),
                    Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                ui.post(() -> Toast.makeText(this,
                    "下载失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String fmtSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) return String.format("%.1f MB", b / 1024.0 / 1024);
        return String.format("%.2f GB", b / 1024.0 / 1024 / 1024);
    }

    class MsgAdapter extends BaseAdapter {
        @Override
        public int getCount() { return msgs.size(); }

        @Override
        public ChatMsg getItem(int i) { return msgs.get(i); }

        @Override
        public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            ChatMsg raw = getItem(pos);
            ChatMsg m = decodeForShow(raw);

            View v = LayoutInflater.from(ChatActivity.this)
                .inflate(R.layout.item_chat_msg, parent, false);

            TextView tvUser = v.findViewById(R.id.tv_msg_user);
            TextView tvTime = v.findViewById(R.id.tv_msg_time);
            TextView tvContent = v.findViewById(R.id.tv_msg_content);
            ImageView ivImg = v.findViewById(R.id.iv_msg_image);
            ImageView ivAvatar = v.findViewById(R.id.iv_msg_avatar);
            LinearLayout boxFile = v.findViewById(R.id.box_msg_file);
            TextView tvFileName = v.findViewById(R.id.tv_msg_file_name);
            TextView tvFileSize = v.findViewById(R.id.tv_msg_file_size);

            tvUser.setText(m.isSystem() ? "" : m.user);
            tvTime.setText(sdf.format(new Date(m.time)));

            tvContent.setVisibility(View.GONE);
            ivImg.setVisibility(View.GONE);
            boxFile.setVisibility(View.GONE);
            ivAvatar.setVisibility(View.GONE);

            if (m.isSystem()) {
                tvUser.setVisibility(View.GONE);
                tvTime.setVisibility(View.GONE);
                tvContent.setVisibility(View.VISIBLE);
                tvContent.setText(m.content);
                if (m.tampered) {
                    tvContent.setTextColor(0xFFB71C1C);
                } else {
                    tvContent.setTextColor(0xFF888888);
                }
                tvContent.setGravity(android.view.Gravity.CENTER);
            } else {
                ivAvatar.setVisibility(View.VISIBLE);
                Bitmap avBmp = loadAvatar(m.user);
                if (avBmp != null) ivAvatar.setImageBitmap(avBmp);

                if (m.isText()) {
                    tvContent.setVisibility(View.VISIBLE);
                    tvContent.setText(m.content);
                    if (m.tampered) {
                        tvContent.setTextColor(0xFFB71C1C);
                    } else {
                        tvContent.setTextColor(0xFF333333);
                    }
                    tvContent.setGravity(android.view.Gravity.START);
                } else if (m.isImage()) {
                    if (m.tampered) {
                        tvContent.setVisibility(View.VISIBLE);
                        tvContent.setText("⚠ 图片疑似被修改");
                        tvContent.setTextColor(0xFFB71C1C);
                    } else {
                        ivImg.setVisibility(View.VISIBLE);
                        final String fname = m.content;
                        new Thread(() -> {
                            Bitmap b = loadImage(fname);
                            if (b != null) ui.post(() -> ivImg.setImageBitmap(b));
                        }).start();
                        ivImg.setOnClickListener(vv -> {
                            String[] ops = {"保存到 Download", "全屏查看"};
                            new AlertDialog.Builder(ChatActivity.this)
                                .setItems(ops, (d, w) -> {
                                    if (w == 0) downloadFile(raw);
                                    else showFullImage(raw);
                                })
                                .show();
                        });
                    }
                } else if (m.isFile()) {
                    if (m.tampered) {
                        tvContent.setVisibility(View.VISIBLE);
                        tvContent.setText("⚠ 文件疑似被修改");
                        tvContent.setTextColor(0xFFB71C1C);
                    } else {
                        boxFile.setVisibility(View.VISIBLE);
                        tvFileName.setText(m.content);
                        tvFileSize.setText(fmtSize(m.size));
                        boxFile.setOnClickListener(vv -> {
                            String[] ops = {"下载到 Download"};
                            new AlertDialog.Builder(ChatActivity.this)
                                .setItems(ops, (d, w) -> downloadFile(raw))
                                .show();
                        });
                    }
                }
            }

            return v;
        }
    }

    private void showFullImage(ChatMsg m) {
        ChatMsg d = decodeForShow(m);
        final Bitmap b = loadImage(d.content);
        if (b == null) {
            Toast.makeText(this, "图还没下下来", Toast.LENGTH_SHORT).show();
            return;
        }
        ImageView iv = new ImageView(this);
        iv.setImageBitmap(b);
        iv.setAdjustViewBounds(true);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = dp(8);
        iv.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
            .setView(iv)
            .setPositiveButton("关闭", null)
            .show();
    }
}