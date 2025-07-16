package com.app.gameform.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.app.gameform.domain.Post;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.ApiService;

public class PostLikeManager {

    public interface LikeStatusCallback {
        void onUpdate(boolean hasLiked, int newLikeCount);
        void onFail(String errorMessage);
    }

    private final Context context;
    private final Handler mainHandler;

    public PostLikeManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void handleLikeClick(Post post, int position, LikeStatusCallback callback) {
        ApiService.getInstance().checkPostLikeStatus(
                context,
                post.getPostId(),
                new ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean hasLiked) {
                        if (hasLiked != null && hasLiked) {
                            ApiService.getInstance().unlikePost(
                                    context,
                                    post.getPostId(),
                                    new ApiCallback<Boolean>() {
                                        @Override
                                        public void onSuccess(Boolean result) {
                                            mainHandler.post(() -> {
                                                if (result) {
                                                    int newCount = Math.max(0, post.getLikeCount() - 1);
                                                    callback.onUpdate(false, newCount);
                                                    Toast.makeText(context, "取消点赞成功", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    callback.onFail("取消点赞失败");
                                                    Toast.makeText(context, "取消点赞失败", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(String error) {
                                            mainHandler.post(() -> {
                                                Toast.makeText(context, "取消点赞失败: " + error, Toast.LENGTH_SHORT).show();
                                            });
                                            callback.onFail("取消点赞失败: " + error);
                                        }
                                    });
                        } else {
                            ApiService.getInstance().likePost(
                                    context,
                                    post.getPostId(),
                                    new ApiCallback<Boolean>() {
                                        @Override
                                        public void onSuccess(Boolean result) {
                                            mainHandler.post(() -> {
                                                if (result) {
                                                    int newCount = post.getLikeCount() + 1;
                                                    callback.onUpdate(true, newCount);
                                                    Toast.makeText(context, "点赞成功", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    callback.onFail("点赞失败");
                                                    Toast.makeText(context, "点赞失败", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(String error) {
                                            mainHandler.post(() -> {
                                                Toast.makeText(context, "点赞失败: " + error, Toast.LENGTH_SHORT).show();
                                            });
                                            callback.onFail("点赞失败: " + error);
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        mainHandler.post(() -> {
                            Toast.makeText(context, "检查点赞状态失败: " + error, Toast.LENGTH_SHORT).show();
                        });
                        callback.onFail("检查点赞状态失败: " + error);
                    }
                });
    }
}

