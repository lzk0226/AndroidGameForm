package com.app.gameform.Activity.Home;

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

import com.app.gameform.Activity.PostDetailActivity;
import com.app.gameform.Activity.SearchActivity;
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

public class HomeActivity extends BaseActivity implements
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

    // 懒加载相关变量
    private static final int PAGE_SIZE = 8; // 每页加载8个帖子
    private static final int LOAD_MORE_THRESHOLD = 6; // 浏览到第6个时开始加载
    private Map<String, Integer> currentPageMap = new HashMap<>(); // 每个标签的当前页码
    private Map<String, Boolean> hasMoreDataMap = new HashMap<>(); // 每个标签是否还有更多数据
    private Map<String, Boolean> isLoadingMap = new HashMap<>(); // 每个标签的加载状态

    // 数据缓存 - 缓存每个标签的数据
    private Map<String, List<Post>> dataCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.home);

            // 设置自定义退出提示信息
            setExitMessage("再按一次退出应用");

            initViews();
            initLazyLoadingData();
            setupRecyclerView();
            setupTabListeners();
            setupSearchListener();
            setupBottomNavigation();
            setupSwipeRefresh();
            setupScrollListener();

            // 默认显示推荐
            switchTab(currentTab);
            loadPostDataWithCache(currentTab, true);
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

    private void initLazyLoadingData() {
        String[] tabs = {"hot", "recommend", "follow", "new"};
        for (String tab : tabs) {
            currentPageMap.put(tab, 1);
            hasMoreDataMap.put(tab, true);
            isLoadingMap.put(tab, false);
        }
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

    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) { // 向下滑动
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    // 计算当前可见的最后一个item的位置
                    int lastVisibleItem = firstVisibleItemPosition + visibleItemCount;

                    // 当浏览到倒数第3个item时开始加载更多(总数-当前可见最后位置<=阈值)
                    if (!isCurrentTabLoading() && hasMoreData(currentTab) &&
                            (totalItemCount - lastVisibleItem) <= (PAGE_SIZE - LOAD_MORE_THRESHOLD)) {
                        loadMorePosts();
                    }
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        swipeRefreshLayout.setOnRefreshListener(this::refreshCurrentTab);
    }

    private void refreshCurrentTab() {
        // 重置页码和状态
        resetTabData(currentTab);
        loadPostDataWithCache(currentTab, true);
    }

    private void setupTabListeners() {
        tabHot.setOnClickListener(v -> {
            if (!currentTab.equals("hot")) {
                switchTab("hot");
                loadPostDataWithCache("hot", true);
            }
        });

        tabRecommend.setOnClickListener(v -> {
            if (!currentTab.equals("recommend")) {
                switchTab("recommend");
                loadPostDataWithCache("recommend", true);
            }
        });

        tabFollow.setOnClickListener(v -> {
            if (!currentTab.equals("follow")) {
                switchTab("follow");
                loadPostDataWithCache("follow", true);
            }
        });

        tabNew.setOnClickListener(v -> {
            if (!currentTab.equals("new")) {
                switchTab("new");
                loadPostDataWithCache("new", true);
            }
        });
    }

    private void setupSearchListener() {
        iconSearch.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper(this, findViewById(R.id.bottomNavigationInclude));
        bottomNavigationHelper.setSelectedItem(BottomNavigationHelper.NavigationItem.HOME);
        bottomNavigationHelper.setOnHomeDoubleClickListener(this);
    }

    @Override
    public void onHomeDoubleClick() {
        recyclerView.smoothScrollToPosition(0);
        recyclerView.postDelayed(() -> {
            refreshCurrentTab();
            Toast.makeText(HomeActivity.this, "已刷新", Toast.LENGTH_SHORT).show();
        }, 200);
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
    private void loadPostDataWithCache(String type, boolean isRefresh) {
        if (isRefresh) {
            resetTabData(type);
            loadPostData(type, true, false);
        } else {
            List<Post> cachedData = dataCache.get(type);
            if (cachedData != null && !cachedData.isEmpty() && getCurrentPage(type) == 1) {
                updatePostList(cachedData, false);
            } else {
                loadPostData(type, false, false);
            }
        }
    }

    /**
     * 加载更多帖子
     */
    private void loadMorePosts() {
        if (!isCurrentTabLoading() && hasMoreData(currentTab)) {
            int nextPage = getCurrentPage(currentTab) + 1;
            setCurrentPage(currentTab, nextPage);
            loadPostData(currentTab, false, true);
        }
    }

    /**
     * 加载帖子数据
     */
    private void loadPostData(String type, boolean isRefresh, boolean isLoadMore) {
        if (isCurrentTabLoading()) return;

        setLoading(type, true);

        if (!isRefresh && !isLoadMore) {
            showLoading();
        }

        String url = getApiUrl(type, getCurrentPage(type), PAGE_SIZE);

        ApiService.getInstance().getPosts(url, new ApiCallback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                runOnUiThread(() -> {
                    setLoading(type, false);

                    if (!isRefresh && !isLoadMore) {
                        hideLoading();
                    }

                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    // 检查是否还有更多数据
                    boolean hasMore = posts != null && posts.size() == PAGE_SIZE;
                    setHasMoreData(type, hasMore);

                    if (posts != null) {
                        if (isRefresh || (!isLoadMore && getCurrentPage(type) == 1)) {
                            // 刷新或首次加载：替换所有数据
                            dataCache.put(type, new ArrayList<>(posts));
                            updatePostList(posts, false);
                        } else if (isLoadMore) {
                            // 加载更多：追加数据
                            List<Post> cachedData = dataCache.get(type);
                            if (cachedData == null) {
                                cachedData = new ArrayList<>();
                                dataCache.put(type, cachedData);
                            }
                            cachedData.addAll(posts);
                            updatePostList(posts, true);
                        }

                        if (isRefresh) {
                            Toast.makeText(HomeActivity.this, "刷新成功", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        setHasMoreData(type, false);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(type, false);

                    if (!isRefresh && !isLoadMore) {
                        hideLoading();
                    }

                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    // 加载失败时回退页码
                    if (isLoadMore && getCurrentPage(type) > 1) {
                        setCurrentPage(type, getCurrentPage(type) - 1);
                    }

                    Toast.makeText(HomeActivity.this, "加载失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updatePostList(List<Post> newPosts, boolean isAppend) {
        if (isAppend) {
            int startPosition = postList.size();
            postList.addAll(newPosts);
            postAdapter.notifyItemRangeInserted(startPosition, newPosts.size());
        } else {
            postList.clear();
            postList.addAll(newPosts);
            postAdapter.notifyDataSetChanged();
        }
    }

    private String getApiUrl(String type, int page, int pageSize) {
        String baseUrl = USER_POST;
        String url;

        switch (type) {
            case "hot":
                url = baseUrl + "hot";
                break;
            case "recommend":
            case "follow":
            case "new":
            default:
                url = baseUrl + "list";
                break;
        }

        // 添加分页参数
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "page=" + page + "&size=" + pageSize;
    }

    // 辅助方法
    private void resetTabData(String tab) {
        setCurrentPage(tab, 1);
        setHasMoreData(tab, true);
        setLoading(tab, false);
    }

    private int getCurrentPage(String tab) {
        return currentPageMap.getOrDefault(tab, 1);
    }

    private void setCurrentPage(String tab, int page) {
        currentPageMap.put(tab, page);
    }

    private boolean hasMoreData(String tab) {
        return hasMoreDataMap.getOrDefault(tab, true);
    }

    private void setHasMoreData(String tab, boolean hasMore) {
        hasMoreDataMap.put(tab, hasMore);
    }

    private boolean isCurrentTabLoading() {
        return isLoadingMap.getOrDefault(currentTab, false);
    }

    private void setLoading(String tab, boolean loading) {
        isLoadingMap.put(tab, loading);
    }

    private void showLoading() {
        // TODO: 显示加载动画
    }

    private void hideLoading() {
        // TODO: 隐藏加载动画
    }

    // 点击事件实现
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
        if (bottomNavigationHelper != null) {
            bottomNavigationHelper.destroy();
        }
    }
}