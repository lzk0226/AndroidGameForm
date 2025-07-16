package com.app.gameform.Activity;

import static com.app.gameform.network.ApiConstants.USER_POST;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.domain.Post;
import com.app.gameform.manager.PostLikeManager;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.ApiService;
import com.app.gameform.utils.BottomNavigationHelper;
import com.app.gameform.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements PostAdapter.OnPostClickListener, PostAdapter.OnPostLikeListener {
    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private TextView tabHot, tabRecommend, tabFollow, tabNew;
    private ImageView iconSearch;
    private BottomNavigationHelper bottomNavigationHelper;
    private String currentTab = "follow"; // 默认选中关注

    // 添加 SharedPreferences 来保存状态
    private static final String PREFS_NAME = "HomeActivityPrefs";
    private static final String KEY_CURRENT_TAB = "current_tab";
    private static final String KEY_SCROLL_POSITION = "scroll_position_";
    private SharedPreferences prefs;

    // 数据缓存 - 缓存每个标签的数据
    private Map<String, List<Post>> dataCache = new HashMap<>();
    private Map<String, Integer> scrollPositionCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.home);

            // 初始化 SharedPreferences
            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

            // 恢复上次选中的标签
            currentTab = prefs.getString(KEY_CURRENT_TAB, "follow");

            initViews();
            setupRecyclerView();
            setupTabListeners();
            setupSearchListener();
            setupBottomNavigation();

            // 设置当前标签并加载数据
            switchTab(currentTab);
            loadPostDataWithCache(currentTab);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("HomeActivity", "Error in onCreate: " + e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 保存当前滚动位置
        saveScrollPosition();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 恢复滚动位置
        restoreScrollPosition();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tabHot = findViewById(R.id.tab_hot);
        tabRecommend = findViewById(R.id.tab_recommend);
        tabFollow = findViewById(R.id.tab_follow);
        tabNew = findViewById(R.id.tab_new);
        iconSearch = findViewById(R.id.icon_search);

        postList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        postAdapter = new PostAdapter(this, postList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(postAdapter);

        // 设置监听器
        postAdapter.setOnPostClickListener(this);
        postAdapter.setOnPostLikeListener(this);
    }

    private void setupTabListeners() {
        tabHot.setOnClickListener(v -> {
            saveScrollPosition(); // 保存当前滚动位置
            switchTab("hot");
            loadPostDataWithCache("hot");
            saveCurrentTab("hot"); // 保存当前选中的标签
        });

        tabRecommend.setOnClickListener(v -> {
            saveScrollPosition();
            switchTab("recommend");
            loadPostDataWithCache("recommend");
            saveCurrentTab("recommend");
        });

        tabFollow.setOnClickListener(v -> {
            saveScrollPosition();
            switchTab("follow");
            loadPostDataWithCache("follow");
            saveCurrentTab("follow");
        });

        tabNew.setOnClickListener(v -> {
            saveScrollPosition();
            switchTab("new");
            loadPostDataWithCache("new");
            saveCurrentTab("new");
        });
    }

    private void setupSearchListener() {
        iconSearch.setOnClickListener(v -> {
            // 跳转到搜索页面
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        // 使用底部导航栏工具类
        bottomNavigationHelper = new BottomNavigationHelper(this, findViewById(R.id.bottomNavigationInclude));

        // 设置当前页面为HOME选中状态
        bottomNavigationHelper.setSelectedItem(BottomNavigationHelper.NavigationItem.HOME);
    }

    private void switchTab(String tab) {
        // 重置所有标签样式
        resetTabStyles();

        currentTab = tab;

        // 设置选中标签样式
        switch (tab) {
            case "hot":
                setTabSelected(tabHot);
                break;
            case "recommend":
                setTabSelected(tabRecommend);
                break;
            case "follow":
                setTabSelected(tabFollow);
                break;
            case "new":
                setTabSelected(tabNew);
                break;
        }
    }

    private void resetTabStyles() {
        tabHot.setTextColor(Color.parseColor("#999999"));
        tabHot.setTypeface(null, Typeface.NORMAL);

        tabRecommend.setTextColor(Color.parseColor("#999999"));
        tabRecommend.setTypeface(null, Typeface.NORMAL);

        tabFollow.setTextColor(Color.parseColor("#999999"));
        tabFollow.setTypeface(null, Typeface.NORMAL);

        tabNew.setTextColor(Color.parseColor("#999999"));
        tabNew.setTypeface(null, Typeface.NORMAL);
    }

    private void setTabSelected(TextView tab) {
        tab.setTextColor(Color.parseColor("#333333"));
        tab.setTypeface(null, Typeface.BOLD);
    }

    /**
     * 保存当前选中的标签到 SharedPreferences
     */
    private void saveCurrentTab(String tab) {
        prefs.edit().putString(KEY_CURRENT_TAB, tab).apply();
    }

    /**
     * 保存当前滚动位置
     */
    private void saveScrollPosition() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null) {
            int position = layoutManager.findFirstVisibleItemPosition();
            scrollPositionCache.put(currentTab, position);

            // 也可以保存到 SharedPreferences（持久化存储）
            prefs.edit().putInt(KEY_SCROLL_POSITION + currentTab, position).apply();
        }
    }

    /**
     * 恢复滚动位置
     */
    private void restoreScrollPosition() {
        Integer position = scrollPositionCache.get(currentTab);
        if (position == null) {
            // 从 SharedPreferences 恢复
            position = prefs.getInt(KEY_SCROLL_POSITION + currentTab, 0);
        }

        if (position > 0 && position < postList.size()) {
            recyclerView.scrollToPosition(position);
        }
    }

    /**
     * 带缓存的数据加载
     */
    private void loadPostDataWithCache(String type) {
        // 检查是否有缓存数据
        List<Post> cachedData = dataCache.get(type);
        if (cachedData != null && !cachedData.isEmpty()) {
            // 使用缓存数据
            postList.clear();
            postList.addAll(cachedData);
            postAdapter.notifyDataSetChanged();

            // 恢复滚动位置
            restoreScrollPosition();

            // 可选：在后台刷新数据
            refreshDataInBackground(type);
        } else {
            // 没有缓存，加载新数据
            loadPostData(type);
        }
    }

    /**
     * 后台刷新数据
     */
    private void refreshDataInBackground(String type) {
        String url = getApiUrl(type);

        ApiService.getInstance().getPosts(url, new ApiCallback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                runOnUiThread(() -> {
                    // 更新缓存
                    dataCache.put(type, new ArrayList<>(posts));

                    // 如果当前标签页还是这个类型，更新UI
                    if (currentTab.equals(type)) {
                        postList.clear();
                        postList.addAll(posts);
                        postAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(HomeActivity.this, "加载失败: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void loadPostData(String type) {
        // 显示加载状态
        showLoading();

        String url = getApiUrl(type);

        // 使用你的网络请求库（如Retrofit、OkHttp等）
        ApiService.getInstance().getPosts(url, new ApiCallback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                runOnUiThread(() -> {
                    hideLoading();

                    // 更新缓存
                    dataCache.put(type, new ArrayList<>(posts));

                    postList.clear();
                    postList.addAll(posts);
                    postAdapter.notifyDataSetChanged();

                    // 恢复滚动位置
                    restoreScrollPosition();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(HomeActivity.this, "加载失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String getApiUrl(String type) {
        String baseUrl = USER_POST;

        switch (type) {
            case "hot":
                return baseUrl + "hot?limit=20";
            case "recommend":
                return baseUrl + "list";
            case "follow":
                // 这里可能需要特殊处理，获取关注用户的帖子
                return baseUrl + "list"; // 临时使用通用列表
            case "new":
                return baseUrl + "list"; // 可以添加排序参数
            default:
                return baseUrl + "list";
        }
    }

    private void showLoading() {
        // 显示加载动画或进度条
        // 这里可以显示一个加载对话框或者进度条
    }

    private void hideLoading() {
        // 隐藏加载动画或进度条
        // 这里隐藏加载对话框或者进度条
    }

    // 实现PostAdapter.OnPostClickListener接口
    @Override
    public void onPostClick(Post post, int position) {
        // 处理帖子点击事件 - 跳转到帖子详情页
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post_id", post.getPostId());
        startActivity(intent);
    }

    @Override
    public void onUserClick(Post post, int position) {
        // 处理用户点击事件 - 跳转到用户资料页
        Toast.makeText(this, "点击了用户: " + post.getNickName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCommentClick(Post post, int position) {
        // 处理评论点击事件 - 跳转到评论页面
        Toast.makeText(this, "点击了评论", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onViewClick(Post post, int position) {
        // 处理浏览按钮点击事件（原来的分享按钮）
        Toast.makeText(this, "浏览量: " + post.getViewCount(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMoreClick(Post post, int position) {
        // 处理更多操作点击事件
        Toast.makeText(this, "点击了更多", Toast.LENGTH_SHORT).show();
        // 显示更多操作菜单
    }

    // 实现PostAdapter.OnPostLikeListener接口
    PostLikeManager likeManager = new PostLikeManager(this);

    @Override
    public void onLikeClick(Post post, int position) {
        likeManager.handleLikeClick(post, position, new PostLikeManager.LikeStatusCallback() {
            @Override
            public void onUpdate(boolean hasLiked, int newLikeCount) {
                runOnUiThread(() -> {
                    postAdapter.updateLikeStatus(position, hasLiked, newLikeCount);
                });
            }

            @Override
            public void onFail(String errorMessage) {
                // 可以额外处理失败逻辑，比如日志
                Log.e("PostLikeManager", errorMessage);
            }
        });
    }



}