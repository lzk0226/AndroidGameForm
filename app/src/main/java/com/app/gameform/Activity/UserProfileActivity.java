package com.app.gameform.Activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.adapter.PostAdapter;
import com.app.gameform.domain.Post;
import com.app.gameform.domain.User;
import com.app.gameform.manager.SharedPrefManager;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.ApiConstants;
import com.app.gameform.network.ApiService;
import com.app.gameform.network.UserApiService;
import com.app.gameform.utils.ImageUtils;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "user_id";

    private ImageView ivBack;
    private TextView tvTitle;
    private CircleImageView ivAvatar;
    private TextView tvNickname;
    private TextView tvUserType;
    private CardView btnFollowCard;
    private TextView btnFollow;
    private TextView tvPostCount;
    private TextView tvFollowingCount;
    private TextView tvFollowersCount;
    private LinearLayout layoutPosts;
    private LinearLayout layoutFollowing;
    private LinearLayout layoutFollowers;
    private RecyclerView rvPosts;
    private LinearLayout emptyLayout;
    private ProgressBar progressBar;

    private PostAdapter postAdapter;
    private List<Post> postList = new ArrayList<>();

    private SharedPrefManager sharedPrefManager;
    private UserApiService userApiService;

    private long userId;
    private User userProfile;
    private boolean isFollowing = false;
    private boolean followLoading = false;

    public static void start(Context context, long userId) {
        Intent intent = new Intent(context, UserProfileActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        userId = getIntent().getLongExtra(EXTRA_USER_ID, 0);
        if (userId == 0) {
            Toast.makeText(this, "用户ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initServices();
        setupRecyclerView();
        loadData();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvNickname = findViewById(R.id.tvNickname);
        tvUserType = findViewById(R.id.tvUserType);
        btnFollowCard = findViewById(R.id.btnFollowCard);
        btnFollow = findViewById(R.id.btnFollow);
        tvPostCount = findViewById(R.id.tvPostCount);
        tvFollowingCount = findViewById(R.id.tvFollowingCount);
        tvFollowersCount = findViewById(R.id.tvFollowersCount);
        layoutPosts = findViewById(R.id.layoutPosts);
        layoutFollowing = findViewById(R.id.layoutFollowing);
        layoutFollowers = findViewById(R.id.layoutFollowers);
        rvPosts = findViewById(R.id.rvPosts);
        emptyLayout = findViewById(R.id.emptyLayout);
        progressBar = findViewById(R.id.progressBar);

        ivBack.setOnClickListener(v -> finish());

        btnFollow.setOnClickListener(v -> handleFollowToggle());

        layoutFollowing.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserListActivity.class);
            intent.putExtra(UserListActivity.EXTRA_LIST_TYPE, UserListActivity.TYPE_FOLLOWING);
            startActivity(intent);
        });

        layoutFollowers.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserListActivity.class);
            intent.putExtra(UserListActivity.EXTRA_LIST_TYPE, UserListActivity.TYPE_FOLLOWERS);
            startActivity(intent);
        });
    }

    private void initServices() {
        sharedPrefManager = SharedPrefManager.getInstance(this);
        userApiService = UserApiService.getInstance();
    }

    private void setupRecyclerView() {
        postAdapter = new PostAdapter(this, postList);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(postAdapter);

        postAdapter.setOnPostClickListener(new PostAdapter.OnPostClickListener() {
            @Override
            public void onPostClick(Post post, int position) {
                // ✅ 改为 "post_id"
                Intent intent = new Intent(UserProfileActivity.this, PostDetailActivity.class);
                intent.putExtra("post_id", post.getPostId());
                startActivity(intent);
            }

            @Override
            public void onUserClick(Post post, int position) {
                // 已经在用户主页,不需要处理
            }

            @Override
            public void onCommentClick(Post post, int position) {
                // ✅ 改为 "post_id"
                Intent intent = new Intent(UserProfileActivity.this, PostDetailActivity.class);
                intent.putExtra("post_id", post.getPostId());
                startActivity(intent);
            }

            @Override
            public void onViewClick(Post post, int position) {
                // ✅ 改为 "post_id"
                Intent intent = new Intent(UserProfileActivity.this, PostDetailActivity.class);
                intent.putExtra("post_id", post.getPostId());
                startActivity(intent);
            }

            @Override
            public void onMoreClick(Post post, int position) {
                // 不显示更多选项
            }

            @Override
            public void onDeleteClick(Post post, int position) {
                // 不允许删除他人帖子
            }
        });

        postAdapter.setOnPostLikeListener((post, position) -> {
            // 处理点赞
            handlePostLike(post, position);
        });
    }

    private void loadData() {
        showLoading();
        loadUserProfile();
        loadUserPosts();
        checkFollowStatus();
        loadFollowingCount();
        loadFollowersCount();
    }

    private void loadUserProfile() {
        String token = sharedPrefManager.getToken();
        if (TextUtils.isEmpty(token)) {
            return;
        }

        userApiService.getUserInfo(userId, token, new ApiCallback<User>() {
            @Override
            public void onSuccess(User data) {
                runOnUiThread(() -> {
                    userProfile = data;
                    updateUserInfo();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(UserProfileActivity.this, "加载用户信息失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateUserInfo() {
        if (userProfile == null) return;

        tvNickname.setText(userProfile.getNickName());
        tvTitle.setText(userProfile.getNickName() + "的主页");

        // 用户类型
        String userType = "10".equals(userProfile.getUserType()) ? "普通用户" : "系统用户";
        tvUserType.setText(userType);

        // 加载头像
        if (!TextUtils.isEmpty(userProfile.getAvatar())) {
            ImageUtils.loadUserAvatar(this, ivAvatar, userProfile.getAvatar());
        }
    }

    private void loadUserPosts() {
        String url = ApiConstants.GET_POSTS_BY_USER + userId;

        ApiService.getInstance().getPosts(url, new ApiCallback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> data) {
                runOnUiThread(() -> {
                    hideLoading();
                    postList.clear();
                    if (data != null && !data.isEmpty()) {
                        postList.addAll(data);
                    }
                    postAdapter.notifyDataSetChanged();
                    updateEmptyState();
                    tvPostCount.setText(String.valueOf(postList.size()));
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    updateEmptyState();
                    tvPostCount.setText("0");
                });
            }
        });
    }

    private void checkFollowStatus() {
        String token = sharedPrefManager.getToken();
        if (TextUtils.isEmpty(token)) {
            return;
        }

        userApiService.checkFollowStatus(userId, token, new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                runOnUiThread(() -> {
                    isFollowing = data != null && data;
                    updateFollowButton();
                });
            }

            @Override
            public void onError(String error) {
                // 忽略错误
            }
        });
    }

    private void loadFollowingCount() {
        String url = ApiConstants.GET_USER_FOLLOWING + userId;
        String token = sharedPrefManager.getToken();

        if (TextUtils.isEmpty(token)) {
            return;
        }

        ApiService.getInstance().getRequestWithAuth(this, url, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    ApiService.ApiResponse<List> apiResponse = ApiService.getInstance().getGson().fromJson(
                            response,
                            new TypeToken<ApiService.ApiResponse<List>>(){}.getType()
                    );

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        int count = apiResponse.getData().size();
                        runOnUiThread(() -> tvFollowingCount.setText(String.valueOf(count)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {
                // 忽略错误
            }
        });
    }

    private void loadFollowersCount() {
        String url = ApiConstants.GET_USER_FOLLOWERS + userId;
        String token = sharedPrefManager.getToken();

        if (TextUtils.isEmpty(token)) {
            return;
        }

        ApiService.getInstance().getRequestWithAuth(this, url, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    ApiService.ApiResponse<List> apiResponse = ApiService.getInstance().getGson().fromJson(
                            response,
                            new TypeToken<ApiService.ApiResponse<List>>(){}.getType()
                    );

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        int count = apiResponse.getData().size();
                        runOnUiThread(() -> tvFollowersCount.setText(String.valueOf(count)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {
                // 忽略错误
            }
        });
    }

    private void handleFollowToggle() {
        if (followLoading) return;

        String token = sharedPrefManager.getToken();
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        followLoading = true;
        btnFollow.setEnabled(false);

        if (isFollowing) {
            // 取消关注
            userApiService.unfollowUser(userId, token, new ApiCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    runOnUiThread(() -> {
                        followLoading = false;
                        btnFollow.setEnabled(true);
                        isFollowing = false;
                        updateFollowButton();
                        Toast.makeText(UserProfileActivity.this, "已取消关注", Toast.LENGTH_SHORT).show();
                        loadFollowersCount();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        followLoading = false;
                        btnFollow.setEnabled(true);
                        Toast.makeText(UserProfileActivity.this, "取消关注失败: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            // 关注
            userApiService.followUser(userId, token, new ApiCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    runOnUiThread(() -> {
                        followLoading = false;
                        btnFollow.setEnabled(true);
                        isFollowing = true;
                        updateFollowButton();
                        Toast.makeText(UserProfileActivity.this, "关注成功", Toast.LENGTH_SHORT).show();
                        loadFollowersCount();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        followLoading = false;
                        btnFollow.setEnabled(true);
                        Toast.makeText(UserProfileActivity.this, "关注失败: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void updateFollowButton() {
        if (isFollowing) {
            btnFollow.setText("已关注");
            btnFollowCard.setCardBackgroundColor(0xFFE0E0E0);
            btnFollow.setTextColor(0xFF666666);
        } else {
            btnFollow.setText("关注");
            btnFollowCard.setCardBackgroundColor(0xFF007AFF);
            btnFollow.setTextColor(0xFFFFFFFF);
        }
    }

    private void handlePostLike(Post post, int position) {
        ApiService.getInstance().checkPostLikeStatus(this, post.getPostId(), new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean hasLiked) {
                runOnUiThread(() -> {
                    if (hasLiked) {
                        unlikePost(post, position);
                    } else {
                        likePost(post, position);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(UserProfileActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void likePost(Post post, int position) {
        ApiService.getInstance().likePost(this, post.getPostId(), new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                if (success) {
                    runOnUiThread(() -> {
                        int currentCount = post.getLikeCount() != null ? post.getLikeCount() : 0;
                        post.setLikeCount(currentCount + 1);
                        postAdapter.notifyItemChanged(position);
                    });
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(UserProfileActivity.this, "点赞失败", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void unlikePost(Post post, int position) {
        ApiService.getInstance().unlikePost(this, post.getPostId(), new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                if (success) {
                    runOnUiThread(() -> {
                        int currentCount = post.getLikeCount() != null ? post.getLikeCount() : 0;
                        post.setLikeCount(Math.max(0, currentCount - 1));
                        postAdapter.notifyItemChanged(position);
                    });
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(UserProfileActivity.this, "取消点赞失败", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void updateEmptyState() {
        if (postList.isEmpty()) {
            rvPosts.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            rvPosts.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userApiService != null) {
            userApiService.cancelAllRequests();
        }
    }
}