package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReceiveActivity extends Activity {
    private ListView lv;
    private final Handler handler = new Handler();
    private final List<File> files = new ArrayList<>();

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            loadFiles();
            handler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        lv = findViewById(R.id.lv_received);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            File f = files.get(position);
            openFile(f);
        });

        handler.post(poll);
    }

    private void loadFiles() {
        File dir = new File(getExternalFilesDir(null), "received");
        files.clear();
        File[] fs = dir.listFiles();
        List<String> names = new ArrayList<>();
        if (fs != null) {
            java.util.Arrays.sort(fs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (File f : fs) {
                files.add(f);
                names.add(f.getName() + "  (" + f.length() + " B)");
            }
        }
        if (names.isEmpty()) names.add("还没收到东西");
        lv.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_list_item_1, names));
    }

    private void openFile(File f) {
        String n = f.getName().toLowerCase();
        if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
            || n.endsWith(".gif") || n.endsWith(".webp") || n.endsWith(".bmp")) {
            Intent i = new Intent(this, ImageViewActivity.class);
            i.putExtra("path", f.getAbsolutePath());
            startActivity(i);
            return;
        }
        if (n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".avi")
            || n.endsWith(".mp3") || n.endsWith(".wav")) {
            Intent i = new Intent(this, MediaActivity.class);
            i.putExtra("path", f.getAbsolutePath());
            startActivity(i);
            return;
        }
        try {
            Intent i = new Intent(this, EditorActivity.class);
            i.putExtra("file_path", f.getAbsolutePath());
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "打不开", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(poll);
        super.onDestroy();
    }
}