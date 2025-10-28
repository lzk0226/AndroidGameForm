package com.app.gameform.Activity;

import static com.app.gameform.network.ApiConstants.*;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.adapter.CommentAdapter;
import com.app.gameform.domain.Comment;
import com.app.gameform.domain.Post;
import com.app.gameform.manager.CommentLikeManager;
import com.app.gameform.manager.CommentManager;
import com.app.gameform.manager.PostFavoriteManager;
import com.app.gameform.manager.SharedPrefManager;
import com.app.gameform.manager.UserFollowManager;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.ApiService;
import com.app.gameform.utils.ImageUtils;
import com.app.gameform.utils.TimeUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 帖子详情页 - 修复版
 * 修复点赞功能：
 * 1. 点赞后立即更新UI和点赞数
 * 2. 退出重进后正确显示点赞状态
 * 3. 参照Vue网页端的实现逻辑
 */
public class PostDetailActivity extends AppCompatActivity {

    private static final String TAG = "PostDetailActivity";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // 视图组件
    private NestedScrollView scrollContent;
    private WebView webViewContent;
    private TextView tvPostTitle, tvUserName, tvTime;
    private TextView tvLikeCount, tvCommentCount, tvViewCount;
    private TextView tvReplyHint;
    private CircleImageView ivUserAvatar;
    private ImageView ivLike, ivFavorite, ivFollow;
    private ImageView ivCancelReply;
    private EditText etCommentInput;
    private ImageView ivSendComment;
    private RecyclerView recyclerComments;
    private View loadingView, errorView;
    private LinearLayout layoutStats;
    private LinearLayout layoutReplyHint;

    // 数据
    private Post currentPost;
    private List<Comment> commentList = new ArrayList<>();
    private CommentAdapter commentAdapter;
    private int postId;
    private String authToken;
    private boolean isLoggedIn = false;

    // 状态标识
    private boolean hasLiked = false;
    private boolean hasFavorited = false;
    private boolean hasFollowed = false;
    private boolean isOwnPost = false;

    // 加载状态标识 - 防止重复点击
    private boolean likeLoading = false;
    private boolean favoriteLoading = false;

    // 管理器
    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();
    private PostFavoriteManager favoriteManager;
    private CommentManager commentManager;
    private SharedPrefManager sharedPrefManager;
    private CommentLikeManager commentLikeManager;
    private UserFollowManager followManager;  // 新增：关注管理器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        try {
            initializeManagers();
            initializeViews();

            if (!initializeData()) {
                return;
            }

            setupListeners();
            setupCommentInputWatcher();
            loadPostDetails();
        } catch (Exception e) {
            Log.e(TAG, "onCreate error: ", e);
            showToast("页面加载失败");
            finish();
        }
    }

    /**
     * 初始化管理器
     */
    private void initializeManagers() {
        sharedPrefManager = SharedPrefManager.getInstance(this);
        favoriteManager = new PostFavoriteManager(this);
        commentManager = new CommentManager(this);
        commentLikeManager = new CommentLikeManager(this);
        followManager = new UserFollowManager(this);  // 新增：初始化关注管理器
    }

    /**
     * 初始化视图
     */
    private void initializeViews() {
        // 主容器
        scrollContent = findViewById(R.id.scroll_content);
        loadingView = findViewById(R.id.loading_view);
        errorView = findViewById(R.id.error_view);

        // 返回按钮
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        // 帖子内容区域
        ivUserAvatar = findViewById(R.id.iv_user_avatar);
        tvUserName = findViewById(R.id.tv_user_name);
        tvTime = findViewById(R.id.tv_time);
        tvPostTitle = findViewById(R.id.tv_post_title);
        webViewContent = findViewById(R.id.web_view_content);
        ivFollow = findViewById(R.id.iv_follow);

        setupWebView();

        // 统计区域
        layoutStats = findViewById(R.id.layout_stats);
        ivLike = findViewById(R.id.iv_like);
        tvLikeCount = findViewById(R.id.tv_like_count);
        tvCommentCount = findViewById(R.id.tv_comment_count);
        tvViewCount = findViewById(R.id.tv_view_count);
        ivFavorite = findViewById(R.id.iv_favorite);

        // 评论区域
        recyclerComments = findViewById(R.id.recycler_comments);
        setupCommentsRecyclerView();

        // 输入区域
        etCommentInput = findViewById(R.id.et_comment_input);
        ivSendComment = findViewById(R.id.iv_send_comment);
    }

    /**
     * 设置WebView
     */
    private void setupWebView() {
        WebSettings webSettings = webViewContent.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webViewContent.setBackgroundColor(0x00000000);
        webViewContent.setWebChromeClient(new WebChromeClient());
    }

    /**
     * 设置评论列表
     */
    private void setupCommentsRecyclerView() {
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerComments.setNestedScrollingEnabled(false);

        commentAdapter = new CommentAdapter(
                commentList,
                this::onCommentLikeClicked,
                this::onCommentReplyClicked
        );
        recyclerComments.setAdapter(commentAdapter);
    }

    /**
     * 设置输入框文本监听器
     */
    private void setupCommentInputWatcher() {
        etCommentInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (commentManager.isReplying()) {
                    String currentText = s.toString();
                    if (!commentManager.containsReplyPrefix(currentText)) {
                        Log.d(TAG, "用户删除了回复前缀，取消回复状态");
                        cancelReply();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * 初始化数据
     */
    private boolean initializeData() {
        try {
            postId = getIntent().getIntExtra("post_id", -1);
            if (postId == -1) {
                showToast("帖子ID无效");
                finish();
                return false;
            }

            authToken = sharedPrefManager.getToken();
            isLoggedIn = !TextUtils.isEmpty(authToken);

            Log.d(TAG, "postId: " + postId + ", isLoggedIn: " + isLoggedIn);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "initializeData error: ", e);
            showToast("数据初始化失败");
            finish();
            return false;
        }
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 点赞
        findViewById(R.id.layout_like).setOnClickListener(v -> toggleLike());

        // 评论（滚动到评论区）
        findViewById(R.id.layout_comment).setOnClickListener(v -> scrollToComments());

        // 收藏
        findViewById(R.id.layout_favorite).setOnClickListener(v -> toggleFavorite());

        // 关注
        ivFollow.setOnClickListener(v -> toggleFollow());

        // 用户头像点击
        ivUserAvatar.setOnClickListener(v -> {
            if (currentPost != null && currentPost.getUserId() != null) {
                // TODO: 跳转到用户主页
            }
        });

        // 发送评论/回复
        ivSendComment.setOnClickListener(v -> {
            if (!isLoggedIn) {
                showToast("请先登录");
                return;
            }
            sendComment();
        });

        // 取消回复按钮（如果有的话）
        if (ivCancelReply != null) {
            ivCancelReply.setOnClickListener(v -> cancelReply());
        }
    }

    /**
     * 加载帖子详情
     */
    private void loadPostDetails() {
        showLoading();

        try {
            String url = USER_POST + postId;
            Request request = createGetRequest(url);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "loadPostDetails onFailure: ", e);
                    runOnUiThread(() -> {
                        hideLoading();
                        showError("加载失败");
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String json = response.body().string();
                            Log.d(TAG, "Post detail response: " + json);

                            Type type = new TypeToken<ApiResponse<Post>>() {}.getType();
                            ApiResponse<Post> apiResponse = gson.fromJson(json, type);

                            if (apiResponse != null && apiResponse.isSuccess()) {
                                currentPost = apiResponse.getData();
                                if (currentPost != null) {
                                    runOnUiThread(() -> {
                                        hideLoading();
                                        updatePostUI();
                                        // 先加载评论，然后检查所有状态
                                        loadCommentsList();
                                        if (isLoggedIn) {
                                            checkAllStatus();
                                        }
                                    });
                                } else {
                                    runOnUiThread(() -> {
                                        hideLoading();
                                        showError("帖子数据为空");
                                    });
                                }
                            } else {
                                String errorMsg = apiResponse != null ? apiResponse.msg : "未知错误";
                                runOnUiThread(() -> {
                                    hideLoading();
                                    showError(errorMsg);
                                });
                            }
                        } else {
                            runOnUiThread(() -> {
                                hideLoading();
                                showError("网络请求失败: " + response.code());
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "loadPostDetails parse error: ", e);
                        runOnUiThread(() -> {
                            hideLoading();
                            showError("数据解析失败");
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "loadPostDetails error: ", e);
            hideLoading();
            showError("加载帖子详情失败");
        }
    }

    /**
     * 检查所有状态（点赞、收藏、关注）
     */
    private void checkAllStatus() {
        if (!isLoggedIn || currentPost == null) {
            return;
        }

        // 检查是否是自己的帖子
        long currentUserId = sharedPrefManager.getUserId();
        isOwnPost = currentUserId != 0 && currentUserId == currentPost.getUserId();

        if (isOwnPost) {
            ivFollow.setVisibility(View.GONE);
        } else {
            ivFollow.setVisibility(View.VISIBLE);
            checkFollowStatus();
        }

        // 检查点赞和收藏状态
        checkLikeStatus();
        checkFavoriteStatus();
    }

    /**
     * 检查点赞状态 - 参照Vue网页端实现
     */
    private void checkLikeStatus() {
        if (currentPost == null) return;

        ApiService.getInstance().checkPostLikeStatus(this, currentPost.getPostId(),
                new ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean liked) {
                        runOnUiThread(() -> {
                            hasLiked = liked != null && liked;
                            updateLikeUI();
                            Log.d(TAG, "点赞状态检查完成 - hasLiked: " + hasLiked);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "检查点赞状态失败：" + errorMessage);
                    }
                });
    }

    /**
     * 检查收藏状态
     */
    private void checkFavoriteStatus() {
        favoriteManager.checkFavoriteStatus(currentPost.getPostId(), new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean favorited) {
                runOnUiThread(() -> {
                    hasFavorited = favorited != null && favorited;
                    updateFavoriteUI();
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "检查收藏状态失败：" + errorMessage);
            }
        });
    }

    /**
     * 检查关注状态
     */
    private void checkFollowStatus() {
        if (currentPost == null || currentPost.getUserId() == null) {
            return;
        }

        // 使用 UserFollowManager 检查关注状态
        followManager.checkFollowStatus(currentPost.getUserId(), new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean hasFollowedStatus) {
                runOnUiThread(() -> {
                    hasFollowed = hasFollowedStatus != null && hasFollowedStatus;
                    updateFollowUI();
                    Log.d(TAG, "关注状态检查完成 - hasFollowed: " + hasFollowed);
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "检查关注状态失败: " + error);
                // 失败时默认显示未关注状态
                runOnUiThread(() -> {
                    hasFollowed = false;
                    updateFollowUI();
                });
            }
        });
    }

    /**
     * 更新帖子UI
     */
    private void updatePostUI() {
        if (currentPost == null) return;

        try {
            // 标题
            tvPostTitle.setText(currentPost.getPostTitle() != null ?
                    currentPost.getPostTitle() : "");

            // 用户信息
            tvUserName.setText(currentPost.getNickName() != null ?
                    currentPost.getNickName() : "未知用户");

            ImageUtils.loadUserAvatar(this, ivUserAvatar, currentPost.getAvatar());

            // 时间
            if (currentPost.getCreateTime() != null) {
                tvTime.setText(TimeUtils.formatTimeAgo(currentPost.getCreateTime()));
            } else if (currentPost.getUpdateTime() != null) {
                tvTime.setText(TimeUtils.formatTimeAgo(currentPost.getUpdateTime()));
            }

            // 富文本内容
            if (webViewContent != null && currentPost.getPostContent() != null) {
                String htmlContent = processHtmlContent(currentPost.getPostContent());
                webViewContent.loadDataWithBaseURL(
                        null,
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                );
            }

            // 统计数据
            updateStats();

        } catch (Exception e) {
            Log.e(TAG, "updatePostUI error: ", e);
        }
    }

    /**
     * 处理HTML内容
     */
    private String processHtmlContent(String rawHtml) {
        if (TextUtils.isEmpty(rawHtml)) {
            return "";
        }

        String cssStyle = "<style>" +
                "body { " +
                "   margin: 0; " +
                "   padding: 16px; " +
                "   font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; " +
                "   font-size: 15px; " +
                "   line-height: 1.8; " +
                "   color: #333333; " +
                "   word-wrap: break-word; " +
                "} " +
                "img { " +
                "   max-width: 100% !important; " +
                "   height: auto !important; " +
                "   display: block; " +
                "   margin: 16px auto; " +
                "   border-radius: 6px; " +
                "   box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1); " +
                "} " +
                "</style>";

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "   <meta charset=\"UTF-8\">\n" +
                "   <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
                cssStyle +
                "</head>\n" +
                "<body>\n" +
                rawHtml +
                "</body>\n" +
                "</html>";
    }

    /**
     * 更新统计数据
     */
    private void updateStats() {
        if (currentPost == null) return;

        tvLikeCount.setText(String.valueOf(
                currentPost.getLikeCount() != null ? currentPost.getLikeCount() : 0));
        tvCommentCount.setText(String.valueOf(
                currentPost.getCommentCount() != null ? currentPost.getCommentCount() : 0));
        tvViewCount.setText(String.valueOf(
                currentPost.getViewCount() != null ? currentPost.getViewCount() : 0));
    }

    /**
     * 更新点赞UI
     */
    private void updateLikeUI() {
        ivLike.setImageResource(hasLiked ? R.mipmap.ydz : R.mipmap.dz);
    }

    /**
     * 更新收藏UI
     */
    private void updateFavoriteUI() {
        ivFavorite.setImageResource(hasFavorited ? R.mipmap.ysc : R.mipmap.sc);
    }

    /**
     * 切换点赞 - 修复版，参照Vue网页端实现
     * 关键改进：
     * 1. 防止重复点击
     * 2. 根据当前状态发送正确的请求（POST或DELETE）
     * 3. 立即更新本地状态和UI
     * 4. 如果失败则回滚状态
     */
    private void toggleLike() {
        if (!isLoggedIn) {
            showToast("请先登录");
            return;
        }

        if (currentPost == null) {
            showToast("帖子数据无效");
            return;
        }

        // 防止重复点击
        if (likeLoading) {
            Log.d(TAG, "点赞请求进行中，忽略重复点击");
            return;
        }

        likeLoading = true;

        // 保存旧状态，用于失败时回滚
        final boolean oldLikeStatus = hasLiked;
        final int oldLikeCount = currentPost.getLikeCount() != null ? currentPost.getLikeCount() : 0;

        // 1. 先乐观更新UI（立即响应用户操作）
        hasLiked = !hasLiked;
        final int newLikeCount = oldLikeCount + (hasLiked ? 1 : -1);
        currentPost.setLikeCount(Math.max(0, newLikeCount));
        updateLikeUI();
        updateStats();

        Log.d(TAG, "开始切换点赞 - 当前状态: " + (hasLiked ? "点赞" : "取消点赞") +
                ", 旧点赞数: " + oldLikeCount + ", 新点赞数: " + newLikeCount);

        // 2. 发送请求到服务器
        String url = POST_LIKE_URL + postId;
        Request request;

        try {
            if (oldLikeStatus) {
                // 之前是点赞状态，现在取消点赞 - 发送DELETE请求
                request = createDeleteRequest(url);
                Log.d(TAG, "发送DELETE请求取消点赞: " + url);
            } else {
                // 之前未点赞，现在点赞 - 发送POST请求
                RequestBody emptyBody = RequestBody.create(JSON, "{}");
                request = createPostRequest(url, emptyBody);
                Log.d(TAG, "发送POST请求点赞: " + url);
            }

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "点赞请求失败: ", e);
                    runOnUiThread(() -> {
                        // 失败：回滚状态
                        hasLiked = oldLikeStatus;
                        currentPost.setLikeCount(oldLikeCount);
                        updateLikeUI();
                        updateStats();
                        showToast("操作失败: " + e.getMessage());
                        likeLoading = false;
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "点赞响应: code=" + response.code() + ", body=" + responseBody);

                        if (response.isSuccessful()) {
                            // 成功：解析响应
                            Type type = new TypeToken<ApiResponse<Object>>() {}.getType();
                            ApiResponse<Object> apiResponse = gson.fromJson(responseBody, type);

                            runOnUiThread(() -> {
                                if (apiResponse != null && apiResponse.isSuccess()) {
                                    // 服务器确认成功，保持当前状态
                                    Log.d(TAG, "点赞操作成功 - 最终状态: " +
                                            (hasLiked ? "已点赞" : "未点赞") +
                                            ", 点赞数: " + currentPost.getLikeCount());
                                    showToast(hasLiked ? "点赞成功" : "取消点赞");
                                } else {
                                    // 服务器返回失败：回滚状态
                                    String errorMsg = apiResponse != null ? apiResponse.msg : "未知错误";
                                    Log.e(TAG, "点赞操作失败: " + errorMsg);
                                    hasLiked = oldLikeStatus;
                                    currentPost.setLikeCount(oldLikeCount);
                                    updateLikeUI();
                                    updateStats();
                                    showToast("操作失败: " + errorMsg);
                                }
                                likeLoading = false;
                            });
                        } else {
                            // HTTP错误：回滚状态
                            runOnUiThread(() -> {
                                Log.e(TAG, "点赞HTTP错误: " + response.code());
                                hasLiked = oldLikeStatus;
                                currentPost.setLikeCount(oldLikeCount);
                                updateLikeUI();
                                updateStats();
                                showToast("操作失败: HTTP " + response.code());
                                likeLoading = false;
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "点赞响应解析失败: ", e);
                        runOnUiThread(() -> {
                            // 解析失败：回滚状态
                            hasLiked = oldLikeStatus;
                            currentPost.setLikeCount(oldLikeCount);
                            updateLikeUI();
                            updateStats();
                            showToast("操作失败");
                            likeLoading = false;
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "创建点赞请求失败: ", e);
            // 异常：回滚状态
            hasLiked = oldLikeStatus;
            currentPost.setLikeCount(oldLikeCount);
            updateLikeUI();
            updateStats();
            showToast("操作失败");
            likeLoading = false;
        }
    }

    /**
     * 切换收藏
     */
    private void toggleFavorite() {
        if (!isLoggedIn) {
            showToast("请先登录");
            return;
        }

        if (currentPost == null) return;

        // 防止重复点击
        if (favoriteLoading) {
            return;
        }

        favoriteLoading = true;

        if (hasFavorited) {
            // 取消收藏
            favoriteManager.removeFavorite(currentPost.getPostId(), new ApiCallback<String>() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        hasFavorited = false;
                        updateFavoriteUI();
                        showToast("取消收藏成功");
                        favoriteLoading = false;
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    showToastOnUiThread("取消收藏失败: " + errorMessage);
                    favoriteLoading = false;
                }
            });
        } else {
            // 添加收藏
            favoriteManager.addFavorite(currentPost.getPostId(), new ApiCallback<String>() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        hasFavorited = true;
                        updateFavoriteUI();
                        showToast("收藏成功");
                        favoriteLoading = false;
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    showToastOnUiThread("收藏失败: " + errorMessage);
                    favoriteLoading = false;
                }
            });
        }
    }

    /**
     * 切换关注
     */
    private void toggleFollow() {
        if (!isLoggedIn) {
            showToast("请先登录");
            return;
        }

        if (isOwnPost) {
            showToast("不能关注自己");
            return;
        }

        if (currentPost == null || currentPost.getUserId() == null) {
            showToast("用户信息无效");
            return;
        }

        // 使用 UserFollowManager 处理关注
        followManager.handleFollowClick(currentPost.getUserId(),
                new UserFollowManager.FollowStatusCallback() {
                    @Override
                    public void onUpdate(boolean hasFollowedNow, String message) {
                        // 更新关注状态
                        hasFollowed = hasFollowedNow;
                        // 更新UI
                        updateFollowUI();
                        Log.d(TAG, "关注状态更新: " + hasFollowedNow);
                    }

                    @Override
                    public void onFail(String errorMessage) {
                        Log.e(TAG, "关注操作失败: " + errorMessage);
                        // Toast已在Manager中显示
                    }
                });
    }

    /**
     * 更新关注按钮UI
     */
    private void updateFollowUI() {
        runOnUiThread(() -> {
            if (ivFollow != null) {
                if (hasFollowed) {
                    // 已关注状态 - 显示已关注图标
                    ivFollow.setImageResource(R.mipmap.ygz);
                    ivFollow.setColorFilter(getColor(R.color.colorAccent));
                } else {
                    // 未关注状态 - 显示关注图标
                    ivFollow.setImageResource(R.mipmap.ft);
                    ivFollow.setColorFilter(getColor(android.R.color.holo_blue_light));
                }
            }
        });
    }

    /**
     * 滚动到评论区
     */
    private void scrollToComments() {
        if (recyclerComments != null) {
            scrollContent.post(() -> {
                int[] location = new int[2];
                recyclerComments.getLocationInWindow(location);
                int y = location[1];
                scrollContent.smoothScrollTo(0, y - 100);
            });
        }

        // 让评论输入框获得焦点
        if (etCommentInput != null) {
            etCommentInput.requestFocus();
        }
    }

    /**
     * 加载评论列表
     */
    private void loadCommentsList() {
        commentManager.getPostComments(postId, new ApiCallback<List<Comment>>() {
            @Override
            public void onSuccess(List<Comment> comments) {
                runOnUiThread(() -> {
                    commentList.clear();
                    if (comments != null) {
                        commentList.addAll(comments);
                    }
                    commentAdapter.notifyDataSetChanged();
                    Log.d(TAG, "评论加载成功，共 " + commentList.size() + " 条");
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "加载评论失败: " + errorMessage);
                showToastOnUiThread("加载评论失败: " + errorMessage);
            }
        });
    }

    /**
     * 发送评论或回复
     */
    private void sendComment() {
        String content = etCommentInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            showToast("评论内容不能为空");
            return;
        }

        Comment comment = new Comment();
        comment.setPostId(postId);

        if (commentManager.isReplying()) {
            comment.setCommentContent(content);
            comment.setParentId(commentManager.getReplyToCommentId());
            Log.d(TAG, "准备发送回复 - PostId: " + postId +
                    ", ParentId: " + comment.getParentId() +
                    ", Content: " + content);
        } else {
            comment.setCommentContent(content);
            Log.d(TAG, "准备发送评论 - PostId: " + postId + ", Content: " + content);
        }

        commentManager.createComment(comment, new ApiCallback<Comment>() {
            @Override
            public void onSuccess(Comment createdComment) {
                runOnUiThread(() -> {
                    if (commentManager.isReplying()) {
                        showToast("回复成功");
                        cancelReply();
                    } else {
                        showToast("评论成功");
                    }

                    etCommentInput.setText("");

                    // 更新评论数
                    if (currentPost != null) {
                        int newCommentCount = (currentPost.getCommentCount() != null ?
                                currentPost.getCommentCount() : 0) + 1;
                        currentPost.setCommentCount(newCommentCount);
                        updateStats();
                    }

                    // 重新加载评论列表
                    loadCommentsList();

                    Log.d(TAG, "评论/回复发送成功");
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "评论/回复发送失败: " + errorMessage);
                showToastOnUiThread("发送失败: " + errorMessage);
            }
        });
    }

    /**
     * 评论点赞 - 使用新的 CommentLikeManager
     */
    private void onCommentLikeClicked(Comment comment) {
        if (!isLoggedIn) {
            showToast("请先登录");
            return;
        }

        // 使用新的 CommentLikeManager 处理点赞
        commentLikeManager.handleLikeClick(comment, new CommentLikeManager.LikeStatusCallback() {
            @Override
            public void onUpdate(boolean hasLiked, int newLikeCount) {
                runOnUiThread(() -> {
                    // 更新评论的点赞状态和点赞数
                    commentAdapter.updateCommentLikeStatus(
                            comment.getCommentId(), hasLiked, newLikeCount);

                    Log.d(TAG, "评论点赞状态更新 - ID: " + comment.getCommentId() +
                            ", Liked: " + hasLiked + ", Count: " + newLikeCount);
                });
            }

            @Override
            public void onFail(String errorMessage) {
                runOnUiThread(() -> {
                    showToast("操作失败: " + errorMessage);
                    Log.e(TAG, "评论点赞失败: " + errorMessage);
                });
            }
        });
    }

    /**
     * 回复评论
     */
    private void onCommentReplyClicked(Comment comment) {
        if (!isLoggedIn) {
            showToast("请先登录");
            return;
        }

        if (comment == null || comment.getCommentId() == null) {
            showToast("评论信息无效");
            return;
        }

        String username = comment.getNickName();
        if (TextUtils.isEmpty(username)) {
            username = "用户";
        }

        commentManager.setReplyTo(comment.getCommentId(), username);
        String replyPrefix = commentManager.getReplyPrefix();
        etCommentInput.setText(replyPrefix);
        etCommentInput.setSelection(replyPrefix.length());
        etCommentInput.requestFocus();

        updateReplyHintUI();
        scrollToComments();

        Log.d(TAG, "开始回复评论 - CommentId: " + comment.getCommentId() +
                ", Username: " + username);
    }

    /**
     * 取消回复
     */
    private void cancelReply() {
        if (!commentManager.isReplying()) {
            return;
        }

        commentManager.clearReplyTo();
        etCommentInput.setText("");
        updateReplyHintUI();

        Log.d(TAG, "取消回复");
    }

    /**
     * 更新回复提示UI
     */
    private void updateReplyHintUI() {
        if (layoutReplyHint != null && tvReplyHint != null) {
            if (commentManager.isReplying()) {
                layoutReplyHint.setVisibility(View.VISIBLE);
                tvReplyHint.setText("回复 @" + commentManager.getReplyToUsername());
            } else {
                layoutReplyHint.setVisibility(View.GONE);
            }
        }
    }

    // ========== 辅助方法 ==========

    private Request createGetRequest(String url) {
        Request.Builder builder = new Request.Builder().url(url);
        if (isLoggedIn && !TextUtils.isEmpty(authToken)) {
            String token = authToken.startsWith("Bearer ") ? authToken : "Bearer " + authToken;
            builder.addHeader("Authorization", token);
        }
        return builder.build();
    }

    private Request createPostRequest(String url, RequestBody body) {
        Request.Builder builder = new Request.Builder().url(url).post(body);
        if (isLoggedIn && !TextUtils.isEmpty(authToken)) {
            String token = authToken.startsWith("Bearer ") ? authToken : "Bearer " + authToken;
            builder.addHeader("Authorization", token);
        }
        return builder.build();
    }

    private Request createDeleteRequest(String url) {
        Request.Builder builder = new Request.Builder().url(url).delete();
        if (isLoggedIn && !TextUtils.isEmpty(authToken)) {
            String token = authToken.startsWith("Bearer ") ? authToken : "Bearer " + authToken;
            builder.addHeader("Authorization", token);
        }
        return builder.build();
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        scrollContent.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingView.setVisibility(View.GONE);
        scrollContent.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        loadingView.setVisibility(View.GONE);
        scrollContent.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        TextView tvError = errorView.findViewById(R.id.tv_error_message);
        if (tvError != null) {
            tvError.setText(message);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showToastOnUiThread(String message) {
        runOnUiThread(() -> showToast(message));
    }

    // API响应类
    public static class ApiResponse<T> {
        private int code;
        private String msg;
        private T data;

        public boolean isSuccess() {
            return code == 200;
        }

        public T getData() {
            return data;
        }

        public String getMsg() {
            return msg;
        }
    }
}