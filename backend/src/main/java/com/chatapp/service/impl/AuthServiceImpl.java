package com.chatapp.service.impl;

import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.LoginResponse;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.entity.User;
import com.chatapp.service.AuthService;
import com.chatapp.service.JwtTokenService;
import com.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 认证服务实现类
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Autowired
    public AuthServiceImpl(UserService userService, 
                          PasswordEncoder passwordEncoder,
                          JwtTokenService jwtTokenService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        // 查找用户
        Optional<User> userOptional = userService.findByUsername(loginRequest.getUsername());
        if (userOptional.isEmpty()) {
            throw new RuntimeException("用户名或密码错误");
        }

        User user = userOptional.get();

        // 验证密码
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 更新最后登录时间
        userService.updateLastLoginTime(user.getId());

        // 生成JWT令牌
        String token = jwtTokenService.generateToken(user);

        // 创建响应
        return new LoginResponse(token, 3600L, user);
    }

    @Override
    @Transactional
    public User register(RegisterRequest registerRequest) {
        // 验证密码是否匹配
        if (!registerRequest.isPasswordMatch()) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        // 检查用户名是否已存在
        if (userService.isUsernameExists(registerRequest.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在（邮箱不为空时才检查）
        if (registerRequest.getEmail() != null && !registerRequest.getEmail().isEmpty() && 
            userService.isEmailExists(registerRequest.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setNickname(registerRequest.getNickname() != null ? 
            registerRequest.getNickname() : registerRequest.getUsername());
        user.setPhone(registerRequest.getPhone());
        user.setStatus(0); // 默认离线状态

        // 保存用户
        return userService.createUser(user);
    }

    @Override
    public boolean validateCredentials(String username, String password) {
        Optional<User> userOptional = userService.findByUsername(username);
        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        return passwordEncoder.matches(password, user.getPassword());
    }

    @Override
    public String generateToken(User user) {
        return jwtTokenService.generateToken(user);
    }

    @Override
    public boolean validateToken(String token) {
        return jwtTokenService.validateToken(token);
    }

    @Override
    public User getUserFromToken(String token) {
        String username = jwtTokenService.getUsernameFromToken(token);
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Override
    public String refreshToken(String oldToken) {
        try {
            // 验证token格式和有效性
            if (!validateToken(oldToken)) {
                throw new RuntimeException("无效的令牌或令牌已过期");
            }

            // 获取用户信息
            String username = jwtTokenService.getUsernameFromToken(oldToken);
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + username));
            
            // 生成新token
            return generateToken(user);
        } catch (Exception e) {
            // 记录详细错误信息
            System.err.println("RefreshToken失败: " + e.getMessage());
            throw new RuntimeException("Token刷新失败: " + e.getMessage());
        }
    }
}