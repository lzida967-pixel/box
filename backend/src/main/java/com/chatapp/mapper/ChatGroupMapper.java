package com.chatapp.mapper;

import com.chatapp.entity.ChatGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 群组数据访问层
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Mapper
public interface ChatGroupMapper {

    /**
     * 创建群组
     */
    int insertGroup(ChatGroup group);

    /**
     * 根据ID查询群组
     */
    ChatGroup selectGroupById(@Param("id") Long id);

    /**
     * 根据群主ID查询群组列表
     */
    List<ChatGroup> selectGroupsByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * 查询用户加入的所有群组
     */
    List<ChatGroup> selectGroupsByUserId(@Param("userId") Long userId);

    /**
     * 更新群组信息
     */
    int updateGroup(ChatGroup group);

    /**
     * 更新群组成员数量
     */
    int updateMemberCount(@Param("groupId") Long groupId, @Param("memberCount") Integer memberCount);

    /**
     * 删除群组（软删除）
     */
    int deleteGroup(@Param("id") Long id);

    /**
     * 根据群名搜索群组
     */
    List<ChatGroup> searchGroupsByName(@Param("keyword") String keyword, @Param("limit") Integer limit);

    /**
     * 检查群组是否存在
     */
    boolean existsById(@Param("id") Long id);

    /**
     * 获取群组头像数据
     */
    ChatGroup selectGroupAvatarById(@Param("id") Long id);

    /**
     * 更新群组头像
     */
    int updateGroupAvatar(@Param("id") Long id, @Param("avatarData") byte[] avatarData, 
                         @Param("contentType") String contentType);
}