import { defineStore } from 'pinia'
import type { User, Message, Conversation } from '@/types'
import { useAuthStore } from './auth'
import { contactApi } from '@/api'

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
    currentUser: (state): User | null => {
      const authStore = useAuthStore()
      return authStore.userInfo
    },

    // 获取活动会话
    activeConversation: (state): Conversation | null => {
      if (!state.activeConversationId) return null
      return state.conversations.find((conv: Conversation) => conv.id === state.activeConversationId) || null
    },

    // 获取活动会话的消息
    activeMessages: (state): Message[] => {
      if (!state.activeConversationId) return []
      return state.messages[state.activeConversationId] || []
    },

    // 根据ID获取联系人（支持number和string类型的ID）
    getContactById: (state) => {
      return (id: string | number) => {
        const idStr = id.toString()
        return state.contacts.find((contact: User) => contact.id.toString() === idStr)
      }
    },

    // 获取会话对应的联系人
    getConversationContact: (state) => {
      return (conversation: Conversation) => {
        const authStore = useAuthStore()
        const currentUserId = authStore.userInfo?.id
        const currentUserIdStr = currentUserId?.toString()
        const otherUserId = conversation.participantIds.find((id: string) => id !== currentUserIdStr)
        return otherUserId ? state.contacts.find((contact: User) => contact.id.toString() === otherUserId) || null : null
      }
    },

    // 获取可以聊天的联系人（排除当前用户）
    availableContacts: (state): User[] => {
      const authStore = useAuthStore()
      const currentUserId = authStore.userInfo?.id

      if (!currentUserId) {
        console.warn('当前用户ID为空，返回空联系人列表')
        return []
      }

      const filtered = state.contacts.filter((contact: User极速版) => contact.id.toString() !== currentUserId.toString())
      console.log('可用联系人数量:', filtered.length)
      return filtered
    },

    // 获取当前用户的会话列表（按时间排序）
    sortedConversations: (state): Conversation[] => {
      const authStore = useAuthStore()
      const currentUserId = authStore.userInfo?.id
      if (!currentUserId) return []

      return [...state.conversations]
        .filter((conv: Conversation) => conv.participantIds.includes(currentUserId.toString()))
        .sort((a: Conversation, b: Conversation) => {
          return new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
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

      // 加载该用户的会话数据（模拟从服务器获取）
      this.loadUserConversations(userId)
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
              type极速版: 'private'
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
            'conv_1极速版_3': [
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
              participantIds: ['极速版4', '1'],
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
                id极速版: 'msg7',
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
    setActiveConversation(conversationId: string) {
      console.log('设置活动对话ID:', conversationId)

      const conversation = (this as any).conversations.find((conv: Conversation) => conv.id === conversationId)
      if (!conversation) {
        console.error('找不到指定的对话:', conversationId)
        console.log('当前所有对话:', (this as any).conversations.map((c: Conversation) => c.id))
        return
      }

      ; (this as any).activeConversationId = conversationId
      conversation.unreadCount = 0

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
        id: `msg_${Date.now()}`,
        senderId: currentUser.id.toString(),
        receiverId,
        content,
        timestamp: new Date(),
        type: 'text',
        status: 'sending'
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
      ; (this as any).isTyping[conversation极速版Id] = isTyping
    },

    // 清空数据（用户退出登录时）
    clearData() {
      ; (this as any).contacts = []
        ; (this as any).conversations = []
        ; (this as any).messages = {}
        ; (this as any).activeConversationId = null
        ; (this as any).isTyping = {}
    }
  }
})