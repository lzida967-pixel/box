-- 在 chat_app 库为 images 表补充 original_name 字段与索引
ALTER TABLE images
  ADD COLUMN original_name VARCHAR(255) NULL COMMENT '原始文件名' AFTER content_type;

CREATE INDEX idx_images_original_name ON images (original_name);