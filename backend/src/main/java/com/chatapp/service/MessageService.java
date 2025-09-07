package com.chatapp.service;

import com.chatapp.entity.Message;
import java.util.List;

/**
 * 消息服务接口
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public interface MessageService {

    /**
     * 发送私聊消息
     */
    Message sendPrivateMessage(Long fromUserId, Long toUserId, String content, Integer messageType);

    /**
     * 发送群聊消息
     */
    Message sendGroupMessage(Long fromUserId, Long groupId, String content, Integer messageType);

    /**
     * 获取私聊消息历史
     */
    List<Message> getPrivateMessageHistory(Long userId1, Long userId2, Integer limit, Integer offset);

    /**
     * 获取群聊消息历史
     */
    List<Message> getGroupMessageHistory(Long groupId, Integer limit, Integer offset);

    /**
     * 获取未读消息数量
     */
    int getUnreadMessageCount(Long userId);

    /**
     * 获取与特定用户的未读消息数量
     */
    int getUnreadMessageCountFromUser(Long userId, Long fromUserId);

    /**
     * 标记消息为已读
     */
    boolean markMessageAsRead(Long messageId);

    /**
     * 标记与特定用户的所有消息为已读
     */
    boolean markAllMessagesAsRead(Long userId, Long fromUserId);

    /**
     * 撤回消息
     */
    boolean recallMessage(Long messageId);

    /**
     * 获取最近联系人列表
     */
    List<Long> getRecentContacts(Long userId, Integer limit);

    /**
     * 获取最后一条消息
     */
    Message getLastMessage(Long userId, Long contactId, Boolean isGroup);
}