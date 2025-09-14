import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { API_CONFIG } from '@/api/config'
import type { Message } from '@/types'

export interface WebSocketMessage {
  type: string
  [key: string]: any
}

export class WebSocketService {
  private ws: WebSocket | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectInterval = 3000
  private heartbeatInterval: NodeJS.Timeout | null = null
  private isConnecting = false
  private messageQueue: WebSocketMessage[] = []
  private status: 'disconnected' | 'connecting' | 'connected' = 'disconnected'
  private messageListeners: ((data: any) => void)[] = []
  private statusListeners: ((status: string) => void)[] = []

  constructor() {
    // 不在构造函数中自动连接，需要手动调用connect
  }

  /**
   * 建立WebSocket连接
   */
  async connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.isConnecting || (this.ws && this.ws.readyState === WebSocket.OPEN)) {
        resolve()
        return
      }

      this.isConnecting = true
      this.status = 'connecting'
      const authStore = useAuthStore()
      
      if (!authStore.currentUser?.token) {
        const error = 'WebSocket连接失败: 缺少认证token'
        console.error(error)
        this.isConnecting = false
        this.status = 'disconnected'
        reject(new Error(error))
        return
      }

      try {
        // 使用API配置中的基础URL来构建WebSocket URL
        const baseUrl = API_CONFIG.BASE_URL?.replace(/^http/, 'ws') || 'ws://localhost:8080'
        const wsUrl = `${baseUrl}/ws/chat?token=${authStore.currentUser.token}`
        
        console.log('WebSocket连接URL:', wsUrl)
        console.log('API基础URL:', API_CONFIG.BASE_URL)
        this.ws = new WebSocket(wsUrl)

        // 保存resolve和reject以便在事件中使用
        const originalOnOpen = this.onOpen.bind(this)
        const originalOnError = this.onError.bind(this)

        this.ws.onopen = (event) => {
          originalOnOpen(event)
          resolve()
        }

        this.ws.onmessage = this.handleWebSocketMessage.bind(this)
        this.ws.onclose = this.onClose.bind(this)
        
        this.ws.onerror = (event) => {
          originalOnError(event)
          reject(new Error('WebSocket连接错误'))
        }

      } catch (error) {
        console.error('WebSocket连接失败:', error)
        this.isConnecting = false
        this.status = 'disconnected'
        reject(error)
      }
    })
  }

  /**
   * 连接打开事件
   */
  private onOpen(event: Event): void {
    console.log('WebSocket连接已建立')
    this.isConnecting = false
    this.reconnectAttempts = 0
    this.status = 'connected'
    
    // 通知状态监听器
    this.notifyStatusListeners('open')
    
    // 开始心跳
    this.startHeartbeat()
    
    // 发送队列中的消息
    this.flushMessageQueue()
    
    // 连接建立后延迟获取离线消息，避免阻塞连接
    setTimeout(() => {
      this.loadOfflineMessages()
    }, 1000)
  }

  /**
   * 接收消息事件
   */
  private handleWebSocketMessage(event: MessageEvent): void {
    try {
      const message: WebSocketMessage = JSON.parse(event.data)
      this.handleMessage(message)
    } catch (error) {
      console.error('解析WebSocket消息失败:', error)
    }
  }

  /**
   * 连接关闭事件
   */
  private onClose(event: CloseEvent): void {
    console.log('WebSocket连接已关闭:', event.code, event.reason)
    this.status = 'disconnected'
    this.stopHeartbeat()
    
    // 通知状态监听器
    this.notifyStatusListeners('close')
    
    if (!event.wasClean && this.reconnectAttempts < this.maxReconnectAttempts) {
      this.scheduleReconnect()
    }
  }

  /**
   * 连接错误事件
   */
  private onError(event: Event): void {
    console.error('WebSocket连接错误:', event)
    this.isConnecting = false
    this.status = 'disconnected'
    
    // 通知状态监听器
    this.notifyStatusListeners('error')
  }

  /**
   * 处理接收到的消息
   */
  private handleMessage(message: WebSocketMessage): void {
    const chatStore = useChatStore()

    switch (message.type) {
      case 'connection':
        console.log('WebSocket连接确认:', message)
        break

      case 'private':
        this.handlePrivateMessage(message)
        break

      case 'group':
        this.handleGroupMessage(message)
        break

      case 'typing':
        this.handleTypingIndicator(message)
        break

      case 'read_receipt':
        this.handleReadReceipt(message)
        break

      case 'online_users':
        this.handleOnlineUsers(message)
        break

      case 'system':
        this.handleSystemMessage(message)
        break

      case 'heartbeat':
        // 心跳消息，无需处理
        break

      case 'error':
        console.error('WebSocket错误:', message.message)
        break

      default:
        console.warn('未知的WebSocket消息类型:', message.type)
    }
  }

  /**
   * 处理私聊消息
   */
  private handlePrivateMessage(message: any): void {
    const chatStore = useChatStore()
    const authStore = useAuthStore()
    
    // 检查消息格式，支持两种格式：message.content 或 message.message.content
    const messageContent = message.content || message.message?.content
    if (!messageContent) {
      console.warn('收到无效的私聊消息格式:', message)
      return
    }

    // 获取当前用户ID
    const currentUserId = authStore.userInfo?.id
    if (!currentUserId) {
      console.warn('当前用户未登录，忽略私聊消息')
      return
    }

    // 获取消息的接收方ID（支持多种格式）
    const receiverId = message.toUserId || message.receiverId || message.message?.toUserId || message.message?.receiverId
    
    // 只有当消息是发送给当前用户时才处理
    if (receiverId && receiverId.toString() === currentUserId.toString()) {
      console.log('收到发送给当前用户的私聊消息:', message)
      chatStore.handleWebSocketPrivateMessage(message)
    } else {
      console.log('忽略非当前用户的私聊消息，接收方ID:', receiverId, '当前用户ID:', currentUserId)
    }
  }

  /**
   * 处理群聊消息
   */
  private handleGroupMessage(message: any): void {
    const chatStore = useChatStore()
    const authStore = useAuthStore()
    
    console.log('收到群聊消息:', message)
    
    // 获取当前用户ID
    const currentUserId = authStore.userInfo?.id
    if (!currentUserId) {
      console.warn('当前用户未登录，忽略群聊消息')
      return
    }
    
    // 检查消息格式
    const groupId = message.groupId || message.message?.groupId
    if (!groupId) {
      console.warn('收到无效的群聊消息格式:', message)
      return
    }
    
    // 将消息传递给聊天存储进行处理
    chatStore.handleWebSocketGroupMessage(message)
  }

  /**
   * 处理输入指示器
   */
  private handleTypingIndicator(message: any): void {
    const chatStore = useChatStore()
    const conversationId = `conv_${message.fromUserId}`
    chatStore.setTyping(conversationId, message.isTyping)
  }

  /**
   * 处理已读回执
   */
  private handleReadReceipt(message: any): void {
    const chatStore = useChatStore()
    chatStore.markMessageAsRead(message.messageId)
  }

  /**
   * 处理在线用户列表
   */
  private handleOnlineUsers(message: any): void {
    const chatStore = useChatStore()
    chatStore.updateOnlineUsers(message.userIds)
  }

  /**
   * 处理系统消息
   */
  private handleSystemMessage(message: any): void {
    // TODO: 显示系统通知
    console.log('系统消息:', message.content)
  }

  /**
   * 发送消息
   */
  send(message: WebSocketMessage): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    } else {
      // 连接未就绪，加入队列
      this.messageQueue.push(message)
      if (!this.isConnecting && this.status !== 'connecting') {
        this.connect().catch(error => {
          console.error('自动重连失败:', error)
        })
      }
    }
  }

  /**
   * 发送私聊消息
   */
  sendPrivateMessage(toUserId: number, content: string, messageType: number = 1): void {
    this.send({
      type: 'private',
      toUserId,
      content,
      messageType
    })
  }

  /**
   * 发送群聊消息
   */
  sendGroupMessage(groupId: number, content: string, messageType: number = 1): void {
    this.send({
      type: 'group',
      groupId,
      content,
      messageType
    })
  }

  /**
   * 发送输入指示器
   */
  sendTypingIndicator(toUserId: number, isTyping: boolean): void {
    this.send({
      type: 'typing',
      toUserId,
      isTyping
    })
  }

  /**
   * 发送已读回执
   */
  sendReadReceipt(messageId: number): void {
    this.send({
      type: 'read_receipt',
      messageId
    })
  }

  /**
   * 获取在线用户列表
   */
  getOnlineUsers(): void {
    this.send({
      type: 'get_online_users'
    })
  }

  /**
   * 开始心跳
   */
  private startHeartbeat(): void {
    this.heartbeatInterval = setInterval(() => {
      this.send({ type: 'heartbeat' })
    }, 30000) // 30秒心跳
  }

  /**
   * 停止心跳
   */
  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  /**
   * 安排重连
   */
  private scheduleReconnect(): void {
    this.reconnectAttempts++
    console.log(`WebSocket重连尝试 ${this.reconnectAttempts}/${this.maxReconnectAttempts}`)
    
    setTimeout(() => {
      this.connect()
    }, this.reconnectInterval * this.reconnectAttempts)
  }

  /**
   * 发送队列中的消息
   */
  private flushMessageQueue(): void {
    while (this.messageQueue.length > 0) {
      const message = this.messageQueue.shift()
      if (message) {
        this.send(message)
      }
    }
  }

  /**
   * 转换消息类型
   */
  private convertMessageType(type: number): 'text' | 'image' | 'file' | 'system' {
    switch (type) {
      case 1: return 'text'
      case 2: return 'image'
      case 3: return 'file'
      case 6: return 'system'
      default: return 'text'
    }
  }

  /**
   * 获取离线消息
   */
  private async loadOfflineMessages(): Promise<void> {
    try {
      const chatStore = useChatStore()
      await chatStore.loadOfflineMessages()
      console.log('离线消息加载成功')
    } catch (error) {
      console.error('获取离线消息失败:', error)
      // 现在后端已经有了更好的错误处理，不需要前端重试
      console.warn('离线消息加载失败，但不影响正常聊天功能')
    }
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    this.stopHeartbeat()
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  /**
   * 获取连接状态
   */
  get isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN
  }

  /**
   * 手动重新加载离线消息
   */
  async retryLoadOfflineMessages(): Promise<void> {
    console.log('手动重试加载离线消息')
    await this.loadOfflineMessages()
  }

  /**
   * 添加消息监听器
   */
  onMessage(listener: (data: any) => void): void {
    this.messageListeners.push(listener)
  }

  /**
   * 移除消息监听器
   */
  offMessage(listener: (data: any) => void): void {
    const index = this.messageListeners.indexOf(listener)
    if (index > -1) {
      this.messageListeners.splice(index, 1)
    }
  }

  /**
   * 添加状态监听器
   */
  onStatusChange(listener: (status: string) => void): void {
    this.statusListeners.push(listener)
  }

  /**
   * 移除状态监听器
   */
  offStatusChange(listener: (status: string) => void): void {
    const index = this.statusListeners.indexOf(listener)
    if (index > -1) {
      this.statusListeners.splice(index, 1)
    }
  }

  /**
   * 通知消息监听器
   */
  private notifyMessageListeners(data: any): void {
    this.messageListeners.forEach(listener => {
      try {
        listener(data)
      } catch (error) {
        console.error('消息监听器执行错误:', error)
      }
    })
  }

  /**
   * 通知状态监听器
   */
  private notifyStatusListeners(status: string): void {
    this.statusListeners.forEach(listener => {
      try {
        listener(status)
      } catch (error) {
        console.error('状态监听器执行错误:', error)
      }
    })
  }
}

// 单例模式
let webSocketService: WebSocketService | null = null

export function getWebSocketService(): WebSocketService {
  if (!webSocketService) {
    webSocketService = new WebSocketService()
  }
  return webSocketService
}

export function destroyWebSocketService(): void {
  if (webSocketService) {
    webSocketService.disconnect()
    webSocketService = null
  }
}