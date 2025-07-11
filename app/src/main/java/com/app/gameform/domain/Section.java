package com.app.gameform.domain;

import java.util.Date;

/**
 * 版块实体类
 * @version 1.0
 * @Author : SockLightDust
 */
public class Section {
    /** 版块ID */
    private Integer sectionId;

    /** 版块名称 */
    private String sectionName;

    /** 版块描述 */
    private String sectionDescription;

    /** 关联游戏ID */
    private Integer gameId;

    /** 游戏名称 (关联查询) */
    private String gameName;

    /** 显示顺序 */
    private Integer orderNum;

    /** 创建时间 */
    private Date createTime;

    /** 备注 */
    private String remark;

    // 构造函数
    public Section() {}

    // Getter 和 Setter 方法
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

    public String getSectionDescription() {
        return sectionDescription;
    }

    public void setSectionDescription(String sectionDescription) {
        this.sectionDescription = sectionDescription;
    }

    public Integer getGameId() {
        return gameId;
    }

    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "Section{" +
                "sectionId=" + sectionId +
                ", sectionName='" + sectionName + '\'' +
                ", sectionDescription='" + sectionDescription + '\'' +
                ", gameId=" + gameId +
                ", gameName='" + gameName + '\'' +
                ", orderNum=" + orderNum +
                ", createTime=" + createTime +
                ", remark='" + remark + '\'' +
                '}';
    }
}
