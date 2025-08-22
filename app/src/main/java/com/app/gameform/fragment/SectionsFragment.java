package com.app.gameform.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.app.gameform.R;
import com.app.gameform.adapter.SectionAdapter;
import com.app.gameform.domain.Section;
import com.app.gameform.network.ApiConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SectionsFragment extends Fragment {
    private static final String TAG = "SectionsFragment";

    private EditText etSearch;
    private Button btnSearch;
    private RecyclerView rvSections;
    private SectionAdapter sectionAdapter;
    private List<Section> sectionList;
    private List<Section> filteredSectionList;

    private RequestQueue requestQueue;
    private String currentSearchKeyword = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sections, container, false);

        // 初始化RequestQueue
        requestQueue = Volley.newRequestQueue(requireContext());

        initViews(view);
        setupRecyclerView();
        setupSearch();
        loadSections();

        return view;
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        btnSearch = view.findViewById(R.id.btnSearch);
        rvSections = view.findViewById(R.id.rvSections);
    }

    private void setupRecyclerView() {
        sectionList = new ArrayList<>();
        filteredSectionList = new ArrayList<>();

        sectionAdapter = new SectionAdapter(filteredSectionList);
        sectionAdapter.setOnItemClickListener(section -> {
            // 跳转到版块详情页面
            // Intent intent = new Intent(getActivity(), SectionDetailActivity.class);
            // intent.putExtra("sectionId", section.getSectionId());
            // startActivity(intent);
            Toast.makeText(getContext(), "版块: " + section.getSectionName(), Toast.LENGTH_SHORT).show();
        });

        rvSections.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSections.setAdapter(sectionAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchKeyword = s.toString().trim();
                performSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSearch.setOnClickListener(v -> {
            currentSearchKeyword = etSearch.getText().toString().trim();
            performSearch();
        });
    }

    /**
     * 执行搜索
     */
    private void performSearch() {
        if (currentSearchKeyword.isEmpty()) {
            // 如果搜索关键词为空，显示所有版块
            filteredSectionList.clear();
            filteredSectionList.addAll(sectionList);
            sectionAdapter.notifyDataSetChanged();
        } else {
            // 执行搜索
            searchSections(currentSearchKeyword);
        }
    }

    /**
     * 加载所有版块
     */
    private void loadSections() {
        String url = ApiConstants.GET_ALL_SECTIONS;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            if (code == 200) {
                                JSONArray dataArray = response.getJSONArray("data");
                                sectionList = parseSections(dataArray);

                                // 如果当前没有搜索关键词，显示所有版块
                                if (currentSearchKeyword.isEmpty()) {
                                    filteredSectionList.clear();
                                    filteredSectionList.addAll(sectionList);
                                    sectionAdapter.notifyDataSetChanged();
                                }
                            } else {
                                String msg = response.optString("msg", "加载版块列表失败");
                                showError(msg);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "解析版块数据失败", e);
                            showError("数据解析失败");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "加载版块列表失败", error);
                        showError("网络错误，请检查网络连接");
                    }
                });

        requestQueue.add(request);
    }

    /**
     * 搜索版块
     */
    private void searchSections(String keyword) {
        String url = ApiConstants.BASE_URL + "/user/section/search?name=" + keyword;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            if (code == 200) {
                                JSONArray dataArray = response.getJSONArray("data");
                                List<Section> searchResults = parseSections(dataArray);

                                filteredSectionList.clear();
                                filteredSectionList.addAll(searchResults);
                                sectionAdapter.notifyDataSetChanged();
                            } else {
                                String msg = response.optString("msg", "搜索失败");
                                showError(msg);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "解析搜索结果失败", e);
                            showError("数据解析失败");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "搜索版块失败", error);
                        // 搜索失败时，执行本地筛选
                        performLocalSearch(keyword);
                    }
                });

        requestQueue.add(request);
    }

    /**
     * 本地搜索（当服务器搜索失败时的备用方案）
     */
    private void performLocalSearch(String keyword) {
        filteredSectionList.clear();

        for (Section section : sectionList) {
            if (section.getSectionName().toLowerCase().contains(keyword.toLowerCase()) ||
                    section.getSectionDescription().toLowerCase().contains(keyword.toLowerCase()) ||
                    (section.getGameName() != null && section.getGameName().toLowerCase().contains(keyword.toLowerCase()))) {
                filteredSectionList.add(section);
            }
        }

        sectionAdapter.notifyDataSetChanged();
    }

    /**
     * 解析版块数据
     */
    private List<Section> parseSections(JSONArray dataArray) {
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
                if (!createTimeStr.isEmpty()) {
                    try {
                        Date createTime = dateFormat.parse(createTimeStr);
                        section.setCreateTime(createTime);
                    } catch (Exception e) {
                        Log.w(TAG, "解析创建时间失败: " + createTimeStr, e);
                    }
                }

                sections.add(section);
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析版块数据失败", e);
        }

        return sections;
    }

    /**
     * 根据游戏ID加载版块
     */
    public void loadSectionsByGameId(Integer gameId) {
        String url = ApiConstants.BASE_URL + "/user/section/game/" + gameId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            if (code == 200) {
                                JSONArray dataArray = response.getJSONArray("data");
                                List<Section> gameSections = parseSections(dataArray);

                                filteredSectionList.clear();
                                filteredSectionList.addAll(gameSections);
                                sectionAdapter.notifyDataSetChanged();
                            } else {
                                String msg = response.optString("msg", "加载版块列表失败");
                                showError(msg);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "解析版块数据失败", e);
                            showError("数据解析失败");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "根据游戏加载版块失败", error);
                        showError("网络错误，请检查网络连接");
                    }
                });

        requestQueue.add(request);
    }

    /**
     * 获取热门版块
     */
    private void loadHotSections() {
        String url = ApiConstants.BASE_URL + "/user/section/hot?limit=5";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            if (code == 200) {
                                JSONArray dataArray = response.getJSONArray("data");
                                List<Section> hotSections = parseSections(dataArray);

                                // 可以在这里处理热门版块的显示逻辑
                                // 比如在列表顶部显示热门版块
                                Log.d(TAG, "获取到 " + hotSections.size() + " 个热门版块");
                            } else {
                                String msg = response.optString("msg", "加载热门版块失败");
                                Log.w(TAG, msg);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "解析热门版块数据失败", e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "加载热门版块失败", error);
                    }
                });

        requestQueue.add(request);
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 清空搜索
     */
    public void clearSearch() {
        etSearch.setText("");
        currentSearchKeyword = "";
        filteredSectionList.clear();
        filteredSectionList.addAll(sectionList);
        sectionAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}