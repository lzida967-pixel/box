// ==================== 用户相关类型 ====================

// 用户信息（与后端 UserInfo 对应）
export interface User {
  id: number
  username: string
  nickname?: string // 后端使用nickname
  email?: string
  phone?: string
  avatar?: string
  signature?: string
  status?: number // 后端使用数字状态
  gender?: number // 性别: 0-未知, 1-男, 2-女
  name?: string // 显示名称（兼容字段）
  remark?: string // 用户备注
  groupName?: string // 群组名称（用于群聊场景）
}

// 好友关系
export interface Friendship {
  id: number
  userId: number
  friendId: number
  status: number // 关系状态: 0-待确认, 1-已确认, 2-已拉黑
  nickname?: string // 好友备注昵称
  groupName?: string // 好友分组
  requestMessage?: string // 好友申请消息
  createTime: string
  updateTime: string
}

// 好友关系与用户信息DTO
export interface FriendshipWithUser {
  friendship: Friendship
  fromUser?: User  // 发送请求的用户（收到的请求中使用）
  toUser?: User    // 接收请求的用户（发送的请求中使用）
}

// 登录凭据（与后端 LoginRequest 对应）
export interface LoginCredentials {
  account: string // 用户名/邮箱/手机号
  password: string
}

// 注册请求（与后端 RegisterRequest 对应）
export interface RegisterCredentials {
  username: string
  displayName: string // 前端使用displayName，后端转换为nickname
  email: string
  phone?: string
  password: string
  confirmPassword: string
}

// 认证用户（包含token信息）
export interface AuthUser {
  id: string
  username: string
  name: string // 显示名称
  displayName: string // 显示名称（兼容字段）
  email?: string
  phone?: string
  avatar?: string
  avatarTimestamp?: number // 头像更新时间戳，用于防止缓存
  signature?: string
  status: 'online' | 'offline' | 'away' // 前端使用字符串状态
  // token 信息
  token: string
  refreshToken?: string
}

// 登录响应（与后端 LoginResponse 对应）
export interface LoginResponse {
  token: string // JWT令牌
  tokenType: string // 令牌类型
  expiresIn: number // 过期时间（秒）
  userInfo: User // 用户信息
}

// 模拟用户账号（仅用于演示）
export interface UserAccount {
  id: string
  username: string
  password: string
  user: {
    id: string
    name: string
    avatar: string
    status: 'online' | 'offline' | 'away'
    email: string
  }
}

// ==================== 聊天相关类型 ====================

// 消息（与后端Message实体对应）
export interface Message {
  id: number
  fromUserId: number  // 发送者ID（与后端保持一致）
  toUserId?: number   // 接收者ID（私聊时必需，群聊时可选）
  groupId?: number    // 群聊ID（群聊时必需，私聊时可选）
  content: string
  messageType: number | 'text' | 'image' | 'file' | 'system'  // 支持数字和字符串类型
  status: number | 'sending' | 'sent' | 'delivered' | 'read' | 'recalled'  // 支持数字和字符串类型
  sendTime: string    // 发送时间（与后端保持一致）
  createTime: string
  updateTime: string
  readTime?: string
  // 兼容字段（用于向后兼容）
  senderId?: number   // 映射到fromUserId
  receiverId?: number // 映射到toUserId
  isRecalled?: boolean
  recallTime?: string
}

// 会话
export interface Conversation {
  id: string
  participantIds: string[]
  lastMessage?: Message
  unreadCount: number
  timestamp: Date
  type: 'private' | 'group'
  name?: string
  avatar?: string
}

// ==================== 群聊相关类型 ====================

// 群聊信息
export interface ChatGroup {
  id: number
  name: string
  groupName?: string // 后端返回的群组名称字段
  description?: string
  avatar?: string
  ownerId: number
  ownerName?: string // 群主名称
  maxMembers: number
  isPrivate: boolean
  memberCount: number
  announcement?: string
  remark?: string
  createdAt: string
  updatedAt: string
  lastMessage?: Message
  lastMessageTime?: Date | string
  unreadCount?: number
  myNickname?: string // 当前用户在群内的昵称
}

// 群成员
export interface GroupMember {
  id: number
  groupId: number
  userId: number
  role: 'OWNER' | 'ADMIN' | 'MEMBER'
  nickname?: string
  joinedAt: string
  isMuted: boolean
  mutedUntil?: string
  user?: User
}

// 群公告
export interface GroupAnnouncement {
  id: number
  groupId: number
  content: string
  createdBy: number
  createdAt: string
  creator?: User
}

// 创建群聊请求
export interface CreateGroupRequest {
  name: string
  description?: string
  avatar?: string
  isPrivate: boolean
  maxMembers: number
  memberIds: number[]
}

// 群聊DTO
export interface GroupDTO {
  group: ChatGroup
  members: GroupMember[]
  announcement?: GroupAnnouncement
  currentUserRole?: 'OWNER' | 'ADMIN' | 'MEMBER'
}

// ==================== API响应类型 ====================

// 统一响应格式（与后端 Result 对应）
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// ==================== 表单验证类型 ====================

// 表单验证规则
export interface FormRule {
  required?: boolean
  message: string
  trigger?: string | string[]
  min?: number
  max?: number
  type?: string
  validator?: (rule: any, value: any, callback: Function) => void
}

// 表单验证规则集
export interface FormRules {
  [key: string]: FormRule[]
}

// ==================== 状态管理类型 ====================

// 认证状态
export interface AuthState {
  currentUser: AuthUser | null
  isLoggedIn: boolean
  isLoading: boolean
  error: string | null
}

// 聊天状态
export interface ChatState {
  conversations: Conversation[]
  messages: { [conversationId: string]: Message[] }
  activeConversationId: string | null
  isLoading: boolean
}

// ==================== 组件Props类型 ====================

// 用户资料组件Props
export interface UserProfileProps {
  size?: number
  showDetails?: boolean
  showArrow?: boolean
}

// 消息组件Props
export interface MessageItemProps {
  message: Message
  isOwn: boolean
  showAvatar?: boolean
}

// 会话项组件Props
export interface ConversationItemProps {
  conversation: Conversation
  isActive: boolean
  onClick: () => void
}