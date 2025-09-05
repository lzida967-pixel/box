package com.chatapp.controller;

import com.chatapp.entity.User;
import com.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 * 处理用户个人信息修改、密码修改等操作
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("未找到认证信息或认证失败");
                return ResponseEntity.status(401)
                        .body(createErrorResponse(401, "未授权访问，请重新登录"));
            }
            
            String username = authentication.getName();
            System.out.println("当前认证用户: " + username);
            
            if (username == null || "anonymousUser".equals(username)) {
                System.err.println("用户名为空或为匿名用户");
                return ResponseEntity.status(401)
                        .body(createErrorResponse(401, "未授权访问，请重新登录"));
            }
            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> {
                        System.err.println("用户不存在: " + username);
                        return new RuntimeException("用户不存在: " + username);
                    });
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("nickname", user.getNickname());
            userInfo.put("avatar", user.getAvatar());
            userInfo.put("signature", user.getSignature());
            userInfo.put("phone", user.getPhone());
            userInfo.put("gender", user.getGender());
            userInfo.put("status", user.getStatus());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取用户信息成功");
            response.put("data", userInfo);
            
            System.out.println("用户信息获取成功: " + username);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.err.println("获取用户信息失败: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(createErrorResponse(500, e.getMessage()));
        } catch (Exception e) {
            System.err.println("获取用户信息发生未知错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(createErrorResponse(500, "服务器内部错误"));
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> updateData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            System.out.println("更新用户信息 - 用户: " + username);
            System.out.println("更新数据: " + updateData);
            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 更新用户信息，增加类型安全处理
            if (updateData.containsKey("nickname")) {
                String nickname = (String) updateData.get("nickname");
                user.setNickname(nickname);
                System.out.println("更新昵称: " + nickname);
            }
            if (updateData.containsKey("signature")) {
                String signature = (String) updateData.get("signature");
                user.setSignature(signature);
                System.out.println("更新签名: " + signature);
            }
            if (updateData.containsKey("phone")) {
                String phone = (String) updateData.get("phone");
                user.setPhone(phone);
                System.out.println("更新手机: " + phone);
            }
            if (updateData.containsKey("gender")) {
                // 安全的类型转换
                Object genderObj = updateData.get("gender");
                Integer gender = null;
                if (genderObj instanceof Integer) {
                    gender = (Integer) genderObj;
                } else if (genderObj instanceof Number) {
                    gender = ((Number) genderObj).intValue();
                } else if (genderObj instanceof String) {
                    try {
                        gender = Integer.parseInt((String) genderObj);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("性别格式无效: " + genderObj);
                    }
                }
                
                if (gender != null) {
                    user.setGender(gender);
                    System.out.println("更新性别: " + gender);
                }
            }
            
            User updatedUser = userService.updateUser(user);
            System.out.println("用户信息更新成功: " + updatedUser.getUsername());
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", updatedUser.getId());
            userInfo.put("username", updatedUser.getUsername());
            userInfo.put("email", updatedUser.getEmail());
            userInfo.put("nickname", updatedUser.getNickname());
            userInfo.put("avatar", updatedUser.getAvatar());
            userInfo.put("signature", updatedUser.getSignature());
            userInfo.put("phone", updatedUser.getPhone());
            userInfo.put("gender", updatedUser.getGender());
            userInfo.put("status", updatedUser.getStatus());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "更新用户信息成功");
            response.put("data", userInfo);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("更新用户信息失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, "更新用户信息失败: " + e.getMessage()));
        }
    }

    /**
     * 上传头像
     */
    @PostMapping("/upload-avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
        try {
            System.out.println("=== 头像上传开始 ===");
            System.out.println("文件是否为空: " + (file == null || file.isEmpty()));
            if (file != null) {
                System.out.println("文件名: " + file.getOriginalFilename());
                System.out.println("文件大小: " + file.getSize() + " bytes");
                System.out.println("文件类型: " + file.getContentType());
            }
            
            if (file == null || file.isEmpty()) {
                System.err.println("错误：文件为空");
                throw new RuntimeException("请选择头像文件");
            }
            
            if (file.getSize() > 2 * 1024 * 1024) {
                System.err.println("错误：文件太大，大小: " + file.getSize());
                throw new RuntimeException("头像大小不能超过2MB");
            }
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            System.out.println("当前认证用户: " + username);
            
            System.out.println("开始调用上传服务...");
            String avatarUrl = userService.uploadAvatar(username, file);
            System.out.println("上传成功，头像URL: " + avatarUrl);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "头像上传成功");
            response.put("data", avatarUrl);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("头像上传失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 获取用户头像
     */
    @GetMapping("/avatar/{userId}")
    public ResponseEntity<byte[]> getUserAvatar(@PathVariable Long userId) {
        try {
            System.out.println("获取用户头像，用户ID: " + userId);
            
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            byte[] avatarData = user.getAvatarData();
            String contentType = user.getAvatarContentType();
            
            if (avatarData == null || avatarData.length == 0) {
                System.out.println("用户没有头像数据");
                return ResponseEntity.notFound().build();
            }
            
            System.out.println("返回头像数据，大小: " + avatarData.length + " bytes, 类型: " + contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(avatarData);
                    
        } catch (Exception e) {
            System.err.println("获取头像失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 修改密码
     */
    /**
     * 调试接口：获取所有用户列表（仅用于调试）
     */
    @GetMapping("/list-all")
    public ResponseEntity<?> listAllUsers() {
        try {
            List<User> users = userService.findAllUsers();
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取用户列表成功");
            response.put("data", users);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");
            
            if (oldPassword == null || newPassword == null) {
                throw new RuntimeException("密码不能为空");
            }
            
            if (newPassword.length() < 6) {
                throw new RuntimeException("新密码长度至少6位");
            }
            
            // 验证旧密码并更新密码
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 这里需要实现密码验证和更新逻辑
            // TODO: 添加密码验证逻辑
            boolean isPasswordValid = userService.validatePassword(user.getId(), oldPassword);
            if (!isPasswordValid) {
                throw new RuntimeException("原密码错误");
            }
            
            // 更新密码
            boolean updateSuccess = userService.updatePassword(user.getId(), newPassword);
            if (!updateSuccess) {
                throw new RuntimeException("密码更新失败");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "密码修改成功");
            response.put("data", null);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(int code, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", code);
        errorResponse.put("message", message);
        errorResponse.put("data", null);
        return errorResponse;
    }
}