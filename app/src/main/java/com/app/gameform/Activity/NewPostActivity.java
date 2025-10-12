package com.app.gameform.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.app.gameform.utils.SharedPrefManager;

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
 * 发帖页面 - 支持富文本编辑
 */
public class NewPostActivity extends AppCompatActivity {

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

    // 图片选择器
    private ActivityResultLauncher<String> pickImageLauncher;

    // 草稿管理器
    private DraftManager draftManager;

    // 当前编辑的草稿ID（如果是编辑草稿则不为null）
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newpost);

        initViews();
        initImagePicker();
        initRichTextEditor();
        initEvents();
        initDraftManager();
        initImageUploadHelper();
        handleIntent();
        loadTopicData();
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

        // 图片预览RecyclerView（可选功能）
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
     * 初始化富文本编辑器
     */
    private void initRichTextEditor() {
        WebSettings webSettings = webViewContent.getSettings();

        // 启用JavaScript
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        // 设置自适应屏幕
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // 添加JavaScript接口
        webViewContent.addJavascriptInterface(new WebAppInterface(), "Android");

        // 加载富文本编辑器HTML
        String editorHtml = createRichTextEditorHtml();
        webViewContent.loadDataWithBaseURL("file:///android_asset/", editorHtml, "text/html", "UTF-8", null);

        // 设置内容变化监听
        webViewContent.setWebChromeClient(new WebChromeClient());
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
                "        // 处理占位符\n" +
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
                "        // 内容变化时通知Android\n" +
                "        editor.addEventListener('input', function() {\n" +
                "            Android.onContentChanged(this.innerHTML);\n" +
                "        });\n" +
                "        \n" +
                "        // 插入图片\n" +
                "        function insertImage(imageUrl) {\n" +
                "            const img = document.createElement('img');\n" +
                "            img.src = imageUrl;\n" +
                "            img.style.maxWidth = '100%';\n" +
                "            \n" +
                "            // 在光标位置插入图片\n" +
                "            const selection = window.getSelection();\n" +
                "            if (selection.rangeCount > 0) {\n" +
                "                const range = selection.getRangeAt(0);\n" +
                "                range.insertNode(img);\n" +
                "                \n" +
                "                // 在图片后添加换行\n" +
                "                const br = document.createElement('br');\n" +
                "                range.insertNode(br);\n" +
                "                \n" +
                "                // 将光标移动到换行后\n" +
                "                range.setStartAfter(br);\n" +
                "                range.setEndAfter(br);\n" +
                "                selection.removeAllRanges();\n" +
                "                selection.addRange(range);\n" +
                "            } else {\n" +
                "                editor.appendChild(img);\n" +
                "                editor.appendChild(document.createElement('br'));\n" +
                "            }\n" +
                "            \n" +
                "            Android.onContentChanged(editor.innerHTML);\n" +
                "        }\n" +
                "        \n" +
                "        // 格式化文本\n" +
                "        function formatText(tag) {\n" +
                "            document.execCommand(tag, false, null);\n" +
                "            Android.onContentChanged(editor.innerHTML);\n" +
                "        }\n" +
                "        \n" +
                "        // 获取内容\n" +
                "        function getContent() {\n" +
                "            return editor.innerHTML;\n" +
                "        }\n" +
                "        \n" +
                "        // 设置内容\n" +
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
                        // 将图片添加到预览列表（可选）
                        selectedImageUris.add(uri);
                        rvSelectedImages.setVisibility(View.VISIBLE);
                        imageAdapter.notifyDataSetChanged();

                        // 上传图片并插入到富文本编辑器
                        uploadAndInsertImage(uri);
                    }
                }
        );
    }

    private void initEvents() {
        btnClose.setOnClickListener(v -> onBackPressed());

        // 插入图片
        btnInsertImage.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });

        // 粗体按钮
        btnBold.setOnClickListener(v -> {
            webViewContent.evaluateJavascript("formatText('bold');", null);
        });

        // 斜体按钮
        btnItalic.setOnClickListener(v -> {
            webViewContent.evaluateJavascript("formatText('italic');", null);
        });

        // 话题选择点击事件
        findViewById(R.id.layout_topic_selector).setOnClickListener(v -> showTopicSelector());

        // 标题输入监听
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

    /**
     * 初始化草稿管理器
     */
    private void initDraftManager() {
        draftManager = DraftManager.getInstance(this);
    }

    /**
     * 初始化图片上传工具类
     */
    private void initImageUploadHelper() {
        imageUploadHelper = new ImageUploadHelper(this);
    }

    /**
     * 处理Intent参数（用于草稿编辑）
     */
    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            // 检查是否是编辑草稿
            int draftId = intent.getIntExtra("draft_id", -1);
            if (draftId != -1) {
                editingDraftId = draftId;
                // 数据加载完成后再设置草稿内容
            }
        }
    }

    /**
     * 加载草稿内容
     */
    private void loadDraft(int draftId) {
        Draft draft = draftManager.getDraftById(draftId);
        if (draft != null) {
            Log.d("LoadDraft", "加载草稿: " + draft.toString());

            // 设置标题
            if (!TextUtils.isEmpty(draft.getDraftTitle())) {
                etTitle.setText(draft.getDraftTitle());
            }

            // 设置富文本内容
            if (!TextUtils.isEmpty(draft.getDraftContent())) {
                currentHtmlContent = draft.getDraftContent();
                String jsCode = String.format("setContent('%s');",
                        draft.getDraftContent().replace("'", "\\'"));
                webViewContent.evaluateJavascript(jsCode, null);
            }

            // 设置选中的版块
            if (draft.getSectionId() != null) {
                setSelectedSectionFromDraft(draft);
            }

            hasContentChanged = false;
            updatePublishButton();
        }
    }

    /**
     * 从草稿中设置选中的版块
     */
    private void setSelectedSectionFromDraft(Draft draft) {
        // 如果数据还没加载完成，等待数据加载后再设置
        if (sectionList.isEmpty() || gameList.isEmpty() || gameTypeList.isEmpty()) {
            // 使用handler延迟执行
            findViewById(R.id.et_title).postDelayed(() -> {
                setSelectedSectionFromDraft(draft);
            }, 500);
            return;
        }

        try {
            // 根据sectionId查找对应的Section
            Section targetSection = null;
            for (Section section : sectionList) {
                if (section.getSectionId().equals(draft.getSectionId())) {
                    targetSection = section;
                    break;
                }
            }

            if (targetSection != null) {
                selectedSection = targetSection;

                // 构建显示文本
                String gameTypeName = "";
                String gameName = "";

                // 根据gameId查找游戏信息
                for (Game game : gameList) {
                    if (game.getGameId().equals(targetSection.getGameId())) {
                        gameName = game.getGameName();

                        // 根据gameTypeId查找游戏类型
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

                Log.d("LoadDraft", "设置话题成功: " + topicText);
            } else {
                Log.w("LoadDraft", "未找到对应的Section: " + draft.getSectionId());
            }
        } catch (Exception e) {
            Log.e("LoadDraft", "设置话题失败: " + e.getMessage());
        }

        updatePublishButton();
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
        if (gameTypeList.isEmpty()) {
            Toast.makeText(this, "数据加载中，请稍候...", Toast.LENGTH_SHORT).show();
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

        // 显示对话框
        dialog.show();

        // 在对话框显示后设置数据，确保适配器已初始化
        dialog.setData(gameTypeList, gameList, sectionList);
    }

    /**
     * 重写返回按钮处理
     */
    @Override
    public void onBackPressed() {
        if (!checkAndSaveDraft()) {
            // 没有草稿需要保存，直接走默认返回逻辑
            super.onBackPressed();
        }
    }

    /**
     * 检查并保存草稿
     * @return 是否拦截返回（true=拦截，false=继续返回）
     */
    private boolean checkAndSaveDraft() {
        String title = etTitle.getText().toString().trim();

        // 草稿保存时不考虑图片，只保存文本内容
        boolean hasContent = draftManager.hasContentToSave(title, currentHtmlContent) || selectedSection != null;

        if (hasContent && hasContentChanged) {
            showSaveDraftDialog(title, currentHtmlContent);
            return true; // 拦截返回
        }
        return false; // 不拦截
    }

    /**
     * 显示保存草稿确认对话框
     */
    private void showSaveDraftDialog(String title, String content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("保存草稿");
        builder.setMessage("检测到您有未发布的内容，是否保存为草稿？");

        builder.setPositiveButton("保存", (dialog, which) -> {
            saveDraft(title, content);
            super.onBackPressed();
        });

        builder.setNegativeButton("不保存", (dialog, which) -> {
            super.onBackPressed();
        });

        builder.setNeutralButton("取消", (dialog, which) -> {
            // 用户取消，不退出页面
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false); // 防止点击外部区域关闭
        dialog.show();
    }

    /**
     * 保存草稿
     */
    private void saveDraft(String title, String content) {
        try {
            // 处理标题
            if (TextUtils.isEmpty(title)) {
                title = draftManager.generateDefaultDraftTitle();
            }

            // 处理内容（使用富文本内容）
            if (TextUtils.isEmpty(content) || content.equals("请输入内容...")) {
                content = "";
            }

            // 处理话题信息
            Integer typeId = null;
            Integer gameId = null;
            Integer sectionId = null;
            String sectionName = null;

            if (selectedSection != null) {
                sectionId = selectedSection.getSectionId();
                sectionName = selectedSection.getSectionName();
                gameId = selectedSection.getGameId();

                // 通过 gameId 找到 gameTypeId
                if (gameId != null) {
                    for (Game game : gameList) {
                        if (game.getGameId().equals(gameId)) {
                            typeId = game.getGameTypeId();
                            break;
                        }
                    }
                }
            }

            Log.d("SaveDraft", String.format("保存草稿信息 - typeId: %d, gameId: %d, sectionId: %d, sectionName: %s",
                    typeId, gameId, sectionId, sectionName));

            if (editingDraftId != null) {
                // 更新现有草稿（不保存图片）
                boolean success = draftManager.updateDraft(
                        editingDraftId,
                        title,
                        content,
                        typeId,
                        gameId,
                        sectionId,
                        sectionName
                );
                if (success) {
                    Toast.makeText(this, "草稿已更新", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "草稿更新失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                // 保存新草稿（不保存图片）
                Draft savedDraft = draftManager.saveDraft(
                        title,
                        content,
                        typeId,
                        gameId,
                        sectionId,
                        sectionName
                );
                if (savedDraft != null) {
                    Toast.makeText(this, "草稿已保存", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "草稿保存失败", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            Log.e("保存草稿", "保存失败: " + e.getMessage());
            Toast.makeText(this, "草稿保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 获取有效的认证token
     */
    private String getValidToken() {
        String token = SharedPrefManager.getInstance(this).getToken();
        if (token == null || token.isEmpty()) {
            return null;
        }

        // 确保token格式正确，统一处理Bearer前缀
        if (!token.startsWith("Bearer ")) {
            token = "Bearer " + token;
        }

        Log.d("Token验证", "使用的token: " + token.substring(0, Math.min(token.length(), 50)) + "...");
        return token;
    }

    /**
     * 加载话题相关数据
     */
    private void loadTopicData() {
        String token = getValidToken();
        if (token == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 加载游戏类型
        loadGameTypes(token);
        // 加载游戏列表
        loadGames(token);
        // 加载版块列表
        loadSections(token);
    }

    private void loadGameTypes(String token) {
        Request request = new Request.Builder()
                .url(ApiConstants.GET_ALL_GAME_TYPES)
                .addHeader("Authorization", token)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("加载游戏类型", "失败: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(NewPostActivity.this, "加载游戏类型失败", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respStr = response.body().string();
                Log.d("游戏类型响应", "状态码: " + response.code() + ", 响应: " + respStr);

                try {
                    JSONObject json = new JSONObject(respStr);
                    if (json.optInt("code") == 200) {
                        JSONArray dataArray = json.optJSONArray("data");
                        if (dataArray != null) {
                            gameTypeList.clear();
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject item = dataArray.getJSONObject(i);
                                GameType gameType = new GameType();
                                gameType.setTypeId(item.optInt("typeId"));
                                gameType.setTypeName(item.optString("typeName"));
                                gameTypeList.add(gameType);
                            }

                            runOnUiThread(() -> {
                                Log.d("游戏类型", "加载成功，共 " + gameTypeList.size() + " 个类型");
                                // 数据加载完成后检查是否需要加载草稿
                                checkAndLoadDraftAfterDataLoaded();
                            });
                        }
                    } else {
                        Log.e("游戏类型", "服务器返回错误: " + json.optString("msg"));
                        runOnUiThread(() -> {
                            if (response.code() == 401) {
                                Toast.makeText(NewPostActivity.this, "登录已过期，请重新登录", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(NewPostActivity.this, "加载失败: " + json.optString("msg"), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("解析游戏类型", "错误: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(NewPostActivity.this, "数据解析失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void loadGames(String token) {
        Request request = new Request.Builder()
                .url(ApiConstants.GET_ALL_GAMES)
                .addHeader("Authorization", token)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("加载游戏", "失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respStr = response.body().string();
                Log.d("游戏列表响应", "状态码: " + response.code() + ", 响应: " + respStr);

                try {
                    JSONObject json = new JSONObject(respStr);
                    if (json.optInt("code") == 200) {
                        JSONArray dataArray = json.optJSONArray("data");
                        if (dataArray != null) {
                            gameList.clear();
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject item = dataArray.getJSONObject(i);
                                Game game = new Game();
                                game.setGameId(item.optInt("gameId"));
                                game.setGameName(item.optString("gameName"));
                                game.setGameTypeId(item.optInt("gameTypeId"));
                                game.setGameDescription(item.optString("gameDescription"));
                                game.setGameIcon(item.optString("gameIcon"));
                                gameList.add(game);
                            }

                            runOnUiThread(() -> {
                                Log.d("游戏列表", "加载成功，共 " + gameList.size() + " 个游戏");
                                checkAndLoadDraftAfterDataLoaded();
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e("解析游戏", "错误: " + e.getMessage());
                }
            }
        });
    }

    private void loadSections(String token) {
        Request request = new Request.Builder()
                .url(ApiConstants.GET_ALL_SECTIONS)
                .addHeader("Authorization", token)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("加载版块", "失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respStr = response.body().string();
                Log.d("版块列表响应", "状态码: " + response.code() + ", 响应: " + respStr);

                try {
                    JSONObject json = new JSONObject(respStr);
                    if (json.optInt("code") == 200) {
                        JSONArray dataArray = json.optJSONArray("data");
                        if (dataArray != null) {
                            sectionList.clear();
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject item = dataArray.getJSONObject(i);
                                Section section = new Section();
                                section.setSectionId(item.optInt("sectionId"));
                                section.setSectionName(item.optString("sectionName"));
                                section.setGameId(item.optInt("gameId"));
                                section.setSectionDescription(item.optString("sectionDescription"));
                                section.setOrderNum(item.optInt("orderNum"));
                                sectionList.add(section);
                            }

                            runOnUiThread(() -> {
                                Log.d("版块列表", "加载成功，共 " + sectionList.size() + " 个版块");
                                checkAndLoadDraftAfterDataLoaded();
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e("解析版块", "错误: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 检查数据是否加载完成，如果完成且需要加载草稿，则加载草稿
     */
    private void checkAndLoadDraftAfterDataLoaded() {
        // 检查所有数据是否都已加载完成
        if (!gameTypeList.isEmpty() && !gameList.isEmpty() && !sectionList.isEmpty()) {
            // 如果是编辑草稿模式，加载草稿内容
            if (editingDraftId != null) {
                loadDraft(editingDraftId);
            }
        }
    }

    /**
     * 上传图片并插入到富文本编辑器
     */
    private void uploadAndInsertImage(Uri imageUri) {
        String token = getValidToken();
        if (token == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示上传状态
        Toast.makeText(this, "正在上传图片...", Toast.LENGTH_SHORT).show();

        imageUploadHelper.uploadImage(imageUri, token, new ImageUploadHelper.ImageUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                runOnUiThread(() -> {
                    // 将图片URL添加到已上传列表
                    uploadedImageUrls.add(imageUrl);

                    // 在富文本编辑器中插入图片
                    String jsCode = String.format("insertImage('%s');", imageUrl);
                    webViewContent.evaluateJavascript(jsCode, null);

                    Toast.makeText(NewPostActivity.this, "图片上传成功", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(NewPostActivity.this, "图片上传失败: " + error, Toast.LENGTH_SHORT).show();
                    // 从预览列表中移除失败的图片
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

    /**
     * 发布帖子
     */
    private void publishPost() {
        String title = etTitle.getText().toString().trim();
        String content = currentHtmlContent;

        if (title.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查内容是否为空（去除HTML标签后）
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

        // 直接发布，图片已经在内容中
        publishPostWithRichText(title, content, token);
    }

    /**
     * 发布富文本帖子
     */
    private void publishPostWithRichText(String title, String content, String token) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("postTitle", title);
            jsonBody.put("postContent", content);
            jsonBody.put("sectionId", selectedSection.getSectionId());

            // 如果有图片，设置第一张图片作为封面（可选）
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

            // 禁用发布按钮，防止重复提交
            btnPublish.setEnabled(false);
            btnPublish.setText("发布中...");

            Log.d("发帖请求", "URL: " + ApiConstants.USER_POST);
            Log.d("发帖请求", "请求体: " + jsonBody.toString());

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
                    Log.d("发帖响应", "状态码: " + response.code() + ", 响应: " + respStr);

                    runOnUiThread(() -> {
                        btnPublish.setEnabled(true);
                        btnPublish.setText("发布");

                        if (response.isSuccessful()) {
                            try {
                                JSONObject json = new JSONObject(respStr);
                                if (json.optInt("code") == 200) {
                                    Toast.makeText(NewPostActivity.this, "发布成功", Toast.LENGTH_SHORT).show();

                                    // 发布成功后，如果是编辑草稿，则删除该草稿
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
            Log.e("构造请求", "错误: " + e.getMessage());
            Toast.makeText(this, "构造请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnPublish.setEnabled(true);
            btnPublish.setText("发布");
        }
    }

    /**
     * 启动编辑草稿的静态方法
     */
    public static void startForEditDraft(Context context, int draftId) {
        Intent intent = new Intent(context, NewPostActivity.class);
        intent.putExtra("draft_id", draftId);
        context.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理WebView资源
        if (webViewContent != null) {
            webViewContent.destroy();
        }
        // 销毁图片上传工具类的资源
        if (imageUploadHelper != null) {
            imageUploadHelper.destroy();
        }
    }
}