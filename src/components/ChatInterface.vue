<template>
  <div class="chat-interface">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <div class="contact-info">
        <el-avatar :src="getContactAvatar(currentContact)" :size="40" />
        <div class="contact-details">
          <div class="contact-name">
            {{ getContactDisplayName(currentContact) }}
            <el-icon v-if="chatStore.activeConversationId?.startsWith('group_')" class="group-icon" title="群聊">
              <UserFilled />
            </el-icon>
          </div>
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
              <!-- 群聊中显示发送者名称（仅非自己的消息） -->
              <div v-if="!isOwnMessage(message) && chatStore.activeConversationId?.startsWith('group_')" class="sender-name">
                {{ chatStore.getContactById(message.fromUserId || message.senderId)?.nickname || '未知用户' }}
              </div>
              
              <div class="message-bubble" :class="getBubbleClass(message)">
                <template v-if="isImage(message)">
                  <el-image
                    :src="imageSrc(message)"
                    :preview-src-list="previewSrcList"
                    :initial-index="previewIndex(message)"
                    fit="cover"
                    preview-teleported
                    hide-on-click-modal
                    :z-index="3000"
                    style="max-width: 220px; max-height: 220px; border-radius: 8px; overflow: hidden"
                  >
                    <template #error>
                      <div style="width:200px;height:160px;display:flex;align-items:center;justify-content:center;color:#999;background:#f5f5f5">
                        图片加载失败
                      </div>
                    </template>
                  </el-image>
                </template>
                <div v-else class="message-text">{{ message.content }}</div>
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
        <el-popover
          placement="top-start"
          :width="320"
          trigger="click"
          v-model:visible="showEmojiPicker"
          popper-class="emoji-popover"
        >
          <template #reference>
            <el-button text>
              <span style="font-size:18px;line-height:1">😀</span>
            </el-button>
          </template>
          <div class="emoji-grid">
            <button
              v-for="emo in emojis"
              :key="emo"
              class="emoji-btn"
              @click="insertEmoji(emo)"
              type="button"
            >
              {{ emo }}
            </button>
          </div>
        </el-popover>

        <el-button text @click="selectImage">
          <el-icon><Picture /></el-icon>
        </el-button>
      </div>

      <div class="input-main">
        <el-input
          v-model="inputText"
          ref="inputRef"
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
import { chatApi, contactApi, imageApi } from '@/api'

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
const inputRef = ref<any>(null)
const inputText = ref('')
const showEmojiPicker = ref(false)
const wsService = getWebSocketService()
const typingTimer = ref<NodeJS.Timeout | null>(null)

// 简易常用表情集（可后续替换为更全的表情包/分类）
const emojis = ref<string[]>([
  '😀','😁','😂','🤣','😊','😍','😘','😜','🤗','🤩',
  '👍','🙏','👏','👌','✌️','🤝','👋','💪','🔥','⭐',
  '🎉','🥳','❤️','💖','💯','✔️','❗','❓','😢','😡'
])

// 将表情插入到当前光标位置
const insertEmoji = (emoji: string) => {
  const elTextarea = (inputRef.value?.textarea as HTMLTextAreaElement | undefined)
  if (!elTextarea) {
    inputText.value += emoji
    return
  }
  const start = elTextarea.selectionStart ?? inputText.value.length
  const end = elTextarea.selectionEnd ?? inputText.value.length
  const before = inputText.value.slice(0, start)
  const after = inputText.value.slice(end)
  inputText.value = before + emoji + after
  nextTick(() => {
    const pos = start + emoji.length
    elTextarea.selectionStart = elTextarea.selectionEnd = pos
    elTextarea.focus()
  })
}

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

// 预览用：同会话的全部图片URL列表
const previewSrcList = computed(() => {
  const list = (chatStore.activeMessages || []).filter((m: any) => isImage(m))
    .map((m: any) => imageSrc(m))
    .filter((u: string) => !!u)
  return list
})

// 预览用：获取当前消息在预览列表中的索引
const previewIndex = (message: Message) => {
  const url = imageSrc(message)
  return previewSrcList.value.indexOf(url)
}

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
  // 获取发送者ID，兼容fromUserId和senderId
  const senderId = message.fromUserId || message.senderId
  console.log('获取头像 - 消息senderId:', senderId, '消息内容:', message.content)
  
  // 获取发送者信息
  const sender = chatStore.getContactById(senderId)
  
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

const isImage = (message: Message) => {
  const t: any = message.messageType
  return t === 2 || t === 'image'
}

const imageSrc = (message: Message) => {
  const raw = message.content
  if (!raw || raw === '__uploading__') return ''
  const idNum = Number(raw)
  if (!Number.isFinite(idNum)) {
    console.warn('图片消息content不是数字ID，忽略渲染图片：', raw, message)
    return ''
  }
  const url = imageApi.url(idNum)
  console.debug('生成图片URL', { id: idNum, url, messageType: message.messageType, message })
  return url
}

const formatMessageTime = (timestamp: Date | string) => {
  const now = dayjs()
  const messageTime = dayjs(timestamp)
  
  if (!messageTime.isValid()) {
    console.warn('无效的消息时间:', timestamp)
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
  if (!contact) return '未知联系人'
  
  // 检查是否是群聊会话
  if (chatStore.activeConversationId?.startsWith('group_')) {
    // 群聊显示名称优先级：群名称 > 群ID
    return contact.name || contact.groupName || `群聊 ${contact.id}` || '未知群聊'
  }
  
  // 私聊显示名称优先级：备注 > 昵称 > 用户名
  return contact.remark || contact.nickname || contact.username || '未知联系人'
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
  console.log('开始发送消息...')
  
  if (!canSend.value || !chatStore.activeConversationId) {
    console.log('发送条件不满足，取消发送')
    return
  }

  const content = inputText.value.trim()
  console.log('准备发送消息内容:', content)
  
  // 检查好友关系（仅私聊需要）
  const contact = currentContact.value
  if (contact && !chatStore.activeConversationId.startsWith('group_')) {
    console.log('检查与联系人的好友关系:', contact.id)
    const isFriend = await checkFriendship(contact.id)
    console.log('好友关系检查结果:', isFriend)
    
    if (!isFriend) {
      console.log('不是好友，阻止发送消息')
      ElMessage.warning('你们已经不是好友了，无法发送消息')
      return
    }
  }

  console.log('关系验证通过，开始发送消息')
  
  try {
    // 使用WebSocket发送消息
    chatStore.sendWebSocketMessage(content)
    inputText.value = ''
    
    // 停止输入指示器
    stopTypingIndicator()
    
    scrollToBottom()
    
    console.log('消息发送完成')
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败')
  }
}

// 检查好友关系
const checkFriendship = async (friendId: number): Promise<boolean> => {
  // 如果是群聊会话，直接返回true，不需要检查好友关系
  if (chatStore.activeConversationId?.startsWith('group_')) {
    console.log('当前是群聊会话，跳过好友关系检查')
    return true
  }
  
  try {
    // 首先检查本地联系人列表中是否还有这个好友
    const isInContactList = chatStore.contacts.some(contact => contact.id === friendId)
    if (!isInContactList) {
      return false
    }
    
    // 调用API验证服务器端的好友关系
    const response = await contactApi.checkFriendship(friendId)
    console.log('好友关系检查API响应:', response.data)
    
    // 兼容多种响应格式
    let isFriend = false
    
    if (response.data) {
      // 格式1: {code: 200, message: "...", data: {isFriend: boolean}}
      if (response.data.data && typeof response.data.data.isFriend === 'boolean') {
        isFriend = response.data.data.isFriend
      }
      // 格式2: {isFriend: boolean}
      else if (typeof response.data.isFriend === 'boolean') {
        isFriend = response.data.isFriend
      }
      // 格式3: 直接是boolean值
      else if (typeof response.data === 'boolean') {
        isFriend = response.data
      }
      else {
        console.warn('API响应结构异常，使用本地检查')
        return isInContactList
      }
    }
    
    console.log('解析后的好友关系状态:', isFriend)
    return isFriend
  } catch (error) {
    console.error('检查好友关系失败:', error)
    // 如果API调用失败，回退到本地检查
    return chatStore.contacts.some(contact => contact.id === friendId)
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

const handleImageSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  try {
    if (!chatStore.activeConversationId) return
    // 解析当前会话的对方ID
    const conv = chatStore.activeConversation
    const me = authStore.userInfo?.id
    const friendIdStr = conv?.participantIds.find(id => id !== String(me))
    if (!friendIdStr) return
    const friendId = parseInt(friendIdStr)

    if (files && files.length > 0) {
      for (const file of Array.from(files)) {
        // 本地乐观消息
        const tempId = Date.now() + Math.random()
        const optimistic: Message = {
          id: tempId as any,
          fromUserId: me!,
          toUserId: friendId,
          content: '__uploading__', // 占位，发送成功后替换为图片ID
          messageType: 'image',
          status: 'sending',
          sendTime: new Date().toISOString(),
          createTime: new Date().toISOString(),
          updateTime: new Date().toISOString(),
          senderId: me!,
          receiverId: friendId
        }
        // 推入当前会话消息
        if (!chatStore.messages[chatStore.activeConversationId]) {
          (chatStore as any).messages[chatStore.activeConversationId] = []
        }
        ;(chatStore as any).messages[chatStore.activeConversationId].push(optimistic)

        // 上传+创建消息
        const resp = await chatApi.sendImage(friendId, file)
        if (resp.code === 200 && resp.data) {
          const serverMsg = resp.data as any
          // 后端应返回 message，其中 content=图片ID 或 data.imageId
          const imageId = serverMsg.content ?? serverMsg.imageId ?? serverMsg.data?.imageId
          // 找回本地临时消息并更新
          const list = (chatStore as any).messages[chatStore.activeConversationId] as Message[]
          const found = list.find(m => m.id === tempId)
          if (found) {
            found.id = serverMsg.id ?? found.id
            found.status = 'delivered'
            found.messageType = 'image'
            found.content = String(imageId)
            found.updateTime = new Date().toISOString()
          }
        } else {
          ElMessage.error('图片发送失败')
          // 标记失败
          const list = (chatStore as any).messages[chatStore.activeConversationId] as Message[]
          const found = list.find(m => m.id === tempId)
          if (found) {
            found.status = 'sent'
          }
        }
      }
    }
  } catch (e:any) {
    console.error(e)
    ElMessage.error('图片发送失败')
  } finally {
    target.value = ''
  }
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

.sender-name {
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
  padding-left: 8px;
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

<style scoped>
/* 表情选择器样式（使用 el-popover 容器） */
:global(.emoji-popover) {
  padding: 8px;
}
.emoji-grid {
  display: grid;
  grid-template-columns: repeat(10, 28px);
  gap: 6px;
  max-height: 180px;
  overflow-y: auto;
  padding: 4px;
}
.emoji-btn {
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 18px;
  border-radius: 4px;
}
.emoji-btn:hover {
  background: #f2f3f5;
}
</style>