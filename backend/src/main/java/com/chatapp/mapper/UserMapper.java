package com.chatapp.mapper;

import com.chatapp.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Mapper
public interface UserMapper {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    void insert(User user);

    void update(User user);

    int updatePassword(@Param("userId") Long userId, @Param("newPassword") String newPassword);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void updateLastLoginTime(Long userId);

    int deleteById(Long id);
    
    List<User> findAll();
}