package com.example.myempty.activity2;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

public class AudioDecoder {

    public static class Result {
        public boolean ok;
        public String morse;
        public String message;
    }

    private static final float FREQ_LOW = 400f;
    private static final float FREQ_HIGH = 1500f;
    private static final int DOT_MS = 100;
    private static final int DASH_MS = 300;
    private static final int MIN_TONE_MS = 30;

    public static Result decode(Context ctx, Uri uri) {
        Result r = new Result();
        try {
            short[] pcm = readPcm(ctx, uri);
            if (pcm == null || pcm.length == 0) {
                r.ok = false;
                r.message = "识别失败，音频里可能有杂声";
                return r;
            }

            int sr = guessSampleRate(pcm.length);
            String morse = pcmToMorse(pcm, sr);
            if (morse == null || morse.trim().isEmpty()) {
                r.ok = false;
                r.message = "识别失败，音频里可能有杂声";
                return r;
            }
            r.ok = true;
            r.morse = morse.trim();
            r.message = "可能识别不准";
            return r;
        } catch (Exception e) {
            r.ok = false;
            r.message = "识别失败：" + e.getMessage();
            return r;
        }
    }

    private static int guessSampleRate(int len) {
        return 8000;
    }

    private static short[] readPcm(Context ctx, Uri uri) throws Exception {
        String name = uri.toString().toLowerCase();
        if (name.endsWith(".wav")) {
            try {
                short[] w = readWav(ctx, uri);
                if (w != null && w.length > 0) return w;
            } catch (Exception ignored) {}
        }
        return readByCodec(ctx, uri);
    }

    private static File copyToCache(Context ctx, Uri uri) throws Exception {
        File tmp = new File(ctx.getCacheDir(),
            "ad_" + System.currentTimeMillis() + ".tmp");
        InputStream is = ctx.getContentResolver().openInputStream(uri);
        FileOutputStream fos = new FileOutputStream(tmp);
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
        is.close();
        fos.close();
        return tmp;
    }

    private static short[] readWav(Context ctx, Uri uri) throws Exception {
        InputStream is = ctx.getContentResolver().openInputStream(uri);
        if (is == null) return null;

        byte[] head = new byte[44];
        int n = 0;
        while (n < 44) {
            int k = is.read(head, n, 44 - n);
            if (k < 0) break;
            n += k;
        }
        if (n < 44) { is.close(); return null; }

        if (!(head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F')) {
            is.close();
            return null;
        }

        int channels = ((head[22] & 0xFF)) | ((head[23] & 0xFF) << 8);
        int sampleRate = ((head[24] & 0xFF))
            | ((head[25] & 0xFF) << 8)
            | ((head[26] & 0xFF) << 16)
            | ((head[27] & 0xFF) << 24);
        int bits = ((head[34] & 0xFF)) | ((head[35] & 0xFF) << 8);

        int dataLen = ((head[40] & 0xFF))
            | ((head[41] & 0xFF) << 8)
            | ((head[42] & 0xFF) << 16)
            | ((head[43] & 0xFF) << 24);

        if (dataLen <= 0) {
            is.close();
            return null;
        }

        byte[] data = new byte[dataLen];
        int read = 0;
        while (read < dataLen) {
            int k = is.read(data, read, dataLen - read);
            if (k < 0) break;
            read += k;
        }
        is.close();

        if (channels <= 0) channels = 1;
        if (bits <= 0) bits = 16;

        short[] pcm;

        if (bits == 8) {
            int samples = read / channels;
            pcm = new short[samples];
            int idx = 0;
            for (int i = 0; i < samples; i++) {
                int sum = 0;
                for (int c = 0; c < channels; c++) {
                    int v = (data[idx++] & 0xFF) - 128;
                    sum += v;
                }
                int v = sum / channels;
                pcm[i] = (short) (v * 256);
            }
        } else if (bits == 16) {
            ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            ShortBuffer sb = bb.asShortBuffer();
            int total = sb.remaining();
            int samples = total / channels;
            pcm = new short[samples];
            for (int i = 0; i < samples; i++) {
                int sum = 0;
                for (int c = 0; c < channels; c++) sum += sb.get();
                pcm[i] = (short) (sum / channels);
            }
        } else if (bits == 24) {
            int bytes = bits / 8;
            int samples = read / (bytes * channels);
            pcm = new short[samples];
            for (int i = 0; i < samples; i++) {
                int sum = 0;
                for (int c = 0; c < channels; c++) {
                    int base = (i * channels + c) * bytes;
                    int v = ((data[base] & 0xFF))
                        | ((data[base + 1] & 0xFF) << 8)
                        | ((data[base + 2] & 0xFF) << 16);
                    if ((v & 0x800000) != 0) v |= 0xFF000000;
                    sum += v >> 8;
                }
                pcm[i] = (short) (sum / channels);
            }
        } else if (bits == 32) {
            ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            int total = bb.remaining() / 4;
            int samples = total / channels;
            pcm = new short[samples];
            for (int i = 0; i < samples; i++) {
                long sum = 0;
                for (int c = 0; c < channels; c++) sum += bb.getInt();
                pcm[i] = (short) (sum / channels);
            }
        } else {
            ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            ShortBuffer sb = bb.asShortBuffer();
            pcm = new short[sb.remaining()];
            sb.get(pcm);
        }

        return pcm;
    }

    private static short[] readByCodec(Context ctx, Uri uri) throws Exception {
        File tmp = copyToCache(ctx, uri);
        MediaExtractor ex = new MediaExtractor();
        try {
            ex.setDataSource(tmp.getAbsolutePath());

            int track = -1;
            MediaFormat fmt = null;
            for (int i = 0; i < ex.getTrackCount(); i++) {
                MediaFormat f = ex.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    track = i;
                    fmt = f;
                    break;
                }
            }
            if (track < 0) return null;

            ex.selectTrack(track);
            String mime = fmt.getString(MediaFormat.KEY_MIME);
            MediaCodec codec = MediaCodec.createDecoderByType(mime);
            codec.configure(fmt, null, null, 0);
            codec.start();

            List<Short> out = new ArrayList<>();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean eos = false;

            while (!eos) {
                int inIdx = codec.dequeueInputBuffer(10000);
                if (inIdx >= 0) {
                    ByteBuffer buf = codec.getInputBuffer(inIdx);
                    int size = ex.readSampleData(buf, 0);
                    if (size < 0) {
                        codec.queueInputBuffer(inIdx, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        eos = true;
                    } else {
                        long t = ex.getSampleTime();
                        codec.queueInputBuffer(inIdx, 0, size, t, 0);
                        ex.advance();
                    }
                }

                int outIdx = codec.dequeueOutputBuffer(info, 10000);
                if (outIdx >= 0) {
                    ByteBuffer buf = codec.getOutputBuffer(outIdx);
                    if (buf != null && info.size > 0) {
                        buf.position(info.offset);
                        buf.limit(info.offset + info.size);
                        ShortBuffer sb = buf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                        while (sb.hasRemaining()) out.add(sb.get());
                    }
                    codec.releaseOutputBuffer(outIdx, false);
                }
            }

            codec.stop();
            codec.release();

            short[] pcm = new short[out.size()];
            for (int i = 0; i < out.size(); i++) pcm[i] = out.get(i);
            return pcm;
        } finally {
            try { ex.release(); } catch (Exception ignored) {}
            if (tmp != null) tmp.delete();
        }
    }

    private static String pcmToMorse(short[] pcm, int sampleRate) {
        int frame = sampleRate / 100;
        int total = pcm.length / frame;
        if (total <= 0) return "";

        float[] rms = new float[total];
        for (int i = 0; i < total; i++) {
            double sum = 0;
            int start = i * frame;
            int end = Math.min(start + frame, pcm.length);
            for (int j = start; j < end; j++) {
                double v = pcm[j] / 32768.0;
                sum += v * v;
            }
            rms[i] = (float) Math.sqrt(sum / Math.max(1, end - start));
        }

        float maxRms = 0;
        for (float v : rms) if (v > maxRms) maxRms = v;
        if (maxRms <= 0.0001f) return "";

        float threshold = Math.max(0.008f, maxRms * 0.25f);

        boolean[] tone = new boolean[total];
        for (int i = 0; i < total; i++) {
            tone[i] = rms[i] > threshold;
        }

        int dotFrames = DOT_MS / 10;
        int dashFrames = DASH_MS / 10;
        int minTone = MIN_TONE_MS / 10;

        StringBuilder morse = new StringBuilder();
        int i = 0;
        while (i < total) {
            if (tone[i]) {
                int start = i;
                while (i < total && tone[i]) i++;
                int len = i - start;
                if (len < minTone) continue;

                if (len < (dotFrames + dashFrames) / 2) morse.append('.');
                else morse.append('-');
            } else {
                int start = i;
                while (i < total && !tone[i]) i++;
                int gap = i - start;
                if (gap >= 50) morse.append(" / ");
                else if (gap >= 20) morse.append(' ');
            }
        }

        return morse.toString();
    }
}