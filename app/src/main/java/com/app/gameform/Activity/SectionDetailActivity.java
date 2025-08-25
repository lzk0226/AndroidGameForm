package com.app.gameform.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.app.gameform.R;
import com.app.gameform.adapter.PostAdapter;
import com.app.gameform.domain.Game;
import com.app.gameform.domain.Post;
import com.app.gameform.domain.Section;
import com.app.gameform.manager.PostLikeManager;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.ApiConstants;
import com.app.gameform.network.ApiService;
import com.app.gameform.utils.ImageUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SectionDetailActivity extends AppCompatActivity implements PostAdapter.OnPostClickListener, PostAdapter.OnPostLikeListener {

    private static final String TAG = "SectionDetailActivity";
    private static final String PREFS_NAME = "SectionDetailPrefs";
    private static final String KEY_SCROLL_POSITION = "scroll_position_";
    private static final String KEY_CURRENT_TAB = "current_tab_";
    private static final int PAGE_SIZE = 5;

    // Views
    private TextView tvBack;
    private CircleImageView ivGameIcon;
    private TextView tvSectionName;
    private TextView tvSectionDescription;
    private TextView tvPost;
    private TextView tvHot;
    private TextView tvLatest;
    private RecyclerView recyclerView;

    // Data
    private Integer sectionId;
    private Section sectionInfo;
    private Game gameInfo;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private String currentTab = "hot"; // "hot" 或 "latest"
    private boolean isLoading = false;
    private boolean hasMore = true;
    private int currentPage = 1;

    // Cache
    private SharedPreferences prefs;
    private Map<String, List<Post>> dataCache = new HashMap<>();
    private Map<String, Integer> scrollPositionCache = new HashMap<>();

    // Like manager
    private PostLikeManager likeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_section_detail);

        try {
            // 初始化数据
            sectionId = getIntent().getIntExtra("section_id", -1);
            if (sectionId == -1) {
                Toast.makeText(this, "板块参数错误", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // 初始化SharedPreferences和管理器
            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            likeManager = new PostLikeManager(this);

            // 恢复上次选中的标签
            currentTab = prefs.getString(KEY_CURRENT_TAB + sectionId, "hot");

            // 初始化Views
            initViews();
            setupRecyclerView();
            setupClickListeners();

            // 设置当前标签并加载数据
            switchTab(currentTab);
            loadSectionInfo();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "页面加载失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveScrollPosition();
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreScrollPosition();
    }

    private void initViews() {
        tvBack = findViewById(R.id.tv_back);
        ivGameIcon = findViewById(R.id.iv_game_icon);
        tvSectionName = findViewById(R.id.tv_section_name);
        tvSectionDescription = findViewById(R.id.tv_section_description);
        tvPost = findViewById(R.id.tv_post);
        tvHot = findViewById(R.id.tv_hot);
        tvLatest = findViewById(R.id.tv_latest);

        // 修改这里：给RecyclerView一个新的ID，或者在布局中添加一个RecyclerView
        // 方案1：如果你已经按照上面的布局修改了，那么这行代码保持不变
        recyclerView = findViewById(R.id.vp_posts);

        // 方案2：如果你要保持ViewPager2，那么需要在布局中添加一个新的RecyclerView
        // recyclerView = findViewById(R.id.rv_posts); // 需要在布局中添加这个ID的RecyclerView

        postList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        postAdapter = new PostAdapter(this, postList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(postAdapter);

        // 设置监听器
        postAdapter.setOnPostClickListener(this);
        postAdapter.setOnPostLikeListener(this);

        // 设置滚动监听，实现懒加载
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) { // 向下滚动
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

                        if (!isLoading && hasMore && (visibleItemCount + firstVisibleItem) >= totalItemCount - 2) {
                            loadMorePosts();
                        }
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        // 返回按钮
        tvBack.setOnClickListener(v -> finish());

        // 发帖按钮
        tvPost.setOnClickListener(v -> {
            Intent intent = new Intent(this, NewPostActivity.class);
            intent.putExtra("section_id", sectionId);
            if (sectionInfo != null) {
                intent.putExtra("section_name", sectionInfo.getSectionName());
            }
            startActivity(intent);
        });

        // 最热标签
        tvHot.setOnClickListener(v -> {
            if (!"hot".equals(currentTab)) {
                saveScrollPosition();
                switchTab("hot");
                loadPostDataWithCache("hot");
                saveCurrentTab("hot");
            }
        });

        // 最新标签
        tvLatest.setOnClickListener(v -> {
            if (!"latest".equals(currentTab)) {
                saveScrollPosition();
                switchTab("latest");
                loadPostDataWithCache("latest");
                saveCurrentTab("latest");
            }
        });
    }

    private void switchTab(String tab) {
        // 重置所有标签样式
        tvHot.setTextColor(Color.parseColor("#999999"));
        tvHot.setTypeface(null, Typeface.NORMAL);
        tvLatest.setTextColor(Color.parseColor("#999999"));
        tvLatest.setTypeface(null, Typeface.NORMAL);

        currentTab = tab;

        // 设置选中标签样式
        if ("hot".equals(tab)) {
            tvHot.setTextColor(Color.parseColor("#333333"));
            tvHot.setTypeface(null, Typeface.BOLD);
        } else {
            tvLatest.setTextColor(Color.parseColor("#333333"));
            tvLatest.setTypeface(null, Typeface.BOLD);
        }
    }

    private void loadSectionInfo() {
        String url = ApiConstants.buildUrlWithParam(ApiConstants.GET_SECTION_DETAIL, sectionId.toString());

        ApiService.getInstance().getRequest(url, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        // 解析响应
                        ApiResponse<Section> apiResponse = parseApiResponse(response, Section.class);
                        if (apiResponse != null && apiResponse.getCode() == 200) {
                            sectionInfo = apiResponse.getData();
                            updateSectionUI();

                            // 如果有关联的游戏ID，加载游戏信息
                            if (sectionInfo.getGameId() != null) {
                                loadGameInfo(sectionInfo.getGameId());
                            }

                            // 加载帖子数据
                            loadPostDataWithCache(currentTab);
                        } else {
                            Toast.makeText(SectionDetailActivity.this, "加载板块信息失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parse section info error: " + e.getMessage(), e);
                        Toast.makeText(SectionDetailActivity.this, "解析板块信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Load section info error: " + error);
                    Toast.makeText(SectionDetailActivity.this, "加载板块信息失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadGameInfo(Integer gameId) {
        String url = ApiConstants.buildUrlWithParam(ApiConstants.GET_GAME_DETAIL, gameId.toString());

        ApiService.getInstance().getRequest(url, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        ApiResponse<Game> apiResponse = parseApiResponse(response, Game.class);
                        if (apiResponse != null && apiResponse.getCode() == 200) {
                            gameInfo = apiResponse.getData();
                            updateGameIcon();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parse game info error: " + e.getMessage(), e);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Load game info error: " + error);
            }
        });
    }

    private void updateSectionUI() {
        if (sectionInfo == null) return;

        tvSectionName.setText(sectionInfo.getSectionName() != null ? sectionInfo.getSectionName() : "未知板块");

        if (sectionInfo.getSectionDescription() != null && !sectionInfo.getSectionDescription().trim().isEmpty()) {
            tvSectionDescription.setText(sectionInfo.getSectionDescription());
            tvSectionDescription.setVisibility(View.VISIBLE);
        } else {
            tvSectionDescription.setVisibility(View.GONE);
        }
    }

    private void updateGameIcon() {
        if (gameInfo != null && gameInfo.getGameIcon() != null) {
            ImageUtils.loadUserAvatar(this, ivGameIcon, gameInfo.getGameIcon());
        }
    }

    private void loadPostDataWithCache(String type) {
        // 检查是否有缓存数据
        List<Post> cachedData = dataCache.get(type);
        if (cachedData != null && !cachedData.isEmpty()) {
            // 使用缓存数据
            postList.clear();
            postList.addAll(cachedData);
            postAdapter.notifyDataSetChanged();
            restoreScrollPosition();

            // 可选：在后台刷新数据
            refreshDataInBackground(type);
        } else {
            // 没有缓存，加载新数据
            loadPosts(type, true);
        }
    }

    private void refreshDataInBackground(String type) {
        loadPostsFromServer(type, 1, new ApiCallback<List<Post>>() {
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
                // 后台刷新失败不显示错误，保持用户体验
                Log.e(TAG, "Background refresh failed: " + error);
            }
        });
    }

    private void loadPosts(String type, boolean reset) {
        if (isLoading) return;

        if (reset) {
            currentPage = 1;
            hasMore = true;
        }

        isLoading = true;

        loadPostsFromServer(type, currentPage, new ApiCallback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                runOnUiThread(() -> {
                    isLoading = false;

                    if (reset) {
                        // 更新缓存
                        dataCache.put(type, new ArrayList<>(posts));

                        postList.clear();
                        postList.addAll(posts);
                        postAdapter.notifyDataSetChanged();
                        restoreScrollPosition();
                    } else {
                        // 添加到现有列表
                        int oldSize = postList.size();
                        postList.addAll(posts);
                        postAdapter.notifyItemRangeInserted(oldSize, posts.size());

                        // 更新缓存
                        List<Post> cachedData = dataCache.get(type);
                        if (cachedData != null) {
                            cachedData.addAll(posts);
                        }
                    }

                    // 判断是否还有更多数据
                    hasMore = posts.size() == PAGE_SIZE;
                    if (hasMore) {
                        currentPage++;
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isLoading = false;
                    Toast.makeText(SectionDetailActivity.this, "加载帖子失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadMorePosts() {
        if (!isLoading && hasMore) {
            loadPosts(currentTab, false);
        }
    }

    private void loadPostsFromServer(String type, int page, ApiCallback<List<Post>> callback) {
        String url = getPostsUrl(type, page);

        ApiService.getInstance().getRequest(url, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    ApiResponse<List<Post>> apiResponse = parseApiListResponse(response, Post.class);
                    if (apiResponse != null && apiResponse.getCode() == 200) {
                        List<Post> posts = apiResponse.getData();
                        callback.onSuccess(posts != null ? posts : new ArrayList<>());
                    } else {
                        callback.onError("服务器返回错误");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Parse posts error: " + e.getMessage(), e);
                    callback.onError("解析数据失败");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private String getPostsUrl(String type, int page) {
        String baseUrl;
        if ("hot".equals(type)) {
            baseUrl = ApiConstants.GET_HOT_POSTS + "?limit=" + PAGE_SIZE + "&page=" + page;
            // 如果是热门帖子，也需要按板块过滤
            baseUrl += "&sectionId=" + sectionId;
        } else {
            baseUrl = ApiConstants.buildUrlWithParam(ApiConstants.GET_POSTS_BY_SECTION, sectionId.toString());
            baseUrl += "?page=" + page + "&size=" + PAGE_SIZE + "&sort=latest";
        }
        return baseUrl;
    }

    private void saveCurrentTab(String tab) {
        prefs.edit().putString(KEY_CURRENT_TAB + sectionId, tab).apply();
    }

    private void saveScrollPosition() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null) {
            int position = layoutManager.findFirstVisibleItemPosition();
            scrollPositionCache.put(currentTab, position);
            prefs.edit().putInt(KEY_SCROLL_POSITION + sectionId + "_" + currentTab, position).apply();
        }
    }

    private void restoreScrollPosition() {
        Integer position = scrollPositionCache.get(currentTab);
        if (position == null) {
            position = prefs.getInt(KEY_SCROLL_POSITION + sectionId + "_" + currentTab, 0);
        }

        if (position > 0 && position < postList.size()) {
            recyclerView.scrollToPosition(position);
        }
    }

    // PostAdapter.OnPostClickListener 实现
    @Override
    public void onPostClick(Post post, int position) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post_id", post.getPostId());
        startActivity(intent);
    }

    @Override
    public void onUserClick(Post post, int position) {
        Toast.makeText(this, "点击了用户: " + post.getNickName(), Toast.LENGTH_SHORT).show();
        // 可以跳转到用户资料页
    }
    @Override
    public void onDeleteClick(Post post, int position) {
        // 暂时不需要删除功能，可以空实现
    }


    @Override
    public void onCommentClick(Post post, int position) {
        // 跳转到帖子详情页的评论区
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post_id", post.getPostId());
        intent.putExtra("show_comments", true);
        startActivity(intent);
    }

    @Override
    public void onViewClick(Post post, int position) {
        Toast.makeText(this, "浏览量: " + post.getViewCount(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMoreClick(Post post, int position) {
        Toast.makeText(this, "点击了更多", Toast.LENGTH_SHORT).show();
        // 可以显示更多操作菜单
    }

    // PostAdapter.OnPostLikeListener 实现
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
                Log.e(TAG, "Like operation failed: " + errorMessage);
            }
        });
    }

    // 辅助方法：解析API响应
    private <T> ApiResponse<T> parseApiResponse(String jsonResponse, Class<T> dataClass) {
        try {
            Gson gson = ApiService.getInstance().getGson(); // 使用 ApiService 的 Gson 实例
            Type type = TypeToken.getParameterized(ApiResponse.class, dataClass).getType();
            return gson.fromJson(jsonResponse, type);
        } catch (Exception e) {
            Log.e(TAG, "Parse API response error: " + e.getMessage(), e);
            return null;
        }
    }

    private <T> ApiResponse<List<T>> parseApiListResponse(String jsonResponse, Class<T> dataClass) {
        try {
            Gson gson = ApiService.getInstance().getGson(); // 使用 ApiService 的 Gson 实例
            Type listType = TypeToken.getParameterized(List.class, dataClass).getType();
            Type responseType = TypeToken.getParameterized(ApiResponse.class, listType).getType();
            return gson.fromJson(jsonResponse, responseType);
        } catch (Exception e) {
            Log.e(TAG, "Parse API list response error: " + e.getMessage(), e);
            return null;
        }
    }

    // API响应包装类
    private static class ApiResponse<T> {
        private int code;
        private String msg;
        private T data;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}