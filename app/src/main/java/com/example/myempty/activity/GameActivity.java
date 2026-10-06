package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class GameActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_game);

        findViewById(R.id.btn_mine).setOnClickListener(v ->
            startActivity(new Intent(this, MineGameActivity.class)));
    }
}