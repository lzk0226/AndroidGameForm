package com.app.gameform.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.app.gameform.R;

public class AddActivity extends AppCompatActivity {
    private LinearLayout navHome, navDynamic, navAdd, navLike, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        initViews();
        setupBottomNavigation();
    }

    private void initViews() {
        navHome = findViewById(R.id.nav_home);
        navDynamic = findViewById(R.id.nav_dynamic);
        navAdd = findViewById(R.id.nav_add);
        navLike = findViewById(R.id.nav_like);
        navProfile = findViewById(R.id.nav_profile);
    }

    private void setupBottomNavigation() {
        // 发布页面的导航栏样式保持不变，因为中间是特殊的圆形按钮

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        navDynamic.setOnClickListener(v -> {
            startActivity(new Intent(this, CircleActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        navAdd.setOnClickListener(v -> {
            // 当前页面，不做任何操作
        });

        navLike.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }
}