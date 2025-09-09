<template>
  <div class="message-test-view">
    <div class="header">
      <h1>聊天消息持久化测试</h1>
      <p>测试离线消息推送和消息持久化功能</p>
    </div>

    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="离线消息测试" name="offline">
        <OfflineMessageTest />
      </el-tab-pane>
      
      <el-tab-pane label="聊天历史测试" name="history">
        <ChatHistoryTest />
      </el-tab-pane>
      
      <el-tab-pane label="WebSocket状态" name="websocket">
        <WebSocketStatusTest />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import OfflineMessageTest from '@/components/OfflineMessageTest.vue'

const activeTab = ref('offline')

// 简单的聊天历史测试组件
const ChatHistoryTest = {
  template: `
    <div class="chat-history-test">
      <el-card header="聊天历史测试">
        <div class="test-info">
          <p>此功能测试消息的持久化存储和历史记录加载。</p>
          <p>当你选择一个联系人开始聊天时，系统会自动从数据库加载历史消息。</p>
          <p>请到主聊天界面测试此功能。</p>
        </div>
        <el-button type="primary" @click="goToChat">
          前往聊天界面
        </el-button>
      </el-card>
    </div>
  `,
  methods: {
    goToChat() {
      this.$router.push('/chat')
    }
  }
}

// WebSocket状态测试组件
const WebSocketStatusTest = {
  template: `
    <div class="websocket-status-test">
      <el-card header="WebSocket连接状态">
        <div class="status-info">
          <p>WebSocket连接状态: <span :class="statusClass">{{ wsStatus }}</span></p>
          <p>重连次数: {{ reconnectCount }}</p>
          <p>最后心跳时间: {{ lastHeartbeat }}</p>
        </div>
        <div class="actions">
          <el-button @click="connectWS" type="primary" :disabled="isConnected">
            连接WebSocket
          </el-button>
          <el-button @click="disconnectWS" type="danger" :disabled="!isConnected">
            断开连接
          </el-button>
          <el-button @click="sendHeartbeat" type="info" :disabled="!isConnected">
            发送心跳
          </el-button>
        </div>
      </el-card>
    </div>
  `,
  data() {
    return {
      wsStatus: 'disconnected',
      reconnectCount: 0,
      lastHeartbeat: '未知',
      isConnected: false
    }
  },
  computed: {
    statusClass() {
      return {
        'status-connected': this.isConnected,
        'status-disconnected': !this.isConnected
      }
    }
  },
  methods: {
    async connectWS() {
      try {
        const wsService = await import('@/services/websocket').then(m => m.getWebSocketService())
        await wsService.connect()
        this.updateStatus()
      } catch (error) {
        console.error('连接WebSocket失败:', error)
      }
    },
    disconnectWS() {
      import('@/services/websocket').then(m => {
        const wsService = m.getWebSocketService()
        wsService.disconnect()
        this.updateStatus()
      })
    },
    sendHeartbeat() {
      import('@/services/websocket').then(m => {
        const wsService = m.getWebSocketService()
        wsService.send({ type: 'heartbeat' })
        this.lastHeartbeat = new Date().toLocaleTimeString()
      })
    },
    updateStatus() {
      import('@/services/websocket').then(m => {
        const wsService = m.getWebSocketService()
        this.isConnected = wsService.isConnected
        this.wsStatus = this.isConnected ? 'connected' : 'disconnected'
      })
    }
  },
  mounted() {
    this.updateStatus()
    // 定期更新状态
    this.statusInterval = setInterval(this.updateStatus, 1000)
  },
  unmounted() {
    if (this.statusInterval) {
      clearInterval(this.statusInterval)
    }
  }
}
</script>

<style scoped>
.message-test-view {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  text-align: center;
  margin-bottom: 30px;
}

.header h1 {
  color: #333;
  margin-bottom: 10px;
}

.header p {
  color: #666;
  font-size: 16px;
}

.chat-history-test,
.websocket-status-test {
  padding: 20px;
}

.test-info {
  margin-bottom: 20px;
  line-height: 1.6;
}

.status-info {
  margin-bottom: 20px;
}

.status-connected {
  color: #67c23a;
  font-weight: bold;
}

.status-disconnected {
  color: #f56c6c;
  font-weight: bold;
}

.actions {
  display: flex;
  gap: 10px;
}

:deep(.el-tabs__content) {
  padding: 0;
}
</style>