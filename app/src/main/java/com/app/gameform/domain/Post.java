package com.app.gameform.domain;

import java.util.Date;

/**
 * 论坛帖子对象
 * @author SockLightDust
 * @date 2025-05-24
 */
public class Post {
    /** 帖子ID */
    private Integer postId;

    /** 帖子标题 */
    private String postTitle;

    /** 帖子内容 */
    private String postContent;

    /** 发帖用户ID */
    private Long userId;

    /** 所属版块ID */
    private Integer sectionId;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;

    /** 浏览数 */
    private Integer viewCount;

    /** 置顶标志（0不置顶 1置顶） */
    private String topFlag;

    /** 热门标志（0不热门 1热门） */
    private String hotFlag;

    /** 状态（0正常 1停用） */
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;

    // 关联信息（用于前端显示）
    /** 版块名称 */
    private String sectionName;

    /** 用户昵称 */
    private String nickName;

    /** 用户头像 */
    private String avatar;

    /** 帖子图片 */
    private String photo;

    public Post() {}

    // Getter 和 Setter 方法
    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getSectionId() {
        return sectionId;
    }

    public void setSectionId(Integer sectionId) {
        this.sectionId = sectionId;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public String getTopFlag() {
        return topFlag;
    }

    public void setTopFlag(String topFlag) {
        this.topFlag = topFlag;
    }

    public String getHotFlag() {
        return hotFlag;
    }

    public void setHotFlag(String hotFlag) {
        this.hotFlag = hotFlag;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
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

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    @Override
    public String toString() {
        return "Post{" +
                "postId=" + postId +
                ", postTitle='" + postTitle + '\'' +
                ", postContent='" + postContent + '\'' +
                ", userId=" + userId +
                ", sectionId=" + sectionId +
                ", likeCount=" + likeCount +
                ", commentCount=" + commentCount +
                ", viewCount=" + viewCount +
                ", topFlag='" + topFlag + '\'' +
                ", hotFlag='" + hotFlag + '\'' +
                ", status='" + status + '\'' +
                ", delFlag='" + delFlag + '\'' +
                ", createBy='" + createBy + '\'' +
                ", createTime=" + createTime +
                ", updateBy='" + updateBy + '\'' +
                ", updateTime=" + updateTime +
                ", remark='" + remark + '\'' +
                ", sectionName='" + sectionName + '\'' +
                ", nickName='" + nickName + '\'' +
                ", avatar='" + avatar + '\'' +
                ", photo='" + photo + '\'' +
                '}';
    }
}