package com.chatapp.entity;

import java.time.LocalDateTime;

/**
 * 好友关系实体类
 * 对应数据库 friendships 表
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public class Friendship {
    
    private Long id;
    private Long userId;
    private Long friendId;
    private Integer status; // 关系状态: 0-待确认, 1-已确认, 2-已拉黑
    private String nickname; // 好友备注昵称
    private String groupName; // 好友分组
    private String requestMessage; // 好友申请消息
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    // 构造函数
    public Friendship() {
        this.status = 0;
        this.deleted = 0;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public Friendship(Long userId, Long friendId, String requestMessage) {
        this();
        this.userId = userId;
        this.friendId = friendId;
        this.requestMessage = requestMessage;
    }

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFriendId() {
        return friendId;
    }

    public void setFriendId(Long friendId) {
        this.friendId = friendId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getRequestMessage() {
        return requestMessage;
    }

    public void setRequestMessage(String requestMessage) {
        this.requestMessage = requestMessage;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "Friendship{" +
                "id=" + id +
                ", userId=" + userId +
                ", friendId=" + friendId +
                ", status=" + status +
                ", nickname='" + nickname + '\'' +
                ", groupName='" + groupName + '\'' +
                '}';
    }
}