package com.app.gameform.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.app.gameform.R;
import com.app.gameform.domain.User;
import com.app.gameform.network.ApiCallback;
import com.app.gameform.network.UserApiService;
import com.app.gameform.utils.BottomNavigationHelper;
import com.app.gameform.utils.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {
    private BottomNavigationHelper bottomNavigationHelper;
    private SharedPreferences sharedPreferences;
    private UserApiService userApiService;

    // UI组件
    private LinearLayout loginPromptLayout;
    private LinearLayout userInfoLayout;
    private TextView tvLoginPrompt;
    private TextView tvUsername;
    private TextView tvNickname;
    private TextView tvEmail;
    private TextView tvPhone;
    private TextView tvJoinDate;
    private CardView cvLogout;
    private CardView cvEditProfile;
    private ImageView ivEdit;
    private CircleImageView ivAvatar;

    // 常量
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final int REQUEST_CODE_EDIT_PROFILE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        initServices();
        setupBottomNavigation();
        checkLoginStatus();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        loginPromptLayout = findViewById(R.id.loginPromptLayout);
        userInfoLayout = findViewById(R.id.userInfoLayout);
        tvLoginPrompt = findViewById(R.id.tvLoginPrompt);
        tvUsername = findViewById(R.id.tvUsername);
        tvNickname = findViewById(R.id.tvNickname);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvJoinDate = findViewById(R.id.tvJoinDate);
        cvLogout = findViewById(R.id.cvLogout);
        cvEditProfile = findViewById(R.id.cvEditProfile);
        ivEdit = findViewById(R.id.ivEdit);
        ivAvatar = findViewById(R.id.ivAvatar);

        // 设置登录提示点击事件
        loginPromptLayout.setOnClickListener(v -> navigateToLogin());

        // 设置退出登录点击事件
        cvLogout.setOnClickListener(v -> logout());

        // 设置编辑按钮点击事件
        cvEditProfile.setOnClickListener(v -> navigateToEditProfile());
        ivEdit.setOnClickListener(v -> navigateToEditProfile());
    }

    /**
     * 初始化服务
     */
    private void initServices() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userApiService = UserApiService.getInstance();
    }

    /**
     * 设置底部导航栏
     */
    private void setupBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper(this, findViewById(R.id.bottomNavigationInclude));
        bottomNavigationHelper.setSelectedItem(BottomNavigationHelper.NavigationItem.PROFILE);
    }

    /**
     * 检查登录状态
     */
    private void checkLoginStatus() {
        String token = sharedPreferences.getString(KEY_TOKEN, "");

        if (TextUtils.isEmpty(token)) {
            // 未登录状态
            showLoginPrompt();
        } else {
            // 已登录状态，加载用户信息
            loadUserInfo();
        }
    }

    /**
     * 显示登录提示
     */
    private void showLoginPrompt() {
        loginPromptLayout.setVisibility(View.VISIBLE);
        userInfoLayout.setVisibility(View.GONE);
    }

    /**
     * 显示用户信息
     */
    private void showUserInfo() {
        loginPromptLayout.setVisibility(View.GONE);
        userInfoLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 加载用户信息
     */
    private void loadUserInfo() {
        long userId = sharedPreferences.getLong(KEY_USER_ID, 0);
        String username = sharedPreferences.getString(KEY_USERNAME, "");
        String token = sharedPreferences.getString(KEY_TOKEN, "");

        if (userId == 0) {
            // 用户ID无效，显示登录提示
            showLoginPrompt();
            return;
        }

        // 检查token是否存在
        if (TextUtils.isEmpty(token)) {
            showLoginPrompt();
            return;
        }

        // 从API获取完整用户信息
        userApiService.getUserInfo(userId, token, new ApiCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    displayUserInfo(user);
                    showUserInfo();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // 如果获取用户信息失败，可能是token过期，显示登录提示
                    Toast.makeText(ProfileActivity.this, "获取用户信息失败，请重新登录", Toast.LENGTH_SHORT).show();
                    clearUserData();
                    showLoginPrompt();
                });
            }
        });
    }

    /**
     * 显示用户信息
     */
    private void displayUserInfo(User user) {
        // 修改：昵称在上面，用户名在下面
        tvNickname.setText(user.getNickName());
        tvUsername.setText(user.getUserName());

        // 邮箱
        if (!TextUtils.isEmpty(user.getEmail())) {
            tvEmail.setText(user.getEmail());
            tvEmail.setVisibility(View.VISIBLE);
        } else {
            tvEmail.setText("未绑定");
            tvEmail.setVisibility(View.VISIBLE);
        }

        // 手机号
        if (!TextUtils.isEmpty(user.getPhonenumber())) {
            tvPhone.setText(user.getPhonenumber());
            tvPhone.setVisibility(View.VISIBLE);
        } else {
            tvPhone.setText("未绑定");
            tvPhone.setVisibility(View.VISIBLE);
        }

        // 注册时间
        if (user.getCreateTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dateStr = sdf.format(user.getCreateTime());
            tvJoinDate.setText(dateStr);
        }
        else {
            tvJoinDate.setText("未知");
        }

        // 加载用户头像
        loadUserAvatar(user.getAvatar());
    }

    /**
     * 加载用户头像的方法
     */
    private void loadUserAvatar(String avatarUrl) {
        if (!TextUtils.isEmpty(avatarUrl)) {
            // 使用工具类加载用户头像
            ImageUtils.loadUserAvatar(this, ivAvatar, avatarUrl);
        } else {
            // 设置默认头像
            ivAvatar.setImageResource(R.drawable.ic_default_avatar);
        }
    }

    /**
     * 跳转到登录页面
     */
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    /**
     * 跳转到编辑个人信息页面
     */
    private void navigateToEditProfile() {
        Intent intent = new Intent(this, EditProfileActivity.class);
        startActivityForResult(intent, REQUEST_CODE_EDIT_PROFILE);
    }

    /**
     * 退出登录
     */
    private void logout() {
        // 清除本地存储的用户数据
        clearUserData();

        // 显示登录提示
        showLoginPrompt();

        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
    }

    /**
     * 清除用户数据
     */
    private void clearUserData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_REFRESH_TOKEN);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USERNAME);
        editor.apply();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_PROFILE && resultCode == RESULT_OK) {
            // 编辑个人信息成功后，重新加载用户信息
            loadUserInfo();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 页面重新显示时检查登录状态
        checkLoginStatus();
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