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

/**
 * 话题选择对话框
 */
public class TopicSelectorDialog extends Dialog {

    private EditText etSearch;
    private RecyclerView rvGameTypes, rvGames, rvSections;
    private LinearLayout rootLayout;

    private GameTypeAdapter gameTypeAdapter;
    private GameAdapter gameAdapter;
    private SectionAdapter sectionAdapter;

    private List<GameType> gameTypeList = new ArrayList<>();
    private List<Game> gameList = new ArrayList<>();
    private List<Section> sectionList = new ArrayList<>();

    private List<Game> filteredGames = new ArrayList<>();
    private List<Section> filteredSections = new ArrayList<>();

    private GameType selectedGameType;
    private Game selectedGame;
    private Section selectedSection;

    private OnTopicSelectedListener listener;

    // 手势相关变量
    private float startY;
    private float currentY;
    private boolean isDragging = false;
    private static final int MIN_DRAG_DISTANCE = 100; // 最小拖拽距离

    public interface OnTopicSelectedListener {
        void onTopicSelected(GameType gameType, Game game, Section section);
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
        rootLayout = findViewById(R.id.root_layout); // ✅ 正确获取根布局
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

            // 添加窗口动画和样式
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

                            if (deltaY > 0 && deltaY > 50) { // 向下拖拽超过50px开始响应
                                isDragging = true;
                                // 可以在这里添加拖拽时的视觉反馈
                                v.setTranslationY(deltaY * 0.5f); // 添加阻尼效果
                                return true;
                            }
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (isDragging) {
                                deltaY = currentY - startY;
                                if (deltaY > MIN_DRAG_DISTANCE) {
                                    // 拖拽距离足够，关闭对话框
                                    dismiss();
                                } else {
                                    // 拖拽距离不够，恢复原位
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

    private void updateGames(int gameTypeId) {
        filteredGames.clear();
        for (Game game : gameList) {
            if (game.getGameTypeId() == gameTypeId) {
                filteredGames.add(game);
            }
        }
        gameAdapter.notifyDataSetChanged();

        // 清空版块选择
        filteredSections.clear();
        sectionAdapter.notifyDataSetChanged();
        selectedGame = null;
        selectedSection = null;
    }

    private void updateSections(int gameId) {
        filteredSections.clear();
        for (Section section : sectionList) {
            if (section.getGameId() == gameId) {
                filteredSections.add(section);
            }
        }
        sectionAdapter.notifyDataSetChanged();
        selectedSection = null;
    }

    public void setData(List<GameType> gameTypes, List<Game> games, List<Section> sections) {
        this.gameTypeList = gameTypes != null ? gameTypes : new ArrayList<>();
        this.gameList = games != null ? games : new ArrayList<>();
        this.sectionList = sections != null ? sections : new ArrayList<>();

        Log.d("TopicSelectorDialog", "setData - 游戏类型: " + this.gameTypeList.size() +
                ", 游戏: " + this.gameList.size() +
                ", 版块: " + this.sectionList.size());

        // ✅ 确保在主线程更新适配器
        if (gameTypeAdapter != null) {
            gameTypeAdapter.updateData(this.gameTypeList);
            Log.d("TopicSelectorDialog", "游戏类型适配器已更新，数据量: " + this.gameTypeList.size());
        } else {
            Log.w("TopicSelectorDialog", "gameTypeAdapter 为 null，适配器尚未初始化");
        }
    }

    public void setOnTopicSelectedListener(OnTopicSelectedListener listener) {
        this.listener = listener;
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

    // GameType适配器
    private class GameTypeAdapter extends RecyclerView.Adapter<GameTypeAdapter.ViewHolder> {
        private List<GameType> data = new ArrayList<>();

        void updateData(List<GameType> newData) {
            this.data = newData;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(getContext());
            // ✅ 设置宽度为MATCH_PARENT，确保背景色能填满整行
            textView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setPadding(32, 24, 32, 24);
            textView.setTextSize(16);
            textView.setTextColor(Color.parseColor("#333333"));
            textView.setGravity(Gravity.CENTER_VERTICAL);

            // 创建背景drawable
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
                updateGames(gameType.getTypeId());
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

    // Game适配器
    private class GameAdapter extends RecyclerView.Adapter<GameAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView nameView = new TextView(getContext());
            // ✅ 设置宽度为MATCH_PARENT，确保背景色能填满整行
            nameView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            nameView.setPadding(32, 24, 32, 24);
            nameView.setTextSize(16);
            nameView.setTextColor(Color.parseColor("#333333"));
            nameView.setGravity(Gravity.CENTER_VERTICAL);

            // 创建背景drawable
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
                updateSections(game.getGameId());
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

    // Section适配器
    private class SectionAdapter extends RecyclerView.Adapter<SectionAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView nameView = new TextView(getContext());
            // ✅ 设置宽度为MATCH_PARENT，确保背景色能填满整行
            nameView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            nameView.setPadding(32, 24, 32, 24);
            nameView.setTextSize(16);
            nameView.setTextColor(Color.parseColor("#333333"));
            nameView.setGravity(Gravity.CENTER_VERTICAL);

            // 创建背景drawable
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

                // 选择版块后自动触发回调并关闭对话框
                if (listener != null) {
                    listener.onTopicSelected(selectedGameType, selectedGame, selectedSection);
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