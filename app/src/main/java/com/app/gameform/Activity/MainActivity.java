package com.app.gameform.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.app.gameform.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 加载动画布局

        // 延迟 2 秒后跳转到 HomeActivity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                finish(); // 关闭当前 MainActivity
            }
        }, 3000); // 2000 毫秒 = 2 秒
    }
}