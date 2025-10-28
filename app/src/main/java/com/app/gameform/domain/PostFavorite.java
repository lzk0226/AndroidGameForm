package com.app.gameform.domain;
import java.io.Serializable;
import java.util.Date;

/**
 * 帖子收藏对象 biz_post_favorite
 *
 * @author SockLightDust
 * @date 2025-10-19
 */
public class PostFavorite implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 帖子ID */
    private Integer postId;

    /** 收藏时间 */
    private Date createTime;

    // 扩展字段（用于显示）
    /** 用户昵称 */
    private String nickName;

    /** 用户头像 */
    private String avatar;

    /** 帖子标题 */
    private String postTitle;

    /** 帖子内容 */
    private String postContent;

    /** 帖子作者昵称 */
    private String postAuthorName;

    /** 版块名称 */
    private String sectionName;

    /** 帖子预览图 */
    private String photo;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public String getPostContent() {
        return postContent;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }

    public String getPostAuthorName() {
        return postAuthorName;
    }

    public void setPostAuthorName(String postAuthorName) {
        this.postAuthorName = postAuthorName;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    @Override
    public String toString() {
        return "PostFavorite{" +
                "userId=" + userId +
                ", postId=" + postId +
                ", createTime=" + createTime +
                ", nickName='" + nickName + '\'' +
                ", avatar='" + avatar + '\'' +
                ", postTitle='" + postTitle + '\'' +
                ", postContent='" + postContent + '\'' +
                ", postAuthorName='" + postAuthorName + '\'' +
                ", sectionName='" + sectionName + '\'' +
                ", photo='" + photo + '\'' +
                '}';
    }
}
