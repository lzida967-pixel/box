# 快速修复500错误 - 解决方案

## 问题：登录时返回500内部服务器错误

### 🚨 立即检查清单

#### 1. MySQL数据库是否运行？
```bash
# Windows检查服务状态
sc query mysql80

# 如果没运行，启动服务
net start mysql80
```

#### 2. 数据库chat_app是否存在？
```sql
-- 连接MySQL
mysql -u root -p123456

-- 检查数据库
SHOW DATABASES LIKE 'chat_app';
```

#### 3. 用户表是否存在？
```sql
USE chat_app;
SHOW TABLES LIKE 'users';
```

### ⚡ 快速修复步骤

#### 如果数据库不存在，执行：
```sql
-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS chat_app DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 导入初始化脚本
-- 方法1：在MySQL命令行执行
SOURCE d:/code/code/backend/src/main/resources/sql/init.sql;

-- 方法2：直接执行SQL创建用户表
USE chat_app;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `display_name` varchar(100) NOT NULL COMMENT '显示名称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `password` varchar(255) NOT NULL COMMENT '密码（加密后）',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `signature` varchar(200) DEFAULT NULL COMMENT '个性签名',
  `status` varchar(20) DEFAULT 'offline' COMMENT '在线状态',
  `last_online` datetime DEFAULT NULL COMMENT '最后在线时间',
  `account_status` tinyint DEFAULT 1 COMMENT '账号状态：1-正常，0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 插入测试用户（密码是123456的BCrypt加密）
INSERT INTO `users` (`username`, `display_name`, `email`, `password`, `avatar`, `signature`, `status`) VALUES
('zhangsan', '张三', 'zhangsan@example.com', '$2a$10$YOUiEVKwn7BoEvVf/jzGluKHSjBpB7hGRcvQNFt0.3c1WmDmjLv0O', 'https://avatars.githubusercontent.com/u/1?v=4', '这是张三的个性签名', 'online'),
('admin', '管理员', 'admin@example.com', '$2a$10$YOUiEVKwn7BoEvVf/jzGluKHSjBpB7hGRcvQNFt0.3c1WmDmjLv0O', 'https://avatars.githubusercontent.com/u/4?v=4', '系统管理员', 'online');
```

#### 然后重启后端：
```bash
cd d:/code/code/backend
./mvnw spring-boot:run
```

### ✅ 验证修复

#### 1. 测试健康检查
```bash
curl http://localhost:8080/api/test/hello
```
期望响应：`"Hello, Chat Backend!"`

#### 2. 测试登录接口
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account": "zhangsan", "password": "123456"}'
```

#### 3. 在前端测试
- 用户名：`zhangsan`
- 密码：`123456`

### 🔍 如果仍然有问题

1. **查看后端启动日志**，寻找具体错误信息
2. **检查application.yml中的数据库配置**：
   - URL: `jdbc:mysql://localhost:3306/chat_app`
   - 用户名: `root`
   - 密码: `123456`
3. **确认防火墙没有阻止8080和3306端口**

### 📞 需要帮助？

如果按照以上步骤操作后仍有问题，请提供：
1. 后端完整启动日志
2. MySQL版本信息
3. 操作系统版本

---
*修复时间：2025-09-02*