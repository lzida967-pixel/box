package com.chatapp.mapper;

import com.chatapp.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息数据访问接口
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Mapper
public interface MessageMapper {

    /**
     * 根据ID查询消息
     */
    Message findById(Long id);

    /**
     * 插入消息
     */
    int insert(Message message);

    /**
     * 更新消息
     */
    int update(Message message);

    /**
     * 删除消息（软删除）
     */
    int deleteById(Long id);

    /**
     * 查询私聊消息历史记录
     */
    List<Message> findPrivateMessages(@Param("userId1") Long userId1, 
                                     @Param("userId2") Long userId2,
                                     @Param("limit") Integer limit,
                                     @Param("offset") Integer offset);

    /**
     * 查询群聊消息历史记录
     */
    List<Message> findGroupMessages(@Param("groupId") Long groupId,
                                  @Param("limit") Integer limit,
                                  @Param("offset") Integer offset);

    /**
     * 查询用户未读消息数量
     */
    int countUnreadMessages(Long userId);

    /**
     * 查询与特定用户的未读消息数量
     */
    int countUnreadMessagesFromUser(@Param("userId") Long userId, 
                                   @Param("fromUserId") Long fromUserId);

    /**
     * 标记消息为已读
     */
    int markAsRead(Long messageId);

    /**
     * 标记与特定用户的所有消息为已读
     */
    int markAllAsRead(@Param("userId") Long userId, 
                     @Param("fromUserId") Long fromUserId);

    /**
     * 撤回消息
     */
    int recallMessage(Long messageId);

    /**
     * 查询用户最近的联系人（通过消息记录）
     */
    List<Long> findRecentContacts(@Param("userId") Long userId, 
                                 @Param("limit") Integer limit);

    /**
     * 查询用户与特定联系人的最后一条消息
     */
    Message findLastMessage(@Param("userId") Long userId, 
                          @Param("contactId") Long contactId,
                          @Param("isGroup") Boolean isGroup);
}