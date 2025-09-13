<template>
  <div class="friends-page">
    <div class="friends-layout">
      <!-- 左侧好友列表 -->
      <div class="friends-sidebar">
        <div class="friends-header">
          <h2>我的好友</h2>
          <el-button 
            type="primary" 
            @click="showAddFriend = true"
            icon="Plus"
            size="small"
          >
            添加好友
          </el-button>
        </div>

        <el-input
          v-model="searchText"
          placeholder="搜索好友"
          prefix-icon="Search"
          clearable
          class="search-input"
        />

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

        <div class="friends-list">
          <div
            v-for="friend in filteredFriends"
            :key="friend.id"
            class="friend-item"
            :class="{ active: selectedFriend?.id === friend.id }"
            @click="selectFriend(friend)"
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

          <el-empty 
            v-if="filteredFriends.length === 0 && !loading" 
            description="暂无好友" 
          />
        </div>
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
import ElMessage from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus'
import { contactApi } from '@/api'
import { useChatStore } from '@/stores/chat'
import AddFriendDialog from './AddFriendDialog.vue'
import FriendRequestsDialog from './FriendRequestsDialog.vue'
import type { User as UserType } from '@/types'

interface Emits {
  (e: 'start-chat', user: UserType): void
  (e: 'friend-selected', user: UserType): void
}

const emit = defineEmits<Emits>()

const chatStore = useChatStore()
const friends = ref<UserType[]>([])
const searchText = ref('')
const loading = ref(false)
const showAddFriend = ref(false)
const showFriendRequests = ref(false)
const selectedFriend = ref<UserType | null>(null)
const unreadRequestCount = ref(0)

const filteredFriends = computed(() => {
  if (!searchText.value) return friends.value
  
  const keyword = searchText.value.toLowerCase()
  return friends.value.filter(friend => 
    (friend.nickname && friend.nickname.toLowerCase().includes(keyword)) ||
    friend.username.toLowerCase().includes(keyword) ||
    (friend.signature && friend.signature.toLowerCase().includes(keyword))
  )
})

const loadFriends = async () => {
  loading.value = true
  try {
    const response = await contactApi.getContacts()
    console.log('好友列表API响应:', response)
    // API返回格式为 { code: number, message: string, data: User[] }
    friends.value = Array.isArray(response.data) ? response.data : []
    console.log('解析后的好友列表:', friends.value)
    // 如果之前选中的好友还在列表中，保持选中状态
    if (selectedFriend.value) {
      const stillExists = friends.value.find(f => f.id === selectedFriend.value?.id)
      if (!stillExists) {
        selectedFriend.value = null
      }
    }
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
    console.log('好友请求API响应:', response.data)
    // 计算未处理的请求数量（状态为待确认的请求）
    const requests = Array.isArray(response.data) ? response.data : []
    unreadRequestCount.value = requests.filter(request => 
      request.friendship && (request.friendship.status === 0 || request.friendship.status === 'pending')
    ).length
    console.log('未读好友请求数量:', unreadRequestCount.value)
  } catch (error: any) {
    console.error('获取好友请求失败:', error)
    // 不显示错误消息，避免干扰用户体验
  }
}

// 选择好友
const selectFriend = (friend: UserType) => {
  selectedFriend.value = friend
  // 发送选中好友事件给父组件
  emit('friend-selected', friend)
}

// 监听好友删除事件
onMounted(() => {
  // 监听好友删除事件，重新加载好友列表
  const handleFriendDeleted = () => {
    loadFriends()
    loadUnreadRequestCount()
  }
  
  // 监听好友请求处理事件，更新未读数量
  const handleFriendRequestHandled = () => {
    loadUnreadRequestCount()
  }
  
  // 添加事件监听器
  window.addEventListener('friend-deleted', handleFriendDeleted)
  window.addEventListener('friend-request-handled', handleFriendRequestHandled)
  
  // 组件卸载时移除事件监听器
  onUnmounted(() => {
    window.removeEventListener('friend-deleted', handleFriendDeleted)
    window.removeEventListener('friend-request-handled', handleFriendRequestHandled)
  })
})

// 处理头像URL
const getUserAvatar = (user?: UserType) => {
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

const handleFriendAdded = () => {
  ElMessage.success('好友请求已发送')
  // 重新加载好友列表
  loadFriends()
  // 刷新未读请求数量
  loadUnreadRequestCount()
}

const startChat = (friend: UserType) => {
  emit('start-chat', friend)
}

// 删除好友
const removeFriend = async (friend: UserType) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除好友 "${friend.nickname || friend.username}" 吗？删除后将同时清除所有聊天记录，此操作不可恢复。`,
      '删除好友',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )

    // 调用删除好友API
    await contactApi.removeContact(friend.id.toString())
    
    // 清理本地聊天数据
    clearLocalChatData(friend.id)
    
    ElMessage.success('好友删除成功')
    
    // 重新加载好友列表
    await loadFriends()
    
    // 如果删除的是当前选中的好友，清空选中状态
    if (selectedFriend.value?.id === friend.id) {
      selectedFriend.value = null
    }
    
    // 发送全局事件通知其他组件
    window.dispatchEvent(new CustomEvent('friend-deleted', { 
      detail: { friendId: friend.id } 
    }))
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除好友失败:', error)
      ElMessage.error(error.message || '删除好友失败')
    }
  }
}

// 清理本地聊天数据
const clearLocalChatData = (friendId: number) => {
  chatStore.clearFriendChatData(friendId)
}

const getStatusType = (status: number) => {
  switch (status) {
    case 1: return 'success'  // 在线
    case 2: return 'warning'  // 忙碌
    case 3: return 'info'     // 离开
    case 0: 
    default: return 'danger'  // 离线
  }
}

const getStatusText = (status: number) => {
  switch (status) {
    case 1: return '在线'
    case 2: return '忙碌'
    case 3: return '离开'
    case 0: 
    default: return '离线'
  }
}

onMounted(() => {
  loadFriends()
  loadUnreadRequestCount()
})
</script>

<style scoped>
.friends-page {
  height: 100%;
  padding: 0;
}

.friends-layout {
  display: flex;
  height: 100%;
}

.friends-sidebar {
  width: 400px;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}

.friends-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
}

.friends-header h2 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.search-input {
  margin: 20px;
}

/* 新的朋友选项样式 */
.new-friends-section {
  margin: 0 20px 20px 20px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 极速版0.1);
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
  padding: 0 20px 20px 20px;
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

.friend-item.active {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.friend-info {
  flex: 1;
  min-width: 0;
}

.friend-name {
  font-weight: 500;
  font-size: 极速版16px;
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