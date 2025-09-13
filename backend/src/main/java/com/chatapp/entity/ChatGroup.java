package com.chatapp.entity;

import java.time.LocalDateTime;

/**
 * 群组实体类
 * 对应数据库 chat_groups 表
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public class ChatGroup {
    
    private Long id;
    private String groupName;
    private String groupDescription;
    private String groupAvatar;
    private byte[] groupAvatarData;        // 群头像二进制数据
    private String groupAvatarContentType; // 群头像MIME类型
    private Long ownerId;
    private Integer maxMembers;
    private Integer memberCount;
    private Integer muteAll;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    // 构造函数
    public ChatGroup() {
        this.maxMembers = 200;
        this.memberCount = 0;
        this.muteAll = 0;
        this.status = 1;
        this.deleted = 0;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public ChatGroup(String groupName, Long ownerId) {
        this();
        this.groupName = groupName;
        this.ownerId = ownerId;
    }

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupDescription() {
        return groupDescription;
    }

    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
    }

    public String getGroupAvatar() {
        return groupAvatar;
    }

    public void setGroupAvatar(String groupAvatar) {
        this.groupAvatar = groupAvatar;
    }

    public byte[] getGroupAvatarData() {
        return groupAvatarData;
    }

    public void setGroupAvatarData(byte[] groupAvatarData) {
        this.groupAvatarData = groupAvatarData;
    }

    public String getGroupAvatarContentType() {
        return groupAvatarContentType;
    }

    public void setGroupAvatarContentType(String groupAvatarContentType) {
        this.groupAvatarContentType = groupAvatarContentType;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public Integer getMuteAll() {
        return muteAll;
    }

    public void setMuteAll(Integer muteAll) {
        this.muteAll = muteAll;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
        return "ChatGroup{" +
                "id=" + id +
                ", groupName='" + groupName + '\'' +
                ", ownerId=" + ownerId +
                ", memberCount=" + memberCount +
                ", status=" + status +
                '}';
    }
}