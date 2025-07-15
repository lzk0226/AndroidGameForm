package com.app.gameform.network;

import android.text.TextUtils;

import com.app.gameform.domain.Post;
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
import okhttp3.Response;

/**
 * API服务类
 */
public class ApiService {
    private static ApiService instance;
    private OkHttpClient client;
    private Gson gson;

    private ApiService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        // 配置Gson以支持多种日期格式
        gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }

    public static ApiService getInstance() {
        if (instance == null) {
            synchronized (ApiService.class) {
                if (instance == null) {
                    instance = new ApiService();
                }
            }
        }
        return instance;
    }

    /**
     * 获取帖子列表
     */
    public void getPosts(String url, ApiCallback<List<Post>> callback) {
        // 这里的url应该是完整的相对路径，不需要再拼接BASE_URL
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String json = response.body().string();

                        // 使用正确的类型来解析帖子列表
                        Type listType = new TypeToken<ApiResponse<List<Post>>>(){}.getType();
                        ApiResponse<List<Post>> apiResponse = gson.fromJson(json, listType);

                        if (apiResponse.isSuccess()) {
                            callback.onSuccess(apiResponse.getData());
                        } else {
                            callback.onError(apiResponse.getMsg());
                        }
                    } else {
                        callback.onError("请求失败: " + response.code());
                    }
                } catch (Exception e) {
                    callback.onError("解析响应失败: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * 点赞帖子
     */
    public void likePost(Integer postId, String token, ApiCallback<Boolean> callback) {
        String url = ApiConstants.POST_LIKE_URL + postId;

        // 避免 token 为 null 导致崩溃
        if (TextUtils.isEmpty(token)) {
            callback.onError("token为空，无法点赞，请重新登录");
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .post(okhttp3.RequestBody.create(new byte[0]))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String json = response.body().string();
                        Type type = new TypeToken<ApiResponse<String>>(){}.getType();
                        ApiResponse<String> apiResponse = gson.fromJson(json, type);
                        callback.onSuccess(apiResponse.isSuccess());
                    } else {
                        callback.onError("请求失败: " + response.code());
                    }
                } catch (Exception e) {
                    callback.onError("解析响应失败: " + e.getMessage());
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