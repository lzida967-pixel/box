<template>
  <div class="chat-interface">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <div class="contact-info">
        <el-avatar :src="getContactAvatar(currentContact)" :size="40" />
        <div class="contact-details">
          <div class="contact-name">{{ getContactDisplayName(currentContact) }}</div>
          <div class="contact-status">{{ getContactStatusText(currentContact?.status) }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button text @click="showContactInfo">
          <el-icon><InfoFilled /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <div v-if="messages.length === 0" class="empty-state">
        <el-icon size="48" color="#ccc"><ChatDotRound /></el-icon>
        <div class="empty-text">暂无消息</div>
      </div>

      <div v-else class="messages-container">
        <div
          v-for="message in messages"
          :key="message.id"
          class="message-wrapper"
          :class="{ 'own-message': isOwnMessage(message) }"
        >
          <div class="message-item">
            <!-- 对方头像 -->
            <div v-if="!isOwnMessage(message)" class="message-avatar">
              <el-avatar :src="getSenderAvatar(message)" :size="36" />
            </div>

            <!-- 消息内容 -->
            <div class="message-content">
              <div class="message-bubble" :class="getBubbleClass(message)">
                <div class="message-text">{{ message.content }}</div>
                <div v-if="isOwnMessage(message)" class="message-status">
                  <el-icon v-if="message.status === 'sending'" class="status-sending">
                    <Loading />
                  </el-icon>
                  <el-icon v-else-if="message.status === 'sent'" class="status-sent">
                    <Check />
                  </el-icon>
                </div>
              </div>
              <div class="message-time">{{ formatMessageTime(message.sendTime || message.createTime || message.timestamp) }}</div>
            </div>

            <!-- 自己头像 -->
            <div v-if="isOwnMessage(message)" class="message-avatar">
              <el-avatar :src="getOwnAvatar()" :size="36" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 消息输入区域 -->
    <div class="input-area">
      <div class="input-toolbar">
        <el-button text @click="showEmojiPicker = !showEmojiPicker">
          <el-icon><Sunny /></el-icon>
        </el-button>
        <el-button text @click="selectImage">
          <el-icon><Picture /></el-icon>
        </el-button>
      </div>

      <div class="input-main">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="输入消息..."
          @keydown="handleKeyDown"
          @input="handleInputChange"
          @blur="stopTypingIndicator"
          class="message-input"
        />
      </div>

      <div class="send-area">
        <span class="send-tip">按 Enter 发送</span>
        <el-button
          type="primary"
          @click="sendMessage"
          :disabled="!canSend"
          size="small"
        >
          发送
        </el-button>
      </div>
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      ref="imageInputRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleImageSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { getWebSocketService } from '@/services/websocket'
import type { Message, User } from '@/types'
import dayjs from 'dayjs'

const props = defineProps<{
  contact?: User | null
}>()

const emit = defineEmits<{
  (e: 'showContactInfo', contact: User): void
}>()

const authStore = useAuthStore()
const chatStore = useChatStore()

const messageListRef = ref<HTMLElement>()
const imageInputRef = ref<HTMLInputElement>()
const inputText = ref('')
const showEmojiPicker = ref(false)
const wsService = getWebSocketService()
const typingTimer = ref<NodeJS.Timeout | null>(null)

// 计算属性
const currentContact = computed(() => {
  // 优先使用props传入的联系人信息
  if (props.contact) {
    return props.contact
  }
  // 如果没有props传入，则使用store中的联系人信息
  if (!chatStore.activeConversationId) return null
  const conversation = chatStore.activeConversation
  return conversation ? chatStore.getConversationContact(conversation) : null
})

const messages = computed(() => {
  return chatStore.activeMessages || []
})

const canSend = computed(() => {
  return inputText.value.trim().length > 0
})

// 方法
const isOwnMessage = (message: Message) => {
  const currentUserId = authStore.userInfo?.id
  if (!currentUserId) return false
  
  // 使用正确的字段名：fromUserId（兼容senderId）
  const messageSenderId = message.fromUserId || message.senderId
  if (!messageSenderId) return false
  
  // 确保类型匹配：将两个ID都转换为字符串进行比较
  return messageSenderId.toString() === currentUserId.toString()
}

const getSenderAvatar = (message: Message) => {
  console.log('获取头像 - 消息senderId:', message.senderId, '消息内容:', message.content)
  const sender = chatStore.getContactById(message.senderId)
  
  // 如果sender存在且有avatar字段，检查是否是标识符格式
  let avatarUrl = 'https://avatars.githubusercontent.com/u/0?v=4' // 默认头像
  
  if (sender?.avatar) {
    // 如果avatar是标识符格式（如avatar_15），则构建完整URL
    if (sender.avatar.startsWith('avatar_')) {
      avatarUrl = `http://localhost:8080/api/user/avatar/${sender.id}`
    } else if (sender.avatar.startsWith('/api/user/avatar/')) {
      // 如果已经是相对路径，转换为完整URL
      avatarUrl = `http://localhost:8080${sender.avatar}`
    } else {
      // 否则直接使用avatar字段
      avatarUrl = sender.avatar
    }
  }
  
  console.log('头像URL:', avatarUrl, '发送者信息:', sender ? {
    id: sender.id,
    name: sender.nickname || sender.username,
    hasAvatar: !!sender.avatar
  } : '发送者不存在')
  return avatarUrl
}

const getContactAvatar = (contact: User | null) => {
  if (!contact?.avatar) return 'https://avatars.githubusercontent.com/u/0?v=4'
  
  // 如果avatar是标识符格式（如avatar_15），则构建完整URL
  if (contact.avatar.startsWith('avatar_')) {
    return `http://localhost:8080/api/user/avatar/${contact.id}`
  } else if (contact.avatar.startsWith('/api/user/avatar/')) {
    // 如果已经是相对路径，转换为完整URL
    return `http://localhost:8080${contact.avatar}`
  }
  
  return contact.avatar
}

const getOwnAvatar = () => {
  // 直接使用authStore中的userAvatar getter，它已经处理了完整的URL生成
  return authStore.userAvatar
}

const getBubbleClass = (message: Message) => {
  return isOwnMessage(message) ? 'own-bubble' : 'other-bubble'
}

const formatMessageTime = (timestamp: Date | string) => {
  const now = dayjs()
  const messageTime = dayjs(timestamp)
  
  if (!messageTime.isValid()) {
    return '未知时间'
  }
  
  if (now.isSame(messageTime, 'day')) {
    return messageTime.format('HH:mm')
  } else if (now.subtract(1, 'day').isSame(messageTime, 'day')) {
    return '昨天 ' + messageTime.format('HH:mm')
  } else if (now.isSame(messageTime, 'year')) {
    return messageTime.format('MM/DD HH:mm')
  } else {
    return messageTime.format('YYYY/MM/DD HH:mm')
  }
}

const getContactDisplayName = (contact: User | null) => {
  if (!contact) return '选择联系人'
  return contact.nickname || contact.username || contact.name || '未知联系人'
}

const getContactStatusText = (status?: number) => {
  // 后端状态: 1-在线, 2-忙碌, 3-离开, 0-离线
  switch (status) {
    case 1: return '在线'
    case 2: return '忙碌'
    case 3: return '离开'
    case 0:
    default: return '离线'
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

const handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

const sendMessage = async () => {
  if (!canSend.value || !chatStore.activeConversationId) return

  const content = inputText.value.trim()
  try {
    // 使用WebSocket发送消息
    chatStore.sendWebSocketMessage(content)
    inputText.value = ''
    
    // 停止输入指示器
    stopTypingIndicator()
    
    scrollToBottom()
  } catch (error) {
    ElMessage.error('发送消息失败')
  }
}

// 处理输入变化，发送输入指示器
const handleInputChange = () => {
  if (!chatStore.activeConversationId) return
  
  // 发送正在输入指示器
  chatStore.sendTypingIndicator(true)
  
  // 清除之前的定时器
  if (typingTimer.value) {
    clearTimeout(typingTimer.value)
  }
  
  // 设置新的定时器，1秒后停止输入指示器
  typingTimer.value = setTimeout(() => {
    chatStore.sendTypingIndicator(false)
  }, 1000)
}

// 停止输入指示器
const stopTypingIndicator = () => {
  if (typingTimer.value) {
    clearTimeout(typingTimer.value)
    typingTimer.value = null
  }
  if (chatStore.activeConversationId) {
    chatStore.sendTypingIndicator(false)
  }
}

const selectImage = () => {
  imageInputRef.value?.click()
}

const handleImageSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    ElMessage.info('图片发送功能开发中')
  }
  target.value = ''
}

const showContactInfo = () => {
  const contact = currentContact.value
  if (contact) {
    emit('showContactInfo', contact)
  } else {
    ElMessage.info('请先选择联系人')
  }
}

// 监听消息变化
watch(messages, () => {
  scrollToBottom()
}, { deep: true })

// 初始化
onMounted(() => {
  scrollToBottom()
  // 确保WebSocket连接已建立
  chatStore.initWebSocket().catch((error: any) => {
    console.error('WebSocket连接失败:', error)
    ElMessage.error('聊天服务连接失败，请刷新页面重试')
  })
})

// 清理
onUnmounted(() => {
  stopTypingIndicator()
})
</script>

<style scoped>
.chat-interface {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f8f9fa;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
}

.contact-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.contact-details {
  display: flex;
  flex-direction: column;
}

.contact-name {
  font-weight: 500;
  font-size: 16px;
  color: #333;
}

.contact-status {
  font-size: 12px;
  color: #999;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f8f9fa;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #ccc;
}

.empty-text {
  margin-top: 8px;
  font-size: 14px;
}

.messages-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-wrapper {
  display: flex;
}

.message-wrapper.own-message {
  justify-content: flex-end;
}

.message-item {
  display: flex;
  gap: 8px;
  max-width: 70%;
}

.own-message .message-item {
  flex-direction: row;
  justify-content: flex-end;
}

.message-avatar {
  flex-shrink: 0;
  align-self: flex-end;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.own-message .message-content {
  align-items: flex-end;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 18px;
  max-width: 100%;
  word-wrap: break-word;
  position: relative;
}

.other-bubble {
  background: white;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.own-bubble {
  background: #1890ff;
  color: white;
  border-bottom-right-radius: 4px;
}

.message-text {
  line-height: 1.4;
  font-size: 14px;
}

.message-status {
  position: absolute;
  right: -20px;
  bottom: 0;
  font-size: 12px;
}

.status-sending {
  color: #999;
  animation: spin 1s linear infinite;
}

.status-sent {
  color: #999;
}

.message-time {
  font-size: 12px;
  color: #999;
  padding: 0 4px;
}

.input-area {
  background: white;
  border-top: 1px solid #e8e8e8;
  padding: 12px 16px;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.input-main {
  margin-bottom: 12px;
}

.message-input :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  resize: none;
  font-size: 14px;
  line-height: 1.5;
}

.message-input :deep(.el-textarea__inner):focus {
  border: none;
  box-shadow: none;
}

.send-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.send-tip {
  font-size: 12px;
  color: #999;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 滚动条样式 */
.message-list::-webkit-scrollbar {
  width: 6px;
}

.message-list::-webkit-scrollbar-track {
  background: transparent;
}

.message-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

.message-list::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>