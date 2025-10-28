package com.app.gameform.manager;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.app.gameform.domain.PostFavorite;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 帖子收藏管理器
 * 负责处理帖子收藏相关的操作
 */
public class PostFavoriteManager {

    private static final String TAG = "PostFavoriteManager";

    private Context context;
    private OkHttpClient client;
    private Gson gson;
    private SharedPrefManager sharedPrefManager;

    public PostFavoriteManager(Context context) {
        this.context = context;
        this.sharedPrefManager = SharedPrefManager.getInstance(context);

        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }

    /**
     * 添加收藏
     */
    public void addFavorite(int postId, ApiCallback<String> callback) {
        String token = sharedPrefManager.getToken();
        if (TextUtils.isEmpty(token)) {
            callback.onError("请先登录");
            return;
        }

        String url = ApiConstants.ADD_POST_FAVORITE + postId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .post(RequestBody.create(new byte[0]))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "addFavorite failed: " + e.getMessage());
                callback.onError("收藏失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "addFavorite response: " + responseBody);

                    if (response.isSuccessful()) {
                        ApiResponse<String> apiResponse = gson.fromJson(
                                responseBody,
                                new TypeToken<ApiResponse<String>>(){}.getType()
                        );

                        if (apiResponse != null && apiResponse.isSuccess()) {
                            callback.onSuccess(apiResponse.getData() != null ?
                                    apiResponse.getData() : "收藏成功");
                        } else {
                            callback.onError(apiResponse != null ?
                                    apiResponse.getMsg() : "收藏失败");
                        }
                    } else {
                        callback.onError("收藏失败: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "addFavorite parse error: ", e);
                    callback.onError("解析响应失败");
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * 取消收藏
     */
    public void removeFavorite(int postId, ApiCallback<String> callback) {
        String token = sharedPrefManager.getToken();
        if (TextUtils.isEmpty(token)) {
            callback.onError("请先登录");
            return;
        }

        String url = ApiConstants.REMOVE_POST_FAVORITE + postId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "removeFavorite failed: " + e.getMessage());
                callback.onError("取消收藏失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "removeFavorite response: " + responseBody);

                    if (response.isSuccessful()) {
                        ApiResponse<String> apiResponse = gson.fromJson(
                                responseBody,
                                new TypeToken<ApiResponse<String>>(){}.getType()
                        );

                        if (apiResponse != null && apiResponse.isSuccess()) {
                            callback.onSuccess(apiResponse.getData() != null ?
                                    apiResponse.getData() : "取消收藏成功");
                        } else {
                            callback.onError(apiResponse != null ?
                                    apiResponse.getMsg() : "取消收藏失败");
                        }
                    } else {
                        callback.onError("取消收藏失败: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "removeFavorite parse error: ", e);
                    callback.onError("解析响应失败");
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * 检查收藏状态
     */
    public void checkFavoriteStatus(int postId, ApiCallback<Boolean> callback) {
        String token = sharedPrefManager.getToken();
        if (TextUtils.isEmpty(token)) {
            callback.onError("请先登录");
            return;
        }

        String url = ApiConstants.CHECK_POST_FAVORITE_STATUS + postId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "checkFavoriteStatus failed: " + e.getMessage());
                callback.onError("检查收藏状态失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "checkFavoriteStatus response: " + responseBody);

                    if (response.isSuccessful()) {
                        ApiResponse<Boolean> apiResponse = gson.fromJson(
                                responseBody,
                                new TypeToken<ApiResponse<Boolean>>(){}.getType()
                        );

                        if (apiResponse != null && apiResponse.isSuccess()) {
                            callback.onSuccess(apiResponse.getData() != null ?
                                    apiResponse.getData() : false);
                        } else {
                            callback.onError(apiResponse != null ?
                                    apiResponse.getMsg() : "检查收藏状态失败");
                        }
                    } else {
                        callback.onError("检查收藏状态失败: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "checkFavoriteStatus parse error: ", e);
                    callback.onError("解析响应失败");
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * 获取收藏数量
     */
    public void getFavoriteCount(int postId, ApiCallback<Integer> callback) {
        String url = ApiConstants.GET_POST_FAVORITE_COUNT + postId;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "getFavoriteCount failed: " + e.getMessage());
                callback.onError("获取收藏数量失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "getFavoriteCount response: " + responseBody);

                    if (response.isSuccessful()) {
                        ApiResponse<Integer> apiResponse = gson.fromJson(
                                responseBody,
                                new TypeToken<ApiResponse<Integer>>(){}.getType()
                        );

                        if (apiResponse != null && apiResponse.isSuccess()) {
                            callback.onSuccess(apiResponse.getData() != null ?
                                    apiResponse.getData() : 0);
                        } else {
                            callback.onError(apiResponse != null ?
                                    apiResponse.getMsg() : "获取收藏数量失败");
                        }
                    } else {
                        callback.onError("获取收藏数量失败: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "getFavoriteCount parse error: ", e);
                    callback.onError("解析响应失败");
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * 获取我的收藏列表
     */
    public void getMyFavorites(ApiCallback<List<PostFavorite>> callback) {
        String token = sharedPrefManager.getToken();
        if (TextUtils.isEmpty(token)) {
            callback.onError("请先登录");
            return;
        }

        String url = ApiConstants.GET_MY_FAVORITES;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "getMyFavorites failed: " + e.getMessage());
                callback.onError("获取收藏列表失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "getMyFavorites response: " + responseBody);

                    if (response.isSuccessful()) {
                        Type type = new TypeToken<ApiResponse<List<PostFavorite>>>(){}.getType();
                        ApiResponse<List<PostFavorite>> apiResponse = gson.fromJson(responseBody, type);

                        if (apiResponse != null && apiResponse.isSuccess()) {
                            callback.onSuccess(apiResponse.getData());
                        } else {
                            callback.onError(apiResponse != null ?
                                    apiResponse.getMsg() : "获取收藏列表失败");
                        }
                    } else {
                        callback.onError("获取收藏列表失败: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "getMyFavorites parse error: ", e);
                    callback.onError("解析响应失败");
                } finally {
                    response.close();
                }
            }
        });
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