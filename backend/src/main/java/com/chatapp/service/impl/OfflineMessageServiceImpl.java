package com.chatapp.service.impl;

import com.chatapp.entity.Message;
import com.chatapp.mapper.MessageMapper;
import com.chatapp.mapper.MessagePushRecordMapper;
import com.chatapp.service.OfflineMessageService;
import com.chatapp.service.WebSocketSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 离线消息服务实现类
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Service
public class OfflineMessageServiceImpl implements OfflineMessageService {

    private static final Logger logger = LoggerFactory.getLogger(OfflineMessageServiceImpl.class);

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private MessagePushRecordMapper pushRecordMapper;

    @Autowired
    private WebSocketSessionService sessionService;

    @Override
    @Transactional
    public boolean pushMessageToUser(Long userId, Message message) {
        try {
            // 检查用户是否在线
            if (sessionService.isUserOnline(userId)) {
                // 用户在线，直接通过WebSocket推送
                Map<String, Object> response = new HashMap<>();
                response.put("type", "private");
                response.put("fromUserId", message.getFromUserId());
                response.put("toUserId", message.getToUserId());
                response.put("message", message);
                response.put("timestamp", System.currentTimeMillis());

                try {
                    sessionService.sendToUser(userId, response);
                    logger.debug("消息实时推送成功: messageId={}, userId={}", message.getId(), userId);
                    return true;
                } catch (Exception e) {
                    logger.warn("消息实时推送失败，记录为离线消息: messageId={}, userId={}", message.getId(), userId, e);
                }
            }

            // 用户离线或推送失败，创建推送记录
            createPushRecord(message.getId(), userId, 0); // 0-待推送
            logger.debug("消息记录为离线消息: messageId={}, userId={}", message.getId(), userId);
            return true;

        } catch (Exception e) {
            logger.error("推送消息失败: messageId={}, userId={}", message.getId(), userId, e);
            // 创建失败的推送记录
            createPushRecord(message.getId(), userId, 2); // 2-推送失败
            return false;
        }
    }

    @Override
    public List<Message> getOfflineMessages(Long userId) {
        try {
            if (userId == null) {
                logger.warn("用户ID为空，无法获取离线消息");
                return List.of();
            }
            
            // 检查pushRecordMapper是否为null
            if (pushRecordMapper == null) {
                logger.error("pushRecordMapper未初始化，可能是MyBatis配置问题");
                return List.of();
            }
            
            // 获取用户的未推送消息
            List<Message> offlineMessages;
            try {
                offlineMessages = pushRecordMapper.findUnpushedMessages(userId);
            } catch (Exception dbException) {
                logger.warn("数据库查询失败，可能是表不存在或配置问题: {}", dbException.getMessage());
                // 如果数据库查询失败，返回空列表
                return List.of();
            }
            
            if (offlineMessages == null) {
                offlineMessages = List.of();
            }
            logger.debug("获取用户离线消息: userId={}, count={}", userId, offlineMessages.size());
            return offlineMessages;
        } catch (Exception e) {
            logger.error("获取离线消息失败: userId={}", userId, e);
            // 返回空列表而不是抛出异常
            return List.of();
        }
    }

    @Override
    @Transactional
    public boolean markOfflineMessagesAsPushed(Long userId, List<Long> messageIds) {
        try {
            if (messageIds == null || messageIds.isEmpty()) {
                return true;
            }

            int updated = pushRecordMapper.markMessagesAsPushed(userId, messageIds);
            logger.debug("标记离线消息为已推送: userId={}, messageIds={}, updated={}", 
                        userId, messageIds, updated);
            return updated > 0;
        } catch (Exception e) {
            logger.error("标记离线消息为已推送失败: userId={}, messageIds={}", userId, messageIds, e);
            return false;
        }
    }

    @Override
    @Async
    public void cleanupPushedRecords() {
        try {
            // 清理7天前的已推送记录
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
            int deleted = pushRecordMapper.deletePushedRecordsBefore(cutoffTime);
            logger.info("清理已推送的离线消息记录: deleted={}", deleted);
        } catch (Exception e) {
            logger.error("清理已推送记录失败", e);
        }
    }

    @Override
    @Async
    public void retryFailedPushes() {
        try {
            // 获取失败的推送记录（重试次数小于3次）
            List<Map<String, Object>> failedRecords = pushRecordMapper.findFailedPushRecords(3);
            
            for (Map<String, Object> record : failedRecords) {
                Long messageId = (Long) record.get("message_id");
                Long userId = (Long) record.get("user_id");
                Integer retryCount = (Integer) record.get("retry_count");

                // 获取消息详情
                Message message = messageMapper.findById(messageId);
                if (message != null) {
                    // 重试推送
                    boolean success = pushMessageToUser(userId, message);
                    if (success) {
                        // 推送成功，更新记录状态
                        pushRecordMapper.updatePushStatus(messageId, userId, 1, null);
                    } else {
                        // 推送失败，增加重试次数
                        pushRecordMapper.incrementRetryCount(messageId, userId, "重试推送失败");
                    }
                }
            }

            logger.info("重试失败推送完成: records={}", failedRecords.size());
        } catch (Exception e) {
            logger.error("重试失败推送异常", e);
        }
    }

    @Override
    @Transactional
    public void handleUserOnline(Long userId) {
        try {
            // 获取用户的离线消息
            List<Message> offlineMessages = getOfflineMessages(userId);
            
            if (!offlineMessages.isEmpty()) {
                logger.info("用户上线，推送离线消息: userId={}, count={}", userId, offlineMessages.size());
                
                // 推送离线消息
                for (Message message : offlineMessages) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "private");
                    response.put("fromUserId", message.getFromUserId());
                    response.put("toUserId", message.getToUserId());
                    response.put("message", message);
                    response.put("timestamp", System.currentTimeMillis());
                    response.put("isOfflineMessage", true); // 标记为离线消息

                    sessionService.sendToUser(userId, response);
                }

                // 标记消息为已推送
                List<Long> messageIds = offlineMessages.stream()
                    .map(Message::getId)
                    .toList();
                markOfflineMessagesAsPushed(userId, messageIds);
            }
        } catch (Exception e) {
            logger.error("处理用户上线离线消息失败: userId={}", userId, e);
        }
    }

    /**
     * 创建推送记录
     */
    private void createPushRecord(Long messageId, Long userId, Integer status) {
        try {
            Map<String, Object> record = new HashMap<>();
            record.put("message_id", messageId);
            record.put("user_id", userId);
            record.put("push_status", status);
            record.put("retry_count", 0);
            record.put("create_time", LocalDateTime.now());
            record.put("update_time", LocalDateTime.now());

            if (status == 1) { // 已推送
                record.put("push_time", LocalDateTime.now());
            }

            pushRecordMapper.insert(record);
        } catch (Exception e) {
            logger.error("创建推送记录失败: messageId={}, userId={}, status={}", 
                        messageId, userId, status, e);
        }
    }
}