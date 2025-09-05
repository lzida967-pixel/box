<template>
  <el-dialog
    v-model="visible"
    title="选择联系人"
    width="400px"
    @close="handleClose"
  >
    <div class="contact-selector">
      <el-input
        v-model="searchText"
        placeholder="搜索联系人"
        prefix-icon="Search"
        clearable
        class="search-input"
      />
      
      <div class="contact-list">
        <div
          v-for="contact in filteredContacts"
          :key="contact.id"
          class="contact-item"
          @click="selectContact(contact)"
        >
          <el-avatar :src="contact.avatar" :size="40" />
          <div class="contact-info">
            <div class="contact-name">{{ contact.name }}</div>
            <div class="contact-status">
              <el-tag :type="getStatusType(contact.status)" size="small">
                {{ getStatusText(contact.status) }}
              </el-tag>
            </div>
          </div>
          <el-icon class="contact-arrow">
            <ArrowRight />
          </el-icon>
        </div>
        
        <el-empty v-if="filteredContacts.length === 0" description="没有找到联系人" />
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useChatStore } from '@/stores/chat'
import type { User } from '@/types'

interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'select', contact: User): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const chatStore = useChatStore()
const searchText = ref('')

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const filteredContacts = computed(() => {
  const contacts = chatStore.availableContacts
  if (!searchText.value) return contacts
  
  return contacts.filter(contact =>
    contact.name.toLowerCase().includes(searchText.value.toLowerCase())
  )
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

const selectContact = (contact: User) => {
  emit('select', contact)
  handleClose()
}

const handleClose = () => {
  searchText.value = ''
  visible.value = false
}
</script>

<style scoped>
.contact-selector {
  max-height: 400px;
}

.search-input {
  margin-bottom: 16px;
}

.contact-list {
  max-height: 320px;
  overflow-y: auto;
}

.contact-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  gap: 12px;
}

.contact-item:hover {
  background-color: #f8f8f8;
}

.contact-info {
  flex: 1;
  min-width: 0;
}

.contact-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-status {
  display: flex;
  align-items: center;
}

.contact-arrow {
  color: #ccc;
  transition: color 0.2s;
}

.contact-item:hover .contact-arrow {
  color: #1890ff;
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