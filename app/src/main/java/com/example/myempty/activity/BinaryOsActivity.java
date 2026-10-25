package com.example.myempty.activity2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class BinaryOsActivity extends AppCompatActivity {

    static {
        System.loadLibrary("c++_shared");
        System.loadLibrary("binaryos");
    }

    private static final int REQ_STORAGE = 2011;

    private EditText etCode;
    private TextView tvConsole;
    private ScrollView svTerminal;
    private ImageView ivScreen;
    private View layoutTerminal;
    private Button btnRun;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;
    private volatile boolean videoMode = false;
    private Thread runThread = null;

    private Bitmap screenBitmap = null;
    private int screenW = 640;
    private int screenH = 360;
    private int[] osPixels = null;

    private final Runnable renderTask = new Runnable() {
        @Override
        public void run() {
            if (!videoMode) return;
            if (osPixels == null || osPixels.length != screenW * screenH) {
                osPixels = new int[screenW * screenH];
            }
            boolean ok = nativeGetPixels(osPixels);
            if (ok && screenBitmap != null && !screenBitmap.isRecycled()) {
                screenBitmap.setPixels(osPixels, 0, screenW, 0, 0, screenW, screenH);
                ivScreen.invalidate();
            }
            ui.postDelayed(this, 16);
        }
    };

    private native void nativeInit();
    private native void nativeSetPaths(String c, String d);
    private native void nativeSetResolution(int w, int h);
    private native void nativeClear();
    private native void nativeTouch(int x, int y, boolean pressed, int pointerCount);
    private native void nativeClearTouch();
    private native void nativeAbort();
    private native boolean nativeRunAsm(String code, int watchdog);
    private native boolean nativeGetPixels(int[] out);
    private native void nativeLoadRegistry(String path);
    private native void nativeFlushRegistry();
    private native void nativeStartDesktop();

    public void onNativePushText(int x, int y, String text, int color, int size) {
    }

    public void onNativeRequestKeyboard() {
        ui.post(() -> {
            if (etCode == null) return;
            etCode.requestFocus();
            InputMethodManager im = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if (im != null) im.showSoftInput(etCode, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    public void onNativeShutdown() {
        ui.post(() -> finish());
    }

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_binary_os);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int sw = Math.min(dm.widthPixels, dm.heightPixels);
        int sh = Math.max(dm.widthPixels, dm.heightPixels);
        screenW = sw / 2;
        screenH = sh / 2;
        if (screenW < 480) screenW = 480;
        if (screenH < 270) screenH = 270;
        osPixels = new int[screenW * screenH];

        nativeInit();

        String cPath = getFilesDir().getAbsolutePath();
        String dPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        nativeSetPaths(cPath, dPath);
        nativeLoadRegistry(cPath + "/registry.dat");
        nativeSetResolution(screenW, screenH);

        etCode = findViewById(R.id.et_code);
        tvConsole = findViewById(R.id.tv_console);
        svTerminal = findViewById(R.id.sv_terminal);
        ivScreen = findViewById(R.id.iv_screen);
        layoutTerminal = findViewById(R.id.layout_terminal);
        btnRun = findViewById(R.id.btn_run);

        screenBitmap = Bitmap.createBitmap(screenW, screenH, Bitmap.Config.ARGB_8888);
        ivScreen.setImageBitmap(screenBitmap);
        ivScreen.setScaleType(ImageView.ScaleType.FIT_XY);
        ivScreen.setFocusableInTouchMode(true);

        ivScreen.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN
                    || action == MotionEvent.ACTION_MOVE) {
                int vw = v.getWidth();
                int vh = v.getHeight();
                if (vw <= 0 || vh <= 0) return true;
                int x = (int)(event.getX() / vw * screenW);
                int y = (int)(event.getY() / vh * screenH);
                if (x < 0) x = 0;
                if (x >= screenW) x = screenW - 1;
                if (y < 0) y = 0;
                if (y >= screenH) y = screenH - 1;
                int count = event.getPointerCount() >= 2 ? 2 : 1;
                nativeTouch(x, y, true, count);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP
                    || action == MotionEvent.ACTION_CANCEL) {
                nativeClearTouch();
            }
            return true;
        });

        etCode.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etCode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                executeCode();
                return true;
            }
            return false;
        });

        btnRun.setOnClickListener(v -> executeCode());

        requestStoragePermission();
        showDelayDialog();
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent it = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    it.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(it);
                } catch (Exception e) {
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                    } catch (Exception ignored) { }
                }
            }
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) return;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQ_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == REQ_STORAGE) {
            boolean ok = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
            if (!ok) printToConsole("Storage permission denied", 0xFFFF2222);
        }
    }

    private void showDelayDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_os_delay, null);
        EditText etDelay = view.findViewById(R.id.et_os_delay);
        CheckBox cbUnlimited = view.findViewById(R.id.cb_os_unlimited);
        Button btnAgree = view.findViewById(R.id.btn_os_agree);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnAgree.setOnClickListener(v -> {
            if (!cbUnlimited.isChecked()) {
                try { Integer.parseInt(etDelay.getText().toString().trim()); } catch (Exception ignored) { }
            }
            dialog.dismiss();
            printToConsole("BinaryOS Booted.", 0xFF00AAFF);
            printToConsole("Enter BOOT to start desktop.", 0xFF00AAFF);
            etCode.requestFocus();
            InputMethodManager im = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if (im != null) im.showSoftInput(etCode, InputMethodManager.SHOW_IMPLICIT);
        });

        dialog.show();
    }

    private void printToConsole(String text, int color) {
        ui.post(() -> {
            tvConsole.setTextColor(color);
            tvConsole.append(text + "\n");
            svTerminal.post(() -> svTerminal.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void startVideoMode() {
        videoMode = true;
        ui.post(() -> {
            layoutTerminal.setVisibility(View.GONE);
            btnRun.setVisibility(View.GONE);
            ivScreen.setVisibility(View.VISIBLE);
            ivScreen.requestFocus();
            ui.removeCallbacks(renderTask);
            ivScreen.post(() -> ui.post(renderTask));
        });
    }

    private void stopVideoMode() {
        videoMode = false;
        ui.removeCallbacks(renderTask);
        ui.post(() -> {
            layoutTerminal.setVisibility(View.VISIBLE);
            btnRun.setVisibility(View.VISIBLE);
            ivScreen.setVisibility(View.GONE);
            etCode.requestFocus();
            InputMethodManager im = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if (im != null) im.showSoftInput(etCode, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private boolean needsVideo(String code) {
        String upper = code.toUpperCase();
        return upper.contains("BOOT") || upper.contains("VIDEO_ON");
    }

    private void executeCode() {
        if (running) { nativeAbort(); return; }
        String input = etCode.getText().toString().trim();
        etCode.setText("");
        if (input.isEmpty()) {
            printToConsole("Empty", 0xFFFF2222);
            return;
        }
        running = true;
        videoMode = false;
        printToConsole("> " + input, 0xFF00AAFF);
        nativeClear();
        if (needsVideo(input)) startVideoMode();
        final String code = input;
        runThread = new Thread(() -> {
            boolean halted = nativeRunAsm(code, 0);
            boolean wasVideo = videoMode;
            running = false;
            if (wasVideo) stopVideoMode();
            else {
                ui.post(() -> {
                    etCode.requestFocus();
                    InputMethodManager im = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                    if (im != null) im.showSoftInput(etCode, InputMethodManager.SHOW_IMPLICIT);
                });
            }
            ui.post(() -> printToConsole(halted ? "HALTED" : "ERROR", halted ? 0xFF00FF66 : 0xFFFF2222));
        });
        runThread.start();
    }

    @Override
    protected void onDestroy() {
        nativeFlushRegistry();
        nativeAbort();
        Thread t = runThread;
        if (t != null) {
            try { t.join(1000); } catch (InterruptedException ignored) { }
        }
        runThread = null;
        running = false;
        videoMode = false;
        ui.removeCallbacks(renderTask);
        if (screenBitmap != null && !screenBitmap.isRecycled()) {
            screenBitmap.recycle();
            screenBitmap = null;
        }
        super.onDestroy();
    }
}