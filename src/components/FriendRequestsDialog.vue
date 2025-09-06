<template>
  <el-dialog
    v-model="visible"
    title="好友请求"
    width="600px"
    @close="handleClose"
  >
    <div class="friend-requests-dialog">
      <!-- 标签页 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="收到的请求" name="received">
          <div class="requests-list">
            <div
              v-for="request in receivedRequests"
              :key="request.friendship.id"
              class="request-item"
            >
              <el-avatar :src="getUserAvatar(request.fromUser)" :size="48" />
              <div class="request-info">
                <div class="user-name">{{ request.fromUser?.nickname || request.fromUser?.username }}</div>
                <div class="request-message">{{ request.friendship.requestMessage || '请求添加您为好友' }}</div>
                <div class="request-time">{{ formatTime(request.friendship.createTime) }}</div>
              </div>
              <div class="request-actions">
                <el-button 
                  size="small" 
                  @click="handleRequest(request.friendship.id, false)"
                  :loading="processingRequests.has(request.friendship.id)"
                >
                  拒绝
                </el-button>
                <el-button 
                  type="primary" 
                  size="small"
                  @click="handleRequest(request.friendship.id, true)"
                  :loading="processingRequests.has(request.friendship.id)"
                >
                  接受
                </el-button>
              </div>
            </div>
            
            <el-empty 
              v-if="receivedRequests.length === 0 && !loading" 
              description="暂无好友请求" 
            />
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="发送的请求" name="sent">
          <div class="requests-list">
            <div
              v-for="request in sentRequests"
              :key="request.friendship.id"
              class="request-item"
            >
              <el-avatar :src="getUserAvatar(request.toUser)" :size="48" />
              <div class="request-info">
                <div class="user-name">{{ request.toUser?.nickname || request.toUser?.username }}</div>
                <div class="request-message">{{ request.friendship.requestMessage || '等待对方确认' }}</div>
                <div class="request-time">{{ formatTime(request.friendship.createTime) }}</div>
              </div>
              <div class="request-status">
                <el-tag type="warning" size="small">等待确认</el-tag>
              </div>
            </div>
            
            <el-empty 
              v-if="sentRequests.length === 0 && !loading" 
              description="暂无发送的请求" 
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import ElMessage from 'element-plus/es/components/message/index.mjs'
import { contactApi } from '@/api'
import type { FriendshipWithUser, User } from '@/types'
import dayjs from 'dayjs'

interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'request-handled'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const activeTab = ref('received')
const loading = ref(false)
const processingRequests = ref(new Set<number>())

const receivedRequests = ref<FriendshipWithUser[]>([])
const sentRequests = ref<FriendshipWithUser[]>([])

// 监听对话框打开状态
watch(visible, (newVal) => {
  if (newVal) {
    loadRequests()
  }
})

const loadRequests = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'received') {
      await loadReceivedRequests()
    } else {
      await loadSentRequests()
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载请求失败')
  } finally {
    loading.value = false
  }
}

const loadReceivedRequests = async () => {
  try {
    const response = await contactApi.getReceivedRequests()
    receivedRequests.value = response.data || []
  } catch (error) {
    throw error
  }
}

const loadSentRequests = async () => {
  try {
    const response = await contactApi.getSentRequests()
    sentRequests.value = response.data || []
  } catch (error) {
    throw error
  }
}

const handleTabChange = (tabName: string) => {
  activeTab.value = tabName
  loadRequests()
}

const handleRequest = async (requestId: number, accept: boolean) => {
  processingRequests.value.add(requestId)
  try {
    const action = accept ? 'accept' : 'reject'
    await contactApi.handleFriendRequest(requestId.toString(), action)
    
    const message = accept ? '已接受好友请求' : '已拒绝好友请求'
    ElMessage.success(message)
    
    // 从列表中移除处理过的请求
    receivedRequests.value = receivedRequests.value.filter(req => req.friendship.id !== requestId)
    
    emit('request-handled')
  } catch (error: any) {
    ElMessage.error(error.message || '处理请求失败')
  } finally {
    processingRequests.value.delete(requestId)
  }
}

// 处理头像URL
const getUserAvatar = (user?: User) => {
  if (!user) return 'https://avatars.githubusercontent.com/u/0?v=4'
  
  // 如果用户有自定义头像
  if (user.avatar) {
    // 如果是本地上传的头像（以avatar_开头）
    if (user.avatar.startsWith('avatar_')) {
      return `http://localhost:8080/api/user/avatar/${user.id}`
    }
    // 其他情况直接使用avatar字段
    return user.avatar
  }
  
  // 默认头像
  return 'https://avatars.githubusercontent.com/u/0?v=4'
}

const formatTime = (timeStr: string) => {
  return dayjs(timeStr).format('YYYY-MM-DD HH:mm')
}

const handleClose = () => {
  receivedRequests.value = []
  sentRequests.value = []
}
</script>

<style scoped>
.friend-requests-dialog {
  min-height: 400px;
}

.requests-list {
  max-height: 400px;
  overflow-y: auto;
}

.request-item {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  gap: 12px;
}

.request-item:last-child {
  border-bottom: none;
}

.request-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-weight: 500;
  font-size: 16px;
  color: #333;
  margin-bottom: 4px;
}

.request-message {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.request-time {
  font-size: 12px;
  color: #999;
}

.request-actions {
  display: flex;
  gap: 8px;
}

.request-status {
  display: flex;
  align-items: center;
}

/* 滚动条样式 */
.requests-list::-webkit-scrollbar {
  width: 4px;
}

.requests-list::-webkit-scrollbar-track {
  background: transparent;
}

.requests-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.requests-list::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>