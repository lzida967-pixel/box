package com.chatapp.service;

import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.LoginResponse;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.entity.User;

import java.util.Map;

/**
 * 认证服务接口
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * 用户注册
     */
    User register(RegisterRequest registerRequest);

    /**
     * 验证用户凭证
     */
    boolean validateCredentials(String username, String password);

    /**
     * 生成JWT令牌
     */
    String generateToken(User user);

    /**
     * 验证JWT令牌
     */
    boolean validateToken(String token);

    /**
     * 从令牌中获取用户信息
     */
    User getUserFromToken(String token);

    /**
     * 刷新令牌
     */
    String refreshToken(String oldToken);
};


