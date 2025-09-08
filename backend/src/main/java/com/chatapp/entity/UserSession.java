package com.chatapp.entity;

import java.time.LocalDateTime;

/**
 * 用户在线会话实体类
 * 对应数据库 user_sessions 表
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public class UserSession {
    
    private Long id;
    private Long userId;
    private String sessionId;
    private LocalDateTime connectTime;
    private LocalDateTime lastHeartbeat;
    private String clientInfo;
    private String ipAddress;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 构造函数
    public UserSession() {
        this.status = 1;
        this.connectTime = LocalDateTime.now();
        this.lastHeartbeat = LocalDateTime.now();
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public UserSession(Long userId, String sessionId) {
        this();
        this.userId = userId;
        this.sessionId = sessionId;
    }

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getConnectTime() {
        return connectTime;
    }

    public void setConnectTime(LocalDateTime connectTime) {
        this.connectTime = connectTime;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(String clientInfo) {
        this.clientInfo = clientInfo;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "id=" + id +
                ", userId=" + userId +
                ", sessionId='" + sessionId + '\'' +
                ", status=" + status +
                '}';
    }
}