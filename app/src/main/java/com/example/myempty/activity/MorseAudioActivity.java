package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class MorseAudioActivity extends Activity {

    private static final int SAMPLE_RATE = 8000;
    private static final int DOT = 100;
    private static final int DASH = 300;
    private static final int GAP_INTRA = 100;
    private static final int GAP_CHAR = 300;
    private static final int GAP_WORD = 700;
    private static final int FREQ = 800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_morse_audio);

        EditText input = findViewById(R.id.et_morse_input);
        TextView result = findViewById(R.id.tv_morse_result);

        findViewById(R.id.btn_gen_morse).setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "先输点东西", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                String morse = MorseUtils.toMorse(text);
                File dir = new File(Environment.getExternalStorageDirectory(),
                    "Download/摩斯电码音频");
                if (!dir.exists()) dir.mkdirs();

                File out = buildUnique(dir, "morse.wav");

                generateWav(morse, out);
                result.setText("生成好了：\n" + out.getAbsolutePath());
            } catch (Exception e) {
                result.setText("失败了：" + e.getMessage());
            }
        });
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

    private void generateWav(String morse, File out) throws Exception {
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();

        String[] tokens = morse.trim().split("\\s+");
        boolean first = true;

        for (String token : tokens) {
            if (token.equals("/")) {
                writeSilence(pcm, GAP_WORD);
                first = true;
                continue;
            }

            if (!first) writeSilence(pcm, GAP_CHAR);
            first = false;

            for (int i = 0; i < token.length(); i++) {
                char c = token.charAt(i);
                if (c == '.') writeTone(pcm, DOT);
                else if (c == '-') writeTone(pcm, DASH);

                if (i < token.length() - 1) writeSilence(pcm, GAP_INTRA);
            }
        }

        byte[] data = pcm.toByteArray();
        FileOutputStream fos = new FileOutputStream(out);

        int dataLen = data.length;
        int fileLen = 36 + dataLen;

        fos.write("RIFF".getBytes());
        writeInt(fos, fileLen);
        fos.write("WAVE".getBytes());
        fos.write("fmt ".getBytes());
        writeInt(fos, 16);
        writeShort(fos, (short) 1);
        writeShort(fos, (short) 1);
        writeInt(fos, SAMPLE_RATE);
        writeInt(fos, SAMPLE_RATE);
        writeShort(fos, (short) 1);
        writeShort(fos, (short) 8);
        fos.write("data".getBytes());
        writeInt(fos, dataLen);
        fos.write(data);
        fos.close();
    }

    private void writeTone(ByteArrayOutputStream out, int ms) {
        int samples = SAMPLE_RATE * ms / 1000;
        for (int i = 0; i < samples; i++) {
            double angle = 2.0 * Math.PI * i * FREQ / SAMPLE_RATE;
            out.write((byte) (Math.sin(angle) * 127 + 128));
        }
    }

    private void writeSilence(ByteArrayOutputStream out, int ms) {
        int samples = SAMPLE_RATE * ms / 1000;
        for (int i = 0; i < samples; i++) out.write(128);
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
}