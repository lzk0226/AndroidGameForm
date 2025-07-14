package com.app.gameform.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.gameform.R;
import com.app.gameform.Run.ApiResponse;
import com.app.gameform.domain.User;
import com.app.gameform.network.UserApiService;
import com.app.gameform.network.ApiCallback;
import com.google.android.material.textfield.TextInputEditText;

/**
 * 登录注册页面
 * 支持登录和注册功能切换
 */
public class LoginActivity extends AppCompatActivity {

    // UI组件
    private LinearLayout loginForm, registerForm;
    private TextInputEditText etLoginUsername, etLoginPassword;
    private TextInputEditText etRegisterUsername, etRegisterNickname, etRegisterEmail,
            etRegisterPhone, etRegisterPassword, etRegisterConfirmPassword;
    private Button btnLogin, btnRegister;
    private TextView tvSwitchToRegister, tvSwitchToLogin;
    private ProgressBar progressBar;

    // 服务类
    private UserApiService userApiService;
    private SharedPreferences sharedPreferences;

    // 常量
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        initViews();
        initServices();
        setListeners();

        // 检查是否已经登录
        checkLoginStatus();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        // 表单容器
        loginForm = findViewById(R.id.loginForm);
        registerForm = findViewById(R.id.registerForm);

        // 登录表单
        etLoginUsername = findViewById(R.id.etLoginUsername);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // 注册表单
        etRegisterUsername = findViewById(R.id.etRegisterUsername);
        etRegisterNickname = findViewById(R.id.etRegisterNickname);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPhone = findViewById(R.id.etRegisterPhone);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

        // 切换按钮
        tvSwitchToRegister = findViewById(R.id.tvSwitchToRegister);
        tvSwitchToLogin = findViewById(R.id.tvSwitchToLogin);

        // 进度条
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * 初始化服务
     */
    private void initServices() {
        userApiService = UserApiService.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    /**
     * 设置监听器
     */
    private void setListeners() {
        // 登录按钮
        btnLogin.setOnClickListener(v -> performLogin());

        // 注册按钮
        btnRegister.setOnClickListener(v -> performRegister());

        // 切换到注册
        tvSwitchToRegister.setOnClickListener(v -> switchToRegister());

        // 切换到登录
        tvSwitchToLogin.setOnClickListener(v -> switchToLogin());
    }

    /**
     * 检查登录状态
     */
    private void checkLoginStatus() {
        String token = sharedPreferences.getString(KEY_TOKEN, "");
        if (!TextUtils.isEmpty(token)) {
            // 已登录，跳转到主页
            navigateToMain();
        }
    }

    /**
     * 执行登录
     */
    private void performLogin() {
        String username = etLoginUsername.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        // 表单验证
        if (TextUtils.isEmpty(username)) {
            etLoginUsername.setError("请输入用户名");
            etLoginUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etLoginPassword.setError("请输入密码");
            etLoginPassword.requestFocus();
            return;
        }

        // 显示加载状态
        showLoading(true);

        // 创建用户对象
        User loginUser = new User();
        loginUser.setUserName(username);
        loginUser.setPassword(password);

        // 调用登录API
        userApiService.login(loginUser, new ApiCallback<UserApiService.LoginResponse>() {
            @Override
            public void onSuccess(UserApiService.LoginResponse response) {
                runOnUiThread(() -> {
                    showLoading(false);

                    // 保存登录信息
                    saveLoginInfo(response);

                    // 显示成功消息
                    Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();

                    // 跳转到主页
                    navigateToMain();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "登录失败: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 执行注册
     */
    private void performRegister() {
        String username = etRegisterUsername.getText().toString().trim();
        String nickname = etRegisterNickname.getText().toString().trim();
        String email = etRegisterEmail.getText().toString().trim();
        String phone = etRegisterPhone.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();
        String confirmPassword = etRegisterConfirmPassword.getText().toString().trim();

        // 表单验证
        if (TextUtils.isEmpty(username)) {
            etRegisterUsername.setError("请输入用户名");
            etRegisterUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(nickname)) {
            etRegisterNickname.setError("请输入昵称");
            etRegisterNickname.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etRegisterPassword.setError("请输入密码");
            etRegisterPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etRegisterPassword.setError("密码长度至少6位");
            etRegisterPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etRegisterConfirmPassword.setError("两次输入的密码不一致");
            etRegisterConfirmPassword.requestFocus();
            return;
        }

        // 邮箱格式验证
        if (!TextUtils.isEmpty(email) && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegisterEmail.setError("请输入有效的邮箱地址");
            etRegisterEmail.requestFocus();
            return;
        }

        // 手机号格式验证（改进后的验证）
        if (!TextUtils.isEmpty(phone) && !isValidPhone(phone)) {
            etRegisterPhone.setError("请输入有效的手机号");
            etRegisterPhone.requestFocus();
            return;
        }

        // 显示加载状态
        showLoading(true);

        // 先检查用户名唯一性
        userApiService.checkUserNameUnique(username, new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isUnique) {
                if (isUnique) {
                    // 用户名可用，继续检查邮箱
                    checkEmailAndProceed(username, nickname, email, phone, password);
                } else {
                    runOnUiThread(() -> {
                        showLoading(false);
                        etRegisterUsername.setError("用户名已存在");
                        etRegisterUsername.requestFocus();
                    });
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "检查用户名失败: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 检查邮箱并继续注册流程
     */
    private void checkEmailAndProceed(String username, String nickname, String email, String phone, String password) {
        if (!TextUtils.isEmpty(email)) {
            userApiService.checkEmailUnique(email, new ApiCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean isUnique) {
                    if (isUnique) {
                        // 邮箱可用，继续检查手机号
                        checkPhoneAndProceed(username, nickname, email, phone, password);
                    } else {
                        runOnUiThread(() -> {
                            showLoading(false);
                            etRegisterEmail.setError("邮箱已被使用");
                            etRegisterEmail.requestFocus();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, "检查邮箱失败: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            // 没有邮箱，直接检查手机号
            checkPhoneAndProceed(username, nickname, email, phone, password);
        }
    }

    /**
     * 检查手机号并继续注册流程
     */
    private void checkPhoneAndProceed(String username, String nickname, String email, String phone, String password) {
        if (!TextUtils.isEmpty(phone)) {
            userApiService.checkPhoneUnique(phone, new ApiCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean isUnique) {
                    if (isUnique) {
                        // 手机号可用，执行注册
                        executeRegister(username, nickname, email, phone, password);
                    } else {
                        runOnUiThread(() -> {
                            showLoading(false);
                            etRegisterPhone.setError("手机号已被使用");
                            etRegisterPhone.requestFocus();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, "检查手机号失败: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            // 没有手机号，直接执行注册
            executeRegister(username, nickname, email, phone, password);
        }
    }

    /**
     * 执行注册
     */
    private void executeRegister(String username, String nickname, String email, String phone, String password) {
        // 创建用户对象
        User user = new User();
        user.setUserName(username);
        user.setNickName(nickname);
        user.setEmail(TextUtils.isEmpty(email) ? null : email);
        user.setPhonenumber(TextUtils.isEmpty(phone) ? null : phone);
        user.setPassword(password);

        // 调用注册API
        userApiService.register(user, new ApiCallback<String>() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();

                    // 注册成功后切换到登录页面
                    switchToLogin();

                    // 预填充用户名
                    etLoginUsername.setText(username);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "注册失败: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 手机号验证
     */
    private boolean isValidPhone(String phone) {
        // 简单的手机号验证，11位数字，以1开头
        return phone.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 切换到注册页面
     */
    private void switchToRegister() {
        loginForm.setVisibility(View.GONE);
        registerForm.setVisibility(View.VISIBLE);

        // 清空登录表单
        etLoginUsername.setText("");
        etLoginPassword.setText("");
        // 清空错误提示
        etLoginUsername.setError(null);
        etLoginPassword.setError(null);
    }

    /**
     * 切换到登录页面
     */
    private void switchToLogin() {
        registerForm.setVisibility(View.GONE);
        loginForm.setVisibility(View.VISIBLE);

        // 清空注册表单
        clearRegisterForm();
    }

    /**
     * 清空注册表单
     */
    private void clearRegisterForm() {
        etRegisterUsername.setText("");
        etRegisterNickname.setText("");
        etRegisterEmail.setText("");
        etRegisterPhone.setText("");
        etRegisterPassword.setText("");
        etRegisterConfirmPassword.setText("");

        // 清空错误提示
        etRegisterUsername.setError(null);
        etRegisterNickname.setError(null);
        etRegisterEmail.setError(null);
        etRegisterPhone.setError(null);
        etRegisterPassword.setError(null);
        etRegisterConfirmPassword.setError(null);
    }

    /**
     * 显示/隐藏加载状态
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnLogin != null) {
            btnLogin.setEnabled(!show);
        }
        if (btnRegister != null) {
            btnRegister.setEnabled(!show);
        }
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
     * 跳转到主页
     */
    private void navigateToMain() {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        if (userApiService != null) {
            userApiService.cancelAllRequests();
        }
    }
}