package com.app.gameform.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.app.gameform.R;

public class ProfileActivity extends AppCompatActivity {
    private LinearLayout navHome, navDynamic, navAdd, navLike, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

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
        // 设置当前页面为选中状态
        setSelectedNavItem(navProfile);

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        navDynamic.setOnClickListener(v -> {
            startActivity(new Intent(this, CircleActivity.class));
            finish();
        });

        navAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, AddActivity.class));
            finish();
        });

        navLike.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationActivity.class));
            finish();
        });

        navProfile.setOnClickListener(v -> {
            // 当前页面，不做任何操作
        });
    }

    private void setSelectedNavItem(LinearLayout selectedNav) {
        // 重置所有导航项
        resetNavItems();

        // 设置选中项
        ImageView icon = (ImageView) selectedNav.getChildAt(0);
        TextView text = (TextView) selectedNav.getChildAt(1);

        icon.setColorFilter(getResources().getColor(android.R.color.black));
        text.setTextColor(getResources().getColor(android.R.color.black));
    }

    private void resetNavItems() {
        resetNavItem(navHome);
        resetNavItem(navDynamic);
        resetNavItem(navLike);
        resetNavItem(navProfile);
    }

    private void resetNavItem(LinearLayout nav) {
        ImageView icon = (ImageView) nav.getChildAt(0);
        TextView text = (TextView) nav.getChildAt(1);

        icon.setColorFilter(getResources().getColor(android.R.color.darker_gray));
        text.setTextColor(getResources().getColor(android.R.color.darker_gray));
    }
}