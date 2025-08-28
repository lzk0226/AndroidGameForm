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
import com.app.gameform.network.ApiConstants;
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
    private TextView tabHot, tabRecommend, tabDiscover, tabNew; // 原来 tabFollow → tabDiscover
    private ImageView iconSearch;
    private BottomNavigationHelper bottomNavigationHelper;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currentTab = "recommend";
    private LinearLayoutManager layoutManager;

    // 懒加载相关变量
    private static final int PAGE_SIZE = 8; // 每页加载8个帖子
    private static final int LOAD_MORE_THRESHOLD = 6; // 浏览到第6个时开始加载
    private Map<String, Integer> currentPageMap = new HashMap<>();
    private Map<String, Boolean> hasMoreDataMap = new HashMap<>();
    private Map<String, Boolean> isLoadingMap = new HashMap<>();

    // 数据缓存
    private Map<String, List<Post>> dataCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.home);

            setExitMessage("再按一次退出应用");

            initViews();
            initLazyLoadingData();
            setupRecyclerView();
            setupTabListeners();
            setupSearchListener();
            setupBottomNavigation();
            setupSwipeRefresh();
            setupScrollListener();

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
        tabDiscover = findViewById(R.id.tab_discover);
        tabNew = findViewById(R.id.tab_new);
        iconSearch = findViewById(R.id.icon_search);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        postList = new ArrayList<>();
    }

    private void initLazyLoadingData() {
        String[] tabs = {"hot", "recommend", "discover", "new"};
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

        postAdapter.setOnPostClickListener(this);
        postAdapter.setOnPostLikeListener(this);
    }

    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    int lastVisibleItem = firstVisibleItemPosition + visibleItemCount;

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

        tabDiscover.setOnClickListener(v -> {
            if (!currentTab.equals("discover")) {
                switchTab("discover");
                loadPostDataWithCache("discover", true);
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
        recyclerView.postDelayed(this::refreshCurrentTab, 200);
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
            case "discover": // follow → discover
                setTabSelected(tabDiscover);
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
        tabDiscover.setTextColor(Color.parseColor("#999999"));
        tabDiscover.setTypeface(null, Typeface.NORMAL);
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


    private void loadPostData(String type, boolean isRefresh, boolean isLoadMore) {
        if (isCurrentTabLoading()) return;

        setLoading(type, true);

        if (!isRefresh && !isLoadMore) {
            showLoading();
        }

        if ("recommend".equals(type)) {
            int page = getCurrentPage(type);
            int limit = PAGE_SIZE;
            int offset = (page - 1) * limit;

            ApiService.getInstance().getHybridRecommendations(this, offset, page, new ApiCallback<List<Post>>() {
                @Override
                public void onSuccess(List<Post> posts) {
                    handlePostsResponse(posts, type, isRefresh, isLoadMore);
                }

                @Override
                public void onError(String error) {
                    Log.w("HomeActivity", "混合推荐失败，尝试个性化推荐: " + error);
                    loadFallbackPersonalizedRecommendations(limit, type, isRefresh, isLoadMore);
                }
            });
        } else {
            String url = getApiUrl(type, getCurrentPage(type), PAGE_SIZE);
            ApiService.getInstance().getPosts(url, new ApiCallback<List<Post>>() {
                @Override
                public void onSuccess(List<Post> posts) {
                    handlePostsResponse(posts, type, isRefresh, isLoadMore);
                }

                @Override
                public void onError(String error) {
                    handleApiError(error, type, isRefresh, isLoadMore);
                }
            });
        }
    }

    /**
     * 备选个性化推荐（当混合推荐失败时使用）
     */
    // 5. 修改备选推荐方法
    private void loadFallbackPersonalizedRecommendations(int limit, String type, boolean isRefresh, boolean isLoadMore) {
        ApiService.getInstance().getPersonalizedRecommendations(this, limit, new ApiCallback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                handlePostsResponse(posts, type, isRefresh, isLoadMore);
                Toast.makeText(HomeActivity.this, "推荐服务已切换到个性化模式", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Log.w("HomeActivity", "个性化推荐也失败，降级到热门帖子: " + error);
                loadFallbackHotPosts(type, isRefresh, isLoadMore);
            }
        });
    }

    private void loadFallbackHotPosts(String type, boolean isRefresh, boolean isLoadMore) {
        String hotUrl = ApiConstants.GET_HOT_POSTS + "?limit=" + PAGE_SIZE;
        ApiService.getInstance().getPosts(hotUrl, new ApiCallback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                handlePostsResponse(posts, type, isRefresh, isLoadMore);
                Toast.makeText(HomeActivity.this, "推荐服务暂时不可用，为您显示热门内容", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                handleApiError(error, type, isRefresh, isLoadMore);
            }
        });
    }
    /**
     * 处理帖子列表响应 - 修改版本，添加用户标识
     */
    private void handlePostsResponse(List<Post> posts, String type, boolean isRefresh, boolean isLoadMore) {
        runOnUiThread(() -> {
            setLoading(type, false);

            if (!isRefresh && !isLoadMore) {
                hideLoading();
            }

            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }

            boolean hasMore = posts != null && posts.size() == PAGE_SIZE;

            // 对于推荐页面的特殊处理
            if ("recommend".equals(type) && posts != null) {
                hasMore = posts.size() >= PAGE_SIZE;
            }

            setHasMoreData(type, hasMore);

            if (posts != null) {
                if (isRefresh || (!isLoadMore && getCurrentPage(type) == 1)) {
                    // 刷新时清除本地缓存，确保获取最新推荐
                    if ("recommend".equals(type)) {
                        dataCache.remove(type); // 清除本地缓存
                    }
                    dataCache.put(type, new ArrayList<>(posts));
                    updatePostList(posts, false);
                } else if (isLoadMore) {
                    List<Post> cachedData = dataCache.get(type);
                    if (cachedData == null) {
                        cachedData = new ArrayList<>();
                        dataCache.put(type, cachedData);
                    }
                    // 去重处理，防止重复数据
                    List<Post> newPosts = new ArrayList<>();
                    for (Post post : posts) {
                        boolean isDuplicate = false;
                        for (Post existingPost : cachedData) {
                            if (existingPost.getPostId().equals(post.getPostId())) {
                                isDuplicate = true;
                                break;
                            }
                        }
                        if (!isDuplicate) {
                            newPosts.add(post);
                        }
                    }
                    cachedData.addAll(newPosts);
                    updatePostList(newPosts, true);
                }

                if (isRefresh) {
                    String message = "recommend".equals(type) ? "推荐已更新" : "刷新成功";
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            } else {
                setHasMoreData(type, false);
            }
        });
    }
    /**
     * 处理API错误
     */
    private void handleApiError(String error, String type, boolean isRefresh, boolean isLoadMore) {
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

            String errorMessage = "recommend".equals(type) && error.contains("TOKEN_INVALID") ?
                    "请先登录以获取个性化推荐" : "加载失败: " + error;
            Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
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
                // 使用混合推荐接口
                url = ApiConstants.GET_HYBRID_RECOMMENDATIONS; // 需要在ApiConstants中定义
                break;
            case "follow":
            case "new":
            default:
                url = baseUrl + "list";
                break;
        }

        // 添加分页参数
        String separator = url.contains("?") ? "&" : "?";

        // 所有页面都支持分页
        return url + separator + "limit=" + pageSize + "&page=" + page;
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
                runOnUiThread(() -> {
                    postAdapter.updateLikeStatus(position, hasLiked, newLikeCount);
                    //dataCache.remove("recommend");
                });
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