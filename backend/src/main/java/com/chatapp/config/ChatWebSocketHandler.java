package com.chatapp.config;

import com.chatapp.entity.Message;
import com.chatapp.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket消息处理器
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MessageService messageService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从session属性中获取用户ID
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.put(userId, session);
            System.out.println("用户 " + userId + " 已连接");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> data = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) data.get("type");
            Long userId = (Long) session.getAttributes().get("userId");

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
            }
        } catch (Exception e) {
            sendError(session, "消息处理失败: " + e.getMessage());
        }
    }

    private void handlePrivateMessage(Long fromUserId, Map<String, Object> data) throws IOException {
        Long toUserId = Long.valueOf(data.get("toUserId").toString());
        String content = (String) data.get("content");
        Integer messageType = data.get("messageType") != null ? 
            Integer.valueOf(data.get("messageType").toString()) : 1;

        // 保存消息到数据库
        Message message = messageService.sendPrivateMessage(fromUserId, toUserId, content, messageType);

        // 构建响应消息
        Map<String, Object> response = Map.of(
            "type", "private",
            "fromUserId", fromUserId,
            "message", message,
            "timestamp", System.currentTimeMillis()
        );

        // 发送给发送者（确认送达）
        sendToUser(fromUserId, response);

        // 发送给接收者（如果在线）
        if (userSessions.containsKey(toUserId)) {
            sendToUser(toUserId, response);
        }
    }

    private void handleGroupMessage(Long fromUserId, Map<String, Object> data) throws IOException {
        Long groupId = Long.valueOf(data.get("groupId").toString());
        String content = (String) data.get("content");
        Integer messageType = data.get("messageType") != null ? 
            Integer.valueOf(data.get("messageType").toString()) : 1;

        // 保存消息到数据库
        Message message = messageService.sendGroupMessage(fromUserId, groupId, content, messageType);

        // 构建响应消息
        Map<String, Object> response = Map.of(
            "type", "group",
            "fromUserId", fromUserId,
            "groupId", groupId,
            "message", message,
            "timestamp", System.currentTimeMillis()
        );

        // 发送给所有在线群成员（这里需要实现群成员管理）
        // 暂时只发送给发送者作为确认
        sendToUser(fromUserId, response);
    }

    private void handleTypingIndicator(Long userId, Map<String, Object> data) throws IOException {
        Long toUserId = Long.valueOf(data.get("toUserId").toString());
        Boolean isTyping = (Boolean) data.get("isTyping");

        Map<String, Object> response = Map.of(
            "type", "typing",
            "fromUserId", userId,
            "isTyping", isTyping,
            "timestamp", System.currentTimeMillis()
        );

        if (userSessions.containsKey(toUserId)) {
            sendToUser(toUserId, response);
        }
    }

    private void handleReadReceipt(Long userId, Map<String, Object> data) throws IOException {
        Long messageId = Long.valueOf(data.get("messageId").toString());
        messageService.markMessageAsRead(messageId);

        Map<String, Object> response = Map.of(
            "type", "read_receipt",
            "messageId", messageId,
            "timestamp", System.currentTimeMillis()
        );

        sendToUser(userId, response);
    }

    private void sendToUser(Long userId, Object message) throws IOException {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) throws IOException {
        Map<String, Object> error = Map.of(
            "type", "error",
            "message", errorMessage,
            "timestamp", System.currentTimeMillis()
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
            System.out.println("用户 " + userId + " 已断开连接");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket传输错误: " + exception.getMessage());
    }
}