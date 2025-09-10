package com.chatapp.service.impl;

import com.chatapp.entity.Message;
import com.chatapp.mapper.MessageMapper;
import com.chatapp.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息服务实现类
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    @Transactional
    public Message sendPrivateMessage(Long fromUserId, Long toUserId, String content, Integer messageType) {
        Message message = new Message();
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        int type = messageType != null ? messageType : 1;

        // 当为图片消息时，content 必须为 images.id（数字）
        if (type == 2) {
            if (content == null) {
                throw new IllegalArgumentException("图片消息缺少 content(imageId)");
            }
            try {
                // 允许字符串数字，标准化为字符串
                Long imageId = Long.valueOf(content.trim());
                message.setContent(String.valueOf(imageId));
            } catch (NumberFormatException e) {
                // 一律拒绝文件名等非法值，避免错误数据入库
                throw new IllegalArgumentException("图片消息 content 必须为数字ID，实际为: " + content);
            }
        } else {
            message.setContent(content);
        }

        message.setMessageType(type);
        message.setSendTime(LocalDateTime.now());

        messageMapper.insert(message);
        return message;
    }

    @Override
    @Transactional
    public Message sendGroupMessage(Long fromUserId, Long groupId, String content, Integer messageType) {
        Message message = new Message();
        message.setFromUserId(fromUserId);
        message.setGroupId(groupId);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : 1);
        message.setSendTime(LocalDateTime.now());
        
        messageMapper.insert(message);
        return message;
    }

    @Override
    public List<Message> getPrivateMessageHistory(Long userId1, Long userId2, Integer limit, Integer offset) {
        return messageMapper.findPrivateMessages(userId1, userId2, 
                limit != null ? limit : 50, 
                offset != null ? offset : 0);
    }

    @Override
    public List<Message> getGroupMessageHistory(Long groupId, Integer limit, Integer offset) {
        return messageMapper.findGroupMessages(groupId, 
                limit != null ? limit : 50, 
                offset != null ? offset : 0);
    }

    @Override
    public int getUnreadMessageCount(Long userId) {
        return messageMapper.countUnreadMessages(userId);
    }

    @Override
    public int getUnreadMessageCountFromUser(Long userId, Long fromUserId) {
        return messageMapper.countUnreadMessagesFromUser(userId, fromUserId);
    }

    @Override
    @Transactional
    public boolean markMessageAsRead(Long messageId) {
        return messageMapper.markAsRead(messageId) > 0;
    }

    @Override
    @Transactional
    public boolean markAllMessagesAsRead(Long userId, Long fromUserId) {
        return messageMapper.markAllAsRead(userId, fromUserId) > 0;
    }

    @Override
    @Transactional
    public boolean recallMessage(Long messageId) {
        return messageMapper.recallMessage(messageId) > 0;
    }

    @Override
    public List<Long> getRecentContacts(Long userId, Integer limit) {
        return messageMapper.findRecentContacts(userId, limit != null ? limit : 10);
    }

    @Override
    public Message getLastMessage(Long userId, Long contactId, Boolean isGroup) {
        return messageMapper.findLastMessage(userId, contactId, isGroup != null ? isGroup : false);
    }
}