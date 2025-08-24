package com.app.gameform.utils;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * 无限滚动监听器
 * 用于RecyclerView实现分页加载更多功能
 */
public abstract class EndlessRecyclerViewScrollListener extends RecyclerView.OnScrollListener {

    // 触发加载更多的剩余项目数阈值
    private int visibleThreshold = 5;

    // 当前页码
    private int currentPage = 0;

    // 上次总数，用于检测数据集是否发生变化
    private int previousTotalItemCount = 0;

    // 是否正在加载数据
    private boolean loading = true;

    // 起始页码
    private int startingPageIndex = 0;

    // 布局管理器
    private RecyclerView.LayoutManager layoutManager;

    /**
     * 构造函数 - LinearLayoutManager
     */
    public EndlessRecyclerViewScrollListener(LinearLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    /**
     * 构造函数 - GridLayoutManager
     */
    public EndlessRecyclerViewScrollListener(GridLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
        visibleThreshold = visibleThreshold * layoutManager.getSpanCount();
    }

    /**
     * 构造函数 - StaggeredGridLayoutManager
     */
    public EndlessRecyclerViewScrollListener(StaggeredGridLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
        visibleThreshold = visibleThreshold * layoutManager.getSpanCount();
    }

    /**
     * 自定义构造函数，可指定阈值和起始页码
     */
    public EndlessRecyclerViewScrollListener(LinearLayoutManager layoutManager,
                                             int visibleThreshold, int startPage) {
        this.layoutManager = layoutManager;
        this.visibleThreshold = visibleThreshold;
        this.startingPageIndex = startPage;
        this.currentPage = startPage;
    }

    /**
     * 获取最后一个可见项目的位置
     */
    public int getLastVisibleItem(int[] lastVisibleItemPositions) {
        int maxSize = 0;
        for (int i = 0; i < lastVisibleItemPositions.length; i++) {
            if (i == 0) {
                maxSize = lastVisibleItemPositions[i];
            } else if (lastVisibleItemPositions[i] > maxSize) {
                maxSize = lastVisibleItemPositions[i];
            }
        }
        return maxSize;
    }

    /**
     * 滚动状态改变时调用
     */
    @Override
    public void onScrollStateChanged(RecyclerView view, int scrollState) {
        super.onScrollStateChanged(view, scrollState);
    }

    /**
     * 滚动时调用 - 主要逻辑处理
     */
    @Override
    public void onScrolled(RecyclerView view, int dx, int dy) {
        super.onScrolled(view, dx, dy);

        // 只在向下滚动时处理
        if (dy <= 0) return;

        int lastVisibleItemPosition = 0;
        int totalItemCount = layoutManager.getItemCount();

        // 根据不同的布局管理器获取最后可见项目位置
        if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] lastVisibleItemPositions = ((StaggeredGridLayoutManager) layoutManager)
                    .findLastVisibleItemPositions(null);
            lastVisibleItemPosition = getLastVisibleItem(lastVisibleItemPositions);
        } else if (layoutManager instanceof GridLayoutManager) {
            lastVisibleItemPosition = ((GridLayoutManager) layoutManager)
                    .findLastVisibleItemPosition();
        } else if (layoutManager instanceof LinearLayoutManager) {
            lastVisibleItemPosition = ((LinearLayoutManager) layoutManager)
                    .findLastVisibleItemPosition();
        }

        // 如果总数小于之前记录的总数，说明数据被重置了（如下拉刷新）
        if (totalItemCount < previousTotalItemCount) {
            this.currentPage = this.startingPageIndex;
            this.previousTotalItemCount = totalItemCount;
            if (totalItemCount == 0) {
                this.loading = true;
            }
        }

        // 如果还在加载中，检查数据集是否已更新
        if (loading && (totalItemCount > previousTotalItemCount)) {
            loading = false;
            previousTotalItemCount = totalItemCount;
        }

        // 检查是否需要加载更多数据
        // 条件：未在加载中 且 (最后可见项目位置 + 阈值) >= 总项目数
        if (!loading && (lastVisibleItemPosition + visibleThreshold) >= totalItemCount) {
            currentPage++;
            onLoadMore(currentPage, totalItemCount, view);
            loading = true;
        }
    }

    /**
     * 重置滚动监听器状态
     * 通常在刷新数据时调用
     */
    public void resetState() {
        this.currentPage = this.startingPageIndex;
        this.previousTotalItemCount = 0;
        this.loading = true;
    }

    /**
     * 设置加载状态
     * @param loading 是否正在加载
     */
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    /**
     * 获取当前页码
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * 设置当前页码
     */
    public void setCurrentPage(int page) {
        this.currentPage = page;
    }

    /**
     * 获取可见阈值
     */
    public int getVisibleThreshold() {
        return visibleThreshold;
    }

    /**
     * 设置可见阈值
     */
    public void setVisibleThreshold(int visibleThreshold) {
        this.visibleThreshold = visibleThreshold;
    }

    /**
     * 是否正在加载
     */
    public boolean isLoading() {
        return loading;
    }

    /**
     * 抽象方法：需要加载更多数据时调用
     * @param page 当前页码
     * @param totalItemsCount 当前总项目数
     * @param view RecyclerView实例
     */
    public abstract void onLoadMore(int page, int totalItemsCount, RecyclerView view);
}