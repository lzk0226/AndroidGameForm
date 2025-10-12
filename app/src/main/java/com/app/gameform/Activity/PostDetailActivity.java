package com.app.gameform.Activity;

import static com.app.gameform.network.ApiConstants.USER_COMMENT;
import static com.app.gameform.network.ApiConstants.USER_COMMENT_LIKE;
import static com.app.gameform.network.ApiConstants.USER_POST;
import static com.app.gameform.network.ApiConstants.USER_POST_COMMENT;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.gameform.R;
import com.app.gameform.adapter.PostAdapter;
import com.app.gameform.domain.Comment;
import com.app.gameform.domain.Post;
import com.app.gameform.manager.PostLikeManager;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.ApiService;
import com.app.gameform.utils.HtmlUtils;
import com.app.gameform.utils.ImageUtils;
import com.app.gameform.utils.SharedPrefManager;
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

public class PostDetailActivity extends AppCompatActivity {

    private static final String TAG = "PostDetailActivity";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    //private TextView tvContent, tvLikeCount, tvCommentCount, tvShareCount, tvPostTitle, tvUserName, tvTime;
    private TextView tvLikeCount, tvCommentCount, tvShareCount, tvPostTitle, tvUserName, tvTime;
    private WebView webViewContent;
    private ImageView ivImage, ivLike, ivComment, ivShare, ivSend;
    private CircleImageView ivUserAvatar;
    private EditText etCommentInput;
    private ScrollView scrollContent;
    private LinearLayout layoutCommentsContainer;

    private Post currentPost;
    private int postId;
    private String authToken;
    private boolean isLoggedIn = false; // 添加登录状态标识
    private List<Comment> commentList = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        try {
            initializeViews();
            if (!initializeData()) {
                // 初始化失败，直接返回
                return;
            }
            setupListeners();
            loadPostDetails();
            loadCommentsList();
        } catch (Exception e) {
            Log.e(TAG, "onCreate error: ", e);
            showToast("页面加载失败");
            finish();
        }
    }

    private void initializeViews() {
        scrollContent = findViewById(R.id.scroll_content);
        ivUserAvatar = findViewById(R.id.iv_user_avatar);
        tvUserName = findViewById(R.id.tv_user_name);
        tvTime = findViewById(R.id.tv_time);
        tvPostTitle = findViewById(R.id.tv_post_title);
        //tvContent = findViewById(R.id.tv_post_content);
        //ivImage = findViewById(R.id.iv_post_image);
        webViewContent = findViewById(R.id.web_view_content);
        setupWebView();
        ImageView ivBack = findViewById(R.id.iv_back);

        ivLike = findViewById(R.id.iv_like_icon);
        tvLikeCount = findViewById(R.id.tv_like_count);
        tvCommentCount = findViewById(R.id.tv_comment_count);
        tvShareCount = findViewById(R.id.tv_share_count);

        etCommentInput = findViewById(R.id.et_comment_input);
        ivSend = findViewById(R.id.iv_send_comment);

        layoutCommentsContainer = findViewById(R.id.layout_comments_container);
    }

    /**
     * 设置WebView配置
     */
    private void setupWebView() {
        WebSettings webSettings = webViewContent.getSettings();

        // 启用JavaScript（如果需要）
        webSettings.setJavaScriptEnabled(true);

        // 设置自适应屏幕
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);

        // 支持缩放
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

        // 其他设置
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        // 设置透明背景
        webViewContent.setBackgroundColor(0x00000000);

        // 设置WebChromeClient来处理进度等
        webViewContent.setWebChromeClient(new WebChromeClient());
    }

    private boolean initializeData() {
        try {
            // 获取传递的帖子ID
            postId = getIntent().getIntExtra("post_id", -1);
            if (postId == -1) {
                showToast("帖子ID无效");
                finish();
                return false;
            }

            // 获取认证token
            authToken = SharedPrefManager.getInstance(this).getToken();

            // 检查是否已登录
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

    private void setupListeners() {
        ivSend.setOnClickListener(v -> {
            if (!isLoggedIn) {
                showToast("请先登录");
                return;
            }
            sendNewComment();
        });

        findViewById(R.id.layout_back).setOnClickListener(v -> finish());

        findViewById(R.id.like_button).setOnClickListener(v -> {
            if (!isLoggedIn) {
                showToast("请先登录");
                return;
            }
            if (currentPost != null) {
                onLikeClick(currentPost, 0);
            }
        });
    }

    PostLikeManager likeManager = new PostLikeManager(this);
    private PostAdapter postAdapter;

    public void onLikeClick(Post post, int position) {
        if (!isLoggedIn) {
            showToast("请先登录");
            return;
        }

        likeManager.handleLikeClick(post, position, new PostLikeManager.LikeStatusCallback() {
            @Override
            public void onUpdate(boolean hasLiked, int newLikeCount) {
                runOnUiThread(() -> {
                    // 更新点赞数量显示
                    tvLikeCount.setText(String.valueOf(newLikeCount));

                    // 根据是否点赞，切换图标
                    if (hasLiked) {
                        ivLike.setImageResource(R.mipmap.ydz); // 已点赞图标
                    } else {
                        ivLike.setImageResource(R.mipmap.dz);  // 未点赞图标
                    }
                });
            }

            @Override
            public void onFail(String errorMessage) {
                Log.e("PostLikeManager", errorMessage);
                showToastOnUiThread("操作失败: " + errorMessage);
            }
        });
    }

    private void loadPostDetails() {
        try {
            String url = USER_POST + postId;
            Request request = createGetRequest(url);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "loadPostDetails onFailure: ", e);
                    showToastOnUiThread("加载失败: " + e.getMessage());
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
                                    updatePostUIOnUiThread();
                                    if (isLoggedIn) {
                                        checkPostLikeStatus(currentPost);
                                    }
                                } else {
                                    showToastOnUiThread("帖子数据为空");
                                }
                            } else {
                                String errorMsg = apiResponse != null ? apiResponse.msg : "未知错误";
                                showToastOnUiThread("加载失败: " + errorMsg);
                            }
                        } else {
                            showToastOnUiThread("网络请求失败: " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "loadPostDetails parse error: ", e);
                        showToastOnUiThread("数据解析失败");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "loadPostDetails error: ", e);
            showToast("加载帖子详情失败");
        }
    }

    private void checkPostLikeStatus(Post post) {
        if (!isLoggedIn || post == null) {
            // 未登录状态显示默认图标
            runOnUiThread(() -> ivLike.setImageResource(R.mipmap.dz));
            return;
        }

        try {
            ApiService.getInstance().checkPostLikeStatus(this, post.getPostId(), new ApiCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean hasLiked) {
                    runOnUiThread(() -> {
                        if (hasLiked != null && hasLiked) {
                            ivLike.setImageResource(R.mipmap.ydz); // 显示已点赞图标
                        } else {
                            ivLike.setImageResource(R.mipmap.dz); // 显示未点赞图标
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "检查点赞状态失败：" + errorMessage);
                    // 失败时显示默认图标
                    runOnUiThread(() -> ivLike.setImageResource(R.mipmap.dz));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "checkPostLikeStatus error: ", e);
            runOnUiThread(() -> ivLike.setImageResource(R.mipmap.dz));
        }
    }

    private void updatePostUI() {
        try {
            if (currentPost == null) {
                showToast("帖子数据为空");
                return;
            }

            // 安全地设置文本内容
            if (tvPostTitle != null) {
                tvPostTitle.setText(currentPost.getPostTitle() != null ? currentPost.getPostTitle() : "");
            }

            /*if (tvContent != null) {
                String content = currentPost.getPostContent() != null ? currentPost.getPostContent() : "";
                tvContent.setText(HtmlUtils.removeHtmlTags(content));
            }*/

            // 使用WebView显示富文本内容
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

            if (tvUserName != null) {
                tvUserName.setText(currentPost.getNickName() != null ? currentPost.getNickName() : "未知用户");
            }

            // 修改这里：使用实际的时间数据而不是硬编码"刚刚"
            if (tvTime != null) {
                if (currentPost.getCreateTime() != null) {
                    tvTime.setText(TimeUtils.formatTimeAgo(currentPost.getCreateTime()));
                } else if (currentPost.getUpdateTime() != null) {
                    tvTime.setText(TimeUtils.formatTimeAgo(currentPost.getUpdateTime()));
                } else {
                    tvTime.setText("未知时间");
                }
            }

            // 加载用户头像
            if (ivUserAvatar != null) {
                loadUserAvatar(ivUserAvatar, currentPost.getAvatar());
            }

            // 处理帖子图片
            if (ivImage != null) {
                if (!TextUtils.isEmpty(currentPost.getPhoto())) {
                    showPostImage();
                    loadPostImage(ivImage, currentPost.getPhoto());
                } else {
                    hidePostImage();
                }
            }

            updateCounters();
        } catch (Exception e) {
            Log.e(TAG, "updatePostUI error: ", e);
            showToast("更新界面失败");
        }
    }

    /**
     * 处理HTML内容，添加CSS样式使其适配移动端
     */
    private String processHtmlContent(String rawHtml) {
        if (TextUtils.isEmpty(rawHtml)) {
            return "";
        }

        // 创建适配移动端的CSS样式
        String cssStyle = "<style>" +
                "body { " +
                "   margin: 0; " +
                "   padding: 0; " +
                "   font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; " +
                "   font-size: 16px; " +
                "   line-height: 1.6; " +
                "   color: #333333; " +
                "   word-wrap: break-word; " +
                "} " +
                "img { " +
                "   max-width: 100% !important; " +
                "   height: auto !important; " +
                "   display: block; " +
                "   margin: 8px auto; " +
                "} " +
                "video { " +
                "   max-width: 100% !important; " +
                "   height: auto !important; " +
                "} " +
                "iframe { " +
                "   max-width: 100% !important; " +
                "} " +
                "table { " +
                "   max-width: 100% !important; " +
                "   border-collapse: collapse; " +
                "} " +
                "pre { " +
                "   background: #f5f5f5; " +
                "   padding: 12px; " +
                "   border-radius: 4px; " +
                "   overflow-x: auto; " +
                "} " +
                "code { " +
                "   background: #f5f5f5; " +
                "   padding: 2px 4px; " +
                "   border-radius: 2px; " +
                "} " +
                "blockquote { " +
                "   border-left: 4px solid #ddd; " +
                "   margin: 8px 0; " +
                "   padding-left: 12px; " +
                "   color: #666; " +
                "} " +
                "</style>";

        // 包装HTML内容
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

    private void loadCommentsList() {
        try {
            String url = USER_POST_COMMENT + postId;
            Request request = createGetRequest(url);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "加载评论失败: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String json = response.body().string();
                            Log.d(TAG, "Comments response: " + json);

                            Type type = new TypeToken<ApiResponse<List<Comment>>>() {}.getType();
                            ApiResponse<List<Comment>> apiResponse = gson.fromJson(json, type);

                            if (apiResponse != null && apiResponse.isSuccess()) {
                                List<Comment> comments = apiResponse.getData();
                                if (comments != null) {
                                    updateCommentList(comments);
                                    updateCommentsUIOnUiThread();
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "loadCommentsList parse error: ", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "loadCommentsList error: ", e);
        }
    }

    private void updateCommentsUI() {
        try {
            clearCommentsContainer();
            addCommentsToContainer();
        } catch (Exception e) {
            Log.e(TAG, "updateCommentsUI error: ", e);
        }
    }

    private View createCommentView(Comment comment) {
        try {
            View commentView = LayoutInflater.from(this).inflate(R.layout.activity_item_comment, null);

            CircleImageView ivUserAvatar = commentView.findViewById(R.id.iv_user_avatar);
            TextView tvUserName = commentView.findViewById(R.id.tv_user_name);
            TextView tvCommentContent = commentView.findViewById(R.id.tv_comment_content);
            ImageView ivLike = commentView.findViewById(R.id.iv_like);
            TextView tvLikeCount = commentView.findViewById(R.id.tv_like_count);
            LinearLayout layoutChildComments = commentView.findViewById(R.id.layout_child_comments);

            setCommentData(ivUserAvatar, tvUserName, tvCommentContent, tvLikeCount, comment);
            setLikeButtonStatus(ivLike, comment.getHasLiked());

            if (isLoggedIn) {
                setupLikeClickListener(ivLike, comment);
            }

            renderChildComments(layoutChildComments, comment.getChildren());

            return commentView;
        } catch (Exception e) {
            Log.e(TAG, "createCommentView error: ", e);
            return new View(this); // 返回空视图避免崩溃
        }
    }

    private void sendNewComment() {
        if (!isLoggedIn) {
            showToast("请先登录");
            return;
        }

        try {
            String content = etCommentInput.getText().toString().trim();
            if (TextUtils.isEmpty(content)) {
                showToast("评论内容不能为空");
                return;
            }

            Comment comment = createNewComment(content);
            String url = USER_COMMENT;
            String jsonBody = gson.toJson(comment);
            RequestBody body = RequestBody.create(JSON, jsonBody);
            Request request = createPostRequest(url, body);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "sendNewComment onFailure: ", e);
                    showToastOnUiThread("评论失败: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            showToastOnUiThread("评论成功");
                            clearCommentInput();
                            refreshCommentsList();
                            updateCommentCounter();
                        } else {
                            showToastOnUiThread("评论失败: " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "sendNewComment parse error: ", e);
                        showToastOnUiThread("评论失败");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "sendNewComment error: ", e);
            showToast("发送评论失败");
        }
    }

    private void onCommentLikeClicked(Comment comment) {
        if (!isLoggedIn) {
            showToast("请先登录");
            return;
        }

        try {
            if (comment.getHasLiked() != null && comment.getHasLiked()) {
                unlikeComment(comment);
            } else {
                likeComment(comment);
            }
        } catch (Exception e) {
            Log.e(TAG, "onCommentLikeClicked error: ", e);
            showToast("操作失败");
        }
    }

    private void likeComment(Comment comment) {
        try {
            String url = USER_COMMENT_LIKE + comment.getCommentId();
            Request request = createPostRequest(url, RequestBody.create(new byte[0]));

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "点赞失败: " + e.getMessage());
                    showToastOnUiThread("点赞失败");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String json = response.body().string();
                            Log.d(TAG, "Comment like response: " + json);

                            // 解析服务器响应获取新的点赞数量
                            Type type = new TypeToken<ApiResponse<Object>>() {}.getType();
                            ApiResponse<Object> apiResponse = gson.fromJson(json, type);

                            if (apiResponse != null && apiResponse.isSuccess()) {
                                // 点赞成功，更新点赞状态和数量
                                int newLikeCount = (comment.getLikeCount() != null ? comment.getLikeCount() : 0) + 1;
                                updateCommentLikeStatusOnUiThread(comment, true, newLikeCount);
                            } else {
                                showToastOnUiThread("点赞失败: " + (apiResponse != null ? apiResponse.getMsg() : "未知错误"));
                            }
                        } else {
                            showToastOnUiThread("点赞失败: " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "likeComment parse error: ", e);
                        showToastOnUiThread("点赞失败");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "likeComment error: ", e);
            showToast("点赞失败");
        }
    }

    private void unlikeComment(Comment comment) {
        try {
            String url = USER_COMMENT_LIKE + comment.getCommentId();
            Request request = createDeleteRequest(url);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "取消点赞失败: " + e.getMessage());
                    showToastOnUiThread("取消点赞失败");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String json = response.body().string();
                            Log.d(TAG, "Comment unlike response: " + json);

                            // 解析服务器响应
                            Type type = new TypeToken<ApiResponse<Object>>() {}.getType();
                            ApiResponse<Object> apiResponse = gson.fromJson(json, type);

                            if (apiResponse != null && apiResponse.isSuccess()) {
                                // 取消点赞成功，更新点赞状态和数量
                                int newLikeCount = Math.max(0, (comment.getLikeCount() != null ? comment.getLikeCount() : 0) - 1);
                                updateCommentLikeStatusOnUiThread(comment, false, newLikeCount);
                            } else {
                                showToastOnUiThread("取消点赞失败: " + (apiResponse != null ? apiResponse.getMsg() : "未知错误"));
                            }
                        } else {
                            showToastOnUiThread("取消点赞失败: " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "unlikeComment parse error: ", e);
                        showToastOnUiThread("取消点赞失败");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "unlikeComment error: ", e);
            showToast("取消点赞失败");
        }
    }

    // 辅助方法
    private Request createGetRequest(String url) {
        Request.Builder builder = new Request.Builder().url(url);

        // 只有在已登录时才添加Authorization头
        if (isLoggedIn && !TextUtils.isEmpty(authToken)) {
            builder.addHeader("Authorization", authToken);
        }

        return builder.build();
    }

    private Request createPostRequest(String url, RequestBody body) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);

        if (isLoggedIn && !TextUtils.isEmpty(authToken)) {
            builder.addHeader("Authorization", authToken);
        }

        return builder.build();
    }

    private Request createDeleteRequest(String url) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .delete();

        if (isLoggedIn && !TextUtils.isEmpty(authToken)) {
            builder.addHeader("Authorization", authToken);
        }

        return builder.build();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showToastOnUiThread(String message) {
        runOnUiThread(() -> showToast(message));
    }

    private void updatePostUIOnUiThread() {
        runOnUiThread(this::updatePostUI);
    }

    private void loadUserAvatar(CircleImageView imageView, String avatarUrl) {
        try {
            ImageUtils.loadUserAvatar(this, imageView, avatarUrl);
        } catch (Exception e) {
            Log.e(TAG, "loadUserAvatar error: ", e);
        }
    }

    private void showPostImage() {
        if (ivImage != null) {
            ivImage.setVisibility(View.VISIBLE);
        }
    }

    private void hidePostImage() {
        if (ivImage != null) {
            ivImage.setVisibility(View.GONE);
        }
    }

    private void loadPostImage(ImageView imageView, String photoUrl) {
        try {
            ImageUtils.loadPostImage(this, imageView, photoUrl);
        } catch (Exception e) {
            Log.e(TAG, "loadPostImage error: ", e);
        }
    }

    private void updateCounters() {
        try {
            if (currentPost != null) {
                if (tvLikeCount != null) {
                    tvLikeCount.setText(String.valueOf(currentPost.getLikeCount() != null ? currentPost.getLikeCount() : 0));
                }
                if (tvCommentCount != null) {
                    tvCommentCount.setText(String.valueOf(currentPost.getCommentCount() != null ? currentPost.getCommentCount() : 0));
                }
                if (tvShareCount != null) {
                    tvShareCount.setText(String.valueOf(currentPost.getViewCount() != null ? currentPost.getViewCount() : 0));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "updateCounters error: ", e);
        }
    }

    private void updateCommentList(List<Comment> newComments) {
        try {
            commentList.clear();
            if (newComments != null) {
                commentList.addAll(newComments);
            }
        } catch (Exception e) {
            Log.e(TAG, "updateCommentList error: ", e);
        }
    }

    private void updateCommentsUIOnUiThread() {
        runOnUiThread(this::updateCommentsUI);
    }

    private void clearCommentsContainer() {
        try {
            if (layoutCommentsContainer != null) {
                layoutCommentsContainer.removeAllViews();
            }
        } catch (Exception e) {
            Log.e(TAG, "clearCommentsContainer error: ", e);
        }
    }

    private void addCommentsToContainer() {
        try {
            if (layoutCommentsContainer != null && commentList != null) {
                for (Comment comment : commentList) {
                    if (comment != null) {
                        View commentView = createCommentView(comment);
                        if (commentView != null) {
                            layoutCommentsContainer.addView(commentView);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "addCommentsToContainer error: ", e);
        }
    }

    private void setCommentData(CircleImageView avatar, TextView name, TextView content, TextView likeCount, Comment comment) {
        try {
            if (name != null) {
                name.setText(comment.getNickName() != null ? comment.getNickName() : "未知用户");
            }
            if (content != null) {
                String commentContent = comment.getCommentContent() != null ? comment.getCommentContent() : "";
                content.setText(HtmlUtils.removeHtmlTags(commentContent));
            }
            if (likeCount != null) {
                likeCount.setText(String.valueOf(comment.getLikeCount() != null ? comment.getLikeCount() : 0));
            }
            if (avatar != null) {
                loadUserAvatar(avatar, comment.getUserAvatar());
            }
        } catch (Exception e) {
            Log.e(TAG, "setCommentData error: ", e);
        }
    }

    private void setLikeButtonStatus(ImageView likeButton, Boolean hasLiked) {
        try {
            if (likeButton != null) {
                if (hasLiked != null && hasLiked) {
                    likeButton.setImageResource(R.mipmap.ydz);
                } else {
                    likeButton.setImageResource(R.mipmap.dz);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "setLikeButtonStatus error: ", e);
        }
    }

    private void setupLikeClickListener(ImageView likeButton, Comment comment) {
        try {
            if (likeButton != null && comment != null) {
                likeButton.setOnClickListener(v -> onCommentLikeClicked(comment));
            }
        } catch (Exception e) {
            Log.e(TAG, "setupLikeClickListener error: ", e);
        }
    }

    private void renderChildComments(LinearLayout container, List<Comment> children) {
        try {
            if (container != null && children != null && !children.isEmpty()) {
                for (Comment child : children) {
                    if (child != null) {
                        View childView = LayoutInflater.from(this).inflate(R.layout.activity_item_comment, null);

                        CircleImageView childAvatar = childView.findViewById(R.id.iv_user_avatar);
                        TextView childName = childView.findViewById(R.id.tv_user_name);
                        TextView childContent = childView.findViewById(R.id.tv_comment_content);
                        ImageView childLike = childView.findViewById(R.id.iv_like);
                        TextView childLikeCount = childView.findViewById(R.id.tv_like_count);

                        setCommentData(childAvatar, childName, childContent, childLikeCount, child);
                        setLikeButtonStatus(childLike, child.getHasLiked());

                        if (isLoggedIn) {
                            setupLikeClickListener(childLike, child);
                        }

                        container.addView(childView);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "renderChildComments error: ", e);
        }
    }

    private Comment createNewComment(String content) {
        try {
            Comment comment = new Comment();
            comment.setPostId(postId);
            comment.setCommentContent(content);
            return comment;
        } catch (Exception e) {
            Log.e(TAG, "createNewComment error: ", e);
            return new Comment(); // 返回空对象避免崩溃
        }
    }

    private void clearCommentInput() {
        try {
            if (etCommentInput != null) {
                etCommentInput.setText("");
            }
        } catch (Exception e) {
            Log.e(TAG, "clearCommentInput error: ", e);
        }
    }

    private void refreshCommentsList() {
        loadCommentsList();
    }

    private void updateCommentCounter() {
        try {
            if (currentPost != null) {
                currentPost.setCommentCount(currentPost.getCommentCount() + 1);
                if (tvCommentCount != null) {
                    tvCommentCount.setText(String.valueOf(currentPost.getCommentCount()));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "updateCommentCounter error: ", e);
        }
    }

    private void updateCommentLikeStatusOnUiThread(Comment comment, boolean hasLiked, int likeCount) {
        runOnUiThread(() -> {
            try {
                if (comment != null) {
                    comment.setHasLiked(hasLiked);
                    comment.setLikeCount(likeCount);
                    updateCommentsUI();
                }
            } catch (Exception e) {
                Log.e(TAG, "updateCommentLikeStatusOnUiThread error: ", e);
            }
        });
    }

    // 适配后端响应的数据结构
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