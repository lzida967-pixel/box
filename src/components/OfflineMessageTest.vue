<template>
  <div class="offline-message-test">
    <el-card header="离线消息测试">
      <div class="test-section">
        <h3>当前状态</h3>
        <p>WebSocket连接状态: {{ wsStatus }}</p>
        <p>当前用户: {{ authStore.userInfo?.username }}</p>
        <p>离线消息数量: {{ offlineMessages.length }}</p>
      </div>

      <div class="test-section">
        <h3>操作</h3>
        <el-button @click="loadOfflineMessages" type="primary">
          加载离线消息
        </el-button>
        <el-button @click="clearOfflineMessages" type="warning">
          清空离线消息
        </el-button>
        <el-button @click="simulateOfflineMessage" type="success">
          模拟离线消息
        </el-button>
      </div>

      <div class="test-section">
        <h3>离线消息列表</h3>
        <div v-if="offlineMessages.length === 0" class="empty-state">
          暂无离线消息
        </div>
        <div v-else class="message-list">
          <div 
            v-for="message in offlineMessages" 
            :key="message.id"
            class="message-item"
          >
            <div class="message-info">
              <span class="sender">发送者: {{ message.senderId }}</span>
              <span class="time">{{ formatTime(message.createTime) }}</span>
            </div>
            <div class="message-content">{{ message.content }}</div>
          </div>
        </div>
      </div>

      <div class="test-section">
        <h3>测试日志</h3>
        <div class="log-container">
          <div 
            v-for="(log, index) in testLogs" 
            :key="index"
            class="log-item"
            :class="log.type"
          >
            <span class="log-time">{{ formatTime(log.time) }}</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { getWebSocketService } from '@/services/websocket'
import { chatApi } from '@/api'
import type { Message } from '@/types'
import dayjs from 'dayjs'

const authStore = useAuthStore()
const chatStore = useChatStore()
const wsService = getWebSocketService()

const wsStatus = ref('disconnected')
const offlineMessages = ref<Message[]>([])
const testLogs = ref<Array<{time: string, message: string, type: string}>>([])

// 添加日志
const addLog = (message: string, type: 'info' | 'success' | 'warning' | 'error' = 'info') => {
  testLogs.value.unshift({
    time: new Date().toISOString(),
    message,
    type
  })
  
  // 限制日志数量
  if (testLogs.value.length > 50) {
    testLogs.value = testLogs.value.slice(0, 50)
  }
}

// 加载离线消息
const loadOfflineMessages = async () => {
  try {
    addLog('开始加载离线消息...', 'info')
    const response = await chatApi.getOfflineMessages()
    
    if (response.code === 200) {
      offlineMessages.value = response.data || []
      addLog(`加载离线消息成功: ${offlineMessages.value.length} 条`, 'success')
      ElMessage.success(`加载到 ${offlineMessages.value.length} 条离线消息`)
    } else {
      addLog(`加载离线消息失败: ${response.message}`, 'error')
      ElMessage.error('加载离线消息失败')
    }
  } catch (error) {
    addLog(`加载离线消息异常: ${error}`, 'error')
    ElMessage.error('加载离线消息异常')
  }
}

// 清空离线消息
const clearOfflineMessages = async () => {
  try {
    if (offlineMessages.value.length === 0) {
      ElMessage.info('没有离线消息需要清空')
      return
    }

    const messageIds = offlineMessages.value.map(msg => msg.id)
    const response = await chatApi.markOfflineMessagesAsRead(messageIds)
    
    if (response.code === 200) {
      offlineMessages.value = []
      addLog('离线消息清空成功', 'success')
      ElMessage.success('离线消息已清空')
    } else {
      addLog(`清空离线消息失败: ${response.message}`, 'error')
      ElMessage.error('清空离线消息失败')
    }
  } catch (error) {
    addLog(`清空离线消息异常: ${error}`, 'error')
    ElMessage.error('清空离线消息异常')
  }
}

// 模拟离线消息
const simulateOfflineMessage = () => {
  const mockMessage: Message = {
    id: Date.now(),
    senderId: 999,
    receiverId: authStore.userInfo?.id || 1,
    content: `模拟离线消息 - ${new Date().toLocaleTimeString()}`,
    messageType: 'text',
    status: 'delivered',
    createTime: new Date().toISOString(),
    updateTime: new Date().toISOString(),
    isRecalled: false
  }
  
  offlineMessages.value.unshift(mockMessage)
  addLog('添加模拟离线消息', 'info')
  ElMessage.info('已添加模拟离线消息')
}

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).format('HH:mm:ss')
}

// WebSocket状态监听
const handleWSStatusChange = (status: string) => {
  wsStatus.value = status
  addLog(`WebSocket状态变更: ${status}`, 'info')
}

// 初始化
onMounted(() => {
  addLog('离线消息测试组件已加载', 'info')
  
  // 监听WebSocket状态
  wsService.onStatusChange(handleWSStatusChange)
  
  // 获取当前状态
  wsStatus.value = wsService.isConnected ? 'connected' : 'disconnected'
  
  // 自动加载离线消息
  loadOfflineMessages()
})

// 清理
onUnmounted(() => {
  wsService.offStatusChange(handleWSStatusChange)
})
</script>

<style scoped>
.offline-message-test {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.test-section {
  margin-bottom: 20px;
  padding: 15px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background: #fafafa;
}

.test-section h3 {
  margin: 0 0 10px 0;
  color: #333;
}

.empty-state {
  text-align: center;
  color: #999;
  padding: 20px;
}

.message-list {
  max-height: 300px;
  overflow-y: auto;
}

.message-item {
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  margin-bottom: 8px;
  background: white;
}

.message-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #666;
  margin-bottom: 5px;
}

.message-content {
  font-size: 14px;
  color: #333;
}

.log-container {
  max-height: 200px;
  overflow-y: auto;
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
}

.log-item {
  font-size: 12px;
  margin-bottom: 4px;
  padding: 2px 0;
}

.log-item.info { color: #666; }
.log-item.success { color: #67c23a; }
.log-item.warning { color: #e6a23c; }
.log-item.error { color: #f56c6c; }

.log-time {
  margin-right: 8px;
  color: #999;
}

.log-message {
  color: inherit;
}

/* 滚动条样式 */
.message-list::-webkit-scrollbar,
.log-container::-webkit-scrollbar {
  width: 6px;
}

.message-list::-webkit-scrollbar-track,
.log-container::-webkit-scrollbar-track {
  background: transparent;
}

.message-list::-webkit-scrollbar-thumb,
.log-container::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}
</style>