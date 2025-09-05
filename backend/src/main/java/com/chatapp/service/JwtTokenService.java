package com.chatapp.service;

import com.chatapp.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

/**
 * JWT Token Service (Simplified Implementation)
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Service
public class JwtTokenService {

    @Value("${jwt.secret:defaultSecretKeyForJWTTokenGeneration}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600}")
    private long jwtExpiration;

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
     * Get user ID from token (simplified version, should query database in real project)
     */
    public Long getUserIdFromToken(String token) {
        // Return default value, should query database by username in real project
        return 1L;
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token));
            String[] parts = decoded.split(":");
            
            // 检查格式
            if (parts.length != 3) {
                return false;
            }
            
            String username = parts[0];
            String uuid = parts[1];
            String timestampStr = parts[2];
            
            // 检查各部分是否为空
            if (username == null || username.isEmpty() || 
                uuid == null || uuid.isEmpty() || 
                timestampStr == null || timestampStr.isEmpty()) {
                return false;
            }
            
            // 检查时间戳格式
            try {
                long timestamp = Long.parseLong(timestampStr);
                
                // 检查是否过期（简化处理，假设24小时有效期）
                long currentTime = System.currentTimeMillis();
                long maxAge = 24 * 60 * 60 * 1000; // 24小时
                
                if (currentTime - timestamp > maxAge) {
                    return false; // token已过期
                }
                
                return true;
            } catch (NumberFormatException e) {
                return false; // 时间戳格式错误
            }
        } catch (Exception e) {
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