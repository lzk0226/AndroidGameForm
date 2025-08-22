/*
package com.app.gameform.Activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.gameform.R;
import com.app.gameform.domain.Game;
import com.app.gameform.domain.GameType;
import com.app.gameform.domain.Post;
import com.app.gameform.domain.Section;
import com.app.gameform.network.ApiConstants;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchResultActivity extends AppCompatActivity {

    private EditText etSearch;
    private TextView btnSearch;
    private TextView tabAll, tabPosts, tabGames, tabBoards, tabGameType;
    private View tabIndicator;
    private LinearLayout layoutSearchStats;
    private TextView tvSearchStats, tvSearchKeyword;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvSearchResults;
    private LinearLayout layoutEmpty, layoutLoading;

    private String currentQuery = "";
    private String activeTab = "all";
    private SearchResultAdapter adapter;
    private List<SearchResultItem> searchResults = new ArrayList<>();
    private boolean isLoading = false;

    private OkHttpClient okHttpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        initViews();
        initData();
        initListeners();

        // 获取传入的搜索关键词
        String query = getIntent().getStringExtra("query");
        if (!TextUtils.isEmpty(query)) {
            etSearch.setText(query);
            performSearch(query);
        }
    }

    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);

        tabAll = findViewById(R.id.tab_all);
        tabPosts = findViewById(R.id.tab_posts);
        tabGames = findViewById(R.id.tab_games);
        tabBoards = findViewById(R.id.tab_boards);
        tabGameType = findViewById(R.id.tab_game_type);
        tabIndicator = findViewById(R.id.tab_indicator);

        layoutSearchStats = findViewById(R.id.layout_search_stats);
        tvSearchStats = findViewById(R.id.tv_search_stats);
        tvSearchKeyword = findViewById(R.id.tv_search_keyword);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        rvSearchResults = findViewById(R.id.rv_search_results);
        layoutEmpty = findViewById(R.id.layout_empty);
        layoutLoading = findViewById(R.id.layout_loading);
    }

    private void initData() {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        adapter = new SearchResultAdapter(searchResults, this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);
    }

    private void initListeners() {
        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!TextUtils.isEmpty(query)) {
                performSearch(query);
                hideKeyboard();
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = etSearch.getText().toString().trim();
                if (!TextUtils.isEmpty(query)) {
                    performSearch(query);
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });

        tabAll.setOnClickListener(v -> switchTab("all", tabAll));
        tabPosts.setOnClickListener(v -> switchTab("posts", tabPosts));
        tabGames.setOnClickListener(v -> switchTab("games", tabGames));
        tabBoards.setOnClickListener(v -> switchTab("boards", tabBoards));
        tabGameType.setOnClickListener(v -> switchTab("gameTypes", tabGameType));

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!TextUtils.isEmpty(currentQuery)) {
                performSearch(currentQuery);
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void switchTab(String tabKey, TextView tabView) {
        if (activeTab.equals(tabKey)) return;

        // 重置所有标签样式
        resetTabStyles();

        // 设置当前标签为激活状态
        activeTab = tabKey;
        tabView.setTextColor(getColor(R.color.tab_active_color));
        tabView.setTypeface(null, android.graphics.Typeface.BOLD);

        // 移动指示器
        moveIndicator(tabView);

        // 过滤显示结果
        filterResults();
    }

    private void resetTabStyles() {
        int inactiveColor = getColor(R.color.tab_inactive_color);
        tabAll.setTextColor(inactiveColor);
        tabPosts.setTextColor(inactiveColor);
        tabGames.setTextColor(inactiveColor);
        tabBoards.setTextColor(inactiveColor);
        tabGameType.setTextColor(inactiveColor);

        tabAll.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabPosts.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabGames.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabBoards.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabGameType.setTypeface(null, android.graphics.Typeface.NORMAL);
    }

    private void moveIndicator(TextView activeTabView) {
        // 简单的指示器移动，可以添加动画效果
        ViewGroup.LayoutParams params = tabIndicator.getLayoutParams();
        tabIndicator.post(() -> {
            int[] location = new int[2];
            activeTabView.getLocationInWindow(location);
            int[] containerLocation = new int[2];
            ((View) tabIndicator.getParent()).getLocationInWindow(containerLocation);

            tabIndicator.setX(location[0] - containerLocation[0]);
            params.width = activeTabView.getWidth();
            tabIndicator.setLayoutParams(params);
        });
    }

    private void performSearch(String query) {
        if (isLoading) return;

        currentQuery = query;
        showLoading(true);

        // 清空之前的结果
        searchResults.clear();
        adapter.notifyDataSetChanged();

        // 执行搜索
        searchAllTypes(query);
    }

    private void searchAllTypes(String query) {
        final int[] completedRequests = {0};
        final int totalRequests = 4;
        final List<SearchResultItem> allResults = new ArrayList<>();

        // 搜索游戏
        searchGames(query, new SearchCallback() {
            @Override
            public void onSuccess(List<SearchResultItem> results) {
                synchronized (allResults) {
                    allResults.addAll(results);
                    completedRequests[0]++;
                    if (completedRequests[0] == totalRequests) {
                        runOnUiThread(() -> handleSearchComplete(allResults));
                    }
                }
            }

            @Override
            public void onError(String error) {
                synchronized (allResults) {
                    completedRequests[0]++;
                    if (completedRequests[0] == totalRequests) {
                        runOnUiThread(() -> handleSearchComplete(allResults));
                    }
                }
            }
        });

        // 搜索帖子
        searchPosts(query, new SearchCallback() {
            @Override
            public void onSuccess(List<SearchResultItem> results) {
                synchronized (allResults) {
                    allResults.addAll(results);
                    completedRequests[0]++;
                    if (completedRequests[0] == totalRequests) {
                        runOnUiThread(() -> handleSearchComplete(allResults));
                    }
                }
            }

            @Override
            public void onError(String error) {
                synchronized (allResults) {
                    completedRequests[0]++;
                    if (completedRequests[0] == totalRequests) {
                        runOnUiThread(() -> handleSearchComplete(allResults));
                    }
                }
            }
        });

        // 搜索版块
        searchSections(query, new SearchCallback() {
            @Override
            public void onSuccess(List<SearchResultItem> results) {
                synchronized (allResults) {
                    allResults.addAll(results);
                    completedRequests[0]++;
                    if (completedRequests[0] == totalRequests) {
                        runOnUiThread(() -> handleSearchComplete(allResults));
                    }
                }
            }

            @Override
            public void onError(String error) {
                synchronized (allResults) {
                    completedRequests[0]++;
                    if (completedRequests[0] == totalRequests) {
                        runOnUiThread(() -> handleSearchComplete(allResults));
                    }
                }
            }
        });

        // 搜索游戏类型
        searchGameTypes(query, new SearchCallback() {
            @Override
            public void onSuccess(List<SearchResultItem> results) {
                synchronized (allResults) {
                    allResults.addAll(results);
                    completedRequests[0]++;
                    if (completedRequests[0] == totalRequests) {
                        runOnUiThread(() -> handleSearchComplete(allResults));
                    }
                }
            }

            @Override
            public void onError(String error) {
                synchronized (allResults) {
                    completedRequests[0]++;
                    if (completedRequests[0] == totalRequests) {
                        runOnUiThread(() -> handleSearchComplete(allResults));
                    }
                }
            }
        });
    }

    private void searchGames(String query, SearchCallback callback) {
        String url = ApiConstants.BASE_URL + "/user/game/search?name=" + query;
        Request request = new Request.Builder().url(url).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseBody);

                    List<SearchResultItem> results = new ArrayList<>();
                    if (jsonObject.getInt("code") == 200) {
                        JSONArray dataArray = jsonObject.getJSONArray("data");
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject gameJson = dataArray.getJSONObject(i);
                            Game game = parseGame(gameJson);
                            results.add(new SearchResultItem(SearchResultItem.TYPE_GAME, game));
                        }
                    }
                    callback.onSuccess(results);
                } catch (JSONException e) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    private void searchPosts(String query, SearchCallback callback) {
        String url = ApiConstants.BASE_URL + "/user/post/search?title=" + query;
        Request request = new Request.Builder().url(url).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseBody);

                    List<SearchResultItem> results = new ArrayList<>();
                    if (jsonObject.getInt("code") == 200) {
                        JSONArray dataArray = jsonObject.getJSONArray("data");
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject postJson = dataArray.getJSONObject(i);
                            Post post = parsePost(postJson);
                            results.add(new SearchResultItem(SearchResultItem.TYPE_POST, post));
                        }
                    }
                    callback.onSuccess(results);
                } catch (JSONException e) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    private void searchSections(String query, SearchCallback callback) {
        String url = ApiConstants.BASE_URL + "/user/section/search?name=" + query;
        Request request = new Request.Builder().url(url).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseBody);

                    List<SearchResultItem> results = new ArrayList<>();
                    if (jsonObject.getInt("code") == 200) {
                        JSONArray dataArray = jsonObject.getJSONArray("data");
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject sectionJson = dataArray.getJSONObject(i);
                            Section section = parseSection(sectionJson);
                            results.add(new SearchResultItem(SearchResultItem.TYPE_SECTION, section));
                        }
                    }
                    callback.onSuccess(results);
                } catch (JSONException e) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    private void searchGameTypes(String query, SearchCallback callback) {
        String url = ApiConstants.BASE_URL + "/user/gameType/search?name=" + query;
        Request request = new Request.Builder().url(url).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseBody);

                    List<SearchResultItem> results = new ArrayList<>();
                    if (jsonObject.getInt("code") == 200) {
                        JSONArray dataArray = jsonObject.getJSONArray("data");
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject gameTypeJson = dataArray.getJSONObject(i);
                            GameType gameType = parseGameType(gameTypeJson);
                            results.add(new SearchResultItem(SearchResultItem.TYPE_GAME_TYPE, gameType));
                        }
                    }
                    callback.onSuccess(results);
                } catch (JSONException e) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    private void handleSearchComplete(List<SearchResultItem> allResults) {
        searchResults.clear();
        searchResults.addAll(allResults);

        updateTabCounts();
        filterResults();
        updateSearchStats();
        showLoading(false);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void updateTabCounts() {
        int gameCount = 0, postCount = 0, sectionCount = 0, gameTypeCount = 0;

        for (SearchResultItem item : searchResults) {
            switch (item.getType()) {
                case SearchResultItem.TYPE_GAME:
                    gameCount++;
                    break;
                case SearchResultItem.TYPE_POST:
                    postCount++;
                    break;
                case SearchResultItem.TYPE_SECTION:
                    sectionCount++;
                    break;
                case SearchResultItem.TYPE_GAME_TYPE:
                    gameTypeCount++;
                    break;
            }
        }

        tabPosts.setText("帖子 (" + postCount + ")");
        tabGames.setText("游戏 (" + gameCount + ")");
        tabBoards.setText("版块 (" + sectionCount + ")");
        tabGameType.setText("游戏类型 (" + gameTypeCount + ")");
    }

    private void filterResults() {
        List<SearchResultItem> filteredResults = new ArrayList<>();

        if (activeTab.equals("all")) {
            filteredResults.addAll(searchResults);
        } else {
            int targetType = getTypeByTab(activeTab);
            for (SearchResultItem item : searchResults) {
                if (item.getType() == targetType) {
                    filteredResults.add(item);
                }
            }
        }

        adapter.updateResults(filteredResults);

        if (filteredResults.isEmpty() && !currentQuery.isEmpty()) {
            showEmpty(true);
        } else {
            showEmpty(false);
        }
    }

    private int getTypeByTab(String tab) {
        switch (tab) {
            case "games":
                return SearchResultItem.TYPE_GAME;
            case "posts":
                return SearchResultItem.TYPE_POST;
            case "boards":
                return SearchResultItem.TYPE_SECTION;
            case "gameTypes":
                return SearchResultItem.TYPE_GAME_TYPE;
            default:
                return -1;
        }
    }

    private void updateSearchStats() {
        int totalCount = searchResults.size();
        tvSearchStats.setText("找到 " + totalCount + " 个结果");
        tvSearchKeyword.setText("关于 \"" + currentQuery + "\"");
        layoutSearchStats.setVisibility(totalCount > 0 ? View.VISIBLE : View.GONE);
    }

    private void showLoading(boolean show) {
        isLoading = show;
        layoutLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        rvSearchResults.setVisibility(show ? View.GONE : View.VISIBLE);
        if (show) {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmpty(boolean show) {
        layoutEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        rvSearchResults.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    // JSON解析方法
    private Game parseGame(JSONObject json) throws JSONException {
        Game game = new Game();
        game.setGameId(json.optInt("gameId"));
        game.setGameName(json.optString("gameName"));
        game.setGameDescription(json.optString("gameDescription"));
        game.setGameIcon(json.optString("gameIcon"));
        game.setGameTypeId(json.optInt("gameTypeId"));
        game.setGameTypeName(json.optString("gameTypeName"));
        return game;
    }

    private Post parsePost(JSONObject json) throws JSONException {
        Post post = new Post();
        post.setPostId(json.optInt("postId"));
        post.setPostTitle(json.optString("postTitle"));
        post.setPostContent(json.optString("postContent"));
        post.setUserId(json.optLong("userId"));
        post.setSectionId(json.optInt("sectionId"));
        post.setSectionName(json.optString("sectionName"));
        post.setNickName(json.optString("nickName"));
        post.setLikeCount(json.optInt("likeCount"));
        post.setViewCount(json.optInt("viewCount"));
        post.setCommentCount(json.optInt("commentCount"));
        post.setCreateBy(json.optString("createBy"));

        String createTimeStr = json.optString("createTime");
        if (!TextUtils.isEmpty(createTimeStr)) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                post.setCreateTime(sdf.parse(createTimeStr));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return post;
    }

    private Section parseSection(JSONObject json) throws JSONException {
        Section section = new Section();
        section.setSectionId(json.optInt("sectionId"));
        section.setSectionName(json.optString("sectionName"));
        section.setSectionDescription(json.optString("sectionDescription"));
        section.setGameId(json.optInt("gameId"));
        section.setGameName(json.optString("gameName"));
        return section;
    }

    private GameType parseGameType(JSONObject json) throws JSONException {
        GameType gameType = new GameType();
        gameType.setTypeId(json.optInt("typeId"));
        gameType.setTypeName(json.optString("typeName"));
        return gameType;
    }

    // 搜索回调接口
    interface SearchCallback {
        void onSuccess(List<SearchResultItem> results);
        void onError(String error);
    }

    // 搜索结果项
    static class SearchResultItem {
        public static final int TYPE_GAME = 1;
        public static final int TYPE_POST = 2;
        public static final int TYPE_SECTION = 3;
        public static final int TYPE_GAME_TYPE = 4;

        private int type;
        private Object data;

        public SearchResultItem(int type, Object data) {
            this.type = type;
            this.data = data;
        }

        public int getType() {
            return type;
        }

        public Object getData() {
            return data;
        }
    }

    // 搜索结果适配器
    static class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<SearchResultItem> results;
        private Context context;

        public SearchResultAdapter(List<SearchResultItem> results, Context context) {
            this.results = results;
            this.context = context;
        }

        public void updateResults(List<SearchResultItem> newResults) {
            this.results.clear();
            this.results.addAll(newResults);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return results.get(position).getType();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(context);

            switch (viewType) {
                case SearchResultItem.TYPE_GAME:
                    return new GameViewHolder(inflater.inflate(R.layout.item_search_game, parent, false));
                case SearchResultItem.TYPE_POST:
                    return new PostViewHolder(inflater.inflate(R.layout.item_search_post, parent, false));
                case SearchResultItem.TYPE_SECTION:
                    return new SectionViewHolder(inflater.inflate(R.layout.item_search_section, parent, false));
                case SearchResultItem.TYPE_GAME_TYPE:
                    return new GameTypeViewHolder(inflater.inflate(R.layout.item_search_game_type, parent, false));
                default:
                    return new GameViewHolder(inflater.inflate(R.layout.item_search_game, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            SearchResultItem item = results.get(position);

            switch (item.getType()) {
                case SearchResultItem.TYPE_GAME:
                    ((GameViewHolder) holder).bind((Game) item.getData());
                    break;
                case SearchResultItem.TYPE_POST:
                    ((PostViewHolder) holder).bind((Post) item.getData());
                    break;
                case SearchResultItem.TYPE_SECTION:
                    ((SectionViewHolder) holder).bind((Section) item.getData());
                    break;
                case SearchResultItem.TYPE_GAME_TYPE:
                    ((GameTypeViewHolder) holder).bind((GameType) item.getData());
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        // ViewHolder类（简化版本，你需要创建对应的布局文件）
        static class GameViewHolder extends RecyclerView.ViewHolder {
            TextView gameName, gameDesc, gameType;
            ImageView gameIcon;

            public GameViewHolder(@NonNull View itemView) {
                super(itemView);
                gameName = itemView.findViewById(R.id.tv_game_name);
                gameDesc = itemView.findViewById(R.id.tv_game_desc);
                gameType = itemView.findViewById(R.id.tv_game_type);
                gameIcon = itemView.findViewById(R.id.iv_game_icon);
            }

            public void bind(Game game) {
                gameName.setText(game.getGameName());
                gameDesc.setText(game.getGameDescription());
                gameType.setText(game.getGameTypeName());

                if (!TextUtils.isEmpty(game.getGameIcon())) {
                    String iconUrl = ApiConstants.BASE_URL + game.getGameIcon();
                    Glide.with(itemView.getContext())
                            .load(iconUrl)
                            .placeholder(R.drawable.placeholder_game)
                            .error(R.drawable.placeholder_game)
                            .into(gameIcon);
                }

                itemView.setOnClickListener(v -> {
                    // 跳转到游戏详情页
                    Intent intent = new Intent(itemView.getContext(), GameDetailActivity.class);
                    intent.putExtra("gameId", game.getGameId());
                    itemView.getContext().startActivity(intent);
                });
            }
        }

        static class PostViewHolder extends RecyclerView.ViewHolder {
            TextView postTitle, postContent, postAuthor, postTime, postStats;

            public PostViewHolder(@NonNull View itemView) {
                super(itemView);
                postTitle = itemView.findViewById(R.id.tv_post_title);
                postContent = itemView.findViewById(R.id.tv_post_content);
                postAuthor = itemView.findViewById(R.id.tv_post_author);
                postTime = itemView.findViewById(R.id.tv_post_time);
                postStats = itemView.findViewById(R.id.tv_post_stats);
            }

            public void bind(Post post) {
                postTitle.setText(post.getPostTitle());
                postContent.setText(truncateText(post.getPostContent(), 100));
                postAuthor.setText(post.getCreateBy());
                postTime.setText(formatTime(post.getCreateTime()));
                postStats.setText(String.format("👍 %d  👁 %d",
                        post.getLikeCount() != null ? post.getLikeCount() : 0,
                        post.getViewCount() != null ? post.getViewCount() : 0));

                itemView.setOnClickListener(v -> {
                    // 跳转到帖子详情页
                    Intent intent = new Intent(itemView.getContext(), PostDetailActivity.class);
                    intent.putExtra("postId", post.getPostId());
                    itemView.getContext().startActivity(intent);
                });
            }
        }

        static class SectionViewHolder extends RecyclerView.ViewHolder {
            TextView sectionName, sectionDesc, sectionGame;

            public SectionViewHolder(@NonNull View itemView) {
                super(itemView);
                sectionName = itemView.findViewById(R.id.tv_section_name);
                sectionDesc = itemView.findViewById(R.id.tv_section_desc);
                sectionGame = itemView.findViewById(R.id.tv_section_game);
            }

            public void bind(Section section) {
                sectionName.setText(section.getSectionName());
                sectionDesc.setText(section.getSectionDescription());
                sectionGame.setText("🎮 " + section.getGameName());

                itemView.setOnClickListener(v -> {
                    // 跳转到版块页面
                    Intent intent = new Intent(itemView.getContext(), SectionActivity.class);
                    intent.putExtra("sectionId", section.getSectionId());
                    itemView.getContext().startActivity(intent);
                });
            }
        }

        static class GameTypeViewHolder extends RecyclerView.ViewHolder {
            TextView gameTypeName;

            public GameTypeViewHolder(@NonNull View itemView) {
                super(itemView);
                gameTypeName = itemView.findViewById(R.id.tv_game_type_name);
            }

            public void bind(GameType gameType) {
                gameTypeName.setText(gameType.getTypeName());

                itemView.setOnClickListener(v -> {
                    // 跳转到游戏类型页面
                    Intent intent = new Intent(itemView.getContext(), GameTypeActivity.class);
                    intent.putExtra("gameTypeId", gameType.getTypeId());
                    itemView.getContext().startActivity(intent);
                });
            }
        }

        // 辅助方法
        private static String truncateText(String text, int maxLength) {
            if (TextUtils.isEmpty(text)) return "";
            return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
        }

        private static String formatTime(Date date) {
            if (date == null) return "";

            long diff = System.currentTimeMillis() - date.getTime();
            long minutes = diff / (1000 * 60);
            long hours = diff / (1000 * 60 * 60);
            long days = diff / (1000 * 60 * 60 * 24);

            if (minutes < 1) return "刚刚";
            if (minutes < 60) return minutes + "分钟前";
            if (hours < 24) return hours + "小时前";
            if (days < 30) return days + "天前";

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.format(date);
        }
    }

    // 静态方法启动Activity
    public static void start(Context context, String query) {
        Intent intent = new Intent(context, SearchResultActivity.class);
        intent.putExtra("query", query);
        context.startActivity(intent);
    }
}*/
