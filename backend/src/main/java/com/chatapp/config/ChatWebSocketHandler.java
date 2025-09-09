package com.chatapp.config;

import com.chatapp.entity.Message;
import com.chatapp.service.MessageService;
import com.chatapp.service.OfflineMessageService;
import com.chatapp.service.WebSocketSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket消息处理器
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MessageService messageService;

    @Autowired
    private WebSocketSessionService sessionService;

    @Autowired
    private OfflineMessageService offlineMessageService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            // 注册会话到会话管理服务
            sessionService.registerSession(userId, session);
            
            // 发送连接成功消息
            Map<String, Object> welcomeMessage = new HashMap<>();
            welcomeMessage.put("type", "connection");
            welcomeMessage.put("status", "connected");
            welcomeMessage.put("userId", userId);
            welcomeMessage.put("timestamp", System.currentTimeMillis());
            
            sendToSession(session, welcomeMessage);
            
            // 处理用户上线，推送离线消息
            offlineMessageService.handleUserOnline(userId);
            
            logger.info("用户 {} 建立WebSocket连接成功", userId);
        } else {
            logger.warn("WebSocket连接缺少用户ID，关闭连接");
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> data = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) data.get("type");
            Long userId = (Long) session.getAttributes().get("userId");

            if (userId == null) {
                sendError(session, "用户未认证");
                return;
            }

            // 更新心跳
            sessionService.updateHeartbeat(session.getId());

            switch (type) {
                case "private":
                    handlePrivateMessage(userId, data);
                    break;
                case "group":
                    handleGroupMessage(userId, data);
                    break;
                case "typing":
                    handleTypingIndicator(userId, data);
                    break;
                case "read_receipt":
                    handleReadReceipt(userId, data);
                    break;
                case "heartbeat":
                    handleHeartbeat(userId, session);
                    break;
                case "get_online_users":
                    handleGetOnlineUsers(userId);
                    break;
                default:
                    sendError(session, "未知的消息类型: " + type);
            }
        } catch (Exception e) {
            logger.error("处理WebSocket消息失败", e);
            sendError(session, "消息处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理私聊消息
     */
    private void handlePrivateMessage(Long fromUserId, Map<String, Object> data) {
        try {
            Long toUserId = Long.valueOf(data.get("toUserId").toString());
            String content = (String) data.get("content");
            Integer messageType = data.get("messageType") != null ? 
                Integer.valueOf(data.get("messageType").toString()) : 1;

            // 保存消息到数据库
            Message message = messageService.sendPrivateMessage(fromUserId, toUserId, content, messageType);

            // 构建响应消息
            Map<String, Object> response = new HashMap<>();
            response.put("type", "private");
            response.put("fromUserId", fromUserId);
            response.put("toUserId", toUserId);
            response.put("message", message);
            response.put("timestamp", System.currentTimeMillis());

            // 发送给发送者（确认送达）
            sessionService.sendToUser(fromUserId, response);

            // 使用离线消息服务推送给接收者（支持离线推送）
            offlineMessageService.pushMessageToUser(toUserId, message);

            logger.debug("私聊消息发送成功: {} -> {}", fromUserId, toUserId);
        } catch (Exception e) {
            logger.error("处理私聊消息失败", e);
        }
    }

    /**
     * 处理群聊消息
     */
    private void handleGroupMessage(Long fromUserId, Map<String, Object> data) {
        try {
            Long groupId = Long.valueOf(data.get("groupId").toString());
            String content = (String) data.get("content");
            Integer messageType = data.get("messageType") != null ? 
                Integer.valueOf(data.get("messageType").toString()) : 1;

            // 保存消息到数据库
            Message message = messageService.sendGroupMessage(fromUserId, groupId, content, messageType);

            // 构建响应消息
            Map<String, Object> response = new HashMap<>();
            response.put("type", "group");
            response.put("fromUserId", fromUserId);
            response.put("groupId", groupId);
            response.put("message", message);
            response.put("timestamp", System.currentTimeMillis());

            // TODO: 获取群成员列表并发送给所有在线成员
            // 暂时只发送给发送者作为确认
            sessionService.sendToUser(fromUserId, response);

            logger.debug("群聊消息发送成功: 用户{} -> 群{}", fromUserId, groupId);
        } catch (Exception e) {
            logger.error("处理群聊消息失败", e);
        }
    }

    /**
     * 处理正在输入指示器
     */
    private void handleTypingIndicator(Long userId, Map<String, Object> data) {
        try {
            Long toUserId = Long.valueOf(data.get("toUserId").toString());
            Boolean isTyping = (Boolean) data.get("isTyping");

            Map<String, Object> response = new HashMap<>();
            response.put("type", "typing");
            response.put("fromUserId", userId);
            response.put("isTyping", isTyping);
            response.put("timestamp", System.currentTimeMillis());

            if (sessionService.isUserOnline(toUserId)) {
                sessionService.sendToUser(toUserId, response);
            }
        } catch (Exception e) {
            logger.error("处理输入指示器失败", e);
        }
    }

    /**
     * 处理已读回执
     */
    private void handleReadReceipt(Long userId, Map<String, Object> data) {
        try {
            Long messageId = Long.valueOf(data.get("messageId").toString());
            messageService.markMessageAsRead(messageId);

            Map<String, Object> response = new HashMap<>();
            response.put("type", "read_receipt");
            response.put("messageId", messageId);
            response.put("readBy", userId);
            response.put("timestamp", System.currentTimeMillis());

            sessionService.sendToUser(userId, response);
        } catch (Exception e) {
            logger.error("处理已读回执失败", e);
        }
    }

    /**
     * 处理心跳消息
     */
    private void handleHeartbeat(Long userId, WebSocketSession session) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "heartbeat");
            response.put("status", "ok");
            response.put("timestamp", System.currentTimeMillis());

            sendToSession(session, response);
        } catch (Exception e) {
            logger.error("处理心跳消息失败", e);
        }
    }

    /**
     * 处理获取在线用户列表请求
     */
    private void handleGetOnlineUsers(Long userId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "online_users");
            response.put("userIds", sessionService.getOnlineUserIds());
            response.put("timestamp", System.currentTimeMillis());

            sessionService.sendToUser(userId, response);
        } catch (Exception e) {
            logger.error("获取在线用户列表失败", e);
        }
    }

    /**
     * 发送消息到指定会话
     */
    private void sendToSession(WebSocketSession session, Object message) {
        try {
            if (session.isOpen()) {
                String messageJson = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(messageJson));
            }
        } catch (IOException e) {
            logger.error("发送消息到会话失败: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("type", "error");
            error.put("message", errorMessage);
            error.put("timestamp", System.currentTimeMillis());
            
            sendToSession(session, error);
        } catch (Exception e) {
            logger.error("发送错误消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        String sessionId = session.getId();
        
        // 从会话管理服务中移除会话
        sessionService.removeSession(sessionId);
        
        logger.info("用户 {} 断开WebSocket连接: sessionId={}, status={}", userId, sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        String sessionId = session.getId();
        
        logger.error("WebSocket传输错误: userId={}, sessionId={}", userId, sessionId, exception);
        
        // 移除出错的会话
        sessionService.removeSession(sessionId);
    }
}