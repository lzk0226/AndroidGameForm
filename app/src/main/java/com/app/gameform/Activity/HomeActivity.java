package com.app.gameform.Activity;

import static com.app.gameform.network.ApiConstants.USER_POST;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.gameform.R;
import com.app.gameform.adapter.PostAdapter;
import com.app.gameform.domain.Post;
import com.app.gameform.manager.PostLikeManager;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.ApiService;
import com.app.gameform.utils.BottomNavigationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements
        PostAdapter.OnPostClickListener,
        PostAdapter.OnPostLikeListener,
        BottomNavigationHelper.OnHomeDoubleClickListener {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private TextView tabHot, tabRecommend, tabFollow, tabNew;
    private ImageView iconSearch;
    private BottomNavigationHelper bottomNavigationHelper;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currentTab = "recommend";
    private LinearLayoutManager layoutManager;

    // 数据缓存 - 缓存每个标签的数据
    private Map<String, List<Post>> dataCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.home);

            initViews();
            setupRecyclerView();
            setupTabListeners();
            setupSearchListener();
            setupBottomNavigation();
            setupSwipeRefresh();

            // 默认显示推荐
            switchTab(currentTab);
            loadPostDataWithCache(currentTab);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("HomeActivity", "Error in onCreate: " + e.getMessage());
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tabHot = findViewById(R.id.tab_hot);
        tabRecommend = findViewById(R.id.tab_recommend);
        tabFollow = findViewById(R.id.tab_follow);
        tabNew = findViewById(R.id.tab_new);
        iconSearch = findViewById(R.id.icon_search);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        postList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(this);
        postAdapter = new PostAdapter(this, postList);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(postAdapter);

        // 设置监听器
        postAdapter.setOnPostClickListener(this);
        postAdapter.setOnPostLikeListener(this);
    }

    private void setupSwipeRefresh() {
        // 设置下拉刷新的颜色
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        // 设置下拉刷新监听器
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshCurrentTab();
            }
        });
    }

    private void refreshCurrentTab() {
        // 下拉刷新时，强制重新加载数据
        loadPostData(currentTab, true);
    }

    private void setupTabListeners() {
        tabHot.setOnClickListener(v -> {
            switchTab("hot");
            loadPostDataWithCache("hot");
        });

        tabRecommend.setOnClickListener(v -> {
            switchTab("recommend");
            loadPostDataWithCache("recommend");
        });

        tabFollow.setOnClickListener(v -> {
            switchTab("follow");
            loadPostDataWithCache("follow");
        });

        tabNew.setOnClickListener(v -> {
            switchTab("new");
            loadPostDataWithCache("new");
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
        bottomNavigationHelper.setSelectedItem(BottomNavigationHelper.NavigationItem.HOME);

        // 设置主页双击监听器
        bottomNavigationHelper.setOnHomeDoubleClickListener(this);
    }

    /**
     * 实现双击主页按钮回调
     * 双击主页按钮时，滚动到顶部并刷新数据
     */
    @Override
    public void onHomeDoubleClick() {
        // 滚动到顶部（平滑滚动）
        recyclerView.smoothScrollToPosition(0);

        // 延迟一点时间再刷新，让滚动动画先执行
        recyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 刷新当前标签的数据
                refreshCurrentTab();

                // 显示提示
                Toast.makeText(HomeActivity.this, "已刷新", Toast.LENGTH_SHORT).show();
            }
        }, 200); // 延迟200毫秒
    }

    private void switchTab(String tab) {
        resetTabStyles();
        currentTab = tab;

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
     * 带缓存的数据加载
     */
    private void loadPostDataWithCache(String type) {
        List<Post> cachedData = dataCache.get(type);
        if (cachedData != null && !cachedData.isEmpty()) {
            postList.clear();
            postList.addAll(cachedData);
            postAdapter.notifyDataSetChanged();

            // 可选：后台刷新
            refreshDataInBackground(type);
        } else {
            loadPostData(type, false);
        }
    }

    private void refreshDataInBackground(String type) {
        String url = getApiUrl(type);
        ApiService.getInstance().getPosts(url, new ApiCallback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                runOnUiThread(() -> {
                    dataCache.put(type, new ArrayList<>(posts));
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

    /**
     * 加载帖子数据
     * @param type 帖子类型
     * @param isRefresh 是否是下拉刷新
     */
    private void loadPostData(String type, boolean isRefresh) {
        if (!isRefresh) {
            showLoading();
        }

        String url = getApiUrl(type);

        ApiService.getInstance().getPosts(url, new ApiCallback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                runOnUiThread(() -> {
                    if (!isRefresh) {
                        hideLoading();
                    }

                    // 停止下拉刷新动画
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    dataCache.put(type, new ArrayList<>(posts));
                    postList.clear();
                    postList.addAll(posts);
                    postAdapter.notifyDataSetChanged();

                    if (isRefresh) {
                        Toast.makeText(HomeActivity.this, "刷新成功", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (!isRefresh) {
                        hideLoading();
                    }

                    // 停止下拉刷新动画
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    Toast.makeText(HomeActivity.this, "加载失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 重载方法，保持向后兼容
     */
    private void loadPostData(String type) {
        loadPostData(type, false);
    }

    private String getApiUrl(String type) {
        String baseUrl = USER_POST;
        switch (type) {
            case "hot":
                return baseUrl + "hot?limit=20";
            case "recommend":
                return baseUrl + "list";
            case "follow":
                return baseUrl + "list";
            case "new":
                return baseUrl + "list";
            default:
                return baseUrl + "list";
        }
    }

    private void showLoading() {
        // TODO: 显示加载动画
    }

    private void hideLoading() {
        // TODO: 隐藏加载动画
    }

    // 点击事件
    @Override
    public void onPostClick(Post post, int position) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post_id", post.getPostId());
        startActivity(intent);
    }

    @Override
    public void onUserClick(Post post, int position) {
        Toast.makeText(this, "点击了用户: " + post.getNickName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCommentClick(Post post, int position) {
        Toast.makeText(this, "点击了评论", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onViewClick(Post post, int position) {
        Toast.makeText(this, "浏览量: " + post.getViewCount(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMoreClick(Post post, int position) {
        Toast.makeText(this, "点击了更多", Toast.LENGTH_SHORT).show();
    }

    // 点赞功能
    PostLikeManager likeManager = new PostLikeManager(this);

    @Override
    public void onLikeClick(Post post, int position) {
        likeManager.handleLikeClick(post, position, new PostLikeManager.LikeStatusCallback() {
            @Override
            public void onUpdate(boolean hasLiked, int newLikeCount) {
                runOnUiThread(() ->
                        postAdapter.updateLikeStatus(position, hasLiked, newLikeCount)
                );
            }

            @Override
            public void onFail(String errorMessage) {
                Log.e("PostLikeManager", errorMessage);
            }
        });
    }

    @Override
    public void onDeleteClick(Post post, int position) {
        // 暂时不需要删除功能
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        if (bottomNavigationHelper != null) {
            bottomNavigationHelper.destroy();
        }
    }
}