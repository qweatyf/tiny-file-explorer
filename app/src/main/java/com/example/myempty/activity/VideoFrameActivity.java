package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class VideoFrameActivity extends Activity {

    private Uri videoUri;
    private TextView tvInfo;
    private EditText etSec;

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

        findViewById(R.id.btn_vf_do).setOnClickListener(v -> doFrame());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            videoUri = data.getData();
            tvInfo.setText("已选: " + videoUri);
        }
    }

    private File copyToCache(Uri uri) throws Exception {
        File tmp = new File(getCacheDir(),
            "vf_" + System.currentTimeMillis() + ".tmp");
        InputStream is = getContentResolver().openInputStream(uri);
        FileOutputStream fos = new FileOutputStream(tmp);
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
        is.close();
        fos.close();
        return tmp;
    }

    private void doFrame() {
        if (videoUri == null) {
            Toast.makeText(this, "先选视频", Toast.LENGTH_SHORT).show();
            return;
        }
        int sec = 0;
        try { sec = Integer.parseInt(etSec.getText().toString().trim()); }
        catch (Exception ignored) {}

        final int fsec = sec;
        new Thread(() -> {
            MediaMetadataRetriever r = new MediaMetadataRetriever();
            File tmp = null;
            try {
                tmp = copyToCache(videoUri);
                r.setDataSource(tmp.getAbsolutePath());

                Bitmap bmp = r.getFrameAtTime(fsec * 1000000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (bmp == null) {
                    runOnUiThread(() -> Toast.makeText(this,
                        "抽不出帧", Toast.LENGTH_SHORT).show());
                    return;
                }

                File dir = new File("/storage/emulated/0/Download/视频帧");
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, "frame_" + System.currentTimeMillis() + ".png");
                FileOutputStream fos = new FileOutputStream(f);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                bmp.recycle();

                runOnUiThread(() -> Toast.makeText(this,
                    "抽好了: " + f.getAbsolutePath(),
                    Toast.LENGTH_LONG).show());
            } catch (Throwable e) {
                runOnUiThread(() -> Toast.makeText(this,
                    "失败: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            } finally {
                try { r.release(); } catch (Exception ignored) {}
                if (tmp != null) tmp.delete();
            }
        }).start();
    }
}