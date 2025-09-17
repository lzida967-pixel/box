package com.chatapp.controller;

import com.chatapp.dto.GroupDTO;
import com.chatapp.entity.ChatGroup;
import com.chatapp.entity.GroupAnnouncement;
import com.chatapp.service.GroupService;
import com.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

/**
 * 群组控制器
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@RestController
@RequestMapping("/group")
@CrossOrigin(origins = "*")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserService userService;

    // ==================== 群组管理 ====================

    /**
     * 创建群组
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createGroup(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", null);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            String groupName = (String) request.get("name");
            String groupDescription = (String) request.get("description");
            String avatar = (String) request.get("avatar");
            Integer maxMembers = (Integer) request.get("maxMembers");
            @SuppressWarnings("unchecked")
            List<Long> memberIds = (List<Long>) request.get("memberIds");
            
            // 设置默认值
            if (maxMembers == null) maxMembers = 200;

            if (groupName == null || groupName.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "群组名称不能为空");
                result.put("data", null);
                return ResponseEntity.ok(result);
            }

            GroupDTO group = groupService.createGroup(groupName, groupDescription, avatar, maxMembers, userId, memberIds);
            
            if (group != null) {
                result.put("code", 200);
                result.put("message", "创建群组成功");
                result.put("data", group);
            } else {
                result.put("code", 500);
                result.put("message", "创建群组失败");
                result.put("data", null);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "创建群组失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户的群组列表
     */
    @GetMapping("/user-groups")
    public ResponseEntity<Map<String, Object>> getUserGroups(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 从认证信息中获取用户名，然后通过用户服务获取用户ID
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", null);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            List<GroupDTO> groups = groupService.getUserGroups(userId);
            
            result.put("code", 200);
            result.put("message", "获取群组列表成功");
            result.put("data", groups);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取群组列表失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取群组详情
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<Map<String, Object>> getGroupDetail(
            @PathVariable Long groupId,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", null);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            GroupDTO group = groupService.getGroupById(groupId, userId);
            
            if (group != null) {
                result.put("code", 200);
                result.put("message", "获取群组详情成功");
                result.put("data", group);
            } else {
                result.put("code", 404);
                result.put("message", "群组不存在");
                result.put("data", null);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取群组详情失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 更新群组信息
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<Map<String, Object>> updateGroup(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", false);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            
            // 获取更新参数
            String groupName = (String) request.get("groupName");
            String groupDescription = (String) request.get("groupDescription");
            String remark = (String) request.get("remark");
            String announcement = (String) request.get("announcement");
            String nickname = (String) request.get("nickname");

            boolean success = true;
            StringBuilder messages = new StringBuilder();
            
            // 更新群组基本信息（群名称、描述）
            if (groupName != null || groupDescription != null) {
                boolean groupInfoSuccess = groupService.updateGroupInfo(groupId, userId, groupName, groupDescription);
                if (groupInfoSuccess) {
                    messages.append("群组信息更新成功；");
                } else {
                    success = false;
                    messages.append("群组信息更新失败；");
                }
            }
            
            // 更新群备注
            if (remark != null) {
                boolean remarkSuccess = groupService.updateGroupRemark(groupId, userId, remark);
                if (remarkSuccess) {
                    messages.append("群备注更新成功；");
                } else {
                    success = false;
                    messages.append("群备注更新失败；");
                }
            }
            
            // 更新群内昵称
            if (nickname != null) {
                boolean nicknameSuccess = groupService.updateMemberNickname(groupId, userId, nickname);
                if (nicknameSuccess) {
                    messages.append("群内昵称更新成功；");
                } else {
                    success = false;
                    messages.append("群内昵称更新失败；");
                }
            }
            
            // 更新群公告（只有群主可以更新）
            if (announcement != null) {
                boolean announcementSuccess = groupService.updateGroupAnnouncement(groupId, userId, announcement);
                if (announcementSuccess) {
                    messages.append("群公告更新成功；");
                } else {
                    success = false;
                    messages.append("群公告更新失败；");
                }
            }
            
            String message = messages.length() > 0 ? messages.toString() : "没有需要更新的内容";
            result.put("code", success ? 200 : 500);
            result.put("message", message);
            result.put("data", success);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "更新群组信息失败: " + e.getMessage());
            result.put("data", false);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 解散群组
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Map<String, Object>> dissolveGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", false);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            boolean success = groupService.dissolveGroup(groupId, userId);
            
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "解散群组成功" : "解散群组失败");
            result.put("data", success);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "解散群组失败: " + e.getMessage());
            result.put("data", false);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 搜索群组
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchGroups(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") Integer limit) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<GroupDTO> groups = groupService.searchGroups(keyword, limit);
            
            result.put("code", 200);
            result.put("message", "搜索群组成功");
            result.put("data", groups);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "搜索群组失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 上传群头像
     */
    @PostMapping("/{groupId}/avatar")
    public ResponseEntity<Map<String, Object>> uploadGroupAvatar(
            @PathVariable Long groupId,
            @RequestParam("avatar") MultipartFile file,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", false);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            
            if (file.isEmpty()) {
                result.put("code", 400);
                result.put("message", "请选择头像文件");
                result.put("data", false);
                return ResponseEntity.ok(result);
            }

            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                result.put("code", 400);
                result.put("message", "只支持图片格式");
                result.put("data", false);
                return ResponseEntity.ok(result);
            }

            // 检查文件大小（限制为2MB）
            if (file.getSize() > 2 * 1024 * 1024) {
                result.put("code", 400);
                result.put("message", "头像文件不能超过2MB");
                result.put("data", false);
                return ResponseEntity.ok(result);
            }

            boolean success = groupService.uploadGroupAvatar(groupId, userId, file.getBytes(), contentType);
            
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "上传群头像成功" : "上传群头像失败");
            result.put("data", success);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "上传群头像失败: " + e.getMessage());
            result.put("data", false);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取群头像
     */
    @GetMapping("/{groupId}/avatar")
    public ResponseEntity<byte[]> getGroupAvatar(@PathVariable Long groupId) {
        try {
            ChatGroup group = groupService.getGroupAvatar(groupId);
            
            if (group != null && group.getGroupAvatarData() != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(
                    group.getGroupAvatarContentType() != null ? 
                    group.getGroupAvatarContentType() : "image/jpeg"));
                
                return ResponseEntity.ok()
                    .headers(headers)
                    .body(group.getGroupAvatarData());
            } else {
                // 返回默认头像
                byte[] defaultAvatar = getDefaultGroupAvatar();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_JPEG);
                return ResponseEntity.ok()
                    .headers(headers)
                    .body(defaultAvatar);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 生成默认群头像
     */
    private byte[] getDefaultGroupAvatar() {
        try {
            // 创建一个简单的默认头像（蓝色圆形背景 + 白色"群"字）
            int width = 100;
            int height = 100;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            // 设置抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 绘制蓝色背景
            g2d.setColor(new Color(64, 158, 255)); // Element Plus 主题蓝色
            g2d.fillOval(0, 0, width, height);
            
            // 绘制白色"群"字
            g2d.setColor(Color.WHITE);
            Font font = new Font("Microsoft YaHei", Font.BOLD, 40);
            g2d.setFont(font);
            
            FontMetrics metrics = g2d.getFontMetrics();
            int x = (width - metrics.stringWidth("群")) / 2;
            int y = ((height - metrics.getHeight()) / 2) + metrics.getAscent();
            
            g2d.drawString("群", x, y);
            g2d.dispose();
            
            // 转换为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "JPEG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            // 如果生成失败，返回一个简单的占位符
            return new byte[0];
        }
    }

    // ==================== 成员管理 ====================

    /**
     * 获取群成员列表
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<Map<String, Object>> getGroupMembers(
            @PathVariable Long groupId,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", null);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            List<GroupDTO.GroupMemberDTO> members = groupService.getGroupMembers(groupId, userId);
            
            result.put("code", 200);
            result.put("message", "获取群成员列表成功");
            result.put("data", members);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取群成员列表失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 邀请用户加入群组
     */
    @PostMapping("/{groupId}/members")
    public ResponseEntity<Map<String, Object>> inviteUsers(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", false);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) request.get("userIds");

            if (userIds == null || userIds.isEmpty()) {
                result.put("code", 400);
                result.put("message", "请选择要邀请的用户");
                result.put("data", false);
                return ResponseEntity.ok(result);
            }

            boolean success = groupService.inviteUsers(groupId, userId, userIds);
            
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "邀请用户成功" : "邀请用户失败");
            result.put("data", success);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "邀请用户失败: " + e.getMessage());
            result.put("data", false);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 邀请成员加入群组 (前端调用的invite接口)
     */
    @PostMapping("/{groupId}/invite")
    public ResponseEntity<Map<String, Object>> inviteMembers(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", false);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            @SuppressWarnings("unchecked")
            List<Object> memberIdsObj = (List<Object>) request.get("memberIds");

            if (memberIdsObj == null || memberIdsObj.isEmpty()) {
                result.put("code", 400);
                result.put("message", "请选择要邀请的成员");
                result.put("data", false);
                return ResponseEntity.ok(result);
            }

            // 转换为Long类型的ID列表
            List<Long> memberIds = memberIdsObj.stream()
                    .map(obj -> {
                        if (obj instanceof Number) {
                            return ((Number) obj).longValue();
                        } else if (obj instanceof String) {
                            return Long.parseLong((String) obj);
                        } else {
                            throw new IllegalArgumentException("无效的成员ID格式: " + obj);
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());

            boolean success = groupService.inviteUsers(groupId, userId, memberIds);
            
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "邀请成员成功" : "邀请成员失败");
            result.put("data", success);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "邀请成员失败: " + e.getMessage());
            result.put("data", false);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 移除群成员
     */
    @DeleteMapping("/{groupId}/members")
    public ResponseEntity<Map<String, Object>> removeMembers(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", false);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            
            // 安全地转换memberIds为Long类型列表
            List<Long> memberIds = new ArrayList<>();
            Object memberIdsObj = request.get("memberIds");
            if (memberIdsObj instanceof List) {
                for (Object id : (List<?>) memberIdsObj) {
                    if (id instanceof Number) {
                        memberIds.add(((Number) id).longValue());
                    }
                }
            }

            if (memberIds.isEmpty()) {
                result.put("code", 400);
                result.put("message", "请选择要移除的成员");
                result.put("data", false);
                return ResponseEntity.ok(result);
            }

            boolean success = groupService.removeMembers(groupId, userId, memberIds);
            
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "移除成员成功" : "移除成员失败");
            result.put("data", success);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "移除成员失败: " + e.getMessage());
            result.put("data", false);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 退出群组
     */
    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Map<String, Object>> leaveGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", false);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            boolean success = groupService.leaveGroup(groupId, userId);
            
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "退出群组成功" : "退出群组失败");
            result.put("data", success);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "退出群组失败: " + e.getMessage());
            result.put("data", false);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 更新成员角色
     */
    @PutMapping("/{groupId}/members/{memberId}/role")
    public ResponseEntity<Map<String, Object>> updateMemberRole(
            @PathVariable Long groupId,
            @PathVariable Long memberId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", false);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            Integer newRole = (Integer) request.get("role");

            if (newRole == null || (newRole != 1 && newRole != 2)) {
                result.put("code", 400);
                result.put("message", "角色参数无效");
                result.put("data", false);
                return ResponseEntity.ok(result);
            }

            boolean success = groupService.updateMemberRole(groupId, userId, memberId, newRole);
            
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "更新成员角色成功" : "更新成员角色失败");
            result.put("data", success);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "更新成员角色失败: " + e.getMessage());
            result.put("data", false);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 更新群内昵称
     */
    @PutMapping("/{groupId}/nickname")
    public ResponseEntity<Map<String, Object>> updateMemberNickname(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            String nickname = (String) request.get("nickname");

            boolean success = groupService.updateMemberNickname(groupId, userId, nickname);
            
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "更新群内昵称成功" : "更新群内昵称失败");
            result.put("data", success);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "更新群内昵称失败: " + e.getMessage());
            result.put("data", false);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 更新群备注
     */
    @PutMapping("/{groupId}/remark")
    public ResponseEntity<Map<String, Object>> updateGroupRemark(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", false);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            String remark = (String) request.get("remark");

            boolean success = groupService.updateGroupRemark(groupId, userId, remark);
            
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "更新群备注成功" : "更新群备注失败");
            result.put("data", success);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "更新群备注失败: " + e.getMessage());
            result.put("data", false);
        }
        
        return ResponseEntity.ok(result);
    }

    // ==================== 禁言管理 ====================

    /**
     * 禁言群成员
     */
    @PostMapping("/{groupId}/mute")
    public ResponseEntity<Map<String, Object>> muteMembers(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", false);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            @SuppressWarnings("unchecked")
            List<Long> memberIds = (List<Long>) request.get("memberIds");
            String muteUntilStr = (String) request.get("muteUntil");

            if (memberIds == null || memberIds.isEmpty()) {
                result.put("code", 400);
                result.put("message", "请选择要禁言的成员");
                result.put("data", false);
                return ResponseEntity.ok(result);
            }

            LocalDateTime muteUntil = muteUntilStr != null ? 
                LocalDateTime.parse(muteUntilStr) : LocalDateTime.now().plusDays(1);

            boolean success = groupService.muteMembers(groupId, userId, memberIds, muteUntil);
            
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "禁言成员成功" : "禁言成员失败");
            result.put("data", success);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "禁言成员失败: " + e.getMessage());
            result.put("data", false);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 解除禁言
     */
    @PostMapping("/{groupId}/unmute")
    public ResponseEntity<Map<String, Object>> unmuteMembers(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", false);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            @SuppressWarnings("unchecked")
            List<Long> memberIds = (List<Long>) request.get("memberIds");

            if (memberIds == null || memberIds.isEmpty()) {
                result.put("code", 400);
                result.put("message", "请选择要解除禁言的成员");
                result.put("data", false);
                return ResponseEntity.ok(result);
            }

            boolean success = groupService.unmuteMembers(groupId, userId, memberIds);
            
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "解除禁言成功" : "解除禁言失败");
            result.put("data", success);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "解除禁言失败: " + e.getMessage());
            result.put("data", false);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 全员禁言
     */
    @PostMapping("/{groupId}/mute-all")
    public ResponseEntity<Map<String, Object>> muteAllMembers(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", false);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            Boolean mute = (Boolean) request.get("mute");

            if (mute == null) {
                result.put("code", 400);
                result.put("message", "参数无效");
                result.put("data", false);
                return ResponseEntity.ok(result);
            }

            boolean success = groupService.muteAllMembers(groupId, userId, mute);
            
            result.put("code", success ? 200 : 500);
            result.put("message", success ? (mute ? "开启全员禁言成功" : "关闭全员禁言成功") : "设置全员禁言失败");
            result.put("data", success);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "设置全员禁言失败: " + e.getMessage());
            result.put("data", false);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取被禁言的成员列表
     */
    @GetMapping("/{groupId}/muted-members")
    public ResponseEntity<Map<String, Object>> getMutedMembers(
            @PathVariable Long groupId,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", null);
                return ResponseEntity.ok(result);
            }
            
            Long userId = userOpt.get().getId();
            List<GroupDTO.GroupMemberDTO> mutedMembers = groupService.getMutedMembers(groupId, userId);
            
            result.put("code", 200);
            result.put("message", "获取禁言成员列表成功");
            result.put("data", mutedMembers);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取禁言成员列表失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return ResponseEntity.ok(result);
    }

    // ==================== 公告管理 ====================

    /**
     * 发布群公告
     */
    @PostMapping("/{groupId}/announcements")
    public ResponseEntity<Map<String, Object>> publishAnnouncement(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);

            if (userOpt.isEmpty()) {
                result.put("code", 500);
                result.put("message", "用户不存在: " + username);
                result.put("data", null);
                return ResponseEntity.ok(result);
            }

            Long userId = userOpt.get().getId();
            String title = (String) request.get("title");
            String content = (String) request.get("content");

            if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "公告标题和内容不能为空");
                result.put("data", null);
                return ResponseEntity.ok(result);
            }

            GroupAnnouncement announcement = groupService.publishAnnouncement(groupId, userId, title, content);
            
            if (announcement != null) {
                result.put("code", 200);
                result.put("message", "发布群公告成功");
                result.put("data", announcement);
            } else {
                result.put("code", 500);
                result.put("message", "发布群公告失败");
                result.put("data", null);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "发布群公告失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取群公告列表
     */
    @GetMapping("/{groupId}/announcements")
    public ResponseEntity<Map<String, Object>> getGroupAnnouncements(@PathVariable Long groupId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<GroupAnnouncement> announcements = groupService.getGroupAnnouncements(groupId);
            
            result.put("code", 200);
            result.put("message", "获取群公告列表成功");
            result.put("data", announcements);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取群公告列表失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取最新群公告
     */
    @GetMapping("/{groupId}/announcements/latest")
    public ResponseEntity<Map<String, Object>> getLatestAnnouncement(@PathVariable Long groupId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            GroupAnnouncement announcement = groupService.getLatestAnnouncement(groupId);
            
            result.put("code", 200);
            result.put("message", "获取最新群公告成功");
            result.put("data", announcement);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取最新群公告失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return ResponseEntity.ok(result);
    }
}