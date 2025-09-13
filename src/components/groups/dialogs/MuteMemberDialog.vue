<template>
  <el-dialog
    v-model="dialogVisible"
    title="禁言成员"
    width="500px"
    :before-close="handleClose"
  >
    <!-- 搜索栏 -->
    <div class="search-section">
      <el-input
        v-model="searchText"
        placeholder="搜索成员"
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
          <div class="member-join-time">
            加入时间: {{ formatTime(member.joinedAt) }}
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredMembers.length === 0" class="empty-state">
        <el-empty :description="mutableMembers.length === 0 ? '没有可禁言的成员' : '未找到匹配的成员'" />
      </div>
    </div>

    <!-- 已选择的成员 -->
    <div v-if="selectedMembers.length > 0" class="selected-section">
      <div class="selected-title">将禁言 ({{ selectedMembers.length }})</div>
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

    <!-- 禁言时长设置 -->
    <div v-if="selectedMembers.length > 0" class="mute-duration-section">
      <div class="duration-title">禁言时长</div>
      <el-radio-group v-model="muteDuration" class="duration-options">
        <el-radio :label="10">10分钟</el-radio>
        <el-radio :label="60">1小时</el-radio>
        <el-radio :label="360">6小时</el-radio>
        <el-radio :label="1440">1天</el-radio>
        <el-radio :label="10080">7天</el-radio>
        <el-radio :label="43200">30天</el-radio>
      </el-radio-group>
      
      <div class="custom-duration">
        <el-radio v-model="muteDuration" :label="customDuration">自定义</el-radio>
        <el-input-number
          v-model="customDuration"
          :min="1"
          :max="525600"
          :step="1"
          placeholder="分钟"
          @change="muteDuration = customDuration"
        />
        <span class="duration-unit">分钟</span>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="warning" 
          @click="handleMute" 
          :loading="loading"
          :disabled="selectedMembers.length === 0"
        >
          禁言 ({{ selectedMembers.length }})
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
const muteDuration = ref(60) // 默认1小时
const customDuration = ref(60)
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

// 可禁言的成员（排除群主和自己，以及已禁言的成员）
const mutableMembers = computed(() => {
  return props.members.filter(member => {
    // 不能禁言群主
    if (member.role === 'OWNER') return false
    
    // 不能禁言自己
    if (member.userId === authStore.userInfo?.id) return false
    
    // 不能禁言已经被禁言的成员
    if (member.isMuted) return false
    
    // 如果当前用户是群主，可以禁言所有人
    if (currentUserRole.value === 'OWNER') return true
    
    // 如果当前用户是管理员，只能禁言普通成员
    if (currentUserRole.value === 'ADMIN' && member.role === 'MEMBER') return true
    
    return false
  })
})

const filteredMembers = computed(() => {
  if (!searchText.value) return mutableMembers.value
  
  const keyword = searchText.value.toLowerCase()
  return mutableMembers.value.filter(member => 
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
  muteDuration.value = 60
  customDuration.value = 60
  emit('update:modelValue', false)
}

const handleMute = async () => {
  if (!props.groupId || selectedMembers.value.length === 0) return

  try {
    const memberNames = selectedMembers.value.map(m => m.nickname || m.user?.username).join('、')
    const durationText = getDurationText(muteDuration.value)
    
    await ElMessageBox.confirm(
      `确定要禁言以下成员 ${durationText} 吗？\n${memberNames}`,
      '禁言成员',
      {
        confirmButtonText: '确定禁言',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    loading.value = true
    
    const memberIds = selectedMembers.value.map(m => m.userId)
    await groupApi.muteMembers(props.groupId, memberIds, muteDuration.value)
    
    ElMessage.success(`成功禁言 ${selectedMembers.value.length} 位成员`)
    emit('success')
    handleClose()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('禁言成员失败:', error)
      ElMessage.error(error.message || '禁言成员失败')
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
  if (!timestamp) return '未知'
  
  try {
    return dayjs(timestamp).format('YYYY-MM-DD HH:mm')
  } catch (error) {
    return '未知'
  }
}

const getDurationText = (minutes: number) => {
  if (minutes < 60) {
    return `${minutes}分钟`
  } else if (minutes < 1440) {
    const hours = Math.floor(minutes / 60)
    return `${hours}小时`
  } else {
    const days = Math.floor(minutes / 1440)
    return `${days}天`
  }
}

// 监听对话框显示状态
watch(dialogVisible, (visible) => {
  if (!visible) {
    selectedMembers.value = []
    searchText.value = ''
    muteDuration.value = 60
    customDuration.value = 60
  }
})
</script>

<style scoped>
.search-section {
  margin-bottom: 16px;
}

.member-list {
  max-height: 250px;
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
  background-color: #fff7e6;
  border: 1px solid #faad14;
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

.member-join-time {
  font-size: 12px;
  color: #999;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
}

.selected-section {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
  margin-bottom: 16px;
}

.selected-title {
  font-size: 14px;
  font-weight: 500;
  color: #faad14;
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
  background: #fff7e6;
  border: 1px solid #faad14;
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
  color: #faad14;
  font-size: 14px;
}

.remove-icon:hover {
  color: #ffc53d;
}

.mute-duration-section {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}

.duration-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
}

.duration-options {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
}

.custom-duration {
  display: flex;
  align-items: center;
  gap: 8px;
}

.duration-unit {
  font-size: 12px;
  color: #666;
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