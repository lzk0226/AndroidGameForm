package com.app.gameform.Activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.app.gameform.R;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.UserApiService;

public class ChangePasswordActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private UserApiService userApiService;

    // UI组件
    private Toolbar toolbar;
    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Button btnChangePassword;

    // 常量
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        initViews();
        initServices();
        setupToolbar();
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
    }

    private void initServices() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userApiService = UserApiService.getInstance();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("修改密码");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        if (!validateInput()) {
            return;
        }

        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        long userId = sharedPreferences.getLong(KEY_USER_ID, 0);
        String token = sharedPreferences.getString(KEY_TOKEN, "");

        if (userId == 0 || TextUtils.isEmpty(token)) {
            Toast.makeText(this, "用户信息异常，请重新登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnChangePassword.setEnabled(false);
        btnChangePassword.setText("修改中...");

        UserApiService.UpdatePasswordRequest request = new UserApiService.UpdatePasswordRequest(
                userId, oldPassword, newPassword);

        userApiService.updatePassword(request, token, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(ChangePasswordActivity.this, "密码修改成功", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ChangePasswordActivity.this, "修改失败：" + error, Toast.LENGTH_SHORT).show();
                    btnChangePassword.setEnabled(true);
                    btnChangePassword.setText("确认修改");
                });
            }
        });
    }

    private boolean validateInput() {
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 原密码不能为空
        if (TextUtils.isEmpty(oldPassword)) {
            etOldPassword.setError("请输入原密码");
            etOldPassword.requestFocus();
            return false;
        }

        // 新密码不能为空
        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("请输入新密码");
            etNewPassword.requestFocus();
            return false;
        }

        // 新密码长度检查
        if (newPassword.length() < 6) {
            etNewPassword.setError("新密码长度不能少于6位");
            etNewPassword.requestFocus();
            return false;
        }

        // 确认密码不能为空
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("请确认新密码");
            etConfirmPassword.requestFocus();
            return false;
        }

        // 两次密码输入一致性检查
        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("两次输入的密码不一致");
            etConfirmPassword.requestFocus();
            return false;
        }

        // 新密码不能与原密码相同
        if (oldPassword.equals(newPassword)) {
            etNewPassword.setError("新密码不能与原密码相同");
            etNewPassword.requestFocus();
            return false;
        }

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userApiService != null) {
            userApiService.cancelAllRequests();
        }
    }
}