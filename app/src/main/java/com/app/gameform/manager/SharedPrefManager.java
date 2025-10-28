package com.app.gameform.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.app.gameform.network.ApiConstants;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SharedPrefManager {
    private static final String TAG = "SharedPrefManager";
    private static SharedPrefManager instance;
    private SharedPreferences sharedPreferences;
    private OkHttpClient httpClient;

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";

    // ⭐ 新增：Token 变更监听器列表
    private List<TokenChangeListener> tokenChangeListeners = new ArrayList<>();

    private SharedPrefManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        httpClient = new OkHttpClient();
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context.getApplicationContext());
        }
        return instance;
    }

    // ==================== Token 相关方法 ====================

    /**
     * 保存 token
     */
    public void saveToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_TOKEN, token);
        editor.apply();

        // ⭐ 通知监听器 token 已更新
        notifyTokenChanged(token);
    }

    /**
     * 获取 token
     */
    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    /**
     * 保存 refresh token
     */
    public void saveRefreshToken(String refreshToken) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.apply();
    }

    /**
     * 获取 refresh token
     */
    public String getRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    // ==================== 用户信息相关方法 ====================

    /**
     * 保存用户 ID
     */
    public void saveUserId(long userId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(KEY_USER_ID, userId);
        editor.apply();
    }

    /**
     * 获取用户 ID
     */
    public long getUserId() {
        return sharedPreferences.getLong(KEY_USER_ID, 0);
    }

    /**
     * 保存用户名
     */
    public void saveUsername(String username) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    /**
     * 获取用户名
     */
    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    /**
     * 检查是否已登录
     */
    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    /**
     * 清除所有用户数据（用于退出登录）
     */
    public void clearUserData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_REFRESH_TOKEN);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USERNAME);
        editor.apply();

        // ⭐ 通知监听器用户已登出
        notifyUserLoggedOut();
    }

    // ==================== 自动刷新 Token 功能 ====================

    /**
     * 应用启动时自动刷新 Token（如果已登录）
     * 建议在 Application 的 onCreate 或启动页调用
     */
    public void autoRefreshTokenOnStartup(final TokenRefreshCallback callback) {
        if (!isLoggedIn()) {
            Log.d(TAG, "用户未登录，跳过 Token 刷新");
            if (callback != null) {
                callback.onRefreshSkipped();
            }
            return;
        }

        String refreshToken = getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            Log.w(TAG, "RefreshToken 不存在，无法刷新");
            if (callback != null) {
                callback.onRefreshFailed("RefreshToken 不存在");
            }
            return;
        }

        Log.d(TAG, "开始自动刷新 Token...");
        refreshAccessToken(refreshToken, callback);
    }

    /**
     * 使用 RefreshToken 刷新 AccessToken
     */
    private void refreshAccessToken(String refreshToken, final TokenRefreshCallback callback) {
        // 构建请求 URL
        String url = ApiConstants.BASE_URL + "/user/profile/refreshToken";

        // 构建请求
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + refreshToken)
                .post(RequestBody.create(MediaType.parse("application/json"), "{}"))
                .build();

        // 异步请求
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "刷新 Token 失败: " + e.getMessage());
                if (callback != null) {
                    callback.onRefreshFailed(e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "刷新 Token 失败，HTTP 状态码: " + response.code());
                    if (callback != null) {
                        callback.onRefreshFailed("HTTP " + response.code());
                    }
                    // Token 过期或无效，清除用户数据
                    if (response.code() == 401 || response.code() == 403) {
                        clearUserData();
                    }
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);

                    // 检查响应状态
                    int code = jsonResponse.optInt("code", -1);
                    if (code == 200) {
                        JSONObject data = jsonResponse.getJSONObject("data");
                        String newAccessToken = data.getString("accessToken");
                        String newRefreshToken = data.getString("refreshToken");

                        // 保存新的 Token（会自动触发监听器）
                        saveToken(newAccessToken);
                        saveRefreshToken(newRefreshToken);

                        Log.d(TAG, "Token 刷新成功");
                        if (callback != null) {
                            callback.onRefreshSuccess(newAccessToken, newRefreshToken);
                        }
                    } else {
                        String message = jsonResponse.optString("msg", "刷新失败");
                        Log.e(TAG, "刷新 Token 失败: " + message);
                        if (callback != null) {
                            callback.onRefreshFailed(message);
                        }
                        // Token 无效，清除用户数据
                        clearUserData();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析响应失败: " + e.getMessage());
                    if (callback != null) {
                        callback.onRefreshFailed("解析响应失败");
                    }
                }
            }
        });
    }

    // ==================== ⭐ 新增：Token 监听器机制 ====================

    /**
     * 添加 Token 变更监听器
     */
    public void addTokenChangeListener(TokenChangeListener listener) {
        if (listener != null && !tokenChangeListeners.contains(listener)) {
            tokenChangeListeners.add(listener);
            Log.d(TAG, "添加 Token 监听器，当前监听器数量: " + tokenChangeListeners.size());
        }
    }

    /**
     * 移除 Token 变更监听器
     */
    public void removeTokenChangeListener(TokenChangeListener listener) {
        if (listener != null) {
            tokenChangeListeners.remove(listener);
            Log.d(TAG, "移除 Token 监听器，当前监听器数量: " + tokenChangeListeners.size());
        }
    }

    /**
     * 通知所有监听器 Token 已更新
     */
    private void notifyTokenChanged(String newToken) {
        Log.d(TAG, "通知 Token 变更，监听器数量: " + tokenChangeListeners.size());
        for (TokenChangeListener listener : new ArrayList<>(tokenChangeListeners)) {
            try {
                listener.onTokenChanged(newToken);
            } catch (Exception e) {
                Log.e(TAG, "通知监听器时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 通知所有监听器用户已登出
     */
    private void notifyUserLoggedOut() {
        Log.d(TAG, "通知用户登出，监听器数量: " + tokenChangeListeners.size());
        for (TokenChangeListener listener : new ArrayList<>(tokenChangeListeners)) {
            try {
                listener.onUserLoggedOut();
            } catch (Exception e) {
                Log.e(TAG, "通知监听器时出错: " + e.getMessage());
            }
        }
    }

    /**
     * Token 变更监听器接口
     */
    public interface TokenChangeListener {
        /**
         * Token 已更新
         */
        void onTokenChanged(String newToken);

        /**
         * 用户已登出
         */
        void onUserLoggedOut();
    }

    /**
     * Token 刷新回调接口
     */
    public interface TokenRefreshCallback {
        /**
         * 刷新成功
         */
        void onRefreshSuccess(String newAccessToken, String newRefreshToken);

        /**
         * 刷新失败
         */
        void onRefreshFailed(String error);

        /**
         * 跳过刷新（用户未登录）
         */
        void onRefreshSkipped();
    }
}