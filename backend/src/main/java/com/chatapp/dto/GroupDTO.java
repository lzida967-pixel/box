package com.chatapp.dto;

import com.chatapp.entity.ChatGroup;
import com.chatapp.entity.GroupMember;
import com.chatapp.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群组数据传输对象
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public class GroupDTO {
    
    private Long id;
    private String groupName;
    private String groupDescription;
    private String groupAvatar;
    private Long ownerId;
    private String ownerName;
    private Integer maxMembers;
    private Integer memberCount;
    private Integer muteAll;
    private Integer status;
    private LocalDateTime createTime;
    
    // 当前用户在群内的信息
    private Integer myRole;           // 我的角色
    private String myNickname;        // 我在群内的昵称
    private String myRemark;          // 我对这个群的备注
    private LocalDateTime myJoinTime; // 我的加入时间
    private LocalDateTime myMuteUntil; // 我的禁言到期时间
    
    // 群成员列表（可选）
    private List<GroupMemberDTO> members;
    
    // 最新公告（可选）
    private String latestAnnouncement;

    // 构造函数
    public GroupDTO() {}

    public GroupDTO(ChatGroup group) {
        this.id = group.getId();
        this.groupName = group.getGroupName();
        this.groupDescription = group.getGroupDescription();
        this.groupAvatar = group.getGroupAvatar();
        this.ownerId = group.getOwnerId();
        this.maxMembers = group.getMaxMembers();
        this.memberCount = group.getMemberCount();
        this.muteAll = group.getMuteAll();
        this.status = group.getStatus();
        this.createTime = group.getCreateTime();
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

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
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

    public Integer getMyRole() {
        return myRole;
    }

    public void setMyRole(Integer myRole) {
        this.myRole = myRole;
    }

    public String getMyNickname() {
        return myNickname;
    }

    public void setMyNickname(String myNickname) {
        this.myNickname = myNickname;
    }

    public String getMyRemark() {
        return myRemark;
    }

    public void setMyRemark(String myRemark) {
        this.myRemark = myRemark;
    }

    public LocalDateTime getMyJoinTime() {
        return myJoinTime;
    }

    public void setMyJoinTime(LocalDateTime myJoinTime) {
        this.myJoinTime = myJoinTime;
    }

    public LocalDateTime getMyMuteUntil() {
        return myMuteUntil;
    }

    public void setMyMuteUntil(LocalDateTime myMuteUntil) {
        this.myMuteUntil = myMuteUntil;
    }

    public List<GroupMemberDTO> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMemberDTO> members) {
        this.members = members;
    }

    public String getLatestAnnouncement() {
        return latestAnnouncement;
    }

    public void setLatestAnnouncement(String latestAnnouncement) {
        this.latestAnnouncement = latestAnnouncement;
    }

    /**
     * 群成员DTO
     */
    public static class GroupMemberDTO {
        private Long id;
        private Long userId;
        private String username;
        private String nickname;
        private String avatar;
        private Integer memberRole;
        private String memberNickname;
        private LocalDateTime joinTime;
        private Long inviteUserId;
        private String inviteUserName;
        private LocalDateTime muteUntil;
        private Integer status;

        // 构造函数
        public GroupMemberDTO() {}

        public GroupMemberDTO(GroupMember member, User user) {
            this.id = member.getId();
            this.userId = member.getUserId();
            this.memberRole = member.getMemberRole();
            this.memberNickname = member.getMemberNickname();
            this.joinTime = member.getJoinTime();
            this.inviteUserId = member.getInviteUserId();
            this.muteUntil = member.getMuteUntil();
            this.status = member.getStatus();
            
            if (user != null) {
                this.username = user.getUsername();
                this.nickname = user.getNickname();
                this.avatar = user.getAvatar();
            }
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

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
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

        public String getInviteUserName() {
            return inviteUserName;
        }

        public void setInviteUserName(String inviteUserName) {
            this.inviteUserName = inviteUserName;
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
    }
}