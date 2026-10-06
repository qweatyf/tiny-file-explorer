package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MorseTerminalActivity extends Activity {

    private TerminalView term;
    private EditText hidden;
    private Button btnCtrl;
    private LinearLayout barCopy;

    private boolean ctrl = false;
    private Thread decodeThread;
    private volatile boolean decoding = false;

    private final List<String> history = new ArrayList<>();
    private int hisIdx = -1;
    private float downX = 0;
    private Uri pendingUri;

    private int exitSwipeCount = 0;
    private long lastSwipeTime = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_morse_terminal);

        term = findViewById(R.id.terminal);
        hidden = findViewById(R.id.hidden_input);
        btnCtrl = findViewById(R.id.btn_ctrl);
        barCopy = findViewById(R.id.bar_copy);

        banner();

        term.setOnTap(() -> {
            hidden.requestFocus();
            InputMethodManager im = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
            im.showSoftInput(hidden, InputMethodManager.SHOW_IMPLICIT);
        });

        hidden.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                term.setInput(s.toString());
            }
        });

        hidden.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND
                || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_GO) {
                String cmd = hidden.getText().toString().trim();
                hidden.setText("");
                if (!TextUtils.isEmpty(cmd)) submitText(cmd);
                return true;
            }
            return false;
        });

        btnCtrl.setOnClickListener(v -> {
            ctrl = !ctrl;
            btnCtrl.setText(ctrl ? "Ctrl*" : "Ctrl");
        });

        findViewById(R.id.btn_c).setOnClickListener(v -> {
            if (ctrl) {
                interrupt();
                ctrl = false;
                btnCtrl.setText("Ctrl");
            } else {
                hidden.append("c");
            }
        });

        findViewById(R.id.btn_tab).setOnClickListener(v -> {
            String cmd = hidden.getText().toString().trim();
            hidden.setText("");
            if (!TextUtils.isEmpty(cmd)) submitText(cmd);
        });

        findViewById(R.id.btn_esc).setOnClickListener(v -> confirmExit());
        findViewById(R.id.btn_slash).setOnClickListener(v -> hidden.append("/"));
        findViewById(R.id.btn_up).setOnClickListener(v -> hisPrev());
        findViewById(R.id.btn_down).setOnClickListener(v -> hisNext());
        findViewById(R.id.btn_ime).setOnClickListener(v -> showImeDialog());

        findViewById(R.id.btn_copy_sel).setOnClickListener(v -> {
            String s = term.getSelectedText();
            if (s != null && !s.isEmpty()) {
                ClipboardManager cm = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("term", s));
                Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
            }
            term.exitSelect();
            barCopy.setVisibility(View.GONE);
        });

        findViewById(R.id.btn_cancel_sel).setOnClickListener(v -> {
            term.exitSelect();
            barCopy.setVisibility(View.GONE);
        });

        term.postDelayed(new Runnable() {
            @Override public void run() {
                barCopy.setVisibility(term.isSelecting() ? View.VISIBLE : View.GONE);
                term.postDelayed(this, 300);
            }
        }, 300);

        loadHistory();
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

    private void banner() {
        term.append("欢迎使用摩斯电码终端\n", TerminalView.COLOR_NORMAL);
        term.append("可用命令：\n", TerminalView.COLOR_NORMAL);
        term.append("clear    - 清屏\n", TerminalView.COLOR_NORMAL);
        term.append("open     - 选音频文件\n", TerminalView.COLOR_NORMAL);
        term.append("decode   - 开始破译\n", TerminalView.COLOR_NORMAL);
        term.append("list     - 查看保存记录\n", TerminalView.COLOR_NORMAL);
        term.append("clean    - 清理全部\n", TerminalView.COLOR_NORMAL);
        term.append("clean log    - 只清日志\n", TerminalView.COLOR_NORMAL);
        term.append("clean audio  - 只清缓存音频\n", TerminalView.COLOR_NORMAL);
        term.append("clean result - 只清破译结果\n", TerminalView.COLOR_NORMAL);
        term.append("history  - 查看历史\n", TerminalView.COLOR_NORMAL);
        term.append("exit     - 退出\n", TerminalView.COLOR_NORMAL);
        term.append("\n点屏幕就能输，长按文字可复制\n", TerminalView.COLOR_NORMAL);
        term.append("滑三次退出\n\n", TerminalView.COLOR_NORMAL);

        if (cacheBig()) {
            term.append("检测到你缓存较多\n", TerminalView.COLOR_ERR);
            term.append("是否清除？清除请输入 clean\n\n", TerminalView.COLOR_ERR);
        }
    }

    private void showImeDialog() {
        EditText input = new EditText(this);
        input.setHint("输入中文、英文或摩斯");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
            .setTitle("输入")
            .setView(input)
            .setPositiveButton("确定", (d, w) -> {
                String s = input.getText().toString().trim();
                if (!TextUtils.isEmpty(s)) submitText(s);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void submitText(String cmd) {
        term.append("> " + cmd + "\n", TerminalView.COLOR_USER);
        history.add(cmd);
        hisIdx = history.size();

        String low = cmd.toLowerCase();
        if (low.equals("clear")) { term.clear(); return; }
        if (low.equals("exit")) { confirmExit(); return; }
        if (low.equals("open")) { pickAudio(); return; }
        if (low.equals("decode")) { startDecode(); return; }
        if (low.equals("list")) { doList(); return; }
        if (low.equals("history")) { showHistory(); return; }
        if (low.startsWith("clean")) { doClean(low); return; }

        if (looksMorse(cmd)) {
            String r = MorseUtils.fromMorse(cmd);
            term.append("Text: " + r + "\n", TerminalView.COLOR_OK);
        } else if (isChinese(cmd)) {
            String py = PinyinUtils.toPinyin(cmd);
            String m = MorseUtils.toMorse(py);
            term.append("Morse: " + m + "\n", TerminalView.COLOR_OK);
        } else if (isEnglish(cmd)) {
            String m = MorseUtils.toMorse(cmd);
            term.append("Morse: " + m + "\n", TerminalView.COLOR_OK);
        } else {
            term.append("Unsupported input.\n", TerminalView.COLOR_ERR);
        }
    }

    private boolean looksMorse(String s) { return s.matches("[\\-./ ]+"); }
    private boolean isEnglish(String s) { return s.matches("[a-zA-Z0-9 /]+"); }

    private boolean isChinese(String s) {
        for (char c : s.toCharArray()) if (c >= 0x4E00 && c <= 0x9FA5) return true;
        return false;
    }

    private void hisPrev() {
        if (history.isEmpty()) return;
        if (hisIdx > 0) hisIdx--;
        hidden.setText(history.get(hisIdx));
        hidden.setSelection(hidden.getText().length());
    }

    private void hisNext() {
        if (history.isEmpty()) return;
        if (hisIdx < history.size() - 1) hisIdx++;
        else hisIdx = history.size();
        hidden.setText(hisIdx >= history.size() ? "" : history.get(hisIdx));
        hidden.setSelection(hidden.getText().length());
    }

    private void interrupt() {
        if (decoding) {
            decoding = false;
            term.append("Process interrupted.\n", TerminalView.COLOR_ERR);
            if (decodeThread != null) decodeThread.interrupt();
        }
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
            .setTitle("确定退出？")
            .setMessage("退出后会终止所有后台进程，防止手机卡顿。")
            .setPositiveButton("确定", (d, w) -> {
                saveLog();
                stopService(new Intent(this, MyServerService.class));
                finishAffinity();
                System.exit(0);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void pickAudio() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, 100);
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent data) {
        super.onActivityResult(rc, res, data);
        if (rc == 100 && res == RESULT_OK && data != null) {
            pendingUri = data.getData();
            term.append("File loaded: " + pendingUri + "\n", TerminalView.COLOR_OK);
        }
    }

    private File buildUnique(File dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) return f;

        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";

        int i = 1;
        while (true) {
            File g = new File(dir, base + "(" + i + ")" + ext);
            if (!g.exists()) return g;
            i++;
        }
    }

    private String buildSameSound(String pinyin) {
        StringBuilder sb = new StringBuilder();
        String[] parts = pinyin.toLowerCase().split("\\s+");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            String r;
            try {
                r = PinyinUtils.fromPinyin(part);
            } catch (Throwable t) {
                r = "（查询失败）";
            }
            sb.append(part).append(" → ").append(r).append("\n");
        }
        return sb.toString().trim();
    }

    private void startDecode() {
        if (pendingUri == null) {
            term.append("No file. Use 'open' first.\n", TerminalView.COLOR_ERR);
            return;
        }
        if (decoding) {
            term.append("Already decoding.\n", TerminalView.COLOR_ERR);
            return;
        }

        decoding = true;
        final Uri uri = pendingUri;
        final long startTime = System.currentTimeMillis();

        decodeThread = new Thread(() -> {
            try {
                term.post(() -> term.append("Reading file...\n", TerminalView.COLOR_NORMAL));
                Thread.sleep(200);
                term.post(() -> term.append("Analyzing audio...\n", TerminalView.COLOR_NORMAL));

                AudioDecoder.Result r = AudioDecoder.decode(this, uri);

                if (!decoding) return;

                if (!r.ok) {
                    term.post(() -> term.append("Error: " + r.message + "\n", TerminalView.COLOR_ERR));
                    decoding = false;
                    return;
                }

                term.post(() -> {
                    term.append("Detected morse: " + r.morse + "\n", TerminalView.COLOR_NORMAL);
                    term.append("Note: " + r.message + "\n", TerminalView.COLOR_ERR);
                });

                Thread.sleep(300);
                if (!decoding) return;

                term.post(() -> term.append("Translating...\n", TerminalView.COLOR_NORMAL));
                String pinyin = MorseUtils.fromMorse(r.morse);

                String sameSound = buildSameSound(pinyin);

                Thread.sleep(200);

                long elapsed = (System.currentTimeMillis() - startTime) / 1000;

                File dirEn = new File(Environment.getExternalStorageDirectory(),
                    "Download/摩斯电码破译文件存放地/英文");
                if (!dirEn.exists()) dirEn.mkdirs();
                File fEn = buildUnique(dirEn, "破译结果.txt");
                FileOutputStream fosEn = new FileOutputStream(fEn);
                fosEn.write(("摩斯电码：" + r.morse + "\n").getBytes("UTF-8"));
                fosEn.write(("拼音：" + pinyin + "\n").getBytes("UTF-8"));
                fosEn.close();

                File dirCn = new File(Environment.getExternalStorageDirectory(),
                    "Download/摩斯电码破译文件存放地/中文");
                if (!dirCn.exists()) dirCn.mkdirs();
                File fCn = buildUnique(dirCn, "破译结果.txt");
                FileOutputStream fosCn = new FileOutputStream(fCn);
                fosCn.write(("摩斯电码：" + r.morse + "\n").getBytes("UTF-8"));
                fosCn.write(("拼音：" + pinyin + "\n").getBytes("UTF-8"));
                fosCn.write(("同音字：\n" + sameSound + "\n").getBytes("UTF-8"));
                fosCn.close();

                final String py = pinyin;
                final String ss = sameSound;
                final long el = elapsed;

                term.post(() -> {
                    term.append("拼音：" + py + "\n", TerminalView.COLOR_OK);
                    term.append("同音字：\n" + ss + "\n", TerminalView.COLOR_OK);
                    term.append("Saved to: " + fCn.getAbsolutePath() + "\n", TerminalView.COLOR_OK);
                    term.append("Done. Time: " + el + "s\n", TerminalView.COLOR_OK);
                });
            } catch (Exception e) {
                term.post(() -> term.append("Error: " + e.getMessage() + "\n", TerminalView.COLOR_ERR));
            } finally {
                decoding = false;
            }
        });
        decodeThread.start();
    }

    private void doList() {
        File root = Environment.getExternalStorageDirectory();
        File result = new File(root, "Download/摩斯电码破译文件存放地");
        if (!result.exists()) {
            term.append("No result yet.\n", TerminalView.COLOR_NORMAL);
            return;
        }
        File[] subs = result.listFiles();
        if (subs == null || subs.length == 0) {
            term.append("No result yet.\n", TerminalView.COLOR_NORMAL);
            return;
        }
        for (File s : subs) {
            if (!s.isDirectory()) continue;
            File[] fs = s.listFiles();
            if (fs == null) continue;
            for (File f : fs) {
                term.append("[结果] " + s.getName() + "/" + f.getName() + "\n", TerminalView.COLOR_NORMAL);
            }
        }
    }

    private void showHistory() {
        for (String s : history) term.append("> " + s + "\n", TerminalView.COLOR_NORMAL);
    }

    private void doClean(String cmd) {
        File files = getExternalFilesDir(null);
        File log = new File(files, "terminal_log.txt");
        File cache = new File(files, "cache");
        File result = new File(Environment.getExternalStorageDirectory(),
            "Download/摩斯电码破译文件存放地");

        if (cmd.equals("clean log")) {
            delete(log);
            term.append("Log cleared.\n", TerminalView.COLOR_OK);
        } else if (cmd.equals("clean audio")) {
            delete(cache);
            term.append("Audio cache cleared.\n", TerminalView.COLOR_OK);
        } else if (cmd.equals("clean result")) {
            delete(result);
            term.append("Result cleared.\n", TerminalView.COLOR_OK);
        } else {
            delete(log);
            delete(cache);
            delete(result);
            term.append("All cleaned.\n", TerminalView.COLOR_OK);
        }
    }

    private void delete(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] fs = f.listFiles();
            if (fs != null) for (File c : fs) delete(c);
        }
        f.delete();
    }

    private boolean cacheBig() {
        File files = getExternalFilesDir(null);
        if (files == null) return false;
        File cache = new File(files, "cache");
        if (!cache.exists()) return false;
        int n = 0;
        long size = 0;
        File[] fs = cache.listFiles();
        if (fs != null) {
            for (File f : fs) {
                n++;
                size += f.length();
            }
        }
        return n > 20 || size > 5 * 1024 * 1024;
    }

    private void saveLog() {
        try {
            File files = getExternalFilesDir(null);
            if (files == null) return;
            File log = new File(files, "terminal_log.txt");
            FileOutputStream fos = new FileOutputStream(log, true);
            for (String s : history) fos.write(("> " + s + "\n").getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadHistory() {
        try {
            File files = getExternalFilesDir(null);
            if (files == null) return;
            File log = new File(files, "terminal_log.txt");
            if (!log.exists()) return;
            BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(log), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("> ")) history.add(line.substring(2));
            }
            br.close();
            hisIdx = history.size();
        } catch (Exception e) { e.printStackTrace(); }
    }
}