package com.app.gameform.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.app.gameform.R;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private TextView btnSearch;
    private ImageView btnBack;
    private TextView btnClearHistory;

    // 热门搜索按钮
    private TextView hotSearch1;
    private TextView hotSearch2;
    private TextView hotSearch3;
    private TextView hotSearch4;
    private TextView hotSearch5;
    private TextView hotSearch6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setClickListeners();
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);
        btnBack = findViewById(R.id.btn_back);
        btnClearHistory = findViewById(R.id.btn_clear_history);

        // 热门搜索按钮
        hotSearch1 = findViewById(R.id.hot_search_1);
        hotSearch2 = findViewById(R.id.hot_search_2);
        hotSearch3 = findViewById(R.id.hot_search_3);
        hotSearch4 = findViewById(R.id.hot_search_4);
        hotSearch5 = findViewById(R.id.hot_search_5);
        hotSearch6 = findViewById(R.id.hot_search_6);
    }

    private void setClickListeners() {
        // 返回按钮
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 搜索按钮
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = etSearch.getText().toString().trim();
                if (!TextUtils.isEmpty(query)) {
                    performSearch(query);
                }
            }
        });

        // 清空历史按钮
        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSearchHistory();
            }
        });

        // 热门搜索点击事件
        hotSearch1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fillSearchBox("人工智能");
            }
        });

        hotSearch2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fillSearchBox("区块链");
            }
        });

        hotSearch3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fillSearchBox("元宇宙");
            }
        });

        hotSearch4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fillSearchBox("机器学习");
            }
        });

        hotSearch5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fillSearchBox("云计算");
            }
        });

        hotSearch6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fillSearchBox("大数据");
            }
        });

        // 搜索框回车事件
        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = etSearch.getText().toString().trim();
                    if (!TextUtils.isEmpty(query)) {
                        performSearch(query);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * 将内容填入搜索框
     */
    private void fillSearchBox(String content) {
        etSearch.setText(content);
        etSearch.setSelection(content.length()); // 光标移动到末尾
    }

    /**
     * 执行搜索
     */
    private void performSearch(String query) {
        // 这里实现搜索逻辑
        // 保存搜索历史
        saveSearchHistory(query);
    }

    /**
     * 保存搜索历史
     */
    private void saveSearchHistory(String query) {
        // 这里实现保存搜索历史的逻辑
        // 可以使用SharedPreferences或数据库
    }

    /**
     * 清空搜索历史
     */
    private void clearSearchHistory() {
        // 这里实现清空搜索历史的逻辑
        // 同时刷新搜索历史RecyclerView
    }
}