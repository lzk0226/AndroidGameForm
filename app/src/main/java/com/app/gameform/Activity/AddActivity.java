package com.app.gameform.Activity;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.app.gameform.R;
import com.app.gameform.utils.BottomNavigationHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AddActivity extends AppCompatActivity {
    private BottomNavigationHelper bottomNavigationHelper;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private PostPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        initViews();
        setupViewPager();
        setupBottomNavigation();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
    }

    private void setupViewPager() {
        pagerAdapter = new PostPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // 连接TabLayout和ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("草稿");
                            break;
                        case 1:
                            tab.setText("已发布");
                            break;
                        case 2:
                            tab.setText("回收站");
                            break;
                    }
                }
        ).attach();
    }

    private void setupBottomNavigation() {
        // 使用底部导航栏工具类
        bottomNavigationHelper = new BottomNavigationHelper(this, findViewById(R.id.bottomNavigationInclude));
    }

    // ViewPager2适配器
    private static class PostPagerAdapter extends FragmentStateAdapter {

        public PostPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return PostListFragment.newInstance(position);
        }

        @Override
        public int getItemCount() {
            return 3; // 草稿、已发布、回收站
        }
    }

    // 公共方法供外部调用
    public void refreshCurrentTab() {
        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentByTag("f" + viewPager.getCurrentItem());
        if (currentFragment instanceof PostListFragment) {
            ((PostListFragment) currentFragment).refreshData();
        }
    }

    public void switchToTab(int position) {
        if (position >= 0 && position < 3) {
            viewPager.setCurrentItem(position, true);
        }
    }
}