package com.app.gameform.Activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.app.gameform.R;
import com.app.gameform.utils.BottomNavigationHelper;

public class AddActivity extends AppCompatActivity {
    private BottomNavigationHelper bottomNavigationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        initViews();
        setupBottomNavigation();
    }

    private void initViews() {
    }

    private void setupBottomNavigation() {
        // 使用底部导航栏工具类
        bottomNavigationHelper = new BottomNavigationHelper(this, findViewById(R.id.bottomNavigationInclude));

    }
}