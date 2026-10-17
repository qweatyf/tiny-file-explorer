package com.example.myempty.activity2;

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

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GifMakeActivity extends AppCompatActivity {

    private final List<Uri> picked = new ArrayList<>();
    private Uri videoUri;
    private TextView tvList;
    private EditText etDelay, etLoop, etW, etH;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private int mode = 0;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

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
    }

    private void pickMode() {
        String[] ops = {getString(R.string.gif_pick_images),
            getString(R.string.gif_pick_video)};
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
                for (int i = 0; i < n; i++) picked.add(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) {
                picked.add(data.getData());
            }
            tvList.setText(picked.size() + "");
        } else if (req == 101) {
            videoUri = data.getData();
            tvList.setText(videoUri.toString());
        }
    }

    private void makeGif() {
        if (mode == 0) {
            if (picked.size() < 2) {
                Toast.makeText(this, getString(R.string.file_enc_none),
                    Toast.LENGTH_SHORT).show();
                return;
            }
            makeFromImages();
        } else {
            if (videoUri == null) {
                Toast.makeText(this, getString(R.string.file_enc_none),
                    Toast.LENGTH_SHORT).show();
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
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();
                    if (bmp != null) frames.add(bmp);
                }
                if (frames.size() < 2) {
                    ui.post(() -> Toast.makeText(this,
                        getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show());
                    return;
                }
                encodeGif(frames, fDelay, fLoop, w, h);
            } catch (Throwable e) {
                ui.post(() -> Toast.makeText(this,
                    e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void askVideoParams() {
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(40, 20, 40, 0);

        final EditText etStart = new EditText(this);
        etStart.setHint(getString(R.string.gif_start));
        etStart.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        box.addView(etStart);

        final EditText etEnd = new EditText(this);
        etEnd.setHint(getString(R.string.gif_end));
        etEnd.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        box.addView(etEnd);

        final EditText etFps = new EditText(this);
        etFps.setHint(getString(R.string.gif_fps));
        etFps.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        box.addView(etFps);

        final EditText etGifDelay = new EditText(this);
        etGifDelay.setHint(getString(R.string.gif_delay_hint));
        etGifDelay.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        box.addView(etGifDelay);

        new AlertDialog.Builder(this)
            .setView(box)
            .setPositiveButton(getString(R.string.lang_ok), (d, w) -> {
                int start = parseInt(etStart.getText().toString(), 0);
                int end = parseInt(etEnd.getText().toString(), -1);
                int fps = parseInt(etFps.getText().toString(), 5);
                int gifDelay = parseInt(etGifDelay.getText().toString(), 200);
                videoToGif(start, end, fps, gifDelay);
            })
            .setNegativeButton(getString(R.string.file_rename_cancel), null)
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

                int stepMs = 1000 / fFps;
                List<Bitmap> frames = new ArrayList<>();
                for (int t = fromMs; t < toMs; t += stepMs) {
                    Bitmap frame = retriever.getFrameAtTime(t * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if (frame != null) frames.add(frame);
                    if (frames.size() > 200) break;
                }
                if (frames.size() < 2) {
                    ui.post(() -> Toast.makeText(this,
                        getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show());
                    return;
                }
                encodeGif(frames, fDelay, fLoop, fW, fH);
            } catch (Throwable e) {
                ui.post(() -> Toast.makeText(this,
                    e.getMessage(), Toast.LENGTH_LONG).show());
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

            File outDir = new File("/storage/emulated/0/Download/" + getString(R.string.title_gif_make));
            if (!outDir.exists()) outDir.mkdirs();
            File out = new File(outDir, "gif_" + System.currentTimeMillis() + ".gif");

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
            ui.post(() -> Toast.makeText(this,
                fout.getAbsolutePath(), Toast.LENGTH_LONG).show());
        } catch (Throwable e) {
            ui.post(() -> Toast.makeText(this,
                e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}