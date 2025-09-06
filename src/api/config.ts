import axios, { InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

// 确保axios模块正确导入
if (!axios) {
  throw new Error('axios模块导入失败，请检查依赖安装')
}

// API 基础配置
export const API_CONFIG = {
  // 后端服务器地址
  BASE_URL: 'http://localhost:8080/api',
  // 请求超时时间（毫秒）
  TIMEOUT: 10000,
  // API版本
  VERSION: 'v1'
}

// 创建 axios 实例
export const apiClient = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
})

// 刷新token的标志，防止重复刷新
let isRefreshing = false
// 存储等待token刷新的请求
let failedQueue: Array<{ resolve: (value?: any) => void, reject: (reason?: any) => void, config: any }> = []

// 处理等待队列中的请求
const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error)
    } else {
      // 更新请求的token
      if (prom.config.headers) {
        prom.config.headers.Authorization = `Bearer ${token}`
      }
      prom.resolve(apiClient(prom.config))
    }
  })
  failedQueue = []
}

// 请求拦截器 - 添加认证token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig & { metadata?: any }) => {
    // 添加请求时间戳
    config.metadata = { startTime: new Date() }

    // 如果是FormData，删除默认的Content-Type让浏览器自动设置
    if (config.data instanceof FormData) {
      console.log('检测到FormData，移除默认Content-Type')
      delete config.headers['Content-Type']
    }

    // 添加认证令牌
    try {
      const authStore = useAuthStore()
      const token = authStore.currentUser?.token || ''

      if (token) {
        config.headers.Authorization = `Bearer ${token}`
        console.log('已添加认证token:', token.substring(0, 10) + '...')
        console.log('完整token:', token)
        console.log('请求URL:', config.url)
        console.log('请求方法:', config.method)
        if (config.data instanceof FormData) {
          console.log('FormData内容:')
          for (let [key, value] of config.data.entries()) {
            console.log(`${key}:`, value)
          }
        }
      } else {
        console.warn('未找到有效token，请求将以匿名方式发送')
        if (config.url?.includes('/user/profile')) {
          console.error('关键API缺少token:', config.url)
        }
      }
    } catch (error) {
      console.error('处理认证头时出错:', error)
    }

    return config
  },
  (error) => {
    console.error('[API请求错误]', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    const endTime = new Date()
    const startTime = (response.config as any).metadata?.startTime
    const duration = startTime ? endTime.getTime() - startTime.getTime() : 0

    console.log(`[API响应] ${response.config.method?.toUpperCase()} ${response.config.url} - ${duration}ms`)

    // 检查响应数据格式
    if (response.data && typeof response.data === 'object') {
      // 后端统一响应格式: { code, message, data }
      if (response.data.code !== undefined) {
        if (response.data.code === 200) {
          // 成功响应，返回完整响应对象
          return response.data
        } else {
          // 业务错误，不要在这里显示消息，让调用方处理
          const errorMessage = response.data.message || '请求失败'
          const error = new Error(errorMessage)
            ; (error as any).response = { data: response.data }
          return Promise.reject(error)
        }
      }
    }

    // 直接返回响应数据
    return response.data
  },
  async (error) => {
    console.error('[API响应错误]', error)

    const originalRequest = error.config

    // 如果是401错误（token过期）且不是刷新token的请求，尝试刷新token
    if (error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/refresh')) {

      if (isRefreshing) {
        // 如果正在刷新token，将请求加入等待队列
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject, config: originalRequest })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // 获取refreshToken
        const authUser = localStorage.getItem('authUser')
        if (!authUser) {
          throw new Error('未找到用户信息')
        }

        const user = JSON.parse(authUser)
        if (!user.refreshToken) {
          throw new Error('未找到refreshToken')
        }

        // 调用刷新token接口
        const refreshResponse = await axios.post(
          `${API_CONFIG.BASE_URL}/auth/refresh`,
          null,
          {
            params: { refreshToken: user.refreshToken },
            headers: {
              'Content-Type': 'application/json'
            }
          }
        )

        const newToken = refreshResponse.data.data?.token || refreshResponse.data.token
        const newRefreshToken = refreshResponse.data.data?.refreshToken || refreshResponse.data.refreshToken

        if (newToken && newRefreshToken) {
          // 更新本地存储的token
          const updatedUser = { ...user, token: newToken, refreshToken: newRefreshToken }
          localStorage.setItem('authUser', JSON.stringify(updatedUser))

          // 更新请求头中的token
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
          }

          // 处理等待队列中的请求
          processQueue(null, newToken)

          // 重新发起原始请求
          return apiClient(originalRequest)
        } else {
          throw new Error('刷新token失败：未获取到新token')
        }
      } catch (refreshError) {
        console.error('刷新token失败:', refreshError)

        // 刷新token失败，清除用户信息但不直接跳转
        localStorage.removeItem('authUser')
        processQueue(refreshError)

        ElMessage.error('登录已过期，请重新登录')
        // 只在不是设置页面相关的API调用时才跳转
        // 让组件自己决定是否跳转到登录页
        if (!originalRequest.url?.includes('/user/profile')) {
          window.location.href = '/login'
        }

        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // 处理其他类型的错误
    let errorMessage = '网络错误，请稍后重试'

    if (error.response) {
      // 服务器响应错误
      const status = error.response.status
      const data = error.response.data

      switch (status) {
        case 400:
          errorMessage = data?.message || '请求参数错误'
          break
        case 401:
          errorMessage = '未授权访问'
          // 不在这里直接清除token，让组件自己决定如何处理
          break
        case 403:
          errorMessage = `权限不足，无法访问 ${error.config?.url}`
          console.error('403错误详情:', {
            url: error.config?.url,
            method: error.config?.method,
            headers: error.config?.headers,
            data: error.response?.data
          })
          break
        case 404:
          errorMessage = '请求的资源不存在'
          break
        case 500:
          // 500错误提供更详细的信息
          if (data?.message) {
            errorMessage = `服务器错误：${data.message}`
          } else if (data?.code && data?.code !== 500) {
            // 业务错误码，显示具体错误信息
            errorMessage = data.message || '业务处理失败'
          } else {
            errorMessage = '服务器内部错误，请检查：\n1. 后端服务是否正常运行？\n2. 数据库连接是否正常？\n3. 请联系管理员查看服务器日志'
          }
          console.error('500错误详情：', {
            status,
            data,
            url: error.config?.url,
            method: error.config?.method
          })
          break
        default:
          errorMessage = data?.message || `请求失败 (${status})`
      }
    } else if (error.request) {
      // 网络错误 - 无法连接到服务器
      errorMessage = '无法连接到服务器，请检查：\n1. 后端服务是否启动？\n2. 网络连接是否正常？\n3. 服务器地址是否正确？'
      console.error('网络连接错误：', {
        baseURL: error.config?.baseURL,
        url: error.config?.url,
        timeout: error.config?.timeout
      })
    } else {
      // 其他错误
      errorMessage = error.message || '请求失败'
    }

    ElMessage.error(errorMessage)
    return Promise.reject(error)
  }
)

// 导出常用的请求方法
export const api = {
  get: <T = any>(url: string, config?: any) =>
    apiClient.get<T>(url, config),

  post: <T = any>(url: string, data?: any, config?: any) =>
    apiClient.post<T>(url, data, config),

  put: <T = any>(url: string, data?: any) =>
    apiClient.put<T>(url, data),

  delete: <T = any>(url: string, config?: any) =>
    apiClient.delete<T>(url, config),

  patch: <T = any>(url: string, data?: any) =>
    apiClient.patch<T>(url, data)
}

export default apiClient