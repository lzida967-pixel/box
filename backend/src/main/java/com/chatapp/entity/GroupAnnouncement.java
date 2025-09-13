package com.chatapp.entity;

import java.time.LocalDateTime;

/**
 * 群公告实体类
 * 对应数据库 group_announcements 表
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public class GroupAnnouncement {
    
    private Long id;
    private Long groupId;
    private Long publisherId;
    private String title;
    private String content;
    private Integer isPinned;
    private Integer status;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    // 构造函数
    public GroupAnnouncement() {
        this.isPinned = 0;
        this.status = 1;
        this.deleted = 0;
        this.publishTime = LocalDateTime.now();
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public GroupAnnouncement(Long groupId, Long publisherId, String title, String content) {
        this();
        this.groupId = groupId;
        this.publisherId = publisherId;
        this.title = title;
        this.content = content;
    }

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

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

    public Integer getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(Integer isPinned) {
        this.isPinned = isPinned;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
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
        return "GroupAnnouncement{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", publisherId=" + publisherId +
                ", title='" + title + '\'' +
                ", status=" + status +
                '}';
    }
}