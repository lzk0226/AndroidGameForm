package com.app.gameform.emuns;

/**
 * 响应状态码枚举
 * 适用于Android端API响应处理
 */
public enum ResultCodeEnum {

    /* 成功状态码 */
    SUCCESS(200, "操作成功"),

    /* 客户端错误：4xx */
    FAIL(400, "操作失败"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "访问被禁止"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    VALIDATION_ERROR(422, "参数校验失败"),

    /* 服务端错误：5xx */
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    /* 用户相关错误：1xxx */
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_NAME_EMPTY(1002, "用户名不能为空"),
    PASSWORD_EMPTY(1003, "密码不能为空"),
    NICK_NAME_EMPTY(1004, "昵称不能为空"),
    USER_NAME_EXISTS(1005, "用户名已存在"),
    EMAIL_EXISTS(1006, "邮箱已存在"),
    PHONE_EXISTS(1007, "手机号已存在"),
    LOGIN_FAILED(1008, "用户名或密码错误，或账号已被停用"),
    USER_ID_EMPTY(1009, "用户ID不能为空"),
    OLD_PASSWORD_EMPTY(1010, "旧密码不能为空"),
    NEW_PASSWORD_EMPTY(1011, "新密码不能为空"),
    PASSWORD_UPDATE_FAILED(1012, "密码修改失败，请检查旧密码是否正确"),

    /* 游戏相关错误：2xxx */
    GAME_NOT_FOUND(2001, "游戏不存在"),
    GAME_TYPE_NOT_FOUND(2002, "游戏类型不存在"),

    /* 论坛版块相关错误：3xxx */
    SECTION_NOT_FOUND(3001, "论坛版块不存在"),

    /* 认证相关错误：4xxx */
    TOKEN_INVALID(4001, "Token无效或已过期"),
    TOKEN_EXPIRED(4002, "Token已过期"),
    TOKEN_MISSING(4003, "Token缺失"),
    TOKEN_BLACKLISTED(4004, "Token已被注销"),

    /* 帖子相关错误：5xxx */
    POST_NOT_FOUND(5001, "帖子不存在"),
    POST_TITLE_EMPTY(5002, "帖子标题不能为空"),
    POST_CONTENT_EMPTY(5003, "帖子内容不能为空"),
    POST_SECTION_EMPTY(5004, "所属版块不能为空"),
    POST_TITLE_TOO_LONG(5005, "帖子标题过长"),
    POST_CONTENT_TOO_LONG(5006, "帖子内容过长"),
    POST_ALREADY_LIKED(5007, "您已经点赞过该帖子"),
    POST_NOT_LIKED(5008, "您还未点赞该帖子"),
    POST_PERMISSION_DENIED(5009, "您没有权限操作该帖子"),
    POST_ALREADY_DELETED(5010, "帖子已被删除"),
    POST_STATUS_DISABLED(5011, "帖子已被停用"),
    POST_ID_EMPTY(5012, "帖子ID不能为空"),

    /* 评论相关错误：6xxx */
    COMMENT_NOT_FOUND(6001, "评论不存在"),
    COMMENT_CONTENT_EMPTY(6002, "评论内容不能为空"),
    COMMENT_CONTENT_TOO_LONG(6003, "评论内容过长，最多5000字符"),
    COMMENT_ALREADY_LIKED(6004, "您已经点赞过该评论"),
    COMMENT_NOT_LIKED(6005, "您还未点赞该评论"),
    COMMENT_PERMISSION_DENIED(6006, "您没有权限操作该评论"),
    COMMENT_ALREADY_DELETED(6007, "评论已被删除"),
    COMMENT_STATUS_DISABLED(6008, "评论已被停用"),
    COMMENT_ID_EMPTY(6009, "评论ID不能为空"),
    COMMENT_PARENT_NOT_FOUND(6010, "父评论不存在"),
    COMMENT_PARENT_DISABLED(6011, "父评论不可用"),
    COMMENT_POST_MISMATCH(6012, "父评论和子评论必须在同一帖子下"),
    COMMENT_SELF_LIKE_ERROR(6013, "不能给自己的评论点赞"),

    /* 网络相关错误：7xxx */
    NETWORK_ERROR(7001, "网络连接失败"),
    NETWORK_TIMEOUT(7002, "网络请求超时"),
    NETWORK_UNAVAILABLE(7003, "网络不可用"),

    /* 本地存储相关错误：8xxx */
    STORAGE_ERROR(8001, "本地存储错误"),
    STORAGE_FULL(8002, "存储空间不足"),
    FILE_NOT_FOUND(8003, "文件不存在"),

    /* 业务操作相关：9xxx */
    REGISTER_SUCCESS(9001, "注册成功"),
    REGISTER_FAILED(9002, "注册失败"),
    UPDATE_SUCCESS(9003, "修改成功"),
    UPDATE_FAILED(9004, "修改失败"),
    PASSWORD_UPDATE_SUCCESS(9005, "密码修改成功"),
    ACCOUNT_DEACTIVATE_SUCCESS(9006, "账户注销成功"),
    ACCOUNT_DEACTIVATE_FAILED(9007, "账户注销失败"),
    POST_PUBLISH_SUCCESS(9008, "帖子发布成功"),
    POST_PUBLISH_FAILED(9009, "帖子发布失败"),
    POST_UPDATE_SUCCESS(9010, "帖子修改成功"),
    POST_UPDATE_FAILED(9011, "帖子修改失败"),
    POST_DELETE_SUCCESS(9012, "帖子删除成功"),
    POST_DELETE_FAILED(9013, "帖子删除失败"),
    POST_LIKE_SUCCESS(9014, "点赞成功"),
    POST_LIKE_FAILED(9015, "点赞失败"),
    POST_UNLIKE_SUCCESS(9016, "取消点赞成功"),
    POST_UNLIKE_FAILED(9017, "取消点赞失败"),

    /* 评论操作成功相关：91xx */
    COMMENT_PUBLISH_SUCCESS(9101, "评论发布成功"),
    COMMENT_PUBLISH_FAILED(9102, "评论发布失败"),
    COMMENT_UPDATE_SUCCESS(9103, "评论修改成功"),
    COMMENT_UPDATE_FAILED(9104, "评论修改失败"),
    COMMENT_DELETE_SUCCESS(9105, "评论删除成功"),
    COMMENT_DELETE_FAILED(9106, "评论删除失败"),
    COMMENT_LIKE_SUCCESS(9107, "评论点赞成功"),
    COMMENT_LIKE_FAILED(9108, "评论点赞失败"),
    COMMENT_UNLIKE_SUCCESS(9109, "取消评论点赞成功"),
    COMMENT_UNLIKE_FAILED(9110, "取消评论点赞失败"),
    COMMENT_BATCH_DELETE_SUCCESS(9111, "批量删除评论成功"),
    COMMENT_BATCH_DELETE_FAILED(9112, "批量删除评论失败");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据code获取枚举
     * @param code 状态码
     * @return 对应的枚举值，如果不存在返回null
     */
    public static ResultCodeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ResultCodeEnum item : ResultCodeEnum.values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 判断是否为成功状态码
     * @return true表示成功，false表示失败
     */
    public boolean isSuccess() {
        return SUCCESS.code.equals(this.code);
    }

    /**
     * 判断是否为失败状态码
     * @return true表示失败，false表示成功
     */
    public boolean isFailed() {
        return !isSuccess();
    }

    /**
     * 判断是否为网络相关错误
     * @return true表示网络错误
     */
    public boolean isNetworkError() {
        return this.code >= 7001 && this.code <= 7999;
    }

    /**
     * 判断是否为服务器错误
     * @return true表示服务器错误
     */
    public boolean isServerError() {
        return this.code >= 500 && this.code <= 599;
    }
}