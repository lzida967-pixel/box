# 聊天应用 - Chat App

一个基于 Vue 3 + TypeScript + Element Plus 的现代化聊天应用前端界面，模仿 QQ、微信等即时通讯软件的设计。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **TypeScript** - 类型安全的 JavaScript 超集
- **Vite** - 快速的前端构建工具
- **Element Plus** - 基于 Vue 3 的组件库
- **Pinia** - Vue 3 的状态管理库
- **Vue Router** - Vue.js 官方路由管理器

## 功能特性

### 🎨 界面设计
- 现代化的聊天界面设计
- 响应式布局，适配不同屏幕尺寸
- 类似 QQ/微信 的用户体验
- 优雅的动画过渡效果

### 💬 聊天功能
- 实时消息显示
- 支持文本消息
- 消息状态显示（已读/未读）
- 消息时间戳
- 表情包支持
- 图片和文件发送

### 👥 联系人管理
- 联系人列表显示
- 在线状态指示
- 搜索联系人功能
- 未读消息数量提醒

### 📱 用户界面
- 左侧联系人列表
- 中间聊天窗口
- 消息输入框
- 工具栏（图片、文件、表情）

## 项目结构

```
src/
├── components/          # 组件目录
│   ├── ContactList.vue  # 联系人列表组件
│   ├── ChatHeader.vue   # 聊天头部组件
│   ├── MessageList.vue  # 消息列表组件
│   └── MessageInput.vue # 消息输入组件
├── views/              # 页面目录
│   └── ChatView.vue    # 主聊天页面
├── stores/             # 状态管理
│   └── chat.ts         # 聊天相关状态
├── types/              # 类型定义
│   ├── index.ts        # 主要类型定义
│   └── vue-shim.d.ts   # Vue 类型声明
├── router/             # 路由配置
│   └── index.ts        # 路由定义
├── utils/              # 工具函数
├── main.ts             # 应用入口
├── App.vue             # 根组件
└── style.css           # 全局样式
```

## 安装和运行

### 前提条件
- Node.js (版本 16+)
- npm 或 yarn

### 安装依赖
```bash
npm install
# 或
yarn install
```

### 运行开发服务器
```bash
npm run dev
# 或
yarn dev
```

### 构建生产版本
```bash
npm run build
# 或
yarn build
```

### 预览生产构建
```bash
npm run preview
# 或
yarn preview
```

## 主要组件说明

### ChatView.vue
主聊天页面，包含整体布局：
- 左侧边栏（用户信息、搜索、联系人列表）
- 右侧聊天区域（聊天头部、消息列表、输入框）

### ContactList.vue
联系人列表组件：
- 显示所有聊天会话
- 支持搜索过滤
- 显示最后一条消息预览
- 未读消息数量提醒
- 在线状态指示

### MessageList.vue
消息列表组件：
- 消息气泡式显示
- 区分发送者和接收者
- 消息时间戳
- 已读状态显示
- 自动滚动到最新消息

### MessageInput.vue
消息输入组件：
- 多行文本输入
- 工具栏（图片、文件、表情）
- 表情面板
- 快捷键支持（Enter 发送，Ctrl+Enter 换行）

### ChatHeader.vue
聊天头部组件：
- 显示当前聊天对象信息
- 在线状态显示
- 操作按钮（视频通话、语音通话、更多）

## 数据模型

### User（用户）
```typescript
interface User {
  id: string
  name: string
  avatar?: string
  status: 'online' | 'offline' | 'busy'
  lastSeen?: Date
}
```

### Message（消息）
```typescript
interface Message {
  id: string
  senderId: string
  receiverId: string
  content: string
  type: 'text' | 'image' | 'file' | 'emoji'
  timestamp: Date
  isRead: boolean
}
```

### ChatSession（聊天会话）
```typescript
interface ChatSession {
  id: string
  participantId: string
  participantName: string
  participantAvatar?: string
  lastMessage?: Message
  unreadCount: number
  isOnline: boolean
}
```

## 状态管理

使用 Pinia 进行状态管理，主要包含：

- `currentUser`: 当前登录用户信息
- `contacts`: 联系人列表
- `messages`: 所有消息
- `activeContact`: 当前选中的聊天对象
- `chatSessions`: 聊天会话列表（计算属性）
- `currentChatMessages`: 当前聊天的消息列表（计算属性）

### 主要方法
- `sendMessage()`: 发送消息
- `setActiveContact()`: 设置当前聊天对象

## 样式特性

- 使用 CSS Grid 和 Flexbox 进行布局
- 响应式设计，支持不同屏幕尺寸
- 自定义滚动条样式
- 消息气泡设计
- 渐变背景和阴影效果
- 悬停和活动状态样式

## 扩展功能

项目预留了以下扩展功能的接口：

1. **实时通信**: 可集成 WebSocket 或 Socket.io
2. **文件上传**: 已预留图片和文件上传处理逻辑
3. **表情包**: 支持自定义表情包
4. **消息类型**: 支持更多消息类型（语音、视频等）
5. **群聊功能**: 可扩展群组聊天功能
6. **消息搜索**: 可添加消息搜索功能
7. **主题切换**: 可添加深色模式等主题

## 开发说明

- 组件采用 Composition API 编写
- 使用 TypeScript 提供类型安全
- 遵循 Vue 3 最佳实践
- 代码结构清晰，便于维护和扩展

## 注意事项

- 当前为前端演示项目，不包含后端服务
- 消息数据为模拟数据，实际项目需要集成后端API
- 文件上传功能需要配合后端服务实现
- 生产环境需要配置适当的构建优化