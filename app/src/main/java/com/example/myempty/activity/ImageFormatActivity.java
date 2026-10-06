package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageFormatActivity extends Activity {

    private static final int MAX_PICK = 100;

    private GridView gvPreview;
    private TextView tvStatus;
    private final List<Uri> picked = new ArrayList<>();
    private PreviewAdapter adapter;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean converting = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_image_format);

        gvPreview = findViewById(R.id.gv_fmt_preview);
        tvStatus = findViewById(R.id.tv_fmt_status);

        adapter = new PreviewAdapter();
        gvPreview.setAdapter(adapter);

        findViewById(R.id.btn_fmt_pick).setOnClickListener(v -> pickImages());
        findViewById(R.id.btn_fmt_do).setOnClickListener(v -> pickFormat());

        tvStatus.setText("还没选图");
    }

    private void pickImages() {
        if (converting) {
            Toast.makeText(this, "正在转换，等会儿", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(i, "选图片"), 100);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != 100 || res != RESULT_OK || data == null) return;

        List<Uri> list = new ArrayList<>();
        if (data.getClipData() != null) {
            int n = data.getClipData().getItemCount();
            for (int i = 0; i < n; i++) {
                list.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            list.add(data.getData());
        }

        if (list.isEmpty()) {
            Toast.makeText(this, "没选到图", Toast.LENGTH_SHORT).show();
            return;
        }

        picked.clear();
        boolean truncated = false;
        if (list.size() > MAX_PICK) {
            picked.addAll(list.subList(0, MAX_PICK));
            truncated = true;
        } else {
            picked.addAll(list);
        }

        adapter.notifyDataSetChanged();
        tvStatus.setText("已选 " + picked.size() + " 张"
            + (truncated ? "（超 100 张已截断）" : ""));

        if (truncated) {
            Toast.makeText(this, "最多选 " + MAX_PICK + " 张，多的已忽略",
                Toast.LENGTH_LONG).show();
        }
    }

    private void pickFormat() {
        if (picked.isEmpty()) {
            Toast.makeText(this, "先选图", Toast.LENGTH_SHORT).show();
            return;
        }
        if (converting) {
            Toast.makeText(this, "正在转换，等会儿", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] fmts = {"PNG", "JPEG", "WEBP", "BMP", "GIF"};
        new AlertDialog.Builder(this)
            .setTitle("转成哪种")
            .setItems(fmts, (d, w) -> doConvert(fmts[w]))
            .show();
    }

    private void doConvert(final String fmt) {
        converting = true;
        tvStatus.setText("正在转 0/" + picked.size());

        final List<Uri> snapshot = new ArrayList<>(picked);

        new Thread(() -> {
            File outDir = new File(Environment.getExternalStorageDirectory(),
                "Download/转格式图片");
            if (!outDir.exists()) outDir.mkdirs();

            int ok = 0, fail = 0;
            for (int i = 0; i < snapshot.size(); i++) {
                Uri uri = snapshot.get(i);
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();

                    if (bmp == null) {
                        fail++;
                        continue;
                    }

                    String ext = extOf(fmt);
                    String base = "converted_" + System.currentTimeMillis()
                        + "_" + (i + 1);
                    File out = buildUnique(outDir, base + "." + ext);

                    boolean saved;
                    if ("BMP".equals(fmt)) {
                        saved = BmpEncoder.save(bmp, out.getAbsolutePath(), 100);
                    } else if ("GIF".equals(fmt)) {
                        saved = saveGif(bmp, out);
                    } else {
                        saved = saveStandard(bmp, out, fmt);
                    }
                    bmp.recycle();

                    if (saved) ok++;
                    else fail++;
                } catch (Exception e) {
                    fail++;
                }

                final int cur = i + 1;
                ui.post(() -> tvStatus.setText("正在转 " + cur + "/" + snapshot.size()));
            }

            final int fok = ok, ffail = fail;
            ui.post(() -> {
                converting = false;
                tvStatus.setText("转完了，成功 " + fok + " 张，失败 " + ffail + " 张\n"
                    + outDir.getAbsolutePath());
            });
        }).start();
    }

    private boolean saveStandard(Bitmap bmp, File out, String fmt) {
        try {
            FileOutputStream fos = new FileOutputStream(out);
            Bitmap.CompressFormat cf;
            if ("PNG".equals(fmt)) cf = Bitmap.CompressFormat.PNG;
            else if ("WEBP".equals(fmt)) cf = Bitmap.CompressFormat.WEBP;
            else cf = Bitmap.CompressFormat.JPEG;
            bmp.compress(cf, 95, fos);
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean saveGif(Bitmap bmp, File out) {
        try {
            AnimatedGifEncoder enc = new AnimatedGifEncoder();
            enc.start(new FileOutputStream(out));
            enc.setDelay(500);
            enc.setRepeat(0);
            enc.setQuality(10);
            enc.addFrame(bmp);
            enc.finish();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String extOf(String fmt) {
        if ("JPEG".equals(fmt)) return "jpg";
        return fmt.toLowerCase();
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

    class PreviewAdapter extends BaseAdapter {
        @Override public int getCount() { return picked.size(); }
        @Override public Uri getItem(int i) { return picked.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, android.view.ViewGroup parent) {
            ImageView iv;
            if (convert instanceof ImageView) {
                iv = (ImageView) convert;
            } else {
                iv = new ImageView(ImageFormatActivity.this);
                int size = (int) (getResources().getDisplayMetrics().density * 110);
                iv.setLayoutParams(new android.widget.AbsListView.LayoutParams(size, size));
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setPadding(4, 4, 4, 4);
            }
            try {
                iv.setImageURI(getItem(pos));
            } catch (Exception ignored) {}
            return iv;
        }
    }
}