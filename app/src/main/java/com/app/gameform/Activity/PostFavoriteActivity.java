package com.app.gameform.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.gameform.R;
import com.app.gameform.adapter.PostFavoriteAdapter;
import com.app.gameform.domain.PostFavorite;
import com.app.gameform.manager.PostFavoriteManager;
import com.app.gameform.manager.SharedPrefManager;
import com.app.gameform.network.ApiCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * 收藏列表页面
 * 功能：
 * 1. 显示用户收藏的帖子列表
 * 2. 支持批量删除收藏
 * 3. 点击可进入帖子详情
 */
public class PostFavoriteActivity extends AppCompatActivity {

    private static final String TAG = "PostFavoriteActivity";

    // 视图组件
    private ImageView ivBack;
    private TextView tvTitle;
    private TextView tvEditMode;
    private LinearLayout layoutEditActions;
    private CheckBox cbSelectAll;
    private TextView tvDelete;
    private TextView tvCancel;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private View emptyView;
    private View loadingView;
    private TextView tvEmptyText;

    // 数据和适配器
    private PostFavoriteAdapter adapter;
    private List<PostFavorite> favoriteList = new ArrayList<>();

    // 管理器
    private PostFavoriteManager favoriteManager;
    private SharedPrefManager sharedPrefManager;

    // 状态标识
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_favorite);

        initManagers();
        initViews();
        setupRecyclerView();
        setupListeners();

        loadFavorites();
    }

    /**
     * 初始化管理器
     */
    private void initManagers() {
        favoriteManager = new PostFavoriteManager(this);
        sharedPrefManager = SharedPrefManager.getInstance(this);
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        // 标题栏
        ivBack = findViewById(R.id.iv_back);
        tvTitle = findViewById(R.id.tv_title);
        tvEditMode = findViewById(R.id.tv_edit_mode);

        // 编辑操作区
        layoutEditActions = findViewById(R.id.layout_edit_actions);
        cbSelectAll = findViewById(R.id.cb_select_all);
        tvDelete = findViewById(R.id.tv_delete);
        tvCancel = findViewById(R.id.tv_cancel);

        // 列表
        swipeRefresh = findViewById(R.id.swipe_refresh);
        recyclerView = findViewById(R.id.recycler_view);
        emptyView = findViewById(R.id.empty_view);
        loadingView = findViewById(R.id.loading_view);
        tvEmptyText = findViewById(R.id.tv_empty_text);

        tvTitle.setText("我的收藏");
        tvEmptyText.setText("还没有收藏任何帖子");
    }

    /**
     * 设置RecyclerView
     */
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PostFavoriteAdapter(this, favoriteList, new PostFavoriteAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(PostFavorite favorite) {
                if (!isEditMode) {
                    // 跳转到帖子详情
                    navigateToPostDetail(favorite.getPostId());
                }
            }

            @Override
            public void onCheckChanged(int position, boolean isChecked) {
                // 更新全选按钮状态
                updateSelectAllButton();
            }
        });

        recyclerView.setAdapter(adapter);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 返回按钮
        ivBack.setOnClickListener(v -> finish());

        // 编辑/完成按钮
        tvEditMode.setOnClickListener(v -> toggleEditMode());

        // 下拉刷新
        swipeRefresh.setOnRefreshListener(this::loadFavorites);

        // 全选按钮
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) { // 只响应用户点击
                adapter.selectAll(isChecked);
            }
        });

        // 删除按钮
        tvDelete.setOnClickListener(v -> showDeleteConfirmDialog());

        // 取消按钮
        tvCancel.setOnClickListener(v -> exitEditMode());
    }

    /**
     * 切换编辑模式
     */
    private void toggleEditMode() {
        if (isEditMode) {
            exitEditMode();
        } else {
            enterEditMode();
        }
    }

    /**
     * 进入编辑模式
     */
    private void enterEditMode() {
        if (favoriteList.isEmpty()) {
            Toast.makeText(this, "没有可编辑的收藏", Toast.LENGTH_SHORT).show();
            return;
        }

        isEditMode = true;
        tvEditMode.setText("完成");
        layoutEditActions.setVisibility(View.VISIBLE);
        adapter.setEditMode(true);
        cbSelectAll.setChecked(false);
    }

    /**
     * 退出编辑模式
     */
    private void exitEditMode() {
        isEditMode = false;
        tvEditMode.setText("编辑");
        layoutEditActions.setVisibility(View.GONE);
        adapter.setEditMode(false);
        adapter.clearSelection();
    }

    /**
     * 更新全选按钮状态
     */
    private void updateSelectAllButton() {
        int selectedCount = adapter.getSelectedCount();
        int totalCount = adapter.getItemCount();

        cbSelectAll.setOnCheckedChangeListener(null);
        cbSelectAll.setChecked(selectedCount > 0 && selectedCount == totalCount);
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                adapter.selectAll(isChecked);
            }
        });

        // 更新删除按钮文本
        if (selectedCount > 0) {
            tvDelete.setText("删除(" + selectedCount + ")");
            tvDelete.setEnabled(true);
        } else {
            tvDelete.setText("删除");
            tvDelete.setEnabled(false);
        }
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog() {
        int selectedCount = adapter.getSelectedCount();
        if (selectedCount == 0) {
            Toast.makeText(this, "请选择要删除的收藏", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除选中的 " + selectedCount + " 个收藏吗？")
                .setPositiveButton("删除", (dialog, which) -> deleteFavorites())
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 删除选中的收藏
     */
    private void deleteFavorites() {
        List<Integer> selectedPostIds = adapter.getSelectedPostIds();
        if (selectedPostIds.isEmpty()) {
            return;
        }

        showLoading();

        // 批量删除
        deleteNextFavorite(selectedPostIds, 0, selectedPostIds.size());
    }

    /**
     * 递归删除收藏
     */
    private void deleteNextFavorite(List<Integer> postIds, int index, int total) {
        if (index >= postIds.size()) {
            // 删除完成，在主线程中执行UI操作
            runOnUiThread(() -> {
                hideLoading();
                exitEditMode();
                Toast.makeText(PostFavoriteActivity.this, "已删除 " + total + " 个收藏", Toast.LENGTH_SHORT).show();
                loadFavorites(); // 重新加载列表
            });
            return;
        }

        int postId = postIds.get(index);
        favoriteManager.removeFavorite(postId, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "删除收藏成功: PostId=" + postId);
                // 继续删除下一个
                deleteNextFavorite(postIds, index + 1, total);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "删除收藏失败: PostId=" + postId + ", Error=" + error);
                // 继续删除下一个（即使当前失败）
                deleteNextFavorite(postIds, index + 1, total);
            }
        });
    }

    /**
     * 加载收藏列表
     */
    private void loadFavorites() {
        showLoading();

        favoriteManager.getMyFavorites(new ApiCallback<List<PostFavorite>>() {
            @Override
            public void onSuccess(List<PostFavorite> favorites) {
                favoriteList.clear();
                if (favorites != null && !favorites.isEmpty()) {
                    favoriteList.addAll(favorites);
                }

                Log.d(TAG, "获取收藏列表成功，数量: " + favoriteList.size());

                runOnUiThread(() -> {
                    hideLoading();
                    swipeRefresh.setRefreshing(false);

                    if (favoriteList.isEmpty()) {
                        showEmpty();
                    } else {
                        showContent();
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "获取收藏列表失败: " + error);
                runOnUiThread(() -> {
                    hideLoading();
                    swipeRefresh.setRefreshing(false);
                    showEmpty();
                    Toast.makeText(PostFavoriteActivity.this,
                            "加载失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 跳转到帖子详情
     */
    private void navigateToPostDetail(int postId) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post_id", postId);
        startActivity(intent);
    }

    /**
     * 显示加载中
     */
    private void showLoading() {
        if (!swipeRefresh.isRefreshing()) {
            loadingView.setVisibility(View.VISIBLE);
        }
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    /**
     * 隐藏加载中
     */
    private void hideLoading() {
        loadingView.setVisibility(View.GONE);
    }

    /**
     * 显示空视图
     */
    private void showEmpty() {
        loadingView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        tvEditMode.setVisibility(View.GONE);
    }

    /**
     * 显示内容
     */
    private void showContent() {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        tvEditMode.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从帖子详情返回时，刷新列表（可能取消了收藏）
        if (!isEditMode) {
            loadFavorites();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.cleanup();
        }
    }
}