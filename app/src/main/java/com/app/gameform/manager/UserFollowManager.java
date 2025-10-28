package com.app.gameform.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.ApiService;

/**
 * 用户关注管理器
 * 参考 PostLikeManager 实现
 */
public class UserFollowManager {

    private static final String TAG = "UserFollowManager";

    public interface FollowStatusCallback {
        void onUpdate(boolean hasFollowed, String message);
        void onFail(String errorMessage);
    }

    private final Context context;
    private final Handler mainHandler;

    public UserFollowManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 处理关注按钮点击事件
     * @param userId 要关注/取消关注的用户ID
     * @param callback 状态回调
     */
    public void handleFollowClick(Long userId, FollowStatusCallback callback) {
        if (userId == null) {
            callback.onFail("用户ID无效");
            return;
        }

        // 1. 先检查当前关注状态
        ApiService.getInstance().checkFollowStatus(
                context,
                userId,
                new ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean hasFollowed) {
                        if (hasFollowed != null && hasFollowed) {
                            // 已关注，执行取消关注操作
                            unfollowUser(userId, callback);
                        } else {
                            // 未关注，执行关注操作
                            followUser(userId, callback);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "检查关注状态失败: " + error);
                        mainHandler.post(() -> {
                            Toast.makeText(context, "检查关注状态失败: " + error, Toast.LENGTH_SHORT).show();
                        });
                        callback.onFail("检查关注状态失败: " + error);
                    }
                });
    }

    /**
     * 关注用户
     */
    private void followUser(Long userId, FollowStatusCallback callback) {
        ApiService.getInstance().followUser(
                context,
                userId,
                new ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        mainHandler.post(() -> {
                            if (result != null && result) {
                                Toast.makeText(context, "关注成功", Toast.LENGTH_SHORT).show();
                                callback.onUpdate(true, "关注成功");
                                Log.d(TAG, "关注成功 - userId: " + userId);
                            } else {
                                Toast.makeText(context, "关注失败", Toast.LENGTH_SHORT).show();
                                callback.onFail("关注失败");
                                Log.e(TAG, "关注失败 - userId: " + userId);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "关注失败: " + error);
                        mainHandler.post(() -> {
                            Toast.makeText(context, "关注失败: " + error, Toast.LENGTH_SHORT).show();
                        });
                        callback.onFail("关注失败: " + error);
                    }
                });
    }

    /**
     * 取消关注用户
     */
    private void unfollowUser(Long userId, FollowStatusCallback callback) {
        ApiService.getInstance().unfollowUser(
                context,
                userId,
                new ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        mainHandler.post(() -> {
                            if (result != null && result) {
                                Toast.makeText(context, "取消关注成功", Toast.LENGTH_SHORT).show();
                                callback.onUpdate(false, "取消关注成功");
                                Log.d(TAG, "取消关注成功 - userId: " + userId);
                            } else {
                                Toast.makeText(context, "取消关注失败", Toast.LENGTH_SHORT).show();
                                callback.onFail("取消关注失败");
                                Log.e(TAG, "取消关注失败 - userId: " + userId);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "取消关注失败: " + error);
                        mainHandler.post(() -> {
                            Toast.makeText(context, "取消关注失败: " + error, Toast.LENGTH_SHORT).show();
                        });
                        callback.onFail("取消关注失败: " + error);
                    }
                });
    }

    /**
     * 仅检查关注状态（不执行关注/取消关注操作）
     * @param userId 要检查的用户ID
     * @param callback 状态回调
     */
    public void checkFollowStatus(Long userId, ApiCallback<Boolean> callback) {
        if (userId == null) {
            callback.onError("用户ID无效");
            return;
        }

        ApiService.getInstance().checkFollowStatus(context, userId, new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean hasFollowed) {
                callback.onSuccess(hasFollowed);
                Log.d(TAG, "检查关注状态成功 - userId: " + userId + ", hasFollowed: " + hasFollowed);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "检查关注状态失败: " + error);
                callback.onError(error);
            }
        });
    }
}