package com.app.gameform.Activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.gameform.R;
import com.app.gameform.adapter.GameAdapter;
import com.app.gameform.adapter.PostAdapter;
import com.app.gameform.adapter.SectionAdapter;
import com.app.gameform.domain.Game;
import com.app.gameform.domain.Post;
import com.app.gameform.domain.Section;
import com.app.gameform.manager.PostLikeManager;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.ApiConstants;
import com.app.gameform.network.ApiService;

import com.app.gameform.utils.EndlessRecyclerViewScrollListener;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SearchResultActivity extends AppCompatActivity implements
        PostAdapter.OnPostClickListener, PostAdapter.OnPostLikeListener,
        GameAdapter.OnItemClickListener, SectionAdapter.OnItemClickListener {

    // UI 组件
    private FrameLayout btnBackFrame; // 修改：使用FrameLayout作为返回按钮容器
    private EditText etSearch;
    private TextView btnSearch;
    private TextView tabAll, tabPosts, tabGames, tabBoards;
    private View tabIndicator;
    private PostLikeManager likeManager;
    private LinearLayout layoutSearchStats, layoutEmpty, layoutLoading;
    private TextView tvSearchStats, tvSearchKeyword;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvSearchResults;
    private ProgressBar loadingProgress;

    // 数据和适配器
    private String currentTab = "all";
    private String searchQuery = "";
    private PostAdapter postAdapter;
    private GameAdapter gameAdapter;
    private SectionAdapter sectionAdapter;

    // 数据列表
    private List<Post> postList = new ArrayList<>();
    private List<Game> gameList = new ArrayList<>();
    private List<Section> sectionList = new ArrayList<>();

    // 统计数据
    private int postsCount = 0;
    private int gamesCount = 0;
    private int sectionsCount = 0;
    private int totalCount = 0;

    // 分页参数
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private EndlessRecyclerViewScrollListener scrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        initViews();
        setupListeners();
        setupRecyclerView();
        initializeLikeManager();

        // 从Intent获取搜索关键词
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("search_query")) {
            searchQuery = intent.getStringExtra("search_query");
            etSearch.setText(searchQuery);
            performSearch();
        }
    }

    private void initializeLikeManager() {
        likeManager = new PostLikeManager(this);
    }

    private void initViews() {
        // 修改：绑定FrameLayout容器而不是ImageView
        btnBackFrame = findViewById(R.id.btn_back_frame);
        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);

        tabAll = findViewById(R.id.tab_all);
        tabPosts = findViewById(R.id.tab_posts);
        tabGames = findViewById(R.id.tab_games);
        tabBoards = findViewById(R.id.tab_boards);
        tabIndicator = findViewById(R.id.tab_indicator);

        layoutSearchStats = findViewById(R.id.layout_search_stats);
        layoutEmpty = findViewById(R.id.layout_empty);
        layoutLoading = findViewById(R.id.layout_loading);

        tvSearchStats = findViewById(R.id.tv_search_stats);
        tvSearchKeyword = findViewById(R.id.tv_search_keyword);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        rvSearchResults = findViewById(R.id.rv_search_results);

        // 初始化适配器
        postAdapter = new PostAdapter(this, postList);
        gameAdapter = new GameAdapter(gameList);
        sectionAdapter = new SectionAdapter(sectionList);

        // 设置监听器
        postAdapter.setOnPostClickListener(this);
        postAdapter.setOnPostLikeListener(this);
        gameAdapter.setOnItemClickListener(this);
        sectionAdapter.setOnItemClickListener(this);
    }

    private void setupListeners() {
        // 修改：返回按钮绑定到FrameLayout容器
        btnBackFrame.setOnClickListener(v -> finish());

        // 搜索按钮
        btnSearch.setOnClickListener(v -> handleSearch());

        // 搜索输入框
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                handleSearch();
                return true;
            }
            return false;
        });

        // 标签切换
        tabAll.setOnClickListener(v -> switchTab("all"));
        tabPosts.setOnClickListener(v -> switchTab("posts"));
        tabGames.setOnClickListener(v -> switchTab("games"));
        tabBoards.setOnClickListener(v -> switchTab("sections"));

        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::refreshSearch);
    }

    private void setupRecyclerView() {
        // 设置默认布局管理器
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(postAdapter);

        // 设置滚动监听器用于加载更多
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvSearchResults.setLayoutManager(layoutManager);

        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (hasMore && !isLoading) {
                    loadMoreData();
                }
            }
        };
        rvSearchResults.addOnScrollListener(scrollListener);
    }

    private void handleSearch() {
        String query = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            return;
        }

        searchQuery = query;
        currentPage = 1;
        hasMore = true;
        clearAllData();
        performSearch();
    }

    private void refreshSearch() {
        currentPage = 1;
        hasMore = true;
        clearAllData();
        performSearch();
    }

    private void clearAllData() {
        postList.clear();
        gameList.clear();
        sectionList.clear();

        postsCount = 0;
        gamesCount = 0;
        sectionsCount = 0;
        totalCount = 0;

        updateTabCounts();

        if (scrollListener != null) {
            scrollListener.resetState();
        }
    }

    private void switchTab(String tab) {
        if (currentTab.equals(tab)) return;

        currentTab = tab;
        updateTabUI();
        updateRecyclerView();

        // 如果没有数据且有搜索词，则搜索
        if (shouldLoadData() && !TextUtils.isEmpty(searchQuery)) {
            currentPage = 1;
            hasMore = true;
            performSearch();
        }
    }

    private boolean shouldLoadData() {
        switch (currentTab) {
            case "posts":
                return postList.isEmpty();
            case "games":
                return gameList.isEmpty();
            case "sections":
                return sectionList.isEmpty();
            default:
                return postList.isEmpty() && gameList.isEmpty() && sectionList.isEmpty();
        }
    }

    private void updateTabUI() {
        // 重置所有标签样式
        resetTabStyles();

        // 设置选中标签样式
        TextView selectedTab = null;
        switch (currentTab) {
            case "all":
                selectedTab = tabAll;
                break;
            case "posts":
                selectedTab = tabPosts;
                break;
            case "games":
                selectedTab = tabGames;
                break;
            case "sections":
                selectedTab = tabBoards;
                break;
        }

        if (selectedTab != null) {
            selectedTab.setTextColor(Color.parseColor("#007AFF"));
            selectedTab.setTypeface(null, Typeface.BOLD);
        }

        // 更新指示器位置
        updateTabIndicator(selectedTab);
    }

    private void resetTabStyles() {
        tabAll.setTextColor(Color.parseColor("#666666"));
        tabAll.setTypeface(null, Typeface.NORMAL);

        tabPosts.setTextColor(Color.parseColor("#666666"));
        tabPosts.setTypeface(null, Typeface.NORMAL);

        tabGames.setTextColor(Color.parseColor("#666666"));
        tabGames.setTypeface(null, Typeface.NORMAL);

        tabBoards.setTextColor(Color.parseColor("#666666"));
        tabBoards.setTypeface(null, Typeface.NORMAL);
    }

    // 在 SearchResultActivity.java 中替换 updateTabIndicator 方法

    private void updateTabIndicator(TextView selectedTab) {
        if (selectedTab != null && tabIndicator != null) {
            selectedTab.post(() -> {
                // 获取选中标签的位置信息
                int[] tabLocation = new int[2];
                selectedTab.getLocationInWindow(tabLocation);

                // 获取标签容器的位置信息
                int[] containerLocation = new int[2];
                findViewById(R.id.tab_container).getLocationInWindow(containerLocation);

                // 计算相对位置
                int tabLeft = tabLocation[0] - containerLocation[0];
                int tabWidth = selectedTab.getWidth();

                // 设置指示器的位置和宽度
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tabIndicator.getLayoutParams();
                if (params == null) {
                    params = new LinearLayout.LayoutParams(tabWidth, tabIndicator.getLayoutParams().height);
                } else {
                    params.width = tabWidth;
                }
                params.leftMargin = tabLeft;
                tabIndicator.setLayoutParams(params);
            });
        }
    }

    private void updateRecyclerView() {
        switch (currentTab) {
            case "all":
                setupMixedLayout();
                break;
            case "posts":
                rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
                rvSearchResults.setAdapter(postAdapter);
                break;
            case "games":
                rvSearchResults.setLayoutManager(new GridLayoutManager(this, 2));
                rvSearchResults.setAdapter(gameAdapter);
                break;
            case "sections":
                rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
                rvSearchResults.setAdapter(sectionAdapter);
                break;
        }
    }

    private void setupMixedLayout() {
        // 全部标签页显示混合内容，这里简单处理，主要显示帖子
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(postAdapter);
    }

    private void performSearch() {
        if (TextUtils.isEmpty(searchQuery)) return;

        showLoading();
        isLoading = true;

        // 根据当前标签决定搜索类型
        switch (currentTab) {
            case "all":
                searchAll();
                break;
            case "posts":
                searchPosts();
                break;
            case "games":
                searchGames();
                break;
            case "sections":
                searchSections();
                break;
        }
    }

    private void searchAll() {
        // 并行搜索所有类型
        searchPosts();
        searchGames();
        searchSections();
    }

    private void searchPosts() {
        String url = ApiConstants.buildSearchPostUrl(searchQuery);

        ApiService.getInstance().getPosts(url, new ApiCallback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                runOnUiThread(() -> {
                    if (currentPage == 1) {
                        postList.clear();
                    }

                    if (posts != null && !posts.isEmpty()) {
                        postList.addAll(posts);
                        postsCount = postList.size();
                        hasMore = posts.size() >= 10; // 假设每页10条
                    } else {
                        hasMore = false;
                    }

                    updateTabCounts();
                    updateUI();
                    checkAllSearchComplete();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    handleSearchError(error);
                    checkAllSearchComplete();
                });
            }
        });
    }

    private void searchGames() {
        String url = ApiConstants.buildSearchUrl(ApiConstants.SEARCH_GAMES, searchQuery);

        ApiService.getInstance().getRequest(url, new ApiCallback<String>() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUiThread(() -> {
                    try {
                        List<Game> games = parseGamesResponse(jsonResponse);

                        if (currentPage == 1) {
                            gameList.clear();
                        }

                        if (games != null && !games.isEmpty()) {
                            gameList.addAll(games);
                            gamesCount = gameList.size();
                        }

                        updateTabCounts();
                        updateUI();
                        checkAllSearchComplete();

                    } catch (Exception e) {
                        Log.e("SearchResult", "解析游戏数据失败", e);
                        checkAllSearchComplete();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    handleSearchError(error);
                    checkAllSearchComplete();
                });
            }
        });
    }

    private void searchSections() {
        String url = ApiConstants.buildSearchUrl(ApiConstants.SEARCH_SECTIONS, searchQuery);

        ApiService.getInstance().getRequest(url, new ApiCallback<String>() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUiThread(() -> {
                    try {
                        List<Section> sections = parseSectionsResponse(jsonResponse);

                        if (currentPage == 1) {
                            sectionList.clear();
                        }

                        if (sections != null && !sections.isEmpty()) {
                            sectionList.addAll(sections);
                            sectionsCount = sectionList.size();
                        }

                        updateTabCounts();
                        updateUI();
                        checkAllSearchComplete();

                    } catch (Exception e) {
                        Log.e("SearchResult", "解析版块数据失败", e);
                        checkAllSearchComplete();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    handleSearchError(error);
                    checkAllSearchComplete();
                });
            }
        });
    }

    private List<Game> parseGamesResponse(String jsonResponse) {
        try {
            Gson gson = ApiService.getInstance().getGson();
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (jsonObject.get("code").getAsInt() == 200) {
                JsonArray dataArray = jsonObject.getAsJsonArray("data");
                Type listType = new TypeToken<List<Game>>(){}.getType();
                return gson.fromJson(dataArray, listType);
            }
        } catch (Exception e) {
            Log.e("SearchResult", "解析游戏响应失败", e);
        }
        return new ArrayList<>();
    }

    private List<Section> parseSectionsResponse(String jsonResponse) {
        try {
            Gson gson = ApiService.getInstance().getGson();
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (jsonObject.get("code").getAsInt() == 200) {
                JsonArray dataArray = jsonObject.getAsJsonArray("data");
                Type listType = new TypeToken<List<Section>>(){}.getType();
                return gson.fromJson(dataArray, listType);
            }
        } catch (Exception e) {
            Log.e("SearchResult", "解析版块响应失败", e);
        }
        return new ArrayList<>();
    }

    private void loadMoreData() {
        if (isLoading || !hasMore) return;

        currentPage++;
        performSearch();
    }

    private void updateTabCounts() {
        totalCount = postsCount + gamesCount + sectionsCount;

        tabPosts.setText(String.format("帖子 (%d)", postsCount));
        tabGames.setText(String.format("游戏 (%d)", gamesCount));
        tabBoards.setText(String.format("版块 (%d)", sectionsCount));
    }

    private void updateUI() {
        // 更新搜索统计信息
        if (totalCount > 0) {
            layoutSearchStats.setVisibility(View.VISIBLE);
            tvSearchStats.setText(String.format("找到 %d 个结果", totalCount));
            tvSearchKeyword.setText(String.format("关于 \"%s\"", searchQuery));
        } else {
            layoutSearchStats.setVisibility(View.GONE);
        }

        // 通知适配器数据更新
        switch (currentTab) {
            case "posts":
                postAdapter.notifyDataSetChanged();
                break;
            case "games":
                gameAdapter.notifyDataSetChanged();
                break;
            case "sections":
                sectionAdapter.notifyDataSetChanged();
                break;
            case "all":
                // 全部标签页主要显示帖子
                postAdapter.notifyDataSetChanged();
                break;
        }
    }

    private void showLoading() {
        if (currentPage == 1) {
            if (totalCount == 0) {
                layoutLoading.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
                rvSearchResults.setVisibility(View.GONE);
            }
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    private void hideLoading() {
        layoutLoading.setVisibility(View.GONE);

        if (totalCount > 0) {
            layoutEmpty.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.VISIBLE);
        } else {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.GONE);
        }

        isLoading = false;
        swipeRefreshLayout.setRefreshing(false);
    }

    private void handleSearchError(String error) {
        Toast.makeText(this, "搜索失败: " + error, Toast.LENGTH_SHORT).show();
        Log.e("SearchResult", "搜索错误: " + error);
    }

    // 用于检查所有搜索是否完成（在搜索全部时使用）
    private int searchCompleteCount = 0;
    private void checkAllSearchComplete() {
        if (currentTab.equals("all")) {
            searchCompleteCount++;
            if (searchCompleteCount >= 3) { // 三种类型都搜索完成
                searchCompleteCount = 0;
                hideLoading();
            }
        } else {
            hideLoading();
        }
    }

    // 实现 PostAdapter 接口
    @Override
    public void onPostClick(Post post, int position) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post_id", post.getPostId());
        startActivity(intent);
    }

    @Override
    public void onUserClick(Post post, int position) {
        Toast.makeText(this, "用户: " + post.getNickName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCommentClick(Post post, int position) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post_id", post.getPostId());
        intent.putExtra("focus_comment", true);
        startActivity(intent);
    }
    @Override
    public void onDeleteClick(Post post, int position) {
        // 暂时不需要删除功能，可以空实现
    }

    @Override
    public void onViewClick(Post post, int position) {
        Toast.makeText(this, "浏览量: " + post.getViewCount(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMoreClick(Post post, int position) {
        Toast.makeText(this, "更多操作", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLikeClick(Post post, int position) {
        likeManager.handleLikeClick(post, position, new PostLikeManager.LikeStatusCallback() {
            @Override
            public void onUpdate(boolean hasLiked, int newLikeCount) {
                runOnUiThread(() -> {
                    // 更新对应列表中的数据
                    updatePostLikeStatus(post, position, hasLiked, newLikeCount);
                });
            }

            @Override
            public void onFail(String errorMessage) {
                // 记录错误日志
                Log.e("SearchResultActivity", "点赞操作失败: " + errorMessage);
            }
        });
    }

    private void updatePostLikeStatus(Post post, int position, boolean hasLiked, int newLikeCount) {
        // 更新帖子对象的点赞数
        post.setLikeCount(newLikeCount);

        // 根据当前显示的适配器更新UI
        switch (currentTab) {
            case "posts":
            case "all":
                // 对于帖子列表，使用 PostAdapter 的更新方法
                if (postAdapter != null) {
                    // 找到在当前显示列表中的实际位置
                    int actualPosition = findPostPositionInCurrentList(post);
                    if (actualPosition != -1) {
                        postAdapter.updateLikeStatus(actualPosition, hasLiked, newLikeCount);
                    }
                }
                break;
            case "games":
            case "sections":
                // 游戏和版块标签页不涉及帖子点赞
                break;
        }
    }

    private int findPostPositionInCurrentList(Post targetPost) {
        if (targetPost == null || targetPost.getPostId() == null) {
            return -1;
        }

        for (int i = 0; i < postList.size(); i++) {
            Post post = postList.get(i);
            if (post != null && post.getPostId() != null &&
                    post.getPostId().equals(targetPost.getPostId())) {
                return i;
            }
        }
        return -1;
    }


    // 实现 GameAdapter 接口
    @Override
    public void onItemClick(Game game) {
        Intent intent = new Intent(this, GameDetailActivity.class);
        intent.putExtra("gameId", game.getGameId());
        startActivity(intent);
    }

    // 实现 SectionAdapter 接口
    @Override
    public void onItemClick(Section section) {
        Intent intent = new Intent(this, SectionDetailActivity.class);
        intent.putExtra("section_id", section.getSectionId());
        startActivity(intent);
    }
}