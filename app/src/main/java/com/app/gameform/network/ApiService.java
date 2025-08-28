package com.app.gameform.network;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.app.gameform.domain.Post;
import com.app.gameform.utils.SharedPrefManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * API服务类
 */
public class ApiService {
    private static ApiService instance;
    private OkHttpClient client;
    private Gson gson;

    // 在 ApiService.java 的构造函数中，修改日期格式数组
    private ApiService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        // 配置Gson以支持多种日期格式
        gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                    private final SimpleDateFormat[] dateFormats = {
                            // 添加服务器实际使用的格式（放在前面，优先匹配）
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()),
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault()),
                            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    };

                    @Override
                    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                            throws JsonParseException {
                        if (json == null || json.isJsonNull()) {
                            return null;
                        }

                        String dateStr = json.getAsString();
                        if (dateStr == null || dateStr.trim().isEmpty()) {
                            return null;
                        }

                        // 清理日期字符串
                        dateStr = dateStr.trim();

                        // 处理时区格式，将 +08:00 转换为 +0800
                        if (dateStr.matches(".*[+-]\\d{2}:\\d{2}$")) {
                            dateStr = dateStr.replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2");
                        }

                        // 尝试不同的日期格式
                        for (SimpleDateFormat format : dateFormats) {
                            try {
                                return format.parse(dateStr);
                            } catch (ParseException e) {
                                // 继续尝试下一个格式
                            }
                        }

                        // 如果所有格式都失败，记录错误并返回 null
                        Log.w("ApiService", "无法解析日期格式: " + dateStr);
                        return null;
                    }
                })
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
     * 通用GET请求方法
     */
    public void getRequest(String url, ApiCallback<String> callback) {
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
                        callback.onSuccess(json);
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
     * 需要认证的GET请求方法
     */
    public void getRequestWithAuth(Context context, String url, ApiCallback<String> callback) {
        String token = SharedPrefManager.getInstance(context).getToken();
        if (TextUtils.isEmpty(token)) {
            callback.onError("token为空，无法执行请求，请重新登录");
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
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
                        callback.onSuccess(json);
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
     * 需要认证的DELETE请求方法
     */
    public void deleteRequestWithAuth(Context context, String url, ApiCallback<String> callback) {
        String token = SharedPrefManager.getInstance(context).getToken();
        if (TextUtils.isEmpty(token)) {
            callback.onError("token为空，无法执行请求，请重新登录");
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .delete()
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
                        callback.onSuccess(json);
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
     * 需要认证的POST请求方法
     */
    public void postRequestWithAuth(Context context, String url, String jsonBody, ApiCallback<String> callback) {
        String token = SharedPrefManager.getInstance(context).getToken();
        if (TextUtils.isEmpty(token)) {
            callback.onError("token为空，无法执行请求，请重新登录");
            return;
        }

        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .post(body)
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
                        callback.onSuccess(json);
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
    public void likePost(Context context, Integer postId, ApiCallback<Boolean> callback) {
        String url = ApiConstants.POST_LIKE_URL + postId;

        // 从 SharedPreferences 获取 token
        String token = SharedPrefManager.getInstance(context).getToken();
        Log.d("Token调试", "发送评论用的 token = " + token);

        if (TextUtils.isEmpty(token)) {
            callback.onError("token为空，无法点赞，请重新登录");
            return;
        }

        // 空 JSON 请求体，保持与网页端一致
        RequestBody body = RequestBody.create("{}", MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .post(body)
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
                        Type type = new TypeToken<ApiResponse<String>>() {}.getType();
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
     * 取消点赞帖子
     */
    public void unlikePost(Context context, Integer postId, ApiCallback<Boolean> callback) {
        String url = ApiConstants.POST_LIKE_URL + postId; // 与点赞接口相同的url

        String token = SharedPrefManager.getInstance(context).getToken();
        if (TextUtils.isEmpty(token)) {
            callback.onError("token为空，无法取消点赞，请重新登录");
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .delete() // 取消点赞是 DELETE 请求
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
                        Type type = new TypeToken<ApiResponse<String>>() {}.getType();
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
     * 检查帖子是否已点赞
     */
    public void checkPostLikeStatus(Context context, Integer postId, ApiCallback<Boolean> callback) {
        String url = ApiConstants.POST_LIKE_STATUS_URL + postId; // 你需要定义这个常量，对应接口路径

        String token = SharedPrefManager.getInstance(context).getToken();
        if (TextUtils.isEmpty(token)) {
            callback.onError("token为空，无法检查点赞状态，请重新登录");
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .get()  // 查询点赞状态是 GET 请求
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
                        Type type = new TypeToken<ApiResponse<Boolean>>() {}.getType();
                        ApiResponse<Boolean> apiResponse = gson.fromJson(json, type);
                        // 返回 true 表示已点赞，false 表示未点赞
                        callback.onSuccess(apiResponse.isSuccess() && apiResponse.getData() != null && apiResponse.getData());
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

    public Gson getGson() {
        return gson;
    }

    // Add these methods to your existing ApiService.java class

    /**
     * 带分页的帖子列表请求
     */
    public void getPostsWithPagination(String baseUrl, int page, int size, ApiCallback<List<Post>> callback) {
        String url = baseUrl + "?page=" + page + "&size=" + size;
        getPosts(url, callback);
    }

    /**
     * 带认证的分页帖子请求
     */
    public void getPostsWithAuthAndPagination(Context context, String baseUrl, int page, int size, ApiCallback<List<Post>> callback) {
        String url = baseUrl + "?page=" + page + "&size=" + size;
        String token = SharedPrefManager.getInstance(context).getToken();

        if (TextUtils.isEmpty(token)) {
            callback.onError("token为空，无法执行请求，请重新登录");
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
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
     * 获取热门帖子（带分页）
     */
    public void getHotPostsWithPagination(int page, int size, ApiCallback<List<Post>> callback) {
        String url = ApiConstants.GET_HOT_POSTS + "?page=" + page + "&size=" + size;
        getPosts(url, callback);
    }

    /**
     * 获取我的帖子（带分页）
     */
    public void getMyPostsWithPagination(Context context, int page, int size, ApiCallback<List<Post>> callback) {
        getPostsWithAuthAndPagination(context, ApiConstants.GET_MY_POSTS, page, size, callback);
    }

    /**
     * 根据版块获取帖子（带分页）
     */
    public void getPostsBySectionWithPagination(int sectionId, int page, int size, ApiCallback<List<Post>> callback) {
        String url = ApiConstants.GET_POSTS_BY_SECTION + sectionId + "?page=" + page + "&size=" + size;
        getPosts(url, callback);
    }

    /**
     * 根据用户获取帖子（带分页）
     */
    public void getPostsByUserWithPagination(int userId, int page, int size, ApiCallback<List<Post>> callback) {
        String url = ApiConstants.GET_POSTS_BY_USER + userId + "?page=" + page + "&size=" + size;
        getPosts(url, callback);
    }

    /**
     * 搜索帖子（带分页）
     */
    public void searchPostsWithPagination(String title, int page, int size, ApiCallback<List<Post>> callback) {
        String url = ApiConstants.SEARCH_POSTS + "?title=" + title + "&page=" + page + "&size=" + size;
        getPosts(url, callback);
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
    // 在 ApiService.java 中添加以下方法

    /**
     * 获取个性化推荐帖子
     */
    public void getPersonalizedRecommendations(Context context, int limit, ApiCallback<List<Post>> callback) {
        String url = ApiConstants.buildPersonalizedRecommendationsUrl(limit);

        getRequestWithAuth(context, url, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    ApiResponse<List<Post>> apiResponse = gson.fromJson(
                            response,
                            new TypeToken<ApiResponse<List<Post>>>(){}.getType()
                    );

                    if (apiResponse != null && apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getData());
                    } else {
                        String errorMsg = apiResponse != null ? apiResponse.getMsg() : "推荐服务异常";
                        callback.onError(errorMsg);
                    }
                } catch (Exception e) {
                    Log.e("ApiService", "解析个性化推荐数据失败: " + e.getMessage());
                    callback.onError("数据解析失败");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * 获取基于内容的推荐帖子
     */
    public void getContentBasedRecommendations(Context context, int limit, ApiCallback<List<Post>> callback) {
        String url = ApiConstants.buildContentBasedRecommendationsUrl(limit);

        getRequestWithAuth(context, url, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    ApiResponse<List<Post>> apiResponse = gson.fromJson(
                            response,
                            new TypeToken<ApiResponse<List<Post>>>(){}.getType()
                    );

                    if (apiResponse != null && apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getData());
                    } else {
                        String errorMsg = apiResponse != null ? apiResponse.getMsg() : "内容推荐服务异常";
                        callback.onError(errorMsg);
                    }
                } catch (Exception e) {
                    Log.e("ApiService", "解析内容推荐数据失败: " + e.getMessage());
                    callback.onError("数据解析失败");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * 获取混合推荐帖子
     */
    public void getHybridRecommendations(Context context, int limit, int page, ApiCallback<List<Post>> callback) {
        String url = ApiConstants.buildHybridRecommendationsUrl(limit, page);

        getRequestWithAuth(context, url, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    ApiResponse<List<Post>> apiResponse = gson.fromJson(
                            response,
                            new TypeToken<ApiResponse<List<Post>>>(){}.getType()
                    );

                    if (apiResponse != null && apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getData());
                    } else {
                        String errorMsg = apiResponse != null ? apiResponse.getMsg() : "混合推荐服务异常";
                        callback.onError(errorMsg);
                    }
                } catch (Exception e) {
                    Log.e("ApiService", "解析混合推荐数据失败: " + e.getMessage());
                    callback.onError("数据解析失败");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * 获取详细推荐信息
     */
    public void getDetailedRecommendations(Context context, int limit, ApiCallback<Map<String, Object>> callback) {
        String url = ApiConstants.buildDetailedRecommendationsUrl(limit);

        getRequestWithAuth(context, url, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    ApiResponse<Map<String, Object>> apiResponse = gson.fromJson(
                            response,
                            new TypeToken<ApiResponse<Map<String, Object>>>(){}.getType()
                    );

                    if (apiResponse != null && apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getData());
                    } else {
                        String errorMsg = apiResponse != null ? apiResponse.getMsg() : "详细推荐服务异常";
                        callback.onError(errorMsg);
                    }
                } catch (Exception e) {
                    Log.e("ApiService", "解析详细推荐数据失败: " + e.getMessage());
                    callback.onError("数据解析失败");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}