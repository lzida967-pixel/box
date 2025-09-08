package com.chatapp.mapper;

import com.chatapp.entity.UserSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户会话数据访问接口
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Mapper
public interface UserSessionMapper {

    /**
     * 根据ID查询会话
     */
    UserSession findById(Long id);

    /**
     * 根据会话ID查询
     */
    UserSession findBySessionId(String sessionId);

    /**
     * 根据用户ID查询活跃会话
     */
    List<UserSession> findActiveSessionsByUserId(Long userId);

    /**
     * 插入会话记录
     */
    int insert(UserSession userSession);

    /**
     * 更新会话信息
     */
    int update(UserSession userSession);

    /**
     * 更新心跳时间
     */
    int updateHeartbeat(@Param("sessionId") String sessionId, 
                       @Param("heartbeatTime") LocalDateTime heartbeatTime);

    /**
     * 设置会话为离线状态
     */
    int setOffline(String sessionId);

    /**
     * 根据用户ID设置所有会话为离线
     */
    int setAllOfflineByUserId(Long userId);

    /**
     * 删除会话记录
     */
    int deleteBySessionId(String sessionId);

    /**
     * 清理过期的离线会话
     */
    int cleanExpiredSessions(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 获取在线用户列表
     */
    List<Long> getOnlineUserIds();

    /**
     * 检查用户是否在线
     */
    boolean isUserOnline(Long userId);

    /**
     * 获取用户的在线会话数量
     */
    int countActiveSessionsByUserId(Long userId);
}