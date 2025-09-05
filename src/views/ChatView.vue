<template>
  <div class="chat-container">
    <!-- 左侧导航栏 -->
    <div class="sidebar-nav">
      <div class="nav-header">
        <el-avatar :src="authStore.userAvatar" :size="40" class="user-avatar" />
        <div class="unread-badge" v-if="totalUnreadCount > 0">
          {{ totalUnreadCount > 99 ? '99+' : totalUnreadCount }}
        </div>
      </div>
      
      <div class="nav-items">
        <div class="nav-item" :class="{ active: activeTab === 'messages' }" @click="activeTab = 'messages'" title="消息">
          <el-icon><ChatDotRound /></el-icon>
        </div>
        <div class="nav-item" :class="{ active: activeTab === 'contacts' }" @click="activeTab = 'contacts'" title="联系人">
          <el-icon><User /></el-icon>
        </div>
        <div class="nav-item" :class="{ active: activeTab === 'groups' }" @click="activeTab = 'groups'" title="群组">
          <el-icon><UserFilled /></el-icon>
        </div>
      </div>
      
      <div class="nav-footer">
        <div class="nav-item" @click="showSettings = true" title="设置">
          <el-icon><Setting /></el-icon>
        </div>
        <div class="nav-item" @click="handleLogout" title="退出">
          <el-icon><SwitchButton /></el-icon>
        </div>
      </div>
    </div>

    <!-- 中间会话/联系人列表 -->
    <div class="conversation-panel">
      <!-- 搜索栏 -->
      <div class="search-section">
        <el-input
          v-model="searchText"
          placeholder="搜索"
          prefix-icon="Search"
          clearable
          class="search-input"
        />
        <el-button 
          v-if="activeTab === 'contacts'"
          @click="showContactSelector = true" 
          type="primary" 
          circle
          size="small"
          title="添加联系人"
        >
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>

      <!-- 消息列表 -->
      <div v-if="activeTab === 'messages'" class="conversation-list">
        <div class="section-title">最近聊天</div>
        <div
          v-for="conversation in chatStore.sortedConversations"
          :key="conversation.id"
          class="conversation-item"
          :class="{ active: conversation.id === chatStore.activeConversationId }"
          @click="handleSelectConversation(conversation.id)"
        >
          <div class="conversation-avatar">
            <el-avatar :src="getContactAvatar(conversation)" :size="48" />
            <div v-if="getContactStatus(conversation) === 'online'" class="status-dot online"></div>
          </div>
          
          <div class="conversation-content">
            <div class="conversation-header">
              <div class="conversation-name">{{ getContactName(conversation) }}</div>
              <div class="conversation-time">{{ formatTime(conversation.timestamp) }}</div>
            </div>
            
            <div class="conversation-footer">
              <div class="last-message">
                <span v-if="conversation.lastMessage">
                  {{ getLastMessageText(conversation.lastMessage) }}
                </span>
                <span v-else class="no-message">暂无消息</span>
              </div>
              
              <el-badge
                v-if="conversation.unreadCount > 0"
                :value="conversation.unreadCount"
                :max="99"
                class="unread-badge"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 联系人列表 -->
      <div v-else-if="activeTab === 'contacts'" class="contacts-list">
        <div class="section-title">在线</div>
        <div
          v-for="contact in onlineContacts"
          :key="contact.id"
          class="contact-item"
          @click="startChatWithContact(contact)"
        >
          <el-avatar :src="contact.avatar" :size="40" />
          <div class="contact-info">
            <div class="contact-name">{{ contact.name }}</div>
            <div class="contact-status online">在线</div>
          </div>
          <div class="contact-actions">
            <el-icon class="chat-icon"><ChatDotRound /></el-icon>
          </div>
        </div>
        
        <div class="section-title" style="margin-top: 20px;">离线</div>
        <div
          v-for="contact in offlineContacts"
          :key="contact.id"
          class="contact-item offline"
          @click="startChatWithContact(contact)"
        >
          <el-avatar :src="contact.avatar" :size="40" />
          <div class="contact-info">
            <div class="contact-name">{{ contact.name }}</div>
            <div class="contact-status offline">离线</div>
          </div>
          <div class="contact-actions">
            <el-icon class="chat-icon"><ChatDotRound /></el-icon>
          </div>
        </div>
      </div>

      <!-- 群组列表 -->
      <div v-else-if="activeTab === 'groups'" class="groups-list">
        <div class="section-title">我的群组</div>
        <div class="group-item">
          <el-avatar :size="40" class="group-avatar">群</el-avatar>
          <div class="group-info">
            <div class="group-name">示例群组</div>
            <div class="group-member-count">5人</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-area">
      <template v-if="chatStore.activeConversationId">
        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="contact-info">
            <el-avatar :src="currentContact?.avatar" :size="36" />
            <div class="contact-details">
              <div class="contact-name">{{ currentContact?.name }}</div>
              <div class="contact-status">{{ currentContact?.status === 'online' ? '在线' : '离线' }}</div>
            </div>
          </div>
          
          <div class="chat-actions">
            <el-button text @click="showContactInfo = true">
              <el-icon><More /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- 消息列表 -->
        <MessageList :messages="chatStore.activeMessages" />

        <!-- 消息输入框 -->
        <MessageInput @send="handleSendMessage" />
      </template>
      
      <!-- 空状态 -->
      <div v-else class="empty-state">
        <div class="empty-content">
          <el-icon size="64" color="#ddd"><ChatDotRound /></el-icon>
          <div class="empty-text">选择一个对话开始聊天</div>
        </div>
      </div>
    </div>

    <!-- 联系人信息弹窗 -->
    <ContactInfo 
      v-model="showContactInfo"
      :contact="currentContact"
    />

    <!-- 设置弹窗 -->
    <SettingsDialog v-model="showSettings" />

    <!-- 联系人选择器 -->
    <ContactSelector 
      v-model="showContactSelector"
      @select="handleSelectContact"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import MessageList from '@/components/MessageList.vue'
import MessageInput from '@/components/MessageInput.vue'
import ContactSelector from '@/components/ContactSelector.vue'
import ContactInfo from '@/components/ContactInfo.vue'
import SettingsDialog from '@/components/SettingsDialog.vue'
import type { User, Conversation, Message } from '@/types'
import dayjs from 'dayjs'

const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()

const searchText = ref('')
const showContactSelector = ref(false)
const showContactInfo = ref(false)
const showSettings = ref(false)
const activeTab = ref('messages')

const currentContact = computed(() => {
  if (!chatStore.activeConversation) return null
  return chatStore.getConversationContact(chatStore.activeConversation)
})

const totalUnreadCount = computed(() => {
  return chatStore.sortedConversations.reduce((total, conv) => total + conv.unreadCount, 0)
})

const onlineContacts = computed(() => {
  return chatStore.availableContacts.filter(contact => contact.status === 'online')
})

const offlineContacts = computed(() => {
  return chatStore.availableContacts.filter(contact => contact.status !== 'online')
})

const getContactName = (conversation: Conversation) => {
  const contact = chatStore.getConversationContact(conversation)
  return contact?.name || '未知联系人'
}

const getContactAvatar = (conversation: Conversation) => {
  const contact = chatStore.getConversationContact(conversation)
  return contact?.avatar || 'https://avatars.githubusercontent.com/u/0?v=4'
}

const getContactStatus = (conversation: Conversation) => {
  const contact = chatStore.getConversationContact(conversation)
  return contact?.status || 'offline'
}

const getLastMessageText = (message: Message) => {
  if (message.type === 'text') {
    return message.content.length > 30 
      ? message.content.substring(0, 30) + '...' 
      : message.content
  } else if (message.type === 'image') {
    return '[图片]'
  } else if (message.type === 'file') {
    return '[文件]'
  }
  return message.content
}

const formatTime = (timestamp: Date) => {
  const now = dayjs()
  const messageTime = dayjs(timestamp)
  
  if (now.isSame(messageTime, 'day')) {
    return messageTime.format('HH:mm')
  } else if (now.diff(messageTime, 'day') === 1) {
    return '昨天'
  } else if (now.isSame(messageTime, 'week')) {
    return messageTime.format('dddd')
  } else {
    return messageTime.format('MM/DD')
  }
}

const handleSelectConversation = (conversationId: string) => {
  chatStore.setActiveConversation(conversationId)
}

const handleSendMessage = (content: string) => {
  chatStore.sendMessage(content)
}

const handleSelectContact = (contact: User) => {
  // 检查用户登录状态和必要信息
  if (!authStore.isLoggedIn || !authStore.userInfo) {
    console.error('用户未登录或用户信息缺失')
    return
  }
  
  console.log('选择联系人:', contact)
  
  // 开始与选中联系人的对话
  const conversationId = chatStore.startConversation(contact.id)
  console.log('创建/找到对话ID:', conversationId)
  
  if (conversationId) {
    chatStore.setActiveConversation(conversationId)
    console.log('设置活动对话:', conversationId)
    activeTab.value = 'messages'
  } else {
    console.error('无法创建或找到对话')
  }
}

const startChatWithContact = (contact: User) => {
  handleSelectContact(contact)
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出当前账号吗？',
      '退出登录',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    authStore.logout()
    chatStore.clearData()
    ElMessage.success('已退出登录')
    await router.push('/login')
  } catch {
    // 用户取消
  }
}

// 初始化用户数据
onMounted(() => {
  const currentUser = authStore.userInfo
  if (currentUser && currentUser.id) {
    chatStore.initializeUserData(currentUser.id)
  }
})
</script>

<style scoped>
.chat-container {
  display: flex;
  height: 100vh;
  background-color: #f5f5f5;
}

/* 左侧导航栏 */
.sidebar-nav {
  width: 60px;
  background: linear-gradient(180deg, #4A90E2 0%, #357ABD 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
  box-shadow: 2px 0 4px rgba(0, 0, 0, 0.1);
}

.nav-header {
  position: relative;
  margin-bottom: 30px;
}

.user-avatar {
  cursor: pointer;
  border: 2px solid rgba(255, 255, 255, 0.3);
  transition: border-color 0.2s;
}

.user-avatar:hover {
  border-color: rgba(255, 255, 255, 0.6);
}

.unread-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #ff4757;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 12px;
  font-weight: bold;
  min-width: 18px;
  text-align: center;
}

.nav-items {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.nav-item {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 20px;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.nav-item.active {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.nav-footer {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 20px;
}

/* 中间会话列表 */
.conversation-panel {
  width: 320px;
  background: white;
  border-right: 1px solid #e8e8e8;
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
}

.conversation-item:hover {
  background-color: #f8f9fa;
}

.conversation-item.active {
  background-color: #e6f4ff;
  border-right: 3px solid #1890ff;
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

/* 联系人列表 */
.contacts-list {
  flex: 1;
  overflow-y: auto;
}

.contact-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  gap: 12px;
}

.contact-item:hover {
  background-color: #f8f9fa;
}

.contact-item.offline {
  opacity: 0.6;
}

.contact-info {
  flex: 1;
}

.contact-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  margin-bottom: 2px;
}

.contact-status {
  font-size: 12px;
}

.contact-status.online {
  color: #52c41a;
}

.contact-status.offline {
  color: #999;
}

.contact-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.contact-item:hover .contact-actions {
  opacity: 1;
}

.chat-icon {
  font-size: 16px;
  color: #1890ff;
  cursor: pointer;
}

/* 群组列表 */
.groups-list {
  flex: 1;
  overflow-y: auto;
}

.group-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  gap: 12px;
}

.group-item:hover {
  background-color: #f8f9fa;
}

.group-avatar {
  background: linear-gradient(45deg, #1890ff, #36cfc9);
  color: white;
  font-weight: bold;
}

.group-info {
  flex: 1;
}

.group-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  margin-bottom: 2px;
}

.group-member-count {
  font-size: 12px;
  color: #999;
}

/* 右侧聊天区域 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
}

.chat-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
}

.contact-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.contact-details {
  flex: 1;
}

.contact-name {
  font-weight: 500;
  font-size: 16px;
  color: #333;
  margin-bottom: 2px;
}

.contact-status {
  font-size: 12px;
  color: #999;
}

.chat-actions {
  display: flex;
  gap: 8px;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

.empty-content {
  text-align: center;
}

.empty-text {
  margin-top: 16px;
  color: #999;
  font-size: 16px;
}

/* 滚动条样式 */
.conversation-list::-webkit-scrollbar,
.contacts-list::-webkit-scrollbar,
.groups-list::-webkit-scrollbar {
  width: 4px;
}

.conversation-list::-webkit-scrollbar-track,
.contacts-list::-webkit-scrollbar-track,
.groups-list::-webkit-scrollbar-track {
  background: transparent;
}

.conversation-list::-webkit-scrollbar-thumb,
.contacts-list::-webkit-scrollbar-thumb,
.groups-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.conversation-list::-webkit-scrollbar-thumb:hover,
.contacts-list::-webkit-scrollbar-thumb:hover,
.groups-list::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-container {
    flex-direction: column;
  }
  
  .sidebar-nav {
    width: 100%;
    height: 60px;
    flex-direction: row;
    padding: 0 16px;
  }
  
  .conversation-panel {
    width: 100%;
    height: 40vh;
  }
  
  .chat-area {
    height: calc(60vh - 60px);
  }
}
</style>