package com.chatapp.mapper;

import com.chatapp.entity.Friendship;
import com.chatapp.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 好友关系 Mapper 接口
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Mapper
public interface FriendshipMapper {

    /**
     * 根据ID查找好友关系
     */
    Optional<Friendship> findById(Long id);

    /**
     * 查找两个用户间的好友关系（用户→好友方向）
     */
    Optional<Friendship> findByUserAndFriend(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 查找两个用户间的好友关系（好友→用户方向）
     */
    Optional<Friendship> findByFriendAndUser(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 获取用户的所有好友关系（已确认的）
     */
    List<Friendship> findConfirmedFriendshipsByUserId(Long userId);

    /**
     * 获取用户的好友列表详细信息（已确认的）
     */
    List<User> findFriendsByUserId(Long userId);

    /**
     * 获取用户收到的好友请求
     */
    List<Friendship> findPendingRequestsByUserId(Long userId);

    /**
     * 获取用户发送的好友请求
     */
    List<Friendship> findSentRequestsByUserId(Long userId);

    /**
     * 搜索用户（排除已是好友和自己的用户）
     */
    List<User> searchAvailableUsers(@Param("keyword") String keyword, @Param("currentUserId") Long currentUserId);

    /**
     * 插入好友关系
     */
    int insert(Friendship friendship);

    /**
     * 更新好友关系
     */
    int update(Friendship friendship);
    int update1(Friendship friendship);

    /**
     * 删除好友关系（软删除）
     */
    int deleteById(Long id);

    /**
     * 检查是否存在好友关系（任一方向）
     */
    boolean existsFriendship(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 查找已删除的好友关系记录
     */
    Optional<Friendship> findDeletedFriendship(@Param("userId") Long userId, @Param("friendId") Long friendId);


}