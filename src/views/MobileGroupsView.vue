<template>
  <div class="mobile-groups-view">
    <!-- 顶部标题栏 -->
    <div class="header">
      <h2>群聊</h2>
      <el-button 
        type="primary" 
        @click="showCreateGroupDialog = true"
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
        placeholder="搜索群聊"
        prefix-icon="Search"
        clearable
        class="search-input"
      />
    </div>
    
    <!-- 群聊列表 -->
    <div class="groups-list">
      <div class="section-title">我的群聊</div>
      <div
        v-for="group in displayGroups"
        :key="group.id"
        class="group-item"
        @click="handleGroupClick(group)"
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
      <div v-if="displayGroups.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无群聊" />
      </div>
      <div v-else-if="loading" class="loading-state">
        <el-skeleton :rows="5" animated />
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { groupApi } from '@/api'
import CreateGroupDialog from '@/components/groups/dialogs/CreateGroupDialog.vue'
import type { ChatGroup } from '@/types'
import dayjs from 'dayjs'

const router = useRouter()
const authStore = useAuthStore()
const searchText = ref('')
const showCreateGroupDialog = ref(false)
const loading = ref(true)
let refreshInterval: number | null = null

// 群组列表（模拟与电脑端GroupsPage.vue相似的数据结构）
const groups = ref<ChatGroup[]>([])

// 显示的群聊列表
const displayGroups = computed(() => {
  if (!searchText.value) return groups.value
  
  return groups.value.filter(group => 
    (group.remark || group.name).toLowerCase().includes(searchText.value.toLowerCase()) ||
    group.description?.toLowerCase().includes(searchText.value.toLowerCase())
  )
})

// 加载群聊列表（使用与电脑端GroupsPage.vue相同的API调用方式）
const loadGroups = async () => {
  try {
    loading.value = true
    const response = await groupApi.getUserGroups()
    
    // 检查响应结构（与电脑端保持一致）
    if (response.data?.code === 200 && Array.isArray(response.data?.data)) {
      // 将后端返回的 GroupDTO 转换为前端需要的 ChatGroup 格式（与电脑端保持一致）
      groups.value = response.data.data.map((groupDTO: any) => ({
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
    } else if (Array.isArray(response.data)) {
      // 兼容另一种响应格式
      groups.value = response.data.map((groupDTO: any) => ({
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
    } else {
      groups.value = []
    }
  } catch (error: any) {
    console.error('加载群聊列表失败:', error)
    ElMessage.error(error.message || '加载群聊列表失败')
    groups.value = [] // 确保在出错时清空列表
  } finally {
    loading.value = false
  }
}

// 处理群聊点击
const handleGroupClick = (group: ChatGroup) => {
  // 跳转到群聊详情页面
  router.push(`/mobile/group/${group.id}`)
}

// 处理群聊创建
const handleGroupCreated = (groupDTO: any) => {
  // 将后端返回的 GroupDTO 转换为前端需要的 ChatGroup 格式（与电脑端保持一致）
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
  ElMessage.success('群聊创建成功')
  
  // 跳转到新创建的群聊
  router.push(`/mobile/group/${newGroup.id}`)
}

// 存储已加载的群头像
const groupAvatarCache = ref<Map<number, string>>(new Map())

// 获取群头像
const getGroupAvatar = (group: ChatGroup) => {
  // 检查多个可能的头像字段
  const avatar = group.avatar // 移除了不存在的groupAvatar字段
  
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

// 获取最后消息文本
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

// 格式化时间
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

// 定期刷新数据的函数
const refreshData = async () => {
  await loadGroups()
}

// 初始化数据
onMounted(async () => {
  await loadGroups()
  
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
.mobile-groups-view {
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
  background: white;
}

.group-item:hover {
  background-color: #f8f9fa;
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

.empty-state, .loading-state {
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