package com.app.gameform.network;

public class ApiConstants {
    public static final String BASE_URL = "http://110.41.1.63:8080";
    //public static final String BASE_URL = "http://10.0.2.2:8080";
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
    public static final String POST_LIKE_STATUS_URL = BASE_URL + "/user/post/like/check/"; // 检查点赞状态接口，后面拼 postId
    public static final String GET_ALL_GAME_TYPES = BASE_URL + "/user/gameType/all";
    public static final String GET_ALL_GAMES = BASE_URL + "/user/game/list";
    public static final String GET_ALL_SECTIONS = BASE_URL + "/user/section/all";
    public static final String UPLOAD_POST_IMAGE = BASE_URL + "/user/upload/save-post-image";

    // ================== 缺少的游戏相关接口 ==================
    // 游戏详情
    public static final String GET_GAME_DETAIL = BASE_URL + "/user/game/"; // + gameId

    // 热门游戏列表
    public static final String GET_HOT_GAMES = BASE_URL + "/user/game/hot"; // ?limit=数量

    // 根据类型获取游戏列表
    public static final String GET_GAMES_BY_TYPE = BASE_URL + "/user/game/type/"; // + gameTypeId

    // 搜索游戏
    public static final String SEARCH_GAMES = BASE_URL + "/user/game/search"; // ?name=游戏名称

    // 游戏类型搜索
    public static final String SEARCH_GAME_TYPES = BASE_URL + "/user/gameType/search"; // ?name=类型名称

    // ================== 缺少的版块相关接口 ==================
    // 版块详情
    public static final String GET_SECTION_DETAIL = BASE_URL + "/user/section/"; // + sectionId

    // 版块列表
    public static final String GET_SECTION_LIST = BASE_URL + "/user/section/list";

    // 热门版块
    public static final String GET_HOT_SECTIONS = BASE_URL + "/user/section/hot"; // ?limit=数量

    // 根据游戏ID获取版块列表
    public static final String GET_SECTIONS_BY_GAME = BASE_URL + "/user/section/game/"; // + gameId

    // 搜索版块
    public static final String SEARCH_SECTIONS = BASE_URL + "/user/section/search"; // ?name=版块名称

    // 根据版块ID获取游戏ID
    public static final String GET_GAME_ID_BY_SECTION = BASE_URL + "/user/section/gameId/"; // + sectionId

    // ================== 缺少的帖子相关接口 ==================
    // 帖子列表
    public static final String GET_POST_LIST = BASE_URL + "/user/post/list";

    // 帖子详情
    public static final String GET_POST_DETAIL = BASE_URL + "/user/post/"; // + postId

    // 根据版块获取帖子
    public static final String GET_POSTS_BY_SECTION = BASE_URL + "/user/post/section/"; // + sectionId

    // 热门帖子
    public static final String GET_HOT_POSTS = BASE_URL + "/user/post/hot";

    // 置顶帖子
    public static final String GET_TOP_POSTS = BASE_URL + "/user/post/top";

    // 我的帖子
    public static final String GET_MY_POSTS = BASE_URL + "/user/post/my";

    // 根据用户获取帖子
    public static final String GET_POSTS_BY_USER = BASE_URL + "/user/post/user/"; // + userId

    // 搜索帖子
    public static final String SEARCH_POSTS = BASE_URL + "/user/post/search"; // ?title=标题

    // 创建帖子
    public static final String CREATE_POST = BASE_URL + "/user/post";

    // 增加浏览量
    public static final String INCREMENT_VIEW_COUNT = BASE_URL + "/user/post/"; // + postId (GET请求)

    // 点赞帖子
    public static final String CREATE_POST_LIKE = BASE_URL + "/user/post/like/"; // + postId (POST)

    // 取消点赞帖子
    public static final String DELETE_POST_LIKE = BASE_URL + "/user/post/like/"; // + postId (DELETE)

    // ================== 缺少的评论相关接口 ==================
    // 获取帖子评论
    public static final String GET_POST_COMMENTS = BASE_URL + "/user/comment/post/"; // + postId

    // 创建评论
    public static final String CREATE_COMMENT = BASE_URL + "/user/comment";

    // 删除评论
    public static final String DELETE_COMMENT = BASE_URL + "/user/comment/"; // + commentId

    // 点赞评论
    public static final String TOGGLE_COMMENT_LIKE = BASE_URL + "/user/comment/like/"; // + commentId

    // ================== 缺少的用户相关接口 ==================
    // 获取用户详情
    public static final String GET_USER_PROFILE_DETAIL = BASE_URL + "/user/profile/"; // + userId

    // 更新用户资料
    public static final String UPDATE_USER_PROFILE = BASE_URL + "/user/profile/update";

    // 更新密码
    public static final String UPDATE_PASSWORD = BASE_URL + "/user/profile/updatePassword";

    // 注销账户
    public static final String DEACTIVATE_ACCOUNT = BASE_URL + "/user/profile/deactivate/"; // + userId

    // 上传头像
    public static final String UPLOAD_AVATAR = BASE_URL + "/user/upload/save-avatar";

    // ================== 缺少的公告相关接口 ==================
    // 获取公告列表
    public static final String GET_ANNOUNCEMENTS = BASE_URL + "/user/announcements/list";

    // ================== 缺少的图片资源接口 ==================
    // 获取图片资源
    public static final String GET_PHOTOS = BASE_URL + "/user/public/";
    public static final String GET_POST_PHOTOS = BASE_URL + "/user/public/";
    public static final String GET_GAME_ICON = BASE_URL + "/user/public/";
    public static final String GET_SECTION_ICON = BASE_URL + "/user/public/";
    public static final String GET_GAME_PHOTO = BASE_URL + "/user/public/";

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