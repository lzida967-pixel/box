import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import type {
  AuthUser,
  LoginCredentials,
  RegisterCredentials,
  AuthState,
  UserAccount
} from '@/types'

// 模拟用户账号数据（仅用于快速登录演示，与数据库中的测试账号对应）
const mockAccounts: UserAccount[] = [
  {
    id: '1',
    username: 'zhangsan',
    password: '123456',
    user: {
      id: '1',
      name: '张三',
      avatar: 'https://avatars.githubusercontent.com/u/1?v=4',
      status: 'online',
      email: 'zhangsan@example.com'
    }
  },
  {
    id: '2',
    username: 'lisi',
    password: '123456',
    user: {
      id: '2',
      name: '李四',
      avatar: 'https://avatars.githubusercontent.com/u/2?v=4',
      status: 'away',
      email: 'lisi@example.com'
    }
  },
  {
    id: '3',
    username: 'wangwu',
    password: '123456',
    user: {
      id: '3',
      name: '王五',
      avatar: 'https://avatars.githubusercontent.com/u/3?v=4',
      status: 'offline',
      email: 'wangwu@example.com'
    }
  },
  {
    id: '4',
    username: 'admin',
    password: '123456',
    user: {
      id: '4',
      name: '管理员',
      avatar: 'https://avatars.githubusercontent.com/u/4?v=4',
      status: 'online',
      email: 'admin@example.com'
    }
  }
]

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    currentUser: null,
    isLoggedIn: false,
    isLoading: false,
    error: null
  }),

  getters: {
    userInfo: (state) => state.currentUser || null,
    userName: (state) => state.currentUser?.displayName || '',
    userAvatar: (state) => {
      const user = state.currentUser
      if (!user) return 'https://avatars.githubusercontent.com/u/0?v=4'
      
      // 如果有avatar字段且以avatar_开头，说明是数据库存储
      if (user.avatar && user.avatar.startsWith('avatar_')) {
        // 添加时间戳参数防止缓存
        const timestamp = user.avatarTimestamp || Date.now()
        return `http://localhost:8080/api/user/avatar/${user.id}?t=${timestamp}`
      }
      
      // 如果有avatar字段且以/api/user/avatar/开头，直接返回
      if (user.avatar && user.avatar.startsWith('/api/user/avatar/')) {
        const timestamp = user.avatarTimestamp || Date.now()
        return `http://localhost:8080${user.avatar}?t=${timestamp}`
      }
      
      // 兼容旧的静态资源路径
      if (user.avatar && user.avatar.startsWith('/')) {
        return `http://localhost:8080${user.avatar}`
      }
      
      // 默认头像
      return user.avatar || 'https://avatars.githubusercontent.com/u/0?v=4'
    },
    userId: (state) => state.currentUser?.id || null,
    userStatus: (state) => state.currentUser?.status || 'offline'
  },

  actions: {
    // 真实的登录API调用
    async login(credentials: LoginCredentials): Promise<boolean> {
      this.isLoading = true
      this.error = null

      try {
        console.log('开始登录...', credentials)

        // 调用后端API，使用username字段
        const response = await authApi.login({
          username: credentials.account, // 将account转换为username
          password: credentials.password
        })

        console.log('完整登录响应结构:', {
          status: response.status,
          data: response.data,
          headers: response.headers
        })

        // 直接使用API响应拦截器返回的完整响应
        console.log('完整登录响应:', JSON.stringify(response, null, 2))
        
        const responseData = response.data
        if (!responseData) {
          throw new Error('无效的登录响应: 响应数据为空')
        }

        // 确保从正确的位置获取token
        const token = responseData.data?.token || responseData.token
        if (!token) {
          console.error('Token获取失败，响应结构:', responseData)
          throw new Error('无效的登录响应: 缺少token')
        }
        if (!token) {
          throw new Error(`无效的登录响应: 缺少token字段。响应数据: ${JSON.stringify(response.data)}`)
        }

        const userInfo = responseData.userInfo
        if (!userInfo?.username) {
          throw new Error(`无效的登录响应: 缺少用户信息。响应数据: ${JSON.stringify(response.data)}`)
        }

        // 创建认证用户对象
        const authUser: AuthUser = {
          id: userInfo.id?.toString() || '0',
          name: userInfo.nickname || userInfo.username || '用户',
          displayName: userInfo.nickname || userInfo.username || '用户',
          username: userInfo.username || '',
          email: userInfo.email || '',
          avatar: userInfo.avatar || '',
          avatarTimestamp: Date.now(), // 设置初始头像时间戳
          signature: userInfo.signature || '',
          status: userInfo.status === 1 ? 'online' : 'offline',
          token: token,
          refreshToken: responseData.refreshToken || ''
        }

        console.log('创建的authUser:', authUser)

        this.currentUser = authUser
        this.isLoggedIn = true

        // 保存到本地存储
        localStorage.setItem('authUser', JSON.stringify(authUser))

        ElMessage.success('登录成功')
        return true
      } catch (error: any) {
        console.error('登录失败:', error)

        // 设置错误信息
        if (error.response?.data?.message) {
          this.error = error.response.data.message
        } else if (error.message) {
          this.error = error.message
        } else {
          this.error = '登录失败，请重试'
        }

        return false
      } finally {
        this.isLoading = false
      }
    },

    // 真实的注册 API 调用
    async register(credentials: RegisterCredentials): Promise<boolean> {
      this.isLoading = true
      this.error = null

      try {
        console.log('开始注册...', credentials)

        // 调用后端API，使用nickname字段
        const userInfo = await authApi.register({
          username: credentials.username,
          nickname: credentials.displayName, // 将displayName转换为nickname
          email: credentials.email,
          phone: credentials.phone,
          password: credentials.password,
          confirmPassword: credentials.confirmPassword
        })

        console.log('注册成功:', userInfo)
        ElMessage.success('注册成功，请使用新账号登录')
        return true
      } catch (error: any) {
        console.error('注册失败:', error)

        // 设置错误信息
        if (error.response?.data?.message) {
          this.error = error.response.data.message
        } else if (error.message) {
          this.error = error.message
        } else {
          this.error = '注册失败，请重试'
        }

        return false
      } finally {
        this.isLoading = false
      }
    },

    // 退出登录
    logout() {
      this.currentUser = null
      this.isLoggedIn = false
      this.error = null

      // 清除本地存储
      localStorage.removeItem('authUser')
      ElMessage.success('已退出登录')
    },

    // 从本地存储恢复登录状态
    restoreAuth(): boolean {
      console.group('恢复登录状态')
      try {
        const savedUser = localStorage.getItem('authUser')
        if (!savedUser) {
          console.warn('无保存的用户信息')
          console.groupEnd()
          return false
        }
        
        let authUser: AuthUser
        try {
          authUser = JSON.parse(savedUser)
        } catch (e) {
          console.error('解析用户信息失败:', e)
          localStorage.removeItem('authUser')
          console.groupEnd()
          return false
        }

        console.log('用户信息:', {
          username: authUser.username,
          token: authUser.token ? authUser.token.substring(0, 10) + '...' : null,
          timestamp: authUser.token?.split(':')[2] || '无'
        })

        // 验证token存在性
        if (!authUser.token) {
          console.warn('token不存在')
          this.logout()
          console.groupEnd()
          return false
        }

        // 验证token格式和有效期
        const isValid = this.validateToken(authUser.token)
        console.log('token验证结果:', isValid)
        
        if (!isValid) {
          console.warn('token无效')
          this.logout()
          console.groupEnd()
          return false
        }

        // 恢复状态
        this.currentUser = authUser
        this.isLoggedIn = true
        console.log('恢复成功')
        console.groupEnd()
        return true
        
      } catch (error) {
        console.error('恢复过程中出错:', error)
        this.logout()
        console.groupEnd()
        return false
      }
    },

    validateToken(token: string | null): boolean {
      console.group('验证token')
      try {
        if (!token) {
          console.warn('无token提供')
          console.groupEnd()
          return false
        }
        
        try {
          // 尝试Base64解码token（后端格式）
          const decoded = atob(token)
          const parts = decoded.split(':')
          console.log('Base64解码后token结构:', parts)
          
          if (parts.length !== 3) {
            console.warn('Base64 token格式无效，应有3部分')
            console.groupEnd()
            return false
          }
          
          const [username, uuid, timestamp] = parts
          
          if (!username || !uuid || !timestamp) {
            console.warn('Base64 token部分为空')
            console.groupEnd()
            return false
          }
          
          const timestampNum = Number(timestamp)
          if (isNaN(timestampNum)) {
            console.warn('无效的时间戳:', timestamp)
            console.groupEnd()
            return false
          }
          
          // 检查token是否过期（假设有3600秒有效期）
          const tokenAge = Date.now() - timestampNum
          const maxAge = 3600 * 1000 // 3600秒
          const minutes = Math.floor(tokenAge / (60 * 1000))
          
          console.log('Base64 token信息:', {
            用户名: username,
            UUID: uuid.substring(0, 8) + '...',
            时间戳: new Date(timestampNum).toLocaleString(),
            有效期: `${minutes}分钟/${Math.floor(maxAge/(60 * 1000))}分钟`
          })
          
          if (tokenAge > maxAge) {
            console.warn(`Base64 token已过期 (${minutes}分钟)`)
            console.groupEnd()
            return false
          }
          
          console.log('Base64 token验证通过')
          console.groupEnd()
          return true
          
        } catch (base64Error) {
          // Base64解码失败，尝试原始格式
          console.log('Base64解码失败，尝试原始格式:', base64Error.message)
          
          const parts = token.split(':')
          console.log('原始token结构:', parts)
          
          if (parts.length !== 3) {
            console.warn('原始token格式无效，应有3部分')
            console.groupEnd()
            return false
          }
          
          const [username, uuid, timestamp] = parts
          
          if (!username || !uuid || !timestamp) {
            console.warn('原始token部分为空')
            console.groupEnd()
            return false
          }
          
          const timestampNum = Number(timestamp)
          if (isNaN(timestampNum)) {
            console.warn('无效的时间戳:', timestamp)
            console.groupEnd()
            return false
          }
          
          // 检查token是否过期（假设有效期24小时）
          const tokenAge = Date.now() - timestampNum
          const maxAge = 24 * 60 * 60 * 1000 // 24小时
          const hours = Math.floor(tokenAge / (60 * 60 * 1000))
          
          console.log('原始token信息:', {
            用户名: username,
            UUID: uuid.substring(0, 4) + '...',
            时间戳: new Date(timestampNum).toLocaleString(),
            有效期: `${hours}小时/${Math.floor(maxAge/(60 * 60 * 1000))}小时`
          })
          
          if (tokenAge > maxAge) {
            console.warn(`原始token已过期 (${hours}小时)`)
            console.groupEnd()
            return false
          }
          
          console.log('原始token验证通过')
          console.groupEnd()
          return true
        }
        
      } catch (e) {
        console.error('验证过程中出错', e)
        console.groupEnd()
        return false
      }
    },

    // 更新用户状态
    updateUserStatus(status: 'online' | 'offline' | 'away') {
      if (this.currentUser) {
        this.currentUser.status = status
        // 同步更新到本地存储
        localStorage.setItem('authUser', JSON.stringify(this.currentUser))

        // TODO: 调用后端 API 更新状态
        console.log(`状态已更新为: ${status}`)
      }
    },

    // 更新用户信息
    updateUserInfo(userInfo: Partial<AuthUser>) {
      if (this.currentUser) {
        Object.assign(this.currentUser, userInfo)
        localStorage.setItem('authUser', JSON.stringify(this.currentUser))
      }
    },

    // 清除错误信息
    clearError() {
      this.error = null
    },

    // 验证token有效性
    validateToken(token: string | null): boolean {
      if (!token) {
        console.warn('validateToken: 无token提供')
        return false
      }
      
      try {
        // 验证token格式: username:uuid:timestamp
        const parts = token.split(':')
        if (parts.length !== 3) {
          console.warn('validateToken: token格式无效，应有3部分')
          return false
        }
        
        const [username, uuid, timestamp] = parts
        
        // 验证各部分
        if (!username || !uuid || !timestamp) {
          console.warn('validateToken: token部分为空')
          return false
        }
        
        // 验证timestamp是否为有效数字
        const timestampNum = Number(timestamp)
        if (isNaN(timestampNum)) {
          console.warn('validateToken: 无效的时间戳')
          return false
        }
        
        // 检查token是否过期（假设有效期24小时）
        const tokenAge = Date.now() - timestampNum
        const maxAge = 24 * 60 * 60 * 1000 // 24小时
        
        if (tokenAge > maxAge) {
          console.warn(`validateToken: token已过期 (${Math.floor(tokenAge / (60 * 60 * 1000))}小时)`)
          return false
        }
        
        return true
      } catch (e) {
        console.error('validateToken: 验证过程中出错', e)
        return false
      }
    },

    // 检查登录状态和token有效性
    checkAuth(): boolean {
      const token = this.currentUser?.token
      if (!token || !this.validateToken(token)) {
        console.warn('验证失败，token无效:', token)
        this.logout()
        return false
      }
      return true
    },

    // 获取所有可用账号（用于展示，仅演示用）
    getAvailableAccounts() {
      return mockAccounts.map(account => ({
        id: account.id,
        username: account.username,
        name: account.user.name,
        avatar: account.user.avatar
      }))
    },

    // 获取可切换的账号（排除当前用户）
    getSwitchableAccounts() {
      return mockAccounts
        .filter(account => account.id !== this.currentUser?.id?.toString())
        .map(account => ({
          id: account.id,
          username: account.username,
          name: account.user.name,
          avatar: account.user.avatar
        }))
    }
  }
})