-- 图片消息功能数据库迁移脚本
-- 执行此脚本为聊天应用添加图片消息支持

-- 1. 检查messages表是否已包含必要字段（当前表结构已包含这些字段）
-- 如果需要，可以添加索引来优化图片消息查询性能

-- 为消息类型添加索引（如果不存在）- 兼容 MySQL（不支持 IF NOT EXISTS）
SET @idx_exists := (SELECT COUNT(1) FROM information_schema.statistics 
  WHERE table_schema = DATABASE() AND table_name = 'messages' AND index_name = 'idx_messages_message_type');
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_messages_message_type ON messages(message_type);', 'SELECT ''idx_messages_message_type exists'';');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 为文件相关字段添加索引（如果不存在）- 兼容 MySQL
SET @idx2_exists := (SELECT COUNT(1) FROM information_schema.statistics 
  WHERE table_schema = DATABASE() AND table_name = 'messages' AND index_name = 'idx_messages_file_url');
SET @sql2 := IF(@idx2_exists = 0, 'CREATE INDEX idx_messages_file_url ON messages(file_url);', 'SELECT ''idx_messages_file_url exists'';');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- 2. 创建图片上传记录表（可选，用于跟踪上传的图片）
CREATE TABLE IF NOT EXISTS image_uploads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '上传记录ID',
    user_id BIGINT NOT NULL COMMENT '上传用户ID',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_filename VARCHAR(255) NOT NULL COMMENT '存储文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    content_type VARCHAR(100) NOT NULL COMMENT '文件MIME类型',
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    status INT DEFAULT 1 COMMENT '状态: 0-已删除, 1-正常',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_user_id (user_id),
    INDEX idx_stored_filename (stored_filename),
    INDEX idx_upload_time (upload_time),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片上传记录表';

-- 3. 更新消息类型说明（注释）
-- 消息类型定义：
-- 1 - 文本消息
-- 2 - 图片消息  ← 新增支持
-- 3 - 文件消息
-- 4 - 语音消息
-- 5 - 视频消息
-- 6 - 系统消息

-- 4. 插入一些示例数据（可选，用于测试）
-- 注意：这里的用户ID需要根据实际情况调整

-- 示例：用户1向用户2发送图片消息
-- INSERT INTO messages (from_user_id, to_user_id, message_type, content, file_url, file_name, file_size, status, send_time, create_time, update_time) 
-- VALUES (1, 2, 2, '发送了一张图片', '/api/files/image/example.jpg', 'example.jpg', 102400, 0, NOW(), NOW(), NOW());

-- 5. 创建视图，方便查询图片消息
CREATE OR REPLACE VIEW v_image_messages AS
SELECT 
    m.id,
    m.from_user_id,
    m.to_user_id,
    m.content,
    m.file_url,
    m.file_name,
    m.file_size,
    m.send_time,
    m.create_time,
    u1.username as sender_username,
    u1.nickname as sender_nickname,
    u2.username as receiver_username,
    u2.nickname as receiver_nickname
FROM messages m
LEFT JOIN users u1 ON m.from_user_id = u1.id
LEFT JOIN users u2 ON m.to_user_id = u2.id
WHERE m.message_type = 2 AND m.deleted = 0
ORDER BY m.send_time DESC;

-- 6. 创建存储过程，用于清理过期的图片文件记录（可选）
DELIMITER //
CREATE PROCEDURE CleanupOldImageUploads(IN days_old INT)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE file_path_var VARCHAR(500);
    DECLARE cur CURSOR FOR 
        SELECT file_path FROM image_uploads 
        WHERE upload_time < DATE_SUB(NOW(), INTERVAL days_old DAY) 
        AND status = 0;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- 标记为已删除状态
    UPDATE image_uploads 
    SET status = 0, update_time = NOW()
    WHERE upload_time < DATE_SUB(NOW(), INTERVAL days_old DAY) 
    AND status = 1;

    -- 这里可以添加实际删除文件的逻辑
    -- 注意：实际的文件删除需要在应用层处理
    
    SELECT CONCAT('已标记 ', ROW_COUNT(), ' 个过期图片记录为删除状态') as result;
END //
DELIMITER ;

-- 执行完成后的验证查询
-- 验证messages表结构
-- DESCRIBE messages;

-- 验证新创建的表
-- DESCRIBE image_uploads;

-- 查看图片消息视图
-- SELECT * FROM v_image_messages LIMIT 5;

-- 使用说明：
-- 1. 执行此脚本前请备份数据库
-- 2. 确保有足够的磁盘空间存储图片文件
-- 3. 建议定期清理过期的图片文件
-- 4. 可以根据实际需求调整文件大小限制和存储路径