-- 检查 messages 表结构
DESCRIBE messages;

-- 检查是否存在图片相关字段，如果不存在则添加
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'chat_app' 
  AND TABLE_NAME = 'messages' 
  AND COLUMN_NAME IN ('file_url', 'file_name', 'file_size', 'message_type');

-- 如果上面的查询结果少于4行，说明字段不完整，执行以下语句添加缺失字段：

-- 添加 message_type 字段（如果不存在）
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'chat_app' AND TABLE_NAME = 'messages' AND COLUMN_NAME = 'message_type') > 0,
    'SELECT "message_type字段已存在" as result',
    'ALTER TABLE messages ADD COLUMN message_type INT DEFAULT 1 COMMENT "消息类型：1-文本，2-图片"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 file_url 字段（如果不存在）
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'chat_app' AND TABLE_NAME = 'messages' AND COLUMN_NAME = 'file_url') > 0,
    'SELECT "file_url字段已存在" as result',
    'ALTER TABLE messages ADD COLUMN file_url VARCHAR(500) COMMENT "文件URL"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 file_name 字段（如果不存在）
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'chat_app' AND TABLE_NAME = 'messages' AND COLUMN_NAME = 'file_name') > 0,
    'SELECT "file_name字段已存在" as result',
    'ALTER TABLE messages ADD COLUMN file_name VARCHAR(255) COMMENT "文件名"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 file_size 字段（如果不存在）
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'chat_app' AND TABLE_NAME = 'messages' AND COLUMN_NAME = 'file_size') > 0,
    'SELECT "file_size字段已存在" as result',
    'ALTER TABLE messages ADD COLUMN file_size BIGINT COMMENT "文件大小（字节）"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建索引（如果不存在）
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = 'chat_app' AND TABLE_NAME = 'messages' AND INDEX_NAME = 'idx_message_type') > 0,
    'SELECT "idx_message_type索引已存在" as result',
    'CREATE INDEX idx_message_type ON messages(message_type)'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = 'chat_app' AND TABLE_NAME = 'messages' AND INDEX_NAME = 'idx_file_url') > 0,
    'SELECT "idx_file_url索引已存在" as result',
    'CREATE INDEX idx_file_url ON messages(file_url)'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 最后检查表结构
DESCRIBE messages;

-- 检查最近的消息数据
SELECT id, from_user_id, to_user_id, content, message_type, file_url, file_name, file_size, create_time 
FROM messages 
ORDER BY id DESC 
LIMIT 5;