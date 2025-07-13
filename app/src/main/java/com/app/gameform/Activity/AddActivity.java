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
        // 这里可以初始化发布页面的其他控件
        // 例如：编辑框、图片选择器、发布按钮等
    }

    private void setupBottomNavigation() {
        // 使用底部导航栏工具类
        bottomNavigationHelper = new BottomNavigationHelper(this, findViewById(R.id.bottomNavigationInclude));

        // 设置当前页面为ADD选中状态
        // 注意：ADD页面的中间按钮是特殊的圆形按钮，通常不需要设置选中状态
        // 但如果需要可以调用：bottomNavigationHelper.setSelectedItem(BottomNavigationHelper.NavigationItem.ADD);

        // 由于ADD按钮是特殊的圆形按钮，我们保持其他按钮的默认状态
        // 工具类会自动处理页面跳转和防止重复跳转
    }
}