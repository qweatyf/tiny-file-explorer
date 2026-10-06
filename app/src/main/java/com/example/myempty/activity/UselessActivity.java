package com.example.myempty.activity2;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class UselessActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_useless);

        ListView lv = findViewById(R.id.lv_useless);
        List<String> data = UselessProcData.list;
        if (data == null) data = new ArrayList<>();

        lv.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_list_item_1, data));
    }
}