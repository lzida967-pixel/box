package com.chatapp.controller;

import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.LoginResponse;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.entity.User;
import com.chatapp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理用户登录、注册、令牌刷新等认证相关请求
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.login(loginRequest);
            
            // 手动构建响应数据，避免Jackson序列化内部类的问题
            Map<String, Object> loginData = new HashMap<>();
            loginData.put("token", response.getToken());
            loginData.put("tokenType", response.getTokenType());
            loginData.put("expiresIn", response.getExpiresIn());
            loginData.put("refreshToken", response.getToken()); // 使用相同的token作为refreshToken
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", response.getUserInfo().getId());
            userInfo.put("username", response.getUserInfo().getUsername());
            userInfo.put("email", response.getUserInfo().getEmail());
            userInfo.put("nickname", response.getUserInfo().getNickname());
            userInfo.put("avatar", response.getUserInfo().getAvatar());
            userInfo.put("status", response.getUserInfo().getStatus());
            userInfo.put("signature", response.getUserInfo().getSignature());
            
            loginData.put("userInfo", userInfo);
            
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("code", 200);
            successResponse.put("message", "登录成功");
            successResponse.put("data", loginData);
            
            return ResponseEntity.ok(successResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User user = authService.register(registerRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "注册成功");
            response.put("data", user);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam(required = false) String refreshToken,
                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            String token = null;
            
            // 优先使用参数中的refreshToken
            if (refreshToken != null && !refreshToken.isEmpty()) {
                token = refreshToken;
            } else if (authorization != null && authorization.startsWith("Bearer ")) {
                // 如果没有refreshToken参数，尝试从 Authorization 头获取
                token = authorization.substring(7);
            }
            
            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(400, "缺少刷新令牌"));
            }

            String newToken = authService.refreshToken(token);

            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("token", newToken);
            tokenData.put("refreshToken", newToken); // 使用相同的token作为refreshToken
            tokenData.put("tokenType", "Bearer");
            tokenData.put("expiresIn", 3600);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "令牌刷新成功");
            response.put("data", tokenData);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, e.getMessage()));
        }
    }

    /**
     * 验证令牌
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(400, "无效的授权头"));
            }

            String token = authorization.substring(7);
            boolean isValid = authService.validateToken(token);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "令牌验证成功");
            response.put("data", Map.of("valid", isValid));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(400, "令牌验证失败"));
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