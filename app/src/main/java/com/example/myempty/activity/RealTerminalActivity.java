package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class RealTerminalActivity extends Activity {

    private TerminalView term;
    private Button btnCtrl;
    private LinearLayout copyBar;

    private LocalShell shell;

    private final List<String> history = new ArrayList<>();
    private int hisIdx = -1;

    private boolean ctrl = false;
    private float downX = 0;
    private int exitSwipeCount = 0;
    private long lastSwipeTime = 0;

    private float[] fontSizes = {28f, 36f, 44f};
    private int fontIdx = 1;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_real_terminal);

        term = findViewById(R.id.real_terminal);
        btnCtrl = findViewById(R.id.btn_rt_ctrl);
        copyBar = findViewById(R.id.rt_copy_bar);

        term.setFontSize(fontSizes[fontIdx]);

        shell = new LocalShell(this, (text, color) -> {
            if (text != null && text.startsWith("\u0001")) {
                String real = text.substring(1);
                runOnUiThread(() -> term.replaceLastLine(real, color));
            } else {
                runOnUiThread(() -> term.append(text + "\n", color));
            }
        });

        shell.setAsk((question, answer) -> runOnUiThread(() -> {
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setHint("输入 y 或 n");
            input.setBackgroundColor(0xFFFFFFFF);
            input.setPadding(30, 20, 30, 20);

            new AlertDialog.Builder(this)
                .setTitle("需要确认")
                .setMessage(question)
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("确定", (d, w) -> {
                    String s = input.getText().toString().trim();
                    if (s.isEmpty()) s = "n";
                    term.append("> " + s + "\n", TerminalView.COLOR_USER);
                    answer.reply(s);
                })
                .setNegativeButton("取消", (d, w) -> {
                    term.append("> n\n", TerminalView.COLOR_USER);
                    answer.reply("n");
                })
                .show();
        }));

        term.append("本地终端已启动。\n", TerminalView.COLOR_OK);
        term.append("输入 help 查看所有命令。\n\n", TerminalView.COLOR_NORMAL);

        term.setInputListener(new TerminalView.InputListener() {
            @Override
            public void onInputChanged(String text) {
            }

            @Override
            public void onEnter(String text) {
                String cmd = text.trim();
                if (cmd.isEmpty()) return;
                history.add(cmd);
                hisIdx = history.size();
                term.append("> " + cmd + "\n", TerminalView.COLOR_USER);
                runCmd(cmd);
            }
        });

        term.setOnTap(() -> {
            term.requestFocus();
            InputMethodManager im = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
            im.showSoftInput(term, InputMethodManager.SHOW_IMPLICIT);
        });

        btnCtrl.setOnClickListener(v -> {
            ctrl = !ctrl;
            btnCtrl.setText(ctrl ? "Ctrl*" : "Ctrl");
        });

        findViewById(R.id.btn_rt_c).setOnClickListener(v -> {
            if (ctrl) {
                CmdNetwork.cancelCurrent();
                term.append("已发送 Ctrl+C 中断信号\n", TerminalView.COLOR_ERR);
                ctrl = false;
                btnCtrl.setText("Ctrl");
            } else {
                term.setInput(term.getInput() + "c");
            }
        });

        findViewById(R.id.btn_rt_tab).setOnClickListener(v ->
            term.setInput(term.getInput() + "    "));
        findViewById(R.id.btn_rt_esc).setOnClickListener(v -> confirmExit());
        findViewById(R.id.btn_rt_slash).setOnClickListener(v ->
            term.setInput(term.getInput() + "/"));
        findViewById(R.id.btn_rt_up).setOnClickListener(v -> hisPrev());
        findViewById(R.id.btn_rt_down).setOnClickListener(v -> hisNext());

        findViewById(R.id.btn_rt_clear).setOnClickListener(v -> term.clear());
        findViewById(R.id.btn_rt_paste).setOnClickListener(v -> pasteFromClip());
        findViewById(R.id.btn_rt_font).setOnClickListener(v -> switchFont());
        findViewById(R.id.btn_rt_session).setOnClickListener(v -> switchSession());
        findViewById(R.id.btn_rt_pick).setOnClickListener(v -> pickFile());

        findViewById(R.id.rt_btn_copy_sel).setOnClickListener(v -> {
            String s = term.getSelectedText();
            if (s != null && !s.isEmpty()) {
                ClipboardManager cm = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("term", s));
                Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
            }
            term.exitSelect();
            copyBar.setVisibility(View.GONE);
        });

        findViewById(R.id.rt_btn_cancel_sel).setOnClickListener(v -> {
            term.exitSelect();
            copyBar.setVisibility(View.GONE);
        });

        term.postDelayed(new Runnable() {
            @Override public void run() {
                copyBar.setVisibility(term.isSelecting() ? View.VISIBLE : View.GONE);
                term.postDelayed(this, 300);
            }
        }, 300);

        term.requestFocus();
    }

    private void runCmd(String cmd) {
        String low = cmd.trim().toLowerCase();

        if (low.equals("exit") || low.equals("quit")) {
            confirmExit();
            return;
        }
        if (low.equals("clear")) {
            term.clear();
            return;
        }

        new Thread(() -> {
            try {
                shell.exec(cmd);
            } catch (Throwable e) {
                runOnUiThread(() -> term.append(
                    "error: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n",
                    TerminalView.COLOR_ERR));
            }
        }).start();
    }

    private void pasteFromClip() {
        ClipboardManager cm = (ClipboardManager)
            getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm.hasPrimaryClip() && cm.getPrimaryClip() != null) {
            CharSequence s = cm.getPrimaryClip().getItemAt(0).getText();
            if (s != null) term.setInput(term.getInput() + s);
        }
    }

    private void switchFont() {
        fontIdx = (fontIdx + 1) % fontSizes.length;
        term.setFontSize(fontSizes[fontIdx]);
        Toast.makeText(this, "字体：" +
            new String[]{"小", "中", "大"}[fontIdx], Toast.LENGTH_SHORT).show();
    }

    private void switchSession() {
        new AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage("本地模式只有一个会话，刷新页面即重置。")
            .setPositiveButton("知道了", null)
            .show();
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, 200);
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent data) {
        super.onActivityResult(rc, res, data);
        if (rc == 200 && res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String path = uri.getPath();
                if (path != null) term.setInput(term.getInput() + path);
            }
        }
    }

    private void hisPrev() {
        if (history.isEmpty()) return;
        if (hisIdx > 0) hisIdx--;
        term.setInput(history.get(hisIdx));
    }

    private void hisNext() {
        if (history.isEmpty()) return;
        if (hisIdx < history.size() - 1) hisIdx++;
        else hisIdx = history.size();
        term.setInput(hisIdx >= history.size() ? "" : history.get(hisIdx));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (term.isSelecting()) return super.onTouchEvent(e);

        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            downX = e.getX();
            return true;
        }

        if (e.getAction() == MotionEvent.ACTION_UP) {
            float dx = e.getX() - downX;
            if (Math.abs(dx) > 200) {
                long now = System.currentTimeMillis();
                if (now - lastSwipeTime > 3000) exitSwipeCount = 0;
                exitSwipeCount++;
                lastSwipeTime = now;
                if (exitSwipeCount >= 3) {
                    exitSwipeCount = 0;
                    confirmExit();
                } else {
                    Toast.makeText(this,
                        "再滑 " + (3 - exitSwipeCount) + " 次退出",
                        Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        }
        return super.onTouchEvent(e);
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
            .setTitle("确定退出？")
            .setMessage("退出后清空当前会话。")
            .setPositiveButton("确定", (d, w) -> finish())
            .setNegativeButton("取消", null)
            .show();
    }
}