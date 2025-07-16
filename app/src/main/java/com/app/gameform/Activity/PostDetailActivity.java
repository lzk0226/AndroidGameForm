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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.gameform.R;
import com.app.gameform.domain.Comment;
import com.app.gameform.domain.Post;
import com.app.gameform.manager.PostLikeManager;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.ApiService;
import com.app.gameform.utils.HtmlUtils;
import com.app.gameform.utils.ImageUtils;
import com.app.gameform.utils.SharedPrefManager;
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

    private TextView tvContent, tvLikeCount, tvCommentCount, tvShareCount, tvPostTitle, tvUserName, tvTime;
    private ImageView ivImage, ivLike, ivComment, ivShare, ivSend;
    private CircleImageView ivUserAvatar;
    private EditText etCommentInput;
    private ScrollView scrollContent;
    private LinearLayout layoutCommentsContainer;

    private Post currentPost;
    private int postId;
    private String authToken;
    private List<Comment> commentList = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        initializeViews();
        initializeData();
        setupListeners();
        loadPostDetails();
        loadCommentsList();
    }

    private void initializeViews() {
        scrollContent = findViewById(R.id.scroll_content);
        ivUserAvatar = findViewById(R.id.iv_user_avatar);
        tvUserName = findViewById(R.id.tv_user_name);
        tvTime = findViewById(R.id.tv_time);
        tvPostTitle = findViewById(R.id.tv_post_title);
        tvContent = findViewById(R.id.tv_post_content);
        ivImage = findViewById(R.id.iv_post_image);
        ImageView ivBack = findViewById(R.id.iv_back);

        ivLike = findViewById(R.id.iv_like_icon); // ✅ 改为实际布局里的 ID


        tvLikeCount = findViewById(R.id.tv_like_count);
        tvCommentCount = findViewById(R.id.tv_comment_count);
        tvShareCount = findViewById(R.id.tv_share_count);

        etCommentInput = findViewById(R.id.et_comment_input);
        ivSend = findViewById(R.id.iv_send_comment);

        layoutCommentsContainer = findViewById(R.id.layout_comments_container);
    }

    private void initializeData() {
        postId = getIntent().getIntExtra("post_id", -1);
        authToken = SharedPrefManager.getInstance(this).getToken();

        if (postId == -1) {
            showToast("帖子ID无效");
            finish();
        }
    }

    private void setupListeners() {
        ivSend.setOnClickListener(v -> sendNewComment());
        findViewById(R.id.layout_back).setOnClickListener(v -> finish());
        findViewById(R.id.like_button).setOnClickListener(v -> {
            if (currentPost != null) {
                onLikeClick(currentPost, 0);
            }
        });
    }
    PostLikeManager likeManager = new PostLikeManager(this);
    private PostAdapter postAdapter;
    public void onLikeClick(Post post, int position) {
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
            }
        });
    }


    private void loadPostDetails() {
        String url = USER_POST + postId;
        Request request = createGetRequest(url);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                showToastOnUiThread("加载失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Type type = new TypeToken<ApiResponse<Post>>() {}.getType();
                    ApiResponse<Post> apiResponse = gson.fromJson(json, type);

                    if (apiResponse != null && apiResponse.isSuccess()) {
                        currentPost = apiResponse.getData();
                        updatePostUIOnUiThread();
                        checkPostLikeStatus(currentPost);
                    }
                }
            }
        });
    }

    private void checkPostLikeStatus(Post post) {
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
                Log.e("PostDetailActivity", "检查点赞状态失败：" + errorMessage);
            }
        });
    }


    private void updatePostUI() {
        if (currentPost == null) return;

        tvPostTitle.setText(currentPost.getPostTitle());
        tvContent.setText(HtmlUtils.removeHtmlTags(currentPost.getPostContent()));
        tvUserName.setText(currentPost.getNickName());
        tvTime.setText("刚刚");

        loadUserAvatar(ivUserAvatar, currentPost.getAvatar());

        if (!TextUtils.isEmpty(currentPost.getPhoto())) {
            showPostImage();
            loadPostImage(ivImage, currentPost.getPhoto());
        } else {
            hidePostImage();
        }

        updateCounters();
    }

    private void loadCommentsList() {
        String url = USER_POST_COMMENT + postId;
        Request request = createGetRequest(url);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "加载评论失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Type type = new TypeToken<ApiResponse<List<Comment>>>() {}.getType();
                    ApiResponse<List<Comment>> apiResponse = gson.fromJson(json, type);

                    if (apiResponse != null && apiResponse.isSuccess()) {
                        updateCommentList(apiResponse.getData());
                        updateCommentsUIOnUiThread();
                    }
                }
            }
        });
    }

    private void updateCommentsUI() {
        clearCommentsContainer();
        addCommentsToContainer();
    }

    private View createCommentView(Comment comment) {
        View commentView = LayoutInflater.from(this).inflate(R.layout.activity_item_comment, null);

        CircleImageView ivUserAvatar = commentView.findViewById(R.id.iv_user_avatar);
        TextView tvUserName = commentView.findViewById(R.id.tv_user_name);
        TextView tvCommentContent = commentView.findViewById(R.id.tv_comment_content);
        ImageView ivLike = commentView.findViewById(R.id.iv_like);
        TextView tvLikeCount = commentView.findViewById(R.id.tv_like_count);
        LinearLayout layoutChildComments = commentView.findViewById(R.id.layout_child_comments);

        setCommentData(ivUserAvatar, tvUserName, tvCommentContent, tvLikeCount, comment);
        setLikeButtonStatus(ivLike, comment.getHasLiked());
        setupLikeClickListener(ivLike, comment);
        renderChildComments(layoutChildComments, comment.getChildren());

        return commentView;
    }

    private void sendNewComment() {
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
                showToastOnUiThread("评论失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    showToastOnUiThread("评论成功");
                    clearCommentInput();
                    Log.d("Token调试", "发送评论用的 token = " + authToken);
                    refreshCommentsList();
                    updateCommentCounter();
                }
            }
        });
    }

    private void onCommentLikeClicked(Comment comment) {
        if (comment.getHasLiked() != null && comment.getHasLiked()) {
            unlikeComment(comment);
        } else {
            likeComment(comment);
        }
    }

    private void likeComment(Comment comment) {
        String url = USER_COMMENT_LIKE + comment.getCommentId();
        Request request = createPostRequest(url, RequestBody.create(new byte[0]));

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "点赞失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    updateCommentLikeStatusOnUiThread(comment, true, comment.getLikeCount() + 1);
                }
            }
        });
    }

    private void unlikeComment(Comment comment) {
        String url = USER_COMMENT_LIKE + comment.getCommentId();
        Request request = createDeleteRequest(url);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "取消点赞失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    updateCommentLikeStatusOnUiThread(comment, false, comment.getLikeCount() - 1);
                }
            }
        });
    }

    // 辅助方法
    private Request createGetRequest(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", authToken)
                .build();
    }

    private Request createPostRequest(String url, RequestBody body) {
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", authToken)
                .post(body)
                .build();
    }

    private Request createDeleteRequest(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", authToken)
                .delete()
                .build();
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
        ImageUtils.loadUserAvatar(this, imageView, avatarUrl);
    }

    private void showPostImage() {
        ivImage.setVisibility(View.VISIBLE);
    }

    private void hidePostImage() {
        ivImage.setVisibility(View.GONE);
    }

    private void loadPostImage(ImageView imageView, String photoUrl) {
        ImageUtils.loadPostImage(this, imageView, photoUrl);
    }

    private void updateCounters() {
        tvLikeCount.setText(String.valueOf(currentPost.getLikeCount()));
        tvCommentCount.setText(String.valueOf(currentPost.getCommentCount()));
        tvShareCount.setText(String.valueOf(currentPost.getViewCount()));
    }

    private void updateCommentList(List<Comment> newComments) {
        commentList.clear();
        commentList.addAll(newComments);
    }

    private void updateCommentsUIOnUiThread() {
        runOnUiThread(this::updateCommentsUI);
    }

    private void clearCommentsContainer() {
        layoutCommentsContainer.removeAllViews();
    }

    private void addCommentsToContainer() {
        for (Comment comment : commentList) {
            View commentView = createCommentView(comment);
            layoutCommentsContainer.addView(commentView);
        }
    }

    private void setCommentData(CircleImageView avatar, TextView name, TextView content, TextView likeCount, Comment comment) {
        name.setText(comment.getNickName());
        content.setText(HtmlUtils.removeHtmlTags(comment.getCommentContent()));
        likeCount.setText(String.valueOf(comment.getLikeCount()));
        loadUserAvatar(avatar, comment.getUserAvatar());
    }

    private void setLikeButtonStatus(ImageView likeButton, Boolean hasLiked) {
        if (hasLiked != null && hasLiked) {
            likeButton.setImageResource(R.mipmap.ydz);
        } else {
            likeButton.setImageResource(R.mipmap.dz);
        }
    }

    private void setupLikeClickListener(ImageView likeButton, Comment comment) {
        likeButton.setOnClickListener(v -> onCommentLikeClicked(comment));
    }

    private void renderChildComments(LinearLayout container, List<Comment> children) {
        if (children != null && !children.isEmpty()) {
            for (Comment child : children) {
                View childView = LayoutInflater.from(this).inflate(R.layout.activity_item_comment, null);

                CircleImageView childAvatar = childView.findViewById(R.id.iv_user_avatar);
                TextView childName = childView.findViewById(R.id.tv_user_name);
                TextView childContent = childView.findViewById(R.id.tv_comment_content);
                ImageView childLike = childView.findViewById(R.id.iv_like);
                TextView childLikeCount = childView.findViewById(R.id.tv_like_count);

                setCommentData(childAvatar, childName, childContent, childLikeCount, child);
                setLikeButtonStatus(childLike, child.getHasLiked());
                setupLikeClickListener(childLike, child);

                container.addView(childView);
            }
        }
    }

    private Comment createNewComment(String content) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setCommentContent(content);
        return comment;
    }

    private void clearCommentInput() {
        etCommentInput.setText("");
    }

    private void refreshCommentsList() {
        loadCommentsList();
    }

    private void updateCommentCounter() {
        currentPost.setCommentCount(currentPost.getCommentCount() + 1);
        tvCommentCount.setText(String.valueOf(currentPost.getCommentCount()));
    }

    private void updateCommentLikeStatusOnUiThread(Comment comment, boolean hasLiked, int likeCount) {
        runOnUiThread(() -> {
            comment.setHasLiked(hasLiked);
            comment.setLikeCount(likeCount);
            updateCommentsUI();
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
    }
}