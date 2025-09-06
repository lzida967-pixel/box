<template>
  <div class="friends-page">
    <div class="friends-header">
      <h2>我的好友</h2>
      <el-button 
        type="primary" 
        @click="showAddFriend = true"
        icon="Plus"
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
        <el-icon class="new-friends-arrow"><ArrowRight /></el-icon>
      </div>
    </div>

    <div class="friends-list">
      <div
        v-for="friend in filteredFriends"
        :key="friend.id"
        class="friend-item"
        @click="startChat(friend)"
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
        <el-icon class="friend-arrow">
          <ArrowRight />
        </el-icon>
      </div>

      <el-empty 
        v-if="filteredFriends.length === 0 && !loading" 
        description="暂无好友" 
      />
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
import { ref, computed, onMounted } from 'vue'
import ElMessage from 'element-plus/es/components/message/index.mjs'
import { ArrowRight, User } from '@element-plus/icons-vue'
import { contactApi } from '@/api'
import AddFriendDialog from './AddFriendDialog.vue'
import FriendRequestsDialog from './FriendRequestsDialog.vue'
import type { User as UserType } from '@/types'

interface Emits {
  (e: 'start-chat', user: UserType): void
}

const emit = defineEmits<Emits>()

const friends = ref<UserType[]>([])
const searchText = ref('')
const loading = ref(false)
const showAddFriend = ref(false)
const showFriendRequests = ref(false)

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
    friends.value = response.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载好友列表失败')
  } finally {
    loading.value = false
  }
}

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
}

const startChat = (friend: UserType) => {
  emit('start-chat', friend)
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
})
</script>

<style scoped>
.friends-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.friends-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.friends-header h2 {
  margin: 0;
  color: #333;
}

.search-input {
  margin-bottom: 20px;
}

/* 新的朋友选项样式 */
.new-friends-section {
  margin-bottom: 20px;
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

.new-friends-arrow {
  color: #ccc;
  font-size: 16px;
}

.friends-list {
  flex: 1;
  overflow-y: auto;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  gap: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.friend-item:hover {
  background-color: #f8f9fa;
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

.friend-arrow {
  color: #ccc;
  font-size: 16px;
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