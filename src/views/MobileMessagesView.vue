<template>
  <div class="mobile-messages-view">
    <!-- 顶部标题栏 -->
    <div class="header">
      <h2>消息</h2>
    </div>
    
    <!-- 搜索栏 -->
    <div class="search-section">
      <el-input
        v-model="searchText"
        placeholder="搜索"
        prefix-icon="Search"
        clearable
        class="search-input"
      />
    </div>
    
    <!-- 消息列表 -->
    <div class="conversation-list">
      <div
        v-for="conversation in displayConversations"
        :key="conversation.id"
        class="conversation-item"
        :class="{ active: conversation.id === chatStore.activeConversationId }"
        @click="handleSelectConversation(conversation)"
      >
        <div class="conversation-avatar">
          <el-avatar :src="getContactAvatar(conversation)" :size="48" />
          <div v-if="getContactStatus(conversation) === 'online'" class="status-dot online"></div>
          <el-badge
            v-if="conversation.unreadCount > 0"
            :value="conversation.unreadCount"
            :max="99"
            class="unread-badge"
          />
        </div>
        
        <div class="conversation-content">
          <div class="conversation-header">
            <div class="conversation-name">
              {{ getContactName(conversation) }}
              <el-icon v-if="conversation.id?.startsWith('group_')" class="group-icon" title="群聊">
                <UserFilled />
              </el-icon>
            </div>
            <div class="conversation-time">{{ formatTime(getConversationLastMessageTime(conversation)) }}</div>
          </div>
          
          <div class="conversation-footer">
            <div class="last-message">
              <span v-if="conversation.lastMessage">
                {{ getLastMessageText(conversation.lastMessage) }}
              </span>
              <span v-else-if="hasMessages(conversation)" class="no-message">
                {{ getLastMessageTextFromStore(conversation) }}
              </span>
              <span v-else class="no-message">暂无消息</span>
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="displayConversations.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无消息" />
      </div>
      <div v-else-if="loading" class="loading-state">
        <el-skeleton :rows="5" animated />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import type { Conversation, Message } from '@/types'
import { getUserAvatarUrl } from '@/utils/avatar'
import dayjs from 'dayjs'
import { UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const chatStore = useChatStore()
const searchText = ref('')
const loading = ref(true)
let refreshInterval: number | null = null

// 显示的会话列表（使用chatStore.sortedConversations，与电脑端保持一致）
const displayConversations = computed(() => {
  // 如果有搜索文本，进行过滤
  if (searchText.value) {
    const keyword = searchText.value.toLowerCase()
    return chatStore.sortedConversations.filter((conversation: Conversation) => {
      const contact = chatStore.getConversationContact(conversation)
      const name = contact?.name || contact?.remark || contact?.nickname || contact?.username || ''
      return name.toLowerCase().includes(keyword)
    })
  }
  
  // 否则显示排序后的会话列表
  return chatStore.sortedConversations
})

// 获取联系人名称
const getContactName = (conversation: Conversation) => {
  const contact = chatStore.getConversationContact(conversation)
  
  // 检查是否是群聊会话
  if (conversation.id?.startsWith('group_')) {
    // 群聊显示名称优先级：群名称 > 群ID
    return contact?.name || contact?.groupName || `群聊 ${contact?.id}` || '未知群聊'
  }
  
  // 私聊显示名称优先级：备注 > 昵称 > 用户名
  return contact?.name || contact?.remark || contact?.nickname || contact?.username || '未知联系人'
}

// 获取联系人头像
const getContactAvatar = (conversation: Conversation) => {
  const contact = chatStore.getConversationContact(conversation)
  return getUserAvatarUrl(contact)
}

// 获取联系人状态
const getContactStatus = (conversation: Conversation) => {
  const contact = chatStore.getConversationContact(conversation)
  return contact?.status || 'offline'
}

// 获取最后一条消息文本
const getLastMessageText = (message: Message) => {
  if (!message) return '暂无消息'
  
  if (message.messageType === 'text') {
    return message.content.length > 30 
      ? message.content.substring(0, 30) + '...' 
      : message.content
  } else if (message.messageType === 'image') {
    return '[图片]'
  } else if (message.messageType === 'file') {
    return '[文件]'
  }
  return message.content || '暂无消息'
}

// 格式化时间
const formatTime = (timestamp: Date) => {
  try {
    const now = dayjs()
    const messageTime = dayjs(timestamp)
    
    if (!messageTime.isValid()) {
      console.warn('无效的时间戳:', timestamp)
      return '未知时间'
    }
    
    if (now.isSame(messageTime, 'day')) {
      return messageTime.format('HH:mm')
    } else if (now.subtract(1, 'day').isSame(messageTime, 'day')) {
      return '昨天 ' + messageTime.format('HH:mm')
    } else if (now.isSame(messageTime, 'year')) {
      return messageTime.format('MM/DD HH:mm')
    } else {
      return messageTime.format('YYYY/MM/DD HH:mm')
    }
  } catch (error) {
    console.error('时间格式化错误:', error, '时间戳:', timestamp)
    return '时间错误'
  }
}

// 检查会话是否有消息
const hasMessages = (conversation: Conversation) => {
  const messages = chatStore.messages[conversation.id]
  return messages && messages.length > 0
}

// 从store获取会话的最后一条消息
const getLastMessageTextFromStore = (conversation: Conversation) => {
  const messages = chatStore.messages[conversation.id]
  if (messages && messages.length > 0) {
    const lastMessage = messages[messages.length - 1]
    return getLastMessageText(lastMessage)
  }
  return '暂无消息'
}

// 获取会话的最后消息时间
const getConversationLastMessageTime = (conversation: Conversation) => {
  const messages = chatStore.messages[conversation.id]
  if (messages && messages.length > 0) {
    // 获取会话中的最后一条消息（最新的消息）
    const lastMessage = messages[messages.length - 1]
    // 确保返回Date对象而不是字符串
    const timeString = lastMessage.sendTime || lastMessage.createTime || lastMessage.timestamp
    return new Date(timeString)
  }
  return conversation.timestamp || new Date()
}

// 处理选择会话
const handleSelectConversation = (conversation: Conversation) => {
  // 设置活动会话
  chatStore.setActiveConversation(conversation.id)
  
  // 跳转到聊天页面
  if (conversation.id?.startsWith('group_')) {
    // 群聊
    const groupId = conversation.id.replace('group_', '')
    router.push(`/mobile/chat/group/${groupId}`)
  } else {
    // 私聊
    router.push(`/mobile/chat/user/${conversation.id}`)
  }
}

// 定期刷新数据的函数
const refreshData = async () => {
  try {
    // 强制刷新聊天数据
    const currentUser = chatStore.currentUser?.id
    if (currentUser) {
      await chatStore.initializeUserData(currentUser.toString())
    }
  } catch (error) {
    console.error('刷新数据失败:', error)
  }
}

// 初始化数据
onMounted(async () => {
  loading.value = true
  
  try {
    // 确保聊天数据已加载
    const currentUser = chatStore.currentUser?.id
    if (currentUser) {
      await chatStore.initializeUserData(currentUser.toString())
    }
  } catch (error) {
    console.error('初始化数据失败:', error)
  } finally {
    loading.value = false
  }
  
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
.mobile-messages-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.header {
  padding: 16px;
  background: linear-gradient(180deg, #4A90E2 0%, #357ABD 100%);
  color: white;
  text-align: center;
}

.header h2 {
  margin: 0;
  font-size: 18px;
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

.conversation-list {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f8f8f8;
  transition: background-color 0.2s;
  gap: 12px;
  background: white;
}

.conversation-item:hover {
  background-color: #f8f9fa;
}

.conversation-item.active {
  background-color: #e6f4ff;
}

.conversation-avatar {
  position: relative;
  flex-shrink: 0;
}

.status-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid white;
}

.status-dot.online {
  background-color: #52c41a;
}

.conversation-content {
  flex: 1;
  min-width: 0;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.conversation-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 4px;
}

.group-icon {
  color: #1890ff;
  font-size: 12px;
}

.conversation-time {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
  margin-left: 8px;
}

.conversation-footer {
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

.empty-state, .loading-state {
  padding: 40px 20px;
  text-align: center;
}

/* 滚动条样式 */
.conversation-list::-webkit-scrollbar {
  width: 4px;
}

.conversation-list::-webkit-scrollbar-track {
  background: transparent;
}

.conversation-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.conversation-list::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>