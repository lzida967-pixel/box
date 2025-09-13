package com.chatapp.service;

import com.chatapp.dto.GroupDTO;
import com.chatapp.entity.ChatGroup;
import com.chatapp.entity.GroupAnnouncement;
import com.chatapp.entity.GroupMember;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群组服务接口
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public interface GroupService {

    // ==================== 群组管理 ====================

    /**
     * 创建群组
     */
    GroupDTO createGroup(String groupName, String groupDescription, String avatar, Integer maxMembers, Long ownerId, List<Long> memberIds);

    /**
     * 根据ID获取群组信息
     */
    GroupDTO getGroupById(Long groupId, Long userId);

    /**
     * 获取用户加入的所有群组
     */
    List<GroupDTO> getUserGroups(Long userId);

    /**
     * 更新群组信息
     */
    boolean updateGroupInfo(Long groupId, Long operatorId, String groupName, String groupDescription);

    /**
     * 解散群组（仅群主可操作）
     */
    boolean dissolveGroup(Long groupId, Long operatorId);

    /**
     * 搜索群组
     */
    List<GroupDTO> searchGroups(String keyword, Integer limit);

    /**
     * 上传群头像
     */
    boolean uploadGroupAvatar(Long groupId, Long operatorId, byte[] avatarData, String contentType);

    /**
     * 获取群头像
     */
    ChatGroup getGroupAvatar(Long groupId);

    // ==================== 成员管理 ====================

    /**
     * 邀请用户加入群组
     */
    boolean inviteUsers(Long groupId, Long inviterId, List<Long> userIds);

    /**
     * 移除群成员
     */
    boolean removeMembers(Long groupId, Long operatorId, List<Long> memberIds);

    /**
     * 退出群组
     */
    boolean leaveGroup(Long groupId, Long userId);

    /**
     * 获取群成员列表
     */
    List<GroupDTO.GroupMemberDTO> getGroupMembers(Long groupId, Long userId);

    /**
     * 更新成员角色（提升/降级管理员）
     */
    boolean updateMemberRole(Long groupId, Long operatorId, Long memberId, Integer newRole);

    /**
     * 更新群内昵称
     */
    boolean updateMemberNickname(Long groupId, Long userId, String nickname);

    /**
     * 更新群备注
     */
    boolean updateGroupRemark(Long groupId, Long userId, String remark);
    
    boolean updateGroupAnnouncement(Long groupId, Long userId, String announcement);

    // ==================== 禁言管理 ====================

    /**
     * 禁言群成员
     */
    boolean muteMembers(Long groupId, Long operatorId, List<Long> memberIds, LocalDateTime muteUntil);

    /**
     * 解除禁言
     */
    boolean unmuteMembers(Long groupId, Long operatorId, List<Long> memberIds);

    /**
     * 全员禁言
     */
    boolean muteAllMembers(Long groupId, Long operatorId, boolean mute);

    /**
     * 获取被禁言的成员列表
     */
    List<GroupDTO.GroupMemberDTO> getMutedMembers(Long groupId, Long operatorId);

    // ==================== 公告管理 ====================

    /**
     * 发布群公告
     */
    GroupAnnouncement publishAnnouncement(Long groupId, Long publisherId, String title, String content);

    /**
     * 获取群公告列表
     */
    List<GroupAnnouncement> getGroupAnnouncements(Long groupId);

    /**
     * 获取最新群公告
     */
    GroupAnnouncement getLatestAnnouncement(Long groupId);

    /**
     * 更新群公告
     */
    boolean updateAnnouncement(Long announcementId, Long operatorId, String title, String content);

    /**
     * 置顶/取消置顶公告
     */
    boolean pinAnnouncement(Long announcementId, Long operatorId, boolean pin);

    /**
     * 撤回群公告
     */
    boolean recallAnnouncement(Long announcementId, Long operatorId);

    // ==================== 权限检查 ====================

    /**
     * 检查用户是否为群成员
     */
    boolean isMember(Long groupId, Long userId);

    /**
     * 检查用户是否为群主或管理员
     */
    boolean isAdminOrOwner(Long groupId, Long userId);

    /**
     * 检查用户是否为群主
     */
    boolean isOwner(Long groupId, Long userId);

    /**
     * 检查用户是否被禁言
     */
    boolean isMuted(Long groupId, Long userId);

    /**
     * 获取用户在群内的角色
     */
    Integer getUserRole(Long groupId, Long userId);

    /**
     * 检查是否为群成员
     */
    boolean isGroupMember(Long groupId, Long userId);

    /**
     * 检查成员是否被禁言
     */
    boolean isMemberMuted(Long groupId, Long userId);

    /**
     * 获取群成员ID列表
     */
    List<Long> getGroupMemberIds(Long groupId);
}