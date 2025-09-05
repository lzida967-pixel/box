-- 头像数据库存储迁移脚本
-- 执行日期：2025-09-04
-- 说明：将头像存储从文件系统改为数据库二进制存储

-- 检查当前表结构
SELECT 
    COLUMN_NAME, 
    COLUMN_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'chatapp' 
AND TABLE_NAME = 'users' 
AND COLUMN_NAME LIKE '%avatar%'
ORDER BY ORDINAL_POSITION;

-- 添加头像数据存储字段（如果不存在）
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS avatar_data LONGBLOB COMMENT '头像二进制数据';

-- 添加头像文件类型字段（如果不存在）
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS avatar_content_type VARCHAR(100) COMMENT '头像文件类型';

-- 验证字段是否添加成功
SELECT 
    COLUMN_NAME, 
    COLUMN_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'chatapp' 
AND TABLE_NAME = 'users' 
AND COLUMN_NAME LIKE '%avatar%'
ORDER BY ORDINAL_POSITION;

-- 查看当前用户数据（验证现有数据不受影响）
SELECT 
    id, 
    username, 
    avatar,
    avatar_data IS NOT NULL AS has_avatar_data,
    avatar_content_type,
    LENGTH(avatar_data) AS avatar_size_bytes
FROM users 
WHERE id IN (1, 2, 3, 9, 10)
ORDER BY id;