<template>
  <div class="message-list" ref="messageListRef">
    <div class="messages-container">
      <div
        v-for="message in messages"
        :key="message.id"
        class="message-wrapper"
        :class="{ 'own-message': isOwnMessage(message) }"
      >
        <div class="message-item">
          <!-- 对方的头像 -->
          <div v-if="!isOwnMessage(message)" class="message-avatar">
            <el-avatar :src="getSenderAvatar(message)" :size="36" />
          </div>

          <!-- 消息内容 -->
          <div class="message-content">
            <!-- 群聊中显示发送者名称（仅非自己的消息） -->
            <div v-if="!isOwnMessage(message) && isGroupMessage()" class="sender-name">
              {{ getSenderName(message) }}
            </div>
            
            <!-- 消息气泡 -->
            <div class="message-bubble" :class="getBubbleClass(message)">
              <template v-if="isImage(message)">
                <el-image
                  :src="imageSrc(message)"
                  :preview-src-list="[imageSrc(message)]"
                  fit="cover"
                  style="max-width: 220px; max-height: 220px; border-radius: 8px; overflow: hidden"
                >
                  <template #error>
                    <div style="width:200px;height:160px;display:flex;align-items:center;justify-content:center;color:#999;background:#f5f5f5">图片加载失败</div>
                  </template>
                </el-image>
              </template>
              <div v-else class="message-text">{{ message.content }}</div>

              <!-- 消息状态（仅自己的消息显示） -->
              <div v-if="isOwnMessage(message)" class="message-status">
                <el-icon v-if="message.status === 'sending'" class="status-sending">
                  <Loading />
                </el-icon>
                <el-icon v-else-if="message.status === 'sent'" class="status-sent">
                  <Check />
                </el-icon>
                <el-icon v-else-if="message.status === 'delivered'" class="status-delivered">
                  <Check />
                </el-icon>
                <el-icon v-else-if="message.status === 'read'" class="status-read">
                  <Check />
                </el-icon>
              </div>
            </div>
            
            <!-- 消息时间 -->
            <div class="message-time">{{ formatMessageTime(message.sendTime || message.createTime) }}</div>
          </div>

          <!-- 自己的头像 -->
          <div v-if="isOwnMessage(message)" class="message-avatar">
            <el-avatar :src="getOwnAvatar()" :size="36" />
          </div>
        </div>
      </div>

      <!-- 正在输入提示 -->
      <div v-if="showTypingIndicator" class="typing-indicator">
        <div class="typing-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <span class="typing-text">对方正在输入...</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { useChatStore } from '@/stores/chat'
import type { Message } from '@/types'
import { imageApi } from '@/api'
import { getUserAvatarUrl } from '@/utils/avatar'
import dayjs from 'dayjs'

interface Props {
  messages: Message[]
}

const props = defineProps<Props>()

const chatStore = useChatStore()
const messageListRef = ref<HTMLElement>()

const showTypingIndicator = computed(() => {
  return chatStore.isTyping[chatStore.activeConversationId || ''] || false
})

const isOwnMessage = (message: Message) => {
  const currentUserId = chatStore.currentUser?.id
  if (!currentUserId) return false
  
  // 使用正确的字段名：fromUserId（兼容senderId）
  const messageSenderId = message.fromUserId || message.senderId
  if (!messageSenderId) return false
  
  // 确保类型匹配：将两个ID都转换为字符串进行比较
  return messageSenderId.toString() === currentUserId.toString()
}

const getSenderAvatar = (message: Message) => {
  // 使用正确的字段名：fromUserId（兼容senderId）
  const senderId = message.fromUserId || message.senderId
  const sender = chatStore.getContactById(senderId)
  return getUserAvatarUrl(sender)
}

// 获取自己的头像
const getOwnAvatar = () => {
  return getUserAvatarUrl(chatStore.currentUser)
}

// 检查是否为群聊消息
const isGroupMessage = () => {
  return chatStore.activeConversationId?.startsWith('group_')
}

// 获取发送者名称
const getSenderName = (message: Message) => {
  // 使用正确的字段名：fromUserId（兼容senderId）
  const senderId = message.fromUserId || message.senderId
  const sender = chatStore.getContactById(senderId)
  return sender?.nickname || sender?.username || '未知用户'
}

const getBubbleClass = (message: Message) => {
  return isOwnMessage(message) ? 'own-bubble' : 'other-bubble'
}

const isImage = (message: Message) => {
  const t = message.messageType
  return t === 2 || t === 'image'
}

const imageSrc = (message: Message) => {
  // message.content 保存图片ID
  const id = (message.content || '').toString()
  if (!id || id === '__uploading__') return ''
  return imageApi.url(id)
}

const formatMessageTime = (createTime: string) => {
  return dayjs(createTime).format('HH:mm')
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 监听消息变化，自动滚动到底部
watch(() => props.messages.length, () => {
  scrollToBottom()
}, { immediate: true })

// 监听正在输入状态
watch(showTypingIndicator, () => {
  if (showTypingIndicator.value) {
    scrollToBottom()
  }
})
</script>

<style scoped>
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0;
  background: #f8f9fa;
}

.messages-container {
  padding: 0 20px;
}

.message-wrapper {
  margin-bottom: 16px;
}

.message-wrapper.own-message {
  display: flex;
  justify-content: flex-end;
}

.message-item {
  display: flex;
  gap: 0;
  max-width: 70%;
}

.own-message .message-item {
  flex-direction: row;
}

.message-avatar {
  flex-shrink: 0;
  align-self: flex-end;
}

.own-message .message-avatar {
  margin-left: 8px;
  margin-right: 0;
}

.message-wrapper:not(.own-message) .message-avatar {
  margin-right: 8px;
  margin-left: 0;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.own-message .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-bubble {
  position: relative;
  padding: 12px 16px;
  border-radius: 18px;
  word-wrap: break-word;
  max-width: 100%;
  display: inline-block;
}

.other-bubble {
  background: white;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.own-bubble {
  background: #1890ff;
  color: white;
  border-bottom-right-radius: 4px;
}

.message-text {
  line-height: 1.4;
  font-size: 14px;
  white-space: pre-wrap; /* 保留空格和换行，自动换行 */
  word-break: break-word; /* 长单词或URL强制换行 */
  overflow-wrap: break-word; /* 确保长内容正确换行 */
}

.sender-name {
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
  padding-left: 8px;
}

.message-status {
  position: absolute;
  right: -20px;
  bottom: 0;
  font-size: 12px;
}

.status-sending {
  color: #999;
  animation: spin 1s linear infinite;
}

.status-sent {
  color: #999;
}

.status-delivered {
  color: #52c41a;
}

.status-read {
  color: #52c41a;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  padding: 0 4px;
}

.own-message .message-time {
  text-align: right;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  color: #666;
  font-size: 13px;
}

.typing-dots {
  display: flex;
  gap: 4px;
}

.typing-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #ccc;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 滚动条样式 */
.message-list::-webkit-scrollbar {
  width: 6px;
}

.message-list::-webkit-scrollbar-track {
  background: transparent;
}

.message-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

.message-list::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>