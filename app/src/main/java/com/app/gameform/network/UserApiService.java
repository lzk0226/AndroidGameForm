package com.app.gameform.network;

import static com.app.gameform.network.ApiConstants.USER_PROFILE;

import android.util.Log;

import com.app.gameform.Run.ApiResponse;
import com.app.gameform.domain.User;
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

public class UserApiService {

    private static UserApiService instance;
    private OkHttpClient client;
    private Gson gson;

    private static final String TAG = "UserApiService";

    private static final String BASE_URL = USER_PROFILE;
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

    // 通用请求执行方法
    private <T> void makeRequest(String url, String method, String token, Object requestBodyObj,
                                 Type apiResponseType, ApiCallback<T> callback, String logTag) {

        RequestBody body = null;
        if (requestBodyObj != null) {
            String json = gson.toJson(requestBodyObj);
            body = RequestBody.create(JSON, json);
        } else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            // 空body占位
            body = RequestBody.create(new byte[0]);
        }

        Request.Builder builder = new Request.Builder()
                .url(url);

        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        switch (method.toUpperCase()) {
            case "GET":
                builder.get();
                break;
            case "POST":
                builder.post(body);
                break;
            case "PUT":
                builder.put(body);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        Request request = builder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                callback.onError("网络连接失败: " + e.getMessage());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    ApiResponse<T> apiResponse = gson.fromJson(responseBody, apiResponseType);

                    if (apiResponse.isSuccess()) {
                        // 兼容原有某些接口空数据返回默认提示文字
                        T data = apiResponse.getData();
                        if (data == null &&
                                (callback instanceof ApiCallbackStringDefault)) {
                            @SuppressWarnings("unchecked")
                            T defaultMsg = (T) ((ApiCallbackStringDefault) callback).getDefaultSuccessMessage();
                            callback.onSuccess(defaultMsg);
                        } else {
                            callback.onSuccess(data);
                        }
                    } else {
                        //callback.onError(apiResponse.getMessage());
                    }
                } catch (Exception e) {
                    //Log.e(TAG, logTag + "解析响应失败", e);
                } finally {
                    response.close();
                }
            }
        });
    }

    // 用于带默认成功提示的String类型回调标记接口
    private interface ApiCallbackStringDefault extends ApiCallback<String> {
        String getDefaultSuccessMessage();
    }

    // 以下所有接口调用均调用 makeRequest，保持方法名和签名不变

    public void register(User user, ApiCallback<String> callback) {
        String url = BASE_URL + "/register";
        makeRequest(url, "POST", null, user,
                new TypeToken<ApiResponse<String>>(){}.getType(),
                new ApiCallbackStringDefault() {
                    @Override public void onSuccess(String data) {
                        callback.onSuccess(data);
                    }
                    @Override public void onError(String errorMsg) {
                        callback.onError(errorMsg);
                    }
                    @Override
                    public String getDefaultSuccessMessage() {
                        return "注册成功";
                    }
                }, "register");
    }

    public void login(User loginUser, ApiCallback<LoginResponse> callback) {
        String url = BASE_URL + "/login";
        makeRequest(url, "POST", null, loginUser,
                new TypeToken<ApiResponse<LoginResponse>>(){}.getType(),
                callback, "login");
    }

    public void refreshToken(String token, ApiCallback<TokenResponse> callback) {
        String url = BASE_URL + "/refreshToken";
        makeRequest(url, "POST", token, null,
                new TypeToken<ApiResponse<TokenResponse>>(){}.getType(),
                callback, "refreshToken");
    }

    public void logout(String token, ApiCallback<String> callback) {
        String url = BASE_URL + "/logout";
        makeRequest(url, "POST", token, null,
                new TypeToken<ApiResponse<String>>(){}.getType(),
                new ApiCallbackStringDefault() {
                    @Override public void onSuccess(String data) {
                        callback.onSuccess(data);
                    }
                    @Override public void onError(String errorMsg) {
                        callback.onError(errorMsg);
                    }
                    @Override
                    public String getDefaultSuccessMessage() {
                        return "退出成功";
                    }
                }, "logout");
    }

    public void getUserInfo(Long userId, String token, ApiCallback<User> callback) {
        String url = BASE_URL + "/" + userId;
        makeRequest(url, "GET", token, null,
                new TypeToken<ApiResponse<User>>(){}.getType(),
                callback, "getUserInfo");
    }

    public void updateProfile(User user, String token, ApiCallback<String> callback) {
        String url = BASE_URL + "/update";
        makeRequest(url, "PUT", token, user,
                new TypeToken<ApiResponse<String>>(){}.getType(),
                new ApiCallbackStringDefault() {
                    @Override public void onSuccess(String data) {
                        callback.onSuccess(data);
                    }
                    @Override public void onError(String errorMsg) {
                        callback.onError(errorMsg);
                    }
                    @Override
                    public String getDefaultSuccessMessage() {
                        return "更新成功";
                    }
                }, "updateProfile");
    }

    public void updatePassword(UpdatePasswordRequest request, String token, ApiCallback<String> callback) {
        String url = BASE_URL + "/updatePassword";
        makeRequest(url, "PUT", token, request,
                new TypeToken<ApiResponse<String>>(){}.getType(),
                new ApiCallbackStringDefault() {
                    @Override public void onSuccess(String data) {
                        callback.onSuccess(data);
                    }
                    @Override public void onError(String errorMsg) {
                        callback.onError(errorMsg);
                    }
                    @Override
                    public String getDefaultSuccessMessage() {
                        return "密码修改成功";
                    }
                }, "updatePassword");
    }

    public void checkUserNameUnique(String userName, ApiCallback<Boolean> callback) {
        String url = BASE_URL + "/checkUserNameUnique/" + userName;
        makeRequest(url, "GET", null, null,
                new TypeToken<ApiResponse<Boolean>>(){}.getType(),
                callback, "checkUserNameUnique");
    }

    public void checkEmailUnique(String email, ApiCallback<Boolean> callback) {
        String url = BASE_URL + "/checkEmailUnique/" + email;
        makeRequest(url, "GET", null, null,
                new TypeToken<ApiResponse<Boolean>>(){}.getType(),
                callback, "checkEmailUnique");
    }

    public void checkPhoneUnique(String phone, ApiCallback<Boolean> callback) {
        String url = BASE_URL + "/checkPhoneUnique/" + phone;
        makeRequest(url, "GET", null, null,
                new TypeToken<ApiResponse<Boolean>>(){}.getType(),
                callback, "checkPhoneUnique");
    }

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