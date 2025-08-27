package com.app.gameform.utils;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 懒加载工具类
 * 统一管理分页加载的配置和逻辑
 */
public class LazyLoadingHelper {

    // 配置常量
    public static final int DEFAULT_PAGE_SIZE = 8;
    public static final int DEFAULT_LOAD_MORE_THRESHOLD = 6;
    public static final int DEFAULT_INITIAL_PAGE = 1;

    /**
     * 分页配置类
     */
    public static class PaginationConfig {
        private int pageSize;
        private int loadMoreThreshold;
        private int initialPage;

        public PaginationConfig() {
            this.pageSize = DEFAULT_PAGE_SIZE;
            this.loadMoreThreshold = DEFAULT_LOAD_MORE_THRESHOLD;
            this.initialPage = DEFAULT_INITIAL_PAGE;
        }

        public PaginationConfig(int pageSize, int loadMoreThreshold, int initialPage) {
            this.pageSize = pageSize;
            this.loadMoreThreshold = loadMoreThreshold;
            this.initialPage = initialPage;
        }

        // Getters and Setters
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getLoadMoreThreshold() { return loadMoreThreshold; }
        public void setLoadMoreThreshold(int loadMoreThreshold) { this.loadMoreThreshold = loadMoreThreshold; }

        public int getInitialPage() { return initialPage; }
        public void setInitialPage(int initialPage) { this.initialPage = initialPage; }
    }

    /**
     * 分页状态管理类
     */
    public static class PaginationState {
        private int currentPage;
        private boolean hasMoreData;
        private boolean isLoading;
        private final PaginationConfig config;

        public PaginationState(PaginationConfig config) {
            this.config = config;
            reset();
        }

        public void reset() {
            this.currentPage = config.getInitialPage();
            this.hasMoreData = true;
            this.isLoading = false;
        }

        public void nextPage() {
            this.currentPage++;
        }

        public void previousPage() {
            if (this.currentPage > config.getInitialPage()) {
                this.currentPage--;
            }
        }

        // Getters and Setters
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

        public boolean hasMoreData() { return hasMoreData; }
        public void setHasMoreData(boolean hasMoreData) { this.hasMoreData = hasMoreData; }

        public boolean isLoading() { return isLoading; }
        public void setLoading(boolean loading) { isLoading = loading; }

        public PaginationConfig getConfig() { return config; }
    }

    /**
     * 滚动监听器接口
     */
    public interface OnLoadMoreListener {
        void onLoadMore(int page);
    }

    /**
     * 创建滚动监听器
     */
    public static RecyclerView.OnScrollListener createScrollListener(
            LinearLayoutManager layoutManager,
            PaginationState state,
            OnLoadMoreListener listener) {

        return new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0 && !state.isLoading() && state.hasMoreData()) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    int lastVisibleItem = firstVisibleItemPosition + visibleItemCount;
                    int threshold = state.getConfig().getPageSize() - state.getConfig().getLoadMoreThreshold();

                    if ((totalItemCount - lastVisibleItem) <= threshold) {
                        state.nextPage();
                        state.setLoading(true);
                        listener.onLoadMore(state.getCurrentPage());
                    }
                }
            }
        };
    }

    /**
     * 构建分页URL
     */
    public static String buildPaginationUrl(String baseUrl, int page, int size) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "page=" + page + "&size=" + size;
    }

    /**
     * 检查是否应该加载更多数据
     */
    public static boolean shouldLoadMore(int dataSize, int pageSize) {
        return dataSize == pageSize;
    }

    /**
     * 构建带限制的URL（用于热门帖子等）
     */
    public static String buildLimitUrl(String baseUrl, int limit) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "limit=" + limit;
    }
}