package com.app.gameform.domain;

import java.util.Date;

/**
 * 用户关注关系实体类
 */
public class UserFollow {

    private Long followId;           // 关注关系ID
    private Long userId;             // 关注者用户ID
    private Long followingId;        // 被关注者用户ID
    private Long followerId;         // 粉丝用户ID
    private Date createTime;         // 创建时间

    // 被关注者信息
    private String followingUserName;
    private String followingNickName;
    private String followingAvatar;

    // 粉丝信息
    private String followerUserName;
    private String followerNickName;
    private String followerAvatar;

    public UserFollow() {
    }

    // Getters and Setters

    public Long getFollowId() {
        return followId;
    }

    public void setFollowId(Long followId) {
        this.followId = followId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFollowingId() {
        return followingId;
    }

    public void setFollowingId(Long followingId) {
        this.followingId = followingId;
    }

    public Long getFollowerId() {
        return followerId;
    }

    public void setFollowerId(Long followerId) {
        this.followerId = followerId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getFollowingUserName() {
        return followingUserName;
    }

    public void setFollowingUserName(String followingUserName) {
        this.followingUserName = followingUserName;
    }

    public String getFollowingNickName() {
        return followingNickName;
    }

    public void setFollowingNickName(String followingNickName) {
        this.followingNickName = followingNickName;
    }

    public String getFollowingAvatar() {
        return followingAvatar;
    }

    public void setFollowingAvatar(String followingAvatar) {
        this.followingAvatar = followingAvatar;
    }

    public String getFollowerUserName() {
        return followerUserName;
    }

    public void setFollowerUserName(String followerUserName) {
        this.followerUserName = followerUserName;
    }

    public String getFollowerNickName() {
        return followerNickName;
    }

    public void setFollowerNickName(String followerNickName) {
        this.followerNickName = followerNickName;
    }

    public String getFollowerAvatar() {
        return followerAvatar;
    }

    public void setFollowerAvatar(String followerAvatar) {
        this.followerAvatar = followerAvatar;
    }
}