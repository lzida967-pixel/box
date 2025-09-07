package com.chatapp.controller;

import com.chatapp.entity.Message;
import com.chatapp.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息控制器
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@RestController
@RequestMapping("/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 发送私聊消息
     */
    @PostMapping("/private")
    public ResponseEntity<Map<String, Object>> sendPrivateMessage(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> request) {
        
        try {
            Long toUserId = Long.valueOf(request.get("toUserId").toString());
            String content = (String) request.get("content");
            Integer messageType = request.get("messageType") != null ? 
                Integer.valueOf(request.get("messageType").toString()) : 1;

            Message message = messageService.sendPrivateMessage(userId, toUserId, content, messageType);
            
            return ResponseEntity.ok(createSuccessResponse("消息发送成功", message));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, "发送消息失败: " + e.getMessage()));
        }
    }

    /**
     * 发送群聊消息
     */
    @PostMapping("/group")
    public ResponseEntity<Map<String, Object>> sendGroupMessage(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> request) {
        
        try {
            Long groupId = Long.valueOf(request.get("groupId").toString());
            String content = (String) request.get("content");
            Integer messageType = request.get("messageType") != null ? 
                Integer.valueOf(request.get("messageType").toString()) : 1;

            Message message = messageService.sendGroupMessage(userId, groupId, content, messageType);
            
            return ResponseEntity.ok(createSuccessResponse("群消息发送成功", message));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, "发送群消息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取私聊消息历史
     */
    @GetMapping("/private/history")
    public ResponseEntity<Map<String, Object>> getPrivateMessageHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long friendId,
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        
        try {
            List<Message> messages = messageService.getPrivateMessageHistory(userId, friendId, limit, offset);
            return ResponseEntity.ok(createSuccessResponse("获取消息历史成功", messages));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, "获取消息历史失败: " + e.getMessage()));
        }
    }

    /**
     * 获取群聊消息历史
     */
    @GetMapping("/group/history")
    public ResponseEntity<Map<String, Object>> getGroupMessageHistory(
            @RequestParam Long groupId,
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        
        try {
            List<Message> messages = messageService.getGroupMessageHistory(groupId, limit, offset);
            return ResponseEntity.ok(createSuccessResponse("获取群消息历史成功", messages));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, "获取群消息历史失败: " + e.getMessage()));
        }
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Object>> getUnreadMessageCount(
            @RequestHeader("X-User-Id") Long userId) {
        
        try {
            int count = messageService.getUnreadMessageCount(userId);
            return ResponseEntity.ok(createSuccessResponse("获取未读消息数量成功", Map.of("count", count)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, "获取未读消息数量失败: " + e.getMessage()));
        }
    }

    /**
     * 获取与特定用户的未读消息数量
     */
    @GetMapping("/unread/from-user")
    public ResponseEntity<Map<String, Object>> getUnreadMessageCountFromUser(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long fromUserId) {
        
        try {
            int count = messageService.getUnreadMessageCountFromUser(userId, fromUserId);
            return ResponseEntity.ok(createSuccessResponse("获取未读消息数量成功", Map.of("count", count)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                   .body(createErrorResponse(400, "获取未读消息数量失败: " + e.getMessage()));
        }
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/read/{messageId}")
    public ResponseEntity<Map<String, Object>> markMessageAsRead(
            @PathVariable Long messageId) {
        
        try {
            boolean success = messageService.markMessageAsRead(messageId);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("消息标记为已读成功", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(400, "消息标记为已读失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, "消息标记为已读失败: " + e.getMessage()));
        }
    }

    /**
     * 标记与特定用户的所有消息为已读
     */
    @PutMapping("/read/all")
    public ResponseEntity<Map<String, Object>> markAllMessagesAsRead(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long fromUserId) {
        
        try {
            boolean success = messageService.markAllMessagesAsRead(userId, fromUserId);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("所有消息标记为已读成功", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(400, "消息标记为已读失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, "消息标记为已读失败: " + e.getMessage()));
        }
    }

    /**
     * 撤回消息
     */
    @PutMapping("/recall/{messageId}")
    public ResponseEntity<Map<String, Object>> recallMessage(
            @PathVariable Long messageId) {
        
        try {
            boolean success = messageService.recallMessage(messageId);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("消息撤回成功", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(400, "消息撤回失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, "消息撤回失败: " + e.getMessage()));
        }
    }

    /**
     * 获取最近联系人列表
     */
    @GetMapping("/recent-contacts")
    public ResponseEntity<Map<String, Object>> getRecentContacts(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        
        try {
            List<Long> contacts = messageService.getRecentContacts(userId, limit);
            return ResponseEntity.ok(createSuccessResponse("获取最近联系人成功", contacts));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, "获取最近联系人失败: " + e.getMessage()));
        }
    }

    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    private Map<String, Object> createErrorResponse(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("data", null);
        return response;
    }
}