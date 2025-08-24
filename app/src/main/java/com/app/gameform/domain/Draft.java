package com.app.gameform.domain;

import java.util.Date;

/**
 * 草稿实体类
 * @version 1.0
 * @Author : SockLightDust
 */
public class Draft {
    /** 草稿ID */
    private Integer draftId;

    /** 草稿标题 */
    private String draftTitle;

    /** 草稿内容 */
    private String draftContent;

    /** 所属版块ID */
    private Integer sectionId;

    /** 版块名称 (关联查询) */
    private String sectionName;

    /** 草稿图片 */
    private String draftImages;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 用户ID */
    private Long userId;

    /** 备注 */
    private String remark;

    // 构造函数
    public Draft() {}

    // Getter 和 Setter 方法
    public Integer getDraftId() {
        return draftId;
    }

    public void setDraftId(Integer draftId) {
        this.draftId = draftId;
    }

    public String getDraftTitle() {
        return draftTitle;
    }

    public void setDraftTitle(String draftTitle) {
        this.draftTitle = draftTitle;
    }

    public String getDraftContent() {
        return draftContent;
    }

    public void setDraftContent(String draftContent) {
        this.draftContent = draftContent;
    }

    public Integer getSectionId() {
        return sectionId;
    }

    public void setSectionId(Integer sectionId) {
        this.sectionId = sectionId;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getDraftImages() {
        return draftImages;
    }

    public void setDraftImages(String draftImages) {
        this.draftImages = draftImages;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 获取显示标题（如果标题为空则返回默认标题）
     * @return 显示标题
     */
    public String getDisplayTitle() {
        if (draftTitle == null || draftTitle.trim().isEmpty()) {
            return "未命名草稿" + (draftId != null ? draftId : "");
        }
        return draftTitle;
    }

    @Override
    public String toString() {
        return "Draft{" +
                "draftId=" + draftId +
                ", draftTitle='" + draftTitle + '\'' +
                ", draftContent='" + draftContent + '\'' +
                ", sectionId=" + sectionId +
                ", sectionName='" + sectionName + '\'' +
                ", draftImages='" + draftImages + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", userId=" + userId +
                ", remark='" + remark + '\'' +
                '}';
    }
}