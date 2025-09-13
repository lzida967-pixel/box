package com.chatapp.mapper;

import com.chatapp.entity.GroupAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 群公告数据访问层
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Mapper
public interface GroupAnnouncementMapper {

    /**
     * 创建群公告
     */
    int insertAnnouncement(GroupAnnouncement announcement);

    /**
     * 根据ID查询群公告
     */
    GroupAnnouncement selectAnnouncementById(@Param("id") Long id);

    /**
     * 查询群组的所有公告
     */
    List<GroupAnnouncement> selectAnnouncementsByGroupId(@Param("groupId") Long groupId);

    /**
     * 查询群组的最新公告
     */
    GroupAnnouncement selectLatestAnnouncementByGroupId(@Param("groupId") Long groupId);

    /**
     * 查询群组的置顶公告
     */
    List<GroupAnnouncement> selectPinnedAnnouncementsByGroupId(@Param("groupId") Long groupId);

    /**
     * 更新群公告
     */
    int updateAnnouncement(GroupAnnouncement announcement);

    /**
     * 置顶公告
     */
    int pinAnnouncement(@Param("id") Long id);

    /**
     * 取消置顶公告
     */
    int unpinAnnouncement(@Param("id") Long id);

    /**
     * 撤回公告
     */
    int recallAnnouncement(@Param("id") Long id);

    /**
     * 删除公告（软删除）
     */
    int deleteAnnouncement(@Param("id") Long id);

    /**
     * 检查公告是否存在
     */
    boolean existsById(@Param("id") Long id);
    
    /**
     * 取消群组所有公告的置顶状态
     */
    int unpinAllAnnouncements(@Param("groupId") Long groupId);
}