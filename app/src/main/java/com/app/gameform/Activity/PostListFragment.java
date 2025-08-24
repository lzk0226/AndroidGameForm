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
import com.app.gameform.adapter.DraftAdapter;
import com.app.gameform.adapter.PostAdapter;
import com.app.gameform.domain.Draft;
import com.app.gameform.domain.Post;

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
                // TODO: 处理草稿点击事件，跳转到编辑页面
                editDraft(draft);
            }

            @Override
            public void onMoreClick(Draft draft, int position) {
                // TODO: 显示草稿操作菜单（编辑、删除、发布等）
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
        // TODO: 加载草稿数据
        // 这里预留接口，实际项目中应该调用API获取草稿数据

        // 模拟数据
        draftList.clear();
        for (int i = 1; i <= 3; i++) {
            Draft draft = new Draft();
            draft.setDraftId(i);
            draft.setDraftTitle(""); // 空标题，会显示为"未命名草稿"
            draft.setDraftContent("这是草稿内容 " + i);
            draft.setCreateTime(new Date());
            draft.setUpdateTime(new Date(System.currentTimeMillis() - i * 60000)); // i分钟前
            draftList.add(draft);
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

    private void editDraft(Draft draft) {
        // TODO: 跳转到编辑页面
    }

    private void showDraftOptions(Draft draft, int position) {
        // TODO: 显示草稿操作选项（编辑、删除、发布等）
        // 可以使用PopupMenu或BottomSheetDialog
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
        if (type == TYPE_DRAFT && draftAdapter != null) {
            draftAdapter.removeDraft(position);
            updateUI();
        } else if (type == TYPE_PUBLISHED && postAdapter != null) {
            postAdapter.removePost(position);
            updateUI();
        }
    }
}