package com.chatapp.service.impl;

import com.chatapp.dto.GroupDTO;
import com.chatapp.entity.ChatGroup;
import com.chatapp.entity.GroupAnnouncement;
import com.chatapp.entity.GroupMember;
import com.chatapp.entity.User;
import com.chatapp.mapper.ChatGroupMapper;
import com.chatapp.mapper.GroupAnnouncementMapper;
import com.chatapp.mapper.GroupMemberMapper;
import com.chatapp.mapper.UserMapper;
import com.chatapp.service.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

/**
 * 群组服务实现类
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Service
public class GroupServiceImpl implements GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

    @Autowired
    private ChatGroupMapper groupMapper;

    @Autowired
    private GroupMemberMapper memberMapper;

    @Autowired
    private GroupAnnouncementMapper announcementMapper;

    @Autowired
    private UserMapper userMapper;

    // ==================== 群组管理 ====================

    @Override
    @Transactional
    public GroupDTO createGroup(String groupName, String groupDescription, String avatar, Integer maxMembers, Long ownerId, List<Long> memberIds) {
        try {
            // 创建群组
            ChatGroup group = new ChatGroup(groupName, ownerId);
            group.setGroupDescription(groupDescription);
            group.setGroupAvatar(avatar);
            group.setStatus(1); // 默认设置为公开群组（1）
            if (maxMembers != null) group.setMaxMembers(maxMembers);
            group.setMemberCount(1); // 初始只有群主
            
            int result = groupMapper.insertGroup(group);
            if (result <= 0) {
                throw new RuntimeException("创建群组失败");
            }

            // 添加群主为成员
            GroupMember ownerMember = new GroupMember(group.getId(), ownerId, 3); // 3-群主
            memberMapper.insertMember(ownerMember);

            // 添加其他成员
            if (memberIds != null && !memberIds.isEmpty()) {
                List<GroupMember> members = new ArrayList<>();
                for (Long memberId : memberIds) {
                    if (!memberId.equals(ownerId)) { // 避免重复添加群主
                        GroupMember member = new GroupMember(group.getId(), memberId, 1); // 1-普通成员
                        member.setInviteUserId(ownerId);
                        members.add(member);
                    }
                }
                
                if (!members.isEmpty()) {
                    memberMapper.insertMembers(members);
                    // 更新成员数量
                    int totalMembers = 1 + members.size();
                    groupMapper.updateMemberCount(group.getId(), totalMembers);
                    group.setMemberCount(totalMembers);
                }
            }

            // 返回群组信息
            return getGroupById(group.getId(), ownerId);
        } catch (Exception e) {
            logger.error("创建群组失败", e);
            throw new RuntimeException("创建群组失败: " + e.getMessage());
        }
    }

    @Override
    public GroupDTO getGroupById(Long groupId, Long userId) {
        try {
            ChatGroup group = groupMapper.selectGroupById(groupId);
            if (group == null) {
                return null;
            }

            GroupDTO groupDTO = new GroupDTO(group);
            
            // 获取群主信息
            User owner = userMapper.findById(group.getOwnerId()).orElse(null);
            if (owner != null) {
                groupDTO.setOwnerName(owner.getNickname() != null ? owner.getNickname() : owner.getUsername());
            }

            // 获取当前用户在群内的信息
            if (userId != null) {
                GroupMember myMember = memberMapper.selectMemberByGroupAndUser(groupId, userId);
                if (myMember != null) {
                    groupDTO.setMyRole(myMember.getMemberRole());
                    groupDTO.setMyNickname(myMember.getMemberNickname());
                    groupDTO.setMyRemark(myMember.getRemark());
                    groupDTO.setMyJoinTime(myMember.getJoinTime());
                    groupDTO.setMyMuteUntil(myMember.getMuteUntil());
                }
            }

            // 获取最新公告
            GroupAnnouncement latestAnnouncement = announcementMapper.selectLatestAnnouncementByGroupId(groupId);
            if (latestAnnouncement != null) {
                groupDTO.setLatestAnnouncement(latestAnnouncement.getContent());
            }

            return groupDTO;
        } catch (Exception e) {
            logger.error("获取群组信息失败", e);
            return null;
        }
    }

    @Override
    public List<GroupDTO> getUserGroups(Long userId) {
        try {
            List<ChatGroup> groups = groupMapper.selectGroupsByUserId(userId);
            List<GroupDTO> groupDTOs = new ArrayList<>();

            for (ChatGroup group : groups) {
                GroupDTO groupDTO = getGroupById(group.getId(), userId);
                if (groupDTO != null) {
                    groupDTOs.add(groupDTO);
                }
            }

            return groupDTOs;
        } catch (Exception e) {
            logger.error("获取用户群组列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public boolean updateGroupInfo(Long groupId, Long operatorId, String groupName, String groupDescription) {
        try {
            // 检查权限（群主或管理员）
            if (!isAdminOrOwner(groupId, operatorId)) {
                throw new RuntimeException("无权限修改群组信息");
            }

            ChatGroup group = groupMapper.selectGroupById(groupId);
            if (group == null) {
                throw new RuntimeException("群组不存在");
            }

            group.setGroupName(groupName);
            group.setGroupDescription(groupDescription);
            group.setUpdateTime(LocalDateTime.now());

            return groupMapper.updateGroup(group) > 0;
        } catch (Exception e) {
            logger.error("更新群组信息失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean dissolveGroup(Long groupId, Long operatorId) {
        try {
            // 检查权限（仅群主可解散）
            if (!isOwner(groupId, operatorId)) {
                throw new RuntimeException("只有群主可以解散群组");
            }

            // 软删除群组
            int result = groupMapper.deleteGroup(groupId);
            
            // 移除所有成员
            List<GroupMember> members = memberMapper.selectActiveMembersByGroupId(groupId);
            if (!members.isEmpty()) {
                List<Long> memberIds = members.stream().map(GroupMember::getId).collect(Collectors.toList());
                memberMapper.removeMembers(groupId, memberIds);
            }

            return result > 0;
        } catch (Exception e) {
            logger.error("解散群组失败", e);
            return false;
        }
    }

    @Override
    public List<GroupDTO> searchGroups(String keyword, Integer limit) {
        try {
            List<ChatGroup> groups = groupMapper.searchGroupsByName(keyword, limit);
            List<GroupDTO> groupDTOs = new ArrayList<>();

            for (ChatGroup group : groups) {
                GroupDTO groupDTO = new GroupDTO(group);
                
                // 获取群主信息
                User owner = userMapper.findById(group.getOwnerId()).orElse(null);
                if (owner != null) {
                    groupDTO.setOwnerName(owner.getNickname() != null ? owner.getNickname() : owner.getUsername());
                }
                
                groupDTOs.add(groupDTO);
            }

            return groupDTOs;
        } catch (Exception e) {
            logger.error("搜索群组失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public boolean uploadGroupAvatar(Long groupId, Long operatorId, byte[] avatarData, String contentType) {
        try {
            // 检查权限（群主或管理员）
            if (!isAdminOrOwner(groupId, operatorId)) {
                throw new RuntimeException("无权限修改群头像");
            }

            return groupMapper.updateGroupAvatar(groupId, avatarData, contentType) > 0;
        } catch (Exception e) {
            logger.error("上传群头像失败", e);
            return false;
        }
    }

    @Override
    public ChatGroup getGroupAvatar(Long groupId) {
        try {
            return groupMapper.selectGroupAvatarById(groupId);
        } catch (Exception e) {
            logger.error("获取群头像失败", e);
            return null;
        }
    }

    // ==================== 成员管理 ====================

    @Override
    @Transactional
    public boolean inviteUsers(Long groupId, Long inviterId, List<Long> userIds) {
        try {
            // 检查群组是否存在
            ChatGroup group = groupMapper.selectGroupById(groupId);
            if (group == null) {
                throw new RuntimeException("群组不存在");
            }

            // 检查邀请者权限（群成员可邀请）
            if (!isMember(groupId, inviterId)) {
                throw new RuntimeException("只有群成员可以邀请新成员");
            }

            List<GroupMember> newMembers = new ArrayList<>();
            int addedCount = 0;

            for (Long userId : userIds) {
                // 检查用户是否已经是活跃群成员
                if (!isMember(groupId, userId)) {
                    // 检查群组成员数量限制
                    if (group.getMemberCount() + addedCount >= group.getMaxMembers()) {
                        logger.warn("群组成员数量已达上限");
                        break;
                    }

                    // 检查是否存在已删除的成员记录
                    GroupMember existingMember = memberMapper.selectMemberByGroupAndUserIncludeDeleted(groupId, userId);
                    if (existingMember != null && existingMember.getStatus() == 0) {
                        // 重新激活已删除的成员记录
                        existingMember.setStatus(1);
                        existingMember.setInviteUserId(inviterId);
                        existingMember.setJoinTime(LocalDateTime.now());
                        existingMember.setUpdateTime(LocalDateTime.now());
                        memberMapper.updateMember(existingMember);
                        addedCount++;
                    } else if (existingMember == null) {
                        // 创建新的成员记录
                        GroupMember member = new GroupMember(groupId, userId, 1); // 1-普通成员
                        member.setInviteUserId(inviterId);
                        member.setStatus(1);
                        newMembers.add(member);
                        addedCount++;
                    }
                }
            }

            if (!newMembers.isEmpty()) {
                memberMapper.insertMembers(newMembers);
                // 更新群组成员数量
                int newMemberCount = group.getMemberCount() + addedCount;
                groupMapper.updateMemberCount(groupId, newMemberCount);
            }

            return addedCount > 0;
        } catch (Exception e) {
            logger.error("邀请用户加入群组失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean removeMembers(Long groupId, Long operatorId, List<Long> memberIds) {
        try {
            // 检查权限（群主或管理员）
            if (!isAdminOrOwner(groupId, operatorId)) {
                throw new RuntimeException("无权限移除群成员");
            }

            List<Long> validMemberIds = new ArrayList<>();
            for (Long memberId : memberIds) {
                GroupMember member = memberMapper.selectMemberById(memberId);
                if (member != null && member.getGroupId().equals(groupId)) {
                    // 群主不能被移除，管理员不能移除群主
                    if (member.getMemberRole() == 3) { // 群主
                        continue;
                    }
                    if (member.getMemberRole() == 2 && !isOwner(groupId, operatorId)) { // 管理员
                        continue;
                    }
                    validMemberIds.add(memberId);
                }
            }

            if (!validMemberIds.isEmpty()) {
                memberMapper.removeMembers(groupId, validMemberIds);
                
                // 更新群组成员数量
                int currentCount = memberMapper.countActiveMembers(groupId);
                groupMapper.updateMemberCount(groupId, currentCount);
            }

            return !validMemberIds.isEmpty();
        } catch (Exception e) {
            logger.error("移除群成员失败", e);
            throw new RuntimeException("移除群成员失败", e);
        }
    }

    @Override
    @Transactional
    public boolean leaveGroup(Long groupId, Long userId) {
        try {
            GroupMember member = memberMapper.selectMemberByGroupAndUser(groupId, userId);
            if (member == null) {
                throw new RuntimeException("用户不是群成员");
            }

            // 群主不能直接退出，需要先转让群主
            if (member.getMemberRole() == 3) {
                throw new RuntimeException("群主不能退出群组，请先转让群主");
            }

            memberMapper.removeMember(member.getId());
            
            // 更新群组成员数量
            int currentCount = memberMapper.countActiveMembers(groupId);
            groupMapper.updateMemberCount(groupId, currentCount);

            return true;
        } catch (Exception e) {
            logger.error("退出群组失败", e);
            return false;
        }
    }

    @Override
    public List<GroupDTO.GroupMemberDTO> getGroupMembers(Long groupId, Long userId) {
        try {
            // 检查权限（群成员可查看）
            if (!isMember(groupId, userId)) {
                throw new RuntimeException("无权限查看群成员");
            }

            List<GroupMember> members = memberMapper.selectActiveMembersByGroupId(groupId);
            List<GroupDTO.GroupMemberDTO> memberDTOs = new ArrayList<>();

            for (GroupMember member : members) {
                User user = userMapper.findById(member.getUserId()).orElse(null);
                GroupDTO.GroupMemberDTO memberDTO = new GroupDTO.GroupMemberDTO(member, user);
                
                // 设置邀请人信息
                if (member.getInviteUserId() != null) {
                    User inviteUser = userMapper.findById(member.getInviteUserId()).orElse(null);
                    if (inviteUser != null) {
                        memberDTO.setInviteUserName(inviteUser.getNickname() != null ? 
                            inviteUser.getNickname() : inviteUser.getUsername());
                    }
                }
                
                memberDTOs.add(memberDTO);
            }

            return memberDTOs;
        } catch (Exception e) {
            logger.error("获取群成员列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public boolean updateMemberRole(Long groupId, Long operatorId, Long memberId, Integer newRole) {
        try {
            // 检查权限（仅群主可操作）
            if (!isOwner(groupId, operatorId)) {
                throw new RuntimeException("只有群主可以修改成员角色");
            }

            GroupMember member = memberMapper.selectMemberById(memberId);
            if (member == null || !member.getGroupId().equals(groupId)) {
                throw new RuntimeException("群成员不存在");
            }

            // 不能修改群主角色
            if (member.getMemberRole() == 3) {
                throw new RuntimeException("不能修改群主角色");
            }

            return memberMapper.updateMemberRole(memberId, newRole) > 0;
        } catch (Exception e) {
            logger.error("更新成员角色失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateMemberNickname(Long groupId, Long userId, String nickname) {
        try {
            GroupMember member = memberMapper.selectMemberByGroupAndUser(groupId, userId);
            if (member == null) {
                throw new RuntimeException("用户不是群成员");
            }

            return memberMapper.updateMemberNickname(member.getId(), nickname) > 0;
        } catch (Exception e) {
            logger.error("更新群内昵称失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateGroupRemark(Long groupId, Long userId, String remark) {
        try {
            return memberMapper.updateGroupRemark(groupId, userId, remark) > 0;
        } catch (Exception e) {
            logger.error("更新群备注失败", e);
            return false;
        }
    }

    // ==================== 禁言管理 ====================

    @Override
    @Transactional
    public boolean muteMembers(Long groupId, Long operatorId, List<Long> memberIds, LocalDateTime muteUntil) {
        try {
            // 检查权限（群主或管理员）
            if (!isAdminOrOwner(groupId, operatorId)) {
                throw new RuntimeException("无权限禁言群成员");
            }

            List<Long> validMemberIds = new ArrayList<>();
            for (Long memberId : memberIds) {
                GroupMember member = memberMapper.selectMemberById(memberId);
                if (member != null && member.getGroupId().equals(groupId)) {
                    // 群主不能被禁言，管理员不能禁言群主
                    if (member.getMemberRole() == 3) { // 群主
                        continue;
                    }
                    if (member.getMemberRole() == 2 && !isOwner(groupId, operatorId)) { // 管理员
                        continue;
                    }
                    validMemberIds.add(memberId);
                }
            }

            if (!validMemberIds.isEmpty()) {
                memberMapper.muteMembers(validMemberIds, muteUntil);
            }

            return !validMemberIds.isEmpty();
        } catch (Exception e) {
            logger.error("禁言群成员失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean unmuteMembers(Long groupId, Long operatorId, List<Long> memberIds) {
        try {
            // 检查权限（群主或管理员）
            if (!isAdminOrOwner(groupId, operatorId)) {
                throw new RuntimeException("无权限解除禁言");
            }

            List<Long> validMemberIds = new ArrayList<>();
            for (Long memberId : memberIds) {
                GroupMember member = memberMapper.selectMemberById(memberId);
                if (member != null && member.getGroupId().equals(groupId)) {
                    validMemberIds.add(memberId);
                }
            }

            if (!validMemberIds.isEmpty()) {
                memberMapper.unmuteMembers(validMemberIds);
            }

            return !validMemberIds.isEmpty();
        } catch (Exception e) {
            logger.error("解除禁言失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean muteAllMembers(Long groupId, Long operatorId, boolean mute) {
        try {
            // 检查权限（群主或管理员）
            if (!isAdminOrOwner(groupId, operatorId)) {
                throw new RuntimeException("无权限设置全员禁言");
            }

            ChatGroup group = groupMapper.selectGroupById(groupId);
            if (group == null) {
                throw new RuntimeException("群组不存在");
            }

            group.setMuteAll(mute ? 1 : 0);
            group.setUpdateTime(LocalDateTime.now());

            return groupMapper.updateGroup(group) > 0;
        } catch (Exception e) {
            logger.error("设置全员禁言失败", e);
            return false;
        }
    }

    @Override
    public List<GroupDTO.GroupMemberDTO> getMutedMembers(Long groupId, Long operatorId) {
        try {
            // 检查权限（群主或管理员）
            if (!isAdminOrOwner(groupId, operatorId)) {
                throw new RuntimeException("无权限查看禁言列表");
            }

            List<GroupMember> mutedMembers = memberMapper.selectMutedMembers(groupId);
            List<GroupDTO.GroupMemberDTO> memberDTOs = new ArrayList<>();

            for (GroupMember member : mutedMembers) {
                User user = userMapper.findById(member.getUserId()).orElse(null);
                GroupDTO.GroupMemberDTO memberDTO = new GroupDTO.GroupMemberDTO(member, user);
                memberDTOs.add(memberDTO);
            }

            return memberDTOs;
        } catch (Exception e) {
            logger.error("获取禁言成员列表失败", e);
            return new ArrayList<>();
        }
    }

    // ==================== 公告管理 ====================

    @Override
    @Transactional
    public GroupAnnouncement publishAnnouncement(Long groupId, Long publisherId, String title, String content) {
        try {
            // 检查权限（群主或管理员）
            if (!isAdminOrOwner(groupId, publisherId)) {
                throw new RuntimeException("无权限发布群公告");
            }

            GroupAnnouncement announcement = new GroupAnnouncement(groupId, publisherId, title, content);
            int result = announcementMapper.insertAnnouncement(announcement);
            
            return result > 0 ? announcement : null;
        } catch (Exception e) {
            logger.error("发布群公告失败", e);
            return null;
        }
    }

    @Override
    public List<GroupAnnouncement> getGroupAnnouncements(Long groupId) {
        try {
            return announcementMapper.selectAnnouncementsByGroupId(groupId);
        } catch (Exception e) {
            logger.error("获取群公告列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public GroupAnnouncement getLatestAnnouncement(Long groupId) {
        try {
            return announcementMapper.selectLatestAnnouncementByGroupId(groupId);
        } catch (Exception e) {
            logger.error("获取最新群公告失败", e);
            return null;
        }
    }

    @Override
    @Transactional
    public boolean updateAnnouncement(Long announcementId, Long operatorId, String title, String content) {
        try {
            GroupAnnouncement announcement = announcementMapper.selectAnnouncementById(announcementId);
            if (announcement == null) {
                throw new RuntimeException("公告不存在");
            }

            // 检查权限（发布者或群主/管理员）
            if (!announcement.getPublisherId().equals(operatorId) && 
                !isAdminOrOwner(announcement.getGroupId(), operatorId)) {
                throw new RuntimeException("无权限修改此公告");
            }

            announcement.setTitle(title);
            announcement.setContent(content);
            announcement.setUpdateTime(LocalDateTime.now());

            return announcementMapper.updateAnnouncement(announcement) > 0;
        } catch (Exception e) {
            logger.error("更新群公告失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean pinAnnouncement(Long announcementId, Long operatorId, boolean pin) {
        try {
            GroupAnnouncement announcement = announcementMapper.selectAnnouncementById(announcementId);
            if (announcement == null) {
                throw new RuntimeException("公告不存在");
            }

            // 检查权限（群主或管理员）
            if (!isAdminOrOwner(announcement.getGroupId(), operatorId)) {
                throw new RuntimeException("无权限置顶公告");
            }

            return pin ? announcementMapper.pinAnnouncement(announcementId) > 0 :
                        announcementMapper.unpinAnnouncement(announcementId) > 0;
        } catch (Exception e) {
            logger.error("置顶公告失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean recallAnnouncement(Long announcementId, Long operatorId) {
        try {
            GroupAnnouncement announcement = announcementMapper.selectAnnouncementById(announcementId);
            if (announcement == null) {
                throw new RuntimeException("公告不存在");
            }

            // 检查权限（发布者或群主/管理员）
            if (!announcement.getPublisherId().equals(operatorId) && 
                !isAdminOrOwner(announcement.getGroupId(), operatorId)) {
                throw new RuntimeException("无权限撤回此公告");
            }

            return announcementMapper.recallAnnouncement(announcementId) > 0;
        } catch (Exception e) {
            logger.error("撤回群公告失败", e);
            return false;
        }
    }

    // ==================== 权限检查 ====================

    @Override
    public boolean isMember(Long groupId, Long userId) {
        try {
            return memberMapper.isMember(groupId, userId);
        } catch (Exception e) {
            logger.error("检查群成员身份失败", e);
            return false;
        }
    }

    @Override
    public boolean isAdminOrOwner(Long groupId, Long userId) {
        try {
            return memberMapper.isAdminOrOwner(groupId, userId);
        } catch (Exception e) {
            logger.error("检查管理员身份失败", e);
            return false;
        }
    }

    @Override
    public boolean isOwner(Long groupId, Long userId) {
        try {
            ChatGroup group = groupMapper.selectGroupById(groupId);
            return group != null && group.getOwnerId().equals(userId);
        } catch (Exception e) {
            logger.error("检查群主身份失败", e);
            return false;
        }
    }

    @Override
    public boolean isMuted(Long groupId, Long userId) {
        try {
            GroupMember member = memberMapper.selectMemberByGroupAndUser(groupId, userId);
            if (member == null) {
                return false;
            }

            // 检查个人禁言
            if (member.getMuteUntil() != null && member.getMuteUntil().isAfter(LocalDateTime.now())) {
                return true;
            }

            // 检查全员禁言
            ChatGroup group = groupMapper.selectGroupById(groupId);
            return group != null && group.getMuteAll() == 1 && member.getMemberRole() == 1; // 普通成员受全员禁言影响
        } catch (Exception e) {
            logger.error("检查禁言状态失败", e);
            return false;
        }
    }

    @Override
    public Integer getUserRole(Long groupId, Long userId) {
        try {
            GroupMember member = memberMapper.selectMemberByGroupAndUser(groupId, userId);
            return member != null ? member.getMemberRole() : null;
        } catch (Exception e) {
            logger.error("获取用户群内角色失败", e);
            return null;
        }
    }

    @Override
    public boolean isGroupMember(Long groupId, Long userId) {
        try {
            GroupMember member = memberMapper.selectMemberByGroupAndUser(groupId, userId);
            return member != null && member.getStatus() == 1; // 1表示正常状态
        } catch (Exception e) {
            logger.error("检查群成员状态失败", e);
            return false;
        }
    }

    @Override
    public boolean isMemberMuted(Long groupId, Long userId) {
        try {
            GroupMember member = memberMapper.selectMemberByGroupAndUser(groupId, userId);
            if (member == null) {
                return false;
            }
            
            // 检查是否被禁言
            if (member.getIsMuted() == null || !member.getIsMuted()) {
                return false;
            }
            
            // 检查禁言是否过期
            if (member.getMuteUntil() != null) {
                return member.getMuteUntil().isAfter(LocalDateTime.now());
            }
            
            // 永久禁言
            return true;
        } catch (Exception e) {
            logger.error("检查成员禁言状态失败", e);
            return false;
        }
    }

    @Override
    public List<Long> getGroupMemberIds(Long groupId) {
        try {
            List<GroupMember> members = memberMapper.selectActiveMembersByGroupId(groupId);
            return members.stream()
                    .map(GroupMember::getUserId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("获取群成员ID列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean updateGroupAnnouncement(Long groupId, Long userId, String announcement) {
        try {
            // 检查用户是否是群主
            ChatGroup group = groupMapper.selectGroupById(groupId);
            if (group == null) {
                throw new RuntimeException("群组不存在");
            }
            // 创建新的群公告
            GroupAnnouncement groupAnnouncement = new GroupAnnouncement();
            groupAnnouncement.setGroupId(groupId);
            groupAnnouncement.setPublisherId(userId);
            groupAnnouncement.setTitle("群公告");
            groupAnnouncement.setContent(announcement);
            groupAnnouncement.setIsPinned(1);
            groupAnnouncement.setStatus(1); // 1表示正常状态
            groupAnnouncement.setPublishTime(LocalDateTime.now());
            groupAnnouncement.setCreateTime(LocalDateTime.now());
            groupAnnouncement.setUpdateTime(LocalDateTime.now());
            groupAnnouncement.setDeleted(0); // 0表示未删除
            
            // 先将之前的公告设为非置顶
            announcementMapper.unpinAllAnnouncements(groupId);
            
            // 插入新公告
            int result = announcementMapper.insertAnnouncement(groupAnnouncement);
            return result > 0;
        } catch (Exception e) {
            logger.error("更新群公告失败: {}", e.getMessage(), e);
            return false;
        }
    }
}