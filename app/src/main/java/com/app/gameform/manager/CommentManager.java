package com.app.gameform.manager;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.app.gameform.domain.Comment;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.ApiConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 评论管理器
 * 负责处理评论相关的所有操作，包括回复功能
 * 回复时保留 @用户名 在评论内容中
 */
public class CommentManager {

    private static final String TAG = "CommentManager";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private Context context;
    private OkHttpClient client;
    private Gson gson;
    private SharedPrefManager sharedPrefManager;

    // 回复状态管理
    private Integer replyToCommentId = null;  // 当前回复的评论ID
    private String replyToUsername = null;    // 当前回复的用户名
    private String replyPrefix = null;        // 回复前缀，如 "@张三 "

    public CommentManager(Context context) {
        this.context = context;
        this.sharedPrefManager = SharedPrefManager.getInstance(context);

        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();

        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }

    /**
     * 获取帖子的评论列表
     */
    public void getPostComments(int postId, ApiCallback<List<Comment>> callback) {
        String url = ApiConstants.GET_POST_COMMENTS + postId;
        Request request = createGetRequest(url, false);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "getPostComments failed: " + e.getMessage());
                callback.onError("获取评论失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "getPostComments response: " + responseBody);

                    if (response.isSuccessful()) {
                        Type type = new TypeToken<ApiResponse<List<Comment>>>(){}.getType();
                        ApiResponse<List<Comment>> apiResponse = gson.fromJson(responseBody, type);

                        if (apiResponse != null && apiResponse.isSuccess()) {
                            callback.onSuccess(apiResponse.getData());
                        } else {
                            callback.onError(apiResponse != null ?
                                    apiResponse.getMsg() : "获取评论失败");
                        }
                    } else {
                        callback.onError("获取评论失败: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "getPostComments parse error: ", e);
                    callback.onError("解析评论数据失败");
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * 创建评论（支持回复功能）
     * 重要：回复时保留 @用户名 在评论内容中，不做任何移除
     */
    public void createComment(Comment comment, ApiCallback<Comment> callback) {
        String token = sharedPrefManager.getToken();
        if (TextUtils.isEmpty(token)) {
            callback.onError("请先登录");
            return;
        }

        // 验证评论内容
        if (comment == null || TextUtils.isEmpty(comment.getCommentContent())) {
            callback.onError("评论内容不能为空");
            return;
        }

        if (comment.getPostId() == null || comment.getPostId() == 0) {
            callback.onError("帖子ID无效");
            return;
        }

        String url = ApiConstants.CREATE_COMMENT;
        String jsonBody = gson.toJson(comment);
        Log.d(TAG, "Creating comment - URL: " + url);
        Log.d(TAG, "Creating comment - Body: " + jsonBody);

        RequestBody body = RequestBody.create(JSON, jsonBody);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "createComment failed: " + e.getMessage(), e);
                callback.onError("发送评论失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "createComment response code: " + response.code());
                    Log.d(TAG, "createComment response: " + responseBody);

                    if (response.isSuccessful()) {
                        Type type = new TypeToken<ApiResponse<Comment>>(){}.getType();
                        ApiResponse<Comment> apiResponse = gson.fromJson(responseBody, type);

                        if (apiResponse != null && apiResponse.isSuccess()) {
                            callback.onSuccess(apiResponse.getData());
                        } else {
                            String errorMsg = apiResponse != null ? apiResponse.getMsg() : "发送评论失败";
                            Log.e(TAG, "createComment error: " + errorMsg);
                            callback.onError(errorMsg);
                        }
                    } else {
                        String errorMsg = "发送评论失败: HTTP " + response.code();
                        Log.e(TAG, errorMsg + ", Body: " + responseBody);
                        callback.onError(errorMsg);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "createComment parse error: ", e);
                    callback.onError("解析响应失败: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * 删除评论
     */
    public void deleteComment(int commentId, ApiCallback<String> callback) {
        String token = sharedPrefManager.getToken();
        if (TextUtils.isEmpty(token)) {
            callback.onError("请先登录");
            return;
        }

        String url = ApiConstants.DELETE_COMMENT + commentId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "deleteComment failed: " + e.getMessage());
                callback.onError("删除评论失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "deleteComment response: " + responseBody);

                    if (response.isSuccessful()) {
                        ApiResponse<String> apiResponse = gson.fromJson(
                                responseBody,
                                new TypeToken<ApiResponse<String>>(){}.getType()
                        );

                        if (apiResponse != null && apiResponse.isSuccess()) {
                            callback.onSuccess("删除成功");
                        } else {
                            callback.onError(apiResponse != null ?
                                    apiResponse.getMsg() : "删除评论失败");
                        }
                    } else {
                        callback.onError("删除评论失败: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "deleteComment parse error: ", e);
                    callback.onError("解析响应失败");
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * 点赞评论
     */
    public void likeComment(int commentId, ApiCallback<String> callback) {
        String token = sharedPrefManager.getToken();
        if (TextUtils.isEmpty(token)) {
            callback.onError("请先登录");
            return;
        }

        String url = ApiConstants.TOGGLE_COMMENT_LIKE + commentId;
        Log.d(TAG, "Liking comment - URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .post(RequestBody.create(new byte[0]))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "likeComment failed: " + e.getMessage());
                callback.onError("点赞失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "likeComment response: " + responseBody);

                    if (response.isSuccessful()) {
                        callback.onSuccess("点赞成功");
                    } else {
                        callback.onError("点赞失败: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "likeComment parse error: ", e);
                    callback.onError("解析响应失败");
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * 取消点赞评论
     */
    public void unlikeComment(int commentId, ApiCallback<String> callback) {
        String token = sharedPrefManager.getToken();
        if (TextUtils.isEmpty(token)) {
            callback.onError("请先登录");
            return;
        }

        String url = ApiConstants.TOGGLE_COMMENT_LIKE + commentId;
        Log.d(TAG, "Unliking comment - URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "unlikeComment failed: " + e.getMessage());
                callback.onError("取消点赞失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "unlikeComment response: " + responseBody);

                    if (response.isSuccessful()) {
                        callback.onSuccess("取消点赞成功");
                    } else {
                        callback.onError("取消点赞失败: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "unlikeComment parse error: ", e);
                    callback.onError("解析响应失败");
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * 切换评论点赞状态（智能判断点赞或取消）
     */
    public void toggleCommentLike(Comment comment, CommentLikeCallback callback) {
        if (comment == null) {
            callback.onFail("评论数据无效");
            return;
        }

        boolean currentLikeStatus = comment.getHasLiked() != null && comment.getHasLiked();
        int currentLikeCount = comment.getLikeCount() != null ? comment.getLikeCount() : 0;

        if (currentLikeStatus) {
            // 已点赞，执行取消点赞
            unlikeComment(comment.getCommentId(), new ApiCallback<String>() {
                @Override
                public void onSuccess(String message) {
                    int newCount = Math.max(0, currentLikeCount - 1);
                    callback.onUpdate(false, newCount);
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onFail(errorMessage);
                }
            });
        } else {
            // 未点赞，执行点赞
            likeComment(comment.getCommentId(), new ApiCallback<String>() {
                @Override
                public void onSuccess(String message) {
                    int newCount = currentLikeCount + 1;
                    callback.onUpdate(true, newCount);
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onFail(errorMessage);
                }
            });
        }
    }

    // ==================== 回复功能相关方法 ====================

    /**
     * 设置回复状态
     * @param commentId 被回复的评论ID
     * @param username 被回复的用户名
     */
    public void setReplyTo(Integer commentId, String username) {
        this.replyToCommentId = commentId;
        this.replyToUsername = username;
        this.replyPrefix = "@" + username + " ";
        Log.d(TAG, "设置回复状态 - CommentId: " + commentId + ", Username: " + username);
    }

    /**
     * 清除回复状态
     */
    public void clearReplyTo() {
        this.replyToCommentId = null;
        this.replyToUsername = null;
        this.replyPrefix = null;
        Log.d(TAG, "清除回复状态");
    }

    /**
     * 获取当前回复的评论ID
     */
    public Integer getReplyToCommentId() {
        return replyToCommentId;
    }

    /**
     * 获取当前回复的用户名
     */
    public String getReplyToUsername() {
        return replyToUsername;
    }

    /**
     * 获取回复前缀（如 "@张三 "）
     */
    public String getReplyPrefix() {
        return replyPrefix;
    }

    /**
     * 检查是否处于回复状态
     */
    public boolean isReplying() {
        return replyToCommentId != null && replyToUsername != null;
    }

    /**
     * 检查输入内容是否仍包含回复前缀
     * 用于监听用户是否删除了@用户名
     */
    public boolean containsReplyPrefix(String text) {
        if (!isReplying() || TextUtils.isEmpty(replyPrefix)) {
            return false;
        }
        return text != null && text.startsWith(replyPrefix);
    }

    /**
     * 创建GET请求
     */
    private Request createGetRequest(String url, boolean needAuth) {
        Request.Builder builder = new Request.Builder().url(url);

        if (needAuth) {
            String token = sharedPrefManager.getToken();
            if (!TextUtils.isEmpty(token)) {
                builder.addHeader("Authorization",
                        token.startsWith("Bearer ") ? token : "Bearer " + token);
            }
        }

        return builder.build();
    }

    /**
     * 评论点赞回调接口
     */
    public interface CommentLikeCallback {
        void onUpdate(boolean liked, int newLikeCount);
        void onFail(String errorMessage);
    }

    /**
     * API响应包装类
     */
    public static class ApiResponse<T> {
        private int code;
        private String msg;
        private T data;

        public boolean isSuccess() {
            return code == 200;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}