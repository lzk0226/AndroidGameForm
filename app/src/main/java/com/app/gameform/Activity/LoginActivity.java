package com.app.gameform.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.gameform.Activity.Home.ProfileActivity;
import com.app.gameform.R;
import com.app.gameform.manager.UserManager;
import com.app.gameform.manager.SharedPrefManager;
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
    private UserManager userManager;

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
        userManager = UserManager.getInstance(this);
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
        if (userManager.isLoggedIn()) {
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
        if (!userManager.validateUsername(username, etLoginUsername)) {
            return;
        }

        if (!userManager.validatePassword(password, etLoginPassword)) {
            return;
        }

        // 显示加载状态
        showLoading(true);

        userManager.login(username, password, new UserManager.UserOperationCallback() {
            @Override
            public void onSuccess(String message, String token) {
                // 确保在主线程中执行UI操作
                if (!isFinishing() && !isDestroyed()) {
                    runOnUiThread(() -> {
                        try {
                            // 隐藏加载状态
                            showLoading(false);

                            // 保存 token（如果token不为空）
                            if (!TextUtils.isEmpty(token)) {
                                SharedPrefManager.getInstance(LoginActivity.this).saveToken(token);
                            }

                            // 显示成功消息
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();

                            // 跳转到主页面
                            navigateToMain();
                        } catch (Exception e) {
                            e.printStackTrace();
                            // 发生异常时也要隐藏加载状态
                            showLoading(false);
                            Toast.makeText(LoginActivity.this, "登录处理异常，请重试", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                // 确保在主线程中执行UI操作
                if (!isFinishing() && !isDestroyed()) {
                    runOnUiThread(() -> {
                        try {
                            // 隐藏加载状态
                            showLoading(false);

                            // 显示错误消息
                            String errorMessage = parseLoginError(error);
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();

                            // 清空密码框
                            etLoginPassword.setText("");
                            etLoginPassword.requestFocus();
                        } catch (Exception e) {
                            e.printStackTrace();
                            // 发生异常时也要隐藏加载状态
                            showLoading(false);
                        }
                    });
                }
            }
        });
    }

    /**
     * 解析登录错误信息
     */
    private String parseLoginError(String error) {
        if (TextUtils.isEmpty(error)) {
            return "登录失败，请重试";
        }

        // 处理常见的登录错误
        if (error.contains("用户名或密码错误") || error.contains("账号已被停用") ||
                error.contains("LOGIN_FAILED") || error.contains("1008")) {
            return "用户名或密码错误";
        } else if (error.contains("网络") || error.contains("连接") ||
                error.contains("timeout") || error.contains("NETWORK")) {
            return "网络连接失败，请检查网络后重试";
        } else if (error.contains("服务器") || error.contains("500") ||
                error.contains("INTERNAL_SERVER_ERROR")) {
            return "服务器暂时不可用，请稍后重试";
        }

        return "登录失败：" + error;
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
        if (!userManager.validateUsername(username, etRegisterUsername)) {
            return;
        }

        if (!userManager.validateNickname(nickname, etRegisterNickname)) {
            return;
        }

        if (!userManager.validatePassword(password, etRegisterPassword)) {
            return;
        }

        if (!userManager.validateConfirmPassword(password, confirmPassword, etRegisterConfirmPassword)) {
            return;
        }

        if (!userManager.validateEmail(email, etRegisterEmail)) {
            return;
        }

        if (!userManager.validatePhone(phone, etRegisterPhone)) {
            return;
        }

        // 显示加载状态
        showLoading(true);

        userManager.register(username, nickname, email, phone, password, new UserManager.UserOperationCallback() {
            @Override
            public void onSuccess(String message, String token) {
                if (!isFinishing() && !isDestroyed()) {
                    runOnUiThread(() -> {
                        try {
                            showLoading(false);
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();

                            // 注册成功后，清空表单并切换到登录页面
                            clearRegisterForm();
                            switchToLogin();
                        } catch (Exception e) {
                            e.printStackTrace();
                            showLoading(false);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (!isFinishing() && !isDestroyed()) {
                    runOnUiThread(() -> {
                        try {
                            showLoading(false);
                            String errorMessage = parseRegisterError(error);
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            showLoading(false);
                        }
                    });
                }
            }
        });
    }

    /**
     * 解析注册错误信息
     */
    private String parseRegisterError(String error) {
        if (TextUtils.isEmpty(error)) {
            return "注册失败，请重试";
        }

        // 处理常见的注册错误
        if (error.contains("用户名已存在") || error.contains("USER_NAME_EXISTS") || error.contains("1005")) {
            return "用户名已存在";
        } else if (error.contains("邮箱已存在") || error.contains("EMAIL_EXISTS") || error.contains("1006")) {
            return "邮箱已被使用";
        } else if (error.contains("手机号已存在") || error.contains("PHONE_EXISTS") || error.contains("1007")) {
            return "手机号已被使用";
        } else if (error.contains("Duplicate entry")) {
            if (error.contains("uk_user_name")) {
                return "用户名已存在";
            } else if (error.contains("email")) {
                return "邮箱已存在";
            } else if (error.contains("phone")) {
                return "手机号已存在";
            }
        }

        return "注册失败：" + error;
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
        try {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (btnLogin != null) {
                btnLogin.setEnabled(!show);
            }
            if (btnRegister != null) {
                btnRegister.setEnabled(!show);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 跳转到主页
     */
    private void navigateToMain() {
        try {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "页面跳转异常", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 确保隐藏加载状态
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }
}