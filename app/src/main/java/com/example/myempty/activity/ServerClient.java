package com.example.myempty.activity2;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class ServerClient {

    public interface Callback {
        void onSuccess(String body);
        void onFail(String error);
    }

    private static String joinUrl(Context ctx, String path) {
        String base = ServerConfig.getUrl(ctx);
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (!path.startsWith("/")) path = "/" + path;
        return base + path;
    }

    private static void applyHeaders(Context ctx, HttpURLConnection conn) {
        conn.setRequestProperty("User-Agent", "MiniFileManager/1.0");
        String token = ServerConfig.getToken(ctx);
        if (!token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
    }

    public static void get(final Context ctx, final String path,
                           final Map<String, String> params, final Callback cb) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                StringBuilder sb = new StringBuilder(joinUrl(ctx, path));
                if (params != null && !params.isEmpty()) {
                    sb.append("?");
                    boolean first = true;
                    for (Map.Entry<String, String> e : params.entrySet()) {
                        if (!first) sb.append("&");
                        first = false;
                        sb.append(URLEncoder.encode(e.getKey(), "UTF-8"))
                          .append("=")
                          .append(URLEncoder.encode(e.getValue(), "UTF-8"));
                    }
                }
                URL url = new URL(sb.toString());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                applyHeaders(ctx, conn);

                int code = conn.getResponseCode();
                InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                String body = readAll(is);

                if (code >= 200 && code < 300) {
                    if (cb != null) cb.onSuccess(body);
                } else {
                    if (cb != null) cb.onFail("HTTP " + code + ": " + body);
                }
            } catch (Exception e) {
                if (cb != null) cb.onFail(e.getMessage() == null ? "unknown" : e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    public static void post(final Context ctx, final String path,
                            final Map<String, String> body, final Callback cb) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(joinUrl(ctx, path));
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(20000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                applyHeaders(ctx, conn);

                StringBuilder sb = new StringBuilder();
                if (body != null) {
                    boolean first = true;
                    for (Map.Entry<String, String> e : body.entrySet()) {
                        if (!first) sb.append("&");
                        first = false;
                        sb.append(URLEncoder.encode(e.getKey(), "UTF-8"))
                          .append("=")
                          .append(URLEncoder.encode(e.getValue(), "UTF-8"));
                    }
                }

                OutputStream os = conn.getOutputStream();
                os.write(sb.toString().getBytes("UTF-8"));
                os.close();

                int code = conn.getResponseCode();
                InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                String resp = readAll(is);

                if (code >= 200 && code < 300) {
                    if (cb != null) cb.onSuccess(resp);
                } else {
                    if (cb != null) cb.onFail("HTTP " + code + ": " + resp);
                }
            } catch (Exception e) {
                if (cb != null) cb.onFail(e.getMessage() == null ? "unknown" : e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private static String readAll(InputStream is) {
        if (is == null) return "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
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