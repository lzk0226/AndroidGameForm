package com.app.gameform.utils;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.app.gameform.Activity.AddActivity;
import com.app.gameform.Activity.CircleActivity;
import com.app.gameform.Activity.HomeActivity;
import com.app.gameform.Activity.NewPostActivity;
import com.app.gameform.Activity.NotificationActivity;
import com.app.gameform.Activity.ProfileActivity;
import com.app.gameform.R;

/**
 * 底部导航栏工具类
 * 用于管理底部导航栏的状态和点击事件
 */
public class BottomNavigationHelper {

    public enum NavigationItem {
        HOME,
        DYNAMIC,
        ADD,
        NOTIFICATION,
        PROFILE
    }

    private Context context;
    private View bottomNavigationView;
    private LinearLayout navHome, navDynamic, navAdd, navLike, navProfile;
    private ImageView homeIcon, dynamicIcon, likeIcon, profileIcon;
    private TextView homeText, dynamicText, likeText, profileText;

    public BottomNavigationHelper(Context context, View bottomNavigationView) {
        this.context = context;
        this.bottomNavigationView = bottomNavigationView;
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        navHome = bottomNavigationView.findViewById(R.id.nav_home);
        navDynamic = bottomNavigationView.findViewById(R.id.nav_dynamic);
        navAdd = bottomNavigationView.findViewById(R.id.nav_add);
        navLike = bottomNavigationView.findViewById(R.id.nav_like);
        navProfile = bottomNavigationView.findViewById(R.id.nav_profile);

        homeIcon = bottomNavigationView.findViewById(R.id.nav_home_icon);
        dynamicIcon = bottomNavigationView.findViewById(R.id.nav_dynamic_icon);
        likeIcon = bottomNavigationView.findViewById(R.id.nav_like_icon);
        profileIcon = bottomNavigationView.findViewById(R.id.nav_profile_icon);

        homeText = bottomNavigationView.findViewById(R.id.nav_home_text);
        dynamicText = bottomNavigationView.findViewById(R.id.nav_dynamic_text);
        likeText = bottomNavigationView.findViewById(R.id.nav_like_text);
        profileText = bottomNavigationView.findViewById(R.id.nav_profile_text);
    }


    private void setupClickListeners() {
        navHome.setOnClickListener(v -> navigateToActivity(HomeActivity.class));
        navDynamic.setOnClickListener(v -> navigateToActivity(CircleActivity.class));

        navAdd.setOnClickListener(v -> {
            if (context instanceof AddActivity) {
                // AddActivity 页面，单击直接跳转 NewPostActivity
                Intent intent = new Intent(context, NewPostActivity.class);
                context.startActivity(intent);
            } else {
                // 非 AddActivity 页面，单击跳转 AddActivity
                navigateToActivity(AddActivity.class);
            }
        });

        // 长按处理
        navAdd.setOnLongClickListener(v -> {
            // 非 AddActivity 页面，长按500ms跳转 NewPostActivity
            Intent intent = new Intent(context, NewPostActivity.class);
            context.startActivity(intent);
            return true; // 返回 true 表示事件已消费，不触发点击事件
        });

        navLike.setOnClickListener(v -> navigateToActivity(NotificationActivity.class));
        navProfile.setOnClickListener(v -> navigateToActivity(ProfileActivity.class));
    }




    private void navigateToActivity(Class<?> activityClass) {
        // 如果当前页面就是目标页面，不做任何操作
        if (context.getClass() == activityClass) {
            return;
        }

        Intent intent = new Intent(context, activityClass);
        context.startActivity(intent);

        // 添加过渡动画
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).overridePendingTransition(0, 0);
            ((AppCompatActivity) context).finish();
        }
    }

    /**
     * 设置当前选中的导航项
     * @param selectedItem 选中的导航项
     */
    public void setSelectedItem(NavigationItem selectedItem) {
        // 先重置所有项
        resetAllItems();

        // 设置选中项
        switch (selectedItem) {
            case HOME:
                setItemSelected(homeIcon, homeText);
                break;
            case DYNAMIC:
                setItemSelected(dynamicIcon, dynamicText);
                break;
            case ADD:
                // 添加按钮不需要设置选中状态，因为它是特殊的圆形按钮
                break;
            case NOTIFICATION:
                setItemSelected(likeIcon, likeText);
                break;
            case PROFILE:
                setItemSelected(profileIcon, profileText);
                break;
        }
    }

    private void resetAllItems() {
        resetItem(homeIcon, homeText);
        resetItem(dynamicIcon, dynamicText);
        resetItem(likeIcon, likeText);
        resetItem(profileIcon, profileText);
    }

    private void resetItem(ImageView icon, TextView text) {
        icon.setColorFilter(context.getResources().getColor(android.R.color.darker_gray));
        text.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
    }

    private void setItemSelected(ImageView icon, TextView text) {
        icon.setColorFilter(context.getResources().getColor(android.R.color.black));
        text.setTextColor(context.getResources().getColor(android.R.color.black));
    }

    /**
     * 设置自定义点击监听器（可选）
     * @param item 导航项
     * @param listener 点击监听器
     */
    public void setOnNavigationItemClickListener(NavigationItem item, View.OnClickListener listener) {
        switch (item) {
            case HOME:
                navHome.setOnClickListener(listener);
                break;
            case DYNAMIC:
                navDynamic.setOnClickListener(listener);
                break;
            case ADD:
                navAdd.setOnClickListener(listener);
                break;
            case NOTIFICATION:
                navLike.setOnClickListener(listener);
                break;
            case PROFILE:
                navProfile.setOnClickListener(listener);
                break;
        }
    }
}