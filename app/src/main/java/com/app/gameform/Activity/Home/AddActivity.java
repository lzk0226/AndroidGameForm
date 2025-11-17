package com.app.gameform.Activity.Home;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.app.gameform.Activity.PostListFragment;
import com.app.gameform.R;
import com.app.gameform.utils.BottomNavigationHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AddActivity extends BaseActivity {
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

        // 处理初始 Intent
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // 当 Activity 已存在时,会调用此方法
        handleIntent(intent);
    }

    /**
     * 处理 Intent 参数,切换到指定选项卡
     */
    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("tab_position")) {
            int tabPosition = intent.getIntExtra("tab_position", 0);
            if (tabPosition >= 0 && tabPosition < 2) {
                viewPager.setCurrentItem(tabPosition, false);
            }
        }
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
            return 2; // 草稿、已发布
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
        if (position >= 0 && position < 2) {
            viewPager.setCurrentItem(position, true);
        }
    }

    // 获取当前Fragment
    public PostListFragment getCurrentFragment() {
        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentByTag("f" + viewPager.getCurrentItem());
        if (currentFragment instanceof PostListFragment) {
            return (PostListFragment) currentFragment;
        }
        return null;
    }

    // 获取草稿Fragment
    public PostListFragment getDraftFragment() {
        Fragment draftFragment = getSupportFragmentManager()
                .findFragmentByTag("f0"); // 草稿是第0个tab
        if (draftFragment instanceof PostListFragment) {
            return (PostListFragment) draftFragment;
        }
        return null;
    }

    // 获取已发布Fragment
    public PostListFragment getPublishedFragment() {
        Fragment publishedFragment = getSupportFragmentManager()
                .findFragmentByTag("f1"); // 已发布是第1个tab
        if (publishedFragment instanceof PostListFragment) {
            return (PostListFragment) publishedFragment;
        }
        return null;
    }
}