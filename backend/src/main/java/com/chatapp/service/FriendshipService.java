package com.chatapp.service;

import com.chatapp.entity.Friendship;
import com.chatapp.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * 好友关系服务接口
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public interface FriendshipService {

    /**
     * 发送好友请求
     * @param userId 发送者ID
     * @param friendId 接收者ID
     * @param message 请求消息
     * @return 好友关系
     */
    Friendship sendFriendRequest(Long userId, Long friendId, String message);

    /**
     * 处理好友请求
     * @param requestId 请求ID
     * @param accept 是否接受
     * @return 是否处理成功
     */
    boolean handleFriendRequest(Long requestId, boolean accept);

    /**
     * 获取用户的好友列表
     * @param userId 用户ID
     * @return 好友列表
     */
    List<User> getFriends(Long userId);

    /**
     * 获取用户收到的好友请求
     * @param userId 用户ID
     * @return 好友请求列表
     */
    List<Friendship> getPendingRequests(Long userId);

    /**
     * 获取用户发送的好友请求
     * @param userId 用户ID
     * @return 已发送的好友请求列表
     */
    List<Friendship> getSentRequests(Long userId);

    /**
     * 搜索可添加的用户
     * @param keyword 搜索关键词
     * @param currentUserId 当前用户ID
     * @return 用户列表
     */
    List<User> searchUsers(String keyword, Long currentUserId);

    /**
     * 删除好友关系
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否删除成功
     */
    boolean removeFriend(Long userId, Long friendId);

    /**
     * 更新好友备注
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param nickname 备注昵称
     * @return 是否更新成功
     */
    boolean updateFriendNickname(Long userId, Long friendId, String nickname);

    /**
     * 检查是否是好友关系
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否是好友
     */
    boolean isFriend(Long userId, Long friendId);

    /**
     * 检查两个用户是否是好友关系（双向检查）
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否是好友
     */
    boolean areFriends(Long userId, Long friendId);

    /**
     * 获取好友关系详情
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 好友关系
     */
    Optional<Friendship> getFriendship(Long userId, Long friendId);
}