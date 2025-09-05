package com.chatapp.service.impl;

import com.chatapp.entity.User;
import com.chatapp.mapper.UserMapper;
import com.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import java.util.Optional;

/**
 * 用户服务实现类
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Autowired
    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }


    @Override
    public List<User> findAllUsers() {
        return userMapper.findAll();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    @Transactional
    public User createUser(User user) {
        userMapper.insert(user);
        return user;
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        // 设置更新时间
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
        return user;
    }

    @Override
    @Transactional
    public boolean updatePassword(Long userId, String newPassword) {
        // 使用BCrypt加密新密码
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String encodedPassword = encoder.encode(newPassword);
        return userMapper.updatePassword(userId, encodedPassword) > 0;
    }

    @Override
    public boolean validatePassword(Long userId, String password) {
        Optional<User> userOpt = userMapper.findById(userId);
        if (!userOpt.isPresent()) {
            return false;
        }
        User user = userOpt.get();
        
        // 使用Spring Security的PasswordEncoder进行密码验证
        try {
            // 这里需要注入PasswordEncoder，但为了简化实现，我们创建一个实例
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            return encoder.matches(password, user.getPassword());
        } catch (Exception e) {
            // 如果密码不是BCrypt格式，使用简单比较作为兼容性处理
            return user.getPassword().equals(password);
        }
    }

    @Override
    public boolean isUsernameExists(String username) {
        return userMapper.existsByUsername(username);
    }

    @Override
    public boolean isEmailExists(String email) {
        // 邮箱为空时认为不存在
        if (email == null || email.isEmpty()) {
            return false;
        }
        return userMapper.existsByEmail(email);
    }

    @Override
    @Transactional
    public void updateLastLoginTime(Long userId) {
        userMapper.updateLastLoginTime(userId);
    }

    @Override
    @Transactional
    public boolean deleteUser(Long userId) {
        return userMapper.deleteById(userId) > 0;
    }

    @Override
    @Transactional
    public String uploadAvatar(String username, MultipartFile file) {
        try {
            System.out.println("=== UserService.uploadAvatar 开始 (数据库存储模式) ===");
            System.out.println("用户名: " + username);
            
            // 1. 查找用户
            User user = findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            System.out.println("用户查找成功，ID: " + user.getId());
            
            // 2. 验证文件
            String originalFilename = file.getOriginalFilename();
            System.out.println("原始文件名: " + originalFilename);
            
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                throw new RuntimeException("文件名为空");
            }
            
            // 3. 获取文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                contentType = "image/jpeg"; // 默认类型
            }
            System.out.println("文件类型: " + contentType);
            System.out.println("文件大小: " + file.getSize() + " bytes");
            
            // 4. 读取文件二进制数据
            byte[] avatarData = file.getBytes();
            System.out.println("读取二进制数据成功，大小: " + avatarData.length + " bytes");
            
            // 5. 更新用户头像数据
            user.setAvatarData(avatarData);
            user.setAvatarContentType(contentType);
            user.setAvatar("avatar_" + user.getId()); // 作为标识符
            
            userMapper.update(user);
            System.out.println("用户头像数据更新成功");
            
            // 6. 返回头像访问 URL
            String avatarUrl = "/api/user/avatar/" + user.getId();
            System.out.println("生成的头像URL: " + avatarUrl);
            
            System.out.println("=== UserService.uploadAvatar 结束 ===");
            return avatarUrl;
            
        } catch (IOException e) {
            System.err.println("IO异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("头像上传失败: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("其他异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("头像上传失败: " + e.getMessage());
        }
    }
}