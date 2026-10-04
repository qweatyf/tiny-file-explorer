package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GifMakeActivity extends Activity {

    private final List<Uri> picked = new ArrayList<>();
    private Uri videoUri;
    private TextView tvList;
    private EditText etDelay, etLoop, etW, etH;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private int mode = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_gif_make);

        tvList = findViewById(R.id.tv_gif_list);
        etDelay = findViewById(R.id.et_gif_delay);
        etLoop = findViewById(R.id.et_gif_loop);
        etW = findViewById(R.id.et_gif_w);
        etH = findViewById(R.id.et_gif_h);

        etDelay.setText("500");
        etLoop.setText("0");
        etW.setText("0");
        etH.setText("0");

        findViewById(R.id.btn_gif_pick).setOnClickListener(v -> pickMode());
        findViewById(R.id.btn_gif_make).setOnClickListener(v -> makeGif());

        tvList.setText("点上面按钮选图片或视频");
    }

    private void pickMode() {
        String[] ops = {"选图片（多选）", "选视频（抽帧）"};
        new AlertDialog.Builder(this)
            .setItems(ops, (d, w) -> {
                mode = w;
                if (w == 0) {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.setType("image/*");
                    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    startActivityForResult(i, 100);
                } else {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.setType("video/*");
                    startActivityForResult(i, 101);
                }
            })
            .show();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null) return;

        if (req == 100) {
            picked.clear();
            if (data.getClipData() != null) {
                int n = data.getClipData().getItemCount();
                for (int i = 0; i < n; i++) {
                    picked.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                picked.add(data.getData());
            }
            tvList.setText("已选 " + picked.size() + " 张图");
        } else if (req == 101) {
            videoUri = data.getData();
            tvList.setText("已选视频: " + videoUri);
        }
    }

    private void makeGif() {
        if (mode == 0) {
            if (picked.size() < 2) {
                Toast.makeText(this, "至少选两张图", Toast.LENGTH_SHORT).show();
                return;
            }
            makeFromImages();
        } else {
            if (videoUri == null) {
                Toast.makeText(this, "先选视频", Toast.LENGTH_SHORT).show();
                return;
            }
            askVideoParams();
        }
    }

    private void makeFromImages() {
        int delay = parseInt(etDelay.getText().toString(), 500);
        int loop = parseInt(etLoop.getText().toString(), 0);
        int w = parseInt(etW.getText().toString(), 0);
        int h = parseInt(etH.getText().toString(), 0);

        final int fDelay = Math.max(50, delay);
        final int fLoop = Math.max(0, loop);

        new Thread(() -> {
            try {
                List<Bitmap> frames = new ArrayList<>();
                for (Uri u : picked) {
                    InputStream is = getContentResolver().openInputStream(u);
                    Bitmap b = BitmapFactory.decodeStream(is);
                    is.close();
                    if (b != null) frames.add(b);
                }
                if (frames.size() < 2) {
                    ui.post(() -> Toast.makeText(this,
                        "有效图片不到两张", Toast.LENGTH_SHORT).show());
                    return;
                }
                encodeGif(frames, fDelay, fLoop, w, h);
            } catch (Throwable e) {
                ui.post(() -> Toast.makeText(this,
                    "合成失败: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void askVideoParams() {
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(40, 20, 40, 0);

        final EditText etStart = new EditText(this);
        etStart.setHint("起始秒（默认 0）");
        etStart.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        box.addView(etStart);

        final EditText etEnd = new EditText(this);
        etEnd.setHint("结束秒（默认视频全长）");
        etEnd.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        box.addView(etEnd);

        final EditText etFps = new EditText(this);
        etFps.setHint("每秒抽几帧（默认 5）");
        etFps.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        box.addView(etFps);

        final EditText etGifDelay = new EditText(this);
        etGifDelay.setHint("GIF 每帧停留毫秒（默认 200）");
        etGifDelay.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        box.addView(etGifDelay);

        new AlertDialog.Builder(this)
            .setTitle("视频转 GIF 参数")
            .setView(box)
            .setPositiveButton("开始", (d, w) -> {
                int start = parseInt(etStart.getText().toString(), 0);
                int end = parseInt(etEnd.getText().toString(), -1);
                int fps = parseInt(etFps.getText().toString(), 5);
                int gifDelay = parseInt(etGifDelay.getText().toString(), 200);
                videoToGif(start, end, fps, gifDelay);
            })
            .setNegativeButton("算了", null)
            .show();
    }

    private void videoToGif(final int startSec, final int endSec,
                            final int fps, final int gifDelay) {
        final int fFps = Math.max(1, Math.min(30, fps));
        final int fDelay = Math.max(50, gifDelay);
        final int fLoop = Math.max(0, parseInt(etLoop.getText().toString(), 0));
        final int fW = parseInt(etW.getText().toString(), 0);
        final int fH = parseInt(etH.getText().toString(), 0);

        new Thread(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(this, videoUri);

                int durationMs = 0;
                try {
                    String d = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION);
                    durationMs = Integer.parseInt(d);
                } catch (Exception ignored) {}

                int fromMs = startSec * 1000;
                int toMs = endSec < 0 ? durationMs : endSec * 1000;
                if (toMs > durationMs) toMs = durationMs;
                if (toMs <= fromMs) {
                    ui.post(() -> Toast.makeText(this,
                        "结束时间要比起始大", Toast.LENGTH_SHORT).show());
                    return;
                }

                int stepMs = 1000 / fFps;
                List<Bitmap> frames = new ArrayList<>();
                for (int t = fromMs; t < toMs; t += stepMs) {
                    Bitmap frame = retriever.getFrameAtTime(
                        t * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if (frame != null) {
                        frames.add(frame);
                    }
                    if (frames.size() > 200) break;
                }

                if (frames.size() < 2) {
                    ui.post(() -> Toast.makeText(this,
                        "抽不出帧，可能视频读不了", Toast.LENGTH_SHORT).show());
                    return;
                }

                encodeGif(frames, fDelay, fLoop, fW, fH);
            } catch (Throwable e) {
                ui.post(() -> Toast.makeText(this,
                    "视频转 GIF 失败: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            } finally {
                try { retriever.release(); } catch (Exception ignored) {}
            }
        }).start();
    }

    private void encodeGif(List<Bitmap> frames, int delay, int loop, int w, int h) {
        try {
            int tw = w, th = h;
            if (tw <= 0 || th <= 0) {
                tw = frames.get(0).getWidth();
                th = frames.get(0).getHeight();
            }

            File outDir = new File("/storage/emulated/0/Download/GIF动图");
            if (!outDir.exists()) outDir.mkdirs();
            File out = new File(outDir,
                "gif_" + System.currentTimeMillis() + ".gif");

            AnimatedGifEncoder enc = new AnimatedGifEncoder();
            enc.start(new FileOutputStream(out));
            enc.setDelay(delay);
            enc.setRepeat(loop);
            enc.setQuality(10);

            for (Bitmap b : frames) {
                Bitmap use = b;
                if (b.getWidth() != tw || b.getHeight() != th) {
                    use = Bitmap.createScaledBitmap(b, tw, th, true);
                }
                enc.addFrame(use);
                if (use != b) use.recycle();
                b.recycle();
            }
            enc.finish();

            final File fout = out;
            final int n = frames.size();
            ui.post(() -> Toast.makeText(this,
                "合成好了，共 " + n + " 帧\n" + fout.getAbsolutePath(),
                Toast.LENGTH_LONG).show());
        } catch (Throwable e) {
            ui.post(() -> Toast.makeText(this,
                "合成失败: " + e.getMessage(),
                Toast.LENGTH_LONG).show());
        }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}