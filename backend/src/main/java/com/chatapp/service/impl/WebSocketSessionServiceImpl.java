package com.chatapp.service.impl;

import com.chatapp.entity.UserSession;
import com.chatapp.mapper.UserSessionMapper;
import com.chatapp.service.WebSocketSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket会话管理服务实现类
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Service
public class WebSocketSessionServiceImpl implements WebSocketSessionService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionServiceImpl.class);
    private static final ObjectMapper objectMapper = createObjectMapper();

    /**
     * 创建并配置ObjectMapper，支持Java 8日期时间类型
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    // 内存中的会话管理 - sessionId -> WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // 用户ID到会话ID的映射 - userId -> Set<sessionId>
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private UserSessionMapper userSessionMapper;

    @Override
    public void registerSession(Long userId, WebSocketSession session) {
        String sessionId = session.getId();
        
        try {
            // 存储到内存
            sessions.put(sessionId, session);
            userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
            
            // 存储到数据库
            UserSession userSession = new UserSession(userId, sessionId);
            userSession.setIpAddress(getClientIpAddress(session));
            userSession.setClientInfo(getUserAgent(session));
            userSessionMapper.insert(userSession);
            
            // 设置会话属性
            session.getAttributes().put("userId", userId);
            session.getAttributes().put("connectTime", LocalDateTime.now());
            
            logger.info("用户 {} 建立WebSocket连接，会话ID: {}", userId, sessionId);
            
        } catch (Exception e) {
            logger.error("注册WebSocket会话失败: userId={}, sessionId={}", userId, sessionId, e);
        }
    }

    @Override
    public void removeSession(String sessionId) {
        try {
            WebSocketSession session = sessions.remove(sessionId);
            if (session != null) {
                Long userId = (Long) session.getAttributes().get("userId");
                if (userId != null) {
                    Set<String> userSessionSet = userSessions.get(userId);
                    if (userSessionSet != null) {
                        userSessionSet.remove(sessionId);
                        if (userSessionSet.isEmpty()) {
                            userSessions.remove(userId);
                        }
                    }
                }
                
                // 更新数据库状态
                userSessionMapper.setOffline(sessionId);
                
                logger.info("移除WebSocket会话: sessionId={}, userId={}", sessionId, userId);
            }
        } catch (Exception e) {
            logger.error("移除WebSocket会话失败: sessionId={}", sessionId, e);
        }
    }

    @Override
    public List<WebSocketSession> getSessionsByUserId(Long userId) {
        Set<String> sessionIds = userSessions.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        return sessionIds.stream()
                .map(sessions::get)
                .filter(Objects::nonNull)
                .filter(WebSocketSession::isOpen)
                .collect(Collectors.toList());
    }

    @Override
    public WebSocketSession getSessionById(String sessionId) {
        return sessions.get(sessionId);
    }

    @Override
    public void updateHeartbeat(String sessionId) {
        try {
            userSessionMapper.updateHeartbeat(sessionId, LocalDateTime.now());
        } catch (Exception e) {
            logger.error("更新心跳失败: sessionId={}", sessionId, e);
        }
    }

    @Override
    public boolean isUserOnline(Long userId) {
        // 先检查内存中的会话
        Set<String> sessionIds = userSessions.get(userId);
        if (sessionIds != null && !sessionIds.isEmpty()) {
            boolean hasActiveSession = sessionIds.stream()
                    .map(sessions::get)
                    .anyMatch(session -> session != null && session.isOpen());
            if (hasActiveSession) {
                return true;
            }
        }
        
        // 检查数据库中的会话状态
        return userSessionMapper.isUserOnline(userId);
    }

    @Override
    public Set<Long> getOnlineUserIds() {
        // 合并内存和数据库中的在线用户
        Set<Long> onlineUsers = new HashSet<>(userSessions.keySet());
        List<Long> dbOnlineUsers = userSessionMapper.getOnlineUserIds();
        onlineUsers.addAll(dbOnlineUsers);
        return onlineUsers;
    }

    @Override
    public int getUserSessionCount(Long userId) {
        Set<String> sessionIds = userSessions.get(userId);
        return sessionIds != null ? sessionIds.size() : 0;
    }

    @Override
    public void cleanExpiredSessions() {
        try {
            // 清理数据库中的过期会话（1小时前）
            LocalDateTime expireTime = LocalDateTime.now().minusHours(1);
            int cleanedCount = userSessionMapper.cleanExpiredSessions(expireTime);
            
            // 清理内存中的无效会话
            Iterator<Map.Entry<String, WebSocketSession>> iterator = sessions.entrySet().iterator();
            int memoryCleanedCount = 0;
            while (iterator.hasNext()) {
                Map.Entry<String, WebSocketSession> entry = iterator.next();
                WebSocketSession session = entry.getValue();
                if (!session.isOpen()) {
                    iterator.remove();
                    String sessionId = entry.getKey();
                    Long userId = (Long) session.getAttributes().get("userId");
                    if (userId != null) {
                        Set<String> userSessionSet = userSessions.get(userId);
                        if (userSessionSet != null) {
                            userSessionSet.remove(sessionId);
                            if (userSessionSet.isEmpty()) {
                                userSessions.remove(userId);
                            }
                        }
                    }
                    memoryCleanedCount++;
                }
            }
            
            if (cleanedCount > 0 || memoryCleanedCount > 0) {
                logger.info("清理过期会话完成: 数据库清理{}个, 内存清理{}个", cleanedCount, memoryCleanedCount);
            }
        } catch (Exception e) {
            logger.error("清理过期会话失败", e);
        }
    }

    @Override
    public void sendToUser(Long userId, Object message) {
        List<WebSocketSession> userSessions = getSessionsByUserId(userId);
        if (userSessions.isEmpty()) {
            logger.debug("用户 {} 不在线，无法发送消息", userId);
            return;
        }
        
        String messageJson = convertToJson(message);
        if (messageJson == null) {
            return;
        }
        
        for (WebSocketSession session : userSessions) {
            sendToSession(session, messageJson);
        }
    }

    @Override
    public void sendToUsers(List<Long> userIds, Object message) {
        String messageJson = convertToJson(message);
        if (messageJson == null) {
            return;
        }
        
        for (Long userId : userIds) {
            List<WebSocketSession> userSessions = getSessionsByUserId(userId);
            for (WebSocketSession session : userSessions) {
                sendToSession(session, messageJson);
            }
        }
    }

    @Override
    public void broadcastToAll(Object message) {
        String messageJson = convertToJson(message);
        if (messageJson == null) {
            return;
        }
        
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                sendToSession(session, messageJson);
            }
        }
    }

    @Override
    public void sendToSession(String sessionId, Object message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            String messageJson = convertToJson(message);
            if (messageJson != null) {
                sendToSession(session, messageJson);
            }
        }
    }

    /**
     * 发送消息到指定会话
     */
    private void sendToSession(WebSocketSession session, String message) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        } catch (IOException e) {
            logger.error("发送WebSocket消息失败: sessionId={}", session.getId(), e);
            // 移除无效会话
            removeSession(session.getId());
        }
    }

    /**
     * 将对象转换为JSON字符串
     */
    private String convertToJson(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            logger.error("转换消息为JSON失败", e);
            return null;
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(WebSocketSession session) {
        try {
            return session.getRemoteAddress() != null ? 
                   session.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取用户代理信息
     */
    private String getUserAgent(WebSocketSession session) {
        try {
            return session.getHandshakeHeaders().getFirst("User-Agent");
        } catch (Exception e) {
            return "unknown";
        }
    }
}