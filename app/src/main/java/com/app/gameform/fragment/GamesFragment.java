package com.app.gameform.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.app.gameform.Activity.GameDetailActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.app.gameform.R;
import com.app.gameform.adapter.GameAdapter;
import com.app.gameform.domain.Game;
import com.app.gameform.domain.GameType;
import com.app.gameform.network.ApiConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GamesFragment extends Fragment {
    private static final String TAG = "GamesFragment";

    private ChipGroup chipGroupGameTypes;
    private RecyclerView rvGames;
    private GameAdapter gameAdapter;
    private List<Game> gameList;
    private List<Game> filteredGameList;
    private List<GameType> gameTypeList;
    private String selectedGameType = "所有";
    private Integer selectedGameTypeId = null;

    private RequestQueue requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_games, container, false);

        // 初始化RequestQueue
        requestQueue = Volley.newRequestQueue(requireContext());

        initViews(view);
        setupRecyclerView();
        setupChipGroup();

        // 先加载游戏类型，再加载游戏列表
        loadGameTypes();

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
            Intent intent = new Intent(getActivity(), GameDetailActivity.class);
            intent.putExtra("gameId", game.getGameId());
            startActivity(intent);

            // 可以保留这个Toast作为调试信息，或者删除
            // Toast.makeText(getContext(), "游戏: " + game.getGameName(), Toast.LENGTH_SHORT).show();
        });

        // 使用网格布局，每行2个
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        rvGames.setLayoutManager(gridLayoutManager);
        rvGames.setAdapter(gameAdapter);
    }

    private void setupChipGroup() {
        // 设置单选模式
        chipGroupGameTypes.setSingleSelection(true);
        chipGroupGameTypes.setSelectionRequired(true);

        chipGroupGameTypes.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            Chip checkedChip = group.findViewById(checkedId);
            if (checkedChip != null) {
                selectedGameType = checkedChip.getText().toString();

                // 根据选中的Chip设置对应的游戏类型ID
                if ("所有".equals(selectedGameType)) {
                    selectedGameTypeId = null;
                } else {
                    // 查找对应的游戏类型ID
                    for (GameType gameType : gameTypeList) {
                        if (gameType.getTypeName().equals(selectedGameType)) {
                            selectedGameTypeId = gameType.getTypeId();
                            break;
                        }
                    }
                }

                filterGamesByType();
            }
        });
    }

    /**
     * 加载游戏类型列表
     */
    private void loadGameTypes() {
        String url = ApiConstants.GET_ALL_GAME_TYPES;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            if (code == 200) {
                                JSONArray dataArray = response.getJSONArray("data");
                                gameTypeList = parseGameTypes(dataArray);

                                // 更新UI中的Chip
                                updateGameTypeChips();

                                // 加载游戏列表
                                loadGames();
                            } else {
                                String msg = response.optString("msg", "加载游戏类型失败");
                                showError(msg);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "解析游戏类型数据失败", e);
                            showError("数据解析失败");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "加载游戏类型失败", error);
                        showError("网络错误，请检查网络连接");
                    }
                });

        requestQueue.add(request);
    }

    /**
     * 更新游戏类型Chip
     */
    private void updateGameTypeChips() {
        // 清除现有的chip
        chipGroupGameTypes.removeAllViews();

        // 设置单选模式
        chipGroupGameTypes.setSingleSelection(true);
        chipGroupGameTypes.setSelectionRequired(true);

        // 添加"所有"chip
        Chip allChip = createStyledChip("所有", R.id.chipAll);
        allChip.setChecked(true);
        chipGroupGameTypes.addView(allChip);

        // 添加游戏类型chip
        for (GameType gameType : gameTypeList) {
            Chip chip = createStyledChip(gameType.getTypeName(), View.generateViewId());
            chipGroupGameTypes.addView(chip);
        }
    }

    /**
     * 创建统一样式的Chip
     */
    private Chip createStyledChip(String text, int id) {
        Chip chip = new Chip(getContext());
        chip.setId(id);
        chip.setText(text);
        chip.setCheckable(true);

        // 设置选中和未选中状态的颜色
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked}, // 选中状态
                        new int[]{} // 默认状态
                },
                new int[]{
                        ContextCompat.getColor(getContext(), R.color.chip_selected_color), // 蓝色
                        ContextCompat.getColor(getContext(), R.color.chip_default_color)   // 白色
                }
        );
        chip.setChipBackgroundColor(colorStateList);

        // 设置文字颜色
        ColorStateList textColorStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked}, // 选中状态
                        new int[]{} // 默认状态
                },
                new int[]{
                        Color.WHITE, // 选中时白色文字
                        ContextCompat.getColor(getContext(), R.color.chip_text_default_color) // 未选中时深色文字
                }
        );
        chip.setTextColor(textColorStateList);

        return chip;
    }


    /**
     * 加载游戏列表
     */
    private void loadGames() {
        String url = ApiConstants.GET_ALL_GAMES;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            if (code == 200) {
                                JSONArray dataArray = response.getJSONArray("data");
                                gameList = parseGames(dataArray);

                                // 初始显示所有游戏
                                filteredGameList.clear();
                                filteredGameList.addAll(gameList);
                                gameAdapter.notifyDataSetChanged();
                            } else {
                                String msg = response.optString("msg", "加载游戏列表失败");
                                showError(msg);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "解析游戏数据失败", e);
                            showError("数据解析失败");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "加载游戏列表失败", error);
                        showError("网络错误，请检查网络连接");
                    }
                });

        requestQueue.add(request);
    }

    /**
     * 根据游戏类型ID加载游戏列表
     */
    private void loadGamesByType(Integer gameTypeId) {
        String url = ApiConstants.BASE_URL + "/user/game/type/" + gameTypeId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            if (code == 200) {
                                JSONArray dataArray = response.getJSONArray("data");
                                List<Game> typeGames = parseGames(dataArray);

                                filteredGameList.clear();
                                filteredGameList.addAll(typeGames);
                                gameAdapter.notifyDataSetChanged();
                            } else {
                                String msg = response.optString("msg", "加载游戏列表失败");
                                showError(msg);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "解析游戏数据失败", e);
                            showError("数据解析失败");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "加载游戏列表失败", error);
                        showError("网络错误，请检查网络连接");
                    }
                });

        requestQueue.add(request);
    }

    /**
     * 根据类型筛选游戏
     */
    private void filterGamesByType() {
        if (selectedGameTypeId == null) {
            // 显示所有游戏
            filteredGameList.clear();
            filteredGameList.addAll(gameList);
            gameAdapter.notifyDataSetChanged();
        } else {
            // 根据类型从服务器重新获取数据
            loadGamesByType(selectedGameTypeId);
        }
    }

    /**
     * 解析游戏类型数据
     */
    private List<GameType> parseGameTypes(JSONArray dataArray) {
        List<GameType> gameTypes = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());

        try {
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject gameTypeObj = dataArray.getJSONObject(i);

                GameType gameType = new GameType();
                gameType.setTypeId(gameTypeObj.optInt("typeId"));
                gameType.setTypeName(gameTypeObj.optString("typeName"));
                gameType.setRemark(gameTypeObj.optString("remark"));

                // 解析创建时间
                String createTimeStr = gameTypeObj.optString("createTime");
                if (!createTimeStr.isEmpty()) {
                    try {
                        // 去掉时区里的冒号，例如 +08:00 → +0800
                        String fixedTimeStr = createTimeStr.replaceAll(":(?=[0-9]{2}$)", "");
                        Date createTime = dateFormat.parse(fixedTimeStr);
                        gameType.setCreateTime(createTime);
                    } catch (Exception e) {
                        Log.w(TAG, "解析创建时间失败: " + createTimeStr, e);
                    }
                }

                gameTypes.add(gameType);
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析游戏类型数据失败", e);
        }

        return gameTypes;
    }


    /**
     * 解析游戏数据
     */
    private List<Game> parseGames(JSONArray dataArray) {
        List<Game> games = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());

        try {
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject gameObj = dataArray.getJSONObject(i);

                Game game = new Game();
                game.setGameId(gameObj.optInt("gameId"));
                game.setGameName(gameObj.optString("gameName"));
                game.setGameDescription(gameObj.optString("gameDescription"));
                game.setGameTypeId(gameObj.optInt("gameTypeId"));
                game.setGameTypeName(gameObj.optString("gameTypeName"));
                game.setGameIcon(gameObj.optString("gameIcon"));
                game.setGameImages(gameObj.optString("gameImages"));
                game.setRemark(gameObj.optString("remark"));

                // 解析创建时间
                String createTimeStr = gameObj.optString("createTime");
                if (!createTimeStr.isEmpty()) {
                    try {
                        // 去掉时区里的冒号  (把 +08:00 → +0800)
                        String fixedTimeStr = createTimeStr.replaceAll(":(?=[0-9]{2}$)", "");
                        Date createTime = dateFormat.parse(fixedTimeStr);
                        game.setCreateTime(createTime);
                    } catch (Exception e) {
                        Log.w(TAG, "解析创建时间失败: " + createTimeStr, e);
                    }
                }

                games.add(game);
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析游戏数据失败", e);
        }

        return games;
    }


    /**
     * 显示错误信息
     */
    private void showError(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}