package com.app.gameform.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;
import com.app.gameform.domain.User;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.UserApiService;

/**
 * 用户管理类 - 统一处理所有用户相关操作
 * 整合登录、注册、个人信息管理等功能，减少代码冗余
 */
public class UserManager {

    private static UserManager instance;
    private Context context;
    private SharedPreferences sharedPreferences;
    private UserApiService userApiService;

    // 常量定义
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";

    // 验证规则
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MIN_NICKNAME_LENGTH = 2;
    private static final int MAX_NICKNAME_LENGTH = 20;

    private UserManager(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.userApiService = UserApiService.getInstance();
    }

    public static UserManager getInstance(Context context) {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager(context);
                }
            }
        }
        return instance;
    }

    // ==================== 登录状态管理 ====================

    /**
     * 检查是否已登录
     */
    public boolean isLoggedIn() {
        String token = sharedPreferences.getString(KEY_TOKEN, "");
        return !TextUtils.isEmpty(token);
    }

    /**
     * 获取当前用户ID
     */
    public long getCurrentUserId() {
        return sharedPreferences.getLong(KEY_USER_ID, 0);
    }

    /**
     * 获取当前用户名
     */
    public String getCurrentUsername() {
        return sharedPreferences.getString(KEY_USERNAME, "");
    }

    /**
     * 获取当前Token
     */
    public String getCurrentToken() {
        return sharedPreferences.getString(KEY_TOKEN, "");
    }

    /**
     * 保存登录信息
     */
    private void saveLoginInfo(UserApiService.LoginResponse response) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_TOKEN, response.getAccessToken());
        editor.putString(KEY_REFRESH_TOKEN, response.getRefreshToken());
        editor.putLong(KEY_USER_ID, response.getUser().getUserId());
        editor.putString(KEY_USERNAME, response.getUser().getUserName());
        editor.apply();
    }

    /**
     * 清除登录信息
     */
    public void clearLoginInfo() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_REFRESH_TOKEN);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USERNAME);
        editor.apply();
    }

    // ==================== 表单验证 ====================

    /**
     * 验证用户名
     */
    public boolean validateUsername(String username, EditText editText) {
        if (TextUtils.isEmpty(username)) {
            setError(editText, "请输入用户名");
            return false;
        }
        return true;
    }

    /**
     * 验证密码
     */
    public boolean validatePassword(String password, EditText editText) {
        if (TextUtils.isEmpty(password)) {
            setError(editText, "请输入密码");
            return false;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            setError(editText, "密码长度至少" + MIN_PASSWORD_LENGTH + "位");
            return false;
        }
        return true;
    }

    /**
     * 验证昵称
     */
    public boolean validateNickname(String nickname, EditText editText) {
        if (TextUtils.isEmpty(nickname)) {
            setError(editText, "昵称不能为空");
            return false;
        }
        if (nickname.length() < MIN_NICKNAME_LENGTH || nickname.length() > MAX_NICKNAME_LENGTH) {
            setError(editText, "昵称长度应在" + MIN_NICKNAME_LENGTH + "-" + MAX_NICKNAME_LENGTH + "个字符之间");
            return false;
        }
        return true;
    }

    /**
     * 验证邮箱
     */
    public boolean validateEmail(String email, EditText editText) {
        if (!TextUtils.isEmpty(email) && !email.matches(EMAIL_PATTERN)) {
            setError(editText, "邮箱格式不正确");
            return false;
        }
        return true;
    }

    /**
     * 验证手机号
     */
    public boolean validatePhone(String phone, EditText editText) {
        if (!TextUtils.isEmpty(phone) && !phone.matches(PHONE_PATTERN)) {
            setError(editText, "手机号格式不正确");
            return false;
        }
        return true;
    }

    /**
     * 验证确认密码
     */
    public boolean validateConfirmPassword(String password, String confirmPassword, EditText editText) {
        if (!password.equals(confirmPassword)) {
            setError(editText, "两次输入的密码不一致");
            return false;
        }
        return true;
    }

    /**
     * 设置错误提示并获取焦点
     */
    private void setError(EditText editText, String message) {
        if (editText != null) {
            editText.setError(message);
            editText.requestFocus();
        }
    }

    // ==================== 用户操作接口 ====================

    /**
     * 用户登录
     */
    public void login(String username, String password, UserOperationCallback callback) {
        User loginUser = new User();
        loginUser.setUserName(username);
        loginUser.setPassword(password);

        userApiService.login(loginUser, new ApiCallback<UserApiService.LoginResponse>() {
            @Override
            public void onSuccess(UserApiService.LoginResponse response) {
                saveLoginInfo(response);
                callback.onSuccess("登录成功");
            }

            @Override
            public void onError(String error) {
                callback.onError("登录失败: " + error);
            }
        });
    }

    /**
     * 用户注册
     */
    public void register(String username, String nickname, String email, String phone, String password, UserOperationCallback callback) {
        // 先进行唯一性检查
        checkUniqueFields(username, email, phone, new UniqueCheckCallback() {
            @Override
            public void onAllUnique() {
                executeRegister(username, nickname, email, phone, password, callback);
            }

            @Override
            public void onUsernameExists() {
                callback.onError("用户名已存在");
            }

            @Override
            public void onEmailExists() {
                callback.onError("邮箱已被使用");
            }

            @Override
            public void onPhoneExists() {
                callback.onError("手机号已被使用");
            }

            @Override
            public void onCheckError(String error) {
                callback.onError("验证失败: " + error);
            }
        });
    }

    /**
     * 执行注册
     */
    private void executeRegister(String username, String nickname, String email, String phone, String password, UserOperationCallback callback) {
        User user = new User();
        user.setUserName(username);
        user.setNickName(nickname);
        user.setEmail(TextUtils.isEmpty(email) ? null : email);
        user.setPhonenumber(TextUtils.isEmpty(phone) ? null : phone);
        user.setPassword(password);

        userApiService.register(user, new ApiCallback<String>() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError("注册失败: " + error);
            }
        });
    }

    /**
     * 获取用户信息
     */
    public void getUserInfo(UserInfoCallback callback) {
        long userId = getCurrentUserId();
        String token = getCurrentToken();

        if (userId == 0 || TextUtils.isEmpty(token)) {
            callback.onError("用户信息异常，请重新登录");
            return;
        }

        userApiService.getUserInfo(userId, token, new ApiCallback<User>() {
            @Override
            public void onSuccess(User user) {
                callback.onSuccess(user);
            }

            @Override
            public void onError(String error) {
                callback.onError("获取用户信息失败: " + error);
            }
        });
    }

    /**
     * 更新用户资料
     */
    public void updateProfile(String nickname, String gender, String email, String phone, UserOperationCallback callback) {
        String token = getCurrentToken();
        long userId = getCurrentUserId();

        if (userId == 0 || TextUtils.isEmpty(token)) {
            callback.onError("用户信息异常，请重新登录");
            return;
        }

        User updateUser = new User();
        updateUser.setUserId(userId);
        updateUser.setNickName(nickname);
        updateUser.setSex(gender);
        updateUser.setEmail(TextUtils.isEmpty(email) ? null : email);
        updateUser.setPhonenumber(TextUtils.isEmpty(phone) ? null : phone);

        userApiService.updateProfile(updateUser, token, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                callback.onSuccess("个人信息更新成功");
            }

            @Override
            public void onError(String error) {
                callback.onError("更新失败: " + error);
            }
        });
    }

    /**
     * 修改密码
     */
    public void changePassword(String oldPassword, String newPassword, UserOperationCallback callback) {
        String token = getCurrentToken();
        long userId = getCurrentUserId();

        if (userId == 0 || TextUtils.isEmpty(token)) {
            callback.onError("用户信息异常，请重新登录");
            return;
        }

        UserApiService.UpdatePasswordRequest request = new UserApiService.UpdatePasswordRequest(userId, oldPassword, newPassword);

        userApiService.updatePassword(request, token, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                callback.onSuccess("密码修改成功");
            }

            @Override
            public void onError(String error) {
                callback.onError("密码修改失败: " + error);
            }
        });
    }

    /**
     * 退出登录
     */
    public void logout(UserOperationCallback callback) {
        String token = getCurrentToken();

        if (TextUtils.isEmpty(token)) {
            clearLoginInfo();
            callback.onSuccess("已退出登录");
            return;
        }

        userApiService.logout(token, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                clearLoginInfo();
                callback.onSuccess("已退出登录");
            }

            @Override
            public void onError(String error) {
                // 即使服务器端登出失败，也清除本地数据
                clearLoginInfo();
                callback.onSuccess("已退出登录");
            }
        });
    }

    // ==================== 唯一性检查 ====================

    /**
     * 检查字段唯一性
     */
    private void checkUniqueFields(String username, String email, String phone, UniqueCheckCallback callback) {
        // 检查用户名
        userApiService.checkUserNameUnique(username, new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isUnique) {
                if (!isUnique) {
                    callback.onUsernameExists();
                    return;
                }

                // 检查邮箱
                if (!TextUtils.isEmpty(email)) {
                    userApiService.checkEmailUnique(email, new ApiCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean isUnique) {
                            if (!isUnique) {
                                callback.onEmailExists();
                                return;
                            }
                            checkPhoneUnique(phone, callback);
                        }

                        @Override
                        public void onError(String error) {
                            callback.onCheckError(error);
                        }
                    });
                } else {
                    checkPhoneUnique(phone, callback);
                }
            }

            @Override
            public void onError(String error) {
                callback.onCheckError(error);
            }
        });
    }

    /**
     * 检查手机号唯一性
     */
    private void checkPhoneUnique(String phone, UniqueCheckCallback callback) {
        if (!TextUtils.isEmpty(phone)) {
            userApiService.checkPhoneUnique(phone, new ApiCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean isUnique) {
                    if (!isUnique) {
                        callback.onPhoneExists();
                    } else {
                        callback.onAllUnique();
                    }
                }

                @Override
                public void onError(String error) {
                    callback.onCheckError(error);
                }
            });
        } else {
            callback.onAllUnique();
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 显示Toast消息
     */
    public void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示长Toast消息
     */
    public void showLongToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    // ==================== 回调接口 ====================

    /**
     * 用户操作回调接口
     */
    public interface UserOperationCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    /**
     * 用户信息回调接口
     */
    public interface UserInfoCallback {
        void onSuccess(User user);
        void onError(String error);
    }

    /**
     * 唯一性检查回调接口
     */
    private interface UniqueCheckCallback {
        void onAllUnique();
        void onUsernameExists();
        void onEmailExists();
        void onPhoneExists();
        void onCheckError(String error);
    }
}