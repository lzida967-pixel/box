package com.chatapp.service.impl;

import com.chatapp.entity.Friendship;
import com.chatapp.entity.User;
import com.chatapp.mapper.FriendshipMapper;
import com.chatapp.mapper.UserMapper;
import com.chatapp.service.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 好友关系服务实现类
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Service
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipMapper friendshipMapper;
    private final UserMapper userMapper;

    @Autowired
    public FriendshipServiceImpl(FriendshipMapper friendshipMapper, UserMapper userMapper) {
        this.friendshipMapper = friendshipMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public Friendship sendFriendRequest(Long userId, Long friendId, String message) {
        // 检查用户是否存在
        if (!userMapper.findById(userId).isPresent()) {
            throw new RuntimeException("发送者用户不存在");
        }
        if (!userMapper.findById(friendId).isPresent()) {
            throw new RuntimeException("接收者用户不存在");
        }

        // 不能添加自己为好友
        if (userId.equals(friendId)) {
            throw new RuntimeException("不能添加自己为好友");
        }

        // 检查是否已存在好友关系
        if (friendshipMapper.existsFriendship(userId, friendId)) {
            throw new RuntimeException("已存在好友关系或待处理的好友请求");
        }

        // 创建好友请求
        Friendship friendship = new Friendship(userId, friendId, message);
        friendshipMapper.insert(friendship);

        return friendship;
    }

    @Override
    @Transactional
    public boolean handleFriendRequest(Long requestId, boolean accept) {
        Optional<Friendship> friendshipOpt = friendshipMapper.findById(requestId);
        if (!friendshipOpt.isPresent()) {
            throw new RuntimeException("好友请求不存在");
        }

        Friendship friendship = friendshipOpt.get();

        // 检查请求状态
        if (friendship.getStatus() != 0) {
            throw new RuntimeException("该请求已被处理");
        }

        if (accept) {
            // 接受请求，更新状态为已确认
            friendship.setStatus(1);
            friendship.setUpdateTime(LocalDateTime.now());

            // 更新原始请求记录
            int updated = friendshipMapper.update(friendship);

            // 创建反向好友关系记录，实现双向好友关系
            Friendship reverseFriendship = new Friendship();
            reverseFriendship.setUserId(friendship.getFriendId()); // 原请求的接收者
            reverseFriendship.setFriendId(friendship.getUserId()); // 原请求的发送者
            reverseFriendship.setStatus(1); // 已确认
            reverseFriendship.setRequestMessage("双向好友关系");
            reverseFriendship.setUpdateTime(LocalDateTime.now());
            reverseFriendship.setCreateTime(LocalDateTime.now());

            // 插入反向好友关系记录
            friendshipMapper.insert(reverseFriendship);

            return updated > 0;
        } else {
            // 拒绝请求，删除记录
            return friendshipMapper.deleteById(requestId) > 0;
        }
    }

    @Override
    public List<User> getFriends(Long userId) {
        return friendshipMapper.findFriendsByUserId(userId);
    }

    @Override
    public List<Friendship> getPendingRequests(Long userId) {
        return friendshipMapper.findPendingRequestsByUserId(userId);
    }

    @Override
    public List<Friendship> getSentRequests(Long userId) {
        return friendshipMapper.findSentRequestsByUserId(userId);
    }

    @Override
    public List<User> searchUsers(String keyword, Long currentUserId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new RuntimeException("搜索关键词不能为空");
        }
        return friendshipMapper.searchAvailableUsers(keyword.trim(), currentUserId);
    }

    @Override
    @Transactional
    public boolean removeFriend(Long userId, Long friendId) {
        Optional<Friendship> friendshipOpt = friendshipMapper.findByUserAndFriend(userId, friendId);
        if (!friendshipOpt.isPresent()) {
            throw new RuntimeException("好友关系不存在");
        }

        Friendship friendship = friendshipOpt.get();

        // 只能删除已确认的好友关系
        if (friendship.getStatus() != 1) {
            throw new RuntimeException("只能删除已确认的好友关系");
        }

        return friendshipMapper.deleteById(friendship.getId()) > 0;
    }

    @Override
    @Transactional
    public boolean updateFriendNickname(Long userId, Long friendId, String nickname) {
        Optional<Friendship> friendshipOpt = friendshipMapper.findByUserAndFriend(userId, friendId);
        if (!friendshipOpt.isPresent()) {
            throw new RuntimeException("好友关系不存在");
        }

        Friendship friendship = friendshipOpt.get();

        // 只能更新已确认的好友关系
        if (friendship.getStatus() != 1) {
            throw new RuntimeException("只能更新已确认的好友关系");
        }

        // 确保是正确的用户在更新备注（只有主动方可以设置备注）
        if (!friendship.getUserId().equals(userId)) {
            // 如果当前用户是被添加方，需要交换角色
            Friendship reverseFriendship = new Friendship();
            reverseFriendship.setUserId(userId);
            reverseFriendship.setFriendId(friendId);
            reverseFriendship.setStatus(1);
            reverseFriendship.setNickname(nickname);
            reverseFriendship.setUpdateTime(LocalDateTime.now());

            // 这里需要特殊处理，或者重新设计数据结构
            // 简化处理：直接更新现有记录的备注
        }

        friendship.setNickname(nickname);
        friendship.setUpdateTime(LocalDateTime.now());

        return friendshipMapper.update(friendship) > 0;
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        Optional<Friendship> friendship = friendshipMapper.findByUserAndFriend(userId, friendId);
        return friendship.isPresent() && friendship.get().getStatus() == 1;
    }

    @Override
    public Optional<Friendship> getFriendship(Long userId, Long friendId) {
        return friendshipMapper.findByUserAndFriend(userId, friendId);
    }
}