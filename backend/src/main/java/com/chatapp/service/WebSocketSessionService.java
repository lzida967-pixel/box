package com.chatapp.service;

import com.chatapp.entity.UserSession;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Set;

/**
 * WebSocket会话管理服务接口
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public interface WebSocketSessionService {

    /**
     * 用户连接时注册会话
     */
    void registerSession(Long userId, WebSocketSession session);

    /**
     * 用户断开连接时移除会话
     */
    void removeSession(String sessionId);

    /**
     * 根据用户ID获取WebSocket会话
     */
    List<WebSocketSession> getSessionsByUserId(Long userId);

    /**
     * 根据会话ID获取WebSocket会话
     */
    WebSocketSession getSessionById(String sessionId);

    /**
     * 更新用户心跳
     */
    void updateHeartbeat(String sessionId);

    /**
     * 检查用户是否在线
     */
    boolean isUserOnline(Long userId);

    /**
     * 获取在线用户ID列表
     */
    Set<Long> getOnlineUserIds();

    /**
     * 获取用户的在线会话数量
     */
    int getUserSessionCount(Long userId);

    /**
     * 清理过期会话
     */
    void cleanExpiredSessions();

    /**
     * 广播消息给指定用户的所有会话
     */
    void sendToUser(Long userId, Object message);

    /**
     * 广播消息给多个用户
     */
    void sendToUsers(List<Long> userIds, Object message);

    /**
     * 广播消息给所有在线用户
     */
    void broadcastToAll(Object message);

    /**
     * 发送消息给指定会话
     */
    void sendToSession(String sessionId, Object message);
}