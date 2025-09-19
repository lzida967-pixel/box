<template>
  <div class="friend-chat-test">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>好友聊天功能测试</span>
          <el-tag :type="connectionStatus === 'connected' ? 'success' : 'danger'">
            {{ connectionStatusText }}
          </el-tag>
        </div>
      </template>

      <div class="test-info">
        <p>当前用户: {{ currentUser?.username || '未登录' }}</p>
        <p>WebSocket状态: {{ connectionStatusText }}</p>
      </div>

      <div class="test-controls">
        <el-button @click="connect" :disabled="connectionStatus === 'connected'">
          连接WebSocket
        </el-button>
        <el-button @click="disconnect" :disabled="connectionStatus !== 'connected'">
          断开连接
        </el-button>
        <el-button @click="loadContacts" :disabled="!authStore.isLoggedIn">
          加载联系人
        </el-button>
      </div>

      <div class="contacts-section" v-if="chatStore.contacts.length > 0">
        <h4>联系人列表:</h4>
        <div class="contacts-list">
          <div
            v-for="contact in chatStore.availableContacts"
            :key="contact.id"
            class="contact-item"
            @click="startChat(contact)"
          >
            <el-avatar :src="getUserAvatar(contact)" :size="40" />
            <div class="contact-info">
              <div class="contact-name">{{ contact.nickname || contact.username }}</div>
              <div class="contact-status" :class="getStatusClass(contact.status)">
                {{ getStatusText(contact.status) }}
              </div>
            </div>
            <el-button size="small" @click.stop="startChat(contact)">
              开始聊天
            </el-button>
          </div>
        </div>
      </div>

      <div class="chat-section" v-if="activeConversation">
        <h4>聊天界面 - 与 {{ currentContact?.nickname || currentContact?.username }} 的对话</h4>
        <div class="message-area">
          <div
            v-for="message in chatStore.activeMessages"
            :key="message.id"
            class="message-item"
            :class="{ 'own-message': isOwnMessage(message) }"
          >
            <div class="message-bubble">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-time">{{ formatTime(message.createTime) }}</div>
              <div class="message-status" v-if="isOwnMessage(message)">
                {{ message.status }}
              </div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <el-input
            v-model="testMessage"
            placeholder="输入消息内容..."
            @keydown.enter="sendTestMessage"
          />
          <el-button
            @click="sendTestMessage"
            :disabled="!testMessage.trim() || connectionStatus !== 'connected'"
          >
            发送
          </el-button>
        </div>
      </div>

      <div class="log-section">
        <h4>操作日志:</h4>
        <div class="log-messages">
          <div v-for="(log, index) in logs" :key="index" class="log-item">
            [{{ formatTime(log.timestamp) }}] {{ log.message }}
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { getWebSocketService } from '@/services/websocket'
import type { User, Message } from '@/types'
import dayjs from 'dayjs'

const authStore = useAuthStore()
const chatStore = useChatStore()
const wsService = getWebSocketService()

const connectionStatus = ref<'disconnected' | 'connecting' | 'connected'>('disconnected')
const testMessage = ref('')
const logs = ref<Array<{ timestamp: number; message: string }>>([])
const activeConversation = ref(false)

const currentUser = computed(() => authStore.userInfo)
const currentContact = computed(() => {
  if (!chatStore.activeConversation) return null
  return chatStore.getConversationContact(chatStore.activeConversation)
})

const connectionStatusText = computed(() => {
  switch (connectionStatus.value) {
    case 'connected': return '已连接'
    case 'connecting': return '连接中...'
    case 'disconnected': return '未连接'
    default: return '未知'
  }
})

const addLog = (message: string) => {
  logs.value.push({
    timestamp: Date.now(),
    message
  })
}

const connect = async () => {
  if (!authStore.isLoggedIn) {
    ElMessage.error('请先登录')
    return
  }

  try {
    connectionStatus.value = 'connecting'
    addLog('正在连接WebSocket...')
    
    await wsService.connect()
    connectionStatus.value = 'connected'
    addLog('WebSocket连接成功')
    ElMessage.success('WebSocket连接成功')
  } catch (error) {
    connectionStatus.value = 'disconnected'
    addLog(`连接失败: ${error}`)
    ElMessage.error('WebSocket连接失败')
  }
}

const disconnect = () => {
  wsService.disconnect()
  connectionStatus.value = 'disconnected'
  addLog('WebSocket连接已断开')
  ElMessage.info('WebSocket连接已断开')
}

const loadContacts = async () => {
  try {
    addLog('正在加载联系人...')
    await chatStore.loadContacts()
    addLog(`成功加载 ${chatStore.contacts.length} 个联系人`)
    ElMessage.success('联系人加载成功')
  } catch (error) {
    addLog('加载联系人失败')
    ElMessage.error('加载联系人失败')
  }
}

const startChat = (contact: User) => {
  addLog(`开始与 ${contact.nickname || contact.username} 聊天`)
  
  const conversationId = chatStore.startConversation(contact.id.toString())
  if (conversationId) {
    chatStore.setActiveConversation(conversationId)
    activeConversation.value = true
    addLog(`对话已创建: ${conversationId}`)
  }
}

const sendTestMessage = () => {
  if (!testMessage.value.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }

  if (!chatStore.activeConversationId) {
    ElMessage.warning('请先选择联系人开始聊天')
    return
  }

  try {
    chatStore.sendWebSocketMessage(testMessage.value.trim())
    addLog(`发送消息: ${testMessage.value}`)
    testMessage.value = ''
  } catch (error) {
    addLog('发送消息失败')
    ElMessage.error('发送消息失败')
  }
}

const isOwnMessage = (message: Message) => {
  const currentUserId = authStore.userInfo?.id
  if (!currentUserId) return false
  
  // 使用正确的字段名：fromUserId（兼容senderId）
  const messageSenderId = message.fromUserId || message.senderId
  if (!messageSenderId) return false
  
  // 确保类型匹配：将两个ID都转换为字符串进行比较
  return messageSenderId.toString() === currentUserId.toString()
}

const formatTime = (timestamp: string | Date) => {
  return dayjs(timestamp).format('HH:mm:ss')
}

const getUserAvatar = (user?: User) => {
  if (!user) return 'https://avatars.githubusercontent.com/u/0?v=4'
  return user.avatar || 'https://avatars.githubusercontent.com/u/0?v=4'
}

const getStatusClass = (status?: number) => {
  switch (status) {
    case 1: return 'online'
    case 2: return 'busy'
    case 3: return 'away'
    default: return 'offline'
  }
}

const getStatusText = (status?: number) => {
  switch (status) {
    case 1: return '在线'
    case 2: return '忙碌'
    case 3: return '离开'
    default: return '离线'
  }
}

// 监听WebSocket消息
const setupWebSocketListeners = () => {
  wsService.onMessage((data) => {
    if (data.type === 'private') {
      addLog(`收到私聊消息: ${data.content}`)
      ElMessage.info(`收到来自 ${data.fromUserId} 的消息`)
    }
  })

  wsService.onStatusChange((status) => {
    connectionStatus.value = status === 'open' ? 'connected' : 'disconnected'
  })
}

onMounted(() => {
  setupWebSocketListeners()
  addLog('好友聊天测试组件已加载')
})

onUnmounted(() => {
  // 清理工作
})
</script>

<style scoped>
.friend-chat-test {
  max-width: 800px;
  margin: 20px auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.test-info {
  margin-bottom: 20px;
}

.test-controls {
  margin-bottom: 20px;
}

.test-controls .el-button {
  margin-right: 10px;
}

.contacts-section {
  margin-bottom: 20px;
}

.contacts-list {
  max-height: 300px;
  overflow-y: auto;
}

.contact-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.contact-item:hover {
  background-color: #f5f7fa;
}

.contact-info {
  flex: 1;
  margin-left: 12px;
}

.contact-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.contact-status {
  font-size: 12px;
}

.contact-status.online {
  color: #52c41a;
}

.contact-status.busy {
  color: #faad14;
}

.contact-status.away {
  color: #fa8c16;
}

.contact-status.offline {
  color: #d9d9d9;
}

.chat-section {
  margin-bottom: 20px;
}

.message-area {
  height: 200px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  overflow-y: auto;
  background-color: #fafafa;
}

.message-item {
  margin-bottom: 8px;
}

.message-item.own-message {
  text-align: right;
}

.message-bubble {
  display: inline-block;
  padding: 8px 12px;
  border-radius: 12px;
  background-color: white;
  border: 1px solid #e8e8e8;
  max-width: 70%;
}

.own-message .message-bubble {
  background-color: #1890ff;
  color: white;
}

.message-content {
  margin-bottom: 4px;
  white-space: pre-wrap; /* 保留空格和换行，自动换行 */
  word-break: break-word; /* 长单词或URL强制换行 */
  overflow-wrap: break-word; /* 确保长内容正确换行 */
}

.message-time {
  font-size: 10px;
  color: #999;
}

.message-status {
  font-size: 10px;
  margin-top: 4px;
}

.input-area {
  display: flex;
  gap: 8px;
}

.input-area .el-input {
  flex: 1;
}

.log-section {
  margin-top: 20px;
}

.log-messages {
  height: 150px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  overflow-y: auto;
  background-color: #fafafa;
  font-family: monospace;
  font-size: 12px;
}

.log-item {
  margin-bottom: 4px;
  color: #666;
}
</style>