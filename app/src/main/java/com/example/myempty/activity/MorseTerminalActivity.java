package com.example.myempty.activity2;

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
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MorseTerminalActivity extends AppCompatActivity {

    private TerminalView term;
    private EditText hidden;
    private Button btnCtrl;
    private LinearLayout barCopy;

    private boolean ctrl = false;
    private Thread decodeThread;
    private volatile boolean decoding = false;

    private final List<String> history = new ArrayList<>();
    private int hisIdx = -1;
    private Uri pendingUri;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

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
                Toast.makeText(this, getString(R.string.file_done), Toast.LENGTH_SHORT).show();
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

    private void banner() {
        term.append(getString(R.string.term_welcome) + "\n", TerminalView.COLOR_NORMAL);
        term.append(getString(R.string.term_cmds) + "\n", TerminalView.COLOR_NORMAL);
        term.append(getString(R.string.term_cmd_clear) + "\n", TerminalView.COLOR_NORMAL);
        term.append(getString(R.string.term_cmd_open) + "\n", TerminalView.COLOR_NORMAL);
        term.append(getString(R.string.term_cmd_decode) + "\n", TerminalView.COLOR_NORMAL);
        term.append(getString(R.string.term_cmd_list) + "\n", TerminalView.COLOR_NORMAL);
        term.append(getString(R.string.term_cmd_clean) + "\n", TerminalView.COLOR_NORMAL);
        term.append(getString(R.string.term_cmd_clean_log) + "\n", TerminalView.COLOR_NORMAL);
        term.append(getString(R.string.term_cmd_clean_audio) + "\n", TerminalView.COLOR_NORMAL);
        term.append(getString(R.string.term_cmd_clean_result) + "\n", TerminalView.COLOR_NORMAL);
        term.append(getString(R.string.term_cmd_history) + "\n", TerminalView.COLOR_NORMAL);
        term.append(getString(R.string.term_cmd_exit) + "\n", TerminalView.COLOR_NORMAL);
        term.append("\n" + getString(R.string.term_tip) + "\n\n", TerminalView.COLOR_NORMAL);

        if (cacheBig()) {
            term.append(getString(R.string.term_cache_big) + "\n", TerminalView.COLOR_ERR);
            term.append(getString(R.string.term_cache_clear) + "\n\n", TerminalView.COLOR_ERR);
        }
    }

    private void showImeDialog() {
        EditText input = new EditText(this);
        input.setHint(getString(R.string.term_input_hint));
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.term_input_title))
            .setView(input)
            .setPositiveButton(getString(R.string.lang_ok), (d, w) -> {
                String s = input.getText().toString().trim();
                if (!TextUtils.isEmpty(s)) submitText(s);
            })
            .setNegativeButton(getString(R.string.file_rename_cancel), null)
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
            term.append(getString(R.string.term_text_label) + r + "\n", TerminalView.COLOR_OK);
        } else if (isChinese(cmd)) {
            String py = PinyinUtils.toPinyin(cmd);
            String m = MorseUtils.toMorse(py);
            term.append(getString(R.string.term_morse_label) + m + "\n", TerminalView.COLOR_OK);
        } else if (isEnglish(cmd)) {
            String m = MorseUtils.toMorse(cmd);
            term.append(getString(R.string.term_morse_label) + m + "\n", TerminalView.COLOR_OK);
        } else {
            term.append(getString(R.string.term_unknown) + "\n", TerminalView.COLOR_ERR);
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
            term.append(getString(R.string.term_interrupted) + "\n", TerminalView.COLOR_ERR);
            if (decodeThread != null) decodeThread.interrupt();
        }
    }

    @Override
    public void onBackPressed() {
        confirmExit();
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.term_exit_title))
            .setMessage(getString(R.string.term_exit_msg))
            .setPositiveButton(getString(R.string.lang_ok), (d, w) -> {
                saveLog();
                stopService(new Intent(this, MyServerService.class));
                finishAffinity();
                System.exit(0);
            })
            .setNegativeButton(getString(R.string.file_rename_cancel), null)
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
            term.append(getString(R.string.term_file_loaded) + pendingUri + "\n", TerminalView.COLOR_OK);
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

    private void startDecode() {
        if (pendingUri == null) {
            term.append(getString(R.string.term_no_file) + "\n", TerminalView.COLOR_ERR);
            return;
        }
        if (decoding) {
            term.append(getString(R.string.term_already_decoding) + "\n", TerminalView.COLOR_ERR);
            return;
        }

        decoding = true;
        final Uri uri = pendingUri;
        final long startTime = System.currentTimeMillis();

        decodeThread = new Thread(() -> {
            try {
                term.post(() -> term.append(getString(R.string.term_read_file) + "\n", TerminalView.COLOR_NORMAL));
                Thread.sleep(200);
                term.post(() -> term.append(getString(R.string.term_analyze) + "\n", TerminalView.COLOR_NORMAL));

                AudioDecoder.Result r = AudioDecoder.decode(this, uri);

                if (!decoding) return;

                if (!r.ok) {
                    term.post(() -> term.append(r.message + "\n", TerminalView.COLOR_ERR));
                    decoding = false;
                    return;
                }

                term.post(() -> {
                    term.append(getString(R.string.term_detected) + r.morse + "\n", TerminalView.COLOR_NORMAL);
                    term.append(getString(R.string.term_note) + r.message + "\n", TerminalView.COLOR_ERR);
                });

                Thread.sleep(300);
                if (!decoding) return;

                term.post(() -> term.append(getString(R.string.term_translating) + "\n", TerminalView.COLOR_NORMAL));
                String rawPinyin = MorseUtils.fromMorse(r.morse);

                Thread.sleep(200);

                long elapsed = (System.currentTimeMillis() - startTime) / 1000;

                File dirEn = new File(Environment.getExternalStorageDirectory(),
                    "Download/摩斯电码破译文件存放地/英文");
                if (!dirEn.exists()) dirEn.mkdirs();
                File fEn = buildUnique(dirEn, "破译结果.txt");
                FileOutputStream fosEn = new FileOutputStream(fEn);
                fosEn.write(("morse: " + r.morse + "\n").getBytes("UTF-8"));
                fosEn.write(("pinyin: " + rawPinyin + "\n").getBytes("UTF-8"));
                fosEn.close();

                File dirCn = new File(Environment.getExternalStorageDirectory(),
                    "Download/摩斯电码破译文件存放地/中文");
                if (!dirCn.exists()) dirCn.mkdirs();
                File fCn = buildUnique(dirCn, "破译结果.txt");

                final String py = rawPinyin;
                final long el = elapsed;
                final String finalPath = fCn.getAbsolutePath();

                String[] parts = rawPinyin.split("\\s+");
                StringBuilder allSameSound = new StringBuilder();

                for (String part : parts) {
                    if (part.trim().isEmpty()) continue;
                    String cleanPart = part.replaceAll("[0-9]", "");
                    if (cleanPart.isEmpty()) continue;

                    final Object lock = new Object();
                    final boolean[] done = {false};

                    PinyinUtils.fromPinyinAsync(cleanPart, new PinyinUtils.OnPinyinCallback() {
                        @Override
                        public void onResult(String result) {
                            synchronized (lock) {
                                allSameSound.append(cleanPart).append(" -> ").append(result).append("\n");
                                done[0] = true;
                                lock.notify();
                            }
                        }
                    });

                    synchronized (lock) {
                        while (!done[0]) {
                            try { lock.wait(500); } catch (Exception ignored) {}
                        }
                    }
                }

                final String finalSameSound = allSameSound.toString().trim();

                try {
                    FileOutputStream fosCn = new FileOutputStream(fCn);
                    fosCn.write(("morse: " + r.morse + "\n").getBytes("UTF-8"));
                    fosCn.write(("pinyin: " + py + "\n").getBytes("UTF-8"));
                    fosCn.write(("same: \n" + finalSameSound + "\n").getBytes("UTF-8"));
                    fosCn.close();
                } catch (Exception ignored) {}

                term.post(() -> {
                    term.append(getString(R.string.term_pinyin) + py + "\n", TerminalView.COLOR_OK);
                    term.append(getString(R.string.term_same_sound) + "\n" + finalSameSound + "\n", TerminalView.COLOR_OK);
                    term.append(getString(R.string.term_saved_to) + finalPath + "\n", TerminalView.COLOR_OK);
                    term.append(getString(R.string.term_done) + " " + getString(R.string.term_time) + el + getString(R.string.term_seconds) + "\n", TerminalView.COLOR_OK);
                });
                decoding = false;

            } catch (Exception e) {
                term.post(() -> term.append("error: " + e.getMessage() + "\n", TerminalView.COLOR_ERR));
                decoding = false;
            }
        });
        decodeThread.start();
    }

    private void doList() {
        File root = Environment.getExternalStorageDirectory();
        File result = new File(root, "Download/摩斯电码破译文件存放地");
        if (!result.exists()) {
            term.append(getString(R.string.term_no_result) + "\n", TerminalView.COLOR_NORMAL);
            return;
        }
        File[] subs = result.listFiles();
        if (subs == null || subs.length == 0) {
            term.append(getString(R.string.term_no_result) + "\n", TerminalView.COLOR_NORMAL);
            return;
        }
        for (File s : subs) {
            if (!s.isDirectory()) continue;
            File[] fs = s.listFiles();
            if (fs == null) continue;
            for (File f : fs) {
                term.append(getString(R.string.term_result_prefix) + s.getName() + "/" + f.getName() + "\n", TerminalView.COLOR_NORMAL);
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
            term.append(getString(R.string.term_log_cleared) + "\n", TerminalView.COLOR_OK);
        } else if (cmd.equals("clean audio")) {
            delete(cache);
            term.append(getString(R.string.term_audio_cleared) + "\n", TerminalView.COLOR_OK);
        } else if (cmd.equals("clean result")) {
            delete(result);
            term.append(getString(R.string.term_result_cleared) + "\n", TerminalView.COLOR_OK);
        } else {
            delete(log);
            delete(cache);
            delete(result);
            term.append(getString(R.string.term_clean_done) + "\n", TerminalView.COLOR_OK);
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
            for (File f : fs) { n++; size += f.length(); }
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