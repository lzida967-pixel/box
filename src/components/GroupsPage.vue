<template>
  <div class="groups-page">
    <!-- 搜索栏 -->
    <div class="search-section">
      <el-input
        v-model="searchText"
        placeholder="搜索群聊"
        prefix-icon="Search"
        clearable
        class="search-input"
      />
      <el-button 
        type="primary" 
        @click="showCreateGroupDialog = true"
        icon="Plus"
        circle
        title="创建群聊"
      />
    </div>

    <!-- 群聊列表 -->
    <div class="groups-list">
      <div class="section-title">我的群聊</div>
      <div
        v-for="group in filteredGroups"
        :key="group.id"
        class="group-item"
        :class="{ active: selectedGroup?.id === group.id }"
        @click="handleSelectGroup(group)"
      >
        <div class="group-avatar">
          <el-avatar :src="getGroupAvatar(group)" :size="48">
            {{ (group.remark || group.name).charAt(0) }}
          </el-avatar>
          <el-badge
            v-if="group.unreadCount && group.unreadCount > 0"
            :value="group.unreadCount"
            :max="99"
            class="unread-badge"
          />
        </div>
        
        <div class="group-content">
          <div class="group-header">
            <div class="group-name">{{ group.remark || group.name }}</div>
            <div class="group-time">{{ formatTime(group.lastMessageTime) }}</div>
          </div>
          
          <div class="group-footer">
            <div class="last-message">
              <span v-if="group.lastMessage">
                {{ getLastMessageText(group.lastMessage) }}
              </span>
              <span v-else class="no-message">暂无消息</span>
            </div>
            <div class="member-count">{{ group.memberCount }}人</div>
          </div>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div v-if="groups.length === 0" class="empty-state">
        <el-empty description="暂无群聊" />
      </div>
    </div>

    <!-- 创建群聊对话框 -->
    <CreateGroupDialog 
      v-model="showCreateGroupDialog"
      @created="handleGroupCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { groupApi } from '@/api'
import CreateGroupDialog from './groups/dialogs/CreateGroupDialog.vue'
import type { ChatGroup } from '@/types'
import dayjs from 'dayjs'

// 响应式数据
const authStore = useAuthStore()
const searchText = ref('')
const groups = ref<ChatGroup[]>([])
const selectedGroup = ref<ChatGroup | null>(null)
const showCreateGroupDialog = ref(false)
const loading = ref(false)

// 计算属性
const filteredGroups = computed(() => {
  if (!searchText.value) return groups.value
  return groups.value.filter(group => 
    (group.remark || group.name).toLowerCase().includes(searchText.value.toLowerCase()) ||
    group.description?.toLowerCase().includes(searchText.value.toLowerCase())
  )
})

// 事件定义
const emit = defineEmits<{
  groupSelected: [group: ChatGroup]
  startGroupChat: [group: ChatGroup]
}>()

// 方法
const loadGroups = async () => {
  try {
    loading.value = true
    const response = await groupApi.getUserGroups()
    // 将后端返回的 GroupDTO 转换为前端需要的 ChatGroup 格式
    groups.value = (response.data || []).map((groupDTO: any) => ({
      id: groupDTO.id,
      name: groupDTO.groupName,
      description: groupDTO.groupDescription,
      avatar: groupDTO.groupAvatar,
      ownerId: groupDTO.ownerId,
      maxMembers: groupDTO.maxMembers || 200,
      isPrivate: groupDTO.status === 2, // 2-私有群组, 1-公开群组
      memberCount: groupDTO.memberCount || 1,
      remark: groupDTO.remark || groupDTO.groupName, // 添加 remark 字段
      announcement: groupDTO.announcement,
      createdAt: groupDTO.createTime ? new Date(groupDTO.createTime).toISOString() : new Date().toISOString(),
      updatedAt: groupDTO.createTime ? new Date(groupDTO.createTime).toISOString() : new Date().toISOString()
    }))
  } catch (error: any) {
    console.error('加载群聊列表失败:', error)
    ElMessage.error(error.message || '加载群聊列表失败')
  } finally {
    loading.value = false
  }
}

const handleSelectGroup = (group: ChatGroup) => {
  selectedGroup.value = group
  emit('groupSelected', group)
}

const handleGroupCreated = (groupDTO: any) => {
  // 将后端返回的 GroupDTO 转换为前端需要的 ChatGroup 格式
  const newGroup: ChatGroup = {
    id: groupDTO.id,
    name: groupDTO.groupName,
    description: groupDTO.groupDescription,
    avatar: groupDTO.groupAvatar,
    ownerId: groupDTO.ownerId,
    maxMembers: groupDTO.maxMembers || 200,
    isPrivate: groupDTO.status === 2, // 2-私有群组, 1-公开群组
    memberCount: groupDTO.memberCount || 1,
    remark: groupDTO.remark || groupDTO.groupName, // 添加 remark 字段
    announcement: groupDTO.announcement,
    createdAt: groupDTO.createTime ? new Date(groupDTO.createTime).toISOString() : new Date().toISOString(),
    updatedAt: groupDTO.createTime ? new Date(groupDTO.createTime).toISOString() : new Date().toISOString()
  }
  
  groups.value.unshift(newGroup)
  selectedGroup.value = newGroup
  emit('groupSelected', newGroup)
  ElMessage.success('群聊创建成功')
}

// 存储已加载的群头像
const groupAvatarCache = ref<Map<number, string>>(new Map())

const getGroupAvatar = (group: ChatGroup) => {
  // 检查多个可能的头像字段
  const avatar = group.avatar || group.groupAvatar
  
  if (avatar) {
    // 如果是base64格式的图片数据
    if (avatar.startsWith('data:image/')) {
      return avatar
    }
  }
  
  // 检查缓存中是否已有该群的头像
  if (groupAvatarCache.value.has(group.id)) {
    const cachedAvatar = groupAvatarCache.value.get(group.id)
    return cachedAvatar || null
  }
  
  // 异步加载头像
  loadGroupAvatar(group.id)
  
  // 返回null让el-avatar显示fallback，直到头像加载完成
  return null
}

// 异步加载群头像
const loadGroupAvatar = async (groupId: number) => {
  try {
    const response = await fetch(`http://localhost:8080/api/group/${groupId}/avatar`, {
      headers: {
        'Authorization': `Bearer ${authStore.currentUser?.token || authStore.userInfo?.token || ''}`
      }
    })
    
    if (response.ok) {
      const blob = await response.blob()
      const reader = new FileReader()
      reader.onload = () => {
        const base64 = reader.result as string
        groupAvatarCache.value.set(groupId, base64)
      }
      reader.readAsDataURL(blob)
    } else {
      // 头像不存在或无权限访问，缓存空字符串避免重复请求
      groupAvatarCache.value.set(groupId, '')
    }
  } catch (error) {
    console.error('加载群头像失败:', error)
    groupAvatarCache.value.set(groupId, '')
  }
}

const getLastMessageText = (message: any) => {
  if (!message) return ''
  
  if (message.messageType === 2) {
    return '[图片]'
  } else if (message.messageType === 3) {
    return '[文件]'
  }
  
  return message.content?.length > 30 
    ? message.content.substring(0, 30) + '...' 
    : message.content || ''
}

const formatTime = (timestamp?: Date | string) => {
  if (!timestamp) return ''
  
  try {
    const now = dayjs()
    const messageTime = dayjs(timestamp)
    
    if (!messageTime.isValid()) {
      return ''
    }
    
    if (now.isSame(messageTime, 'day')) {
      return messageTime.format('HH:mm')
    } else if (now.subtract(1, 'day').isSame(messageTime, 'day')) {
      return '昨天'
    } else if (now.isSame(messageTime, 'year')) {
      return messageTime.format('MM/DD')
    } else {
      return messageTime.format('YYYY/MM/DD')
    }
  } catch (error) {
    console.error('时间格式化错误:', error)
    return ''
  }
}

// 生命周期
onMounted(() => {
  loadGroups()
  
  // 监听群组头像更新事件
  const handleGroupAvatarUpdated = (event: CustomEvent) => {
    const { groupId } = event.detail
    if (groupId && groupAvatarCache.value.has(groupId)) {
      groupAvatarCache.value.delete(groupId)
      console.log('清除群组头像缓存:', groupId)
    }
  }
  
  window.addEventListener('groupAvatarUpdated', handleGroupAvatarUpdated as EventListener)
  
  // 清理事件监听器
  onUnmounted(() => {
    window.removeEventListener('groupAvatarUpdated', handleGroupAvatarUpdated as EventListener)
  })
})

// 暴露方法给父组件
defineExpose({
  loadGroups,
  selectedGroup: computed(() => selectedGroup.value)
})
</script>

<style scoped>
.groups-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.search-section {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 20px;
  background: #f8f9fa;
  border: none;
  box-shadow: none;
}

.section-title {
  padding: 12px 16px 8px;
  color: #666;
  font-size: 12px;
  font-weight: 500;
  background: #fafafa;
}

.groups-list {
  flex: 1;
  overflow-y: auto;
}

.group-item {
  display: flex;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f8f8f8;
  transition: background-color 0.2s;
  gap: 12px;
}

.group-item:hover {
  background-color: #f8f9fa;
}

.group-item.active {
  background-color: #e6f4ff;
  border-right: 3px solid #1890ff;
}

.group-avatar {
  position: relative;
  flex-shrink: 0;
}

.unread-badge {
  position: absolute;
  top: -5px;
  right: -5px;
}

.group-content {
  flex: 1;
  min-width: 0;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.group-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-time {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
  margin-left: 8px;
}

.group-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.last-message {
  flex: 1;
  font-size: 13px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.no-message {
  color: #ccc;
}

.member-count {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
  margin-left: 8px;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
}

/* 滚动条样式 */
.groups-list::-webkit-scrollbar {
  width: 4px;
}

.groups-list::-webkit-scrollbar-track {
  background: transparent;
}

.groups-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.groups-list::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>