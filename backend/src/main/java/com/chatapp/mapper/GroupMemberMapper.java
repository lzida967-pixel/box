package com.chatapp.mapper;

import com.chatapp.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 群成员数据访问层
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Mapper
public interface GroupMemberMapper {

    /**
     * 添加群成员
     */
    int insertMember(GroupMember member);

    /**
     * 批量添加群成员
     */
    int insertMembers(@Param("members") List<GroupMember> members);

    /**
     * 根据ID查询群成员
     */
    GroupMember selectMemberById(@Param("id") Long id);

    /**
     * 查询群组的所有成员
     */
    List<GroupMember> selectMembersByGroupId(@Param("groupId") Long groupId);

    /**
     * 查询群组的有效成员（未退出、未被踢）
     */
    List<GroupMember> selectActiveMembersByGroupId(@Param("groupId") Long groupId);

    /**
     * 查询用户在指定群组中的成员信息
     */
    GroupMember selectMemberByGroupAndUser(@Param("groupId") Long groupId, @Param("userId") Long userId);


    /**
     * 查询用户在指定群组中的成员信息（包括已删除的记录）
     */
    GroupMember selectMemberByGroupAndUserIncludeDeleted(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * 查询用户加入的所有群组
     */
    List<GroupMember> selectMembersByUserId(@Param("userId") Long userId);

    /**
     * 更新群成员信息
     */
    int updateMember(GroupMember member);

    /**
     * 更新群成员角色
     */
    int updateMemberRole(@Param("id") Long id, @Param("memberRole") Integer memberRole);

    /**
     * 更新群成员昵称
     */
    int updateMemberNickname(@Param("id") Long id, @Param("memberNickname") String memberNickname);

    /**
     * 更新群备注
     */
    int updateGroupRemark(@Param("groupId") Long groupId, @Param("userId") Long userId, 
                         @Param("remark") String remark);

    /**
     * 禁言群成员
     */
    int muteMember(@Param("id") Long id, @Param("muteUntil") LocalDateTime muteUntil);

    /**
     * 解除禁言
     */
    int unmuteMember(@Param("id") Long id);

    /**
     * 批量禁言
     */
    int muteMembers(@Param("memberIds") List<Long> memberIds, @Param("muteUntil") LocalDateTime muteUntil);

    /**
     * 批量解除禁言
     */
    int unmuteMembers(@Param("memberIds") List<Long> memberIds);

    /**
     * 移除群成员（更新状态为已退出）
     */
    int removeMember(@Param("id") Long id);

    /**
     * 批量移除群成员
     */
    int removeMembers(@Param("groupId") Long groupId, @Param("memberIds") List<Long> memberIds);

    /**
     * 删除群成员（软删除）
     */
    int deleteMember(@Param("id") Long id);

    /**
     * 检查用户是否为群成员
     */
    boolean isMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * 检查用户是否为群主或管理员
     */
    boolean isAdminOrOwner(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * 获取群组成员数量
     */
    int countActiveMembers(@Param("groupId") Long groupId);

    /**
     * 查询被禁言的成员
     */
    List<GroupMember> selectMutedMembers(@Param("groupId") Long groupId);

    /**
     * 查询群管理员列表
     */
    List<GroupMember> selectAdminMembers(@Param("groupId") Long groupId);


}