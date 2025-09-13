package com.chatapp.controller;

import com.chatapp.entity.Friendship;
import com.chatapp.entity.User;
import com.chatapp.service.FriendshipService;
import com.chatapp.service.UserService;
import com.chatapp.dto.FriendshipWithUserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 好友关系控制器
 * 处理好友添加、删除、搜索等功能
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@RestController
@RequestMapping("/contacts")
public class ContactController {

    private final FriendshipService friendshipService;
    private final UserService userService;

    @Autowired
    public ContactController(FriendshipService friendshipService, UserService userService) {
        this.friendshipService = friendshipService;
        this.userService = userService;
    }

    /**
     * 获取当前用户的好友列表
     */
    @GetMapping

    public ResponseEntity<?> getContacts() {
        try {
            Long currentUserId = getCurrentUserId();
            List<User> friends = friendshipService.getFriends(currentUserId);

            return ResponseEntity.ok(createSuccessResponse("获取好友列表成功", friends));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 搜索用户（用于添加好友）
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String keyword) {
        try {
            System.out.println("收到搜索请求 - 关键词: " + keyword);

            if (keyword == null || keyword.trim().isEmpty()) {
                System.out.println("搜索关键词为空");
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(400, "搜索关键词不能为空"));
            }

            Long currentUserId = getCurrentUserId();
            System.out.println("当前用户ID: " + currentUserId);

            List<User> users = friendshipService.searchUsers(keyword, currentUserId);
            System.out.println("找到 " + users.size() + " 个用户");

            return ResponseEntity.ok(createSuccessResponse("搜索用户成功", users));
        } catch (Exception e) {
            System.err.println("搜索用户失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 发送好友请求
     */
    @PostMapping("/add")
    public ResponseEntity<?> addFriend(@RequestBody Map<String, Object> requestData) {
        try {
            Object userIdObj = requestData.get("userId");
            if (userIdObj == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(400, "用户ID不能为空"));
            }

            Long friendId;
            if (userIdObj instanceof String) {
                friendId = Long.parseLong((String) userIdObj);
            } else if (userIdObj instanceof Number) {
                friendId = ((Number) userIdObj).longValue();
            } else {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(400, "用户ID格式错误"));
            }

            String message = (String) requestData.get("message");
            if (message == null) {
                message = "我是" + getCurrentUsername() + "，请通过我的好友申请。";
            }

            Long currentUserId = getCurrentUserId();
            Friendship friendship = friendshipService.sendFriendRequest(currentUserId, friendId, message);

            return ResponseEntity.ok(createSuccessResponse("好友请求发送成功", friendship));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 处理好友请求
     */
    @PostMapping("/request/{requestId}/{action}")
    public ResponseEntity<?> handleFriendRequest(
            @PathVariable Long requestId,
            @PathVariable String action) {
        try {
            boolean accept = "accept".equals(action);
            boolean success = friendshipService.handleFriendRequest(requestId, accept);

            if (success) {
                String message = accept ? "已接受好友请求" : "已拒绝好友请求";
                return ResponseEntity.ok(createSuccessResponse(message, null));
            } else {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(400, "处理好友请求失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 获取收到的好友请求
     */
    @GetMapping("/requests/received")
    public ResponseEntity<?> getReceivedRequests() {
        try {
            Long currentUserId = getCurrentUserId();
            List<Friendship> requests = friendshipService.getPendingRequests(currentUserId);

            // 将Friendship转换为包含用户信息的DTO
            List<FriendshipWithUserDTO> requestDTOs = requests.stream().map(friendship -> {
                User fromUser = userService.findById(friendship.getUserId()).orElse(null);
                return new FriendshipWithUserDTO(friendship, fromUser, null);
            }).collect(Collectors.toList());

            return ResponseEntity.ok(createSuccessResponse("获取好友请求成功", requestDTOs));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 获取发送的好友请求
     */
    @GetMapping("/requests/sent")
    public ResponseEntity<?> getSentRequests() {
        try {
            Long currentUserId = getCurrentUserId();
            List<Friendship> requests = friendshipService.getSentRequests(currentUserId);

            // 将Friendship转换为包含用户信息的DTO
            List<FriendshipWithUserDTO> requestDTOs = requests.stream().map(friendship -> {
                User toUser = userService.findById(friendship.getFriendId()).orElse(null);
                return new FriendshipWithUserDTO(friendship, null, toUser);
            }).collect(Collectors.toList());

            return ResponseEntity.ok(createSuccessResponse("获取发送的请求成功", requestDTOs));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    public ResponseEntity<?> removeFriend(@PathVariable Long friendId) {
        try {
            Long currentUserId = getCurrentUserId();
            boolean success = friendshipService.removeFriend(currentUserId, friendId);

            if (success) {
                return ResponseEntity.ok(createSuccessResponse("删除好友成功", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(400, "删除好友失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 更新好友备注
     */
    @PutMapping("/{friendId}/nickname")
    public ResponseEntity<?> updateFriendNickname(
            @PathVariable Long friendId,
            @RequestBody Map<String, String> requestData) {
        try {
            String nickname = requestData.get("nickname");
            if (nickname == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(400, "备注昵称不能为空"));
            }

            Long currentUserId = getCurrentUserId();
            boolean success = friendshipService.updateFriendNickname(currentUserId, friendId, nickname);

            if (success) {
                return ResponseEntity.ok(createSuccessResponse("更新好友备注成功", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(400, "更新好友备注失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return user.getId();
    }

    /**
     * 获取当前用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    /**
     * 创建成功响应
     */
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("data", null);
        return response;
    }

    /**
     * 获取好友列表（用于邀请成员等场景）
     */
    @GetMapping("/friends")
    public ResponseEntity<?> getFriends() {
        try {
            Long currentUserId = getCurrentUserId();
            List<User> friends = friendshipService.getFriends(currentUserId);

            return ResponseEntity.ok(createSuccessResponse("获取好友列表成功", friends));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 获取不在指定群内的好友列表（用于邀请群成员）
     */
    @GetMapping("/friends/not-in-group/{groupId}")
    public ResponseEntity<?> getFriendsNotInGroup(@PathVariable Long groupId) {
        try {
            Long currentUserId = getCurrentUserId();
            List<User> friends = friendshipService.getFriendsNotInGroup(currentUserId, groupId);

            return ResponseEntity.ok(createSuccessResponse("获取可邀请好友列表成功", friends));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 检查好友关系
     */
    @GetMapping("/check/{friendId}")
    public ResponseEntity<?> checkFriendship(@PathVariable Long friendId) {
        try {
            Long currentUserId = getCurrentUserId();
            boolean isFriend = friendshipService.areFriends(currentUserId, friendId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("isFriend", isFriend);
            
            return ResponseEntity.ok(createSuccessResponse("检查好友关系成功", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }
}