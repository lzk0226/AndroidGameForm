package com.app.gameform.Activity.Home;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.app.gameform.R;
import com.app.gameform.fragment.SectionsFragment;
import com.app.gameform.fragment.GamesFragment;
import com.app.gameform.utils.BottomNavigationHelper;

public class CircleActivity extends BaseActivity {
    private BottomNavigationHelper bottomNavigationHelper;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private CirclePagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        setContentView(R.layout.activity_circle);

        setupViews();
        setupBottomNavigation();
    }

    private void setupViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // 创建适配器
        pagerAdapter = new CirclePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // 关联TabLayout和ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("板块");
                    break;
                case 1:
                    tab.setText("游戏");
                    break;
            }
        }).attach();
    }

    private void setupBottomNavigation() {
        // 使用底部导航栏工具类
        bottomNavigationHelper = new BottomNavigationHelper(this, findViewById(R.id.bottomNavigationInclude));
        // 设置当前页面为DYNAMIC选中状态
        bottomNavigationHelper.setSelectedItem(BottomNavigationHelper.NavigationItem.DYNAMIC);
    }

    // ViewPager2适配器
    private static class CirclePagerAdapter extends FragmentStateAdapter {
        public CirclePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new SectionsFragment();
                case 1:
                    return new GamesFragment();
                default:
                    return new SectionsFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}