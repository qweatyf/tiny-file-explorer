package com.example.myempty.activity2;

import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class VideoToAudioActivity extends AppCompatActivity {

    private Uri videoUri;
    private TextView tvInfo;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_video_to_audio);

        tvInfo = findViewById(R.id.tv_vta_info);

        findViewById(R.id.btn_vta_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("video/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_vta_do).setOnClickListener(v -> extract());
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
        File tmp = new File(getCacheDir(), "vta_" + System.currentTimeMillis() + ".tmp");
        InputStream is = getContentResolver().openInputStream(uri);
        FileOutputStream fos = new FileOutputStream(tmp);
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
        is.close();
        fos.close();
        return tmp;
    }

    private void extract() {
        if (videoUri == null) {
            Toast.makeText(this, getString(R.string.file_enc_none),
                Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            MediaExtractor ex = new MediaExtractor();
            MediaMuxer mux = null;
            File tmp = null;
            try {
                tmp = copyToCache(videoUri);
                ex.setDataSource(tmp.getAbsolutePath());

                int trackIdx = -1;
                MediaFormat fmt = null;
                for (int i = 0; i < ex.getTrackCount(); i++) {
                    MediaFormat f = ex.getTrackFormat(i);
                    String mime = f.getString(MediaFormat.KEY_MIME);
                    if (mime != null && mime.startsWith("audio/")) {
                        trackIdx = i; fmt = f; break;
                    }
                }
                if (trackIdx < 0) {
                    runOnUiThread(() -> Toast.makeText(this,
                        getString(R.string.file_enc_none), Toast.LENGTH_SHORT).show());
                    return;
                }

                File dir = new File("/storage/emulated/0/Download/"
                    + getString(R.string.title_video_to_audio));
                if (!dir.exists()) dir.mkdirs();
                File out = new File(dir, "audio_" + System.currentTimeMillis() + ".m4a");

                ex.selectTrack(trackIdx);
                mux = new MediaMuxer(out.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                int outTrack = mux.addTrack(fmt);
                mux.start();

                int maxInput = 256 * 1024;
                try {
                    if (fmt.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        int v = fmt.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                        if (v > maxInput) maxInput = v;
                    }
                } catch (Exception ignored) {}

                ByteBuffer buf = ByteBuffer.allocate(maxInput);
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                while (true) {
                    int size = ex.readSampleData(buf, 0);
                    if (size < 0) break;
                    info.offset = 0;
                    info.size = size;
                    info.presentationTimeUs = ex.getSampleTime();
                    info.flags = ex.getSampleFlags();
                    mux.writeSampleData(outTrack, buf, info);
                    ex.advance();
                }
                mux.stop(); mux.release(); mux = null;
                ex.release(); ex = null;

                final File fo = out;
                runOnUiThread(() -> Toast.makeText(this,
                    fo.getAbsolutePath(), Toast.LENGTH_LONG).show());
            } catch (Throwable e) {
                runOnUiThread(() -> Toast.makeText(this,
                    e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                try { if (mux != null) { mux.stop(); mux.release(); } } catch (Exception ignored) {}
                try { if (ex != null) ex.release(); } catch (Exception ignored) {}
                if (tmp != null) tmp.delete();
            }
        }).start();
    }
}