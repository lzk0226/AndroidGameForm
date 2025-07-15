package com.app.gameform.Activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.domain.Comment;
import com.app.gameform.domain.Post;
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
    private RecyclerView rvComments;

    private Post currentPost;
    private int postId;
    private String authToken;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        initView();
        initData();
        initListener();
        loadPostDetail();
        loadComments();
    }

    private void initView() {
        scrollContent = findViewById(R.id.scroll_content);
        ivUserAvatar = findViewById(R.id.iv_user_avatar);
        tvUserName = findViewById(R.id.tv_user_name);
        tvTime = findViewById(R.id.tv_time);
        tvPostTitle = findViewById(R.id.tv_post_title);
        tvContent = findViewById(R.id.tv_post_content);
        ivImage = findViewById(R.id.iv_post_image);

        tvLikeCount = findViewById(R.id.tv_like_count);
        tvCommentCount = findViewById(R.id.tv_comment_count);
        tvShareCount = findViewById(R.id.tv_share_count);

        etCommentInput = findViewById(R.id.et_comment_input);
        ivSend = findViewById(R.id.iv_send_comment);

        rvComments = findViewById(R.id.rv_comments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(commentList, this::onCommentLikeClicked);
        rvComments.setAdapter(commentAdapter);
    }

    private void initData() {
        // 从Intent中获取帖子ID
        postId = getIntent().getIntExtra("post_id", -1);
        authToken = SharedPrefManager.getInstance(this).getToken();

        if (postId == -1) {
            Toast.makeText(this, "帖子ID无效", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initListener() {
        ivSend.setOnClickListener(v -> sendComment());
    }

    private void loadPostDetail() {
        String url = "http://10.0.2.2:8080/user/post/" + postId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + authToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(PostDetailActivity.this,
                        "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Type type = new TypeToken<ApiResponse<Post>>() {}.getType();
                    ApiResponse<Post> apiResponse = gson.fromJson(json, type);

                    if (apiResponse != null && apiResponse.isSuccess()) {
                        currentPost = apiResponse.getData();
                        runOnUiThread(() -> updatePostUI());
                    }
                }
            }
        });
    }

    private void updatePostUI() {
        if (currentPost == null) return;

        tvPostTitle.setText(currentPost.getPostTitle());
        tvContent.setText(HtmlUtils.removeHtmlTags(currentPost.getPostContent()));
        tvUserName.setText(currentPost.getNickName());
        tvTime.setText("刚刚"); // 实际应用中应格式化时间

        // 加载用户头像
        ImageUtils.loadUserAvatar(this, ivUserAvatar, currentPost.getAvatar());

        // 加载帖子图片
        if (!TextUtils.isEmpty(currentPost.getPhoto())) {
            ivImage.setVisibility(View.VISIBLE);
            ImageUtils.loadPostImage(this, ivImage, currentPost.getPhoto());
        } else {
            ivImage.setVisibility(View.GONE);
        }

        // 更新计数
        tvLikeCount.setText(String.valueOf(currentPost.getLikeCount()));
        tvCommentCount.setText(String.valueOf(currentPost.getCommentCount()));
        tvShareCount.setText(String.valueOf(currentPost.getViewCount()));
    }

    private void loadComments() {
        String url = "http://10.0.2.2:8080/user/comment/post/" + postId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + authToken)
                .build();

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
                        commentList.clear();
                        commentList.addAll(apiResponse.getData());
                        runOnUiThread(() -> commentAdapter.notifyDataSetChanged());
                    }
                }
            }
        });
    }

    private void sendComment() {
        String content = etCommentInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "评论内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setCommentContent(content);

        String url = "http://10.0.2.2:8080/user/comment";
        String jsonBody = gson.toJson(comment);

        RequestBody body = RequestBody.create(JSON, jsonBody);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + authToken)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(PostDetailActivity.this,
                        "评论失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(PostDetailActivity.this, "评论成功", Toast.LENGTH_SHORT).show();
                        etCommentInput.setText("");
                        // 刷新评论列表
                        loadComments();
                        // 更新评论计数
                        currentPost.setCommentCount(currentPost.getCommentCount() + 1);
                        tvCommentCount.setText(String.valueOf(currentPost.getCommentCount()));
                    });
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
        String url = "http://10.0.2.2:8080/user/comment/like/" + comment.getCommentId();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + authToken)
                .post(RequestBody.create(new byte[0]))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "点赞失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        comment.setHasLiked(true);
                        comment.setLikeCount(comment.getLikeCount() + 1);
                        commentAdapter.notifyDataSetChanged();
                    });
                }
            }
        });
    }

    private void unlikeComment(Comment comment) {
        String url = "http://10.0.2.2:8080/user/comment/like/" + comment.getCommentId();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + authToken)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "取消点赞失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        comment.setHasLiked(false);
                        comment.setLikeCount(comment.getLikeCount() - 1);
                        commentAdapter.notifyDataSetChanged();
                    });
                }
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
    }
}