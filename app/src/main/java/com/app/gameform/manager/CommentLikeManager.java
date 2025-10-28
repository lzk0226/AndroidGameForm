package com.app.gameform.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.app.gameform.domain.Comment;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.ApiService;

/**
 * 评论点赞管理器
 * 统一管理评论点赞状态检查和点赞操作
 * 类似于 PostLikeManager 的实现
 */
public class CommentLikeManager {

    private static final String TAG = "CommentLikeManager";

    /**
     * 点赞状态回调接口
     */
    public interface LikeStatusCallback {
        /**
         * 点赞状态更新
         * @param hasLiked 是否已点赞
         * @param newLikeCount 新的点赞数
         */
        void onUpdate(boolean hasLiked, int newLikeCount);

        /**
         * 操作失败
         * @param errorMessage 错误信息
         */
        void onFail(String errorMessage);
    }

    private final Context context;
    private final Handler mainHandler;

    public CommentLikeManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 处理评论点赞点击事件
     * 先检查当前点赞状态，然后根据状态执行点赞或取消点赞
     *
     * @param comment 评论对象
     * @param callback 回调接口
     */
    public void handleLikeClick(Comment comment, LikeStatusCallback callback) {
        if (comment == null || comment.getCommentId() == null) {
            mainHandler.post(() -> {
                callback.onFail("评论信息无效");
                Toast.makeText(context, "评论信息无效", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        // 先检查当前点赞状态
        ApiService.getInstance().checkCommentLikeStatus(
                context,
                comment.getCommentId(),
                new ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean hasLiked) {
                        if (hasLiked != null && hasLiked) {
                            // 已点赞，执行取消点赞
                            unlikeComment(comment, callback);
                        } else {
                            // 未点赞，执行点赞
                            likeComment(comment, callback);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        mainHandler.post(() -> {
                            Toast.makeText(context, "检查点赞状态失败: " + error, Toast.LENGTH_SHORT).show();
                            callback.onFail("检查点赞状态失败: " + error);
                        });
                    }
                });
    }

    /**
     * 点赞评论
     */
    private void likeComment(Comment comment, LikeStatusCallback callback) {
        ApiService.getInstance().likeComment(
                context,
                comment.getCommentId(),
                new ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        mainHandler.post(() -> {
                            if (result != null && result) {
                                int newCount = (comment.getLikeCount() != null ? comment.getLikeCount() : 0) + 1;
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
                            callback.onFail("点赞失败: " + error);
                        });
                    }
                });
    }

    /**
     * 取消点赞评论
     */
    private void unlikeComment(Comment comment, LikeStatusCallback callback) {
        ApiService.getInstance().unlikeComment(
                context,
                comment.getCommentId(),
                new ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        mainHandler.post(() -> {
                            if (result != null && result) {
                                int newCount = Math.max(0, (comment.getLikeCount() != null ? comment.getLikeCount() : 0) - 1);
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
                            callback.onFail("取消点赞失败: " + error);
                        });
                    }
                });
    }

    /**
     * 直接切换点赞状态（基于本地状态判断）
     * 这个方法保留用于兼容旧代码，但建议使用 handleLikeClick 方法
     *
     * @param comment 评论对象
     * @param callback 回调接口
     */
    @Deprecated
    public void toggleLikeByLocalStatus(Comment comment, LikeStatusCallback callback) {
        if (comment == null || comment.getCommentId() == null) {
            mainHandler.post(() -> {
                callback.onFail("评论信息无效");
                Toast.makeText(context, "评论信息无效", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        boolean currentLikeStatus = comment.getHasLiked() != null && comment.getHasLiked();

        if (currentLikeStatus) {
            // 已点赞，执行取消点赞
            unlikeComment(comment, callback);
        } else {
            // 未点赞，执行点赞
            likeComment(comment, callback);
        }
    }
}