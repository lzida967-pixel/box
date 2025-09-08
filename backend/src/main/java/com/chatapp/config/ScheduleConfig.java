package com.chatapp.config;

import com.chatapp.service.WebSocketSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 定时任务配置
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleConfig.class);

    @Autowired
    private WebSocketSessionService sessionService;

    /**
     * 每5分钟清理一次过期的WebSocket会话
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void cleanExpiredSessions() {
        try {
            logger.debug("开始清理过期的WebSocket会话");
            sessionService.cleanExpiredSessions();
            logger.debug("清理过期WebSocket会话完成");
        } catch (Exception e) {
            logger.error("清理过期WebSocket会话失败", e);
        }
    }
}