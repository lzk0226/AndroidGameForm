package com.app.gameform.Activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.adapter.DraftAdapter;
import com.app.gameform.adapter.PostAdapter;
import com.app.gameform.domain.Draft;
import com.app.gameform.domain.Post;
import com.app.gameform.manager.DraftManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PostListFragment extends Fragment {
    private static final String ARG_TYPE = "type";

    public static final int TYPE_DRAFT = 0;
    public static final int TYPE_PUBLISHED = 1;

    private int type;
    private RecyclerView recyclerView;
    private LinearLayout emptyLayout;
    private TextView tvEmptyText;

    // 根据类型使用不同的适配器
    private PostAdapter postAdapter;
    private DraftAdapter draftAdapter;
    private List<Post> postList;
    private List<Draft> draftList;

    // 草稿管理器
    private DraftManager draftManager;

    public static PostListFragment newInstance(int type) {
        PostListFragment fragment = new PostListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = getArguments().getInt(ARG_TYPE);
        }
        // 初始化草稿管理器
        if (type == TYPE_DRAFT) {
            draftManager = DraftManager.getInstance(requireContext());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post_list, container, false);
        initViews(view);
        setupRecyclerView();
        loadData();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyLayout = view.findViewById(R.id.emptyLayout);
        tvEmptyText = view.findViewById(R.id.tv_empty_text);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (type == TYPE_DRAFT) {
            setupDraftRecyclerView();
        } else {
            setupPostRecyclerView();
        }
    }

    private void setupDraftRecyclerView() {
        draftList = new ArrayList<>();
        draftAdapter = new DraftAdapter(getContext(), draftList);
        recyclerView.setAdapter(draftAdapter);

        draftAdapter.setOnDraftClickListener(new DraftAdapter.OnDraftClickListener() {
            @Override
            public void onDraftClick(Draft draft, int position) {
                // 编辑草稿 - 跳转到发帖页面
                editDraft(draft);
            }

            @Override
            public void onMoreClick(Draft draft, int position) {
                // 显示草稿操作菜单（删除等）
                showDraftOptions(draft, position);
            }
        });
    }

    private void setupPostRecyclerView() {
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(getContext(), postList);
        recyclerView.setAdapter(postAdapter);

        postAdapter.setOnPostClickListener(new PostAdapter.OnPostClickListener() {
            @Override
            public void onPostClick(Post post, int position) {
                // TODO: 处理帖子点击事件
            }

            @Override
            public void onUserClick(Post post, int position) {
                // TODO: 处理用户点击事件
            }

            @Override
            public void onCommentClick(Post post, int position) {
                // TODO: 处理评论点击事件
            }

            @Override
            public void onViewClick(Post post, int position) {
                // TODO: 处理浏览点击事件
            }

            @Override
            public void onMoreClick(Post post, int position) {
                // TODO: 处理更多操作点击事件
                showPostOptions(post, position);
            }
        });

        postAdapter.setOnPostLikeListener(new PostAdapter.OnPostLikeListener() {
            @Override
            public void onLikeClick(Post post, int position) {
                // TODO: 处理点赞事件
            }
        });
    }

    private void loadData() {
        switch (type) {
            case TYPE_DRAFT:
                loadDraftPosts();
                updateEmptyText("暂无草稿");
                break;
            case TYPE_PUBLISHED:
                loadPublishedPosts();
                updateEmptyText("暂无已发布内容");
                break;
        }
    }

    private void loadDraftPosts() {
        // 从本地加载草稿数据
        if (draftManager != null) {
            List<Draft> localDrafts = draftManager.getAllDrafts();
            draftList.clear();
            draftList.addAll(localDrafts);
        }
        updateUI();
    }

    private void loadPublishedPosts() {
        // TODO: 加载已发布数据
        // 这里预留接口，实际项目中应该调用API获取已发布数据
        updateUI();
    }

    private void updateEmptyText(String text) {
        tvEmptyText.setText(text);
    }

    private void updateUI() {
        if (type == TYPE_DRAFT) {
            if (draftList.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyLayout.setVisibility(View.GONE);
                draftAdapter.notifyDataSetChanged();
            }
        } else {
            if (postList.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyLayout.setVisibility(View.GONE);
                postAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * 编辑草稿 - 跳转到发帖页面
     */
    private void editDraft(Draft draft) {
        if (draft != null && draft.getDraftId() != null) {
            // 使用NewPostActivity的静态方法启动编辑草稿
            NewPostActivity.startForEditDraft(requireContext(), draft.getDraftId());
        }
    }

    /**
     * 显示草稿操作选项对话框
     */
    private void showDraftOptions(Draft draft, int position) {
        if (draft == null || getContext() == null) return;

        String[] options = {"编辑", "删除"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(draft.getDisplayTitle());
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // 编辑
                    editDraft(draft);
                    break;
                case 1: // 删除
                    showDeleteDraftConfirm(draft, position);
                    break;
            }
        });
        builder.show();
    }

    /**
     * 显示删除草稿确认对话框
     */
    private void showDeleteDraftConfirm(Draft draft, int position) {
        if (draft == null || getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("删除草稿");
        builder.setMessage("确定要删除草稿「" + draft.getDisplayTitle() + "」吗？");

        builder.setPositiveButton("删除", (dialog, which) -> {
            deleteDraft(draft, position);
        });

        builder.setNegativeButton("取消", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 删除草稿
     */
    private void deleteDraft(Draft draft, int position) {
        if (draft == null || draftManager == null) return;

        boolean success = draftManager.deleteDraft(draft.getDraftId());
        if (success) {
            // 从列表中移除
            if (position >= 0 && position < draftList.size()) {
                draftList.remove(position);
                draftAdapter.notifyItemRemoved(position);
                draftAdapter.notifyItemRangeChanged(position, draftList.size());
            }
            updateUI();
            Toast.makeText(getContext(), "草稿已删除", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPostOptions(Post post, int position) {
        // TODO: 显示帖子操作选项（编辑、删除等）
        // 可以使用PopupMenu或BottomSheetDialog
    }

    // 公共方法供外部调用
    public void refreshData() {
        loadData();
    }

    public void addPost(Post post) {
        if (type == TYPE_PUBLISHED && postList != null) {
            postList.add(0, post);
            postAdapter.notifyItemInserted(0);
            recyclerView.scrollToPosition(0);
            updateUI();
        }
    }

    public void addDraft(Draft draft) {
        if (type == TYPE_DRAFT && draftList != null) {
            draftList.add(0, draft);
            draftAdapter.notifyItemInserted(0);
            recyclerView.scrollToPosition(0);
            updateUI();
        }
    }

    public void removeItem(int position) {
        if (type == TYPE_DRAFT && draftAdapter != null && position >= 0 && position < draftList.size()) {
            Draft draft = draftList.get(position);
            showDeleteDraftConfirm(draft, position);
        } else if (type == TYPE_PUBLISHED && postAdapter != null && position >= 0 && position < postList.size()) {
            postAdapter.removePost(position);
            updateUI();
        }
    }

    /**
     * 页面恢复时刷新草稿数据
     */
    @Override
    public void onResume() {
        super.onResume();
        if (type == TYPE_DRAFT) {
            // 从其他页面返回时刷新草稿列表
            loadDraftPosts();
        }
    }
}