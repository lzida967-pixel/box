package com.chatapp.service;

import com.chatapp.entity.User;
import com.chatapp.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT Token Service (Simplified Implementation)
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Service
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    @Value("${jwt.secret:defaultSecretKeyForJWTTokenGeneration}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600}")
    private long jwtExpiration;

    @Autowired
    private UserMapper userMapper;

    /**
     * Generate JWT token (simplified version)
     */
    public String generateToken(User user) {
        // Create simple token format: base64(username:UUID:timestamp)
        String tokenData = user.getUsername() + ":" + UUID.randomUUID() + ":" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(tokenData.getBytes());
    }

    /**
     * Get username from token
     */
    public String getUsernameFromToken(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token));
            return decoded.split(":")[0];
        } catch (Exception e) {
            throw new RuntimeException("Invalid token");
        }
    }

    /**
     * Get user ID from token
     */
    public Long getUserIdFromToken(String token) {
        try {
            logger.info("🔍 开始从token获取用户ID...");
            String username = getUsernameFromToken(token);
            logger.info("从token解析出的用户名: {}", username);
            
            Optional<User> userOpt = userMapper.findByUsername(username);
            logger.info("数据库查询结果: {}", userOpt.isPresent() ? "找到用户" : "用户不存在");
            
            if (userOpt.isPresent()) {
                Long userId = userOpt.get().getId();
                logger.info("✅ 成功获取用户ID: {}", userId);
                return userId;
            } else {
                logger.warn("❌ 用户不存在: {}", username);
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ 从token获取用户ID时发生异常", e);
            return null;
        }
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            logger.info("🔍 开始验证token...");
            String decoded = new String(Base64.getDecoder().decode(token));
            logger.info("解码后的token: {}", decoded);
            
            String[] parts = decoded.split(":");
            logger.info("Token分割结果: {} 部分", parts.length);
            
            // 检查格式
            if (parts.length != 3) {
                logger.warn("❌ Token格式错误: 期望3部分，实际{}部分", parts.length);
                return false;
            }
            
            String username = parts[0];
            String uuid = parts[1];
            String timestampStr = parts[2];
            
            logger.info("Token内容: 用户名={}, UUID={}, 时间戳={}", username, uuid, timestampStr);
            
            // 检查各部分是否为空
            if (username == null || username.isEmpty() || 
                uuid == null || uuid.isEmpty() || 
                timestampStr == null || timestampStr.isEmpty()) {
                logger.warn("❌ Token部分为空");
                return false;
            }
            
            // 检查时间戳格式
            try {
                long timestamp = Long.parseLong(timestampStr);
                
                // 检查是否过期（简化处理，假设24小时有效期）
                long currentTime = System.currentTimeMillis();
                long maxAge = 24 * 60 * 60 * 1000; // 24小时
                long age = currentTime - timestamp;
                
                logger.info("Token时间检查: 创建时间={}, 当前时间={}, 年龄={}小时", 
                    new java.util.Date(timestamp), 
                    new java.util.Date(currentTime), 
                    age / (1000.0 * 60 * 60));
                
                if (age > maxAge) {
                    logger.warn("❌ Token已过期: 年龄{}小时 > 最大{}小时", 
                        age / (1000.0 * 60 * 60), maxAge / (1000.0 * 60 * 60));
                    return false;
                }
                
                logger.info("✅ Token验证成功");
                return true;
            } catch (NumberFormatException e) {
                logger.warn("❌ 时间戳格式错误: {}", e.getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ Token验证过程中发生异常", e);
            return false;
        }
    }

    /**
     * Refresh token
     */
    public String refreshToken(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token));
            String[] parts = decoded.split(":");
            if (parts.length == 3) {
                String username = parts[0];
                String newTokenData = username + ":" + UUID.randomUUID() + ":" + System.currentTimeMillis();
                return Base64.getEncoder().encodeToString(newTokenData.getBytes());
            }
            throw new RuntimeException("Invalid token");
        } catch (Exception e) {
            throw new RuntimeException("Token refresh failed");
        }
    }
}