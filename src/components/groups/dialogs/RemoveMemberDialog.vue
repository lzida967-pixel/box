<template>
  <el-dialog
    v-model="dialogVisible"
    title="移除成员"
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
        <el-avatar :src="getUserAvatar(member)" :size="40" />
        <div class="member-info">
          <div class="member-name">
            {{ member.memberNickname || member.nickname || member.user?.username }}
            <el-tag v-if="member.memberRole === 2" type="warning" size="small">管理员</el-tag>
          </div>
          <div class="member-status">
            <span v-if="member.muteUntil" class="muted-status">已禁言</span>
            <span v-else class="normal-status">正常</span>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredMembers.length === 0" class="empty-state">
        <el-empty :description="removableMembers.length === 0 ? '没有可移除的成员' : '未找到匹配的成员'" />
      </div>
    </div>

    <!-- 已选择的成员 -->
    <div v-if="selectedMembers.length > 0" class="selected-section">
      <div class="selected-title">将移除 ({{ selectedMembers.length }})</div>
      <div class="selected-list">
        <div
          v-for="member in selectedMembers"
          :key="member.id"
          class="selected-member"
        >
          <el-avatar :src="getUserAvatar(member)" :size="24" />
          <span class="selected-name">{{ member.memberNickname || member.nickname || member.user?.username }}</span>
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
          type="danger" 
          @click="handleRemove" 
          :loading="loading"
          :disabled="selectedMembers.length === 0"
        >
          移除 ({{ selectedMembers.length }})
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

// Props & Emits
const props = defineProps<{
  modelValue: boolean
  groupId?: number
  members?: GroupMember[] // 改为可选，支持实时查询
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
const allMembers = ref<GroupMember[]>([]) // 存储所有群成员
const loadingMembers = ref(false) // 加载成员的状态

// 计算属性
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 当前用户的角色（数字值：3=群主，2=管理员，1=普通成员）
const currentUserRole = computed(() => {
  const currentUserId = authStore.userInfo?.id
  // 使用宽松比较，避免数字和字符串类型不匹配问题
  const currentMember = allMembers.value.find(m => Number(m.userId) === Number(currentUserId))
  console.log('当前用户查找 - authStore.userInfo.id:', currentUserId, '类型:', typeof currentUserId)
  console.log('当前用户查找 - allMembers:', allMembers.value.map(m => ({ userId: m.userId, type: typeof m.userId })))
  console.log('当前用户查找 - 找到的成员:', currentMember)
  return currentMember?.memberRole
})

// 可移除的成员（先简化逻辑，显示所有成员除了自己）
const removableMembers = computed(() => {
  console.log('removableMembers计算 - allMembers:', allMembers.value)
  console.log('removableMembers计算 - currentUserRole:', currentUserRole.value)
  console.log('removableMembers计算 - authStore.userInfo.id:', authStore.userInfo?.id)
  
  // 先简化逻辑：显示所有成员除了自己
  const result = allMembers.value.filter(member => {
    // 不能移除自己
    if (Number(member.userId) === Number(authStore.userInfo?.id)) {
      console.log('排除自己:', member)
      return false
    }
    console.log('可移除:', member)
    return true
  })
  
  console.log('removableMembers结果:', result)
  return result
})

const filteredMembers = computed(() => {
  console.log('filteredMembers计算 - removableMembers:', removableMembers.value)
  console.log('filteredMembers计算 - searchText:', searchText.value)
  if (!searchText.value) return removableMembers.value
  
  const keyword = searchText.value.toLowerCase()
  return removableMembers.value.filter(member => 
    (member.memberNickname || member.nickname || member.user?.username || '').toLowerCase().includes(keyword)
  )
})

// 方法：加载群成员
const loadGroupMembers = async () => {
  if (!props.groupId) return
  
  loadingMembers.value = true
  try {
    // 实时查询群成员（忽略父组件传递的成员列表，确保数据最新）
    const response = await groupApi.getGroupMembers(props.groupId)
    console.log('API响应结构:', response)
    console.log('响应数据:', response.data)
    // 根据API响应结构，数据直接在response.data中
    allMembers.value = response.data || []
    console.log('实时查询的成员列表:', allMembers.value, '长度:', allMembers.value.length)
  } catch (error: any) {
    console.error('加载群成员失败:', error)
    ElMessage.error(error.message || '加载群成员失败')
  } finally {
    loadingMembers.value = false
  }
}

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

const handleRemove = async () => {
  if (!props.groupId || selectedMembers.value.length === 0) return

  try {
    const memberNames = selectedMembers.value.map(m => m.nickname || m.user?.username).join('、')
    
    await ElMessageBox.confirm(
      `确定要移除以下成员吗？\n${memberNames}`,
      '移除成员',
      {
        confirmButtonText: '确定移除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    loading.value = true
    
    const memberIds = selectedMembers.value.map(m => m.userId)
    await groupApi.removeMembers(props.groupId, memberIds)
    
    ElMessage.success(`成功移除 ${selectedMembers.value.length} 位成员`)
    emit('success')
    handleClose()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('移除成员失败:', error)
      ElMessage.error(error.message || '移除成员失败')
    }
  } finally {
    loading.value = false
  }
}

const getUserAvatar = (userOrMember: any) => {
  // 处理直接传递成员对象的情况（成员对象包含 avatar 字段）
  const avatar = userOrMember?.avatar
  if (!avatar) return 'https://avatars.githubusercontent.com/u/0?v=4'
  
  // 如果头像已经是完整的URL，直接返回
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
    return avatar
  }
  
  // 如果头像字段是 avatar_ 格式，提取用户ID
  if (avatar.startsWith('avatar_')) {
    const userId = avatar.replace('avatar_', '')
    return `http://localhost:8080/api/user/avatar/${userId}`
  }
  
  // 如果头像字段是 /api/user/avatar/ 格式，补全URL
  if (avatar.startsWith('/api/user/avatar/')) {
    return `http://localhost:8080${avatar}`
  }
  
  // 其他情况直接返回
  return avatar
}

// 监听对话框显示状态和groupId变化
watch([dialogVisible, () => props.groupId], ([visible, groupId]) => {
  if (visible && groupId) {
    // 对话框打开且有groupId时加载群成员
    loadGroupMembers()
  } else {
    // 对话框关闭时清空选择
    selectedMembers.value = []
    searchText.value = ''
  }
}, { immediate: true })
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
  background-color: #fef0f0;
  border: 1px solid #f56c6c;
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

.member-status {
  font-size: 12px;
}

.muted-status {
  color: #f56c6c;
}

.normal-status {
  color: #67c23a;
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
  color: #f56c6c;
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
  background: #fef0f0;
  border: 1px solid #f56c6c;
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
  color: #f56c6c;
  font-size: 14px;
}

.remove-icon:hover {
  color: #f78989;
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