import { defineStore } from 'pinia'
import type { User, Message, Conversation } from '@/types'
import { useAuthStore } from './auth'

interface ChatState {
  contacts: User[]
  conversations: Conversation[]
  messages: Record<string, Message[]>
  activeConversationId: string | null
  isTyping: Record<string, boolean>
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    contacts: [
      {
        id: '1',
        name: '张三',
        avatar: 'https://avatars.githubusercontent.com/u/1?v=4',
        status: 'online'
      },
      {
        id: '2',
        name: '李四',
        avatar: 'https://avatars.githubusercontent.com/u/2?v=4',
        status: 'away'
      },
      {
        id: '3',
        name: '王五',
        avatar: 'https://avatars.githubusercontent.com/u/3?v=4',
        status: 'offline'
      },
      {
        id: '4',
        name: '管理员',
        avatar: 'https://avatars.githubusercontent.com/u/4?v=4',
        status: 'online'
      }
    ],
    conversations: [],
    messages: {},
    activeConversationId: null,
    isTyping: {}
  }),

  getters: {
    // 获取当前用户
    currentUser(): User | null {
      const authStore = useAuthStore()
      return authStore.userInfo
    },

    // 获取活动会话
    activeConversation(): Conversation | null {
      if (!this.activeConversationId) return null
      return this.conversations.find(conv => conv.id === this.activeConversationId) || null
    },
    
    // 获取活动会话的消息
    activeMessages(): Message[] {
      if (!this.activeConversationId) return []
      return this.messages[this.activeConversationId] || []
    },

    // 根据ID获取联系人
    getContactById(): (id: string) => User | undefined {
      return (id: string) => {
        return this.contacts.find(contact => contact.id === id)
      }
    },

    // 获取会话对应的联系人
    getConversationContact(): (conversation: Conversation) => User | null {
      return (conversation: Conversation) => {
        const authStore = useAuthStore()
        const currentUserId = authStore.userInfo?.id
        const otherUserId = conversation.participantIds.find(id => id !== currentUserId)
        return otherUserId ? this.contacts.find(contact => contact.id === otherUserId) || null : null
      }
    },

    // 获取当前用户的会话列表（按时间排序）
    sortedConversations(): Conversation[] {
      const authStore = useAuthStore()
      const currentUserId = authStore.userInfo?.id
      if (!currentUserId) return []

      return [...this.conversations]
        .filter(conv => conv.participantIds.includes(currentUserId))
        .sort((a, b) => {
          return new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
        })
    },

    // 获取可以聊天的联系人（排除当前用户）
    availableContacts(): User[] {
      const authStore = useAuthStore()
      const currentUserId = authStore.userInfo?.id
      
      if (!currentUserId) {
        console.warn('当前用户ID为空，返回空联系人列表')
        return []
      }
      
      const filtered = this.contacts.filter(contact => contact.id !== currentUserId)
      console.log('可用联系人数量:', filtered.length)
      return filtered
    }
  },

  actions: {
    // 初始化用户数据
    initializeUserData(userId: string) {
      // 清空之前的数据
      this.conversations = []
      this.messages = {}
      this.activeConversationId = null

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
        this.conversations = userData.conversations
        this.messages = userData.messages
      }
    },

    // 开始与某个联系人的对话
    startConversation(contactId: string): string | null {
      const authStore = useAuthStore()
      const currentUserId = authStore.userInfo?.id
      
      console.log('开始对话 - 当前用户ID:', currentUserId, '联系人ID:', contactId)
      
      if (!currentUserId) {
        console.error('当前用户ID为空')
        return null
      }

      // 检查是否已存在对话
      const existingConv = this.conversations.find(conv => 
        conv.participantIds.includes(currentUserId) && 
        conv.participantIds.includes(contactId)
      )

      if (existingConv) {
        console.log('找到已存在的对话:', existingConv.id)
        return existingConv.id
      }

      // 创建新对话
      const newConversation: Conversation = {
        id: `conv_${currentUserId}_${contactId}_${Date.now()}`,
        participantIds: [currentUserId, contactId],
        unreadCount: 0,
        timestamp: new Date(),
        type: 'private'
      }
      
      console.log('创建新对话:', newConversation)

      this.conversations.unshift(newConversation)
      this.messages[newConversation.id] = []
      
      console.log('对话列表更新后:', this.conversations.length)
      
      return newConversation.id
    },

    // 设置活动对话
    setActiveConversation(conversationId: string) {
      console.log('设置活动对话ID:', conversationId)
      
      const conversation = this.conversations.find(conv => conv.id === conversationId)
      if (!conversation) {
        console.error('找不到指定的对话:', conversationId)
        console.log('当前所有对话:', this.conversations.map(c => c.id))
        return
      }
      
      this.activeConversationId = conversationId
      conversation.unreadCount = 0
      
      console.log('活动对话设置成功:', conversationId)
      console.log('对话参与者:', conversation.participantIds)
    },

    // 发送消息
    sendMessage(content: string) {
      const authStore = useAuthStore()
      const currentUser = authStore.userInfo
      if (!this.activeConversationId || !currentUser) return

      const conversation = this.activeConversation
      if (!conversation) return

      const receiverId = conversation.participantIds.find(id => id !== currentUser.id) || ''
      
      const message: Message = {
        id: `msg_${Date.now()}`,
        senderId: currentUser.id,
        receiverId,
        content,
        timestamp: new Date(),
        type: 'text',
        status: 'sending'
      }

      if (!this.messages[this.activeConversationId]) {
        this.messages[this.activeConversationId] = []
      }
      
      this.messages[this.activeConversationId].push(message)

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
      this.isTyping[conversationId] = isTyping
    },

    // 清空数据（用户退出登录时）
    clearData() {
      this.conversations = []
      this.messages = {}
      this.activeConversationId = null
      this.isTyping = {}
    }
  }
})