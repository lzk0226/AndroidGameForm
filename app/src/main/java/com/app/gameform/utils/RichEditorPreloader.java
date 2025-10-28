package com.app.gameform.utils;

import android.app.Activity;
import android.content.Context;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.util.Log;

/**
 * 富文本编辑器预加载器
 * 在后台预创建 WebView，避免首次打开 NewPostActivity 时卡顿
 */
public class RichEditorPreloader {

    private static final String TAG = "RichEditorPreloader";
    private static RichEditorPreloader instance;
    private WebView preloadedWebView;
    private boolean isWebViewReady = false;

    private RichEditorPreloader() {}

    public static RichEditorPreloader getInstance() {
        if (instance == null) {
            synchronized (RichEditorPreloader.class) {
                if (instance == null) {
                    instance = new RichEditorPreloader();
                }
            }
        }
        return instance;
    }

    /**
     * 预加载 WebView（在 HomeActivity 或 MainActivity 中调用）
     */
    public void preloadWebView(Context context) {
        if (isWebViewReady) {
            Log.d(TAG, "WebView 已经预加载，跳过");
            return;
        }

        // 必须在主线程创建
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.runOnUiThread(() -> {
                try {
                    preloadedWebView = new WebView(context.getApplicationContext());
                    setupWebView(preloadedWebView);

                    // 预加载富文本编辑器 HTML
                    String editorHtml = createRichTextEditorHtml();
                    preloadedWebView.loadDataWithBaseURL(
                            "file:///android_asset/",
                            editorHtml,
                            "text/html",
                            "UTF-8",
                            null
                    );

                    isWebViewReady = true;
                    Log.d(TAG, "富文本编辑器 WebView 预加载完成");
                } catch (Exception e) {
                    Log.e(TAG, "WebView 预加载失败", e);
                }
            });
        }
    }

    /**
     * 获取预加载的 WebView
     */
    public WebView getPreloadedWebView() {
        if (isWebViewReady && preloadedWebView != null) {
            WebView temp = preloadedWebView;
            preloadedWebView = null;
            isWebViewReady = false;
            return temp;
        }
        return null;
    }

    /**
     * 配置 WebView
     */
    private void setupWebView(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
    }

    /**
     * 创建富文本编辑器 HTML
     */
    private String createRichTextEditorHtml() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <style>\n" +
                "        body {\n" +
                "            margin: 0;\n" +
                "            padding: 12px;\n" +
                "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n" +
                "            font-size: 16px;\n" +
                "            line-height: 1.6;\n" +
                "            color: #333333;\n" +
                "            min-height: 300px;\n" +
                "        }\n" +
                "        #editor {\n" +
                "            min-height: 300px;\n" +
                "            outline: none;\n" +
                "            border: none;\n" +
                "        }\n" +
                "        .placeholder {\n" +
                "            color: #999999;\n" +
                "        }\n" +
                "        img {\n" +
                "            max-width: 100%;\n" +
                "            height: auto;\n" +
                "            margin: 8px 0;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"editor\" contenteditable=\"true\" class=\"placeholder\" data-placeholder=\"请输入内容...\"></div>\n" +
                "    \n" +
                "    <script>\n" +
                "        const editor = document.getElementById('editor');\n" +
                "        \n" +
                "        editor.addEventListener('focus', function() {\n" +
                "            if (this.classList.contains('placeholder')) {\n" +
                "                this.innerHTML = '';\n" +
                "                this.classList.remove('placeholder');\n" +
                "            }\n" +
                "        });\n" +
                "        \n" +
                "        editor.addEventListener('blur', function() {\n" +
                "            if (this.innerHTML === '') {\n" +
                "                this.classList.add('placeholder');\n" +
                "                this.innerHTML = '请输入内容...';\n" +
                "            }\n" +
                "        });\n" +
                "        \n" +
                "        editor.addEventListener('input', function() {\n" +
                "            if (typeof Android !== 'undefined') {\n" +
                "                Android.onContentChanged(this.innerHTML);\n" +
                "            }\n" +
                "        });\n" +
                "        \n" +
                "        function insertImage(imageUrl) {\n" +
                "            const img = document.createElement('img');\n" +
                "            img.src = imageUrl;\n" +
                "            img.style.maxWidth = '100%';\n" +
                "            \n" +
                "            const selection = window.getSelection();\n" +
                "            if (selection.rangeCount > 0) {\n" +
                "                const range = selection.getRangeAt(0);\n" +
                "                range.insertNode(img);\n" +
                "                const br = document.createElement('br');\n" +
                "                range.insertNode(br);\n" +
                "                range.setStartAfter(br);\n" +
                "                range.setEndAfter(br);\n" +
                "                selection.removeAllRanges();\n" +
                "                selection.addRange(range);\n" +
                "            } else {\n" +
                "                editor.appendChild(img);\n" +
                "                editor.appendChild(document.createElement('br'));\n" +
                "            }\n" +
                "            \n" +
                "            if (typeof Android !== 'undefined') {\n" +
                "                Android.onContentChanged(editor.innerHTML);\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        function formatText(tag) {\n" +
                "            document.execCommand(tag, false, null);\n" +
                "            if (typeof Android !== 'undefined') {\n" +
                "                Android.onContentChanged(editor.innerHTML);\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        function getContent() {\n" +
                "            return editor.innerHTML;\n" +
                "        }\n" +
                "        \n" +
                "        function setContent(html) {\n" +
                "            if (html && html !== '请输入内容...') {\n" +
                "                editor.innerHTML = html;\n" +
                "                editor.classList.remove('placeholder');\n" +
                "            }\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
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