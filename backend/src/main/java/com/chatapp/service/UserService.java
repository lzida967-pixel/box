package com.chatapp.service;

import com.chatapp.entity.User;

import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务接口
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public interface UserService {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据ID查找用户
     */
    Optional<User> findById(Long id);

    /**
     * 创建新用户
     */
    User createUser(User user);

    /**
     * 更新用户信息
     */
    User updateUser(User user);

    /**
     * 更新用户密码
     */
    boolean updatePassword(Long userId, String newPassword);

    /**
     * 验证用户密码
     */
    boolean validatePassword(Long userId, String password);

    /**
     * 检查用户名是否已存在
     */
    boolean isUsernameExists(String username);

    /**
     * 检查邮箱是否已存在
     */
    boolean isEmailExists(String email);

    /**
     * 更新最后登录时间
     */
    void updateLastLoginTime(Long userId);

    /**
     * 删除用户（软删除）
     */
    boolean deleteUser(Long userId);

    /**
     * 上传用户头像
     * @param username 用户名
     * @param file 头像文件
     * @return 头像URL
     */
    String uploadAvatar(String username, MultipartFile file);

    List<User> findAllUsers();
}