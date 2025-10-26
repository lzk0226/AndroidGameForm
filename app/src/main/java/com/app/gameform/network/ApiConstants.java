package com.app.gameform.network;

public class ApiConstants {
    //public static final String BASE_URL = "http://110.41.1.63:8080";
    public static final String BASE_URL = "http://10.0.2.2:8080";
    //public static final String BASE_URL = "http://192.168.124.17:8080";

    // ================== 现有接口 ==================
    public static final String USER_PUBLIC_BASE_URL = BASE_URL + "/user/public/";
    public static final String POST_LIKE_URL = BASE_URL + "/user/post/like/";
    public static final String USER_UPDATE_PROFILE_URL = BASE_URL + "/update";
    public static final String USER_PROFILE = BASE_URL + "/user/profile";
    public static final String USER_POST = BASE_URL + "/user/post/";
    public static final String USER_POST_COMMENT = BASE_URL + "/user/comment/post/";
    public static final String USER_COMMENT = BASE_URL + "/user/comment";
    public static final String USER_COMMENT_LIKE = BASE_URL + "/user/comment/like/";
    public static final String POST_LIKE_STATUS_URL = BASE_URL + "/user/post/like/check/";
    public static final String GET_ALL_GAME_TYPES = BASE_URL + "/user/gameType/all";
    public static final String GET_ALL_GAMES = BASE_URL + "/user/game/list";
    public static final String GET_ALL_SECTIONS = BASE_URL + "/user/section/all";
    public static final String UPLOAD_POST_IMAGE = BASE_URL + "/user/upload/save-post-image";

    // 游戏详情
    public static final String GET_GAME_DETAIL = BASE_URL + "/user/game/";

    // 热门游戏列表
    public static final String GET_HOT_GAMES = BASE_URL + "/user/game/hot";

    // 根据类型获取游戏列表
    public static final String GET_GAMES_BY_TYPE = BASE_URL + "/user/game/type/";

    // 搜索游戏
    public static final String SEARCH_GAMES = BASE_URL + "/user/game/search";

    // 游戏类型搜索
    public static final String SEARCH_GAME_TYPES = BASE_URL + "/user/gameType/search";

    // 版块详情
    public static final String GET_SECTION_DETAIL = BASE_URL + "/user/section/";

    // 版块列表
    public static final String GET_SECTION_LIST = BASE_URL + "/user/section/list";

    // 热门版块
    public static final String GET_HOT_SECTIONS = BASE_URL + "/user/section/hot";

    // 根据游戏ID获取版块列表
    public static final String GET_SECTIONS_BY_GAME = BASE_URL + "/user/section/game/";

    // 搜索版块
    public static final String SEARCH_SECTIONS = BASE_URL + "/user/section/search";

    // 根据版块ID获取游戏ID
    public static final String GET_GAME_ID_BY_SECTION = BASE_URL + "/user/section/gameId/";

    // 帖子列表
    public static final String GET_POST_LIST = BASE_URL + "/user/post/list";

    // 帖子详情
    public static final String GET_POST_DETAIL = BASE_URL + "/user/post/";

    // 根据版块获取帖子
    public static final String GET_POSTS_BY_SECTION = BASE_URL + "/user/post/section/";

    // 热门帖子
    public static final String GET_HOT_POSTS = BASE_URL + "/user/post/hot";

    // 置顶帖子
    public static final String GET_TOP_POSTS = BASE_URL + "/user/post/top";

    // 我的帖子
    public static final String GET_MY_POSTS = BASE_URL + "/user/post/my";

    // 根据用户获取帖子
    public static final String GET_POSTS_BY_USER = BASE_URL + "/user/post/user/";

    // 搜索帖子
    public static final String SEARCH_POSTS = BASE_URL + "/user/post/search";

    // 创建帖子
    public static final String CREATE_POST = BASE_URL + "/user/post";

    // 增加浏览量
    public static final String INCREMENT_VIEW_COUNT = BASE_URL + "/user/post/";

    // 点赞帖子
    public static final String CREATE_POST_LIKE = BASE_URL + "/user/post/like/";

    // 取消点赞帖子
    public static final String DELETE_POST_LIKE = BASE_URL + "/user/post/like/";

    // ================== 新增：个性化推荐接口 ==================

    // 个性化推荐帖子 - 基于协同过滤算法
    public static final String GET_PERSONALIZED_RECOMMENDATIONS = BASE_URL + "/user/post/recommendations";

    // 基于内容的推荐帖子
    public static final String GET_CONTENT_BASED_RECOMMENDATIONS = BASE_URL + "/user/post/recommendations/content";

    // 混合推荐帖子（协同过滤 + 基于内容）
    public static final String GET_HYBRID_RECOMMENDATIONS = BASE_URL + "/user/post/recommendations/hybrid";

    // 获取推荐帖子的详细信息（包括推荐类型和原因）
    public static final String GET_DETAILED_RECOMMENDATIONS = BASE_URL + "/user/post/recommendations/detailed";

    // 获取帖子评论
    public static final String GET_POST_COMMENTS = BASE_URL + "/user/comment/post/";

    // 创建评论
    public static final String CREATE_COMMENT = BASE_URL + "/user/comment";

    // 删除评论
    public static final String DELETE_COMMENT = BASE_URL + "/user/comment/";

    // 点赞评论
    public static final String TOGGLE_COMMENT_LIKE = BASE_URL + "/user/comment/like/";

    // 获取用户详情
    public static final String GET_USER_PROFILE_DETAIL = BASE_URL + "/user/profile/";

    // 更新用户资料
    public static final String UPDATE_USER_PROFILE = BASE_URL + "/user/profile/update";

    // 更新密码
    public static final String UPDATE_PASSWORD = BASE_URL + "/user/profile/updatePassword";

    // 注销账户
    public static final String DEACTIVATE_ACCOUNT = BASE_URL + "/user/profile/deactivate/";

    // 上传头像
    public static final String UPLOAD_AVATAR = BASE_URL + "/user/upload/save-avatar";

    // 获取公告列表
    public static final String GET_ANNOUNCEMENTS = BASE_URL + "/user/announcements/list";

    // 获取图片资源
    public static final String GET_PHOTOS = BASE_URL + "/user/public/";
    public static final String GET_POST_PHOTOS = BASE_URL + "/user/public/";
    public static final String GET_GAME_ICON = BASE_URL + "/user/public/";
    public static final String GET_SECTION_ICON = BASE_URL + "/user/public/";
    public static final String GET_GAME_PHOTO = BASE_URL + "/user/public/";

    // ================== 新增：帖子收藏相关接口 ==================

    // 添加收藏
    public static final String ADD_POST_FAVORITE = BASE_URL + "/user/post/favorite/";

    // 取消收藏
    public static final String REMOVE_POST_FAVORITE = BASE_URL + "/user/post/favorite/";

    // 检查收藏状态
    public static final String CHECK_POST_FAVORITE_STATUS = BASE_URL + "/user/post/favorite/check/";

    // 获取我的收藏列表
    public static final String GET_MY_FAVORITES = BASE_URL + "/user/post/favorite/my";

    // 获取帖子收藏数量
    public static final String GET_POST_FAVORITE_COUNT = BASE_URL + "/user/post/favorite/count/post/";

    // ================== 新增：用户关注相关接口 ==================

    // 关注用户
    public static final String FOLLOW_USER = BASE_URL + "/user/follow/";

    // 取消关注用户
    public static final String UNFOLLOW_USER = BASE_URL + "/user/follow/";

    // 检查关注状态
    public static final String CHECK_FOLLOW_STATUS = BASE_URL + "/user/follow/check/";

    // 获取我的关注列表
    public static final String GET_MY_FOLLOWING = BASE_URL + "/user/follow/following/my";

    // 获取我的粉丝列表
    public static final String GET_MY_FOLLOWERS = BASE_URL + "/user/follow/follower/my";

    // 获取指定用户的粉丝列表
    public static final String GET_USER_FOLLOWERS = BASE_URL + "/user/follow/follower/";

    // 获取指定用户的关注列表
    public static final String GET_USER_FOLLOWING = BASE_URL + "/user/follow/following/";

    // ================== 工具方法 ==================

    /**
     * 构建带参数的URL
     */
    public static String buildUrlWithParam(String baseUrl, String param) {
        return baseUrl + param;
    }

    /**
     * 构建搜索URL
     */
    public static String buildSearchUrl(String baseUrl, String keyword) {
        return baseUrl + "?name=" + keyword;
    }

    /**
     * 构建搜索帖子URL
     */
    public static String buildSearchPostUrl(String keyword) {
        return SEARCH_POSTS + "?title=" + keyword;
    }

    /**
     * 构建热门游戏URL
     */
    public static String buildHotGamesUrl(int limit) {
        return GET_HOT_GAMES + "?limit=" + limit;
    }

    /**
     * 构建热门版块URL
     */
    public static String buildHotSectionsUrl(int limit) {
        return GET_HOT_SECTIONS + "?limit=" + limit;
    }

    /**
     * 构建个性化推荐URL
     */
    public static String buildPersonalizedRecommendationsUrl(int limit) {
        return GET_PERSONALIZED_RECOMMENDATIONS + "?limit=" + limit;
    }

    /**
     * 构建内容推荐URL
     */
    public static String buildContentBasedRecommendationsUrl(int limit) {
        return GET_CONTENT_BASED_RECOMMENDATIONS + "?limit=" + limit;
    }

    /**
     * 构建混合推荐URL
     */
    public static String buildHybridRecommendationsUrl(int limit, int page) {
        return BASE_URL + "/user/post/recommendations/hybrid?limit=" + limit + "&page=" + page;
    }

    /**
     * 构建详细推荐URL
     */
    public static String buildDetailedRecommendationsUrl(int limit) {
        return GET_DETAILED_RECOMMENDATIONS + "?limit=" + limit;
    }

    /**
     * 获取完整的图片URL
     */
    public static String getFullImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return "";
        }

        // 如果已经是完整URL，直接返回
        if (imagePath.startsWith("http")) {
            return imagePath;
        }

        // 处理路径中的反斜杠
        String cleanPath = imagePath.replace("\\", "/");
        return USER_PUBLIC_BASE_URL + cleanPath;
    }
}