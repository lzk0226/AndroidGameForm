/*
package com.app.gameform.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.app.gameform.R;
import com.app.gameform.domain.Game;
import com.app.gameform.domain.Section;
import com.app.gameform.network.ApiConstants;
import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SectionDetailActivity extends AppCompatActivity {

    // UI组件
    private TextView tvBack;
    private ImageView ivGameIcon;
    private TextView tvSectionName;
    private TextView tvSectionDescription;
    private TextView tvPost;
    private TextView tvHot;
    private TextView tvLatest;
    private ViewPager2 vpPosts;

    // 数据
    private Section section;
    private Game game; // 关联的游戏信息
    private Integer sectionId;
    private OkHttpClient okHttpClient;
    private SectionPostPagerAdapter postPagerAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_section_detail);

        // 获取传递的板块ID
        sectionId = getIntent().getIntExtra("sectionId", -1);
        if (sectionId == -1) {
            Toast.makeText(this, "板块ID不能为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initClient();
        loadSectionDetail();
    }

    private void initViews() {
        tvBack = findViewById(R.id.tv_back);
        ivGameIcon = findViewById(R.id.iv_game_icon);
        tvSectionName = findViewById(R.id.tv_section_name);
        tvSectionDescription = findViewById(R.id.tv_section_description);
        tvPost = findViewById(R.id.tv_post);
        tvHot = findViewById(R.id.tv_hot);
        tvLatest = findViewById(R.id.tv_latest);
        vpPosts = findViewById(R.id.vp_posts);

        // 设置点击事件
        tvBack.setOnClickListener(v -> finish());

        tvPost.setOnClickListener(v -> {
            // 跳转到发帖页面
            Intent intent = new Intent(this, CreatePostActivity.class);
            intent.putExtra("sectionId", sectionId);
            if (game != null) {
                intent.putExtra("gameId", game.getGameId());
            }
            startActivity(intent);
        });

        // 设置帖子类型切换
        tvHot.setOnClickListener(v -> switchPostType("hot"));
        tvLatest.setOnClickListener(v -> switchPostType("latest"));

        // 初始化帖子ViewPager
        setupPostViewPager();
    }

    private void initClient() {
        okHttpClient = new OkHttpClient();
    }

    private void loadSectionDetail() {
        String url = ApiConstants.buildUrlWithParam(ApiConstants.GET_SECTION_DETAIL, String.valueOf(sectionId));

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(SectionDetailActivity.this, "网络请求失败，请稍后重试", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        if (jsonObject.getInt("code") == 200) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            section = parseSectionFromJson(data);

                            runOnUiThread(() -> {
                                updateUI();
                                // 如果有游戏ID，加载游戏图标
                                if (section.getGameId() != null) {
                                    loadGameIcon(section.getGameId());
                                }
                            });
                        } else {
                            runOnUiThread(() -> {
                                String msg = jsonObject.optString("msg", "获取板块信息失败");
                                Toast.makeText(SectionDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(SectionDetailActivity.this, "数据解析失败", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }

    private void loadGameIcon(Integer gameId) {
        String url = ApiConstants.buildUrlWithParam(ApiConstants.GET_GAME_DETAIL, String.valueOf(gameId));

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 图标加载失败不影响主要功能，只是显示默认图标
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        if (jsonObject.getInt("code") == 200) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            game = parseGameFromJson(data);

                            runOnUiThread(() -> {
                                updateGameIcon();
                            });
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }
        });
    }

    private Section parseSectionFromJson(JSONObject data) throws Exception {
        Section section = new Section();
        section.setSectionId(data.optInt("sectionId"));
        section.setSectionName(data.optString("sectionName"));
        section.setSectionDescription(data.optString("sectionDescription"));
        section.setGameId(data.optInt("gameId"));
        section.setGameName(data.optString("gameName"));
        section.setOrderNum(data.optInt("orderNum"));
        section.setRemark(data.optString("remark"));

        // 解析创建时间
        String createTimeStr = data.optString("createTime");
        if (!createTimeStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date createTime = sdf.parse(createTimeStr);
                section.setCreateTime(createTime);
            } catch (Exception e) {
                section.setCreateTime(new Date());
            }
        }

        return section;
    }

    private Game parseGameFromJson(JSONObject data) throws Exception {
        Game game = new Game();
        game.setGameId(data.optInt("gameId"));
        game.setGameName(data.optString("gameName"));
        game.setGameDescription(data.optString("gameDescription"));
        game.setGameIcon(data.optString("gameIcon"));
        return game;
    }

    private void updateUI() {
        if (section == null) return;

        // 设置板块基本信息
        tvSectionName.setText(section.getSectionName());

        // 设置板块描述
        String description = section.getSectionDescription();
        if (description == null || description.trim().isEmpty()) {
            if (section.getGameName() != null) {
                description = section.getGameName() + "讨论区";
            } else {
                description = "游戏讨论区";
            }
        }
        tvSectionDescription.setText(description);
    }

    private void updateGameIcon() {
        if (game != null && game.getGameIcon() != null && !game.getGameIcon().isEmpty()) {
            String iconUrl = ApiConstants.getFullImageUrl(game.getGameIcon());
            Glide.with(this)
                    .load(iconUrl)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(ivGameIcon);
        }
    }

    private void setupPostViewPager() {
        postPagerAdapter = new SectionPostPagerAdapter(this, sectionId);
        vpPosts.setAdapter(postPagerAdapter);

        // 默认选中最热
        switchPostType("hot");
    }

    private void switchPostType(String type) {
        // 更新UI状态
        if ("hot".equals(type)) {
            tvHot.setTextColor(getResources().getColor(android.R.color.black));
            tvHot.setTypeface(null, android.graphics.Typeface.BOLD);
            tvLatest.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvLatest.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            tvLatest.setTextColor(getResources().getColor(android.R.color.black));
            tvLatest.setTypeface(null, android.graphics.Typeface.BOLD);
            tvHot.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvHot.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // 更新帖子列表
        postPagerAdapter.switchPostType(type);
    }

    // 板块帖子ViewPager适配器
    private static class SectionPostPagerAdapter extends FragmentStateAdapter {
        private String currentPostType = "hot";
        private Integer sectionId;

        public SectionPostPagerAdapter(@NonNull FragmentActivity fragmentActivity, Integer sectionId) {
            super(fragmentActivity);
            this.sectionId = sectionId;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // 创建板块帖子列表Fragment
            SectionPostListFragment fragment = SectionPostListFragment.newInstance(sectionId, currentPostType);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return 1; // 只有一个Fragment
        }

        public void switchPostType(String type) {
            this.currentPostType = type;
            notifyDataSetChanged();
        }
    }
}*/
