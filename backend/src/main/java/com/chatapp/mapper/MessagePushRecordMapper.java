package com.chatapp.mapper;

import com.chatapp.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息推送记录数据访问接口
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Mapper
public interface MessagePushRecordMapper {

    /**
     * 插入推送记录
     */
    int insert(Map<String, Object> record);

    /**
     * 根据用户ID查找未推送的消息
     */
    List<Message> findUnpushedMessages(@Param("userId") Long userId);

    /**
     * 标记消息为已推送
     */
    int markMessagesAsPushed(@Param("userId") Long userId, @Param("messageIds") List<Long> messageIds);

    /**
     * 更新推送状态
     */
    int updatePushStatus(@Param("messageId") Long messageId, 
                        @Param("userId") Long userId, 
                        @Param("status") Integer status, 
                        @Param("errorMessage") String errorMessage);

    /**
     * 增加重试次数
     */
    int incrementRetryCount(@Param("messageId") Long messageId, 
                           @Param("userId") Long userId, 
                           @Param("errorMessage") String errorMessage);

    /**
     * 查找失败的推送记录
     */
    List<Map<String, Object>> findFailedPushRecords(@Param("maxRetryCount") Integer maxRetryCount);

    /**
     * 删除指定时间之前的已推送记录
     */
    int deletePushedRecordsBefore(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 根据消息ID和用户ID查找推送记录
     */
    Map<String, Object> findByMessageIdAndUserId(@Param("messageId") Long messageId, @Param("userId") Long userId);

    /**
     * 删除推送记录
     */
    int deleteById(@Param("id") Long id);
}