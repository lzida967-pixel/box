-- 一键迁移脚本：先回填 images.original_name，再把 messages.content(文件名)替换为 images.id
-- 适用库：chat_app（请确保当前连接的库正确）
-- 强烈建议在执行前做好数据库备份

-- 0) 可选：备份
-- CREATE TABLE messages_backup_yyyyMMdd AS SELECT * FROM messages;
-- CREATE TABLE images_backup_yyyyMMdd AS SELECT * FROM images;

-- 1) 统计当前需要迁移的图片消息（content 非数字）
SELECT COUNT(*) AS need_migrate_count
FROM messages
WHERE message_type = 2 AND content REGEXP '^[0-9]+$' = 0;

-- 2) 用 messages 的文件名回填 images.original_name（仅回填 original_name 仍为 NULL 的记录）
--    策略：对每个“文件名”，选择 images 最新的一条记录进行回填
UPDATE images i
JOIN (
  SELECT m.content AS filename, MAX(i2.id) AS image_id
  FROM messages m
  -- 仅考虑图片消息且 content 是文件名
  JOIN images i2 ON 1=1
  WHERE m.message_type = 2 AND m.content REGEXP '^[0-9]+$' = 0
  GROUP BY m.content
) x ON i.id = x.image_id
SET i.original_name = x.filename
WHERE i.original_name IS NULL;

-- 3) 将 messages.content(文件名)替换为对应的 images.id
UPDATE messages m
JOIN (
  SELECT original_name, MAX(id) AS image_id
  FROM images
  WHERE original_name IS NOT NULL AND original_name <> ''
  GROUP BY original_name
) img ON m.message_type = 2 AND m.content = img.original_name
SET m.content = CAST(img.image_id AS CHAR);

-- 4) 统计迁移后仍未成功（content 仍为非数字）的消息数量与样例
SELECT COUNT(*) AS remain_not_migrated
FROM messages
WHERE message_type = 2 AND content REGEXP '^[0-9]+$' = 0;

-- 样例列出 50 条剩余记录（如有），用于人工处理或进一步兜底
SELECT id, from_user_id, to_user_id, content, create_time
FROM messages
WHERE message_type = 2 AND content REGEXP '^[0-9]+$' = 0
ORDER BY id DESC
LIMIT 50;

-- 5) 可选兜底（按时间近邻为 images 填充 original_name，风险较高，默认注释）
-- 假设 images.create_time 与消息 create_time 接近（±5 分钟），可尝试开启以下语句后再次执行第3步
-- UPDATE images i
-- JOIN (
--   SELECT m.id AS msg_id, m.content AS filename, m.create_time AS mtime
--   FROM messages m
--   WHERE m.message_type = 2 AND m.content REGEXP '^[0-9]+$' = 0
-- ) mm ON i.original_name IS NULL
--    AND ABS(TIMESTAMPDIFF(SECOND, i.create_time, mm.mtime)) <= 300
-- SET i.original_name = mm.filename;

-- 注意：
-- - 如存在同名文件多次上传，迁移选择 MAX(id)（最新）的图片。
-- - 如果历史 images 行在真正上传时没有保存文件名，而 messages.content 的文件名与 images 记录数量/时序不一致，
--   可能仍有部分无法自动迁移，请依据上述剩余清单人工比对并处理。