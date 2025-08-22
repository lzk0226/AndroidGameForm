package com.app.gameform.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.app.gameform.R;
import com.app.gameform.adapter.GameAdapter;
import com.app.gameform.domain.Game;
import java.util.ArrayList;
import java.util.List;

public class GamesFragment extends Fragment {
    private ChipGroup chipGroupGameTypes;
    private RecyclerView rvGames;
    private GameAdapter gameAdapter;
    private List<Game> gameList;
    private List<Game> filteredGameList;
    private String selectedGameType = "所有";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_games, container, false);

        initViews(view);
        setupRecyclerView();
        setupChipGroup();
        loadGames();

        return view;
    }

    private void initViews(View view) {
        chipGroupGameTypes = view.findViewById(R.id.chipGroupGameTypes);
        rvGames = view.findViewById(R.id.rvGames);
    }

    private void setupRecyclerView() {
        gameList = new ArrayList<>();
        filteredGameList = new ArrayList<>();

        gameAdapter = new GameAdapter(filteredGameList);
        gameAdapter.setOnItemClickListener(game -> {
            // 跳转到游戏详情页面
            // Intent intent = new Intent(getActivity(), GameDetailActivity.class);
            // intent.putExtra("gameId", game.getGameId());
            // startActivity(intent);
        });

        // 使用网格布局，每行2个
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        rvGames.setLayoutManager(gridLayoutManager);
        rvGames.setAdapter(gameAdapter);
    }

    private void setupChipGroup() {
        chipGroupGameTypes.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            Chip checkedChip = group.findViewById(checkedId);
            if (checkedChip != null) {
                selectedGameType = checkedChip.getText().toString();
                filterGamesByType();
            }
        });
    }

    private void filterGamesByType() {
        filteredGameList.clear();

        if ("所有".equals(selectedGameType)) {
            filteredGameList.addAll(gameList);
        } else {
            for (Game game : gameList) {
                // 使用 gameTypeName 而不是 type
                if (selectedGameType.equals(game.getGameTypeName())) {
                    filteredGameList.add(game);
                }
            }
        }

        gameAdapter.notifyDataSetChanged();
    }

    private void loadGames() {
        // 模拟数据，实际应该从API加载
        gameList.clear();

        // 创建Game对象，使用domain层的属性
        Game game1 = new Game();
        game1.setGameId(1);
        game1.setGameName("魔兽世界");
        game1.setGameTypeName("Rts");
        game1.setGameIcon(""); // 实际项目中设置图片URL
        gameList.add(game1);

        Game game2 = new Game();
        game2.setGameId(2);
        game2.setGameName("英雄联盟");
        game2.setGameTypeName("Rts");
        game2.setGameIcon("");
        gameList.add(game2);

        Game game3 = new Game();
        game3.setGameId(3);
        game3.setGameName("反恐精英");
        game3.setGameTypeName("Fps");
        game3.setGameIcon("");
        gameList.add(game3);

        Game game4 = new Game();
        game4.setGameId(4);
        game4.setGameName("使命召唤");
        game4.setGameTypeName("Fps");
        game4.setGameIcon("");
        gameList.add(game4);

        Game game5 = new Game();
        game5.setGameId(5);
        game5.setGameName("我的世界");
        game5.setGameTypeName("开放世界");
        game5.setGameIcon("");
        gameList.add(game5);

        Game game6 = new Game();
        game6.setGameId(6);
        game6.setGameName("原神");
        game6.setGameTypeName("开放世界");
        game6.setGameIcon("");
        gameList.add(game6);

        Game game7 = new Game();
        game7.setGameId(7);
        game7.setGameName("王者荣耀");
        game7.setGameTypeName("Rts");
        game7.setGameIcon("");
        gameList.add(game7);

        Game game8 = new Game();
        game8.setGameId(8);
        game8.setGameName("绝地求生");
        game8.setGameTypeName("Fps");
        game8.setGameIcon("");
        gameList.add(game8);

        filteredGameList.addAll(gameList);
        gameAdapter.notifyDataSetChanged();
    }
}