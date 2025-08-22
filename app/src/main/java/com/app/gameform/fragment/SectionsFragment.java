package com.app.gameform.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.app.gameform.R;
import com.app.gameform.adapter.SectionAdapter;
import com.app.gameform.domain.Section;
import java.util.ArrayList;
import java.util.List;

public class SectionsFragment extends Fragment {
    private EditText etSearch;
    private Button btnSearch;
    private RecyclerView rvSections;
    private SectionAdapter sectionAdapter;
    private List<Section> sectionList;
    private List<Section> filteredSectionList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sections, container, false);

        initViews(view);
        setupRecyclerView();
        loadSections();
        setupSearch();

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
            // 跳转到板块详情页面
            // Intent intent = new Intent(getActivity(), SectionDetailActivity.class);
            // intent.putExtra("sectionId", section.getSectionId());
            // startActivity(intent);
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
                filterSections(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSearch.setOnClickListener(v -> {
            String searchText = etSearch.getText().toString().trim();
            filterSections(searchText);
        });
    }

    private void filterSections(String query) {
        filteredSectionList.clear();

        if (query.isEmpty()) {
            filteredSectionList.addAll(sectionList);
        } else {
            for (Section section : sectionList) {
                // 使用domain层的属性名
                if (section.getSectionName().toLowerCase().contains(query.toLowerCase()) ||
                        section.getSectionDescription().toLowerCase().contains(query.toLowerCase())) {
                    filteredSectionList.add(section);
                }
            }
        }

        sectionAdapter.notifyDataSetChanged();
    }

    private void loadSections() {
        // 模拟数据，实际应该从API加载
        sectionList.clear();

        // 创建Section对象，使用domain层的属性
        Section section1 = new Section();
        section1.setSectionId(1);
        section1.setSectionName("魔兽世界讨论区");
        section1.setSectionDescription("讨论魔兽世界游戏经验，副本攻略，PVP技巧等");
        section1.setGameId(1);
        section1.setGameName("魔兽世界");
        sectionList.add(section1);

        Section section2 = new Section();
        section2.setSectionId(2);
        section2.setSectionName("英雄联盟讨论区");
        section2.setSectionDescription("讨论英雄联盟游戏经验，英雄攻略，排位心得等");
        section2.setGameId(2);
        section2.setGameName("英雄联盟");
        sectionList.add(section2);

        Section section3 = new Section();
        section3.setSectionId(3);
        section3.setSectionName("原神讨论区");
        section3.setSectionDescription("讨论原神游戏经验，角色培养，世界探索等");
        section3.setGameId(6);
        section3.setGameName("原神");
        sectionList.add(section3);

        Section section4 = new Section();
        section4.setSectionId(4);
        section4.setSectionName("王者荣耀讨论区");
        section4.setSectionDescription("讨论王者荣耀游戏经验，英雄出装，上分技巧等");
        section4.setGameId(7);
        section4.setGameName("王者荣耀");
        sectionList.add(section4);

        Section section5 = new Section();
        section5.setSectionId(5);
        section5.setSectionName("绝地求生讨论区");
        section5.setSectionDescription("讨论绝地求生游戏经验，吃鸡技巧，装备选择等");
        section5.setGameId(8);
        section5.setGameName("绝地求生");
        sectionList.add(section5);

        filteredSectionList.addAll(sectionList);
        sectionAdapter.notifyDataSetChanged();
    }
}