package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class LinksActivity extends Activity {
    private final String[] names = {
        "百度", "QQ", "微信", "B站", "淘宝"
    };
    private final String[] urls = {
        "https://www.baidu.com",
        "https://im.qq.com",
        "https://weixin.qq.com",
        "https://www.bilibili.com",
        "https://www.taobao.com"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_links);

        ListView lv = findViewById(R.id.lv_links);
        lv.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_list_item_1, names));

        lv.setOnItemClickListener((parent, view, position, id) -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(urls[position]));
            startActivity(i);
        });
    }
}