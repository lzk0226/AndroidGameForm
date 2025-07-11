package com.app.gameform.Activity;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.domain.Post;
import com.app.gameform.network.ApiService;
import com.app.gameform.network.ApiCallback;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements PostAdapter.OnPostClickListener, PostAdapter.OnPostLikeListener {
    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private TextView tabHot, tabRecommend, tabFollow, tabNew;
    private String currentTab = "follow"; // 默认选中关注

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.home);
            initViews();
            setupRecyclerView();
            setupTabListeners();
            loadPostData(currentTab);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("HomeActivity", "Error in onCreate: " + e.getMessage());
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tabHot = findViewById(R.id.tab_hot);
        tabRecommend = findViewById(R.id.tab_recommend);
        tabFollow = findViewById(R.id.tab_follow);
        tabNew = findViewById(R.id.tab_new);

        postList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        postAdapter = new PostAdapter(this, postList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(postAdapter);

        // 设置监听器
        postAdapter.setOnPostClickListener(this);
        postAdapter.setOnPostLikeListener(this);
    }

    private void setupTabListeners() {
        tabHot.setOnClickListener(v -> {
            switchTab("hot");
            loadPostData("hot");
        });

        tabRecommend.setOnClickListener(v -> {
            switchTab("recommend");
            loadPostData("recommend");
        });

        tabFollow.setOnClickListener(v -> {
            switchTab("follow");
            loadPostData("follow");
        });

        tabNew.setOnClickListener(v -> {
            switchTab("new");
            loadPostData("new");
        });
    }

    private void switchTab(String tab) {
        // 重置所有标签样式
        resetTabStyles();

        currentTab = tab;

        // 设置选中标签样式
        switch (tab) {
            case "hot":
                setTabSelected(tabHot);
                break;
            case "recommend":
                setTabSelected(tabRecommend);
                break;
            case "follow":
                setTabSelected(tabFollow);
                break;
            case "new":
                setTabSelected(tabNew);
                break;
        }
    }

    private void resetTabStyles() {
        tabHot.setTextColor(Color.parseColor("#999999"));
        tabHot.setTypeface(null, Typeface.NORMAL);

        tabRecommend.setTextColor(Color.parseColor("#999999"));
        tabRecommend.setTypeface(null, Typeface.NORMAL);

        tabFollow.setTextColor(Color.parseColor("#999999"));
        tabFollow.setTypeface(null, Typeface.NORMAL);

        tabNew.setTextColor(Color.parseColor("#999999"));
        tabNew.setTypeface(null, Typeface.NORMAL);
    }

    private void setTabSelected(TextView tab) {
        tab.setTextColor(Color.parseColor("#333333"));
        tab.setTypeface(null, Typeface.BOLD);
    }

    private void loadPostData(String type) {
        // 显示加载状态
        showLoading();

        String url = getApiUrl(type);

        // 使用你的网络请求库（如Retrofit、OkHttp等）
        ApiService.getInstance().getPosts(url, new ApiCallback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                runOnUiThread(() -> {
                    hideLoading();
                    postList.clear();
                    postList.addAll(posts);
                    postAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(HomeActivity.this, "加载失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String getApiUrl(String type) {
        String baseUrl = "http://10.0.2.2:8080/user/post/";

        switch (type) {
            case "hot":
                return baseUrl + "hot?limit=20";
            case "recommend":
                return baseUrl + "list";
            case "follow":
                // 这里可能需要特殊处理，获取关注用户的帖子
                return baseUrl + "list"; // 临时使用通用列表
            case "new":
                return baseUrl + "list"; // 可以添加排序参数
            default:
                return baseUrl + "list";
        }
    }

    private void showLoading() {
        // 显示加载动画或进度条
        // 这里可以显示一个加载对话框或者进度条
    }

    private void hideLoading() {
        // 隐藏加载动画或进度条
        // 这里隐藏加载对话框或者进度条
    }

    // 实现PostAdapter.OnPostClickListener接口
    @Override
    public void onPostClick(Post post, int position) {
        // 处理帖子点击事件 - 跳转到帖子详情页
        // 点击帖子时增加浏览量
        //incrementViewCount(post, position);
        Toast.makeText(this, "点击了帖子: " + post.getPostTitle(), Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(this, PostDetailActivity.class);
        // intent.putExtra("postId", post.getPostId());
        // startActivity(intent);
    }

    @Override
    public void onUserClick(Post post, int position) {
        // 处理用户点击事件 - 跳转到用户资料页
        Toast.makeText(this, "点击了用户: " + post.getNickName(), Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(this, UserProfileActivity.class);
        // intent.putExtra("userId", post.getUserId());
        // startActivity(intent);
    }

    @Override
    public void onCommentClick(Post post, int position) {
        // 处理评论点击事件 - 跳转到评论页面
        Toast.makeText(this, "点击了评论", Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(this, CommentActivity.class);
        // intent.putExtra("postId", post.getPostId());
        // startActivity(intent);
    }

    @Override
    public void onViewClick(Post post, int position) {
        // 修改：处理浏览按钮点击事件（原来的分享按钮）
        // 可以显示浏览详情或者其他操作
        Toast.makeText(this, "浏览量: " + post.getViewCount(), Toast.LENGTH_SHORT).show();
        // 这里可以添加其他浏览相关的功能
    }

    @Override
    public void onMoreClick(Post post, int position) {
        // 处理更多操作点击事件
        Toast.makeText(this, "点击了更多", Toast.LENGTH_SHORT).show();
        // 显示更多操作菜单
    }

    // 实现PostAdapter.OnPostLikeListener接口
    @Override
    public void onLikeClick(Post post, int position) {
        // 处理点赞点击事件
        Toast.makeText(this, "点赞了帖子", Toast.LENGTH_SHORT).show();

        // 这里应该调用API进行点赞操作
        // 示例：
        // ApiService.getInstance().likePost(post.getPostId(), new ApiCallback<Boolean>() {
        //     @Override
        //     public void onSuccess(Boolean result) {
        //         runOnUiThread(() -> {
        //             if (result) {
        //                 // 更新点赞状态
        //                 int newLikeCount = post.getLikeCount() + 1;
        //                 postAdapter.updateLikeStatus(position, true, newLikeCount);
        //                 Toast.makeText(HomeActivity.this, "点赞成功", Toast.LENGTH_SHORT).show();
        //             } else {
        //                 Toast.makeText(HomeActivity.this, "点赞失败", Toast.LENGTH_SHORT).show();
        //             }
        //         });
        //     }
        //
        //     @Override
        //     public void onError(String error) {
        //         runOnUiThread(() -> {
        //             Toast.makeText(HomeActivity.this, "点赞失败: " + error, Toast.LENGTH_SHORT).show();
        //         });
        //     }
        // });
    }

    /**
     * 新增：增加浏览量的方法
     * @param post 帖子对象
     * @param position 位置
     */
    /*private void incrementViewCount(Post post, int position) {
        // 调用API增加浏览量
        ApiService.getInstance().incrementViewCount(post.getPostId(), new ApiCallback<Integer>() {
            @Override
            public void onSuccess(Integer newViewCount) {
                runOnUiThread(() -> {
                    // 更新本地数据和UI
                    postAdapter.updateViewCount(position, newViewCount);
                });
            }

            @Override
            public void onError(String error) {
                // 浏览量增加失败，可以选择静默处理或者记录日志
                Log.e("HomeActivity", "增加浏览量失败: " + error);
            }
        });
    }*/
}