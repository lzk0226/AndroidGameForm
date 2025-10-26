package com.app.gameform.Activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.app.gameform.manager.SharedPrefManager;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.ApiConstants;
import com.app.gameform.network.ApiService;

import java.util.ArrayList;
import java.util.List;

public class PostListFragment extends Fragment {
    private static final String TAG = "PostListFragment";
    private static final String ARG_TYPE = "type";

    public static final int TYPE_DRAFT = 0;
    public static final int TYPE_PUBLISHED = 1;

    private int type;
    private RecyclerView recyclerView;
    private LinearLayout emptyLayout;
    private TextView tvEmptyText;

    // 懒加载相关变量
    private static final int PAGE_SIZE = 8;
    private static final int LOAD_MORE_THRESHOLD = 6;
    private int currentPage = 1;
    private boolean hasMoreData = true;
    private boolean isLoading = false;

    // 根据类型使用不同的适配器
    private PostAdapter postAdapter;
    private DraftAdapter draftAdapter;
    private List<Post> postList;
    private List<Draft> draftList;
    private LinearLayoutManager layoutManager;

    // 管理器
    private DraftManager draftManager;
    private SharedPrefManager sharedPrefManager;

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

        // 初始化管理器
        sharedPrefManager = SharedPrefManager.getInstance(requireContext());

        if (type == TYPE_DRAFT) {
            draftManager = DraftManager.getInstance(requireContext());
        }

        Log.d(TAG, "Fragment创建 - 类型: " + (type == TYPE_DRAFT ? "草稿" : "已发布"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post_list, container, false);
        initViews(view);
        setupRecyclerView();
        setupScrollListener();
        loadData();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyLayout = view.findViewById(R.id.emptyLayout);
        tvEmptyText = view.findViewById(R.id.tv_empty_text);
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        if (type == TYPE_DRAFT) {
            setupDraftRecyclerView();
        } else {
            setupPostRecyclerView();
        }
    }

    private void setupScrollListener() {
        if (type == TYPE_PUBLISHED) {
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    if (dy > 0 && !isLoading && hasMoreData) {
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                        int lastVisibleItem = firstVisibleItemPosition + visibleItemCount;

                        if ((totalItemCount - lastVisibleItem) <= (PAGE_SIZE - LOAD_MORE_THRESHOLD)) {
                            loadMorePosts();
                        }
                    }
                }
            });
        }
    }

    private void setupDraftRecyclerView() {
        draftList = new ArrayList<>();
        draftAdapter = new DraftAdapter(getContext(), draftList);
        recyclerView.setAdapter(draftAdapter);

        draftAdapter.setOnDraftClickListener(new DraftAdapter.OnDraftClickListener() {
            @Override
            public void onDraftClick(Draft draft, int position) {
                editDraft(draft);
            }

            @Override
            public void onMoreClick(Draft draft, int position) {
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
                Log.d(TAG, "帖子点击 - ID: " + post.getPostId());
            }

            @Override
            public void onUserClick(Post post, int position) {
                // TODO: 处理用户点击事件
                Log.d(TAG, "用户点击 - ID: " + post.getUserId());
            }

            @Override
            public void onCommentClick(Post post, int position) {
                // TODO: 处理评论点击事件
                Log.d(TAG, "评论点击 - 帖子ID: " + post.getPostId());
            }

            @Override
            public void onViewClick(Post post, int position) {
                // TODO: 处理浏览点击事件
                Log.d(TAG, "浏览点击 - 帖子ID: " + post.getPostId());
            }

            @Override
            public void onMoreClick(Post post, int position) {
                showPostOptions(post, position);
            }

            @Override
            public void onDeleteClick(Post post, int position) {
                // 暂时不需要删除功能
            }
        });

        postAdapter.setOnPostLikeListener(new PostAdapter.OnPostLikeListener() {
            @Override
            public void onLikeClick(Post post, int position) {
                handlePostLike(post, position);
            }
        });

        postAdapter.setUserPostList(true);
    }

    private void loadData() {
        resetPaginationData();

        // ⭐ 添加登录状态检查和详细日志
        Log.d(TAG, "========== 开始加载数据 ==========");
        Log.d(TAG, "Fragment类型: " + (type == TYPE_DRAFT ? "草稿" : "已发布"));
        Log.d(TAG, "是否已登录: " + sharedPrefManager.isLoggedIn());
        Log.d(TAG, "用户ID: " + sharedPrefManager.getUserId());
        Log.d(TAG, "Token存在: " + (sharedPrefManager.getToken() != null));

        if (sharedPrefManager.getToken() != null) {
            String token = sharedPrefManager.getToken();
            Log.d(TAG, "Token长度: " + token.length());
            Log.d(TAG, "Token前30字符: " + token.substring(0, Math.min(30, token.length())));
        }

        switch (type) {
            case TYPE_DRAFT:
                loadDraftPosts();
                updateEmptyText("暂无草稿");
                break;
            case TYPE_PUBLISHED:
                // ⭐ 检查登录状态
                if (!sharedPrefManager.isLoggedIn()) {
                    Log.w(TAG, "用户未登录，无法加载已发布帖子");
                    Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
                    updateUI();
                    return;
                }

                loadPublishedPosts(false);
                updateEmptyText("暂无已发布内容");
                break;
        }
    }

    private void resetPaginationData() {
        currentPage = 1;
        hasMoreData = true;
        isLoading = false;
    }

    private void loadMorePosts() {
        if (type == TYPE_PUBLISHED && !isLoading && hasMoreData) {
            currentPage++;
            loadPublishedPosts(true);
        }
    }

    private void loadDraftPosts() {
        Log.d(TAG, "开始加载草稿...");

        if (draftManager != null) {
            List<Draft> localDrafts = draftManager.getAllDrafts();
            draftList.clear();
            draftList.addAll(localDrafts);

            Log.d(TAG, "草稿加载完成 - 数量: " + draftList.size());
        }
        updateUI();
    }

    private void loadPublishedPosts(boolean isLoadMore) {
        if (isLoading) {
            Log.d(TAG, "正在加载中，跳过重复请求");
            return;
        }

        isLoading = true;

        // ⭐ 添加详细的请求日志
        String url = ApiConstants.GET_MY_POSTS + "?page=" + currentPage + "&size=" + PAGE_SIZE;
        Log.d(TAG, "========== 请求已发布帖子 ==========");
        Log.d(TAG, "请求URL: " + url);
        Log.d(TAG, "当前页: " + currentPage);
        Log.d(TAG, "是否加载更多: " + isLoadMore);
        Log.d(TAG, "用户ID: " + sharedPrefManager.getUserId());

        String token = sharedPrefManager.getToken();
        if (token == null) {
            Log.e(TAG, "❌ Token为空，无法发起请求");
            isLoading = false;
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Token: Bearer " + token.substring(0, Math.min(30, token.length())) + "...");
        Log.d(TAG, "=====================================");

        ApiService.getInstance().getRequestWithAuth(
                getContext(),
                url,
                new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String response) {
                        Log.d(TAG, "✅ API请求成功");
                        Log.d(TAG, "响应内容: " + response);

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                isLoading = false;
                                try {
                                    ApiService.ApiResponse<List<Post>> apiResponse =
                                            ApiService.getInstance().getGson().fromJson(
                                                    response,
                                                    new com.google.gson.reflect.TypeToken<ApiService.ApiResponse<List<Post>>>(){}.getType()
                                            );

                                    Log.d(TAG, "响应解析成功 - code: " + apiResponse.getCode() + ", msg: " + apiResponse.getMsg());

                                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                        List<Post> newPosts = apiResponse.getData();
                                        Log.d(TAG, "获取到帖子数量: " + newPosts.size());

                                        hasMoreData = newPosts.size() == PAGE_SIZE;

                                        if (isLoadMore) {
                                            int startPosition = postList.size();
                                            postList.addAll(newPosts);
                                            postAdapter.notifyItemRangeInserted(startPosition, newPosts.size());
                                            Log.d(TAG, "追加帖子 - 从位置: " + startPosition + ", 数量: " + newPosts.size());
                                        } else {
                                            postList.clear();
                                            postList.addAll(newPosts);
                                            postAdapter.notifyDataSetChanged();
                                            Log.d(TAG, "刷新帖子列表 - 总数: " + postList.size());
                                        }
                                    } else {
                                        Log.w(TAG, "API返回失败或无数据");
                                        if (!isLoadMore) {
                                            postList.clear();
                                        }
                                        hasMoreData = false;
                                        Toast.makeText(getContext(),
                                                apiResponse.getMsg() != null ? apiResponse.getMsg() : "获取数据失败",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    updateUI();
                                } catch (Exception e) {
                                    Log.e(TAG, "❌ 解析响应失败: " + e.getMessage());
                                    e.printStackTrace();

                                    if (!isLoadMore) {
                                        postList.clear();
                                    }
                                    hasMoreData = false;
                                    updateUI();
                                    Toast.makeText(getContext(), "解析数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ API请求失败: " + error);

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                isLoading = false;

                                if (isLoadMore && currentPage > 1) {
                                    currentPage--;
                                } else if (!isLoadMore) {
                                    postList.clear();
                                }

                                updateUI();
                                Toast.makeText(getContext(), "加载失败: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }
        );
    }

    private void updateEmptyText(String text) {
        tvEmptyText.setText(text);
    }

    private void updateUI() {
        if (type == TYPE_DRAFT) {
            if (draftList.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.VISIBLE);
                Log.d(TAG, "显示草稿空视图");
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyLayout.setVisibility(View.GONE);
                draftAdapter.notifyDataSetChanged();
                Log.d(TAG, "显示草稿列表 - 数量: " + draftList.size());
            }
        } else {
            if (postList.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.VISIBLE);
                Log.d(TAG, "显示已发布帖子空视图");
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyLayout.setVisibility(View.GONE);
                Log.d(TAG, "显示已发布帖子列表 - 数量: " + postList.size());
            }
        }
    }

    private void editDraft(Draft draft) {
        if (draft != null && draft.getDraftId() != null) {
            NewPostActivity.startForEditDraft(requireContext(), draft.getDraftId());
        }
    }

    private void showDraftOptions(Draft draft, int position) {
        if (draft == null || getContext() == null) return;

        String[] options = {"编辑", "删除"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(draft.getDisplayTitle());
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    editDraft(draft);
                    break;
                case 1:
                    showDeleteDraftConfirm(draft, position);
                    break;
            }
        });
        builder.show();
    }

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

    private void deleteDraft(Draft draft, int position) {
        if (draft == null || draftManager == null) return;

        boolean success = draftManager.deleteDraft(draft.getDraftId());
        if (success) {
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
        if (post == null || getContext() == null) return;

        String[] options = {"编辑", "删除"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(post.getPostTitle());
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    editPost(post);
                    break;
                case 1:
                    showDeletePostConfirm(post, position);
                    break;
            }
        });
        builder.show();
    }

    private void editPost(Post post) {
        Toast.makeText(getContext(), "编辑功能待开发", Toast.LENGTH_SHORT).show();
    }

    private void showDeletePostConfirm(Post post, int position) {
        if (post == null || getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("删除帖子");
        builder.setMessage("确定要删除帖子「" + post.getPostTitle() + "」吗？");

        builder.setPositiveButton("删除", (dialog, which) -> {
            deletePost(post, position);
        });

        builder.setNegativeButton("取消", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deletePost(Post post, int position) {
        if (post == null || getContext() == null) return;

        String deleteUrl = ApiConstants.USER_POST + post.getPostId();
        Log.d(TAG, "删除帖子 - URL: " + deleteUrl);

        ApiService.getInstance().deleteRequestWithAuth(
                getContext(),
                deleteUrl,
                new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String response) {
                        Log.d(TAG, "✅ 删除帖子成功 - ID: " + post.getPostId());

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (position >= 0 && position < postList.size()) {
                                    postList.remove(position);
                                    postAdapter.notifyItemRemoved(position);
                                    postAdapter.notifyItemRangeChanged(position, postList.size());
                                }
                                updateUI();
                                Toast.makeText(getContext(), "帖子已删除", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ 删除帖子失败: " + error);

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "删除失败: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }
        );
    }

    private void handlePostLike(Post post, int position) {
        if (post == null || getContext() == null) return;

        ApiService.getInstance().checkPostLikeStatus(
                getContext(),
                post.getPostId(),
                new ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean hasLiked) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (hasLiked) {
                                    unlikePost(post, position);
                                } else {
                                    likePost(post, position);
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "检查点赞状态失败: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }
        );
    }

    private void likePost(Post post, int position) {
        ApiService.getInstance().likePost(
                getContext(),
                post.getPostId(),
                new ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean success) {
                        if (getActivity() != null && success) {
                            getActivity().runOnUiThread(() -> {
                                int currentCount = post.getLikeCount() != null ? post.getLikeCount() : 0;
                                post.setLikeCount(currentCount + 1);
                                postAdapter.notifyItemChanged(position);
                                Toast.makeText(getContext(), "点赞成功", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "点赞失败: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }
        );
    }

    private void unlikePost(Post post, int position) {
        ApiService.getInstance().unlikePost(
                getContext(),
                post.getPostId(),
                new ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean success) {
                        if (getActivity() != null && success) {
                            getActivity().runOnUiThread(() -> {
                                int currentCount = post.getLikeCount() != null ? post.getLikeCount() : 0;
                                post.setLikeCount(Math.max(0, currentCount - 1));
                                postAdapter.notifyItemChanged(position);
                                Toast.makeText(getContext(), "取消点赞成功", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "取消点赞失败: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }
        );
    }

    // 公共方法供外部调用
    public void refreshData() {
        Log.d(TAG, "触发刷新数据");
        resetPaginationData();
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
            Post post = postList.get(position);
            showDeletePostConfirm(post, position);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment onResume - 类型: " + (type == TYPE_DRAFT ? "草稿" : "已发布"));

        if (type == TYPE_DRAFT) {
            loadDraftPosts();
        } else if (type == TYPE_PUBLISHED) {
            refreshData();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "Fragment onPause");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "Fragment onDestroyView");
    }
}