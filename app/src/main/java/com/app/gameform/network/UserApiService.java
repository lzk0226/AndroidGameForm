package com.app.gameform.network;

import com.app.gameform.Run.ApiResponse;
import com.app.gameform.domain.User;
import com.app.gameform.emuns.ResultCodeEnum;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 用户相关API服务类
 * 对应后端UserController的接口
 */
public class UserApiService {

    private static UserApiService instance;
    private OkHttpClient client;
    private Gson gson;

    // 基础URL，根据实际情况修改
    private static final String BASE_URL = "http://10.0.2.2:8080/user/profile";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private UserApiService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }

    public static UserApiService getInstance() {
        if (instance == null) {
            synchronized (UserApiService.class) {
                if (instance == null) {
                    instance = new UserApiService();
                }
            }
        }
        return instance;
    }

    /**
     * 用户注册
     */
    public void register(User user, ApiCallback<String> callback) {
        String url = BASE_URL + "/register";
        String json = gson.toJson(user);

        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Type type = new TypeToken<ApiResponse<String>>(){}.getType();
                    ApiResponse<String> apiResponse = gson.fromJson(responseBody, type);

                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getData() != null ? apiResponse.getData() : "注册成功");
                    } else {
                        callback.onError(apiResponse.getMessage());
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
     * 用户登录
     */
    public void login(User loginUser, ApiCallback<LoginResponse> callback) {
        String url = BASE_URL + "/login";
        String json = gson.toJson(loginUser);

        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Type type = new TypeToken<ApiResponse<LoginResponse>>(){}.getType();
                    ApiResponse<LoginResponse> apiResponse = gson.fromJson(responseBody, type);

                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getData());
                    } else {
                        callback.onError(apiResponse.getMessage());
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
     * 刷新Token
     */
    public void refreshToken(String token, ApiCallback<TokenResponse> callback) {
        String url = BASE_URL + "/refreshToken";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create(new byte[0]))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Type type = new TypeToken<ApiResponse<TokenResponse>>(){}.getType();
                    ApiResponse<TokenResponse> apiResponse = gson.fromJson(responseBody, type);

                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getData());
                    } else {
                        callback.onError(apiResponse.getMessage());
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
     * 退出登录
     */
    public void logout(String token, ApiCallback<String> callback) {
        String url = BASE_URL + "/logout";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create(new byte[0]))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Type type = new TypeToken<ApiResponse<String>>(){}.getType();
                    ApiResponse<String> apiResponse = gson.fromJson(responseBody, type);

                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getData() != null ? apiResponse.getData() : "退出成功");
                    } else {
                        callback.onError(apiResponse.getMessage());
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
     * 获取用户信息
     */
    public void getUserInfo(Long userId, String token, ApiCallback<User> callback) {
        String url = BASE_URL + "/" + userId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Type type = new TypeToken<ApiResponse<User>>(){}.getType();
                    ApiResponse<User> apiResponse = gson.fromJson(responseBody, type);

                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getData());
                    } else {
                        callback.onError(apiResponse.getMessage());
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
     * 更新用户资料
     */
    public void updateProfile(User user, String token, ApiCallback<String> callback) {
        String url = BASE_URL + "/update";
        String json = gson.toJson(user);

        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Type type = new TypeToken<ApiResponse<String>>(){}.getType();
                    ApiResponse<String> apiResponse = gson.fromJson(responseBody, type);

                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getData() != null ? apiResponse.getData() : "更新成功");
                    } else {
                        callback.onError(apiResponse.getMessage());
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
     * 修改密码
     */
    public void updatePassword(UpdatePasswordRequest request, String token, ApiCallback<String> callback) {
        String url = BASE_URL + "/updatePassword";
        String json = gson.toJson(request);

        RequestBody body = RequestBody.create(JSON, json);
        Request requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .put(body)
                .build();

        client.newCall(requestBuilder).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Type type = new TypeToken<ApiResponse<String>>(){}.getType();
                    ApiResponse<String> apiResponse = gson.fromJson(responseBody, type);

                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getData() != null ? apiResponse.getData() : "密码修改成功");
                    } else {
                        callback.onError(apiResponse.getMessage());
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
     * 校验用户名唯一性
     */
    public void checkUserNameUnique(String userName, ApiCallback<Boolean> callback) {
        String url = BASE_URL + "/checkUserNameUnique/" + userName;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Type type = new TypeToken<ApiResponse<Boolean>>(){}.getType();
                    ApiResponse<Boolean> apiResponse = gson.fromJson(responseBody, type);

                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getData());
                    } else {
                        callback.onError(apiResponse.getMessage());
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
     * 校验邮箱唯一性
     */
    public void checkEmailUnique(String email, ApiCallback<Boolean> callback) {
        String url = BASE_URL + "/checkEmailUnique/" + email;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Type type = new TypeToken<ApiResponse<Boolean>>(){}.getType();
                    ApiResponse<Boolean> apiResponse = gson.fromJson(responseBody, type);

                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getData());
                    } else {
                        callback.onError(apiResponse.getMessage());
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
     * 校验手机号唯一性
     */
    public void checkPhoneUnique(String phone, ApiCallback<Boolean> callback) {
        String url = BASE_URL + "/checkPhoneUnique/" + phone;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Type type = new TypeToken<ApiResponse<Boolean>>(){}.getType();
                    ApiResponse<Boolean> apiResponse = gson.fromJson(responseBody, type);

                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getData());
                    } else {
                        callback.onError(apiResponse.getMessage());
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
     * 取消所有请求
     */
    public void cancelAllRequests() {
        client.dispatcher().cancelAll();
    }

    /**
     * 登录响应对象
     */
    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
        private User user;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }
    }

    /**
     * Token响应对象
     */
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
        }
    }

    /**
     * 修改密码请求对象
     */
    public static class UpdatePasswordRequest {
        private Long userId;
        private String oldPassword;
        private String newPassword;

        public UpdatePasswordRequest(Long userId, String oldPassword, String newPassword) {
            this.userId = userId;
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}