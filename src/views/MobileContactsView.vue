<template>
  <div class="mobile-contacts-view">
    <!-- 顶部标题栏 -->
    <div class="header">
      <h2>好友</h2>
      <el-button 
        type="primary" 
        @click="showAddFriend = true"
        icon="Plus"
        size="small"
        circle
        class="add-button"
      />
    </div>
    
    <!-- 搜索栏 -->
    <div class="search-section">
      <el-input
        v-model="searchText"
        placeholder="搜索好友"
        prefix-icon="Search"
        clearable
        class="search-input"
      />
    </div>
    
    <!-- 新的朋友选项 -->
    <div class="new-friends-section" @click="showFriendRequests = true">
      <div class="new-friends-item">
        <el-icon class="new-friends-icon"><User /></el-icon>
        <span class="new-friends-text">新的朋友</span>
        <div class="new-friends-right">
          <el-badge 
            v-if="unreadRequestCount > 0" 
            :value="unreadRequestCount" 
            :max="99"
            class="friend-request-badge"
          />
          <el-icon class="new-friends-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>
    
    <!-- 好友列表 -->
    <div class="friends-list">
      <div
        v-for="friend in displayFriends"
        :key="friend.id"
        class="friend-item"
        @click="handleFriendClick(friend)"
      >
        <el-avatar :src="getUserAvatar(friend)" :size="48" />
        <div class="friend-info">
          <div class="friend-name">{{ friend.nickname || friend.username }}</div>
          <div class="friend-username">@{{ friend.username }}</div>
          <div v-if="friend.signature" class="friend-signature">{{ friend.signature }}</div>
        </div>
        <div class="friend-status">
          <el-tag :type="getStatusType(friend.status || 0)" size="small">
            {{ getStatusText(friend.status || 0) }}
          </el-tag>
        </div>
      </div>
      
      <div v-if="displayFriends.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无好友" />
      </div>
      <div v-else-if="loading" class="loading-state">
        <el-skeleton :rows="5" animated />
      </div>
    </div>
    
    <!-- 添加好友对话框 -->
    <AddFriendDialog 
      v-model="showAddFriend" 
      @friend-added="handleFriendAdded"
    />
    
    <!-- 好友请求对话框 -->
    <FriendRequestsDialog 
      v-model="showFriendRequests"
      @request-handled="loadFriends"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { contactApi } from '@/api'
import { useChatStore } from '@/stores/chat'
import { getUserAvatarUrl } from '@/utils/avatar'
import AddFriendDialog from '@/components/AddFriendDialog.vue'
import FriendRequestsDialog from '@/components/FriendRequestsDialog.vue'
import type { User as UserType } from '@/types'
import { User, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const chatStore = useChatStore()
const searchText = ref('')
const loading = ref(true)
const showAddFriend = ref(false)
const showFriendRequests = ref(false)
const unreadRequestCount = ref(0)
let refreshInterval: number | null = null

// 显示的好友列表（使用chatStore.contacts，与电脑端保持一致）
const displayFriends = computed(() => {
  if (!searchText.value) return chatStore.contacts
  
  const keyword = searchText.value.toLowerCase()
  return chatStore.contacts.filter((friend: UserType) => 
    (friend.nickname && friend.nickname.toLowerCase().includes(keyword)) ||
    friend.username.toLowerCase().includes(keyword) ||
    (friend.signature && friend.signature.toLowerCase().includes(keyword))
  )
})

// 加载好友列表（使用chatStore.loadContacts，与电脑端保持一致）
const loadFriends = async () => {
  loading.value = true
  try {
    await chatStore.loadContacts()
  } catch (error: any) {
    console.error('加载好友列表失败:', error)
    ElMessage.error(error.message || '加载好友列表失败')
  } finally {
    loading.value = false
  }
}

// 获取未读好友请求数量
const loadUnreadRequestCount = async () => {
  try {
    const response = await contactApi.getReceivedRequests()
    // 计算未处理的请求数量（状态为待确认的请求）
    if (Array.isArray(response.data)) {
      const requests = response.data
      unreadRequestCount.value = requests.filter(request => 
        request.friendship && (request.friendship.status === 0 || request.friendship.status === 'pending')
      ).length
    }
  } catch (error: any) {
    console.error('获取好友请求失败:', error)
    // 不显示错误消息，避免干扰用户体验
  }
}

// 获取用户头像
const getUserAvatar = (user?: UserType) => {
  return getUserAvatarUrl(user)
}

// 处理好友点击
const handleFriendClick = (friend: UserType) => {
  // 跳转到好友详情页面
  router.push(`/mobile/contact/${friend.id}`)
}

// 处理好友添加
const handleFriendAdded = () => {
  ElMessage.success('好友请求已发送')
  // 重新加载好友列表
  loadFriends()
  // 刷新未读请求数量
  loadUnreadRequestCount()
}

// 获取状态类型
const getStatusType = (status: number) => {
  switch (status) {
    case 1: return 'success'  // 在线
    case 2: return 'warning'  // 忙碌
    case 3: return 'info'     // 离开
    case 0: 
    default: return 'danger'  // 离开
  }
}

// 获取状态文本
const getStatusText = (status: number) => {
  switch (status) {
    case 1: return '在线'
    case 2: return '忙碌'
    case 3: return '离开'
    case 0: 
    default: return '离线'
  }
}

// 定期刷新数据的函数
const refreshData = async () => {
  await loadFriends()
  await loadUnreadRequestCount()
}

// 初始化数据
onMounted(async () => {
  await loadFriends()
  await loadUnreadRequestCount()
  
  // 每30秒刷新一次数据
  refreshInterval = window.setInterval(refreshData, 30000)
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (refreshInterval) {
    window.clearInterval(refreshInterval)
    refreshInterval = null
  }
})
</script>

<style scoped>
.mobile-contacts-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.header {
  padding: 16px;
  background: linear-gradient(180deg, #4A90E2 0%, #357ABD 100%);
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.header h2 {
  margin: 0;
  font-size: 18px;
}

.add-button {
  position: absolute;
  right: 16px;
}

.search-section {
  padding: 16px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
}

.search-input {
  border-radius: 20px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 20px;
  background: #f8f9fa;
  border: none;
  box-shadow: none;
}

/* 新的朋友选项样式 */
.new-friends-section {
  margin: 0 16px 16px 16px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.new-friends-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: white;
  cursor: pointer;
  transition: background-color 0.2s;
}

.new-friends-item:hover {
  background-color: #f8f9fa;
}

.new-friends-icon {
  font-size: 20px;
  color: #1890ff;
  margin-right: 12px;
}

.new-friends-text {
  flex: 1;
  font-size: 16px;
  color: #333;
}

.new-friends-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.friend-request-badge {
  flex-shrink: 0;
}

.new-friends-arrow {
  color: #ccc;
  font-size: 16px;
}

.friends-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px 16px;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  gap: 16px;
  border: 1px solid transparent;
  background: white;
  margin-bottom: 8px;
}

.friend-item:hover {
  background-color: #f5f7fa;
  border-color: #e4e7ed;
}

.friend-info {
  flex: 1;
  min-width: 0;
}

.friend-name {
  font-weight: 500;
  font-size: 16px;
  color: #333;
  margin-bottom: 4px;
}

.friend-username {
  font-size: 14px;
  color: #666;
  margin-bottom: 2px;
}

.friend-signature {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.friend-status {
  flex-shrink: 0;
}

.empty-state, .loading-state {
  padding: 40px 20px;
  text-align: center;
}

/* 滚动条样式 */
.friends-list::-webkit-scrollbar {
  width: 6px;
}

.friends-list::-webkit-scrollbar-track {
  background: transparent;
}

.friends-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

.friends-list::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>