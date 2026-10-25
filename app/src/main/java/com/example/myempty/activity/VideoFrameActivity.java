package com.example.myempty.activity2;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class VideoFrameActivity extends AppCompatActivity {

    private Uri videoUri;
    private TextView tvInfo;
    private EditText etSec;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_video_frame);

        tvInfo = findViewById(R.id.tv_vf_info);
        etSec = findViewById(R.id.et_vf_sec);
        etSec.setText("0");

        findViewById(R.id.btn_vf_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("video/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_vf_do).setOnClickListener(v -> doSingleFrame());
        findViewById(R.id.btn_vf_long).setOnClickListener(v -> doLongImage());
        findViewById(R.id.btn_vf_gif).setOnClickListener(v -> showGifDialog());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            videoUri = data.getData();
            tvInfo.setText(videoUri.toString());
        }
    }

    private File copyToCache(Uri uri) throws Exception {
        File tmp = new File(getCacheDir(), "vf_" + System.currentTimeMillis() + ".tmp");
        InputStream is = getContentResolver().openInputStream(uri);
        FileOutputStream fos = new FileOutputStream(tmp);
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
        is.close(); fos.close();
        return tmp;
    }

    private void doSingleFrame() {
        if (videoUri == null) { Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show(); return; }
        int sec = 0;
        try { sec = Integer.parseInt(etSec.getText().toString().trim()); } catch (Exception ignored) {}
        final int fsec = sec;

        new Thread(() -> {
            MediaMetadataRetriever r = new MediaMetadataRetriever();
            File tmp = null;
            try {
                tmp = copyToCache(videoUri);
                r.setDataSource(tmp.getAbsolutePath());
                Bitmap bmp = r.getFrameAtTime(fsec * 1000000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (bmp == null) return;
                File dir = new File("/storage/emulated/0/Download/" + getString(R.string.title_video_frame));
                dir.mkdirs();
                File f = new File(dir, "frame_" + System.currentTimeMillis() + ".png");
                FileOutputStream fos = new FileOutputStream(f);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close(); bmp.recycle();
                runOnUiThread(() -> Toast.makeText(this, f.getAbsolutePath(), Toast.LENGTH_LONG).show());
            } catch (Exception ignored) {} finally { try { r.release(); } catch (Exception ignored) {} if (tmp != null) tmp.delete(); }
        }).start();
    }

    private void doLongImage() {
        if (videoUri == null) return;
        new Thread(() -> {
            MediaMetadataRetriever r = new MediaMetadataRetriever();
            File tmp = null;
            try {
                tmp = copyToCache(videoUri);
                r.setDataSource(tmp.getAbsolutePath());
                String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long dur = Long.parseLong(d);

                List<Bitmap> frames = new ArrayList<>();
                for (int t = 0; t < dur; t += 1000) {
                    Bitmap bmp = r.getFrameAtTime(t * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if (bmp != null) frames.add(bmp);
                    if (frames.size() >= 9) break;
                }
                if (frames.isEmpty()) return;

                int w = frames.get(0).getWidth();
                int h = frames.get(0).getHeight();
                int gap = 10;
                int count = frames.size();
                Bitmap out = Bitmap.createBitmap(w, h * count + gap * (count - 1), Bitmap.Config.ARGB_8888);
                Canvas cv = new Canvas(out);
                cv.drawColor(0xFFFFFFFF);
                for (int i = 0; i < count; i++) {
                    cv.drawBitmap(frames.get(i), 0, i * (h + gap), null);
                    frames.get(i).recycle();
                }

                File dir = new File("/storage/emulated/0/Download/" + getString(R.string.video_frame_long_dir));
                dir.mkdirs();
                File f = new File(dir, "long_" + System.currentTimeMillis() + ".jpg");
                FileOutputStream fos = new FileOutputStream(f);
                out.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close(); out.recycle();
                runOnUiThread(() -> Toast.makeText(this, f.getAbsolutePath(), Toast.LENGTH_LONG).show());
            } catch (Exception ignored) {} finally { try { r.release(); } catch (Exception ignored) {} if (tmp != null) tmp.delete(); }
        }).start();
    }

    private void showGifDialog() {
        if (videoUri == null) { Toast.makeText(this, getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show(); return; }

        final EditText et = new EditText(this);
        et.setHint(getString(R.string.gif_end) + " (s)");
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_gif_make))
            .setMessage(getString(R.string.gif_select_duration))
            .setView(et)
            .setPositiveButton(getString(R.string.lang_ok), (d, w) -> {
                int limitSec = 5;
                try { limitSec = Integer.parseInt(et.getText().toString().trim()); } catch (Exception ignored) {}
                if (limitSec <= 0) limitSec = 5;
                doGif(limitSec);
            })
            .setNegativeButton(getString(R.string.file_rename_cancel), null)
            .show();
    }

    private void doGif(int maxSec) {
        final int fmax = maxSec;
        new Thread(() -> {
            MediaMetadataRetriever r = new MediaMetadataRetriever();
            File tmp = null;
            try {
                tmp = copyToCache(videoUri);
                r.setDataSource(tmp.getAbsolutePath());
                String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long dur = Long.parseLong(d);
                long maxDur = fmax * 1000L;

                File dir = new File("/storage/emulated/0/Download/" + getString(R.string.video_frame_gif_dir));
                dir.mkdirs();
                File out = new File(dir, "gif_" + System.currentTimeMillis() + ".gif");

                AnimatedGifEncoder enc = new AnimatedGifEncoder();
                enc.start(new FileOutputStream(out));
                enc.setDelay(200); enc.setRepeat(0); enc.setQuality(10);

                for (int t = 0; t < dur && t < maxDur; t += 200) {
                    Bitmap bmp = r.getFrameAtTime(t * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if (bmp != null) { enc.addFrame(bmp); bmp.recycle(); }
                }
                enc.finish();
                runOnUiThread(() -> Toast.makeText(this, out.getAbsolutePath(), Toast.LENGTH_LONG).show());
            } catch (Exception ignored) {} finally { try { r.release(); } catch (Exception ignored) {} if (tmp != null) tmp.delete(); }
        }).start();
    }
}