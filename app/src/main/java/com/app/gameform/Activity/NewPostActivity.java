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
import com.app.gameform.domain.Draft;
import com.app.gameform.domain.Game;
import com.app.gameform.domain.GameType;
import com.app.gameform.domain.Section;
import com.app.gameform.manager.DraftManager;
import com.app.gameform.network.ApiConstants;
import com.app.gameform.utils.FileUtils;
import com.app.gameform.utils.SharedPrefManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.util.Base64;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 发帖页面 - 增强版本，支持草稿自动保存
 */
public class NewPostActivity extends AppCompatActivity {

    private TextView btnClose, btnPublish, tvSelectedTopic, btnInsertImage;
    private EditText etContent, etTitle;
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

    // 优化 OkHttpClient 配置
    private OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newpost);

        initViews();
        initImagePicker();
        initEvents();
        initDraftManager();
        handleIntent();
        loadTopicData();
    }

    private void initViews() {
        btnClose = findViewById(R.id.btn_close);
        btnPublish = findViewById(R.id.btn_publish);
        tvSelectedTopic = findViewById(R.id.tv_selected_topic);
        btnInsertImage = findViewById(R.id.btn_insert_image);
        etContent = findViewById(R.id.et_content);
        etTitle = findViewById(R.id.et_title);
        rvSelectedImages = findViewById(R.id.rv_selected_images);

        rvSelectedImages.setLayoutManager(new GridLayoutManager(this, 3));
        imageAdapter = new ImageAdapter(selectedImageUris);
        rvSelectedImages.setAdapter(imageAdapter);
    }

    private void initImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUris.add(uri);
                        rvSelectedImages.setVisibility(View.VISIBLE);
                        imageAdapter.notifyDataSetChanged();
                        hasContentChanged = true;
                    }
                }
        );
    }

    private void initEvents() {
        btnClose.setOnClickListener(v -> onBackPressed());

        btnInsertImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // 话题选择点击事件
        findViewById(R.id.layout_topic_selector).setOnClickListener(v -> showTopicSelector());

        // 输入监听，控制发布按钮是否可点击
        TextWatcher contentWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasContentChanged = true;
                updatePublishButton();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        etContent.addTextChangedListener(contentWatcher);
        etTitle.addTextChangedListener(contentWatcher);

        btnPublish.setOnClickListener(v -> publishPost());
    }

    /**
     * 初始化草稿管理器
     */
    private void initDraftManager() {
        draftManager = DraftManager.getInstance(this);
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

            // 设置标题和内容
            if (!TextUtils.isEmpty(draft.getDraftTitle())) {
                etTitle.setText(draft.getDraftTitle());
            }
            if (!TextUtils.isEmpty(draft.getDraftContent())) {
                etContent.setText(draft.getDraftContent());
            }

            // 设置选中的版块 - 等待数据加载完成后设置
            if (draft.getSectionId() != null) {
                // 延迟设置选中的版块，等待数据加载完成
                setSelectedSectionFromDraft(draft);
            }

            hasContentChanged = false; // 加载草稿时不算内容变化
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
        boolean hasContent = etTitle.getText().length() > 0 || etContent.getText().length() > 0;
        boolean hasSection = selectedSection != null;
        btnPublish.setEnabled(hasContent && hasSection);
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
     * 重写返回按键处理
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
        String content = etContent.getText().toString().trim();

        // 草稿保存时不考虑图片，只保存文本内容
        boolean hasContent = draftManager.hasContentToSave(title, content) || selectedSection != null;

        if (hasContent && hasContentChanged) {
            showSaveDraftDialog(title, content);
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
     * 发布帖子
     */
    private void publishPost() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.isEmpty()) {
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

        // 如果有图片，先上传图片
        if (!selectedImageUris.isEmpty()) {
            uploadImagesAndPublish(title, content, token);
        } else {
            publishPostWithJson(title, content, "", token);
        }
    }

    private void publishPostWithJson(String title, String content, String photoUrl, String token) {
        try {
            // 构造 JSON 请求体
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("postTitle", title);
            jsonBody.put("postContent", content);
            jsonBody.put("sectionId", selectedSection.getSectionId());
            if (!photoUrl.isEmpty()) {
                jsonBody.put("photo", photoUrl);
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
            Log.d("发帖请求", "Token: " + token.substring(0, Math.min(token.length(), 50)) + "...");
            Log.d("发帖请求", "请求体: " + jsonBody.toString());

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("发帖失败", "网络错误: " + e.getMessage());
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
                                    if (response.code() == 401) {
                                        Toast.makeText(NewPostActivity.this, "登录已过期，请重新登录", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("解析响应", "错误: " + e.getMessage());
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

    // 先上传图片再发布帖子
    private void uploadImagesAndPublish(String title, String content, String token) {
        if (selectedImageUris.isEmpty()) {
            publishPostWithJson(title, content, "", token);
            return;
        }

        // 只上传第一张图片（根据你的网页版逻辑）
        Uri imageUri = selectedImageUris.get(0);

        // 禁用发布按钮，显示上传中状态
        btnPublish.setEnabled(false);
        btnPublish.setText("上传图片中...");

        try {
            // 获取文件扩展名
            String fileExtension = getFileExtension(imageUri);
            if (fileExtension == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "不支持的图片格式", Toast.LENGTH_SHORT).show();
                    btnPublish.setEnabled(true);
                    btnPublish.setText("发布");
                });
                return;
            }

            // 将 URI 转换为 Base64
            String base64Image = convertUriToBase64(imageUri);
            if (base64Image == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show();
                    btnPublish.setEnabled(true);
                    btnPublish.setText("发布");
                });
                return;
            }

            // 上传图片到服务器
            uploadImageToServer(base64Image, fileExtension, token, new ImageUploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    runOnUiThread(() -> {
                        btnPublish.setText("发布中...");
                        publishPostWithJson(title, content, imageUrl, token);
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(NewPostActivity.this, "图片上传失败: " + error, Toast.LENGTH_SHORT).show();
                        btnPublish.setEnabled(true);
                        btnPublish.setText("发布");
                    });
                }
            });

        } catch (Exception e) {
            Log.e("图片上传", "错误: " + e.getMessage());
            runOnUiThread(() -> {
                Toast.makeText(this, "图片处理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                btnPublish.setEnabled(true);
                btnPublish.setText("发布");
            });
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(Uri imageUri) {
        try {
            // 方法1：通过MIME类型获取扩展名
            String mimeType = getContentResolver().getType(imageUri);
            if (mimeType != null) {
                switch (mimeType.toLowerCase()) {
                    case "image/jpeg":
                    case "image/jpg":
                        return "jpg";
                    case "image/png":
                        return "png";
                    case "image/gif":
                        return "gif";
                    case "image/webp":
                        return "webp";
                    case "image/bmp":
                        return "bmp";
                    default:
                        Log.w("文件扩展名", "未知MIME类型: " + mimeType);
                        break;
                }
            }

            // 方法2：从文件路径获取扩展名
            String path = FileUtils.getPath(this, imageUri);
            if (path != null && path.contains(".")) {
                String ext = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
                if (ext.matches("^(jpg|jpeg|png|gif|webp|bmp)$")) {
                    return ext.equals("jpeg") ? "jpg" : ext;
                }
            }

            // 方法3：默认使用jpg
            Log.w("文件扩展名", "无法确定扩展名，使用默认jpg");
            return "jpg";

        } catch (Exception e) {
            Log.e("获取扩展名", "错误: " + e.getMessage());
            return "jpg"; // 默认返回jpg
        }
    }

    /**
     * 将 URI 转换为 Base64 字符串
     */
    private String convertUriToBase64(Uri imageUri) {
        try {
            // 从 URI 获取输入流
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e("图片转换", "无法打开图片文件");
                return null;
            }

            // 读取图片数据
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            // 检查文件大小（限制为5MB）
            if (imageBytes.length > 5 * 1024 * 1024) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "图片大小不能超过5MB", Toast.LENGTH_SHORT).show();
                });
                return null;
            }

            // 转换为 Base64
            String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            // 添加数据URL前缀（根据图片类型）
            String mimeType = getContentResolver().getType(imageUri);
            if (mimeType == null) mimeType = "image/jpeg";

            return "data:" + mimeType + ";base64," + base64String;

        } catch (IOException e) {
            Log.e("图片转换", "转换失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 生成随机字符串
     */
    private String generateRandomString() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 9; i++) {
            result.append(chars.charAt(random.nextInt(chars.length())));
        }

        return result.toString();
    }

    /**
     * 上传图片到服务器
     */
    private void uploadImageToServer(String base64Image, String fileExtension, String token, ImageUploadCallback callback) {
        try {
            // 生成文件名（与网页版保持一致的格式）
            String fileName = String.format("post_%d_%s.%s",
                    System.currentTimeMillis(),
                    generateRandomString(),
                    fileExtension);

            Log.d("图片上传", "生成的文件名: " + fileName);

            // 构建请求体
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("fileName", fileName);
            jsonBody.put("base64Data", base64Image);

            RequestBody requestBody = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            // 构建请求
            Request request = new Request.Builder()
                    .url(ApiConstants.UPLOAD_POST_IMAGE)
                    .addHeader("Authorization", token)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            Log.d("图片上传", "开始上传，URL: " + ApiConstants.UPLOAD_POST_IMAGE);
            Log.d("图片上传", "Token: " + token.substring(0, Math.min(token.length(), 50)) + "...");

            // 执行请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("图片上传", "请求失败: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respStr = response.body().string();
                    Log.d("图片上传响应", "状态码: " + response.code() + ", 响应: " + respStr);

                    try {
                        JSONObject json = new JSONObject(respStr);
                        if (json.optInt("code") == 200) {
                            // 上传成功，返回图片路径
                            String imageUrl = "images/user/post/" + fileName;
                            Log.d("图片上传", "上传成功，图片路径: " + imageUrl);
                            callback.onSuccess(imageUrl);
                        } else {
                            String errorMsg = json.optString("message", json.optString("msg", "上传失败"));
                            Log.e("图片上传", "服务器返回错误: " + errorMsg);
                            callback.onFailure(errorMsg);
                        }
                    } catch (Exception e) {
                        Log.e("解析响应", "错误: " + e.getMessage());
                        callback.onFailure("响应解析失败");
                    }
                }
            });

        } catch (Exception e) {
            Log.e("构建请求", "错误: " + e.getMessage());
            callback.onFailure("请求构建失败: " + e.getMessage());
        }
    }

    /**
     * 图片上传回调接口
     */
    interface ImageUploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    /**
     * 启动编辑草稿的静态方法
     */
    public static void startForEditDraft(Context context, int draftId) {
        Intent intent = new Intent(context, NewPostActivity.class);
        intent.putExtra("draft_id", draftId);
        context.startActivity(intent);
    }
}