<template>
  <el-dialog
    v-model="dialogVisible"
    title="选择好友"
    width="500px"
    :before-close="handleClose"
    class="friend-selector-dialog"
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
        <el-empty :description="friends.length === 0 ? '暂无好友' : '未找到匹配的好友'" />
      </div>
    </div>

    <!-- 已选择的好友 -->
    <div v-if="localSelectedFriends.length > 0" class="selected-section">
      <div class="selected-title">已选择 ({{ localSelectedFriends.length }})</div>
      <div class="selected-list">
        <div
          v-for="friend in localSelectedFriends"
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
        <el-button type="primary" @click="handleConfirm">
          确定 ({{ localSelectedFriends.length }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import { contactApi } from '@/api'
import type { User } from '@/types'

// Props & Emits
const props = defineProps<{
  modelValue: boolean
  selectedFriends?: User[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [friends: User[]]
}>()

// 响应式数据
const searchText = ref('')
const friends = ref<User[]>([])
const localSelectedFriends = ref<User[]>([])
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
const loadFriends = async () => {
  try {
    loading.value = true
    const response = await contactApi.getFriends()
    friends.value = response.data || []
  } catch (error: any) {
    console.error('加载好友列表失败:', error)
    ElMessage.error(error.message || '加载好友列表失败')
  } finally {
    loading.value = false
  }
}

const isSelected = (friend: User) => {
  return localSelectedFriends.value.some(f => f.id === friend.id)
}

const toggleFriend = (friend: User) => {
  const index = localSelectedFriends.value.findIndex(f => f.id === friend.id)
  if (index > -1) {
    localSelectedFriends.value.splice(index, 1)
  } else {
    localSelectedFriends.value.push(friend)
  }
}

const removeFriend = (friend: User) => {
  const index = localSelectedFriends.value.findIndex(f => f.id === friend.id)
  if (index > -1) {
    localSelectedFriends.value.splice(index, 1)
  }
}

const handleClose = () => {
  emit('update:modelValue', false)
}

const handleConfirm = () => {
  emit('confirm', [...localSelectedFriends.value])
  handleClose()
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
    loadFriends()
    // 初始化已选择的好友
    localSelectedFriends.value = [...(props.selectedFriends || [])]
  } else {
    searchText.value = ''
  }
})

// 生命周期
onMounted(() => {
  if (props.modelValue) {
    loadFriends()
  }
})
</script>

<style scoped>
.friend-selector-dialog :deep(.el-dialog__body) {
  padding: 20px;
  max-height: 60vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.search-section {
  margin-bottom: 16px;
}

.friend-list {
  flex: 1;
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