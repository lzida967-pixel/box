package com.chatapp.service;

import com.chatapp.service.OfflineMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 定时任务服务
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Service
public class ScheduledTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);

    @Autowired
    private OfflineMessageService offlineMessageService;

    /**
     * 每5分钟重试失败的消息推送
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void retryFailedMessagePushes() {
        try {
            logger.debug("开始重试失败的消息推送");
            offlineMessageService.retryFailedPushes();
        } catch (Exception e) {
            logger.error("重试失败的消息推送异常", e);
        }
    }

    /**
     * 每天凌晨2点清理已推送的离线消息记录
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupPushedRecords() {
        try {
            logger.info("开始清理已推送的离线消息记录");
            offlineMessageService.cleanupPushedRecords();
        } catch (Exception e) {
            logger.error("清理已推送记录异常", e);
        }
    }
}