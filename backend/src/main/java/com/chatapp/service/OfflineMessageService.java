package com.chatapp.service;

import com.chatapp.entity.Message;
import java.util.List;

/**
 * 离线消息服务接口
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public interface OfflineMessageService {

    /**
     * 推送消息给用户（在线则直接推送，离线则记录推送记录）
     */
    boolean pushMessageToUser(Long userId, Message message);

    /**
     * 获取用户的离线消息
     */
    List<Message> getOfflineMessages(Long userId);

    /**
     * 标记离线消息为已推送
     */
    boolean markOfflineMessagesAsPushed(Long userId, List<Long> messageIds);

    /**
     * 清理已推送的离线消息记录
     */
    void cleanupPushedRecords();

    /**
     * 重试失败的消息推送
     */
    void retryFailedPushes();

    /**
     * 用户上线时处理离线消息
     */
    void handleUserOnline(Long userId);
}