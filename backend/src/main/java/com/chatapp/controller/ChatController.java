package com.chatapp.controller;

import com.chatapp.entity.Message;
import com.chatapp.service.MessageService;
import com.chatapp.service.WebSocketSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 聊天控制器
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private WebSocketSessionService sessionService;

    /**
     * 获取私聊消息历史
     */
    @GetMapping("/messages/private")
    public ResponseEntity<Map<String, Object>> getPrivateMessages(
            @RequestParam Long friendId,
            @RequestParam(defaultValue = "50") Integer limit,
            @RequestParam(defaultValue = "0") Integer offset,
            Authentication authentication) {
        
        Long userId = Long.valueOf(authentication.getName());
        List<Message> messages = messageService.getPrivateMessageHistory(userId, friendId, limit, offset);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取消息历史成功");
        result.put("data", messages);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取群聊消息历史
     */
    @GetMapping("/messages/group")
    public ResponseEntity<Map<String, Object>> getGroupMessages(
            @RequestParam Long groupId,
            @RequestParam(defaultValue = "50") Integer limit,
            @RequestParam(defaultValue = "0") Integer offset) {
        
        List<Message> messages = messageService.getGroupMessageHistory(groupId, limit, offset);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取群聊消息历史成功");
        result.put("data", messages);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        int unreadCount = messageService.getUnreadMessageCount(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取未读消息数量成功");
        result.put("data", unreadCount);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 标记消息为已读
     */
    @PostMapping("/messages/{messageId}/read")
    public ResponseEntity<Map<String, Object>> markMessageAsRead(@PathVariable Long messageId) {
        boolean success = messageService.markMessageAsRead(messageId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "标记已读成功" : "标记已读失败");
        result.put("data", success);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 标记与特定用户的所有消息为已读
     */
    @PostMapping("/messages/read-all")
    public ResponseEntity<Map<String, Object>> markAllMessagesAsRead(
            @RequestParam Long fromUserId,
            Authentication authentication) {
        
        Long userId = Long.valueOf(authentication.getName());
        boolean success = messageService.markAllMessagesAsRead(userId, fromUserId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "标记全部已读成功" : "标记全部已读失败");
        result.put("data", success);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 撤回消息
     */
    @PostMapping("/messages/{messageId}/recall")
    public ResponseEntity<Map<String, Object>> recallMessage(@PathVariable Long messageId) {
        boolean success = messageService.recallMessage(messageId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "撤回消息成功" : "撤回消息失败");
        result.put("data", success);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取在线用户列表
     */
    @GetMapping("/online-users")
    public ResponseEntity<Map<String, Object>> getOnlineUsers() {
        Set<Long> onlineUserIds = sessionService.getOnlineUserIds();
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取在线用户列表成功");
        result.put("data", onlineUserIds);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/users/{userId}/online")
    public ResponseEntity<Map<String, Object>> checkUserOnline(@PathVariable Long userId) {
        boolean isOnline = sessionService.isUserOnline(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "检查用户在线状态成功");
        result.put("data", isOnline);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取最近联系人
     */
    @GetMapping("/recent-contacts")
    public ResponseEntity<Map<String, Object>> getRecentContacts(
            @RequestParam(defaultValue = "10") Integer limit,
            Authentication authentication) {
        
        Long userId = Long.valueOf(authentication.getName());
        List<Long> recentContacts = messageService.getRecentContacts(userId, limit);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取最近联系人成功");
        result.put("data", recentContacts);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 发送系统通知（管理员功能）
     */
    @PostMapping("/system/broadcast")
    public ResponseEntity<Map<String, Object>> broadcastSystemMessage(
            @RequestBody Map<String, Object> messageData) {
        
        try {
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("type", "system");
            systemMessage.put("content", messageData.get("content"));
            systemMessage.put("timestamp", System.currentTimeMillis());
            
            sessionService.broadcastToAll(systemMessage);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "系统广播发送成功");
            result.put("data", true);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "系统广播发送失败: " + e.getMessage());
            result.put("data", false);
            
            return ResponseEntity.ok(result);
        }
    }
}