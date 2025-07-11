package com.app.gameform.domain;

import java.util.Date;

/**
 * 公告实体类
 * @version 1.0
 * @Author : SockLightDust
 */
public class Announcement {
    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 发布时间 */
    private Date publishTime;

    /** 图片路径 */
    private String photo;

    /** 发布者ID */
    private Integer issuerId;

    // 构造方法
    public Announcement() {}

    public Announcement(String title, String content, Date publishTime, String photo, Integer issuerId) {
        this.title = title;
        this.content = content;
        this.publishTime = publishTime;
        this.photo = photo;
        this.issuerId = issuerId;
    }

    // Getter 和 Setter 方法
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(Date publishTime) {
        this.publishTime = publishTime;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public Integer getIssuerId() {
        return issuerId;
    }

    public void setIssuerId(Integer issuerId) {
        this.issuerId = issuerId;
    }

    @Override
    public String toString() {
        return "Announcement{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", publishTime=" + publishTime +
                ", photo='" + photo + '\'' +
                ", issuerId=" + issuerId +
                '}';
    }
}