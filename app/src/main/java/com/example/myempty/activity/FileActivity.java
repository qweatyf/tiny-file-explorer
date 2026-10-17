package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileActivity extends AppCompatActivity {
    private static final long MAX_EDIT_SIZE = 20 * 1024 * 1024;
    private static final int EDGE_DP = 50;
    private static final float EXIT_RATIO = 0.2f;
    private static final long EXIT_RESET_MS = 4000;

    private File currentDir;
    private List<File> items = new ArrayList<>();
    private EditText etPath;
    private TextView tvExitTip;
    private Button btnUp;
    private ListView lvFiles;
    private LinearLayout barAction;

    private String actionMode = null;
    private File actionSource = null;

    private float downX, downY;
    private int edgePx;
    private int screenW;

    private int exitCount = 0;
    private final Handler exitHandler = new Handler();
    private final Runnable resetExit = new Runnable() {
        @Override
        public void run() {
            exitCount = 0;
            tvExitTip.setVisibility(TextView.GONE);
        }
    };

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);

        etPath = findViewById(R.id.et_path);
        tvExitTip = findViewById(R.id.tv_exit_tip);
        btnUp = findViewById(R.id.btn_up);
        lvFiles = findViewById(R.id.lv_files);
        barAction = findViewById(R.id.bar_action);

        edgePx = (int) (EDGE_DP * getResources().getDisplayMetrics().density);
        screenW = getResources().getDisplayMetrics().widthPixels;

        String mode = getIntent().getStringExtra("mode");
        String srcPath = getIntent().getStringExtra("src");
        if (mode != null && srcPath != null) {
            actionMode = mode;
            actionSource = new File(srcPath);
            barAction.setVisibility(LinearLayout.VISIBLE);
            findViewById(R.id.btn_confirm).setOnClickListener(v -> doAction());
            findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
        }

        String startDir = getIntent().getStringExtra("start_dir");
        if (startDir != null) {
            File sd = new File(startDir);
            if (sd.exists() && sd.isDirectory()) currentDir = sd;
            else currentDir = new File("/storage/emulated/0");
        } else {
            currentDir = new File("/storage/emulated/0");
        }
        refresh();

        btnUp.setOnClickListener(v -> goUp());

        etPath.setOnEditorActionListener((v, actionId, event) -> {
            String p = etPath.getText().toString().trim();
            if (p.isEmpty()) return true;
            File dir = new File(p);
            if (dir.exists() && dir.isDirectory()) {
                currentDir = dir;
                refresh();
            } else {
                Toast.makeText(this, getString(R.string.file_path_not_exist),
                    Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        lvFiles.setOnItemClickListener((parent, view, position, id) -> {
            if (items.isEmpty()) return;
            File f = items.get(position);
            if (f.isDirectory()) {
                currentDir = f;
                refresh();
            } else {
                if (actionMode != null) {
                    Toast.makeText(this, getString(R.string.file_pick_dir),
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                openFile(f);
            }
        });

        lvFiles.setOnItemLongClickListener((parent, view, position, id) -> {
            if (actionMode != null) return true;
            if (items.isEmpty()) return true;
            showFileMenu(items.get(position));
            return true;
        });
    }

    private void refresh() {
        etPath.setText(currentDir.getAbsolutePath());

        File parent = currentDir.getParentFile();
        if (parent == null) {
            btnUp.setAlpha(0.3f);
            btnUp.setEnabled(false);
        } else {
            btnUp.setAlpha(1f);
            btnUp.setEnabled(true);
        }

        File[] files = currentDir.listFiles();
        items.clear();
        if (files != null) {
            Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            items.addAll(Arrays.asList(files));
        }

        List<String> names = new ArrayList<>();
        if (items.isEmpty()) {
            names.add(getString(R.string.file_empty));
            names.add(getString(R.string.file_empty_hint));
        } else {
            for (File item : items) {
                names.add((item.isDirectory() ? getString(R.string.file_dir)
                    : getString(R.string.file_doc)) + item.getName());
            }
        }

        lvFiles.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_list_item_1, names));
    }

    private void goUp() {
        File parent = currentDir.getParentFile();
        if (parent == null) return;
        currentDir = parent;
        refresh();
    }

    private void openFile(File f) {
        String n = f.getName().toLowerCase();
        if (n.endsWith(".zip")) {
            Intent i = new Intent(this, ZipActivity.class);
            i.putExtra("zip_path", f.getAbsolutePath());
            startActivity(i);
            return;
        }

        if (isImage(n)) {
            Intent i = new Intent(this, ImageViewActivity.class);
            i.putExtra("path", f.getAbsolutePath());
            i.putExtra("dir", f.getParent());
            startActivity(i);
            return;
        }

        if (isVideo(n) || isAudio(n)) {
            Intent i = new Intent(this, MediaActivity.class);
            i.putExtra("path", f.getAbsolutePath());
            startActivity(i);
            return;
        }

        if (f.length() > MAX_EDIT_SIZE) {
            Toast.makeText(this, getString(R.string.file_over_size),
                Toast.LENGTH_LONG).show();
            return;
        }
        if (!isTextFile(f)) {
            Toast.makeText(this, getString(R.string.file_not_text),
                Toast.LENGTH_LONG).show();
            return;
        }

        Intent i = new Intent(this, EditorActivity.class);
        i.putExtra("file_path", f.getAbsolutePath());
        startActivity(i);
    }

    private boolean isImage(String n) {
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
            || n.endsWith(".gif") || n.endsWith(".webp") || n.endsWith(".bmp");
    }

    private boolean isVideo(String n) {
        return n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".avi")
            || n.endsWith(".3gp") || n.endsWith(".webm");
    }

    private boolean isAudio(String n) {
        return n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".ogg")
            || n.endsWith(".flac") || n.endsWith(".aac");
    }

    private boolean isTextFile(File f) {
        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] buf = new byte[4096];
            int n = fis.read(buf);
            fis.close();
            if (n > 0) {
                for (int i = 0; i < n; i++) {
                    if (buf[i] == 0) return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void showFileMenu(File f) {
        String[] opts = new String[]{
            getString(R.string.file_menu_rename),
            getString(R.string.file_menu_delete),
            getString(R.string.file_menu_copy),
            getString(R.string.file_menu_move)
        };

        new AlertDialog.Builder(this)
            .setTitle(f.getName())
            .setItems(opts, (d, which) -> {
                switch (which) {
                    case 0: renameFile(f); break;
                    case 1: deleteFile(f); break;
                    case 2: startAction("copy", f); break;
                    case 3: startAction("move", f); break;
                }
            })
            .show();
    }

    private void renameFile(File f) {
        EditText input = new EditText(this);
        input.setText(f.getName());
        input.setSelection(f.getName().length());

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.file_rename_title))
            .setView(input)
            .setPositiveButton(getString(R.string.file_rename_ok), (d, w) -> {
                String newName = input.getText().toString().trim();
                if (newName.isEmpty()) return;
                File target = new File(f.getParent(), newName);
                if (target.exists()) {
                    Toast.makeText(this, getString(R.string.file_rename_occupied),
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                if (f.renameTo(target)) refresh();
                else Toast.makeText(this, getString(R.string.file_rename_fail),
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(getString(R.string.file_rename_cancel), null)
            .show();
    }

    private void deleteFile(File f) {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.file_del_title))
            .setMessage(getString(R.string.file_del_msg))
            .setPositiveButton(getString(R.string.file_del_ok), (d, w) -> {
                if (deleteRecursive(f)) {
                    Toast.makeText(this, getString(R.string.file_del_done),
                        Toast.LENGTH_SHORT).show();
                    refresh();
                } else {
                    Toast.makeText(this, getString(R.string.file_del_fail),
                        Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(getString(R.string.file_del_cancel), null)
            .show();
    }

    private boolean deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursive(k);
        }
        return f.delete();
    }

    private void startAction(String mode, File src) {
        Intent i = new Intent(this, FileActivity.class);
        i.putExtra("mode", mode);
        i.putExtra("src", src.getAbsolutePath());
        startActivity(i);
    }

    private void doAction() {
        if (actionSource == null) return;
        File target = new File(currentDir, actionSource.getName());

        if (target.exists()) {
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.file_conflict_title))
                .setMessage(getString(R.string.file_conflict_msg))
                .setPositiveButton(getString(R.string.file_conflict_skip), (d, w) -> finish())
                .setNeutralButton(getString(R.string.file_conflict_both), (d, w) -> {
                    File newTarget = buildUnique(currentDir, actionSource.getName());
                    copyOrMove(actionSource, newTarget);
                })
                .setNegativeButton(getString(R.string.file_conflict_replace), (d, w) -> {
                    File bak = new File(target.getAbsolutePath() + ".bak");
                    target.renameTo(bak);
                    copyOrMove(actionSource, target);
                })
                .show();
        } else {
            copyOrMove(actionSource, target);
        }
    }

    private File buildUnique(File dir, String name) {
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        int i = 1;
        File f;
        do {
            f = new File(dir, base + "(" + i + ")" + ext);
            i++;
        } while (f.exists());
        return f;
    }

    private void copyOrMove(File src, File dst) {
        try {
            if (src.isDirectory()) copyDir(src, dst);
            else copyFile(src, dst);
            if ("move".equals(actionMode)) deleteRecursive(src);
            Toast.makeText(this, getString(R.string.file_done),
                Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this,
                getString(R.string.file_fail) + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }

    private void copyDir(File src, File dst) {
        if (!dst.exists()) dst.mkdirs();
        File[] kids = src.listFiles();
        if (kids == null) return;
        for (File k : kids) {
            File d = new File(dst, k.getName());
            if (k.isDirectory()) copyDir(k, d);
            else copyFile(k, d);
        }
    }

    private void copyFile(File src, File dst) {
        try {
            InputStream is = new FileInputStream(src);
            OutputStream os = new FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
            is.close();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (downX <= edgePx && dx > 0 && Math.abs(dy) < Math.abs(dx)) {
                    float ratio = dx / screenW;
                    if (ratio >= EXIT_RATIO) handleExitSwipe();
                    else goUp();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleExitSwipe() {
        exitCount++;
        exitHandler.removeCallbacks(resetExit);

        if (exitCount >= 3) {
            exitCount = 0;
            tvExitTip.setVisibility(TextView.GONE);
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
            return;
        }

        tvExitTip.setText(exitCount == 1
            ? getString(R.string.file_exit_tip1)
            : getString(R.string.file_exit_tip2));
        tvExitTip.setVisibility(TextView.VISIBLE);

        exitHandler.postDelayed(resetExit, EXIT_RESET_MS);
    }

    @Override
    protected void onDestroy() {
        exitHandler.removeCallbacks(resetExit);
        super.onDestroy();
    }
}