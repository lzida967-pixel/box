<template>
  <div class="websocket-test">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>WebSocket 连接测试</span>
          <el-tag :type="connectionStatus === 'connected' ? 'success' : 'danger'">
            {{ connectionStatusText }}
          </el-tag>
        </div>
      </template>
      
      <div class="test-controls">
        <el-button @click="connect" :disabled="connectionStatus === 'connected'">
          连接
        </el-button>
        <el-button @click="disconnect" :disabled="connectionStatus !== 'connected'">
          断开
        </el-button>
        <el-button @click="sendTestMessage" :disabled="connectionStatus !== 'connected'">
          发送测试消息
        </el-button>
      </div>
      
      <div class="message-area">
        <h4>接收到的消息:</h4>
        <div class="messages" ref="messagesRef">
          <div v-for="(msg, index) in messages" :key="index" class="message-item">
            <span class="timestamp">{{ formatTime(msg.timestamp) }}</span>
            <span class="content">{{ msg.content }}</span>
          </div>
        </div>
      </div>
      
      <div class="send-area">
        <el-input
          v-model="testMessage"
          placeholder="输入测试消息..."
          @keydown.enter="sendTestMessage"
        />
        <el-button @click="sendTestMessage" :disabled="connectionStatus !== 'connected'">
          发送
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getWebSocketService } from '@/services/websocket'
import { useAuthStore } from '@/stores/auth'
import dayjs from 'dayjs'

interface TestMessage {
  timestamp: number
  content: string
  type: 'sent' | 'received'
}

const authStore = useAuthStore()
const wsService = getWebSocketService()
const messagesRef = ref<HTMLElement>()

const connectionStatus = ref<'disconnected' | 'connecting' | 'connected'>('disconnected')
const messages = ref<TestMessage[]>([])
const testMessage = ref('')

const connectionStatusText = computed(() => {
  switch (connectionStatus.value) {
    case 'connected': return '已连接'
    case 'connecting': return '连接中...'
    case 'disconnected': return '未连接'
    default: return '未知'
  }
})

const connect = async () => {
  if (!authStore.isLoggedIn) {
    ElMessage.error('请先登录')
    return
  }
  
  try {
    connectionStatus.value = 'connecting'
    await wsService.connect()
    connectionStatus.value = 'connected'
    addMessage('WebSocket连接成功', 'received')
    ElMessage.success('WebSocket连接成功')
  } catch (error) {
    connectionStatus.value = 'disconnected'
    addMessage(`连接失败: ${error}`, 'received')
    ElMessage.error('WebSocket连接失败')
  }
}

const disconnect = () => {
  wsService.disconnect()
  connectionStatus.value = 'disconnected'
  addMessage('WebSocket连接已断开', 'received')
  ElMessage.info('WebSocket连接已断开')
}

const sendTestMessage = () => {
  if (!testMessage.value.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }
  
  try {
    // 发送测试消息
    wsService.sendMessage({
      type: 'chat',
      toUserId: '2', // 发送给用户2作为测试
      content: testMessage.value.trim(),
      messageType: 1
    })
    
    addMessage(`发送: ${testMessage.value}`, 'sent')
    testMessage.value = ''
  } catch (error) {
    ElMessage.error('发送消息失败')
  }
}

const addMessage = (content: string, type: 'sent' | 'received') => {
  messages.value.push({
    timestamp: Date.now(),
    content,
    type
  })
  
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const formatTime = (timestamp: number) => {
  return dayjs(timestamp).format('HH:mm:ss')
}

// 监听WebSocket事件
const setupWebSocketListeners = () => {
  wsService.onMessage((data) => {
    addMessage(`收到: ${JSON.stringify(data)}`, 'received')
  })
  
  wsService.onStatusChange((status) => {
    connectionStatus.value = status === 'open' ? 'connected' : 'disconnected'
    if (status === 'close') {
      addMessage('连接已关闭', 'received')
    } else if (status === 'error') {
      addMessage('连接出错', 'received')
    }
  })
}

onMounted(() => {
  setupWebSocketListeners()
  
  // 如果已经登录，自动连接
  if (authStore.isLoggedIn) {
    connect()
  }
})

onUnmounted(() => {
  // 组件卸载时不断开连接，因为其他组件可能还在使用
})
</script>

<style scoped>
.websocket-test {
  max-width: 800px;
  margin: 20px auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.test-controls {
  margin-bottom: 20px;
}

.test-controls .el-button {
  margin-right: 10px;
}

.message-area {
  margin-bottom: 20px;
}

.messages {
  height: 300px;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
  background-color: #f5f7fa;
}

.message-item {
  margin-bottom: 8px;
  padding: 5px;
  border-radius: 4px;
  background-color: white;
}

.timestamp {
  color: #909399;
  font-size: 12px;
  margin-right: 10px;
}

.content {
  color: #303133;
}

.send-area {
  display: flex;
  gap: 10px;
}

.send-area .el-input {
  flex: 1;
}
</style>