package com.app.gameform.utils;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.app.gameform.R;

/**
 * 帖子详情页预加载器
 * 在后台预创建和初始化重量级组件
 */
public class PostDetailPreloader {

    private static PostDetailPreloader instance;
    private WebView preloadedWebView;
    private boolean isWebViewReady = false;

    private PostDetailPreloader() {}

    public static PostDetailPreloader getInstance() {
        if (instance == null) {
            synchronized (PostDetailPreloader.class) {
                if (instance == null) {
                    instance = new PostDetailPreloader();
                }
            }
        }
        return instance;
    }

    /**
     * 在应用启动或 HomeActivity 创建时预初始化
     */
    public void preloadComponents(Context context) {
        if (isWebViewReady) {
            return;
        }

        new Thread(() -> {
            try {
                // 在后台线程预创建 WebView
                Activity activity = (Activity) context;
                activity.runOnUiThread(() -> {
                    try {
                        preloadedWebView = new WebView(context.getApplicationContext());
                        setupWebView(preloadedWebView);
                        isWebViewReady = true;
                        android.util.Log.d("PostDetailPreloader", "WebView 预加载完成");
                    } catch (Exception e) {
                        android.util.Log.e("PostDetailPreloader", "WebView 预加载失败", e);
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("PostDetailPreloader", "预加载失败", e);
            }
        }).start();
    }

    /**
     * 配置 WebView
     */
    private void setupWebView(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webView.setBackgroundColor(0x00000000);
    }

    /**
     * 获取预加载的 WebView
     */
    public WebView getPreloadedWebView() {
        if (isWebViewReady && preloadedWebView != null) {
            WebView temp = preloadedWebView;
            // 标记为已使用，下次需要重新创建
            preloadedWebView = null;
            isWebViewReady = false;
            return temp;
        }
        return null;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (preloadedWebView != null) {
            preloadedWebView.destroy();
            preloadedWebView = null;
        }
        isWebViewReady = false;
    }
}