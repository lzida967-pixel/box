import { api } from './config'

// ==================== 类型定义 ====================

// 登录请求参数（与后端 LoginRequest 对应）
export interface LoginRequest {
  username: string // 用户名
  password: string
  rememberMe?: boolean // 记住我选项
}

// 注册请求参数极速版（与后端 RegisterRequest 对应）
export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
  email?: string
  nickname?: string
  phone?: string
}

// 用户信息（与后端 UserInfo 对应）
export interface UserInfo {
  id: number
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  signature?: string
  status?: number // 后端使用数字状态
}

// 登录响应（与后端 LoginResponse 对应）
export interface LoginResponse {
  token: string // JWT令牌
  refreshToken: string // 刷新令牌
  tokenType: string // 令牌类型
  expiresIn: number // 过期时间（秒）
  userInfo: UserInfo // 用户信息
}

// 刷新token响应
export interface RefreshTokenResponse {
  token: string // 新的JWT令牌
  refreshToken: string // 新的刷新令牌
  tokenType: string // 令牌类型
  expiresIn: number // 过期时间（秒）
}

// 统一响应格式（与后端 Result 对应）
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// ==================== 认证相关API ====================

export const authApi = {
  /**
   * 用户登录
   */
  login: (loginData: LoginRequest): Promise<ApiResponse<LoginResponse>> => {
    return api.post('/auth/login', loginData)
  },

  /**
   * 用户注册
   */
  register: (registerData: RegisterRequest): Promise<ApiResponse<UserInfo>> => {
    return api.post('/auth/register', registerData)
  },

  /**
   * 刷新令牌
   */
  refreshToken: (refreshToken: string): Promise<ApiResponse<RefreshTokenResponse>> => {
    return api.post('/auth/refresh', null, {
      params: { refreshToken }
    })
  },

  /**
   * 检查用户名是否可用
   */
  checkUsername: (username: string): Promise<ApiResponse<boolean>> => {
    return api.get('/auth/check-username', { params: { username } })
  },

  /**
   * 检查邮箱是否可用
   */
  checkEmail: (email: string): Promise<ApiResponse<boolean>> => {
    return api.get('/auth/check-email', { params: { email } })
  }
}

// ==================== 用户相关API ====================

export const userApi = {
  /**
   * 获取当前用户信息 (兼容旧版调用)
   */
  getCurrentUser: (): Promise<ApiResponse<UserInfo>> => {
    return api.get('/user/profile')
  },

  /**
   * 获取用户信息
   */
  getProfile: (): Promise<ApiResponse<UserInfo>> => {
    return api.get('/user/profile')
  },

  /**
   * 更新用户信息
   */
  updateProfile: (userData: Partial<UserInfo>): Promise<ApiResponse<UserInfo>> => {
    return api.put('/user/profile', userData)
  },

  /**
   * 修改密码
   */
  changePassword: (passwordData: { oldPassword: string; newPassword: string }): Promise<ApiResponse<void>> => {
    return api.post('/user/change-password', passwordData)
  },

  /**
   * 上传头像
   */
  uploadAvatar: (file: File): Promise<ApiResponse<string>> => {
    const formData = new FormData()
    formData.append('avatar', file)
    
    // 不手动设置Content-Type，让浏览器自动处理multipart/form-data和boundary
    return api.post('/user/upload-avatar', formData)
  },

  /**
   * 更新在线状态
   */
  updateStatus: (status: 'online' | 'offline' | 'away'): Promise<ApiResponse<void>> => {
    return api.post('/user/status', { status })
  }
}

// ==================== 聊天相关API ====================

export const chatApi = {
  /**
   * 获取聊天记录
   */
  getMessages: (conversationId: string, page = 1, size = 50): Promise<ApiResponse<any[]>> => {
    return api.get('/chat/messages', {
      params: {
        conversationId,
        page,
        size
      }
    })
  },

  /**
   * 发送消息
   */
  sendMessage: (message: any): Promise<ApiResponse<any>> => {
    return api.post('/chat/send', message)
  },

  /**
   * 获取会话列表
   */
  getConversations: (): Promise<ApiResponse<any[]>> => {
    return api.get('/chat/conversations')
  },

  /**
   * 创建私聊会话
   */
  createPrivateConversation: (userId: string): Promise<ApiResponse<any>> => {
    return api.post('/chat/conversation/private', { userId })
  },

  /**
   * 创建群聊会话
   */
  createGroupConversation: (userIds: string[], name: string): Promise<ApiResponse<any>> => {
    return api.post('/chat/conversation/group', {
      userIds,
      name
    })
  }
}

// ==================== 联系人相关API ====================

export const contactApi = {
  /**
   * 获取联系人列表
   */
  getContacts: (): Promise<ApiResponse<any[]>> => {
    return api.get('/contacts')
  },

  /**
   * 搜索用户
   */
  searchUsers: (keyword: string): Promise<ApiResponse<any[]>> => {
    return api.get('/contacts/search', { params: { keyword } })
  },

  /**
   * 添加好友请求
   */
  addFriend: (userId: string, message?: string): Promise<ApiResponse<void>> => {
    return api.post('/contacts/add', {
      userId,
      message
    })
  },

  /**
   * 处理好友请求
   */
  handleFriendRequest: (requestId: string, action: 'accept' | 'reject'): Promise<ApiResponse<void>> => {
    return api.post(`/contacts/request/${requestId}/${action}`)
  },

  /**
   * 删除联系人
   */
  removeContact: (userId: string): Promise<ApiResponse<void>> => {
    return api.delete(`/contacts/${userId}`)
  }
}

// ==================== 文件上传相关API ====================

export const fileApi = {
  /**
   * 上传文件
   */
  upload: (file: File, type: 'image' | 'file' = 'file'): Promise<ApiResponse<string>> => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', type)

    return api.post('/file/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  /**
   * 获取文件下载链接
   */
  getDownloadUrl: (fileId: string): Promise<ApiResponse<string>> => {
    return api.get(`/file/download/${fileId}`)
  }
}

// ==================== 系统相关API ====================

export const systemApi = {
  /**
   * 健康检查
   */
  health: (): Promise<ApiResponse<string>> => {
    return api.get('/test/hello')
  },

  /**
   * 获取系统信息
   */
  getSystemInfo: (): Promise<ApiResponse<any>> => {
    return api.get('/system/info')
  }
}

// 默认导出所有 API
export default {
  auth: authApi,
  user: userApi,
  chat: chatApi,
  contact: contactApi,
  file: fileApi,
  system: systemApi
}