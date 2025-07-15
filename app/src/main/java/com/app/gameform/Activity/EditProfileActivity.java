package com.app.gameform.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.app.gameform.R;
import com.app.gameform.domain.User;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.UserApiService;

import java.util.regex.Pattern;

public class EditProfileActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private UserApiService userApiService;
    private User currentUser;

    // UI组件
    private Toolbar toolbar;
    private EditText etNickname;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale, rbOther;
    private EditText etEmail;
    private EditText etPhone;
    private Button btnSave;
    private Button btnChangePassword;

    // 常量
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";

    // 邮箱和手机号验证正则表达式
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initViews();
        initServices();
        setupToolbar();
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etNickname = findViewById(R.id.etNickname);
        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        rbOther = findViewById(R.id.rbOther);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        btnSave = findViewById(R.id.btnSave);
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
            getSupportActionBar().setTitle("编辑个人信息");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUserData() {
        long userId = sharedPreferences.getLong(KEY_USER_ID, 0);
        String token = sharedPreferences.getString(KEY_TOKEN, "");

        if (userId == 0 || TextUtils.isEmpty(token)) {
            Toast.makeText(this, "用户信息异常，请重新登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 从API获取用户信息
        userApiService.getUserInfo(userId, token, new ApiCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    currentUser = user;
                    displayUserData(user);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, "获取用户信息失败：" + error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void displayUserData(User user) {
        // 设置昵称
        etNickname.setText(user.getNickName());

        // 设置性别
        String gender = user.getSex();
        if ("0".equals(gender)) {
            rbMale.setChecked(true);
        } else if ("1".equals(gender)) {
            rbFemale.setChecked(true);
        } else {
            rbOther.setChecked(true);
        }

        // 设置邮箱
        if (!TextUtils.isEmpty(user.getEmail())) {
            etEmail.setText(user.getEmail());
        }

        // 设置手机号
        if (!TextUtils.isEmpty(user.getPhonenumber())) {
            etPhone.setText(user.getPhonenumber());
        }
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveUserInfo());
        btnChangePassword.setOnClickListener(v -> openChangePasswordDialog());
    }

    private void saveUserInfo() {
        if (!validateInput()) {
            return;
        }

        String nickname = etNickname.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String gender = getSelectedGender();

        // 直接调用更新接口，让后端处理所有验证逻辑
        updateUserProfile(nickname, gender, email, phone);
    }

    private boolean validateInput() {
        String nickname = etNickname.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // 昵称不能为空
        if (TextUtils.isEmpty(nickname)) {
            etNickname.setError("昵称不能为空");
            etNickname.requestFocus();
            return false;
        }

        // 昵称长度检查
        if (nickname.length() < 2 || nickname.length() > 20) {
            etNickname.setError("昵称长度应在2-20个字符之间");
            etNickname.requestFocus();
            return false;
        }

        // 邮箱格式检查（如果不为空）
        if (!TextUtils.isEmpty(email) && !Pattern.matches(EMAIL_PATTERN, email)) {
            etEmail.setError("邮箱格式不正确");
            etEmail.requestFocus();
            return false;
        }

        // 手机号格式检查（如果不为空）
        if (!TextUtils.isEmpty(phone) && !Pattern.matches(PHONE_PATTERN, phone)) {
            etPhone.setError("手机号格式不正确");
            etPhone.requestFocus();
            return false;
        }

        return true;
    }

    private String getSelectedGender() {
        int selectedId = rgGender.getCheckedRadioButtonId();
        if (selectedId == rbMale.getId()) {
            return "0";
        } else if (selectedId == rbFemale.getId()) {
            return "1";
        } else {
            return "2";
        }
    }

    private void updateUserProfile(String nickname, String gender, String email, String phone) {
        String token = sharedPreferences.getString(KEY_TOKEN, "");

        // 创建更新的用户对象
        User updateUser = new User();
        updateUser.setUserId(currentUser.getUserId());
        updateUser.setNickName(nickname);
        updateUser.setSex(gender);
        updateUser.setEmail(TextUtils.isEmpty(email) ? null : email);
        updateUser.setPhonenumber(TextUtils.isEmpty(phone) ? null : phone);

        // 设置按钮状态
        btnSave.setEnabled(false);
        btnSave.setText("保存中...");

        userApiService.updateProfile(updateUser, token, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, "个人信息更新成功", Toast.LENGTH_SHORT).show();

                    // 更新本地用户信息
                    currentUser.setNickName(nickname);
                    currentUser.setSex(gender);
                    currentUser.setEmail(TextUtils.isEmpty(email) ? null : email);
                    currentUser.setPhonenumber(TextUtils.isEmpty(phone) ? null : phone);

                    // 可以选择将更新后的用户信息保存到SharedPreferences
                    saveUserInfoToPrefs(currentUser);

                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, "更新失败：" + error, Toast.LENGTH_SHORT).show();

                    // 恢复按钮状态
                    btnSave.setEnabled(true);
                    btnSave.setText("保存");

                    // 根据错误信息设置对应的错误提示
                    if (error.contains("邮箱")) {
                        etEmail.setError("该邮箱已被使用");
                        etEmail.requestFocus();
                    } else if (error.contains("手机号")) {
                        etPhone.setError("该手机号已被使用");
                        etPhone.requestFocus();
                    }
                });
            }
        });
    }

    private void saveUserInfoToPrefs(User user) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_nickname", user.getNickName());
        editor.putString("user_sex", user.getSex());
        editor.putString("user_email", user.getEmail());
        editor.putString("user_phone", user.getPhonenumber());
        editor.apply();
    }

    private void openChangePasswordDialog() {
        Intent intent = new Intent(this, ChangePasswordActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userApiService != null) {
            userApiService.cancelAllRequests();
        }
    }
}