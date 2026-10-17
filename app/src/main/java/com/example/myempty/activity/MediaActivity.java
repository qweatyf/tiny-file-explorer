package com.example.myempty.activity2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MediaActivity extends AppCompatActivity {

    private VideoView vv;
    private LinearLayout audioBox;
    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvArtist;
    private TextView tvTime;
    private TextView tvLyric;
    private SeekBar sbProgress;
    private Button btnPlayPause;
    private LinearLayout speedBar;

    private MediaPlayer audioPlayer;
    private ParcelFileDescriptor audioFd;

    private Uri currentUri;
    private File currentFile;
    private boolean isAudio = false;
    private boolean wasPlayingBeforeSeek = false;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<long[]> lrcTimes = new ArrayList<>();
    private final List<String> lrcTexts = new ArrayList<>();
    private int lastLrcIdx = -1;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            ui.postDelayed(this, 500);
        }
    };

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        vv = findViewById(R.id.vv_media);
        audioBox = findViewById(R.id.audio_box);
        ivCover = findViewById(R.id.iv_media_cover);
        tvTitle = findViewById(R.id.tv_media_title);
        tvArtist = findViewById(R.id.tv_media_artist);
        tvTime = findViewById(R.id.tv_media_time);
        tvLyric = findViewById(R.id.tv_media_lyric);
        sbProgress = findViewById(R.id.sb_media_progress);
        btnPlayPause = findViewById(R.id.btn_media_play);
        speedBar = findViewById(R.id.speed_bar);

        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int v, boolean fromUser) {
                if (!fromUser) return;
                int dur = getDuration();
                if (dur > 0) {
                    tvTime.setText(fmtTime(v) + " / " + fmtTime(dur));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
                wasPlayingBeforeSeek = isAudio
                    ? (audioPlayer != null && audioPlayer.isPlaying())
                    : vv.isPlaying();
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
                int target = s.getProgress();
                if (isAudio) {
                    if (audioPlayer != null) audioPlayer.seekTo(target);
                } else {
                    vv.seekTo(target);
                }
                if (wasPlayingBeforeSeek) {
                    if (isAudio) {
                        if (audioPlayer != null) audioPlayer.start();
                    } else {
                        vv.start();
                    }
                }
            }
        });

        btnPlayPause.setOnClickListener(v -> togglePlay());

        findViewById(R.id.btn_pick_media).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_05).setOnClickListener(v -> setSpeed(0.5f));
        findViewById(R.id.btn_075).setOnClickListener(v -> setSpeed(0.75f));
        findViewById(R.id.btn_1).setOnClickListener(v -> setSpeed(1.0f));
        findViewById(R.id.btn_15).setOnClickListener(v -> setSpeed(1.5f));
        findViewById(R.id.btn_2).setOnClickListener(v -> setSpeed(2.0f));

        String path = getIntent().getStringExtra("path");
        if (path != null) {
            File f = new File(path);
            if (f.exists()) playFromFile(f);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) {
                Toast.makeText(this, getString(R.string.file_enc_none),
                    Toast.LENGTH_SHORT).show();
                return;
            }
            playFromUri(uri);
        }
    }

    private boolean isVideoName(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".avi")
            || n.endsWith(".3gp") || n.endsWith(".webm") || n.endsWith(".mov");
    }

    private void playFromFile(File f) {
        currentUri = Uri.fromFile(f);
        currentFile = f;
        isAudio = !isVideoName(f.getName());
        stopAll();
        if (isAudio) {
            vv.setVisibility(View.GONE);
            audioBox.setVisibility(View.VISIBLE);
            speedBar.setVisibility(View.VISIBLE);
            playAudioFromFile(f);
        } else {
            audioBox.setVisibility(View.GONE);
            vv.setVisibility(View.VISIBLE);
            speedBar.setVisibility(View.VISIBLE);
            playVideoFromUri(currentUri);
        }
    }

    private void playFromUri(Uri uri) {
        currentUri = uri;
        currentFile = null;

        String name = queryDisplayName(uri);
        if (name == null) name = uri.getLastPathSegment();
        if (name == null) name = "";

        isAudio = !isVideoName(name);
        stopAll();

        if (isAudio) {
            vv.setVisibility(View.GONE);
            audioBox.setVisibility(View.VISIBLE);
            speedBar.setVisibility(View.VISIBLE);
            playAudioFromUri(uri, name);
        } else {
            audioBox.setVisibility(View.GONE);
            vv.setVisibility(View.VISIBLE);
            speedBar.setVisibility(View.VISIBLE);
            playVideoFromUri(uri);
        }
    }

    private String queryDisplayName(Uri uri) {
        try {
            android.database.Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String s = c.getString(idx);
                    c.close();
                    return s;
                }
                c.close();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void playVideoFromUri(Uri uri) {
        try {
            sbProgress.setMax(0);
            sbProgress.setProgress(0);
            tvTime.setText("00:00 / 00:00");

            vv.setVideoURI(uri);
            vv.setOnPreparedListener(mp -> {
                mp.setLooping(false);
                int dur = mp.getDuration();
                if (dur > 0) {
                    sbProgress.setMax(dur);
                }
                tvTime.setText("00:00 / " + fmtTime(dur));
                vv.start();
                btnPlayPause.setText(getString(R.string.media_pause));

                ui.removeCallbacks(tick);
                ui.post(tick);
            });
            vv.setOnCompletionListener(mp -> {
                btnPlayPause.setText(getString(R.string.media_play));
                sbProgress.setProgress(sbProgress.getMax());
            });
            vv.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "play error: " + what + "/" + extra,
                    Toast.LENGTH_SHORT).show();
                return true;
            });
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void playAudioFromFile(File f) {
        try {
            audioPlayer = new MediaPlayer();
            audioPlayer.setDataSource(f.getAbsolutePath());
            audioPlayer.prepare();
            audioPlayer.start();
            btnPlayPause.setText(getString(R.string.media_pause));

            String title = null;
            String artist = null;
            byte[] pic = null;
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(f.getAbsolutePath());
                title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                pic = mmr.getEmbeddedPicture();
                mmr.release();
            } catch (Exception ignored) {}

            setupAudioUi(title, artist, pic, f.getName());

            int dur = audioPlayer.getDuration();
            sbProgress.setMax(dur > 0 ? dur : 0);
            sbProgress.setProgress(0);
            tvTime.setText("00:00 / " + fmtTime(dur));

            loadLyricForFile(f);

            audioPlayer.setOnCompletionListener(mp -> {
                btnPlayPause.setText(getString(R.string.media_play));
                sbProgress.setProgress(sbProgress.getMax());
            });

            ui.removeCallbacks(tick);
            ui.post(tick);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void playAudioFromUri(Uri uri, String displayName) {
        try {
            audioFd = getContentResolver().openFileDescriptor(uri, "r");
            if (audioFd == null) {
                Toast.makeText(this, getString(R.string.file_enc_none),
                    Toast.LENGTH_SHORT).show();
                return;
            }

            audioPlayer = new MediaPlayer();
            audioPlayer.setDataSource(audioFd.getFileDescriptor());
            audioPlayer.prepare();
            audioPlayer.start();
            btnPlayPause.setText(getString(R.string.media_pause));

            String title = null;
            String artist = null;
            byte[] pic = null;
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(audioFd.getFileDescriptor());
                title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                pic = mmr.getEmbeddedPicture();
                mmr.release();
            } catch (Exception ignored) {}

            setupAudioUi(title, artist, pic, displayName);

            int dur = audioPlayer.getDuration();
            sbProgress.setMax(dur > 0 ? dur : 0);
            sbProgress.setProgress(0);
            tvTime.setText("00:00 / " + fmtTime(dur));

            tvLyric.setText(getString(R.string.media_no_lyric));

            audioPlayer.setOnCompletionListener(mp -> {
                btnPlayPause.setText(getString(R.string.media_play));
                sbProgress.setProgress(sbProgress.getMax());
            });

            ui.removeCallbacks(tick);
            ui.post(tick);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupAudioUi(String title, String artist, byte[] pic, String fallbackName) {
        if (title == null || title.isEmpty()) title = fallbackName;
        if (artist == null || artist.isEmpty()) artist = getString(R.string.unspecified);

        tvTitle.setText(title);
        tvArtist.setText(artist);

        if (pic != null) {
            Bitmap b = BitmapFactory.decodeByteArray(pic, 0, pic.length);
            if (b != null) ivCover.setImageBitmap(b);
            else ivCover.setImageResource(android.R.drawable.ic_media_play);
        } else {
            ivCover.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void loadLyricForFile(File audio) {
        lrcTimes.clear();
        lrcTexts.clear();
        lastLrcIdx = -1;
        tvLyric.setText(getString(R.string.media_no_lyric));

        String base = audio.getName();
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        File lrc = new File(audio.getParent(), base + ".lrc");
        if (!lrc.exists()) return;

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(lrc), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                int start = line.indexOf('[');
                int end = line.indexOf(']');
                if (start == 0 && end > 0) {
                    String t = line.substring(start + 1, end);
                    String txt = line.substring(end + 1).trim();
                    String[] parts = t.split(":");
                    if (parts.length >= 2) {
                        try {
                            int min = Integer.parseInt(parts[0].trim());
                            String secStr = parts[1].trim();
                            String[] secParts = secStr.split("\\.");
                            int sec = Integer.parseInt(secParts[0]);
                            int ms = 0;
                            if (secParts.length > 1) {
                                String m = secParts[1];
                                while (m.length() < 3) m += "0";
                                ms = Integer.parseInt(m.substring(0, 3));
                            }
                            long total = min * 60 * 1000L + sec * 1000L + ms;
                            lrcTimes.add(new long[]{total});
                            lrcTexts.add(txt);
                        } catch (Exception ignored) {}
                    }
                }
            }
            br.close();

            if (!lrcTimes.isEmpty()) {
                tvLyric.setText("");
            }
        } catch (Exception ignored) {}
    }

    private int getDuration() {
        if (isAudio) {
            if (audioPlayer == null) return 0;
            try { return audioPlayer.getDuration(); } catch (Exception e) { return 0; }
        } else {
            try { return vv.getDuration(); } catch (Exception e) { return 0; }
        }
    }

    private void updateProgress() {
        if (isAudio) {
            if (audioPlayer == null) return;
            try {
                int pos = audioPlayer.getCurrentPosition();
                int dur = audioPlayer.getDuration();
                if (dur > 0 && sbProgress.getMax() != dur) sbProgress.setMax(dur);
                sbProgress.setProgress(pos);
                tvTime.setText(fmtTime(pos) + " / " + fmtTime(dur));
                updateLyric(pos);
            } catch (Exception ignored) {}
        } else {
            try {
                int pos = vv.getCurrentPosition();
                int dur = vv.getDuration();
                if (dur > 0 && sbProgress.getMax() != dur) sbProgress.setMax(dur);
                sbProgress.setProgress(pos);
                tvTime.setText(fmtTime(pos) + " / " + fmtTime(dur));
            } catch (Exception ignored) {}
        }
    }

    private void updateLyric(int pos) {
        if (lrcTimes.isEmpty()) return;
        int idx = -1;
        for (int i = 0; i < lrcTimes.size(); i++) {
            if (pos >= lrcTimes.get(i)[0]) idx = i;
            else break;
        }
        if (idx != lastLrcIdx && idx >= 0) {
            lastLrcIdx = idx;
            tvLyric.setText(lrcTexts.get(idx));
        }
    }

    private void togglePlay() {
        if (isAudio) {
            if (audioPlayer == null) return;
            if (audioPlayer.isPlaying()) {
                audioPlayer.pause();
                btnPlayPause.setText(getString(R.string.media_play));
            } else {
                audioPlayer.start();
                btnPlayPause.setText(getString(R.string.media_pause));
            }
        } else {
            if (vv.isPlaying()) {
                vv.pause();
                btnPlayPause.setText(getString(R.string.media_play));
            } else {
                vv.start();
                btnPlayPause.setText(getString(R.string.media_pause));
            }
        }
    }

    private void seekTo(int ms) {
        if (isAudio) {
            if (audioPlayer != null) audioPlayer.seekTo(ms);
        } else {
            vv.seekTo(ms);
        }
    }

    private void setSpeed(float speed) {
        if (isAudio) {
            if (audioPlayer == null) return;
            try {
                android.media.PlaybackParams params = audioPlayer.getPlaybackParams();
                params.setSpeed(speed);
                audioPlayer.setPlaybackParams(params);
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            try {
                java.lang.reflect.Field f = VideoView.class.getDeclaredField("mMediaPlayer");
                f.setAccessible(true);
                MediaPlayer mp = (MediaPlayer) f.get(vv);
                if (mp == null) return;
                android.media.PlaybackParams params = mp.getPlaybackParams();
                params.setSpeed(speed);
                mp.setPlaybackParams(params);
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String fmtTime(int ms) {
        if (ms < 0) ms = 0;
        int total = ms / 1000;
        int m = total / 60;
        int s = total % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    private void stopAll() {
        ui.removeCallbacks(tick);
        try {
            if (audioPlayer != null) {
                audioPlayer.stop();
                audioPlayer.release();
                audioPlayer = null;
            }
        } catch (Exception ignored) {}
        try {
            if (audioFd != null) {
                audioFd.close();
                audioFd = null;
            }
        } catch (Exception ignored) {}
        try {
            if (vv.isPlaying()) vv.stopPlayback();
        } catch (Exception ignored) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isAudio && audioPlayer != null && audioPlayer.isPlaying()) {
            audioPlayer.pause();
            btnPlayPause.setText(getString(R.string.media_play));
        }
    }

    @Override
    protected void onDestroy() {
        stopAll();
        super.onDestroy();
    }
}