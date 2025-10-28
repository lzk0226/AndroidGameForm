package com.app.gameform.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.adapter.ImageAdapter;
import com.app.gameform.domain.Draft;
import com.app.gameform.domain.Game;
import com.app.gameform.domain.GameType;
import com.app.gameform.domain.Section;
import com.app.gameform.manager.DraftManager;
import com.app.gameform.network.ApiConstants;
import com.app.gameform.utils.ImageUploadHelper;
import com.app.gameform.manager.SharedPrefManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 发帖页面 - 优化版：异步加载，避免 ANR
 */
public class NewPostActivity extends AppCompatActivity {

    private static final String TAG = "NewPostActivity";

    private TextView btnClose, btnPublish, tvSelectedTopic, btnInsertImage, btnBold, btnItalic;
    private EditText etTitle;
    private WebView webViewContent;
    private RecyclerView rvSelectedImages;

    private List<Uri> selectedImageUris = new ArrayList<>();
    private ImageAdapter imageAdapter;

    // 话题选择相关
    private List<GameType> gameTypeList = new ArrayList<>();
    private List<Game> gameList = new ArrayList<>();
    private List<Section> sectionList = new ArrayList<>();
    private Section selectedSection;

    // 数据加载状态
    private boolean isDataLoading = false;
    private boolean isDataLoaded = false;

    // 图片选择器
    private ActivityResultLauncher<String> pickImageLauncher;

    // 草稿管理器
    private DraftManager draftManager;

    // 当前编辑的草稿ID
    private Integer editingDraftId = null;

    // 内容变化标志
    private boolean hasContentChanged = false;

    // 富文本编辑器相关
    private String currentHtmlContent = "";
    private List<String> uploadedImageUrls = new ArrayList<>();

    // 优化 OkHttpClient 配置
    private OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    // 图片上传工具类
    private ImageUploadHelper imageUploadHelper;

    // 主线程 Handler
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newpost);

        initViews();
        initImagePicker();
        initRichTextEditorAsync(); // ⭐ 异步初始化 WebView
        initEvents();
        initDraftManager();
        initImageUploadHelper();
        handleIntent();

        // ⭐ 异步加载话题数据
        loadTopicDataAsync();
    }

    private void initViews() {
        btnClose = findViewById(R.id.btn_close);
        btnPublish = findViewById(R.id.btn_publish);
        tvSelectedTopic = findViewById(R.id.tv_selected_topic);
        btnInsertImage = findViewById(R.id.btn_insert_image);
        etTitle = findViewById(R.id.et_title);
        rvSelectedImages = findViewById(R.id.rv_selected_images);

        // 初始化富文本编辑器
        webViewContent = findViewById(R.id.web_view_content);

        // 初始化格式按钮
        btnBold = findViewById(R.id.btn_bold);
        btnItalic = findViewById(R.id.btn_italic);

        // 图片预览RecyclerView
        rvSelectedImages.setLayoutManager(new GridLayoutManager(this, 3));
        imageAdapter = new ImageAdapter(selectedImageUris);
        imageAdapter.setOnImageDeleteListener(position -> {
            hasContentChanged = true;
            if (selectedImageUris.isEmpty()) {
                rvSelectedImages.setVisibility(View.GONE);
            }
        });
        rvSelectedImages.setAdapter(imageAdapter);
    }

    /**
     * ⭐ 异步初始化富文本编辑器（避免阻塞主线程）
     */
    private void initRichTextEditorAsync() {
        // 使用 postDelayed 将 WebView 初始化延迟到下一帧
        webViewContent.postDelayed(() -> {
            try {
                WebSettings webSettings = webViewContent.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setAllowFileAccess(true);
                webSettings.setAllowContentAccess(true);
                webSettings.setUseWideViewPort(true);
                webSettings.setLoadWithOverviewMode(true);

                webViewContent.addJavascriptInterface(new WebAppInterface(), "Android");
                webViewContent.setWebChromeClient(new WebChromeClient());

                // 加载富文本编辑器 HTML
                String editorHtml = createRichTextEditorHtml();
                webViewContent.loadDataWithBaseURL(
                        "file:///android_asset/",
                        editorHtml,
                        "text/html",
                        "UTF-8",
                        null
                );

                Log.d(TAG, "WebView 初始化完成");
            } catch (Exception e) {
                Log.e(TAG, "WebView 初始化失败", e);
                Toast.makeText(this, "编辑器加载失败", Toast.LENGTH_SHORT).show();
            }
        }, 50); // 延迟 50ms，让界面先渲染
    }

    /**
     * 创建富文本编辑器HTML
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
     * WebView与Android的通信接口
     */
    private class WebAppInterface {
        @JavascriptInterface
        public void onContentChanged(String html) {
            runOnUiThread(() -> {
                currentHtmlContent = html;
                hasContentChanged = true;
                updatePublishButton();
            });
        }
    }

    private void initImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUris.add(uri);
                        rvSelectedImages.setVisibility(View.VISIBLE);
                        imageAdapter.notifyDataSetChanged();
                        uploadAndInsertImage(uri);
                    }
                }
        );
    }

    private void initEvents() {
        btnClose.setOnClickListener(v -> onBackPressed());

        btnInsertImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnBold.setOnClickListener(v ->
                webViewContent.evaluateJavascript("formatText('bold');", null));

        btnItalic.setOnClickListener(v ->
                webViewContent.evaluateJavascript("formatText('italic');", null));

        // 话题选择点击事件
        findViewById(R.id.layout_topic_selector).setOnClickListener(v -> showTopicSelector());

        etTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasContentChanged = true;
                updatePublishButton();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnPublish.setOnClickListener(v -> publishPost());
    }

    private void initDraftManager() {
        draftManager = DraftManager.getInstance(this);
    }

    private void initImageUploadHelper() {
        imageUploadHelper = new ImageUploadHelper(this);
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            int draftId = intent.getIntExtra("draft_id", -1);
            if (draftId != -1) {
                editingDraftId = draftId;
                // 草稿加载等待数据加载完成后执行
            }
        }
    }

    /**
     * 更新发布按钮状态
     */
    private void updatePublishButton() {
        boolean hasTitle = etTitle.getText().length() > 0;
        boolean hasContent = !currentHtmlContent.isEmpty() &&
                !currentHtmlContent.equals("请输入内容...") &&
                !currentHtmlContent.contains("placeholder");
        boolean hasSection = selectedSection != null;
        btnPublish.setEnabled(hasTitle && hasContent && hasSection);
    }

    /**
     * 显示话题选择对话框
     */
    private void showTopicSelector() {
        // 检查数据是否已加载
        if (!isDataLoaded) {
            if (isDataLoading) {
                Toast.makeText(this, "数据加载中，请稍候...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "数据加载失败，请重试", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        TopicSelectorDialog dialog = new TopicSelectorDialog(this);
        dialog.setOnTopicSelectedListener((gameType, game, section) -> {
            selectedSection = section;
            if (section != null) {
                String topicText = String.format("#%s-%s-%s",
                        gameType.getTypeName(),
                        game.getGameName(),
                        section.getSectionName());
                tvSelectedTopic.setText(topicText);
                tvSelectedTopic.setTextColor(0xFF333333);
                hasContentChanged = true;
            }
            updatePublishButton();
        });

        dialog.show();

        // 设置数据
        dialog.setData(gameTypeList, gameList, sectionList);
    }

    // ==================== ⭐ 异步加载话题数据 ====================

    /**
     * 异步加载话题数据（在后台线程执行）
     */
    private void loadTopicDataAsync() {
        String token = getValidToken();
        if (token == null) {
            runOnUiThread(() ->
                    Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        isDataLoading = true;

        // 在后台线程并行加载所有数据
        new Thread(() -> {
            try {
                // ⭐ 使用 CountDownLatch 等待所有请求完成
                final java.util.concurrent.CountDownLatch latch =
                        new java.util.concurrent.CountDownLatch(3);

                // 加载游戏类型
                loadGameTypesAsync(token, latch);

                // 加载游戏列表
                loadGamesAsync(token, latch);

                // 加载板块列表
                loadSectionsAsync(token, latch);

                // 等待所有请求完成（最多等待10秒）
                latch.await(10, java.util.concurrent.TimeUnit.SECONDS);

                // 所有数据加载完成
                runOnUiThread(() -> {
                    isDataLoading = false;
                    isDataLoaded = true;
                    Log.d(TAG, "话题数据加载完成 - 类型:" + gameTypeList.size() +
                            ", 游戏:" + gameList.size() + ", 板块:" + sectionList.size());

                    // 如果是编辑草稿，现在可以加载草稿内容了
                    if (editingDraftId != null) {
                        checkAndLoadDraftAfterDataLoaded();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "加载话题数据异常", e);
                runOnUiThread(() -> {
                    isDataLoading = false;
                    Toast.makeText(this, "数据加载失败", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void loadGameTypesAsync(String token, java.util.concurrent.CountDownLatch latch) {
        Request request = new Request.Builder()
                .url(ApiConstants.GET_ALL_GAME_TYPES)
                .addHeader("Authorization", token)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "加载游戏类型失败: " + e.getMessage());
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String respStr = response.body().string();
                    JSONObject json = new JSONObject(respStr);

                    if (json.optInt("code") == 200) {
                        JSONArray dataArray = json.optJSONArray("data");
                        if (dataArray != null) {
                            List<GameType> tempList = new ArrayList<>();
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject item = dataArray.getJSONObject(i);
                                GameType gameType = new GameType();
                                gameType.setTypeId(item.optInt("typeId"));
                                gameType.setTypeName(item.optString("typeName"));
                                tempList.add(gameType);
                            }
                            gameTypeList.clear();
                            gameTypeList.addAll(tempList);
                            Log.d(TAG, "游戏类型加载成功: " + gameTypeList.size());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析游戏类型失败", e);
                } finally {
                    latch.countDown();
                }
            }
        });
    }

    private void loadGamesAsync(String token, java.util.concurrent.CountDownLatch latch) {
        Request request = new Request.Builder()
                .url(ApiConstants.GET_ALL_GAMES)
                .addHeader("Authorization", token)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "加载游戏失败: " + e.getMessage());
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String respStr = response.body().string();
                    JSONObject json = new JSONObject(respStr);

                    if (json.optInt("code") == 200) {
                        JSONArray dataArray = json.optJSONArray("data");
                        if (dataArray != null) {
                            List<Game> tempList = new ArrayList<>();
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject item = dataArray.getJSONObject(i);
                                Game game = new Game();
                                game.setGameId(item.optInt("gameId"));
                                game.setGameName(item.optString("gameName"));
                                game.setGameTypeId(item.optInt("gameTypeId"));
                                game.setGameDescription(item.optString("gameDescription"));
                                game.setGameIcon(item.optString("gameIcon"));
                                tempList.add(game);
                            }
                            gameList.clear();
                            gameList.addAll(tempList);
                            Log.d(TAG, "游戏列表加载成功: " + gameList.size());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析游戏列表失败", e);
                } finally {
                    latch.countDown();
                }
            }
        });
    }

    private void loadSectionsAsync(String token, java.util.concurrent.CountDownLatch latch) {
        Request request = new Request.Builder()
                .url(ApiConstants.GET_ALL_SECTIONS)
                .addHeader("Authorization", token)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "加载板块失败: " + e.getMessage());
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String respStr = response.body().string();
                    JSONObject json = new JSONObject(respStr);

                    if (json.optInt("code") == 200) {
                        JSONArray dataArray = json.optJSONArray("data");
                        if (dataArray != null) {
                            List<Section> tempList = new ArrayList<>();
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject item = dataArray.getJSONObject(i);
                                Section section = new Section();
                                section.setSectionId(item.optInt("sectionId"));
                                section.setSectionName(item.optString("sectionName"));
                                section.setGameId(item.optInt("gameId"));
                                section.setSectionDescription(item.optString("sectionDescription"));
                                section.setOrderNum(item.optInt("orderNum"));
                                tempList.add(section);
                            }
                            sectionList.clear();
                            sectionList.addAll(tempList);
                            Log.d(TAG, "板块列表加载成功: " + sectionList.size());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析板块列表失败", e);
                } finally {
                    latch.countDown();
                }
            }
        });
    }

    /**
     * 检查数据是否加载完成，如果完成且需要加载草稿，则加载草稿
     */
    private void checkAndLoadDraftAfterDataLoaded() {
        if (!gameTypeList.isEmpty() && !gameList.isEmpty() && !sectionList.isEmpty()) {
            if (editingDraftId != null) {
                loadDraft(editingDraftId);
            }
        }
    }

    /**
     * 加载草稿内容
     */
    private void loadDraft(int draftId) {
        Draft draft = draftManager.getDraftById(draftId);
        if (draft != null) {
            Log.d(TAG, "加载草稿: " + draft.toString());

            runOnUiThread(() -> {
                if (!TextUtils.isEmpty(draft.getDraftTitle())) {
                    etTitle.setText(draft.getDraftTitle());
                }

                if (!TextUtils.isEmpty(draft.getDraftContent())) {
                    currentHtmlContent = draft.getDraftContent();
                    String jsCode = String.format("setContent('%s');",
                            draft.getDraftContent().replace("'", "\\'"));
                    webViewContent.evaluateJavascript(jsCode, null);
                }

                if (draft.getSectionId() != null) {
                    setSelectedSectionFromDraft(draft);
                }

                hasContentChanged = false;
                updatePublishButton();
            });
        }
    }

    /**
     * 从草稿中设置选中的板块
     */
    private void setSelectedSectionFromDraft(Draft draft) {
        try {
            Section targetSection = null;
            for (Section section : sectionList) {
                if (section.getSectionId().equals(draft.getSectionId())) {
                    targetSection = section;
                    break;
                }
            }

            if (targetSection != null) {
                selectedSection = targetSection;

                String gameTypeName = "";
                String gameName = "";

                for (Game game : gameList) {
                    if (game.getGameId().equals(targetSection.getGameId())) {
                        gameName = game.getGameName();

                        for (GameType gameType : gameTypeList) {
                            if (gameType.getTypeId().equals(game.getGameTypeId())) {
                                gameTypeName = gameType.getTypeName();
                                break;
                            }
                        }
                        break;
                    }
                }

                String topicText = String.format("#%s-%s-%s",
                        gameTypeName, gameName, targetSection.getSectionName());
                tvSelectedTopic.setText(topicText);
                tvSelectedTopic.setTextColor(0xFF333333);

                Log.d(TAG, "设置话题成功: " + topicText);
            }
        } catch (Exception e) {
            Log.e(TAG, "设置话题失败", e);
        }

        updatePublishButton();
    }

    /**
     * 获取有效的认证token
     */
    private String getValidToken() {
        String token = SharedPrefManager.getInstance(this).getToken();
        if (token == null || token.isEmpty()) {
            return null;
        }

        if (!token.startsWith("Bearer ")) {
            token = "Bearer " + token;
        }

        return token;
    }

    // ==================== 其他方法保持不变 ====================

    @Override
    public void onBackPressed() {
        if (!checkAndSaveDraft()) {
            super.onBackPressed();
        }
    }

    private boolean checkAndSaveDraft() {
        String title = etTitle.getText().toString().trim();
        boolean hasContent = draftManager.hasContentToSave(title, currentHtmlContent) || selectedSection != null;

        if (hasContent && hasContentChanged) {
            showSaveDraftDialog(title, currentHtmlContent);
            return true;
        }
        return false;
    }

    private void showSaveDraftDialog(String title, String content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("保存草稿");
        builder.setMessage("检测到您有未发布的内容，是否保存为草稿？");

        builder.setPositiveButton("保存", (dialog, which) -> {
            saveDraft(title, content);
            super.onBackPressed();
        });

        builder.setNegativeButton("不保存", (dialog, which) -> super.onBackPressed());

        builder.setNeutralButton("取消", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void saveDraft(String title, String content) {
        try {
            if (TextUtils.isEmpty(title)) {
                title = draftManager.generateDefaultDraftTitle();
            }

            if (TextUtils.isEmpty(content) || content.equals("请输入内容...")) {
                content = "";
            }

            Integer typeId = null;
            Integer gameId = null;
            Integer sectionId = null;
            String sectionName = null;

            if (selectedSection != null) {
                sectionId = selectedSection.getSectionId();
                sectionName = selectedSection.getSectionName();
                gameId = selectedSection.getGameId();

                if (gameId != null) {
                    for (Game game : gameList) {
                        if (game.getGameId().equals(gameId)) {
                            typeId = game.getGameTypeId();
                            break;
                        }
                    }
                }
            }

            if (editingDraftId != null) {
                boolean success = draftManager.updateDraft(
                        editingDraftId, title, content, typeId, gameId, sectionId, sectionName);
                Toast.makeText(this, success ? "草稿已更新" : "草稿更新失败", Toast.LENGTH_SHORT).show();
            } else {
                Draft savedDraft = draftManager.saveDraft(
                        title, content, typeId, gameId, sectionId, sectionName);
                Toast.makeText(this, savedDraft != null ? "草稿已保存" : "草稿保存失败", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "保存草稿失败: " + e.getMessage());
            Toast.makeText(this, "草稿保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadAndInsertImage(Uri imageUri) {
        String token = getValidToken();
        if (token == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "正在上传图片...", Toast.LENGTH_SHORT).show();

        imageUploadHelper.uploadImage(imageUri, token, new ImageUploadHelper.ImageUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                runOnUiThread(() -> {
                    uploadedImageUrls.add(imageUrl);
                    String jsCode = String.format("insertImage('%s');", imageUrl);
                    webViewContent.evaluateJavascript(jsCode, null);
                    Toast.makeText(NewPostActivity.this, "图片上传成功", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(NewPostActivity.this, "图片上传失败: " + error, Toast.LENGTH_SHORT).show();
                    if (!selectedImageUris.isEmpty()) {
                        selectedImageUris.remove(selectedImageUris.size() - 1);
                        imageAdapter.notifyDataSetChanged();
                        if (selectedImageUris.isEmpty()) {
                            rvSelectedImages.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
    }

    private void publishPost() {
        String title = etTitle.getText().toString().trim();
        String content = currentHtmlContent;

        if (title.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }

        String plainText = android.text.Html.fromHtml(content).toString().trim();
        if (plainText.isEmpty() || plainText.equals("请输入内容...")) {
            Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedSection == null) {
            Toast.makeText(this, "请选择话题", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = getValidToken();
        if (token == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        publishPostWithRichText(title, content, token);
    }

    private void publishPostWithRichText(String title, String content, String token) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("postTitle", title);
            jsonBody.put("postContent", content);
            jsonBody.put("sectionId", selectedSection.getSectionId());

            if (!uploadedImageUrls.isEmpty()) {
                jsonBody.put("photo", uploadedImageUrls.get(0));
            }

            RequestBody requestBody = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(ApiConstants.USER_POST)
                    .addHeader("Authorization", token)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            btnPublish.setEnabled(false);
            btnPublish.setText("发布中...");

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        btnPublish.setEnabled(true);
                        btnPublish.setText("发布");
                        Toast.makeText(NewPostActivity.this, "发布失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respStr = response.body().string();

                    runOnUiThread(() -> {
                        btnPublish.setEnabled(true);
                        btnPublish.setText("发布");

                        if (response.isSuccessful()) {
                            try {
                                JSONObject json = new JSONObject(respStr);
                                if (json.optInt("code") == 200) {
                                    Toast.makeText(NewPostActivity.this, "发布成功", Toast.LENGTH_SHORT).show();

                                    if (editingDraftId != null) {
                                        draftManager.deleteDraft(editingDraftId);
                                    }

                                    setResult(Activity.RESULT_OK);
                                    finish();
                                } else {
                                    String errorMsg = json.optString("msg", "发布失败");
                                    Toast.makeText(NewPostActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(NewPostActivity.this, "解析错误", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(NewPostActivity.this, "请求失败: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "构造请求错误: " + e.getMessage());
            Toast.makeText(this, "构造请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnPublish.setEnabled(true);
            btnPublish.setText("发布");
        }
    }

    public static void startForEditDraft(Context context, int draftId) {
        Intent intent = new Intent(context, NewPostActivity.class);
        intent.putExtra("draft_id", draftId);
        context.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webViewContent != null) {
            webViewContent.destroy();
        }
        if (imageUploadHelper != null) {
            imageUploadHelper.destroy();
        }
    }
}