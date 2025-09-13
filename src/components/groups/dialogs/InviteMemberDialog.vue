<template>
  <el-dialog
    v-model="dialogVisible"
    title="邀请成员"
    width="500px"
    :before-close="handleClose"
  >
    <!-- 搜索栏 -->
    <div class="search-section">
      <el-input
        v-model="searchText"
        placeholder="搜索好友"
        prefix-icon="Search"
        clearable
      />
    </div>

    <!-- 好友列表 -->
    <div class="friend-list">
      <div
        v-for="friend in filteredFriends"
        :key="friend.id"
        class="friend-item"
        :class="{ selected: isSelected(friend) }"
        @click="toggleFriend(friend)"
      >
        <el-checkbox
          :model-value="isSelected(friend)"
          @change="toggleFriend(friend)"
        />
        <el-avatar :src="getUserAvatar(friend)" :size="40" />
        <div class="friend-info">
          <div class="friend-name">{{ friend.nickname || friend.username }}</div>
          <div class="friend-status">{{ getStatusText(friend.status) }}</div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredFriends.length === 0" class="empty-state">
        <el-empty :description="friends.length === 0 ? '没有可邀请的好友' : '未找到匹配的好友'" />
      </div>
    </div>

    <!-- 已选择的好友 -->
    <div v-if="selectedFriends.length > 0" class="selected-section">
      <div class="selected-title">已选择 ({{ selectedFriends.length }})</div>
      <div class="selected-list">
        <div
          v-for="friend in selectedFriends"
          :key="friend.id"
          class="selected-friend"
        >
          <el-avatar :src="getUserAvatar(friend)" :size="24" />
          <span class="selected-name">{{ friend.nickname || friend.username }}</span>
          <el-icon class="remove-icon" @click="removeFriend(friend)">
            <Close />
          </el-icon>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleInvite" 
          :loading="loading"
          :disabled="selectedFriends.length === 0"
        >
          邀请 ({{ selectedFriends.length }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import { contactApi, groupApi } from '@/api'
import type { User } from '@/types'

// Props & Emits
const props = defineProps<{
  modelValue: boolean
  groupId?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

// 响应式数据
const searchText = ref('')
const friends = ref<User[]>([])
const groupMembers = ref<User[]>([])
const selectedFriends = ref<User[]>([])
const loading = ref(false)

// 计算属性
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const filteredFriends = computed(() => {
  if (!searchText.value) return friends.value
  
  const keyword = searchText.value.toLowerCase()
  return friends.value.filter(friend => 
    (friend.nickname || friend.username).toLowerCase().includes(keyword) ||
    friend.username.toLowerCase().includes(keyword)
  )
})

// 方法
const loadData = async () => {
  if (!props.groupId) return

  try {
    loading.value = true
    
    // 直接加载不在群内的好友列表
    console.log('正在调用API，groupId:', props.groupId)
    const friendsResponse = await contactApi.getFriendsNotInGroup(props.groupId)
    console.log('API响应:', friendsResponse)
    console.log('响应数据:', friendsResponse.data)
    
    // 处理API响应数据
    if (Array.isArray(friendsResponse.data)) {
      // 直接返回数组的情况
      friends.value = friendsResponse.data
      console.log('设置好友列表:', friends.value)
    } else if (friendsResponse.data && friendsResponse.data.success) {
      // 标准响应格式的情况
      friends.value = friendsResponse.data.data || []
      console.log('设置好友列表:', friends.value)
    } else {
      friends.value = []
      console.error('API响应格式错误:', friendsResponse.data)
    }
    
    // 由于后端已经过滤了群内成员，这里设置为空数组
    groupMembers.value = []
  } catch (error: any) {
    console.error('加载数据失败:', error)
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

const isSelected = (friend: User) => {
  return selectedFriends.value.some(f => f.id === friend.id)
}

const toggleFriend = (friend: User) => {
  const index = selectedFriends.value.findIndex(f => f.id === friend.id)
  if (index > -1) {
    selectedFriends.value.splice(index, 1)
  } else {
    selectedFriends.value.push(friend)
  }
}

const removeFriend = (friend: User) => {
  const index = selectedFriends.value.findIndex(f => f.id === friend.id)
  if (index > -1) {
    selectedFriends.value.splice(index, 1)
  }
}

const handleClose = () => {
  selectedFriends.value = []
  searchText.value = ''
  emit('update:modelValue', false)
}

const handleInvite = async () => {
  if (!props.groupId || selectedFriends.value.length === 0) return

  try {
    loading.value = true
    
    const memberIds = selectedFriends.value.map(f => f.id)
    await groupApi.inviteMembers(props.groupId, memberIds)
    
    ElMessage.success(`成功邀请 ${selectedFriends.value.length} 位好友`)
    emit('success')
    handleClose()
  } catch (error: any) {
    console.error('邀请成员失败:', error)
    ElMessage.error(error.message || '邀请成员失败')
  } finally {
    loading.value = false
  }
}

const getUserAvatar = (user: User) => {
  if (!user.avatar) return 'https://avatars.githubusercontent.com/u/0?v=4'
  
  if (user.avatar.startsWith('avatar_')) {
    return `http://localhost:8080/api/user/avatar/${user.id}`
  } else if (user.avatar.startsWith('/api/user/avatar/')) {
    return `http://localhost:8080${user.avatar}`
  }
  
  return user.avatar
}

const getStatusText = (status?: number) => {
  switch (status) {
    case 1: return '在线'
    case 2: return '忙碌'
    case 3: return '离开'
    case 0:
    default: return '离线'
  }
}

// 监听对话框显示状态
watch(dialogVisible, (visible) => {
  if (visible) {
    loadData()
  } else {
    selectedFriends.value = []
    searchText.value = ''
  }
})
</script>

<style scoped>
.search-section {
  margin-bottom: 16px;
}

.friend-list {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.2s;
  gap: 12px;
}

.friend-item:hover {
  background-color: #f8f9fa;
}

.friend-item.selected {
  background-color: #e6f4ff;
}

.friend-info {
  flex: 1;
}

.friend-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  margin-bottom: 2px;
}

.friend-status {
  font-size: 12px;
  color: #999;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
}

.selected-section {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}

.selected-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
}

.selected-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 120px;
  overflow-y: auto;
}

.selected-friend {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: #f0f0f0;
  border-radius: 16px;
  font-size: 12px;
}

.selected-name {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remove-icon {
  cursor: pointer;
  color: #999;
  font-size: 14px;
}

.remove-icon:hover {
  color: #f56c6c;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 滚动条样式 */
.friend-list::-webkit-scrollbar,
.selected-list::-webkit-scrollbar {
  width: 4px;
}

.friend-list::-webkit-scrollbar-track,
.selected-list::-webkit-scrollbar-track {
  background: transparent;
}

.friend-list::-webkit-scrollbar-thumb,
.selected-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.friend-list::-webkit-scrollbar-thumb:hover,
.selected-list::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>