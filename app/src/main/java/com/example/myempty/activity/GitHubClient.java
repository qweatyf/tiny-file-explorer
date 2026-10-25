package com.example.myempty.activity2;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GitHubClient {

    public interface Progress {
        void onStep(String msg);
        void onFile(int current, int total, String path);
        void onDone(String commitUrl);
        void onFail(String error);
    }

    public interface TokenCheck {
        void onResult(boolean ok, String loginOrError);
    }

    private static final String API = "https://api.github.com";

    private final String token;

    public GitHubClient(String token) {
        this.token = token;
    }

    public void checkToken(final TokenCheck cb) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = open("GET", "/user", null);
                int code = conn.getResponseCode();
                String body = readAll(code >= 400
                    ? conn.getErrorStream() : conn.getInputStream());
                conn.disconnect();
                if (code == 200) {
                    JSONObject o = new JSONObject(body);
                    cb.onResult(true, o.optString("login", ""));
                } else {
                    cb.onResult(false, "HTTP " + code);
                }
            } catch (Exception e) {
                cb.onResult(false, e.getMessage() == null ? "unknown" : e.getMessage());
            }
        }).start();
    }

    public void uploadDir(final File dir, final String owner, final String repo,
                          final String branch, final String commitMsg,
                          final String excludes,
                          final Progress cb) {
        new Thread(() -> {
            try {
                final List<String> patterns = parseExcludes(excludes);

                cb.onStep("check repo...");
                apiGet("/repos/" + owner + "/" + repo);

                cb.onStep("read branch " + branch + " ...");
                JSONObject refObj = apiGet("/repos/" + owner + "/" + repo
                    + "/git/refs/heads/" + branch);
                String parentSha = refObj.getJSONObject("object").getString("sha");

                cb.onStep("read commit...");
                JSONObject parentCommit = apiGet("/repos/" + owner + "/" + repo
                    + "/git/commits/" + parentSha);
                String parentTreeSha = parentCommit.getJSONObject("tree")
                    .getString("sha");

                List<File> files = new ArrayList<>();
                listFiles(dir, dir, files, patterns);
                cb.onStep("files: " + files.size());

                JSONArray treeItems = new JSONArray();
                for (int i = 0; i < files.size(); i++) {
                    File f = files.get(i);
                    String relPath = relative(dir, f);
                    cb.onFile(i + 1, files.size(), relPath);

                    byte[] data = readBytes(f);
                    String base64 = Base64.encodeToString(data, Base64.NO_WRAP);

                    JSONObject blobReq = new JSONObject();
                    blobReq.put("content", base64);
                    blobReq.put("encoding", "base64");
                    JSONObject blobResp = apiPost("/repos/" + owner + "/" + repo
                        + "/git/blobs", blobReq);
                    String blobSha = blobResp.getString("sha");

                    JSONObject item = new JSONObject();
                    item.put("path", relPath);
                    item.put("mode", "100644");
                    item.put("type", "blob");
                    item.put("sha", blobSha);
                    treeItems.put(item);
                }

                cb.onStep("create tree...");
                JSONObject treeReq = new JSONObject();
                treeReq.put("base_tree", parentTreeSha);
                treeReq.put("tree", treeItems);
                JSONObject treeResp = apiPost("/repos/" + owner + "/" + repo
                    + "/git/trees", treeReq);
                String newTreeSha = treeResp.getString("sha");

                cb.onStep("create commit...");
                JSONObject commitReq = new JSONObject();
                commitReq.put("message", commitMsg);
                commitReq.put("tree", newTreeSha);
                JSONArray parents = new JSONArray();
                parents.put(parentSha);
                commitReq.put("parents", parents);
                JSONObject commitResp = apiPost("/repos/" + owner + "/" + repo
                    + "/git/commits", commitReq);
                String newCommitSha = commitResp.getString("sha");

                cb.onStep("update branch...");
                JSONObject updateReq = new JSONObject();
                updateReq.put("sha", newCommitSha);
                updateReq.put("force", false);
                apiPatch("/repos/" + owner + "/" + repo
                    + "/git/refs/heads/" + branch, updateReq);

                String commitUrl = "https://github.com/" + owner + "/" + repo
                    + "/commit/" + newCommitSha;
                cb.onDone(commitUrl);
            } catch (Exception e) {
                cb.onFail(e.getMessage() == null ? "unknown" : e.getMessage());
            }
        }).start();
    }

    private List<String> parseExcludes(String s) {
        List<String> list = new ArrayList<>();
        if (s == null) return list;
        String[] parts = s.split("[,\\n\\r]+");
        for (String p : parts) {
            p = p.trim();
            if (!p.isEmpty()) list.add(p);
        }
        return list;
    }

    private boolean isExcluded(String relPath, String name, List<String> patterns) {
        for (String p : patterns) {
            String pat = p;
            boolean dirOnly = false;
            if (pat.endsWith("/")) {
                dirOnly = true;
                pat = pat.substring(0, pat.length() - 1);
            }
            if (dirOnly) {
                if (name.equals(pat) || relPath.equals(pat)
                    || relPath.startsWith(pat + "/")) {
                    return true;
                }
            } else {
                if (name.equals(pat) || relPath.equals(pat)) return true;
                if (pat.contains("*")) {
                    String regex = pat.replace(".", "\\.").replace("*", ".*");
                    if (name.matches(regex)) return true;
                    if (relPath.matches(regex)) return true;
                }
            }
        }
        return false;
    }

    private void listFiles(File root, File dir, List<File> out,
                           List<String> patterns) {
        File[] kids = dir.listFiles();
        if (kids == null) return;
        for (File f : kids) {
            String rel = relative(root, f);
            if (isExcluded(rel, f.getName(), patterns)) continue;
            if (f.isDirectory()) {
                listFiles(root, f, out, patterns);
            } else {
                out.add(f);
            }
        }
    }

    private String relative(File root, File f) {
        String r = root.getAbsolutePath();
        String a = f.getAbsolutePath();
        if (a.startsWith(r)) {
            String s = a.substring(r.length());
            if (s.startsWith("/")) s = s.substring(1);
            return s;
        }
        return f.getName();
    }

    private byte[] readBytes(File f) throws Exception {
        byte[] buf = new byte[(int) f.length()];
        FileInputStream fis = new FileInputStream(f);
        int read = 0;
        while (read < buf.length) {
            int n = fis.read(buf, read, buf.length - read);
            if (n < 0) break;
            read += n;
        }
        fis.close();
        return buf;
    }

    private HttpURLConnection open(String method, String path, String body)
        throws Exception {
        URL url = new URL(API + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("Authorization", "token " + token);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "MyEmptyActivity");
        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes("UTF-8"));
            os.close();
        }
        return conn;
    }

    private JSONObject apiGet(String path) throws Exception {
        HttpURLConnection conn = open("GET", path, null);
        int code = conn.getResponseCode();
        String body = readAll(code >= 400
            ? conn.getErrorStream() : conn.getInputStream());
        conn.disconnect();
        if (code < 200 || code >= 300) {
            throw new Exception("GET " + path + " HTTP " + code + ": " + body);
        }
        return new JSONObject(body);
    }

    private JSONObject apiPost(String path, JSONObject req) throws Exception {
        HttpURLConnection conn = open("POST", path, req.toString());
        int code = conn.getResponseCode();
        String body = readAll(code >= 400
            ? conn.getErrorStream() : conn.getInputStream());
        conn.disconnect();
        if (code < 200 || code >= 300) {
            throw new Exception("POST " + path + " HTTP " + code + ": " + body);
        }
        return new JSONObject(body);
    }

    private JSONObject apiPatch(String path, JSONObject req) throws Exception {
        HttpURLConnection conn = open("PATCH", path, req.toString());
        int code = conn.getResponseCode();
        String body = readAll(code >= 400
            ? conn.getErrorStream() : conn.getInputStream());
        conn.disconnect();
        if (code < 200 || code >= 300) {
            throw new Exception("PATCH " + path + " HTTP " + code + ": " + body);
        }
        return new JSONObject(body);
    }

    private String readAll(InputStream is) {
        if (is == null) return "";
        try {
            BufferedReader br = new BufferedReader(
                new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}