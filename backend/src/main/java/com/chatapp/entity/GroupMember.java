package com.chatapp.entity;

import java.time.LocalDateTime;

/**
 * 群成员实体类
 * 对应数据库 group_members 表
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public class GroupMember {
    
    private Long id;
    private Long groupId;
    private Long userId;
    private Integer memberRole;      // 成员角色: 1-普通成员, 2-管理员, 3-群主
    private String memberNickname;   // 群内昵称
    private String remark;           // 我对这个群的备注（个人可见）
    private LocalDateTime joinTime;
    private Long inviteUserId;       // 邀请人ID
    private Boolean isMuted;         // 是否被禁言
    private LocalDateTime muteUntil; // 禁言到期时间
    private Integer status;          // 成员状态: 0-已退出, 1-正常, 2-被踢出
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    // 构造函数
    public GroupMember() {
        this.memberRole = 1;  // 默认普通成员
        this.status = 1;      // 默认正常状态
        this.isMuted = false; // 默认未禁言
        this.deleted = 0;
        this.joinTime = LocalDateTime.now();
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public GroupMember(Long groupId, Long userId, Integer memberRole) {
        this();
        this.groupId = groupId;
        this.userId = userId;
        this.memberRole = memberRole;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(Integer memberRole) {
        this.memberRole = memberRole;
    }

    public String getMemberNickname() {
        return memberNickname;
    }

    public void setMemberNickname(String memberNickname) {
        this.memberNickname = memberNickname;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(LocalDateTime joinTime) {
        this.joinTime = joinTime;
    }

    public Long getInviteUserId() {
        return inviteUserId;
    }

    public void setInviteUserId(Long inviteUserId) {
        this.inviteUserId = inviteUserId;
    }

    public Boolean getIsMuted() {
        return isMuted;
    }

    public void setIsMuted(Boolean isMuted) {
        this.isMuted = isMuted;
    }

    public LocalDateTime getMuteUntil() {
        return muteUntil;
    }

    public void setMuteUntil(LocalDateTime muteUntil) {
        this.muteUntil = muteUntil;
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
        return "GroupMember{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", userId=" + userId +
                ", memberRole=" + memberRole +
                ", status=" + status +
                '}';
    }
}