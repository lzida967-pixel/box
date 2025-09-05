<template>
  <div class="message-input">
    <!-- 工具栏 -->
    <div class="input-toolbar">
      <div class="toolbar-left">
        <el-button text @click="selectEmoji">
          <el-icon><Sunny /></el-icon>
        </el-button>
        <el-button text @click="selectFile">
          <el-icon><Paperclip /></el-icon>
        </el-button>
        <el-button text @click="selectImage">
          <el-icon><Picture /></el-icon>
        </el-button>
        <el-button text @click="recordVoice">
          <el-icon><Microphone /></el-icon>
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-button text @click="openHistory">
          <el-icon><Clock /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <el-input
        ref="inputRef"
        v-model="inputText"
        type="textarea"
        :rows="3"
        resize="none"
        placeholder="输入消息..."
        @keydown="handleKeyDown"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
        class="message-textarea"
      />
    </div>

    <!-- 发送区域 -->
    <div class="send-area">
      <div class="send-info">
        <span class="send-tip">按 Enter 发送，Shift + Enter 换行</span>
      </div>
      <div class="send-actions">
        <el-button type="primary" @click="sendMessage" :disabled="!canSend">
          发送 (Enter)
        </el-button>
      </div>
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      ref="fileInputRef"
      type="file"
      style="display: none"
      @change="handleFileSelect"
      multiple
    />
    
    <input
      ref="imageInputRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleImageSelect"
      multiple
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{
  send: [content: string]
}>()

const inputRef = ref()
const fileInputRef = ref()
const imageInputRef = ref()
const inputText = ref('')
const isComposing = ref(false)
const typingTimer = ref<NodeJS.Timeout | null>(null)

const canSend = computed(() => {
  return inputText.value.trim().length > 0
})

const handleKeyDown = (event: KeyboardEvent) => {
  // 处理输入法组合
  if (event.isComposing || isComposing.value) {
    return
  }

  // Enter 发送消息，Shift + Enter 换行
  if (event.key === 'Enter') {
    if (event.shiftKey) {
      // Shift + Enter 换行，不做处理
      return
    } else {
      // Enter 发送消息
      event.preventDefault()
      sendMessage()
    }
  }
}

const handleInput = () => {
  // 模拟正在输入状态
  if (typingTimer.value) {
    clearTimeout(typingTimer.value)
  }
  
  // 这里可以发送正在输入的状态给其他用户
  // chatStore.setTyping(conversationId, true)
  
  typingTimer.value = setTimeout(() => {
    // chatStore.setTyping(conversationId, false)
  }, 1000) as NodeJS.Timeout
}

const handleFocus = () => {
  // 输入框获得焦点
}

const handleBlur = () => {
  // 输入框失去焦点
  if (typingTimer.value) {
    clearTimeout(typingTimer.value)
  }
}

const sendMessage = () => {
  if (!canSend.value) return

  const content = inputText.value.trim()
  if (content) {
    emit('send', content)
    inputText.value = ''
    
    // 清除正在输入状态
    if (typingTimer.value) {
      clearTimeout(typingTimer.value)
    }
    
    // 重新聚焦输入框
    nextTick(() => {
      inputRef.value?.focus()
    })
  }
}

const selectEmoji = () => {
  ElMessage.info('表情功能开发中...')
}

const selectFile = () => {
  fileInputRef.value?.click()
}

const selectImage = () => {
  imageInputRef.value?.click()
}

const recordVoice = () => {
  ElMessage.info('语音功能开发中...')
}

const openHistory = () => {
  ElMessage.info('聊天记录功能开发中...')
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    for (const file of files) {
      ElMessage.success(`选择了文件: ${file.name}`)
      // 这里可以处理文件上传逻辑
    }
  }
  target.value = '' // 清空选择
}

const handleImageSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    for (const file of files) {
      ElMessage.success(`选择了图片: ${file.name}`)
      // 这里可以处理图片上传逻辑
    }
  }
  target.value = '' // 清空选择
}

// 组合事件处理
const handleCompositionStart = () => {
  isComposing.value = true
}

const handleCompositionEnd = () => {
  isComposing.value = false
}
</script>

<style scoped>
.message-input {
  border-top: 1px solid #e0e0e0;
  background: white;
  display: flex;
  flex-direction: column;
}

.input-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid #f0f0f0;
  min-height: 40px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  gap: 4px;
}

.input-area {
  padding: 12px 16px 8px;
}

.message-textarea {
  font-size: 14px;
}

.message-textarea :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  padding: 0;
  resize: none;
  font-family: inherit;
  line-height: 1.5;
}

.message-textarea :deep(.el-textarea__inner):focus {
  border: none;
  box-shadow: none;
}

.send-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px 12px;
}

.send-info {
  flex: 1;
}

.send-tip {
  font-size: 12px;
  color: #999;
}

.send-actions {
  display: flex;
  gap: 8px;
}

/* 让输入框看起来更自然 */
.input-area :deep(.el-textarea) {
  font-size: 14px;
}

.input-area :deep(.el-textarea__inner) {
  background: transparent;
  color: #333;
  line-height: 1.6;
  font-size: 14px;
}

.input-area :deep(.el-textarea__inner)::placeholder {
  color: #ccc;
}
</style>