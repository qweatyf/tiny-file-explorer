package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class MarkdownViewActivity extends Activity {

    private EditText etInput;
    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_markdown_view);

        etInput = findViewById(R.id.et_md_input);
        tvOutput = findViewById(R.id.tv_md_output);

        etInput.setText("# 大标题\n## 小标题\n\n**粗体** 和 `代码` 和普通文字\n\n> 引用内容\n\n- 列表项1\n- 列表项2\n\n结束");

        findViewById(R.id.btn_md_render).setOnClickListener(v -> render());
        findViewById(R.id.btn_md_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("text/*");
            startActivityForResult(i, 100);
        });

        render();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                is.close();
                etInput.setText(new String(bos.toByteArray(), "UTF-8"));
                render();
            } catch (Exception e) {
                Toast.makeText(this, "读不了", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void render() {
        String src = etInput.getText().toString();
        SpannableString ss = new SpannableString(src);

        String[] lines = src.split("\n", -1);
        int offset = 0;

        for (String line : lines) {
            int lineEnd = offset + line.length();
            String t = line.trim();

            if (t.startsWith("#")) {
                int level = 0;
                while (level < t.length() && t.charAt(level) == '#') level++;
                int contentStart = offset + line.indexOf(t);
                contentStart += level;
                while (contentStart < lineEnd && src.charAt(contentStart) == ' ') {
                    contentStart++;
                }
                if (contentStart < lineEnd) {
                    ss.setSpan(new StyleSpan(Typeface.BOLD),
                        contentStart, lineEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    float scale = level == 1 ? 1.6f
                        : level == 2 ? 1.4f
                        : level == 3 ? 1.2f : 1.1f;
                    ss.setSpan(new android.text.style.RelativeSizeSpan(scale),
                        contentStart, lineEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            if (t.startsWith(">")) {
                ss.setSpan(new ForegroundColorSpan(0xFF888888),
                    offset, lineEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new StyleSpan(Typeface.ITALIC),
                    offset, lineEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (t.startsWith("-") || t.startsWith("*")) {
                ss.setSpan(new ForegroundColorSpan(0xFFD87093),
                    offset, lineEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            int i = 0;
            while (i < line.length() - 1) {
                if (line.charAt(i) == '*' && line.charAt(i + 1) == '*') {
                    int close = line.indexOf("**", i + 2);
                    if (close < 0) break;
                    ss.setSpan(new StyleSpan(Typeface.BOLD),
                        offset + i, offset + close + 2,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = close + 2;
                } else {
                    i++;
                }
            }

            i = 0;
            while (i < line.length()) {
                if (line.charAt(i) == '`') {
                    int close = line.indexOf("`", i + 1);
                    if (close < 0) break;
                    ss.setSpan(new android.text.style.TypefaceSpan("monospace"),
                        offset + i, offset + close + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ss.setSpan(new BackgroundColorSpan(0xFFEEEEEE),
                        offset + i, offset + close + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = close + 1;
                } else {
                    i++;
                }
            }

            offset = lineEnd + 1;
        }

        tvOutput.setText(ss);
    }
}