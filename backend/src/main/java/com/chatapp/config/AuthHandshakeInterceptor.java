package com.chatapp.config;

import com.chatapp.service.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket认证握手拦截器
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthHandshakeInterceptor.class);

    @Autowired
    private JwtTokenService jwtTokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            logger.info("🔍 开始WebSocket握手验证...");
            logger.info("请求URI: {}", request.getURI());
            logger.info("请求方法: {}", request.getMethod());
            logger.info("请求头: {}", request.getHeaders());
            
            // 从请求中获取token
            String token = extractToken(request);
            logger.info("提取到的token: {}", token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null");
            
            if (!StringUtils.hasText(token)) {
                logger.warn("❌ WebSocket握手失败: 缺少认证token");
                return false;
            }

            // 解码token查看内容
            try {
                String decoded = new String(java.util.Base64.getDecoder().decode(token));
                logger.info("解码后的token: {}", decoded);
            } catch (Exception e) {
                logger.warn("Token解码失败: {}", e.getMessage());
            }

            // 验证token并获取用户ID
            boolean isValid = jwtTokenService.validateToken(token);
            logger.info("Token验证结果: {}", isValid);
            
            if (!isValid) {
                logger.warn("❌ WebSocket握手失败: token无效");
                return false;
            }

            Long userId = jwtTokenService.getUserIdFromToken(token);
            logger.info("从token获取的用户ID: {}", userId);
            
            if (userId == null) {
                logger.warn("❌ WebSocket握手失败: 无法从token中获取用户ID");
                return false;
            }

            // 将用户ID存储到会话属性中
            attributes.put("userId", userId);
            attributes.put("token", token);
            
            logger.info("✅ WebSocket握手成功: userId={}", userId);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ WebSocket握手过程中发生异常", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                              WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            logger.error("WebSocket握手后发生异常", exception);
        }
    }

    /**
     * 从请求中提取token
     * 支持从查询参数和请求头中获取
     */
    private String extractToken(ServerHttpRequest request) {
        // 1. 尝试从查询参数中获取token
        URI uri = request.getURI();
        String query = uri.getQuery();
        if (StringUtils.hasText(query)) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6); // 去掉"token="前缀
                }
            }
        }

        // 2. 尝试从Authorization请求头获取
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // 去掉"Bearer "前缀
        }

        // 3. 尝试从自定义请求头获取
        String tokenHeader = request.getHeaders().getFirst("X-Auth-Token");
        if (StringUtils.hasText(tokenHeader)) {
            return tokenHeader;
        }

        return null;
    }
}