package com.app.gameform.Activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.gameform.Activity.Home.BaseActivity;
import com.app.gameform.R;
import com.app.gameform.adapter.UserListAdapter;
import com.app.gameform.domain.UserFollow;
import com.app.gameform.manager.SharedPrefManager;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.UserApiService;

import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends BaseActivity {

    public static final String EXTRA_LIST_TYPE = "list_type";
    public static final int TYPE_FOLLOWING = 1; // 关注列表
    public static final int TYPE_FOLLOWERS = 2; // 粉丝列表

    private ImageView ivBack;
    private TextView tvTitle;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvUserList;
    private LinearLayout emptyLayout;
    private TextView tvEmptyMessage;
    private ProgressBar progressBar;

    private UserListAdapter adapter;
    private SharedPrefManager sharedPrefManager;
    private UserApiService userApiService;

    private int listType;
    private List<UserFollow> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        // 获取列表类型
        listType = getIntent().getIntExtra(EXTRA_LIST_TYPE, TYPE_FOLLOWING);

        initViews();
        initServices();
        setupRecyclerView();
        loadData();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvUserList = findViewById(R.id.rvUserList);
        emptyLayout = findViewById(R.id.emptyLayout);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        progressBar = findViewById(R.id.progressBar);

        // 设置标题
        if (listType == TYPE_FOLLOWING) {
            tvTitle.setText("关注列表");
            tvEmptyMessage.setText("还没有关注任何用户");
        } else {
            tvTitle.setText("粉丝列表");
            tvEmptyMessage.setText("还没有粉丝");
        }

        // 返回按钮
        ivBack.setOnClickListener(v -> finish());

        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener(() -> loadData());
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
    }

    private void initServices() {
        sharedPrefManager = SharedPrefManager.getInstance(this);
        userApiService = UserApiService.getInstance();
    }

    private void setupRecyclerView() {
        adapter = new UserListAdapter(this, userList, listType);

        // 设置按钮点击监听
        adapter.setOnActionClickListener(new UserListAdapter.OnActionClickListener() {
            @Override
            public void onFollowClick(UserFollow user, int position) {
                handleFollow(user, position);
            }

            @Override
            public void onUnfollowClick(UserFollow user, int position) {
                handleUnfollow(user, position);
            }
        });

        rvUserList.setLayoutManager(new LinearLayoutManager(this));
        rvUserList.setAdapter(adapter);
    }

    /**
     * 加载数据
     */
    private void loadData() {
        String token = sharedPrefManager.getToken();

        if (TextUtils.isEmpty(token)) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading();

        if (listType == TYPE_FOLLOWING) {
            loadFollowingList(token);
        } else {
            loadFollowersList(token);
        }
    }

    /**
     * 加载关注列表
     */
    private void loadFollowingList(String token) {
        userApiService.getMyFollowing(token, new ApiCallback<List<UserFollow>>() {
            @Override
            public void onSuccess(List<UserFollow> data) {
                runOnUiThread(() -> {
                    hideLoading();
                    userList.clear();
                    if (data != null && !data.isEmpty()) {
                        userList.addAll(data);
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(UserListActivity.this, "加载失败: " + error, Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
            }
        });
    }

    /**
     * 加载粉丝列表
     */
    private void loadFollowersList(String token) {
        userApiService.getMyFollowers(token, new ApiCallback<List<UserFollow>>() {
            @Override
            public void onSuccess(List<UserFollow> data) {
                runOnUiThread(() -> {
                    hideLoading();
                    userList.clear();
                    if (data != null && !data.isEmpty()) {
                        userList.addAll(data);
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(UserListActivity.this, "加载失败: " + error, Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
            }
        });
    }

    /**
     * 处理关注
     */
    private void handleFollow(UserFollow user, int position) {
        String token = sharedPrefManager.getToken();
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        long userId = listType == TYPE_FOLLOWING ? user.getFollowingId() : user.getFollowerId();

        userApiService.followUser(userId, token, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                runOnUiThread(() -> {
                    Toast.makeText(UserListActivity.this, "关注成功", Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(UserListActivity.this, "关注失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 处理取消关注
     */
    private void handleUnfollow(UserFollow user, int position) {
        String token = sharedPrefManager.getToken();
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        long userId = user.getFollowingId();

        userApiService.unfollowUser(userId, token, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                runOnUiThread(() -> {
                    Toast.makeText(UserListActivity.this, "取消关注成功", Toast.LENGTH_SHORT).show();
                    // 从列表中移除
                    userList.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(UserListActivity.this, "取消关注失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 显示加载中
     */
    private void showLoading() {
        if (userList.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
            rvUserList.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.GONE);
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * 隐藏加载中
     */
    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * 更新空状态显示
     */
    private void updateEmptyState() {
        if (userList.isEmpty()) {
            rvUserList.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            rvUserList.setVisibility(View.VISIBLE);
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