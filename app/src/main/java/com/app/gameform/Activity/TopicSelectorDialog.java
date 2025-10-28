package com.app.gameform.Activity;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.domain.Game;
import com.app.gameform.domain.GameType;
import com.app.gameform.domain.Section;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 话题选择对话框 - 优化版：支持按需加载
 */
public class TopicSelectorDialog extends Dialog {

    private static final String TAG = "TopicSelectorDialog";

    private EditText etSearch;
    private RecyclerView rvGameTypes, rvGames, rvSections;
    private LinearLayout rootLayout;

    private GameTypeAdapter gameTypeAdapter;
    private GameAdapter gameAdapter;
    private SectionAdapter sectionAdapter;

    private List<GameType> gameTypeList = new ArrayList<>();
    private List<Game> filteredGames = new ArrayList<>();
    private List<Section> filteredSections = new ArrayList<>();

    private GameType selectedGameType;
    private Game selectedGame;
    private Section selectedSection;

    private OnTopicSelectedListener topicSelectedListener;

    // ⭐ 新增：按需加载回调
    private OnGameTypeSelectedListener gameTypeSelectedListener;
    private OnGameSelectedListener gameSelectedListener;

    // ⭐ 新增：数据缓存
    private Map<Integer, List<Game>> gamesByTypeCache;
    private Map<Integer, List<Section>> sectionsByGameCache;

    // 手势相关变量
    private float startY;
    private float currentY;
    private boolean isDragging = false;
    private static final int MIN_DRAG_DISTANCE = 100;

    // ⭐ 回调接口
    public interface OnTopicSelectedListener {
        void onTopicSelected(GameType gameType, Game game, Section section);
    }

    public interface OnGameTypeSelectedListener {
        void onGameTypeSelected(GameType gameType);
    }

    public interface OnGameSelectedListener {
        void onGameSelected(Game game);
    }

    public TopicSelectorDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_topic_selector);

        initViews();
        initAdapters();
        initEvents();
        setupWindow();
        setupDragToClose();
    }

    private void initViews() {
        rootLayout = findViewById(R.id.root_layout);
        etSearch = findViewById(R.id.et_search);
        rvGameTypes = findViewById(R.id.rv_game_types);
        rvGames = findViewById(R.id.rv_games);
        rvSections = findViewById(R.id.rv_sections);

        rvGameTypes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvGames.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSections.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void initAdapters() {
        gameTypeAdapter = new GameTypeAdapter();
        gameAdapter = new GameAdapter();
        sectionAdapter = new SectionAdapter();

        rvGameTypes.setAdapter(gameTypeAdapter);
        rvGames.setAdapter(gameAdapter);
        rvSections.setAdapter(sectionAdapter);
    }

    private void initEvents() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContent(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupWindow() {
        Window window = getWindow();
        if (window != null) {
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.7));
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            WindowManager.LayoutParams params = window.getAttributes();
            params.windowAnimations = android.R.style.Animation_Dialog;
            window.setAttributes(params);
        }
    }

    private void setupDragToClose() {
        View contentView = findViewById(android.R.id.content);
        if (contentView != null) {
            contentView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startY = event.getRawY();
                            isDragging = false;
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            currentY = event.getRawY();
                            float deltaY = currentY - startY;

                            if (deltaY > 0 && deltaY > 50) {
                                isDragging = true;
                                v.setTranslationY(deltaY * 0.5f);
                                return true;
                            }
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (isDragging) {
                                deltaY = currentY - startY;
                                if (deltaY > MIN_DRAG_DISTANCE) {
                                    dismiss();
                                } else {
                                    v.animate().translationY(0).setDuration(200).start();
                                }
                                isDragging = false;
                                return true;
                            }
                            break;
                    }
                    return false;
                }
            });
        }
    }

    private void filterContent(String query) {
        if (query.isEmpty()) {
            gameTypeAdapter.updateData(gameTypeList);
            return;
        }

        List<GameType> filteredTypes = new ArrayList<>();
        for (GameType type : gameTypeList) {
            if (type.getTypeName().contains(query)) {
                filteredTypes.add(type);
            }
        }
        gameTypeAdapter.updateData(filteredTypes);
    }

    // ⭐ 新增：设置游戏类型（只设置第一列）
    public void setGameTypes(List<GameType> gameTypes) {
        this.gameTypeList = gameTypes != null ? gameTypes : new ArrayList<>();
        Log.d(TAG, "setGameTypes - 游戏类型数量: " + this.gameTypeList.size());

        if (gameTypeAdapter != null) {
            gameTypeAdapter.updateData(this.gameTypeList);
        }
    }

    // ⭐ 新增：设置数据缓存
    public void setDataCache(Map<Integer, List<Game>> gamesCache,
                             Map<Integer, List<Section>> sectionsCache) {
        this.gamesByTypeCache = gamesCache;
        this.sectionsByGameCache = sectionsCache;
        Log.d(TAG, "setDataCache - 游戏缓存: " +
                (gamesCache != null ? gamesCache.size() : 0) +
                ", 板块缓存: " +
                (sectionsCache != null ? sectionsCache.size() : 0));
    }

    // ⭐ 新增：设置按需加载监听器
    public void setOnGameTypeSelectedListener(OnGameTypeSelectedListener listener) {
        this.gameTypeSelectedListener = listener;
    }

    public void setOnGameSelectedListener(OnGameSelectedListener listener) {
        this.gameSelectedListener = listener;
    }

    public void setOnTopicSelectedListener(OnTopicSelectedListener listener) {
        this.topicSelectedListener = listener;
    }

    // ⭐ 新增：从缓存中更新游戏列表
    public void updateGamesFromCache(int gameTypeId) {
        if (gamesByTypeCache != null && gamesByTypeCache.containsKey(gameTypeId)) {
            List<Game> games = gamesByTypeCache.get(gameTypeId);
            if (games != null) {
                filteredGames.clear();
                filteredGames.addAll(games);
                gameAdapter.notifyDataSetChanged();
                Log.d(TAG, "从缓存更新游戏列表: " + filteredGames.size() + " 个游戏");
            }
        }
    }

    // ⭐ 新增：从缓存中更新板块列表
    public void updateSectionsFromCache(int gameId) {
        if (sectionsByGameCache != null && sectionsByGameCache.containsKey(gameId)) {
            List<Section> sections = sectionsByGameCache.get(gameId);
            if (sections != null) {
                filteredSections.clear();
                filteredSections.addAll(sections);
                sectionAdapter.notifyDataSetChanged();
                Log.d(TAG, "从缓存更新板块列表: " + filteredSections.size() + " 个板块");
            }
        }
    }

    // 旧版本的 setData 方法（保持兼容性，用于编辑草稿）
    @Deprecated
    public void setData(List<GameType> gameTypes, List<Game> games, List<Section> sections) {
        Log.w(TAG, "使用了已废弃的 setData 方法，建议使用 setGameTypes 和 setDataCache");
        this.gameTypeList = gameTypes != null ? gameTypes : new ArrayList<>();

        // 构建缓存
        if (games != null) {
            gamesByTypeCache = new java.util.HashMap<>();
            for (Game game : games) {
                int typeId = game.getGameTypeId();
                if (!gamesByTypeCache.containsKey(typeId)) {
                    gamesByTypeCache.put(typeId, new ArrayList<>());
                }
                gamesByTypeCache.get(typeId).add(game);
            }
        }

        if (sections != null) {
            sectionsByGameCache = new java.util.HashMap<>();
            for (Section section : sections) {
                int gameId = section.getGameId();
                if (!sectionsByGameCache.containsKey(gameId)) {
                    sectionsByGameCache.put(gameId, new ArrayList<>());
                }
                sectionsByGameCache.get(gameId).add(section);
            }
        }

        if (gameTypeAdapter != null) {
            gameTypeAdapter.updateData(this.gameTypeList);
        }
    }

    // 获取当前选择的话题
    public Section getSelectedSection() {
        return selectedSection;
    }

    public Game getSelectedGame() {
        return selectedGame;
    }

    public GameType getSelectedGameType() {
        return selectedGameType;
    }

    // ==================== 适配器 ====================

    // GameType 适配器
    private class GameTypeAdapter extends RecyclerView.Adapter<GameTypeAdapter.ViewHolder> {
        private List<GameType> data = new ArrayList<>();

        void updateData(List<GameType> newData) {
            this.data = newData;
            notifyDataSetChanged();
            Log.d(TAG, "GameTypeAdapter 更新数据: " + data.size() + " 条");
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(getContext());
            textView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setPadding(32, 24, 32, 24);
            textView.setTextSize(16);
            textView.setTextColor(Color.parseColor("#333333"));
            textView.setGravity(Gravity.CENTER_VERTICAL);

            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.TRANSPARENT);
            textView.setBackground(background);

            return new ViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GameType gameType = data.get(position);
            holder.textView.setText(gameType.getTypeName());

            boolean isSelected = selectedGameType != null &&
                    selectedGameType.getTypeId().equals(gameType.getTypeId());

            GradientDrawable background = new GradientDrawable();
            if (isSelected) {
                background.setColor(Color.parseColor("#E3F2FD"));
                holder.textView.setTextColor(Color.parseColor("#1976D2"));
            } else {
                background.setColor(Color.TRANSPARENT);
                holder.textView.setTextColor(Color.parseColor("#333333"));
            }
            holder.textView.setBackground(background);

            holder.textView.setOnClickListener(v -> {
                selectedGameType = gameType;
                notifyDataSetChanged();

                // ⭐ 清空游戏和板块选择
                selectedGame = null;
                selectedSection = null;
                filteredGames.clear();
                filteredSections.clear();
                gameAdapter.notifyDataSetChanged();
                sectionAdapter.notifyDataSetChanged();

                // ⭐ 通知外部加载游戏列表
                if (gameTypeSelectedListener != null) {
                    gameTypeSelectedListener.onGameTypeSelected(gameType);
                }

                // ⭐ 检查缓存，如果有则直接显示
                if (gamesByTypeCache != null && gamesByTypeCache.containsKey(gameType.getTypeId())) {
                    List<Game> cachedGames = gamesByTypeCache.get(gameType.getTypeId());
                    if (cachedGames != null) {
                        filteredGames.clear();
                        filteredGames.addAll(cachedGames);
                        gameAdapter.notifyDataSetChanged();
                        Log.d(TAG, "使用缓存的游戏数据: " + filteredGames.size() + " 个");
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView;
            }
        }
    }

    // Game 适配器
    private class GameAdapter extends RecyclerView.Adapter<GameAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView nameView = new TextView(getContext());
            nameView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            nameView.setPadding(32, 24, 32, 24);
            nameView.setTextSize(16);
            nameView.setTextColor(Color.parseColor("#333333"));
            nameView.setGravity(Gravity.CENTER_VERTICAL);

            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.TRANSPARENT);
            nameView.setBackground(background);

            return new ViewHolder(nameView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Game game = filteredGames.get(position);
            holder.nameView.setText(game.getGameName());

            boolean isSelected = selectedGame != null &&
                    selectedGame.getGameId().equals(game.getGameId());

            GradientDrawable background = new GradientDrawable();
            if (isSelected) {
                background.setColor(Color.parseColor("#E3F2FD"));
                holder.nameView.setTextColor(Color.parseColor("#1976D2"));
            } else {
                background.setColor(Color.TRANSPARENT);
                holder.nameView.setTextColor(Color.parseColor("#333333"));
            }
            holder.nameView.setBackground(background);

            holder.nameView.setOnClickListener(v -> {
                selectedGame = game;
                notifyDataSetChanged();

                // ⭐ 清空板块选择
                selectedSection = null;
                filteredSections.clear();
                sectionAdapter.notifyDataSetChanged();

                // ⭐ 通知外部加载板块列表
                if (gameSelectedListener != null) {
                    gameSelectedListener.onGameSelected(game);
                }

                // ⭐ 检查缓存，如果有则直接显示
                if (sectionsByGameCache != null && sectionsByGameCache.containsKey(game.getGameId())) {
                    List<Section> cachedSections = sectionsByGameCache.get(game.getGameId());
                    if (cachedSections != null) {
                        filteredSections.clear();
                        filteredSections.addAll(cachedSections);
                        sectionAdapter.notifyDataSetChanged();
                        Log.d(TAG, "使用缓存的板块数据: " + filteredSections.size() + " 个");
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return filteredGames.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameView;

            ViewHolder(TextView nameView) {
                super(nameView);
                this.nameView = nameView;
            }
        }
    }

    // Section 适配器
    private class SectionAdapter extends RecyclerView.Adapter<SectionAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView nameView = new TextView(getContext());
            nameView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            nameView.setPadding(32, 24, 32, 24);
            nameView.setTextSize(16);
            nameView.setTextColor(Color.parseColor("#333333"));
            nameView.setGravity(Gravity.CENTER_VERTICAL);

            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.TRANSPARENT);
            nameView.setBackground(background);

            return new ViewHolder(nameView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Section section = filteredSections.get(position);
            holder.nameView.setText(section.getSectionName());

            boolean isSelected = selectedSection != null &&
                    selectedSection.getSectionId().equals(section.getSectionId());

            GradientDrawable background = new GradientDrawable();
            if (isSelected) {
                background.setColor(Color.parseColor("#E3F2FD"));
                holder.nameView.setTextColor(Color.parseColor("#1976D2"));
            } else {
                background.setColor(Color.TRANSPARENT);
                holder.nameView.setTextColor(Color.parseColor("#333333"));
            }
            holder.nameView.setBackground(background);

            holder.nameView.setOnClickListener(v -> {
                selectedSection = section;
                notifyDataSetChanged();

                // 选择板块后自动触发回调并关闭对话框
                if (topicSelectedListener != null) {
                    topicSelectedListener.onTopicSelected(selectedGameType, selectedGame, selectedSection);
                }
                dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return filteredSections.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameView;

            ViewHolder(TextView nameView) {
                super(nameView);
                this.nameView = nameView;
            }
        }
    }
}