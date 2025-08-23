package com.app.gameform.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.app.gameform.R;
import com.app.gameform.adapter.GameImageAdapter;
import com.app.gameform.adapter.SectionAdapter;
import com.app.gameform.domain.Game;
import com.app.gameform.domain.Section;
import com.app.gameform.network.ApiConstants;
import com.app.gameform.utils.ImageUtils;
import com.app.gameform.utils.SharedPrefManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 游戏详情页面
 * @author SockLightDust
 */
public class GameDetailActivity extends AppCompatActivity {
    private static final String TAG = "GameDetailActivity";

    // 视图组件
    private TextView tvBack;
    private ViewPager2 vpGameImages;
    private ImageView ivGameIcon;
    private TextView tvGameName;
    private TextView tvGameDescription;
    private TextView tvFullDescription;
    private TextView tvSectionsTitle;
    private RecyclerView rvSectionsList;

    // 数据
    private Game currentGame;
    private List<Section> sectionList = new ArrayList<>();
    private SectionAdapter sectionAdapter;
    private GameImageAdapter imageAdapter;
    private List<String> gameImages = new ArrayList<>();

    // 网络
    private OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    // 传入的游戏ID
    private Integer gameId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_detail);

        // 获取传入的游戏ID
        gameId = getIntent().getIntExtra("gameId", -1);
        if (gameId == -1) {
            Toast.makeText(this, "游戏ID不能为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupImagePager();
        setupClickListeners();

        // 加载游戏详情
        loadGameDetail();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        tvBack = findViewById(R.id.tv_back);
        vpGameImages = findViewById(R.id.vp_game_images);
        ivGameIcon = findViewById(R.id.iv_game_icon);
        tvGameName = findViewById(R.id.tv_game_name);
        tvGameDescription = findViewById(R.id.tv_game_description);
        tvFullDescription = findViewById(R.id.tv_full_description);
        tvSectionsTitle = findViewById(R.id.tv_sections_title);
        rvSectionsList = findViewById(R.id.rv_sections_list);
    }

    /**
     * 设置RecyclerView
     */
    private void setupRecyclerView() {
        sectionAdapter = new SectionAdapter(sectionList);
        sectionAdapter.setOnItemClickListener(section -> {
            // 跳转到版块详情页面或帖子列表页面
            //Intent intent = new Intent(GameDetailActivity.this, SectionDetailActivity.class);
            //intent.putExtra("sectionId", section.getSectionId());
            //intent.putExtra("sectionName", section.getSectionName());
            //startActivity(intent);
        });

        rvSectionsList.setLayoutManager(new LinearLayoutManager(this));
        rvSectionsList.setAdapter(sectionAdapter);
    }

    /**
     * 设置图片轮播
     */
    private void setupImagePager() {
        imageAdapter = new GameImageAdapter(gameImages);
        vpGameImages.setAdapter(imageAdapter);
    }

    /**
     * 设置点击事件监听
     */
    private void setupClickListeners() {
        tvBack.setOnClickListener(v -> finish());
    }

    /**
     * 加载游戏详情
     */
    private void loadGameDetail() {
        String token = getValidToken();
        if (token == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = ApiConstants.BASE_URL + "/user/game/" + gameId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        Log.d(TAG, "加载游戏详情，URL: " + url);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "加载游戏详情失败: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(GameDetailActivity.this, "网络错误，请检查网络连接", Toast.LENGTH_SHORT).show();
                });
            }

            // 在 loadGameDetail() 方法的 onResponse 中添加详细日志
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respStr = response.body().string();
                Log.d(TAG, "游戏详情完整响应: " + respStr);
                Log.d(TAG, "响应状态码: " + response.code());

                try {
                    JSONObject json = new JSONObject(respStr);
                    Log.d(TAG, "JSON解析成功，code: " + json.optInt("code"));

                    if (json.optInt("code") == 200) {
                        JSONObject gameData = json.optJSONObject("data");
                        if (gameData != null) {
                            // 添加详细的字段日志
                            /*Log.d(TAG, "=== 游戏数据详细信息 ===");
                            Log.d(TAG, "gameId: " + gameData.optInt("gameId"));
                            Log.d(TAG, "gameName: " + gameData.optString("gameName"));
                            Log.d(TAG, "gameDescription: " + gameData.optString("gameDescription"));
                            Log.d(TAG, "gameIcon: " + gameData.optString("gameIcon"));
                            Log.d(TAG, "gameImages: " + gameData.optString("gameImages"));
                            Log.d(TAG, "gameImageList字段: " + gameData.optString("gameImageList"));
                            Log.d(TAG, "game_images字段: " + gameData.optString("game_images"));
                            Log.d(TAG, "images字段: " + gameData.optString("images"));

                            // 打印所有字段名
                            Log.d(TAG, "所有可用字段:");*/
                            Iterator<String> keys = gameData.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                Object value = gameData.opt(key);
                                Log.d(TAG, "  " + key + ": " + value);
                            }

                            currentGame = parseGameFromJson(gameData);

                            runOnUiThread(() -> {
                                updateGameUI();
                                loadGameSections();
                            });
                        }
                    } else {
                        String msg = json.optString("msg", "加载游戏详情失败");
                        Log.e(TAG, "服务器返回错误: " + msg);
                        runOnUiThread(() -> {
                            if (response.code() == 401) {
                                Toast.makeText(GameDetailActivity.this, "登录已过期，请重新登录", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(GameDetailActivity.this, "加载失败: " + msg, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析游戏详情数据失败", e);
                    runOnUiThread(() -> {
                        Toast.makeText(GameDetailActivity.this, "数据解析失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * 从JSON解析游戏对象
     */
    /**
     * 从JSON解析游戏对象 - 修复版
     */
    private Game parseGameFromJson(JSONObject gameData) {
        Game game = new Game();
        game.setGameId(gameData.optInt("gameId"));
        game.setGameName(gameData.optString("gameName"));
        game.setGameDescription(gameData.optString("gameDescription"));
        game.setGameTypeId(gameData.optInt("gameTypeId"));
        game.setGameTypeName(gameData.optString("gameTypeName"));
        game.setGameIcon(gameData.optString("gameIcon"));
        game.setRemark(gameData.optString("remark"));

        // 优先处理 gameImageList 数组字段
        String gameImages = null;
        JSONArray gameImageListArray = gameData.optJSONArray("gameImageList");

        if (gameImageListArray != null && gameImageListArray.length() > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < gameImageListArray.length(); i++) {
                if (i > 0) sb.append(",");
                sb.append(gameImageListArray.optString(i));
            }
            gameImages = sb.toString();
            Log.d(TAG, "从gameImageList数组解析图片: " + gameImages);
        } else {
            // 如果数组字段为空，尝试字符串字段
            gameImages = gameData.optString("gameImages");
            Log.d(TAG, "从gameImages字符串获取图片: " + gameImages);
        }

        game.setGameImages(gameImages);
        Log.d(TAG, "最终设置的游戏图片: " + gameImages);

        // 解析创建时间
        String createTimeStr = gameData.optString("createTime");
        if (!TextUtils.isEmpty(createTimeStr)) {
            try {
                // 注意：服务器返回的时间格式是 ISO 8601，需要特殊处理
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
                Date createTime = dateFormat.parse(createTimeStr);
                game.setCreateTime(createTime);
                Log.d(TAG, "解析创建时间成功: " + createTime);
            } catch (Exception e) {
                Log.w(TAG, "解析创建时间失败: " + createTimeStr, e);
                // 尝试备用格式
                try {
                    SimpleDateFormat fallbackFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date createTime = fallbackFormat.parse(createTimeStr.substring(0, 19).replace("T", " "));
                    game.setCreateTime(createTime);
                    Log.d(TAG, "使用备用格式解析创建时间成功: " + createTime);
                } catch (Exception e2) {
                    Log.e(TAG, "备用格式也解析失败", e2);
                }
            }
        }

        return game;
    }

    /**
     * 更新游戏UI
     */
    private void updateGameUI() {
        if (currentGame == null) return;

        // 设置游戏名称
        tvGameName.setText(currentGame.getGameName());

        // 设置简短描述
        tvGameDescription.setText(currentGame.getGameDescription());

        // 设置详细描述
        String fullDesc = currentGame.getGameDescription();
        if (!TextUtils.isEmpty(currentGame.getRemark())) {
            fullDesc += "\n\n备注：" + currentGame.getRemark();
        }
        tvFullDescription.setText(fullDesc);

        // 加载游戏图标
        loadGameIcon();

        // 加载游戏图片
        loadGameImages();
    }

    /**
     * 加载游戏图标
     */
    private void loadGameIcon() {
        if (currentGame == null || TextUtils.isEmpty(currentGame.getGameIcon())) {
            ivGameIcon.setImageResource(R.mipmap.ic_launcher);
            return;
        }

        String iconUrl = currentGame.getGameIcon();
        Log.d(TAG, "加载游戏图标原始路径: " + iconUrl);

        // 使用 ImageUtils 加载图标
        ImageUtils.loadImage(this, ivGameIcon, iconUrl,
                R.mipmap.ic_launcher, R.mipmap.ic_launcher);
    }

    /**
     * 加载游戏图片 - 修复的版本
     */
    private void loadGameImages() {
        if (currentGame == null) {
            Log.d(TAG, "currentGame为空，隐藏图片轮播");
            vpGameImages.setVisibility(View.GONE);
            return;
        }

        String gameImagesStr = currentGame.getGameImages();
        Log.d(TAG, "游戏图片字符串: " + gameImagesStr);

        if (TextUtils.isEmpty(gameImagesStr) || "null".equals(gameImagesStr)) {
            Log.d(TAG, "游戏图片为空，隐藏图片轮播");
            vpGameImages.setVisibility(View.GONE);
            return;
        }

        // 解析图片列表
        List<String> imageList = currentGame.getGameImageList();
        Log.d(TAG, "解析后的图片列表: " + imageList.toString());

        if (imageList.isEmpty()) {
            Log.d(TAG, "图片列表为空，隐藏图片轮播");
            vpGameImages.setVisibility(View.GONE);
            return;
        }

        // 清空并添加新图片
        gameImages.clear();
        for (String imagePath : imageList) {
            if (!TextUtils.isEmpty(imagePath.trim())) {
                gameImages.add(imagePath.trim());
                Log.d(TAG, "添加图片路径: " + imagePath.trim());
            }
        }

        if (gameImages.isEmpty()) {
            Log.d(TAG, "处理后图片列表为空，隐藏图片轮播");
            vpGameImages.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "最终游戏图片列表: " + gameImages.toString() + ", 共 " + gameImages.size() + " 张图片");

        // 显示图片轮播
        vpGameImages.setVisibility(View.VISIBLE);
        imageAdapter.updateImages(gameImages);
    }

    /**
     * 加载游戏相关版块
     */
    private void loadGameSections() {
        String token = getValidToken();
        if (token == null) {
            return;
        }

        String url = ApiConstants.BASE_URL + "/user/section/game/" + gameId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        Log.d(TAG, "加载游戏版块，URL: " + url);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "加载游戏版块失败: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(GameDetailActivity.this, "加载相关版块失败", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respStr = response.body().string();
                Log.d(TAG, "游戏版块响应 - 状态码: " + response.code() + ", 响应: " + respStr);

                try {
                    JSONObject json = new JSONObject(respStr);
                    if (json.optInt("code") == 200) {
                        JSONArray dataArray = json.optJSONArray("data");
                        if (dataArray != null) {
                            List<Section> sections = parseSectionsFromJson(dataArray);

                            runOnUiThread(() -> {
                                sectionList.clear();
                                sectionList.addAll(sections);
                                sectionAdapter.notifyDataSetChanged();

                                // 更新版块标题，显示数量
                                tvSectionsTitle.setText("相关版块 (" + sections.size() + ")");

                                Log.d(TAG, "加载版块成功，共 " + sections.size() + " 个版块");
                            });
                        }
                    } else {
                        String msg = json.optString("msg", "加载版块列表失败");
                        Log.e(TAG, "服务器返回错误: " + msg);
                        runOnUiThread(() -> {
                            Toast.makeText(GameDetailActivity.this, "加载版块失败: " + msg, Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析版块数据失败", e);
                    runOnUiThread(() -> {
                        Toast.makeText(GameDetailActivity.this, "版块数据解析失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * 从JSON解析版块列表
     */
    private List<Section> parseSectionsFromJson(JSONArray dataArray) {
        List<Section> sections = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        try {
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject sectionObj = dataArray.getJSONObject(i);

                Section section = new Section();
                section.setSectionId(sectionObj.optInt("sectionId"));
                section.setSectionName(sectionObj.optString("sectionName"));
                section.setSectionDescription(sectionObj.optString("sectionDescription"));
                section.setGameId(sectionObj.optInt("gameId"));
                section.setGameName(sectionObj.optString("gameName"));
                section.setOrderNum(sectionObj.optInt("orderNum"));
                section.setRemark(sectionObj.optString("remark"));

                // 解析创建时间
                String createTimeStr = sectionObj.optString("createTime");
                if (!TextUtils.isEmpty(createTimeStr)) {
                    try {
                        Date createTime = dateFormat.parse(createTimeStr);
                        section.setCreateTime(createTime);
                    } catch (Exception e) {
                        Log.w(TAG, "解析版块创建时间失败: " + createTimeStr, e);
                    }
                }

                sections.add(section);
            }
        } catch (Exception e) {
            Log.e(TAG, "解析版块数据失败", e);
        }

        return sections;
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

        return token;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消所有网络请求
        if (client != null) {
            client.dispatcher().cancelAll();
        }
    }
}