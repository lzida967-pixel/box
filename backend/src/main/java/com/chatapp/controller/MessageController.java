package com.chatapp.controller;

import com.chatapp.entity.Message;
import com.chatapp.service.MessageService;
import com.chatapp.service.OfflineMessageService;
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

    @Autowired
    private OfflineMessageService offlineMessageService;

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

            // 入参兜底：当为图片消息且 content 非纯数字时，尝试读取 imageId 字段
            if (messageType == 2) {
                if (content == null || !content.toString().matches("^\\d+$")) {
                    Object imageIdObj = request.get("imageId");
                    if (imageIdObj != null && imageIdObj.toString().matches("^\\d+$")) {
                        content = imageIdObj.toString();
                    }
                }
            }

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

    /**
     * 获取用户的离线消息
     */
    @GetMapping("/offline")
    public ResponseEntity<Map<String, Object>> getOfflineMessages(
            @RequestHeader("X-User-Id") Long userId) {
        
        try {
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(400, "用户ID不能为空"));
            }
            
            List<Message> offlineMessages = offlineMessageService.getOfflineMessages(userId);
            // 确保返回的列表不为null
            if (offlineMessages == null) {
                offlineMessages = List.of();
            }
            return ResponseEntity.ok(createSuccessResponse("获取离线消息成功", offlineMessages));
        } catch (Exception e) {
            // 记录详细错误信息
            System.err.println("获取离线消息时发生错误: userId=" + userId + ", error=" + e.getMessage());
            e.printStackTrace();
            
            // 返回空列表而不是错误，避免影响用户体验
            return ResponseEntity.ok(createSuccessResponse("获取离线消息成功", List.of()));
        }
    }

    /**
     * 标记离线消息为已读
     */
    @PutMapping("/offline/mark-read")
    public ResponseEntity<Map<String, Object>> markOfflineMessagesAsRead(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> request) {
        
        try {
            @SuppressWarnings("unchecked")
            List<Long> messageIds = (List<Long>) request.get("messageIds");
            
            boolean success = offlineMessageService.markOfflineMessagesAsPushed(userId, messageIds);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("标记离线消息为已读成功", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(400, "标记离线消息为已读失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, "标记离线消息为已读失败: " + e.getMessage()));
        }
    }

    /**
     * 获取与指定用户的聊天记录（支持分页）
     */
    @GetMapping("/chat-history/{friendId}")
    public ResponseEntity<Map<String, Object>> getChatHistory(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long friendId,
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        
        try {
            List<Message> messages = messageService.getPrivateMessageHistory(userId, friendId, limit, offset);
            
            // 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("messages", messages);
            data.put("hasMore", messages.size() == limit); // 简单判断是否还有更多消息
            data.put("total", messages.size());
            
            return ResponseEntity.ok(createSuccessResponse("获取聊天记录成功", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, "获取聊天记录失败: " + e.getMessage()));
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