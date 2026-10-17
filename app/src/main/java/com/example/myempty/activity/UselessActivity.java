package com.example.myempty.activity2;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class UselessActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

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