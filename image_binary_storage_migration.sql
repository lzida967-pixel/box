-- 1) 扩展 images 表，增加 original_name
ALTER TABLE images
  ADD COLUMN original_name VARCHAR(255) NULL COMMENT '原始文件名' AFTER content_type;

-- 2) 可选：为 original_name 添加索引（提升迁移与查询按名查找性能）
CREATE INDEX idx_images_original_name ON images (original_name);

-- 注意：执行本迁移后，需要将已有 images 记录的 original_name 补齐。
-- 若历史插入未保存文件名，无法可靠映射；仅能迁移那些上传时我们保存过 original_name 的记录。