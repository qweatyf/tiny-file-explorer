package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpRequestActivity extends Activity {

    private Spinner spMethod;
    private EditText etUrl;
    private EditText etHeaders;
    private EditText etBody;
    private TextView tvResult;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_http_request);

        spMethod = findViewById(R.id.sp_http_method);
        etUrl = findViewById(R.id.et_http_url);
        etHeaders = findViewById(R.id.et_http_headers);
        etBody = findViewById(R.id.et_http_body);
        tvResult = findViewById(R.id.tv_http_result);

        String[] methods = {"GET", "POST", "PUT", "DELETE", "HEAD", "PATCH"};
        spMethod.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, methods));

        findViewById(R.id.btn_http_send).setOnClickListener(v -> send());
        findViewById(R.id.btn_http_clear).setOnClickListener(v -> {
            tvResult.setText("");
            etBody.setText("");
            etHeaders.setText("");
        });
    }

    private void send() {
        String url = etUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "URL 填一下", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        final String method = spMethod.getSelectedItem().toString();
        final String headersText = etHeaders.getText().toString();
        final String body = etBody.getText().toString();
        final String finalUrl = url;

        tvResult.setText("请求中...");

        new Thread(() -> {
            try {
                URL u = new URL(finalUrl);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "MyEmptyActivity/1.0");

                if (!headersText.isEmpty()) {
                    for (String line : headersText.split("\n")) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        int colon = line.indexOf(':');
                        if (colon > 0) {
                            String k = line.substring(0, colon).trim();
                            String v = line.substring(colon + 1).trim();
                            conn.setRequestProperty(k, v);
                        }
                    }
                }

                boolean hasBody = method.equals("POST") || method.equals("PUT")
                    || method.equals("PATCH") || method.equals("DELETE");
                if (hasBody && !body.isEmpty()) {
                    conn.setDoOutput(true);
                    OutputStream os = conn.getOutputStream();
                    os.write(body.getBytes("UTF-8"));
                    os.close();
                }

                int code = conn.getResponseCode();
                String msg = conn.getResponseMessage();

                StringBuilder sb = new StringBuilder();
                sb.append("状态码: ").append(code).append(" ").append(msg).append("\n\n");
                sb.append("响应头:\n");
                for (java.util.Map.Entry<String, java.util.List<String>> e
                    : conn.getHeaderFields().entrySet()) {
                    if (e.getKey() == null) continue;
                    sb.append(e.getKey()).append(": ")
                        .append(String.join(", ", e.getValue())).append("\n");
                }
                sb.append("\n");

                InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                if (is != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder bodyOut = new StringBuilder();
                    String line;
                    int total = 0;
                    while ((line = br.readLine()) != null) {
                        bodyOut.append(line).append("\n");
                        total += line.length() + 1;
                        if (total > 200000) {
                            bodyOut.append("\n...(截断，太大)");
                            break;
                        }
                    }
                    br.close();
                    sb.append("正文:\n").append(bodyOut);
                }

                final String result = sb.toString();
                ui.post(() -> tvResult.setText(result));
            } catch (Exception e) {
                ui.post(() -> tvResult.setText("请求失败: " + e.getMessage()));
            }
        }).start();
    }
}