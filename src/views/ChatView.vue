<template>
  <div class="chat-container">
    <!-- 左侧导航栏 -->
    <div class="sidebar-nav">
      <div class="nav-header">
        <el-avatar :src="authStore.userAvatar" :size="40" class="user-avatar" />

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
      </div>

      <!-- 原来的联系人列表已被好友页面替代 -->
      <!--
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
      -->

      <!-- 联系人列表 -->
      <div v-else-if="activeTab === 'contacts'" class="contacts-list">
        <FriendsPage 
          @start-chat="startChatWithContact" 
          @friend-selected="handleFriendSelected" 
        />
      </div>

      <!-- 群组列表 -->
      <div v-else-if="activeTab === 'groups'" class="groups-list">
        <GroupsPage 
          @start-group-chat="startGroupChat" 
          @group-selected="handleGroupSelected" 
        />
      </div>
    </div>

    <!-- 右侧区域 -->
    <div class="chat-area">
      <!-- 联系人页面显示好友详情 -->
      <template v-if="activeTab === 'contacts'">
        <div class="contact-detail-panel">
          <div class="detail-header">
            <h3>好友详情</h3>
          </div>
          <div class="detail-content" v-if="selectedFriend">
            <div class="avatar-section">
              <el-avatar :src="getUserAvatar(selectedFriend)" :size="80" />
            </div>
            
            <div class="info-section">
              <div class="info-item">
                <label>用户名:</label>
                <span>{{ selectedFriend.username }}</span>
              </div>
              
              <div class="info-item">
                <label>昵称:</label>
                <span>{{ selectedFriend.nickname || '' }}</span>
              </div>
              
              <div class="info-item">
                <label>备注:</label>
                <div class="remark-container">
                  <span>{{ selectedFriend.remark || '' }}</span>
                  <el-icon class="edit-icon" @click="showEditRemarkDialog(selectedFriend)">
                    <Edit />
                  </el-icon>
                </div>
              </div>
              
              <div class="info-item">
                <label>性别:</label>
                <span>{{ selectedFriend.gender ? getGenderText(selectedFriend.gender) : '' }}</span>
              </div>
              
              <div class="info-item">
                <label>签名:</label>
                <span class="signature">{{ selectedFriend.signature || '' }}</span>
              </div>
            </div>
            
            <div class="action-buttons">
              <el-button 
                type="primary" 
                @click="startChatWithContact(selectedFriend)"
                icon="ChatDotRound"
              >
                开始聊天
              </el-button>
              <el-button 
                type="danger" 
                @click="showDeleteConfirm(selectedFriend)"
                icon="Delete"
              >
                删除好友
              </el-button>
            </div>
          </div>
          <div class="detail-content" v-else>
            <el-empty description="请选择好友查看详情" />
          </div>
        </div>
      </template>

      <!-- 群聊页面显示群聊详情 -->
      <template v-else-if="activeTab === 'groups'">
        <div class="group-detail-panel">
          <GroupDetailPanel 
            v-if="selectedGroup"
            :group="selectedGroup"
            @start-chat="startGroupChat"
          />
          <div v-else class="detail-content">
            <el-empty description="请选择群聊查看详情" />
          </div>
        </div>
      </template>
      
      <!-- 消息页面显示聊天区域 -->
      <template v-else-if="activeTab === 'messages'">
        <template v-if="chatStore.activeConversationId">
          <!-- 聊天界面 -->
          <ChatInterface 
            :messages="chatStore.activeMessages"
            :contact="currentContact"
            @send="handleSendMessage"
            @showContactInfo="showContactInfo = true"
          />
        </template>
        
        <!-- 空状态 -->
        <div v-else class="empty-state">
          <div class="empty-content">
            <el-icon size="64" color="#ddd"><ChatDotRound /></el-icon>
            <div class="empty-text">选择一个对话开始聊天</div>
          </div>
        </div>
      </template>
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

    <!-- 修改备注对话框 -->
    <el-dialog
      v-model="showEditRemarkDialogVisible"
      title="修改备注"
      width="400px"
      :before-close="handleDialogClose"
    >
      <el-form :model="remarkForm" label-width="80px">
        <el-form-item label="备注名称">
          <el-input 
            v-model="remarkForm.nickname" 
            placeholder="请输入备注名称"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showEditRemarkDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleUpdateRemark">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import ElMessage from 'element-plus/es/components/message/index.mjs'
import ElMessageBox from 'element-plus/es/components/message-box/index.mjs'
import { Edit } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { contactApi, groupApi } from '@/api'
import ChatInterface from '@/components/ChatInterface.vue'
import ContactSelector from '@/components/ContactSelector.vue'
import ContactInfo from '@/components/ContactInfo.vue'
import SettingsDialog from '@/components/SettingsDialog.vue'
import FriendsPage from '@/components/FriendsPage.vue'
import GroupsPage from '@/components/GroupsPage.vue'
import GroupDetailPanel from '@/components/GroupDetailPanel.vue'
import type { User, Conversation, Message, ChatGroup } from '@/types'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const chatStore = useChatStore()

const searchText = ref('')
const showContactSelector = ref(false)
const showContactInfo = ref(false)
const showSettings = ref(false)
const activeTab = ref('messages')
const selectedFriend = ref<User | null>(null)
const showEditRemarkDialogVisible = ref(false)
const remarkForm = ref({
  friendId: 0,
  nickname: ''
})

const currentContact = computed(() => {
  if (!chatStore.activeConversation) return null
  return chatStore.getConversationContact(chatStore.activeConversation)
})

const totalUnreadCount = computed(() => {
  return chatStore.sortedConversations.reduce((total: number, conv: Conversation) => total + conv.unreadCount, 0)
})

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

const getContactAvatar = (conversation: Conversation) => {
  const contact = chatStore.getConversationContact(conversation)
  // 使用getUserAvatar函数处理头像URL
  return getUserAvatar(contact)
}

const getContactStatus = (conversation: Conversation) => {
  const contact = chatStore.getConversationContact(conversation)
  return contact?.status || 'offline'
}

const getLastMessageText = (message: Message) => {
  if (message.messageType === 'text') {
    return message.content.length > 30 
      ? message.content.substring(0, 30) + '...' 
      : message.content
  } else if (message.messageType === 'image') {
    return '[图片]'
  } else if (message.messageType === 'file') {
    return '[文件]'
  }
  return message.content
}

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
  return conversation.timestamp
}

// 处理头像URL
const getUserAvatar = (user?: User) => {
  if (!user) return 'https://avatars.githubusercontent.com/u/0?v=4'
  
  // 如果用户有自定义头像
  if (user.avatar) {
    // 如果是本地上传的头像（以avatar_开头）
    if (user.avatar.startsWith('avatar_')) {
      return `http://localhost:8080/api/user/avatar/${user.id}`
    } else if (user.avatar.startsWith('/api/user/avatar/')) {
      // 如果已经是相对路径，转换为完整URL
      return `http://localhost:8080${user.avatar}`
    }
    // 其他情况直接使用avatar字段
    return user.avatar
  }
  
  // 默认头像
  return 'https://avatars.githubusercontent.com/u/0?v=4'
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
  
  console.log('选择联系人开始聊天:', contact)
  console.log('当前用户ID:', authStore.userInfo?.id)
  console.log('联系人ID:', contact.id)
  console.log('联系人ID类型:', typeof contact.id)
  
  // 开始与选中联系人的对话
  const conversationId = chatStore.startConversation(contact.id.toString())
  console.log('创建/找到对话ID:', conversationId)
  console.log('当前所有对话:', chatStore.conversations)
  
  if (conversationId) {
    chatStore.setActiveConversation(conversationId)
    console.log('设置活动对话:', conversationId)
    console.log('活动对话设置后:', chatStore.activeConversationId)
    activeTab.value = 'messages'
    console.log('切换到消息标签页')
  } else {
    console.error('无法创建或找到对话')
  }
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

// 添加好友页面需要的聊天启动方法
const startChatWithContact = async (contact: User) => {
  console.log('开始与好友聊天:', contact)
  
  // 检查用户登录状态和必要信息
  if (!authStore.isLoggedIn || !authStore.userInfo) {
    console.error('用户未登录或用户信息缺失')
    return
  }
  
  // 确保联系人列表是最新的
  await chatStore.loadContacts()
  console.log('联系人列表已更新')
  
  // 开始与选中联系人的对话
  const conversationId = chatStore.startConversation(contact.id.toString())
  console.log('创建/找到对话ID:', conversationId)
  
  if (conversationId) {
    // 设置活动对话
    await chatStore.setActiveConversation(conversationId)
    console.log('设置活动对话:', conversationId)
    
    // 切换到消息标签页显示聊天界面
    activeTab.value = 'messages'
    console.log('切换到消息标签页')
  } else {
    console.error('无法创建或找到对话')
  }
}

// 处理好友选中事件
const handleFriendSelected = (friend: User) => {
  selectedFriend.value = friend
  // 调试：输出好友数据查看包含的字段
  console.log('选中的好友数据:', friend)
  // 只设置选中状态，不自动开始聊天
}

// 群聊相关状态和方法
const selectedGroup = ref<ChatGroup | null>(null)

// 处理群聊选中事件
const handleGroupSelected = async (group: ChatGroup) => {
  try {
    // 调用后端接口获取完整的群组详情
    const response = await groupApi.getGroupDetail(group.id)
    // 检查响应数据格式 - 直接包含群组数据对象
    if (response.data && response.data.id) {
      // 将后端返回的 GroupDTO 转换为前端需要的 ChatGroup 格式
      const groupDetail = response.data
      const fullGroup: ChatGroup = {
        id: groupDetail.id,
        name: groupDetail.groupName,
        groupName: groupDetail.groupName, // 保存原始 groupName
        description: groupDetail.groupDescription,
        avatar: groupDetail.groupAvatar,
        ownerId: groupDetail.ownerId,
        ownerName: groupDetail.ownerName,
        maxMembers: groupDetail.maxMembers || 200,
        isPrivate: groupDetail.status === 2,
        memberCount: groupDetail.memberCount || 1,
        remark: groupDetail.myRemark || groupDetail.groupName,
        announcement: groupDetail.latestAnnouncement,
        createdAt: groupDetail.createTime ? new Date(groupDetail.createTime).toISOString() : new Date().toISOString(),
        updatedAt: groupDetail.createTime ? new Date(groupDetail.createTime).toISOString() : new Date().toISOString(),
        // 添加 myNickname 作为扩展属性，供 GroupDetailPanel 使用
        myNickname: groupDetail.myNickname
      }
      selectedGroup.value = fullGroup
      console.log('完整的群聊详情数据:', fullGroup)
    } else {
      console.error('获取群组详情失败:', response.data)
      console.error('响应状态码:', response.data.code)
      console.error('响应消息:', response.data.message)
      selectedGroup.value = group // 使用原始数据作为后备
    }
  } catch (error) {
    console.error('获取群组详情出错:', error)
    selectedGroup.value = group // 使用原始数据作为后备
  }
}

// 开始群聊
const startGroupChat = async (group: ChatGroup) => {
  console.log('开始群聊:', group)
  
  // 检查用户登录状态
  if (!authStore.isLoggedIn || !authStore.userInfo) {
    console.error('用户未登录或用户信息缺失')
    return
  }
  
  try {
    // 创建群聊会话ID（使用group_前缀区分群聊和私聊）
    const conversationId = `group_${group.id}`
    
    // 检查是否已存在该群聊会话
    let conversation = chatStore.conversations.find(c => c.id === conversationId)
    
    if (!conversation) {
      // 创建新的群聊会话
      const currentUserId = authStore.userInfo?.id?.toString()
      conversation = {
        id: conversationId,
        type: 'group',
        participantIds: currentUserId ? [currentUserId] : [], // 包含当前用户ID
        name: group.name || group.groupName || `群聊 ${group.id}`,
        avatar: group.avatar || group.groupAvatar || '',
        description: group.description || group.groupDescription || '',
        memberCount: group.memberCount || 1,
        ownerId: group.ownerId,
        lastMessage: undefined,
        unreadCount: 0,
        timestamp: new Date(),
        // 保存完整的群组信息，以便在界面上显示
        groupInfo: group
      }
      
      // 添加到会话列表
      chatStore.conversations.unshift(conversation) // 使用unshift将新会话添加到列表顶部
      chatStore.messages[conversationId] = [] // 初始化消息数组
    }
    
    // 设置为活动会话
    await chatStore.setActiveConversation(conversationId)
    console.log('设置活动群聊会话:', conversationId)
    
    // 切换到消息标签页
    activeTab.value = 'messages'
    console.log('切换到消息标签页显示群聊')
    
    // 加载群聊历史记录
    await chatStore.loadGroupChatHistory(group.id)
    
  } catch (error) {
    console.error('开始群聊失败:', error)
    ElMessage.error('开始群聊失败')
  }
}

// 显示删除好友确认对话框
const showDeleteConfirm = (friend: User) => {
  ElMessageBox.confirm(
    `确定要删除好友 "${friend.nickname || friend.username}" 吗？`,
    '删除好友',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      // 调用API删除好友
      await contactApi.removeContact(friend.id.toString())
      ElMessage.success('好友删除成功')
      selectedFriend.value = null
      
      // 重新加载好友列表
      if (activeTab.value === 'contacts') {
        // 发射自定义事件通知FriendsPage组件刷新好友列表
        const event = new CustomEvent('friend-deleted')
        window.dispatchEvent(event)
      }
    } catch (error: any) {
      ElMessage.error(error.message || '删除好友失败')
    }
  }).catch(() => {
    // 用户取消
  })
}

// 获取状态类型
const getStatusType = (status: number) => {
  switch (status) {
    case 1: return 'success'  // 在线
    case 2: return 'warning'  // 忙碌
    case 3: return 'info'     // 离开
    case 0: 
    default: return 'danger'  // 离线
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

// 获取性别文本
const getGenderText = (gender: number) => {
  switch (gender) {
    case 1: return '男'
    case 2: return '女'
    case 0:
    default: return '未知'
  }
}

// 显示修改备注对话框
const showEditRemarkDialog = (friend: User) => {
  remarkForm.value = {
    friendId: friend.id,
    nickname: friend.remark || friend.nickname || ''
  }
  showEditRemarkDialogVisible.value = true
}

// 处理对话框关闭
const handleDialogClose = (done: () => void) => {
  remarkForm.value = {
    friendId: 0,
    nickname: ''
  }
  done()
}

// 更新备注
const handleUpdateRemark = async () => {
  if (!remarkForm.value.nickname.trim()) {
    ElMessage.warning('请输入备注名称')
    return
  }

  try {
    // 使用配置好的axios实例调用API更新备注
    await contactApi.updateFriendNickname(
      remarkForm.value.friendId.toString(),
      remarkForm.value.nickname.trim()
    )

    ElMessage.success('备注修改成功')
    showEditRemarkDialogVisible.value = false
    
    // 更新本地数据
    if (selectedFriend.value && selectedFriend.value.id === remarkForm.value.friendId) {
      selectedFriend.value.remark = remarkForm.value.nickname.trim()
    }
    
    // 重新加载好友列表
    if (activeTab.value === 'contacts') {
      // 这里需要触发好友列表重新加载
      // 可以通过事件总线或其他方式通知FriendsPage组件刷新
    }
  } catch (error: any) {
    ElMessage.error(error.message || '修改备注失败')
  }
}

// 初始化用户数据
onMounted(async () => {
  const currentUser = authStore.userInfo
  if (currentUser && currentUser.id) {
    await chatStore.initializeUserData(currentUser.id)
  }
  
  // 处理路由参数
  // 检查是否有路由参数指定聊天类型和ID
  const chatType = route.params.type as string
  const chatId = route.params.id as string
  
  if (chatType && chatId) {
    console.log('路由参数:', { chatType, chatId })
    
    // 根据类型处理不同的聊天启动方式
    if (chatType === 'group') {
      // 群聊类型
      const groupId = parseInt(chatId)
      if (!isNaN(groupId)) {
        console.log('从路由启动群聊:', groupId)
        // 获取群组信息
        try {
          const response = await groupApi.getGroupDetail(groupId)
          if (response.data && response.data.code === 200 && response.data.data) {
            const group = response.data.data
            // 启动群聊
            startGroupChat({
              id: group.id,
              name: group.groupName,
              groupName: group.groupName,
              description: group.groupDescription,
              avatar: group.groupAvatar,
              ownerId: group.ownerId,
              maxMembers: group.maxMembers || 200,
              memberCount: group.memberCount || 1,
              isPrivate: group.status === 2
            })
          }
        } catch (error) {
          console.error('获取群组信息失败:', error)
        }
      }
    } else if (chatType === 'friend') {
      // 好友聊天类型
      const friendId = chatId
      const friendName = route.query.friendName as string
      
      // 模拟好友对象
      const friend: User = {
        id: parseInt(friendId), // 这里保持数字类型，handleSelectContact会转换为字符串
        username: friendName || '好友',
        nickname: friendName || '好友',
        status: 1 // 在线状态
      }
      
      // 延迟执行以确保聊天存储已初始化
      setTimeout(() => {
        handleSelectContact(friend)
        // 切换到消息标签页
        activeTab.value = 'messages'
      }, 100)
    }
  } else {
    // 处理旧版路由参数，从好友详情跳转时自动开始聊天
    const friendId = route.query.friendId as string
    const friendName = route.query.friendName as string
    
    if (friendId) {
      // 模拟好友对象
      const friend: User = {
        id: parseInt(friendId), // 这里保持数字类型，handleSelectContact会转换为字符串
        username: friendName || '好友',
        nickname: friendName || '好友',
        status: 1 // 在线状态
      }
      
      // 延迟执行以确保聊天存储已初始化
      setTimeout(() => {
        handleSelectContact(friend)
        // 切换到消息标签页
        activeTab.value = 'messages'
      }, 100)
    }
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

/* 好友详情面板样式 */
.contact-detail-panel {
  padding: 40px 20px 20px 20px;
  background: white;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.detail-header {
  text-align: center;
  margin-bottom: 30px;
}

.detail-header h3 {
  margin: 0;
  color: #333;
  font-size: 20px;
  font-weight: 600;
}

.detail-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
}

.avatar-section {
  margin-bottom: 24px;
}

.info-section {
  width: 100%;
  max-width: 300px;
  margin-bottom: 32px;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.info-item label {
  width: 80px;
  color: #666;
  font-weight: 500;
  flex-shrink: 0;
  text-align: right;
}

.info-item span {
  flex: 1;
  color: #333;
}

.remark-container {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.edit-icon {
  color: #1890ff;
  cursor: pointer;
  font-size: 16px;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.edit-icon:hover {
  opacity: 1;
}

.signature {
  color: #999;
  font-style: italic;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 20px;
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