-- 将 messages 表中图片消息的文件名，迁移为 images.id（基于 images.original_name 的精确匹配）
-- 前置条件：已执行 image_binary_storage_migration.sql 并确保 images.original_name 在历史记录中被正确填充
-- 注意：如果存在同名文件多次上传，仅会匹配到 latest(id) 记录

-- 先备份将要更新的记录（建议在生产前手动备份整表）
-- CREATE TABLE messages_backup_yyyyMMdd AS SELECT * FROM messages;

-- 执行迁移（MySQL 8.0+ 支持）
UPDATE messages m
JOIN (
  SELECT original_name, MAX(id) AS image_id
  FROM images
  WHERE original_name IS NOT NULL AND original_name <> ''
  GROUP BY original_name
) img ON m.message_type = 2 AND m.content = img.original_name
SET m.content = CAST(img.image_id AS CHAR);

-- 可选：查看还有哪些图片消息未成功迁移（content 非数字）
SELECT m.id, m.content
FROM messages m
WHERE m.message_type = 2 AND m.content REGEXP '^[0-9]+$' = 0;