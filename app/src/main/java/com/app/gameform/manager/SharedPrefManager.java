package com.app.gameform.manager;
import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {
    private static SharedPrefManager instance;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";

    private SharedPrefManager(Context context) {
        // ⭐ 使用 "UserPrefs" 而不是 "app_prefs"
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    // ⭐ 保存 token（键名改为 "token"）
    public void saveToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    // ⭐ 获取 token（键名改为 "token"）
    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    // 新增：保存用户 ID
    public void saveUserId(long userId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(KEY_USER_ID, userId);
        editor.apply();
    }

    // 新增：获取用户 ID
    public long getUserId() {
        return sharedPreferences.getLong(KEY_USER_ID, 0);
    }

    // 新增：保存用户名
    public void saveUsername(String username) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    // 新增：获取用户名
    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    // 新增：保存 refresh token
    public void saveRefreshToken(String refreshToken) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.apply();
    }

    // 新增：获取 refresh token
    public String getRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    // 新增：清除所有用户数据（用于退出登录）
    public void clearUserData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_REFRESH_TOKEN);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USERNAME);
        editor.apply();
    }

    // 新增：检查是否已登录
    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }
}