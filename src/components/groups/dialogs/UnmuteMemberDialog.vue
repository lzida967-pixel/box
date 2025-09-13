<template>
  <el-dialog
    v-model="dialogVisible"
    title="解除禁言"
    width="500px"
    :before-close="handleClose"
  >
    <!-- 搜索栏 -->
    <div class="search-section">
      <el-input
        v-model="searchText"
        placeholder="搜索被禁言的成员"
        prefix-icon="Search"
        clearable
      />
    </div>

    <!-- 成员列表 -->
    <div class="member-list">
      <div
        v-for="member in filteredMembers"
        :key="member.id"
        class="member-item"
        :class="{ selected: isSelected(member) }"
        @click="toggleMember(member)"
      >
        <el-checkbox
          :model-value="isSelected(member)"
          @change="toggleMember(member)"
        />
        <el-avatar :src="getUserAvatar(member.user)" :size="40" />
        <div class="member-info">
          <div class="member-name">
            {{ member.nickname || member.user?.username }}
            <el-tag v-if="member.role === 'ADMIN'" type="warning" size="small">管理员</el-tag>
          </div>
          <div class="mute-info">
            <span class="mute-status">已禁言</span>
            <span v-if="member.mutedUntil" class="mute-until">
              至 {{ formatTime(member.mutedUntil) }}
            </span>
          </div>
        </div>
        <div class="mute-duration">
          {{ getMuteDurationText(member) }}
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredMembers.length === 0" class="empty-state">
        <el-empty :description="mutedMembers.length === 0 ? '没有被禁言的成员' : '未找到匹配的成员'" />
      </div>
    </div>

    <!-- 已选择的成员 -->
    <div v-if="selectedMembers.length > 0" class="selected-section">
      <div class="selected-title">将解除禁言 ({{ selectedMembers.length }})</div>
      <div class="selected-list">
        <div
          v-for="member in selectedMembers"
          :key="member.id"
          class="selected-member"
        >
          <el-avatar :src="getUserAvatar(member.user)" :size="24" />
          <span class="selected-name">{{ member.nickname || member.user?.username }}</span>
          <el-icon class="remove-icon" @click="removeMember(member)">
            <Close />
          </el-icon>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="success" 
          @click="handleUnmute" 
          :loading="loading"
          :disabled="selectedMembers.length === 0"
        >
          解除禁言 ({{ selectedMembers.length }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { groupApi } from '@/api'
import type { GroupMember } from '@/types'
import dayjs from 'dayjs'

// Props & Emits
const props = defineProps<{
  modelValue: boolean
  groupId?: number
  members: GroupMember[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

// 响应式数据
const authStore = useAuthStore()
const searchText = ref('')
const selectedMembers = ref<GroupMember[]>([])
const loading = ref(false)

// 计算属性
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 当前用户的角色
const currentUserRole = computed(() => {
  const currentMember = props.members.find(m => m.userId === authStore.userInfo?.id)
  return currentMember?.role
})

// 被禁言的成员
const mutedMembers = computed(() => {
  return props.members.filter(member => {
    // 只显示被禁言的成员
    if (!member.isMuted) return false
    
    // 如果当前用户是群主，可以解除所有人的禁言
    if (currentUserRole.value === 'OWNER') return true
    
    // 如果当前用户是管理员，只能解除普通成员的禁言
    if (currentUserRole.value === 'ADMIN' && member.role === 'MEMBER') return true
    
    return false
  })
})

const filteredMembers = computed(() => {
  if (!searchText.value) return mutedMembers.value
  
  const keyword = searchText.value.toLowerCase()
  return mutedMembers.value.filter(member => 
    (member.nickname || member.user?.username || '').toLowerCase().includes(keyword)
  )
})

// 方法
const isSelected = (member: GroupMember) => {
  return selectedMembers.value.some(m => m.id === member.id)
}

const toggleMember = (member: GroupMember) => {
  const index = selectedMembers.value.findIndex(m => m.id === member.id)
  if (index > -1) {
    selectedMembers.value.splice(index, 1)
  } else {
    selectedMembers.value.push(member)
  }
}

const removeMember = (member: GroupMember) => {
  const index = selectedMembers.value.findIndex(m => m.id === member.id)
  if (index > -1) {
    selectedMembers.value.splice(index, 1)
  }
}

const handleClose = () => {
  selectedMembers.value = []
  searchText.value = ''
  emit('update:modelValue', false)
}

const handleUnmute = async () => {
  if (!props.groupId || selectedMembers.value.length === 0) return

  try {
    const memberNames = selectedMembers.value.map(m => m.nickname || m.user?.username).join('、')
    
    await ElMessageBox.confirm(
      `确定要解除以下成员的禁言吗？\n${memberNames}`,
      '解除禁言',
      {
        confirmButtonText: '确定解除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    loading.value = true
    
    const memberIds = selectedMembers.value.map(m => m.userId)
    await groupApi.unmuteMembers(props.groupId, memberIds)
    
    ElMessage.success(`成功解除 ${selectedMembers.value.length} 位成员的禁言`)
    emit('success')
    handleClose()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('解除禁言失败:', error)
      ElMessage.error(error.message || '解除禁言失败')
    }
  } finally {
    loading.value = false
  }
}

const getUserAvatar = (user: any) => {
  if (!user?.avatar) return 'https://avatars.githubusercontent.com/u/0?v=4'
  
  if (user.avatar.startsWith('avatar_')) {
    return `http://localhost:8080/api/user/avatar/${user.id}`
  } else if (user.avatar.startsWith('/api/user/avatar/')) {
    return `http://localhost:8080${user.avatar}`
  }
  
  return user.avatar
}

const formatTime = (timestamp?: Date | string) => {
  if (!timestamp) return '永久'
  
  try {
    return dayjs(timestamp).format('MM-DD HH:mm')
  } catch (error) {
    return '永久'
  }
}

const getMuteDurationText = (member: GroupMember) => {
  if (!member.mutedUntil) return '永久禁言'
  
  try {
    const now = dayjs()
    const until = dayjs(member.mutedUntil)
    
    if (until.isBefore(now)) {
      return '已过期'
    }
    
    const diff = until.diff(now, 'minute')
    
    if (diff < 60) {
      return `${diff}分钟`
    } else if (diff < 1440) {
      const hours = Math.floor(diff / 60)
      return `${hours}小时`
    } else {
      const days = Math.floor(diff / 1440)
      return `${days}天`
    }
  } catch (error) {
    return '永久禁言'
  }
}

// 监听对话框显示状态
watch(dialogVisible, (visible) => {
  if (!visible) {
    selectedMembers.value = []
    searchText.value = ''
  }
})
</script>

<style scoped>
.search-section {
  margin-bottom: 16px;
}

.member-list {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.2s;
  gap: 12px;
}

.member-item:hover {
  background-color: #f8f9fa;
}

.member-item.selected {
  background-color: #f6ffed;
  border: 1px solid #52c41a;
}

.member-info {
  flex: 1;
}

.member-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  margin-bottom: 2px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.mute-info {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.mute-status {
  color: #f56c6c;
}

.mute-until {
  color: #999;
}

.mute-duration {
  font-size: 12px;
  color: #f56c6c;
  font-weight: 500;
  flex-shrink: 0;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
}

.selected-section {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}

.selected-title {
  font-size: 14px;
  font-weight: 500;
  color: #52c41a;
  margin-bottom: 12px;
}

.selected-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 120px;
  overflow-y: auto;
}

.selected-member {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: #f6ffed;
  border: 1px solid #52c41a;
  border-radius: 16px;
  font-size: 12px;
}

.selected-name {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remove-icon {
  cursor: pointer;
  color: #52c41a;
  font-size: 14px;
}

.remove-icon:hover {
  color: #73d13d;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 滚动条样式 */
.member-list::-webkit-scrollbar,
.selected-list::-webkit-scrollbar {
  width: 4px;
}

.member-list::-webkit-scrollbar-track,
.selected-list::-webkit-scrollbar-track {
  background: transparent;
}

.member-list::-webkit-scrollbar-thumb,
.selected-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.member-list::-webkit-scrollbar-thumb:hover,
.selected-list::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>