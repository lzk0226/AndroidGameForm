package com.app.gameform.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.gameform.Activity.Home.HomeActivity;
import com.app.gameform.R;
import com.app.gameform.manager.SharedPrefManager;
import com.app.gameform.utils.PostDetailPreloader;
import com.app.gameform.utils.RichEditorPreloader;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int SPLASH_DELAY = 2000; // 启动页延迟时间（毫秒）
    private boolean hasNavigated = false; // 防止重复跳转

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "启动页创建");

        PostDetailPreloader.getInstance().preloadComponents(this);
        RichEditorPreloader.getInstance().preloadWebView(this);
        // ⭐ 启动时自动刷新 Token
        autoRefreshTokenAndNavigate();
    }

    /**
     * 自动刷新 Token 并跳转到主页
     */
    private void autoRefreshTokenAndNavigate() {
        SharedPrefManager sharedPrefManager = SharedPrefManager.getInstance(this);

        // 检查是否已登录
        if (!sharedPrefManager.isLoggedIn()) {
            Log.d(TAG, "用户未登录，直接跳转到主页");
            navigateToHome();
            return;
        }

        Log.d(TAG, "用户已登录，开始刷新 Token...");

        // 尝试刷新 Token
        sharedPrefManager.autoRefreshTokenOnStartup(new SharedPrefManager.TokenRefreshCallback() {
            @Override
            public void onRefreshSuccess(String newAccessToken, String newRefreshToken) {
                Log.d(TAG, "✅ Token 刷新成功");

                runOnUiThread(() -> {
                    // Token 刷新成功，延迟跳转
                    navigateToHomeWithDelay();
                });
            }

            @Override
            public void onRefreshFailed(String error) {
                Log.e(TAG, "❌ Token 刷新失败: " + error);

                runOnUiThread(() -> {
                    // Token 刷新失败，清除用户数据
                    sharedPrefManager.clearUserData();

                    // 显示提示信息（可选）
                    // Toast.makeText(MainActivity.this, "登录已过期", Toast.LENGTH_SHORT).show();

                    // 延迟跳转到主页，用户可以在主页重新登录
                    navigateToHomeWithDelay();
                });
            }

            @Override
            public void onRefreshSkipped() {
                Log.d(TAG, "⏭️ 跳过 Token 刷新（未登录）");

                runOnUiThread(() -> {
                    navigateToHomeWithDelay();
                });
            }
        });
    }

    /**
     * 延迟跳转到主页（保持启动页显示时间）
     */
    private void navigateToHomeWithDelay() {
        // 计算已经过去的时间
        new Handler().postDelayed(() -> {
            navigateToHome();
        }, SPLASH_DELAY);
    }

    /**
     * 立即跳转到主页
     */
    private void navigateToHome() {
        if (hasNavigated) {
            Log.w(TAG, "已经跳转过了，跳过重复跳转");
            return;
        }

        hasNavigated = true;
        Log.d(TAG, "跳转到主页");

        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "启动页销毁");
    }
}