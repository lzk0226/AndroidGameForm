package com.app.gameform.domain;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 游戏实体类
 * @version 1.0
 * @Author : SockLightDust
 */
public class Game {
    /** 游戏ID */
    private Integer gameId;

    /** 游戏名称 */
    private String gameName;

    /** 游戏简介 */
    private String gameDescription;

    /** 游戏类型ID */
    private Integer gameTypeId;

    /** 游戏类型名称 (关联查询) */
    private String gameTypeName;

    /** 游戏图标地址 */
    private String gameIcon;

    /** 游戏详情图片地址（多个图片用逗号分隔） */
    private String gameImages;

    /** 创建时间 */
    private Date createTime;

    /** 备注 */
    private String remark;

    // 构造函数
    public Game() {}

    // Getter 和 Setter 方法
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

    public String getGameDescription() {
        return gameDescription;
    }

    public void setGameDescription(String gameDescription) {
        this.gameDescription = gameDescription;
    }

    public Integer getGameTypeId() {
        return gameTypeId;
    }

    public void setGameTypeId(Integer gameTypeId) {
        this.gameTypeId = gameTypeId;
    }

    public String getGameTypeName() {
        return gameTypeName;
    }

    public void setGameTypeName(String gameTypeName) {
        this.gameTypeName = gameTypeName;
    }

    public String getGameIcon() {
        return gameIcon;
    }

    public void setGameIcon(String gameIcon) {
        this.gameIcon = gameIcon;
    }

    public String getGameImages() {
        return gameImages;
    }

    public void setGameImages(String gameImages) {
        this.gameImages = gameImages;
    }

    /**
     * 获取游戏详情图片列表
     * @return 图片地址列表
     */
    public List<String> getGameImageList() {
        if (gameImages == null || gameImages.trim().isEmpty()) {
            return Arrays.asList();
        }
        return Arrays.asList(gameImages.split(","));
    }

    /**
     * 设置游戏详情图片列表
     * @param imageList 图片地址列表
     */
    public void setGameImageList(List<String> imageList) {
        if (imageList == null || imageList.isEmpty()) {
            this.gameImages = null;
        } else {
            this.gameImages = String.join(",", imageList);
        }
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
        return "Game{" +
                "gameId=" + gameId +
                ", gameName='" + gameName + '\'' +
                ", gameDescription='" + gameDescription + '\'' +
                ", gameTypeId=" + gameTypeId +
                ", gameTypeName='" + gameTypeName + '\'' +
                ", gameIcon='" + gameIcon + '\'' +
                ", gameImages='" + gameImages + '\'' +
                ", createTime=" + createTime +
                ", remark='" + remark + '\'' +
                '}';
    }
}