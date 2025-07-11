package com.app.gameform.domain;

import java.util.Date;

/**
 * 游戏类型实体类
 * @version 1.0
 * @Author : SockLightDust
 */
public class GameType {
    /** 类型ID */
    private Integer typeId;

    /** 类型名称 */
    private String typeName;

    /** 创建时间 */
    private Date createTime;

    /** 备注 */
    private String remark;

    // 构造函数
    public GameType() {}

    // Getter 和 Setter 方法
    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
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
        return "GameType{" +
                "typeId=" + typeId +
                ", typeName='" + typeName + '\'' +
                ", createTime=" + createTime +
                ", remark='" + remark + '\'' +
                '}';
    }
}