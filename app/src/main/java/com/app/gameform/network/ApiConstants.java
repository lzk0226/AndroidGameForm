package com.app.gameform.network;

public class ApiConstants {
    public static final String BASE_URL = "http://110.41.1.63:8080";
    //public static final String BASE_URL = "http://10.0.2.2:8080";
    public static final String USER_PUBLIC_BASE_URL = BASE_URL + "/user/public/";
    public static final String POST_LIKE_URL = BASE_URL + "/user/post/like/";
    public static final String USER_UPDATE_PROFILE_URL = BASE_URL + "/update";
    public static final String USER_PROFILE = BASE_URL +"/user/profile";
    public static final String USER_POST = BASE_URL +"/user/post/";
    public static final String USER_POST_COMMENT = BASE_URL +"/user/comment/post/";
    public static final String USER_COMMENT = BASE_URL +"/user/comment";
    public static final String USER_COMMENT_LIKE = BASE_URL +"/user/comment/like/";
    public static final String POST_LIKE_STATUS_URL = BASE_URL + "/user/post/like/check/"; // 检查点赞状态接口，后面拼 postId

}