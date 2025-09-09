# 聊天系统离线消息和消息持久化解决方案

## 问题描述

原系统存在以下问题：
1. **消息持久化问题**：虽然后端有数据库存储，但前端刷新后消息消失
2. **离线消息推送问题**：用户离线时无法收到消息，上线后也收不到离线期间的消息
3. **消息状态同步问题**：消息的已读/未读状态没有正确同步

## 解决方案概述

### 1. 后端改进

#### 1.1 离线消息服务 (`OfflineMessageService`)
- **功能**：管理离线消息的推送和存储
- **核心方法**：
  - `pushMessageToUser()`: 智能推送消息（在线直推，离线记录）
  - `getOfflineMessages()`: 获取用户离线消息
  - `handleUserOnline()`: 用户上线时推送离线消息
  - `markOfflineMessagesAsPushed()`: 标记消息为已推送

#### 1.2 消息推送记录表 (`message_push_records`)
- **用途**：记录消息推送状态，支持离线消息管理
- **字段**：
  - `message_id`: 关联的消息ID
  - `user_id`: 接收用户ID
  - `push_status`: 推送状态（0-待推送，1-已推送，2-推送失败）
  - `retry_count`: 重试次数
  - `push_time`: 推送时间

#### 1.3 定时任务服务 (`ScheduledTaskService`)
- **功能**：
  - 每5分钟重试失败的消息推送
  - 每天凌晨2点清理已推送的离线消息记录

#### 1.4 WebSocket处理器改进
- **集成离线消息服务**：用户连接时自动推送离线消息
- **智能消息推送**：使用离线消息服务替代直接WebSocket推送

### 2. 前端改进

#### 2.1 API接口扩展
新增接口：
- `getChatHistory()`: 获取聊天历史记录
- `getOfflineMessages()`: 获取离线消息
- `markOfflineMessagesAsRead()`: 标记离线消息为已读

#### 2.2 聊天Store改进
- **历史消息加载**：切换会话时自动加载历史消息
- **离线消息处理**：用户登录时自动加载离线消息
- **消息状态转换**：正确处理后端消息状态

#### 2.3 WebSocket服务优化
- **连接恢复**：连接建立后自动处理离线消息
- **消息格式统一**：支持多种消息格式的解析

## 核心功能实现

### 1. 消息持久化流程

```
用户发送消息 → 保存到数据库 → 推送给接收方
                    ↓
            前端切换会话时自动加载历史消息
```

### 2. 离线消息推送流程

```
发送消息 → 检查接收方是否在线
            ↓
        在线：直接WebSocket推送
            ↓
        离线：创建推送记录 → 用户上线时推送 → 标记为已推送
```

### 3. 消息状态同步

- **发送状态**：sending → sent → delivered → read
- **离线消息**：自动标记为delivered，用户查看后变为read
- **撤回消息**：状态变为recalled，内容显示"消息已撤回"

## 数据库变更

### 新增表：message_push_records
```sql
CREATE TABLE message_push_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    push_status INT DEFAULT 0,
    push_time TIMESTAMP NULL,
    retry_count INT DEFAULT 0,
    error_message VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_message_id (message_id),
    INDEX idx_user_id (user_id),
    INDEX idx_push_status (push_status),
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

## 测试方法

### 1. 消息持久化测试
1. 用户A和用户B进行聊天
2. 刷新浏览器或重新登录
3. 验证聊天记录是否保持

### 2. 离线消息测试
1. 用户A在线，用户B离线
2. 用户A发送消息给用户B
3. 用户B上线后应该收到离线消息

### 3. 使用测试页面
访问 `/message-test` 页面进行功能测试：
- 离线消息测试
- 聊天历史测试
- WebSocket状态监控

## 配置说明

### 后端配置
确保以下配置正确：
- 数据库连接配置
- MyBatis映射文件路径
- 定时任务启用

### 前端配置
- API基础URL配置
- WebSocket连接URL配置

## 性能优化

### 1. 数据库优化
- 消息表添加适当索引
- 推送记录表定期清理
- 分页查询历史消息

### 2. 内存优化
- 限制前端消息缓存数量
- 懒加载历史消息
- 及时清理已推送记录

### 3. 网络优化
- WebSocket心跳机制
- 消息队列处理
- 重连机制

## 监控和日志

### 1. 关键日志
- 消息推送成功/失败日志
- 用户上线/离线日志
- WebSocket连接状态日志

### 2. 性能监控
- 消息推送延迟
- 离线消息积压数量
- 数据库查询性能

## 故障排查

### 常见问题
1. **消息推送失败**：检查WebSocket连接状态和推送记录表
2. **历史消息加载失败**：检查API接口和数据库连接
3. **离线消息丢失**：检查推送记录表和定时任务执行情况

### 调试工具
- 使用测试页面监控系统状态
- 查看浏览器控制台日志
- 检查后端应用日志

## 扩展功能

### 未来可以添加的功能
1. **消息推送优先级**：重要消息优先推送
2. **批量消息推送**：减少数据库操作
3. **消息推送统计**：推送成功率统计
4. **多设备同步**：支持多设备消息同步
5. **消息加密**：端到端消息加密

## 总结

通过以上改进，系统现在支持：
- ✅ 消息持久化存储和加载
- ✅ 离线消息推送和管理
- ✅ 消息状态正确同步
- ✅ 自动重试机制
- ✅ 定时清理任务
- ✅ 完整的测试工具

这个解决方案确保了用户无论在线还是离线都能正常收发消息，提供了可靠的聊天体验。