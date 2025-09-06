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

        // 检查是否存在已删除的好友关系记录（只检查正确方向的记录）
        Optional<Friendship> deletedFriendshipOpt = friendshipMapper.findDeletedFriendship(userId, friendId);
        Friendship friendship;
        
        if (deletedFriendshipOpt.isPresent()) {
            Friendship deletedFriendship = deletedFriendshipOpt.get();
            // 确保是正确方向的记录（user_id = userId, friend_id = friendId）
            if (deletedFriendship.getUserId().equals(userId) && deletedFriendship.getFriendId().equals(friendId)) {
                // 更新已删除的记录
                friendship = deletedFriendship;
                friendship.setStatus(0); // 重置为待确认状态
                friendship.setRequestMessage(message);
                friendship.setDeleted(0); // 恢复为未删除
                friendship.setUpdateTime(LocalDateTime.now());
                friendshipMapper.update1(friendship);
            } else {
                // 方向不对，创建新的好友请求
                friendship = createNewFriendRequest(userId, friendId, message);
            }
        } else {
            // 创建新的好友请求
            friendship = createNewFriendRequest(userId, friendId, message);
        }

        return friendship;
    }

    /**
     * 创建新的好友请求
     */
    private Friendship createNewFriendRequest(Long userId, Long friendId, String message) {
        Friendship friendship = new Friendship(userId, friendId, message);
        
        // 设置默认备注为好友的昵称
        Optional<User> friendOpt = userMapper.findById(friendId);
        if (friendOpt.isPresent()) {
            User friend = friendOpt.get();
            friendship.setNickname(friend.getNickname() != null ? friend.getNickname() : friend.getUsername());
        }
        
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

            // 检查是否存在已删除的反向好友关系记录
            Optional<Friendship> reverseDeletedFriendshipOpt = friendshipMapper.findDeletedFriendship(
                friendship.getFriendId(), friendship.getUserId());
            
            Friendship reverseFriendship;
            if (reverseDeletedFriendshipOpt.isPresent()) {
                // 恢复已删除的反向记录
                reverseFriendship = reverseDeletedFriendshipOpt.get();
                reverseFriendship.setStatus(1); // 已确认
                reverseFriendship.setRequestMessage("双向好友关系");
                reverseFriendship.setDeleted(0); // 恢复为未删除
                reverseFriendship.setUpdateTime(LocalDateTime.now());
                friendshipMapper.update1(reverseFriendship);
            } else {
                // 创建新的反向好友关系记录
                reverseFriendship = new Friendship();
                reverseFriendship.setUserId(friendship.getFriendId()); // 原请求的接收者
                reverseFriendship.setFriendId(friendship.getUserId()); // 原请求的发送者
                reverseFriendship.setStatus(1); // 已确认
                reverseFriendship.setRequestMessage("双向好友关系");
                reverseFriendship.setUpdateTime(LocalDateTime.now());
                reverseFriendship.setCreateTime(LocalDateTime.now());
                
                // 设置备注为申请好友的昵称
                Optional<User> requesterOpt = userMapper.findById(friendship.getUserId());
                if (requesterOpt.isPresent()) {
                    User requester = requesterOpt.get();
                    reverseFriendship.setNickname(requester.getNickname() != null ? requester.getNickname() : requester.getUsername());
                }

                // 插入反向好友关系记录
                friendshipMapper.insert(reverseFriendship);
            }

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
        // 查找双向的好友关系
        Optional<Friendship> friendship1Opt = friendshipMapper.findByUserAndFriend(userId, friendId);
        Optional<Friendship> friendship2Opt = friendshipMapper.findByFriendAndUser(userId, friendId);
        
        if (!friendship1Opt.isPresent() && !friendship2Opt.isPresent()) {
            throw new RuntimeException("好友关系不存在");
        }

        int deleteCount = 0;
        
        // 删除第一个方向的好友关系（A→B）
        if (friendship1Opt.isPresent()) {
            Friendship friendship1 = friendship1Opt.get();
            // 只能删除已确认的好友关系
            if (friendship1.getStatus() != 1) {
                throw new RuntimeException("只能删除已确认的好友关系");
            }
            // 逻辑删除：将status改为0（待确认），deleted改为1
            friendship1.setStatus(0);
            friendship1.setDeleted(1);
            friendship1.setUpdateTime(LocalDateTime.now());
            deleteCount += friendshipMapper.update1(friendship1);
        }
        
        // 删除第二个方向的好友关系（B→A）
        if (friendship2Opt.isPresent()) {
            Friendship friendship2 = friendship2Opt.get();
            // 只能删除已确认的好友关系
            if (friendship2.getStatus() != 1) {
                throw new RuntimeException("只能删除已确认的好友关系");
            }
            // 逻辑删除：将status改为极狐（待确认），deleted改为1
            friendship2.setStatus(0);
            friendship2.setDeleted(1);
            friendship2.setUpdateTime(LocalDateTime.now());
            deleteCount  += friendshipMapper.update1(friendship2);
        }

        return deleteCount > 0;
    }

    @Override
    @Transactional
    public boolean updateFriendNickname(Long userId, Long friendId, String nickname) {
        // 查找用户→好友方向的关系
        Optional<Friendship> friendshipOpt = friendshipMapper.findByUserAndFriend(userId, friendId);
        
        // 如果用户→好友方向的关系不存在，查找好友→用户方向的关系
        if (!friendshipOpt.isPresent()) {
            friendshipOpt = friendshipMapper.findByFriendAndUser(userId, friendId);
        }
        
        if (!friendshipOpt.isPresent()) {
            throw new RuntimeException("好友关系不存在");
        }

        Friendship friendship = friendshipOpt.get();

        // 只能更新已确认的好友关系
        if (friendship.getStatus() != 1) {
            throw new RuntimeException("只能更新已确认的好友关系");
        }

        // 确保是正确的用户在更新备注（只有主动方可以设置备注）
        // 如果当前用户是被添加方，需要找到正确的方向来更新备注
        if (!friendship.getUserId().equals(userId)) {
            // 当前用户是被添加方，需要找到用户→好友方向的关系来设置备注
            Optional<Friendship> userToFriendOpt = friendshipMapper.findByUserAndFriend(userId, friendId);
            if (userToFriendOpt.isPresent()) {
                // 如果用户→好友方向的关系存在，更新该关系的备注
                Friendship userToFriend = userToFriendOpt.get();
                userToFriend.setNickname(nickname);
                userToFriend.setUpdateTime(LocalDateTime.now());
                return friendshipMapper.update(userToFriend) > 0;
            } else {
                // 如果用户→好友方向的关系不存在，创建新的关系来存储备注
                Friendship newFriendship = new Friendship();
                newFriendship.setUserId(userId);
                newFriendship.setFriendId(friendId);
                newFriendship.setStatus(1);
                newFriendship.setNickname(nickname);
                newFriendship.setCreateTime(LocalDateTime.now());
                newFriendship.setUpdateTime(LocalDateTime.now());
                return friendshipMapper.insert(newFriendship) > 0;
            }
        }

        // 当前用户是主动方，直接更新备注
        friendship.setNickname(nickname);
        friendship.setUpdateTime(LocalDateTime.now());

        return friendshipMapper.update(friendship) > 0;
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        // 检查双向好友关系
        Optional<Friendship> friendship1 = friendshipMapper.findByUserAndFriend(userId, friendId);
        Optional<Friendship> friendship2 = friendshipMapper.findByFriendAndUser(userId, friendId);
        
        return (friendship1.isPresent() && friendship1.get().getStatus() == 1) ||
               (friendship2.isPresent() && friendship2.get().getStatus() == 1);
    }

    @Override
    public Optional<Friendship> getFriendship(Long userId, Long friendId) {
        // 优先返回用户→好友方向的关系
        Optional<Friendship> friendship = friendshipMapper.findByUserAndFriend(userId, friendId);
        if (friendship.isPresent()) {
            return friendship;
        }
        // 如果用户→好友方向的关系不存在，返回好友→用户方向的关系
        return friendshipMapper.findByFriendAndUser(userId, friendId);
    }
}