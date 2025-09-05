# 测试账号说明

## 🔐 当前可用的测试账号

### 方式1：手动输入登录

| 用户名 | 密码 | 显示名称 | 状态 | 邮箱 |
|--------|------|----------|------|------|
| zhangsan | 123456 | 张三 | 在线 | zhangsan@example.com |
| lisi | 123456 | 李四 | 离开 | lisi@example.com |
| wangwu | 123456 | 王五 | 离线 | wangwu@example.com |
| admin | 123456 | 管理员 | 在线 | admin@example.com |

### 方式2：快速登录（点击头像）

在登录页面的"快速登录"区域，可以直接点击用户头像进行登录：
- 张三
- 李四  
- 王五
- 管理员

## 🧪 测试建议

### 推荐测试流程：
1. **使用zhangsan账号登录**（最活跃的测试账号）
   - 用户名：`zhangsan`
   - 密码：`123456`

2. **测试不同登录方式**：
   - 用户名登录：`zhangsan`
   - 邮箱登录：`zhangsan@example.com`

3. **测试快速登录功能**：
   - 点击张三的头像

4. **测试管理员功能**：
   - 用户名：`admin`
   - 密码：`123456`

## 🔍 密码说明

**重要**：所有测试账号的密码都是 `123456`

数据库中存储的是BCrypt加密后的密码：
```
$2a$10$YOUiEVKwn7BoEvVf/jzGluKHSjBpB7hGRcvQNFt0.3c1WmDmjLv0O
```

这个哈希值对应的明文密码是 `123456`。

## 🚨 常见登录问题

### 1. 密码错误
- ✅ **正确密码**：`123456`
- ❌ **错误密码**：`admin123`、`password`、`12345678` 等

### 2. 用户名错误
- ✅ **正确用户名**：`zhangsan`、`lisi`、`wangwu`、`admin`
- ❌ **错误用户名**：`user1`、`user2`、`user3`、`张三`、`李四` 等

### 3. 记住正确的格式
- 用户名使用**英文拼音**
- 密码统一为**6位数字**：`123456`

## 📝 账号数据来源

这些测试账号定义在以下文件中：
- **数据库初始化**：`/backend/src/main/resources/sql/init.sql`
- **前端快速登录**：`/src/stores/auth.ts`

## 🔄 重置密码（如果需要）

如果需要重置某个账号的密码，可以在数据库中执行：

```sql
-- 重置zhangsan的密码为123456
UPDATE users 
SET password = '$2a$10$YOUiEVKwn7BoEvVf/jzGluKHSjBpB7hGRcvQNFt0.3c1WmDmjLv0O' 
WHERE username = 'zhangsan';
```

---

**最后更新**：2025-09-02
**状态**：✅ 已验证可用