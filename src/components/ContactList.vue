<template>
  <div class="contact-list">
    <div
      v-for="conversation in conversations"
      :key="conversation.id"
      class="contact-item"
      :class="{ active: conversation.id === activeConversationId }"
      @click="$emit('selectConversation', conversation.id)"
    >
      <div class="contact-avatar">
        <el-avatar :src="getContactAvatar(conversation)" :size="48" />
        <div
          v-if="getContactStatus(conversation) === 'online'"
          class="status-dot online"
        ></div>
      </div>
      
      <div class="contact-content">
        <div class="contact-header">
          <div class="contact-name">{{ getContactName(conversation) }}</div>
          <div class="message-time">{{ formatTime(conversation.timestamp) }}</div>
        </div>
        
        <div class="contact-footer">
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
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useChatStore } from '@/stores/chat'
import type { Conversation, Message } from '@/types'
import dayjs from 'dayjs'

interface Props {
  conversations: Conversation[]
  activeConversationId: string | null
}

defineProps<Props>()
defineEmits<{
  selectConversation: [conversationId: string]
}>()

const chatStore = useChatStore()

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
</script>

<style scoped>
.contact-list {
  flex: 1;
  overflow-y: auto;
}

.contact-item {
  display: flex;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.contact-item:hover {
  background-color: #f8f8f8;
}

.contact-item.active {
  background-color: #e6f4ff;
  border-right: 3px solid #1890ff;
}

.contact-avatar {
  position: relative;
  margin-right: 12px;
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

.contact-content {
  flex: 1;
  min-width: 0;
}

.contact-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.contact-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-time {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
  margin-left: 8px;
}

.contact-footer {
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

.unread-badge {
  flex-shrink: 0;
  margin-left: 8px;
}

/* 滚动条样式 */
.contact-list::-webkit-scrollbar {
  width: 4px;
}

.contact-list::-webkit-scrollbar-track {
  background: transparent;
}

.contact-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.contact-list::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>