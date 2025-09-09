import { defineStore } from 'pinia'
import type { User, Message, Conversation } from '@/types'
import { useAuthStore } from './auth'
import { contactApi, chatApi } from '@/api'
import { getWebSocketService } from '@/services/websocket'

interface ChatState {
  contacts: User[]
  conversations: Conversation[]
  messages: Record<string, Message[]>
  activeConversationId: string | null
  isTyping: Record<string, boolean>
  loading: boolean
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    contacts: [],
    conversations: [],
    messages: {},
    activeConversationId: null,
    isTyping: {},
    loading: false
  }),

  getters: {
    // 获取当前用户
    currentUser: (state: ChatState): User | null => {
      const authStore = useAuthStore()
      return authStore.userInfo
    },

    // 获取活动会话
    activeConversation: (state: ChatState): Conversation | null => {
      if (!state.activeConversationId) return null
      return state.conversations.find((conv: Conversation) => conv.id === state.activeConversationId) || null
    },

    // 获取活动会话的消息
    activeMessages: (state: ChatState): Message[] => {
      if (!state.activeConversationId) return []
      return state.messages[state.activeConversationId] || []
    },

    // 根据ID获取联系人（支持number和string类型的ID）
    getContactById: (state: ChatState) => {
      return (id: string | number) => {
        const idStr = id.toString()
        return state.contacts.find((contact: User) => contact.id.toString() === idStr)
      }
    },

    // 获取会话对应的联系人
    getConversationContact: (state: ChatState) => {
      return (conversation: Conversation) => {
        const authStore = useAuthStore()
        const currentUserId = authStore.userInfo?.id
        const currentUserIdStr = currentUserId?.toString()
        const otherUserId = conversation.participantIds.find((id: string) => id !== currentUserIdStr)
        return otherUserId ? state.contacts.find((contact: User) => contact.id.toString() === otherUserId) || null : null
      }
    },

    // 获取可以聊天的联系人（排除当前用户）
    availableContacts: (state: ChatState): User[] => {
      const authStore = useAuthStore()
      const currentUserId = authStore.userInfo?.id

      if (!currentUserId) {
        console.warn('当前用户ID为空，返回空联系人列表')
        return []
      }

      const filtered = state.contacts.filter((contact: User) => contact.id.toString() !== currentUserId.toString())
      console.log('可用联系人数量:', filtered.length)
      return filtered
    },

    // 获取当前用户的会话列表（按最后消息时间排序，只显示有消息的会话）
    sortedConversations: (state: ChatState): Conversation[] => {
      const authStore = useAuthStore()
      const currentUserId = authStore.userInfo?.id
      if (!currentUserId) return []

      return [...state.conversations]
        .filter((conv: Conversation) => {
          // 只显示有消息的会话
          const hasMessages = state.messages[conv.id] && state.messages[conv.id].length > 0
          return conv.participantIds.includes(currentUserId.toString()) && hasMessages
        })
        .sort((a: Conversation, b: Conversation) => {
          // 获取最后一条消息的时间，如果没有消息则使用会话时间戳
          const getLastMessageTime = (conv: Conversation) => {
            const messages = state.messages[conv.id]
            if (messages && messages.length > 0) {
              const lastMessage = messages[messages.length - 1]
              return new Date(lastMessage.createTime || lastMessage.sendTime || lastMessage.timestamp).getTime()
            }
            return new Date(conv.timestamp).getTime()
          }
          
          return getLastMessageTime(b) - getLastMessageTime(a)
        })
    }
  },

  actions: {
    // 加载联系人数据
    async loadContacts() {
      ; (this as any).loading = true
      try {
        const response = await contactApi.getContacts()
          // 将后端返回的数字ID转换为字符串ID，以匹配前端数据结构
          ; (this as any).contacts = response.data.map((user: any) => ({
            ...user,
            id: user.id.toString(),
            // 保持status为数字类型，与User接口一致
            status: user.status || 0,
            // 确保avatar字段有正确的URL前缀（如果需要）
            avatar: user.avatar ? (user.avatar.startsWith('http') ? user.avatar : `/api${user.avatar}`) : undefined
          }))
        console.log('加载联系人数据成功:', (this as any).contacts)
        // 调试avatar字段
        if ((this as any).contacts.length > 0) {
          const firstContact = (this as any).contacts[0]
          console.log('第一个联系人的avatar字段:', firstContact.avatar)
          console.log('第一个联系人的所有字段:', firstContact)
        }
      } catch (error) {
        console.error('加载联系人数据失败:', error)
      } finally {
        ; (this as any).loading = false
      }
    },

    // 初始化用户数据
    async initializeUserData(userId: string) {
      // 清空之前的数据
      ; (this as any).conversations = []
        ; (this as any).messages = {}
        ; (this as any).activeConversationId = null

      // 加载联系人数据
      await this.loadContacts()

      // 加载离线消息（添加错误处理，避免阻塞初始化）
      try {
        await this.loadOfflineMessages()
      } catch (error) {
        console.warn('初始化时加载离线消息失败，将在后台重试:', error)
        // 不阻塞初始化流程，让WebSocket服务稍后重试
      }

      // 加载该用户的会话数据（模拟从服务器获取）
      this.loadUserConversations(userId)
      
      // 加载真实的聊天历史记录
      await this.loadRealChatHistory(userId)
    },

    // 加载用户会话数据
    loadUserConversations(userId: string) {
      // 模拟不同用户的会话数据
      const userConversationsData: Record<string, any> = {
        '1': { // 张三
          conversations: [
            {
              id: 'conv_1_2',
              participantIds: ['1', '2'],
              unreadCount: 1,
              timestamp: new Date(),
              type: 'private'
            },
            {
              id: 'conv_1_3',
              participantIds: ['1', '3'],
              unreadCount: 0,
              timestamp: new Date(Date.now() - 3600000),
              type: 'private'
            }
          ],
          messages: {
            'conv_1_2': [
              {
                id: 'msg1',
                senderId: '2',
                receiverId: '1',
                content: '张三，你好！',
                timestamp: new Date(Date.now() - 60000),
                type: 'text',
                status: 'read'
              }
            ],
            'conv_1_3': [
              {
                id: 'msg2',
                senderId: '3',
                receiverId: '1',
                content: '最近工作怎么样？',
                timestamp: new Date(Date.now() - 3600000),
                type: 'text',
                status: 'read'
              }
            ]
          }
        },
        '2': { // 李四
          conversations: [
            {
              id: 'conv_2_1',
              participantIds: ['2', '1'],
              unreadCount: 0,
              timestamp: new Date(Date.now() - 1800000),
              type: 'private'
            },
            {
              id: 'conv_2_4',
              participantIds: ['2', '4'],
              unreadCount: 2,
              timestamp: new Date(),
              type: 'private'
            }
          ],
          messages: {
            'conv_2_1': [
              {
                id: 'msg3',
                senderId: '1',
                receiverId: '2',
                content: '李四，最近好吗？',
                timestamp: new Date(Date.now() - 1800000),
                type: 'text',
                status: 'read'
              }
            ],
            'conv_2_4': [
              {
                id: 'msg4',
                senderId: '4',
                receiverId: '2',
                content: '下周的会议准备好了吗？',
                timestamp: new Date(Date.now() - 300000),
                type: 'text',
                status: 'sent'
              },
              {
                id: 'msg5',
                senderId: '4',
                receiverId: '2',
                content: '需要提前准备一些资料',
                timestamp: new Date(),
                type: 'text',
                status: 'sent'
              }
            ]
          }
        },
        '3': { // 王五
          conversations: [
            {
              id: 'conv_3_1',
              participantIds: ['3', '1'],
              unreadCount: 0,
              timestamp: new Date(Date.now() - 7200000),
              type: 'private'
            }
          ],
          messages: {
            'conv_3_1': [
              {
                id: 'msg6',
                senderId: '1',
                receiverId: '3',
                content: '王五，周末有空吗？',
                timestamp: new Date(Date.now() - 7200000),
                type: 'text',
                status: 'read'
              }
            ]
          }
        },
        '4': { // 管理员
          conversations: [
            {
              id: 'conv_4_1',
              participantIds: ['4', '1'],
              unreadCount: 0,
              timestamp: new Date(Date.now() - 900000),
              type: 'private'
            },
            {
              id: 'conv_4_2',
              participantIds: ['4', '2'],
              unreadCount: 1,
              timestamp: new Date(Date.now() - 600000),
              type: 'private'
            },
            {
              id: 'conv_4_3',
              participantIds: ['4', '3'],
              unreadCount: 0,
              timestamp: new Date(Date.now() - 1200000),
              type: 'private'
            }
          ],
          messages: {
            'conv_4_1': [
              {
                id: 'msg7',
                senderId: '4',
                receiverId: '1',
                content: '张三，请查看新的项目文档',
                timestamp: new Date(Date.now() - 900000),
                type: 'text',
                status: 'delivered'
              }
            ],
            'conv_4_2': [
              {
                id: 'msg8',
                senderId: '4',
                receiverId: '2',
                content: '李四，明天开会记得准时参加',
                timestamp: new Date(Date.now() - 600000),
                type: 'text',
                status: 'sent'
              }
            ],
            'conv_4_3': [
              {
                id: 'msg9',
                senderId: '4',
                receiverId: '3',
                content: '王五，你的报告收到了',
                timestamp: new Date(Date.now() - 1200000),
                type: 'text',
                status: 'read'
              }
            ]
          }
        }
      }

      const userData = userConversationsData[userId]
      if (userData) {
        ; (this as any).conversations = userData.conversations
          ; (this as any).messages = userData.messages
      }
    },

    // 开始与某个联系人的对话
    startConversation(contactId: string): string | null {
      const authStore = useAuthStore()
      const currentUserId = authStore.userInfo?.id

      console.log('开始对话 - 当前用户ID:', currentUserId, '联系人ID:', contactId)
      console.log('当前用户ID类型:', typeof currentUserId)
      console.log('联系人ID类型:', typeof contactId)

      if (!currentUserId) {
        console.error('当前用户ID为空')
        return null
      }

      // 确保所有ID都是字符串类型进行比较
      const currentUserIdStr = currentUserId.toString()
      const contactIdStr = contactId.toString()

      console.log('字符串类型 - 当前用户ID:', currentUserIdStr, '联系人ID:', contactIdStr)

      // 检查是否已存在对话
      const existingConv = (this as any).conversations.find((conv: Conversation) =>
        conv.participantIds.includes(currentUserIdStr) &&
        conv.participantIds.includes(contactIdStr)
      )

      if (existingConv) {
        console.log('找到已存在的对话:', existingConv.id)
        console.log('对话参与者:', existingConv.participantIds)
        return existingConv.id
      }

      // 创建新对话
      const newConversation: Conversation = {
        id: `conv_${currentUserIdStr}_${contactIdStr}_${Date.now()}`,
        participantIds: [currentUserIdStr, contactIdStr],
        unreadCount: 0,
        timestamp: new Date(),
        type: 'private'
      }

      console.log('创建新对话:', newConversation)

        ; (this as any).conversations.unshift(newConversation)
        ; (this as any).messages[newConversation.id] = []

      console.log('对话列表更新后:', (this as any).conversations)
      console.log('新对话ID:', newConversation.id)

      return newConversation.id
    },

    // 设置活动对话
    async setActiveConversation(conversationId: string) {
      console.log('设置活动对话ID:', conversationId)

      const conversation = (this as any).conversations.find((conv: Conversation) => conv.id === conversationId)
      if (!conversation) {
        console.error('找不到指定的对话:', conversationId)
        console.log('当前所有对话:', (this as any).conversations.map((c: Conversation) => c.id))
        return
      }

      ; (this as any).activeConversationId = conversationId
      conversation.unreadCount = 0

      // 加载历史消息
      await this.loadChatHistory(conversationId)

      console.log('活动对话设置成功:', conversationId)
      console.log('对话参与者:', conversation.participantIds)
    },

    // 发送消息
    sendMessage(content: string) {
      const authStore = useAuthStore()
      const currentUser = authStore.userInfo
      if (!(this as any).activeConversationId || !currentUser) return

      const conversation = (this as any).activeConversation
      if (!conversation) return

      const receiverId = conversation.participantIds.find((id: string) => id !== currentUser.id.toString()) || ''

      const message: Message = {
        id: Date.now(),
        senderId: currentUser.id,
        receiverId: parseInt(receiverId),
        content,
        messageType: 'text',
        status: 'sending',
        createTime: new Date().toISOString(),
        updateTime: new Date().toISOString(),
        isRecalled: false
      }

      if (!(this as any).messages[(this as any).activeConversationId]) {
        ; (this as any).messages[(this as any).activeConversationId] = []
      }

      ; (this as any).messages[(this as any).activeConversationId].push(message)

      // 更新会话时间戳
      conversation.timestamp = new Date()
      conversation.lastMessage = message

      // 模拟发送状态变化
      setTimeout(() => {
        message.status = 'sent'
      }, 1000)

      setTimeout(() => {
        message.status = 'delivered'
      }, 2000)
    },

    // 设置正在输入状态
    setTyping(conversationId: string, isTyping: boolean) {
      ; (this as any).isTyping[conversationId] = isTyping
    },

    // 清空数据（用户退出登录时）
    clearData() {
      ; (this as any).contacts = []
        ; (this as any).conversations = []
        ; (this as any).messages = {}
        ; (this as any).activeConversationId = null
        ; (this as any).isTyping = {}
    },

    // ==================== WebSocket 相关方法 ====================

    // 初始化WebSocket连接
    async initWebSocket() {
      try {
        const authStore = useAuthStore()
        if (!authStore.isLoggedIn || !authStore.currentUser?.token) {
          console.warn('用户未登录，跳过WebSocket初始化')
          return
        }

        const wsService = getWebSocketService()
        if (!wsService.isConnected) {
          await wsService.connect()
          console.log('WebSocket连接初始化成功')
        }

        // 注册消息监听器
        wsService.onMessage(this.handleWebSocketMessage.bind(this))
      } catch (error) {
        console.error('WebSocket连接初始化失败:', error)
      }
    },

    // 发送WebSocket消息
    sendWebSocketMessage(content: string) {
      const authStore = useAuthStore()
      const currentUser = authStore.userInfo
      if (!(this as any).activeConversationId || !currentUser) return

      const conversation = (this as any).activeConversation
      if (!conversation) return

      const receiverId = conversation.participantIds.find((id: string) => id !== currentUser.id.toString())
      if (!receiverId) return

      // 通过WebSocket发送消息
      const wsService = getWebSocketService()
      wsService.sendPrivateMessage(parseInt(receiverId), content)

      // 创建本地消息对象（乐观更新）
      const message: Message = {
        id: Date.now(), // 临时ID，服务器会返回真实ID
        senderId: currentUser.id,
        receiverId: parseInt(receiverId),
        content,
        messageType: 'text',
        status: 'sending',
        createTime: new Date().toISOString(),
        updateTime: new Date().toISOString(),
        isRecalled: false
      }

      // 添加到本地消息列表
      if (!(this as any).messages[(this as any).activeConversationId]) {
        ; (this as any).messages[(this as any).activeConversationId] = []
      }
      ; (this as any).messages[(this as any).activeConversationId].push(message)

      // 更新会话时间戳
      conversation.timestamp = new Date()
      conversation.lastMessage = message
    },

    // 添加接收到的消息
    addMessage(message: Message) {
      const authStore = useAuthStore()
      const currentUserId = authStore.userInfo?.id
      if (!currentUserId) return

      // 使用正确的字段名（兼容旧字段名）
      const senderId = message.fromUserId || message.senderId
      const receiverId = message.toUserId || message.receiverId
      
      if (!senderId || !receiverId) {
        console.warn('消息缺少发送者或接收者ID:', message)
        return
      }

      // 确保ID类型一致（后端返回数字，前端使用字符串）
      const senderIdStr = senderId.toString()
      const receiverIdStr = receiverId.toString()
      const currentUserIdStr = currentUserId.toString()

      // 确定会话ID
      let conversationId: string | null = null
      
      // 查找现有会话
      const existingConv = (this as any).conversations.find((conv: Conversation) => {
        return conv.participantIds.includes(senderIdStr) &&
               conv.participantIds.includes(receiverIdStr)
      })

      if (existingConv) {
        conversationId = existingConv.id
      } else {
        // 创建新会话
        const newConversation: Conversation = {
          id: `conv_${senderIdStr}_${receiverIdStr}_${Date.now()}`,
          participantIds: [senderIdStr, receiverIdStr],
          unreadCount: senderIdStr !== currentUserIdStr ? 1 : 0,
          timestamp: new Date(message.createTime),
          type: 'private'
        }
        ; (this as any).conversations.unshift(newConversation)
        conversationId = newConversation.id
      }

      // 添加消息到对应会话
      if (conversationId) {
        if (!(this as any).messages[conversationId]) {
          ; (this as any).messages[conversationId] = []
        }
        ; (this as any).messages[conversationId].push(message)

        // 更新会话信息
        const conversation = (this as any).conversations.find((conv: Conversation) => conv.id === conversationId)
        if (conversation) {
          conversation.lastMessage = message
          conversation.timestamp = new Date(message.createTime)
          
          // 如果不是当前活跃会话且不是自己发送的消息，增加未读数
          if (conversationId !== (this as any).activeConversationId && senderIdStr !== currentUserIdStr) {
            conversation.unreadCount++
          }
        }
      }
    },

    // 标记消息为已读
    markMessageAsRead(messageId: number) {
      Object.values((this as any).messages).forEach((messageList: any) => {
        const messages = messageList as Message[]
        const message = messages.find((msg: Message) => msg.id === messageId)
        if (message) {
          message.status = 'read'
          message.readTime = new Date().toISOString()
        }
      })
    },

    // 更新在线用户列表
    updateOnlineUsers(userIds: number[]) {
      ; (this as any).contacts.forEach((contact: User) => {
        contact.status = userIds.includes(contact.id) ? 1 : 0
      })
    },

    // 发送输入指示器
    sendTypingIndicator(isTyping: boolean) {
      try {
        const authStore = useAuthStore()
        const currentUser = authStore.userInfo
        if (!(this as any).activeConversationId || !currentUser) return

        const conversation = (this as any).activeConversation
        if (!conversation) return

        const receiverId = conversation.participantIds.find((id: string) => id !== currentUser.id.toString())
        if (!receiverId) return

        const wsService = getWebSocketService()
        if (wsService.isConnected) {
          wsService.sendTypingIndicator(parseInt(receiverId), isTyping)
        } else {
          console.warn('WebSocket未连接，无法发送输入指示器')
        }
      } catch (error) {
        console.error('发送输入指示器失败:', error)
      }
    },



    // 处理WebSocket接收到的消息
    handleWebSocketMessage(messageData: any) {
      if (!messageData.type) return

      switch (messageData.type) {
        case 'private':
          this.handleWebSocketPrivateMessage(messageData)
          break
        case 'group':
          // TODO: 处理群聊消息
          console.log('收到群聊消息:', messageData)
          break
        case 'typing':
          // TODO: 处理输入指示器
          console.log('收到输入指示器:', messageData)
          break
        case 'read_receipt':
          // TODO: 处理已读回执
          console.log('收到已读回执:', messageData)
          break
        case 'online_users':
          this.updateOnlineUsers(messageData.userIds)
          break
        case 'heartbeat':
          // 心跳消息，无需处理
          break
        default:
          console.warn('未知的WebSocket消息类型:', messageData.type)
      }
    },

    // 处理WebSocket接收到的私聊消息
    handleWebSocketPrivateMessage(messageData: any) {
      console.log('收到WebSocket私聊消息:', messageData)
      
      // 处理嵌套的消息格式，支持两种格式：
      // 1. 直接格式: { content: "消息内容", fromUserId: 1, toUserId: 2 }
      // 2. 嵌套格式: { message: { content: "消息内容", fromUserId: 1, toUserId: 2 } }
      const message = messageData.message || messageData
      
      const chatMessage: Message = {
        id: message.id || messageData.id || Date.now(),
        fromUserId: message.fromUserId || message.senderId || messageData.fromUserId,
        toUserId: message.toUserId || message.receiverId || messageData.toUserId,
        content: message.content || messageData.content,
        messageType: this.convertMessageType(message.messageType || messageData.messageType || 1),
        status: 'delivered',
        sendTime: message.sendTime || messageData.sendTime || new Date().toISOString(),
        createTime: message.createTime || messageData.createTime || new Date().toISOString(),
        updateTime: message.updateTime || messageData.updateTime || new Date().toISOString(),
        // 兼容字段
        senderId: message.fromUserId || message.senderId || messageData.fromUserId,
        receiverId: message.toUserId || message.receiverId || messageData.toUserId,
        isRecalled: false
      }

      console.log('转换后的消息:', chatMessage)
      
      // 添加到聊天store
      this.addMessage(chatMessage)
    },

    // 转换消息类型辅助方法
    convertMessageType(type: number): 'text' | 'image' | 'file' | 'system' {
      switch (type) {
        case 1: return 'text'
        case 2: return 'image'
        case 3: return 'file'
        case 6: return 'system'
        default: return 'text'
      }
    },

    // 加载聊天历史记录
    async loadChatHistory(conversationId: string) {
      try {
        const authStore = useAuthStore()
        const currentUserId = authStore.userInfo?.id
        if (!currentUserId) return

        // 从会话ID中提取好友ID
        const conversation = (this as any).conversations.find((conv: Conversation) => conv.id === conversationId)
        if (!conversation) return

        const friendId = conversation.participantIds.find((id: string) => id !== currentUserId.toString())
        if (!friendId) return

        console.log('加载聊天历史:', { conversationId, friendId })

        // 调用API获取聊天历史
        const response = await chatApi.getChatHistory(parseInt(friendId))
        
        if (response.code === 200 && response.data?.messages) {
          const messages = response.data.messages.map((msg: any) => ({
            id: msg.id,
            senderId: msg.fromUserId,
            receiverId: msg.toUserId,
            content: msg.content,
            messageType: this.convertMessageType(msg.messageType || 1),
            status: this.convertMessageStatus(msg.status),
            createTime: msg.createTime || msg.sendTime,
            updateTime: msg.updateTime,
            isRecalled: msg.status === 2
          }))

          // 按时间排序（旧消息在前）
          messages.sort((a: Message, b: Message) => 
            new Date(a.createTime).getTime() - new Date(b.createTime).getTime()
          )

          // 替换当前会话的消息列表
          ; (this as any).messages[conversationId] = messages

          console.log('聊天历史加载成功:', messages.length, '条消息')
        }
      } catch (error) {
        console.error('加载聊天历史失败:', error)
      }
    },

    // 转换消息状态
    convertMessageStatus(status: number): 'sending' | 'sent' | 'delivered' | 'read' {
      switch (status) {
        case 0: return 'delivered' // 未读
        case 1: return 'read'      // 已读
        case 2: return 'sent'      // 已撤回，显示为已发送
        default: return 'sent'
      }
    },

    // 加载离线消息
    async loadOfflineMessages() {
      try {
        const response = await chatApi.getOfflineMessages()
        
        if (response.code === 200 && response.data) {
          const offlineMessages = response.data
          console.log('收到离线消息:', offlineMessages.length, '条')

          // 处理每条离线消息
          for (const msg of offlineMessages) {
            const message: Message = {
              id: msg.id,
              senderId: msg.fromUserId,
              receiverId: msg.toUserId,
              content: msg.content,
              messageType: this.convertMessageType(msg.messageType || 1),
              status: 'delivered',
              createTime: msg.createTime || msg.sendTime,
              updateTime: msg.updateTime,
              isRecalled: false
            }

            this.addMessage(message)
          }

          // 标记离线消息为已读
          if (offlineMessages.length > 0) {
            const messageIds = offlineMessages.map((msg: any) => msg.id)
            try {
              await chatApi.markOfflineMessagesAsRead(messageIds)
            } catch (markError) {
              console.warn('标记离线消息为已读失败:', markError)
              // 标记失败不影响消息显示
            }
          }
        }
      } catch (error) {
        console.error('加载离线消息失败:', error)
        // 检查是否是网络错误或服务器错误
        if (error.response?.status === 500) {
          console.warn('服务器内部错误，可能是后端服务配置问题')
        } else if (error.response?.status === 401) {
          console.warn('认证失败，可能需要重新登录')
        } else if (!error.response) {
          console.warn('网络连接错误，请检查网络连接')
        }
        // 抛出错误让上层处理
        throw error
      }
    },

    // 加载真实的聊天历史记录
    async loadRealChatHistory(userId: string) {
      try {
        console.log('开始加载真实聊天历史记录，用户ID:', userId)
        
        // 1. 获取当前用户的所有联系人
        await this.loadContacts()
        
        // 2. 为每个联系人加载聊天历史
        for (const contact of this.contacts) {
          if (contact.id.toString() !== userId) {
            try {
              // 创建或获取会话
              const conversationId = this.startConversation(contact.id.toString())
              if (conversationId) {
                // 加载该会话的历史消息
                await this.loadChatHistory(conversationId)
              }
            } catch (error) {
              console.warn(`加载与联系人 ${contact.id} 的聊天历史失败:`, error)
            }
          }
        }
        
        console.log('真实聊天历史记录加载完成')
      } catch (error) {
        console.error('加载真实聊天历史记录失败:', error)
      }
    }
  }
})