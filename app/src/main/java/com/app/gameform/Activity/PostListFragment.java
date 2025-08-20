package com.app.gameform.Activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.adapter.PostAdapter;
import com.app.gameform.domain.Post;

import java.util.ArrayList;
import java.util.List;

public class PostListFragment extends Fragment {
    private static final String ARG_TYPE = "type";

    public static final int TYPE_DRAFT = 0;
    public static final int TYPE_PUBLISHED = 1;
    public static final int TYPE_RECYCLE = 2;

    private int type;
    private RecyclerView recyclerView;
    private LinearLayout emptyLayout;
    private TextView tvEmptyText;
    private PostAdapter postAdapter;
    private List<Post> postList;

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
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(getContext(), postList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(postAdapter);

        // 设置点击监听器
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
                showMoreOptions(post, position);
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
        // TODO: 根据type加载对应的数据
        switch (type) {
            case TYPE_DRAFT:
                loadDraftPosts();
                updateEmptyText("暂无草稿");
                break;
            case TYPE_PUBLISHED:
                loadPublishedPosts();
                updateEmptyText("暂无已发布内容");
                break;
            case TYPE_RECYCLE:
                loadRecyclePosts();
                updateEmptyText("回收站为空");
                break;
        }
    }

    private void loadDraftPosts() {
        // TODO: 加载草稿数据
        // 这里预留接口，实际项目中应该调用API获取草稿数据
        updateUI();
    }

    private void loadPublishedPosts() {
        // TODO: 加载已发布数据
        // 这里预留接口，实际项目中应该调用API获取已发布数据
        updateUI();
    }

    private void loadRecyclePosts() {
        // TODO: 加载回收站数据
        // 这里预留接口，实际项目中应该调用API获取回收站数据
        updateUI();
    }

    private void updateEmptyText(String text) {
        tvEmptyText.setText(text);
    }

    private void updateUI() {
        if (postList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
            postAdapter.notifyDataSetChanged();
        }
    }

    private void showMoreOptions(Post post, int position) {
        // TODO: 显示更多操作选项（编辑、删除等）
        // 可以使用PopupMenu或BottomSheetDialog
    }

    // 公共方法供外部调用
    public void refreshData() {
        loadData();
    }

    public void addPost(Post post) {
        if (postList != null) {
            postList.add(0, post);
            postAdapter.notifyItemInserted(0);
            recyclerView.scrollToPosition(0);
            updateUI();
        }
    }

    public void removePost(int position) {
        if (postAdapter != null) {
            postAdapter.removePost(position);
            updateUI();
        }
    }
}