package com.app.gameform.Activity;

import android.content.Intent;
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
import com.app.gameform.manager.UserManager;

public class EditProfileActivity extends AppCompatActivity {
    private UserManager userManager;
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
        userManager = UserManager.getInstance(this);
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
        userManager.getUserInfo(new UserManager.UserInfoCallback() {
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
                    Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show();
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

        btnSave.setEnabled(false);
        btnSave.setText("保存中...");

        userManager.updateProfile(nickname, gender, email, phone, new UserManager.UserOperationCallback() {
            @Override
            public void onSuccess(String message,String token) {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                    currentUser.setNickName(nickname);
                    currentUser.setSex(gender);
                    currentUser.setEmail(TextUtils.isEmpty(email) ? null : email);
                    currentUser.setPhonenumber(TextUtils.isEmpty(phone) ? null : phone);
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("保存");
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

    private boolean validateInput() {
        String nickname = etNickname.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // 昵称不能为空
        if (!userManager.validateNickname(nickname, etNickname)) {
            return false;
        }

        // 邮箱格式检查（如果不为空）
        if (!userManager.validateEmail(email, etEmail)) {
            return false;
        }

        // 手机号格式检查（如果不为空）
        if (!userManager.validatePhone(phone, etPhone)) {
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

    private void openChangePasswordDialog() {
        Intent intent = new Intent(this, ChangePasswordActivity.class);
        startActivity(intent);
    }
}