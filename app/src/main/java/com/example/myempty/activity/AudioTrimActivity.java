package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AudioTrimActivity extends Activity {

    private Uri wavUri;
    private TextView tvInfo;
    private EditText etStart, etEnd;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_audio_trim);

        tvInfo = findViewById(R.id.tv_at_info);
        etStart = findViewById(R.id.et_at_start);
        etEnd = findViewById(R.id.et_at_end);
        etStart.setText("0");
        etEnd.setText("5");

        findViewById(R.id.btn_at_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("audio/*");
            startActivityForResult(i, 100);
        });

        findViewById(R.id.btn_at_do).setOnClickListener(v -> doTrim());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            wavUri = data.getData();
            tvInfo.setText("已选: " + wavUri);
        }
    }

    private void doTrim() {
        if (wavUri == null) {
            Toast.makeText(this, "先选 WAV", Toast.LENGTH_SHORT).show();
            return;
        }
        int start = parseInt(etStart.getText().toString(), 0);
        int end = parseInt(etEnd.getText().toString(), 5);
        if (end <= start) {
            Toast.makeText(this, "结束要比起始大", Toast.LENGTH_SHORT).show();
            return;
        }

        final int fs = start, fe = end;
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(wavUri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                is.close();
                byte[] raw = bos.toByteArray();

                if (raw.length < 44) {
                    runOnUiThread(() -> Toast.makeText(this,
                        "文件太小", Toast.LENGTH_SHORT).show());
                    return;
                }
                if (!(raw[0] == 'R' && raw[1] == 'I' && raw[2] == 'F' && raw[3] == 'F')) {
                    runOnUiThread(() -> Toast.makeText(this,
                        "只支持 WAV", Toast.LENGTH_SHORT).show());
                    return;
                }

                int sampleRate = ((raw[24] & 0xFF)) | ((raw[25] & 0xFF) << 8)
                    | ((raw[26] & 0xFF) << 16) | ((raw[27] & 0xFF) << 24);
                int channels = (raw[22] & 0xFF) | ((raw[23] & 0xFF) << 8);
                int bitsPerSample = (raw[34] & 0xFF) | ((raw[35] & 0xFF) << 8);

                if (sampleRate <= 0) sampleRate = 8000;
                if (channels <= 0) channels = 1;
                if (bitsPerSample <= 0) bitsPerSample = 16;

                int byteRate = sampleRate * channels * bitsPerSample / 8;
                int startByte = 44 + fs * byteRate;
                int endByte = 44 + fe * byteRate;
                if (startByte >= raw.length) startByte = 44;
                if (endByte > raw.length) endByte = raw.length;
                if (endByte <= startByte) {
                    runOnUiThread(() -> Toast.makeText(this,
                        "范围超了", Toast.LENGTH_SHORT).show());
                    return;
                }

                int dataLen = endByte - startByte;
                File dir = new File("/storage/emulated/0/Download/裁剪音频");
                if (!dir.exists()) dir.mkdirs();
                File out = new File(dir, "trim_" + System.currentTimeMillis() + ".wav");
                FileOutputStream fos = new FileOutputStream(out);

                fos.write("RIFF".getBytes());
                writeInt(fos, 36 + dataLen);
                fos.write("WAVE".getBytes());
                fos.write("fmt ".getBytes());
                writeInt(fos, 16);
                writeShort(fos, (short) 1);
                writeShort(fos, (short) channels);
                writeInt(fos, sampleRate);
                writeInt(fos, byteRate);
                writeShort(fos, (short) (channels * bitsPerSample / 8));
                writeShort(fos, (short) bitsPerSample);
                fos.write("data".getBytes());
                writeInt(fos, dataLen);
                fos.write(raw, startByte, dataLen);
                fos.close();

                runOnUiThread(() -> Toast.makeText(this,
                    "裁好了: " + out.getAbsolutePath(),
                    Toast.LENGTH_LONG).show());
            } catch (Throwable e) {
                runOnUiThread(() -> Toast.makeText(this,
                    "裁失败: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void writeInt(FileOutputStream fos, int v) throws Exception {
        fos.write(v & 0xff);
        fos.write((v >> 8) & 0xff);
        fos.write((v >> 16) & 0xff);
        fos.write((v >> 24) & 0xff);
    }

    private void writeShort(FileOutputStream fos, short v) throws Exception {
        fos.write(v & 0xff);
        fos.write((v >> 8) & 0xff);
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}