import { api } from './config'
import type { AxiosResponse } from 'axios'

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
  login: (loginData: LoginRequest): Promise<AxiosResponse<ApiResponse<LoginResponse>>> => {
    return api.post('/auth/login', loginData)
  },

  /**
   * 用户注册
   */
  register: (registerData: RegisterRequest): Promise<AxiosResponse<ApiResponse<UserInfo>>> => {
    return api.post('/auth/register', registerData)
  },

  /**
   * 刷新令牌
   */
  refreshToken: (refreshToken: string): Promise<AxiosResponse<ApiResponse<RefreshTokenResponse>>> => {
    return api.post('/auth/refresh', null, {
      params: { refreshToken }
    })
  },

  /**
   * 检查用户名是否可用
   */
  checkUsername: (username: string): Promise<AxiosResponse<ApiResponse<boolean>>> => {
    return api.get('/auth/check-username', { params: { username } })
  },

  /**
   * 检查邮箱是否可用
   */
  checkEmail: (email: string): Promise<AxiosResponse<ApiResponse<boolean>>> => {
    return api.get('/auth/check-email', { params: { email } })
  }
}

// ==================== 用户相关API ====================

export const userApi = {
  /**
   * 获取当前用户信息 (兼容旧版调用)
   */
  getCurrentUser: (): Promise<AxiosResponse<ApiResponse<UserInfo>>> => {
    return api.get('/user/profile')
  },

  /**
   * 获取用户信息
   */
  getProfile: (): Promise<AxiosResponse<ApiResponse<UserInfo>>> => {
    return api.get('/user/profile')
  },

  /**
   * 更新用户信息
   */
  updateProfile: (userData: Partial<UserInfo>): Promise<AxiosResponse<ApiResponse<UserInfo>>> => {
    return api.put('/user/profile', userData)
  },

  /**
   * 修改密码
   */
  changePassword: (passwordData: { oldPassword: string; newPassword: string }): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.post('/user/change-password', passwordData)
  },

  /**
   * 上传头像
   */
  uploadAvatar: (file: File): Promise<AxiosResponse<ApiResponse<string>>> => {
    const formData = new FormData()
    formData.append('avatar', file)

    // 不手动设置Content-Type，让浏览器自动处理multipart/form-data和boundary
    return api.post('/user/upload-avatar', formData)
  },

  /**
   * 更新在线状态
   */
  updateStatus: (status: 'online' | 'offline' | 'away'): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.post('/user/status', { status })
  }
}

// ==================== 聊天相关API ====================

export const chatApi = {
  /**
   * 获取私聊消息记录
   */
  getPrivateMessages: (friendId: number, page = 1, size = 50): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get('/messages/private', {
      params: {
        friendId,
        page,
        size
      }
    })
  },

  /**
   * 获取群聊消息记录
   */
  getGroupMessages: (groupId: number, page = 1, size = 50): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get('/messages/group/history', {
      params: {
        groupId,
        page,
        size
      }
    })
  },

  /**
   * 获取聊天历史记录（新增）
   */
  getChatHistory: (friendId: number, limit = 50, offset = 0): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(`/messages/chat-history/${friendId}`, {
      params: {
        limit,
        offset
      }
    })
  },

  /**
   * 获取离线消息（新增）
   */
  getOfflineMessages: (): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get('/messages/offline')
  },

  /**
   * 标记离线消息为已读（新增）
   */
  markOfflineMessagesAsRead: (messageIds: number[]): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.put('/messages/offline/mark-read', { messageIds })
  },

  /**
   * 发送文本消息（兼容老接口占位，后端若无 /messages/send 可忽略）
   */
  sendMessage: (message: any): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/messages/send', message)
  },

  /**
   * 发送图片消息（二进制存储）
   * 后端建议提供 POST /images 上传图片并返回 { imageId }
   * 然后再创建消息：POST /messages/private { toUserId, content: imageId, messageType: 2 }
   * 如果后端提供一站式接口，也可直接 POST /messages/private/image
   */
  sendImage: async (friendId: number, file: File): Promise<AxiosResponse<ApiResponse<any>>> => {
    try {
      const fd = new FormData()
      fd.append('file', file) // 确保表单键为 file
      const imgResp = await api.post('/images', fd) as AxiosResponse<ApiResponse<{ id: number }>>
      const imageIdRaw = (imgResp.data && (imgResp.data as any).data && (imgResp.data as any).data.id) ?? (imgResp.data as any).id
      const imageId = Number(imageIdRaw)
      if (!Number.isFinite(imageId)) {
        console.error('[上传图片] 返回体异常:', imgResp?.data)
        throw new Error('图片上传失败：未获得有效的图片ID')
      }
      return await api.post('/messages/private', {
        toUserId: friendId,
        content: String(imageId),
        messageType: 2
      })
    } catch (error: any) {
      const status = error?.response?.status
      const respData = error?.response?.data
      const msg = respData?.message || error?.message
      console.error('[上传失败详情]', { status, respData, message: msg })
      throw error
    }
  },

  /**
   * 发送群聊图片消息（二进制存储）
   * 先上传图片获取ID，然后发送群聊消息
   */
  sendGroupImage: async (groupId: number, file: File): Promise<AxiosResponse<ApiResponse<any>>> => {
    try {
      const fd = new FormData()
      fd.append('file', file) // 确保表单键为 file
      const imgResp = await api.post('/images', fd) as AxiosResponse<ApiResponse<{ id: number }>>
      const imageIdRaw = (imgResp.data && (imgResp.data as any).data && (imgResp.data as any).data.id) ?? (imgResp.data as any).id
      const imageId = Number(imageIdRaw)
      if (!Number.isFinite(imageId)) {
        console.error('[上传群聊图片] 返回体异常:', imgResp?.data)
        throw new Error('群聊图片上传失败：未获得有效的图片ID')
      }
      return await api.post('/messages/group', {
        groupId: groupId,
        content: String(imageId),
        messageType: 2
      })
    } catch (error: any) {
      const status = error?.response?.status
      const respData = error?.response?.data
      const msg = respData?.message || error?.message
      console.error('[群聊图片上传失败详情]', { status, respData, message: msg })
      throw error
    }
  },

  /**
   * 获取会话列表
   */
  getConversations: (): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get('/chat/conversations')
  },

  /**
   * 创建私聊会话
   */
  createPrivateConversation: (userId: string): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/chat/conversation/private', { userId })
  },

  /**
   * 创建群聊会话
   */
  createGroupConversation: (userIds: string[], name: string): Promise<AxiosResponse<ApiResponse<any>>> => {
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
  getContacts: (): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get('/contacts')
  },

  /**
   * 搜索用户
   */
  searchUsers: (keyword: string): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get('/contacts/search', { params: { keyword } })
  },

  /**
   * 添加好友请求
   */
  addFriend: (userId: string, message?: string): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.post('/contacts/add', {
      userId,
      message
    })
  },

  /**
   * 处理好友请求
   */
  handleFriendRequest: (requestId: string, action: 'accept' | 'reject'): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.post(`/contacts/request/${requestId}/${action}`)
  },

  /**
   * 获取收到的好友请求
   */
  getReceivedRequests: (): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get('/contacts/requests/received')
  },

  /**
   * 获取发送的好友请求
   */
  getSentRequests: (): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get('/contacts/requests/sent')
  },

  /**
   * 获取好友列表
   */
  getFriends: (): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get('/contacts/friends')
  },

  /**
   * 删除联系人
   */
  removeContact: (userId: string): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.delete(`/contacts/${userId}`)
  },

  /**
   * 检查好友关系
   */
  checkFriendship: (friendId: number): Promise<AxiosResponse<ApiResponse<{ isFriend: boolean }>>> => {
    return api.get(`/contacts/check/${friendId}`)
  },

  /**
   * 更新好友备注
   */
  updateFriendNickname: (friendId: string, nickname: string): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.put(`/contacts/${friendId}/nickname`, { nickname })
  },

  /**
   * 删除用户头像
   */
  deleteAvatar: (userId: string): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.delete(`/user/avatar/${userId}`)
  },

  /**
   * 获取不在指定群内的好友列表
   */
  getFriendsNotInGroup: (groupId: number): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get(`/contacts/friends/not-in-group/${groupId}`)
  }
}

// ==================== 文件上传相关API ====================

export const fileApi = {
  /**
   * 上传文件
   */
  upload: (file: File, type: 'image' | 'file' = 'file'): Promise<AxiosResponse<ApiResponse<string>>> => {
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
  getDownloadUrl: (fileId: string): Promise<AxiosResponse<ApiResponse<string>>> => {
    return api.get(`/file/download/${fileId}`)
  }
}

// ==================== 图片（二进制）获取API ====================
export const imageApi = {
  /** 获取图片二进制URL（供前端img使用） */
  url: (id: string | number) => `http://localhost:8080/api/images/${id}`,
  
  /** 通过文件名获取图片URL */
  urlByFilename: (filename: string) => `http://localhost:8080/api/images/file/${encodeURIComponent(filename)}`
}

// ==================== 群聊相关API ====================

export const groupApi = {
  /**
   * 获取用户的群聊列表
   */
  getUserGroups: (): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get('/group/user-groups')
  },

  /**
   * 创建群聊
   */
  createGroup: (groupData: any): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/group/create', groupData)
  },

  /**
   * 获取群聊详情
   */
  getGroupDetail: (groupId: number): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(`/group/${groupId}`)
  },

  /**
   * 获取群成员列表
   */
  getGroupMembers: (groupId: number): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get(`/group/${groupId}/members`)
  },

  /**
   * 邀请成员加入群聊
   */
  inviteMembers: (groupId: number, memberIds: number[]): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.post(`/group/${groupId}/invite`, { memberIds })
  },

  /**
   * 移除群成员（批量）
   */
  removeMembers: (groupId: number, memberIds: number[]): Promise<AxiosResponse<ApiResponse<boolean>>> => {
    return api.delete(`/group/${groupId}/members`, { data: { memberIds } })
  },

  /**
   * 禁言成员
   */
  muteMembers: (groupId: number, memberIds: number[], duration: number): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.post(`/group/${groupId}/mute`, { memberIds, duration })
  },

  /**
   * 禁言单个成员
   */
  muteMember: (groupId: number, memberId: number, duration: number): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.post(`/group/${groupId}/mute/${memberId}`, { duration })
  },

  /**
   * 解除禁言
   */
  unmuteMembers: (groupId: number, memberIds: number[]): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.post(`/group/${groupId}/unmute`, { memberIds })
  },

  /**
   * 解除单个成员禁言
   */
  unmuteMember: (groupId: number, memberId: number): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.delete(`/group/${groupId}/mute/${memberId}`)
  },

  /**
   * 更新成员角色
   */
  updateMemberRole: (groupId: number, memberId: number, role: string): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.put(`/group/${groupId}/member/${memberId}/role`, { role })
  },

  /**
   * 更新群昵称
   */
  updateMemberNickname: (groupId: number, nickname: string): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.put(`/group/${groupId}/nickname`, { nickname })
  },

  /**
   * 更新群备注
   */
  updateGroupRemark: (groupId: number, remark: string): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.put(`/group/${groupId}/remark`, { remark })
  },

  /**
   * 退出群聊
   */
  leaveGroup: (groupId: number): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.post(`/group/${groupId}/leave`)
  },

  /**
   * 解散群聊
   */
  dissolveGroup: (groupId: number): Promise<AxiosResponse<ApiResponse<void>>> => {
    return api.delete(`/group/${groupId}`)
  },

  /**
   * 更新群信息
   */
  updateGroup: (groupId: number, groupData: any): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.put(`/group/${groupId}`, groupData)
  },

  /**
   * 上传群头像
   */
  uploadAvatar: (groupId: number, file: File): Promise<AxiosResponse<ApiResponse<string>>> => {
    const formData = new FormData()
    formData.append('avatar', file)
    return api.post(`/group/${groupId}/avatar`, formData)
  },

  /**
   * 检查是否为群成员
   */
  isGroupMember: (groupId: number, userId: number): Promise<AxiosResponse<ApiResponse<boolean>>> => {
    return api.get(`/group/${groupId}/member/${userId}/check`)
  },

  /**
   * 检查成员是否被禁言
   */
  isMemberMuted: (groupId: number, userId: number): Promise<AxiosResponse<ApiResponse<boolean>>> => {
    return api.get(`/group/${groupId}/member/${userId}/muted`)
  },

  /**
   * 获取群成员ID列表
   */
  getGroupMemberIds: (groupId: number): Promise<AxiosResponse<ApiResponse<number[]>>> => {
    return api.get(`/group/${groupId}/member-ids`)
  }
}

// ==================== 系统相关API ====================

export const systemApi = {
  /**
   * 健康检查
   */
  health: (): Promise<AxiosResponse<ApiResponse<string>>> => {
    return api.get('/test/hello')
  },

  /**
   * 获取系统信息
   */
  getSystemInfo: (): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get('/system/info')
  }
}

// 默认导出所有 API
export default {
  auth: authApi,
  user: userApi,
  chat: chatApi,
  contact: contactApi,
  group: groupApi,
  file: fileApi,
  image: imageApi,
  system: systemApi
}