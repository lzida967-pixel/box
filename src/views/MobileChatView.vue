<template>
  <div class="mobile-chat-view">
    <!-- 顶部标题栏 -->
    <div class="header">
      <el-icon class="back-icon" @click="goBack"><ArrowLeft /></el-icon>
      <div class="header-content" v-if="contact">
        <div class="contact-name">{{ contactName }}</div>
        <div class="contact-status" :class="contactStatusType">{{ contactStatusText }}</div>
      </div>
      <div class="header-content" v-else>
        <div class="contact-name">聊天</div>
      </div>
      <el-icon class="menu-icon" @click="showMenu = !showMenu"><More /></el-icon>
    </div>
    
    <!-- 菜单下拉 -->
    <div v-show="showMenu" class="menu-dropdown">
      <div class="menu-item" @click="clearChatHistory">清空聊天记录</div>
      <div class="menu-item" @click="showContactInfo">联系人信息</div>
    </div>
    
    <!-- 消息列表 -->
    <MessageList 
      :messages="currentMessages"
      :contact="contact"
      class="message-list"
    />
    
    <!-- 消息输入 -->
    <MessageInput 
      @send="handleSendMessage"
      @send-image="handleSendImages"
      class="message-input"
    />
    
    <!-- 联系人信息弹窗 -->
    <ContactInfo 
      v-model="showContactInfoDialog"
      :contact="contact"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import ElMessage from 'element-plus/es/components/message/index.mjs'
import ElMessageBox from 'element-plus/es/components/message-box/index.mjs'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { chatApi } from '@/api'
import MessageList from '@/components/MessageList.vue'
import MessageInput from '@/components/MessageInput.vue'
import ContactInfo from '@/components/ContactInfo.vue'
import type { User, ChatGroup, Message } from '@/types'
import { ArrowLeft, More } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const chatStore = useChatStore()
const authStore = useAuthStore()
const showMenu = ref(false)
const showContactInfoDialog = ref(false)

// 联系人信息
const contact = computed(() => {
  if (!chatStore.activeConversation) return null
  return chatStore.getConversationContact(chatStore.activeConversation)
})

// 联系人名称
const contactName = computed(() => {
  if (!contact.value) return '未知联系人'
  
  // 检查是否是群聊会话
  if (chatStore.activeConversation?.id?.startsWith('group_')) {
    // 群聊显示名称优先级：群名称 > 群ID
    return contact.value.name || contact.value.groupName || `群聊 ${contact.value.id}` || '未知群聊'
  }
  
  // 私聊显示名称优先级：备注 > 昵称 > 用户名
  return contact.value.name || contact.value.remark || contact.value.nickname || contact.value.username || '未知联系人'
})

// 联系人状态类型
const contactStatusType = computed(() => {
  if (!contact.value) return 'offline'
  const status = contact.value.status || 0
  switch (status) {
    case 1: return 'online'
    case 2: return 'busy'
    case 3: return 'away'
    default: return 'offline'
  }
})

// 联系人状态文本
const contactStatusText = computed(() => {
  if (!contact.value) return '离线'
  const status = contact.value.status || 0
  switch (status) {
    case 1: return '在线'
    case 2: return '忙碌'
    case 3: return '离开'
    default: return '离线'
  }
})

// 当前消息列表
const currentMessages = computed(() => {
  return chatStore.activeMessages
})

// 返回上一页
const goBack = () => {
  router.back()
}

// 处理发送消息
const handleSendMessage = (content: string) => {
  if (!content.trim()) {
    ElMessage.warning('不能发送空消息')
    return
  }
  chatStore.sendWebSocketMessage(content)
}

// 处理发送图片（来自 MessageInput）
const handleSendImages = async (files: File[]) => {
  if (!chatStore.activeConversationId || !files?.length) return
  const conv = chatStore.activeConversation
  const me = authStore.userInfo?.id
  if (!conv || !me) return

  try {
    for (const file of files) {
      if (conv.type === 'group') {
        await handleGroupImageSend(file)
      } else {
        await handlePrivateImageSend(file, me)
      }
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('图片发送失败')
  }
}

// 私聊图片发送（与 Web 端保持一致）
const handlePrivateImageSend = async (file: File, me: number) => {
  const friendIdStr = chatStore.activeConversation?.participantIds.find((id: string) => id !== String(me))
  if (!friendIdStr) return
  const friendId = parseInt(friendIdStr)

  const tempId = Date.now() + Math.random()
  const optimistic: Message = {
    id: tempId as any,
    fromUserId: me,
    toUserId: friendId,
    content: '__uploading__',
    messageType: 'image',
    status: 'sending',
    sendTime: new Date().toISOString(),
    createTime: new Date().toISOString(),
    updateTime: new Date().toISOString(),
    senderId: me,
    receiverId: friendId
  }
  if (!chatStore.messages[chatStore.activeConversationId!]) {
    ;(chatStore as any).messages[chatStore.activeConversationId!] = []
  }
  ;(chatStore as any).messages[chatStore.activeConversationId!].push(optimistic)

  try {
    const resp = await chatApi.sendImage(friendId, file)
    if ((resp as any).code === 200 && (resp as any).data) {
      const serverMsg = (resp as any).data as any
      const imageContent = serverMsg.content ?? serverMsg.imageId ?? serverMsg.data?.imageId ?? serverMsg.data?.filename ?? file.name
      const list = (chatStore as any).messages[chatStore.activeConversationId!] as Message[]
      const found = list.find(m => m.id === tempId)
      if (found) {
        found.id = serverMsg.id ?? found.id
        found.status = 'delivered'
        found.messageType = 'image'
        found.content = String(imageContent)
        found.updateTime = new Date().toISOString()
      }
    } else {
      throw new Error('私聊图片发送失败')
    }
  } catch (error) {
    console.error('私聊图片发送失败:', error)
    const list = (chatStore as any).messages[chatStore.activeConversationId!] as Message[]
    const found = list.find(m => m.id === tempId)
    if (found) {
      found.status = 'sent'
      found.content = '图片发送失败'
    }
    throw error
  }
}

// 群聊图片发送（与 Web 端保持一致）
const handleGroupImageSend = async (file: File) => {
  const me = authStore.userInfo?.id
  if (!me) return
  const groupId = parseInt(chatStore.activeConversationId!.replace('group_', ''))
  if (isNaN(groupId)) {
    console.error('无效的群ID:', chatStore.activeConversationId)
    return
  }

  const tempId = Date.now() + Math.random()
  const optimistic: Message = {
    id: tempId as any,
    fromUserId: me,
    groupId,
    content: '__uploading__',
    messageType: 'image',
    status: 'sending',
    sendTime: new Date().toISOString(),
    createTime: new Date().toISOString(),
    updateTime: new Date().toISOString(),
    senderId: me
  }
  if (!chatStore.messages[chatStore.activeConversationId!]) {
    ;(chatStore as any).messages[chatStore.activeConversationId!] = []
  }
  ;(chatStore as any).messages[chatStore.activeConversationId!].push(optimistic)

  try {
    const resp = await chatApi.sendGroupImage(groupId, file)
    if ((resp as any).code === 200 && (resp as any).data) {
      const serverMsg = (resp as any).data as any
      const imageContent = serverMsg.content ?? serverMsg.imageId ?? serverMsg.data?.imageId ?? serverMsg.data?.filename ?? file.name
      const list = (chatStore as any).messages[chatStore.activeConversationId!] as Message[]
      const found = list.find(m => m.id === tempId)
      if (found) {
        found.id = serverMsg.id ?? found.id
        found.status = 'delivered'
        found.messageType = 'image'
        found.content = String(imageContent)
        found.updateTime = new Date().toISOString()
      }
    } else {
      throw new Error('群聊图片发送失败')
    }
  } catch (error) {
    console.error('群聊图片发送失败:', error)
    const list = (chatStore as any).messages[chatStore.activeConversationId!] as Message[]
    const found = list.find(m => m.id === tempId)
    if (found) {
      found.status = 'sent'
      found.content = '图片发送失败'
    }
    throw error
  }
}

// 清空聊天记录
const clearChatHistory = async () => {
  showMenu.value = false
  
  try {
    await ElMessageBox.confirm(
      '确定要清空聊天记录吗？此操作不可恢复。',
      '清空聊天记录',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    chatStore.clearCurrentChatHistory()
    ElMessage.success('聊天记录已清空')
  } catch {
    // 用户取消
  }
}

// 显示联系人信息
const showContactInfo = () => {
  showMenu.value = false
  showContactInfoDialog.value = true
}

// 初始化聊天会话
const initializeChat = async () => {
  const chatType = route.params.type as string // 'user' 或 'group'
  const chatId = route.params.id as string
  
  if (chatType && chatId) {
    let conversationId: string
    
    if (chatType === 'group') {
      // 群聊
      conversationId = `group_${chatId}`
    } else {
      // 私聊
      conversationId = chatId
    }
    
    // 设置活动会话
    await chatStore.setActiveConversation(conversationId)
    
    // 如果是群聊，加载群聊历史记录
    if (chatType === 'group') {
      await chatStore.loadGroupChatHistory(parseInt(chatId))
    }
  }
}

// 点击其他地方关闭菜单
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.menu-icon') && !target.closest('.menu-dropdown')) {
    showMenu.value = false
  }
}

// 生命周期
onMounted(() => {
  // 确保WebSocket连接已建立，实时接收消息与未读数更新（与Web端一致）
  chatStore.initWebSocket().catch((error: any) => {
    console.error('WebSocket连接失败:', error)
  })
  initializeChat()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  // 清空活动会话
  chatStore.setActiveConversation('')
})
</script>

<style scoped>
.mobile-chat-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
  position: relative;
}

.header {
  padding: 16px;
  background: linear-gradient(180deg, #4A90E2 0%, #357ABD 100%);
  color: white;
  display: flex;
  align-items: center;
  position: relative;
}

.back-icon {
  font-size: 20px;
  cursor: pointer;
}

.header-content {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.contact-name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 2px;
}

.contact-status {
  font-size: 12px;
  opacity: 0.9;
}

.contact-status.online {
  color: #52c41a;
}

.contact-status.busy {
  color: #faad14;
}

.contact-status.away {
  color: #1890ff;
}

.contact-status.offline {
  color: #cccccc;
}

.menu-icon {
  font-size: 20px;
  cursor: pointer;
}

.menu-dropdown {
  position: absolute;
  top: 60px;
  right: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 1000;
  min-width: 120px;
}

.menu-item {
  padding: 12px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item:hover {
  background-color: #f5f7fa;
}

.message-list {
  flex: 1;
  overflow-y: auto;
}

.message-input {
  background: white;
  border-top: 1px solid #f0f0f0;
}
</style>