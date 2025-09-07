<template>
  <el-dialog
    v-model="visible"
    :title="contact?.name || '联系人信息'"
    width="500px"
    @close="handleClose"
  >
    <div class="contact-info-content" v-if="contact">
      <div class="contact-avatar-section">
        <el-avatar :src="contact.avatar" :size="80" />
      </div>
      
      <div class="contact-details">
        <div class="detail-item">
          <label>用户名:</label>
          <span>{{ contact.username || contact.name || '未知' }}</span>
        </div>
        
        <div class="detail-item">
          <label>昵称:</label>
          <span>{{ contact.nickname || contact.name || '未设置' }}</span>
        </div>
        
        <div class="detail-item">
          <label>状态:</label>
          <el-tag :type="getStatusType(contact.status)" size="small">
            {{ getStatusText(contact.status) }}
          </el-tag>
        </div>
        
        <div class="detail-item">
          <label>性别:</label>
          <span>{{ getGenderText(contact.gender) }}</span>
        </div>
        
        <div class="detail-item" v-if="contact.email">
          <label>邮箱:</label>
          <span>{{ contact.email }}</span>
        </div>
        
        <div class="detail-item">
          <label>签名:</label>
          <span class="signature">{{ getSignature(contact) }}</span>
        </div>
      </div>
      
      <div class="action-buttons">
        <el-button type="primary" @click="sendMessage">
          <el-icon><ChatDotRound /></el-icon>
          发消息
        </el-button>
        <el-button type="danger" @click="removeContact">
          <el-icon><Delete /></el-icon>
          删除好友
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { User } from '@/types'

interface Props {
  modelValue: boolean
  contact: User | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const getStatusType = (status: string) => {
  switch (status) {
    case 'online': return 'success'
    case 'away': return 'warning'
    case 'offline': return 'danger'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'online': return '在线'
    case 'away': return '离开'
    case 'offline': return '离线'
    default: return '未知'
  }
}

const getGenderText = (gender?: number) => {
  switch (gender) {
    case 1: return '男'
    case 2: return '女'
    case 0:
    default: return '未知'
  }
}

const getSignature = (contact: User) => {
  // 模拟个性签名
  const signatures = {
    '1': '系统管理员',
    '2': '你们已成为好友，现在可以开始聊天了',
    '3': '加好友请备注来意，本号不闲聊',
    '4': '管理员账号'
  }
  return signatures[contact.id as keyof typeof signatures] || '这个人很懒，什么都没留下'
}

const sendMessage = () => {
  ElMessage.success('切换到聊天界面')
  handleClose()
}

const removeContact = () => {
  ElMessage.warning('删除好友功能开发中...')
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.contact-info-content {
  text-align: center;
}

.contact-avatar-section {
  margin-bottom: 24px;
}

.contact-details {
  text-align: left;
  margin-bottom: 24px;
}

.detail-item {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.detail-item label {
  width: 80px;
  color: #666;
  font-weight: 500;
  flex-shrink: 0;
}

.detail-item span {
  flex: 1;
  color: #333;
}

.signature {
  color: #999;
  font-style: italic;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>