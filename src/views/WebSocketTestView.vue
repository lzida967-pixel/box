<template>
  <div class="websocket-test-view">
    <div class="container">
      <h1>WebSocket 聊天功能测试</h1>
      
      <!-- 连接状态 -->
      <el-card class="status-card">
        <template #header>
          <div class="card-header">
            <span>连接状态</span>
            <el-tag :type="statusTagType">{{ statusText }}</el-tag>
          </div>
        </template>
        
        <div class="status-info">
          <p><strong>用户:</strong> {{ currentUser?.name || '未登录' }}</p>
          <p><strong>WebSocket URL:</strong> ws://localhost:8080/api/ws/chat</p>
          <p><strong>连接状态:</strong> {{ connectionStatus }}</p>
          <p><strong>最后心跳:</strong> {{ lastHeartbeat || '无' }}</p>
        </div>
        
        <div class="control-buttons">
          <el-button 
            @click="connectWebSocket" 
            :disabled="connectionStatus === 'connected'"
            type="primary"
          >
            连接
          </el-button>
          <el-button 
            @click="disconnectWebSocket" 
            :disabled="connectionStatus !== 'connected'"
            type="danger"
          >
            断开
          </el-button>
          <el-button @click="clearLogs">清空日志</el-button>
        </div>
      </el-card>

      <!-- 消息测试 -->
      <el-card class="message-card">
        <template #header>
          <span>消息测试</span>
        </template>
        
        <div class="message-controls">
          <el-select v-model="selectedReceiver" placeholder="选择接收者">
            <el-option
              v-for="user in testUsers"
              :key="user.id"
              :label="user.name"
              :value="user.id"
            />
          </el-select>
          
          <el-input
            v-model="testMessage"
            placeholder="输入测试消息..."
            @keydown.enter="sendTestMessage"
            style="margin: 10px 0;"
          />
          
          <el-button 
            @click="sendTestMessage"
            :disabled="!canSendMessage"
            type="primary"
          >
            发送消息
          </el-button>
          
          <el-button 
            @click="sendTypingTest"
            :disabled="!canSendMessage"
          >
            发送输入指示器
          </el-button>
        </div>
      </el-card>

      <!-- 日志显示 -->
      <el-card class="log-card">
        <template #header>
          <span>实时日志</span>
        </template>
        
        <div class="log-container" ref="logContainer">
          <div 
            v-for="(log, index) in logs" 
            :key="index"
            :class="['log-item', `log-${log.type}`]"
          >
            <span class="log-time">{{ formatTime(log.timestamp) }}</span>
            <span class="log-type">[{{ log.type.toUpperCase() }}]</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getWebSocketService } from '@/services/websocket'
import dayjs from 'dayjs'

interface LogEntry {
  timestamp: number
  type: 'info' | 'success' | 'warning' | 'error'
  message: string
}

const authStore = useAuthStore()
const wsService = getWebSocketService()
const logContainer = ref<HTMLElement>()

// 状态数据
const connectionStatus = ref<'disconnected' | 'connecting' | 'connected'>('disconnected')
const lastHeartbeat = ref<string>('')
const logs = ref<LogEntry[]>([])

// 测试数据
const selectedReceiver = ref<string>('')
const testMessage = ref('')
const testUsers = [
  { id: '1', name: '张三 (zhangsan)' },
  { id: '2', name: '李四 (lisi)' },
  { id: '3', name: '王五 (wangwu)' },
  { id: '4', name: '管理员 (admin)' }
]

// 计算属性
const currentUser = computed(() => authStore.currentUser)

const statusTagType = computed(() => {
  switch (connectionStatus.value) {
    case 'connected': return 'success'
    case 'connecting': return 'warning'
    case 'disconnected': return 'danger'
    default: return 'info'
  }
})

const statusText = computed(() => {
  switch (connectionStatus.value) {
    case 'connected': return '已连接'
    case 'connecting': return '连接中...'
    case 'disconnected': return '未连接'
    default: return '未知'
  }
})

const canSendMessage = computed(() => {
  return connectionStatus.value === 'connected' && 
         selectedReceiver.value && 
         testMessage.value.trim()
})

// 方法
const addLog = (type: LogEntry['type'], message: string) => {
  logs.value.push({
    timestamp: Date.now(),
    type,
    message
  })
  
  // 自动滚动到底部
  nextTick(() => {
    if (logContainer.value) {
      logContainer.value.scrollTop = logContainer.value.scrollHeight
    }
  })
  
  // 限制日志数量
  if (logs.value.length > 100) {
    logs.value.splice(0, logs.value.length - 100)
  }
}

const connectWebSocket = async () => {
  if (!authStore.isLoggedIn) {
    ElMessage.error('请先登录')
    return
  }
  
  try {
    connectionStatus.value = 'connecting'
    addLog('info', '正在连接WebSocket...')
    
    await wsService.connect()
    connectionStatus.value = 'connected'
    addLog('success', 'WebSocket连接成功')
    ElMessage.success('WebSocket连接成功')
    
  } catch (error: any) {
    connectionStatus.value = 'disconnected'
    addLog('error', `WebSocket连接失败: ${error.message}`)
    ElMessage.error('WebSocket连接失败')
  }
}

const disconnectWebSocket = () => {
  wsService.disconnect()
  connectionStatus.value = 'disconnected'
  addLog('info', 'WebSocket连接已断开')
  ElMessage.info('WebSocket连接已断开')
}

const sendTestMessage = () => {
  if (!canSendMessage.value) return
  
  try {
    wsService.sendPrivateMessage(
      parseInt(selectedReceiver.value),
      testMessage.value.trim()
    )
    
    addLog('success', `发送消息给用户${selectedReceiver.value}: ${testMessage.value}`)
    testMessage.value = ''
    
  } catch (error: any) {
    addLog('error', `发送消息失败: ${error.message}`)
    ElMessage.error('发送消息失败')
  }
}

const sendTypingTest = () => {
  if (!selectedReceiver.value || connectionStatus.value !== 'connected') return
  
  try {
    // 发送正在输入
    wsService.sendTypingIndicator(parseInt(selectedReceiver.value), true)
    addLog('info', `发送输入指示器给用户${selectedReceiver.value}: 正在输入`)
    
    // 2秒后发送停止输入
    setTimeout(() => {
      wsService.sendTypingIndicator(parseInt(selectedReceiver.value), false)
      addLog('info', `发送输入指示器给用户${selectedReceiver.value}: 停止输入`)
    }, 2000)
    
  } catch (error: any) {
    addLog('error', `发送输入指示器失败: ${error.message}`)
  }
}

const clearLogs = () => {
  logs.value = []
  addLog('info', '日志已清空')
}

const formatTime = (timestamp: number) => {
  return dayjs(timestamp).format('HH:mm:ss.SSS')
}

// WebSocket事件监听
const setupWebSocketListeners = () => {
  // 监听消息
  wsService.onMessage((data) => {
    addLog('info', `收到消息: ${JSON.stringify(data)}`)
  })
  
  // 监听状态变化
  wsService.onStatusChange((status) => {
    switch (status) {
      case 'open':
        connectionStatus.value = 'connected'
        addLog('success', 'WebSocket连接已打开')
        break
      case 'close':
        connectionStatus.value = 'disconnected'
        addLog('warning', 'WebSocket连接已关闭')
        break
      case 'error':
        connectionStatus.value = 'disconnected'
        addLog('error', 'WebSocket连接出错')
        break
    }
  })
}

// 生命周期
onMounted(() => {
  addLog('info', 'WebSocket测试页面已加载')
  setupWebSocketListeners()
  
  // 如果已登录，显示用户信息
  if (authStore.isLoggedIn) {
    addLog('info', `当前用户: ${authStore.currentUser?.name}`)
    
    // 过滤掉当前用户
    const currentUserId = authStore.currentUser?.id
    if (currentUserId) {
      selectedReceiver.value = testUsers.find(u => u.id !== currentUserId)?.id || ''
    }
  }
  
  // 检查WebSocket连接状态
  if (wsService.isConnected) {
    connectionStatus.value = 'connected'
    addLog('info', 'WebSocket已连接')
  }
})

onUnmounted(() => {
  // 清理监听器
  // 注意：这里应该移除监听器，但当前WebSocket服务没有提供移除方法
  // 在实际应用中应该实现监听器的移除功能
})
</script>

<style scoped>
.websocket-test-view {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-info {
  margin-bottom: 20px;
}

.status-info p {
  margin: 8px 0;
  color: #606266;
}

.control-buttons {
  display: flex;
  gap: 10px;
}

.message-controls {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.log-card {
  min-height: 400px;
}

.log-container {
  height: 350px;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
  background-color: #f5f7fa;
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.log-item {
  margin-bottom: 4px;
  padding: 2px 0;
  word-wrap: break-word;
}

.log-time {
  color: #909399;
  margin-right: 8px;
}

.log-type {
  margin-right: 8px;
  font-weight: bold;
}

.log-info .log-type {
  color: #409eff;
}

.log-success .log-type {
  color: #67c23a;
}

.log-warning .log-type {
  color: #e6a23c;
}

.log-error .log-type {
  color: #f56c6c;
}

.log-message {
  color: #303133;
}

@media (max-width: 768px) {
  .websocket-test-view {
    padding: 10px;
  }
  
  .control-buttons {
    flex-direction: column;
  }
  
  .log-container {
    height: 250px;
  }
}
</style>