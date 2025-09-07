package com.chatapp.entity;

import java.time.LocalDateTime;

/**
 * 消息实体类
 * 对应数据库 messages 表
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public class Message {
    
    private Long id;
    private Long fromUserId;      // 发送者ID
    private Long toUserId;         // 接收者ID（私聊消息）
    private Long groupId;          // 群组ID（群聊消息）
    private Integer messageType;   // 消息类型: 1-文本, 2-图片, 3-文件, 4-语音, 5-视频, 6-系统消息
    private String content;        // 消息内容
    private String fileUrl;        // 文件URL（文件消息）
    private String fileName;       // 文件名（文件消息）
    private Long fileSize;         // 文件大小（文件消息）
    private Integer status;        // 消息状态: 0-未读, 1-已读, 2-已撤回
    private Long replyToId;        // 回复的消息ID
    private LocalDateTime sendTime; // 发送时间
    private LocalDateTime readTime; // 阅读时间
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    // 构造函数
    public Message() {
        this.messageType = 1;     // 默认文本消息
        this.status = 0;           // 默认未读
        this.deleted = 0;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.sendTime = LocalDateTime.now();
    }

    public Message(Long fromUserId, Long toUserId, String content) {
        this();
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.content = content;
    }

    public Message(Long fromUserId, Long groupId, String content, Integer messageType) {
        this();
        this.fromUserId = fromUserId;
        this.groupId = groupId;
        this.content = content;
        this.messageType = messageType;
    }

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(Long replyToId) {
        this.replyToId = replyToId;
    }

    public LocalDateTime getSendTime() {
        return sendTime;
    }

    public void setSendTime(LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }

    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", fromUserId=" + fromUserId +
                ", toUserId=" + toUserId +
                ", groupId=" + groupId +
                ", messageType=" + messageType +
                ", content='" + content + '\'' +
                ", status=" + status +
                '}';
    }
}