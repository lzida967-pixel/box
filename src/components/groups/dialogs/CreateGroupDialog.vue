<template>
  <el-dialog
    v-model="dialogVisible"
    title="创建群聊"
    width="500px"
    :before-close="handleClose"
    class="create-group-dialog"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
      class="group-form"
    >
      <!-- 群头像 -->
      <el-form-item label="群头像">
        <div class="avatar-upload">
          <el-upload
            class="avatar-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
            accept="image/*"
          >
            <div class="avatar-container">
              <el-avatar v-if="form.avatar" :src="form.avatar" :size="80" />
              <div v-else class="avatar-placeholder">
                <el-icon><Plus /></el-icon>
                <div class="upload-text">上传头像</div>
              </div>
            </div>
          </el-upload>
        </div>
      </el-form-item>

      <!-- 群名称 -->
      <el-form-item label="群名称" prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入群名称"
          maxlength="20"
          show-word-limit
        />
      </el-form-item>

      <!-- 群描述 -->
      <el-form-item label="群描述">
        <el-input
          v-model="form.description"
          type="textarea"
          placeholder="请输入群描述（可选）"
          :rows="3"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>


      <!-- 最大成员数 -->
      <el-form-item label="成员上限" prop="maxMembers">
        <el-input-number
          v-model="form.maxMembers"
          :min="2"
          :max="500"
          :step="1"
          placeholder="成员上限"
        />
        <span class="member-limit-desc">（2-500人）</span>
      </el-form-item>

      <!-- 邀请好友 -->
      <el-form-item label="邀请好友">
        <div class="invite-section">
          <el-button @click="showFriendSelector = true" icon="Plus">
            选择好友
          </el-button>
          <div v-if="selectedFriends.length > 0" class="selected-friends">
            <div class="selected-count">已选择 {{ selectedFriends.length }} 位好友</div>
            <div class="friend-list">
              <div
                v-for="friend in selectedFriends"
                :key="friend.id"
                class="friend-item"
              >
                <el-avatar :src="getUserAvatar(friend)" :size="32" />
                <span class="friend-name">{{ friend.nickname || friend.username }}</span>
                <el-icon class="remove-icon" @click="removeFriend(friend)">
                  <Close />
                </el-icon>
              </div>
            </div>
          </div>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">
          创建群聊
        </el-button>
      </div>
    </template>

    <!-- 好友选择器 -->
    <FriendSelectorDialog
      v-model="showFriendSelector"
      :selected-friends="selectedFriends"
      @confirm="handleFriendsSelected"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, type FormInstance, type UploadProps } from 'element-plus'
import { Plus, Close } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { groupApi } from '@/api'
import FriendSelectorDialog from './FriendSelectorDialog.vue'
import type { User, ChatGroup } from '@/types'

// Props & Emits
const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  created: [group: any]
}>()

// 响应式数据
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const showFriendSelector = ref(false)
const selectedFriends = ref<User[]>([])

const form = ref({
  name: '',
  description: '',
  avatar: '',
  maxMembers: 50
})

// 计算属性
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const uploadUrl = computed(() => 'http://localhost:8080/api/group/avatar')
const uploadHeaders = computed(() => ({
  'Authorization': `Bearer ${authStore.token}`
}))

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入群名称', trigger: 'blur' },
    { min: 2, max: 20, message: '群名称长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  maxMembers: [
    { required: true, message: '请设置成员上限', trigger: 'blur' },
    { type: 'number', min: 2, max: 500, message: '成员上限在 2 到 500 之间', trigger: 'blur' }
  ]
}

// 方法
const handleClose = () => {
  resetForm()
  emit('update:modelValue', false)
}

const resetForm = () => {
  form.value = {
    name: '',
    description: '',
    avatar: '',
    maxMembers: 50
  }
  selectedFriends.value = []
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    const groupData = {
      ...form.value,
      memberIds: selectedFriends.value.map(f => f.id)
    }

    const response = await groupApi.createGroup(groupData)
    
    if (response && response.code === 200 && response.data) {
      emit('created', response.data)
      handleClose()
      ElMessage.success('群聊创建成功')
    } else {
      throw new Error(response?.message || '创建群聊失败：响应数据格式错误')
    }
  } catch (error: any) {
    console.error('创建群聊失败:', error)
    ElMessage.error(error.message || '创建群聊失败')
  } finally {
    loading.value = false
  }
}

const handleAvatarSuccess: UploadProps['onSuccess'] = (response) => {
  if (response.success) {
    form.value.avatar = response.data.url
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response.message || '头像上传失败')
  }
}

const beforeAvatarUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

const handleFriendsSelected = (friends: User[]) => {
  selectedFriends.value = friends
}

const removeFriend = (friend: User) => {
  const index = selectedFriends.value.findIndex(f => f.id === friend.id)
  if (index > -1) {
    selectedFriends.value.splice(index, 1)
  }
}

const getUserAvatar = (user: User) => {
  if (!user.avatar) return 'https://avatars.githubusercontent.com/u/0?v=4'
  
  if (user.avatar.startsWith('avatar_')) {
    return `http://localhost:8080/api/user/avatar/${user.id}`
  } else if (user.avatar.startsWith('/api/user/avatar/')) {
    return `http://localhost:8080${user.avatar}`
  }
  
  return user.avatar
}

// 监听对话框显示状态
watch(dialogVisible, (visible) => {
  if (!visible) {
    resetForm()
  }
})
</script>

<style scoped>
.create-group-dialog :deep(.el-dialog__body) {
  padding: 20px 20px 0;
}

.group-form {
  max-height: 60vh;
  overflow-y: auto;
}

.avatar-upload {
  display: flex;
  justify-content: flex-start;
}

.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.2s;
}

.avatar-uploader:hover {
  border-color: #409eff;
}

.avatar-container {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8c939d;
  font-size: 14px;
}

.avatar-placeholder .el-icon {
  font-size: 28px;
  margin-bottom: 4px;
}

.upload-text {
  font-size: 12px;
}

.type-description {
  margin-top: 4px;
}

.type-desc {
  font-size: 12px;
  color: #666;
}

.member-limit-desc {
  margin-left: 8px;
  font-size: 12px;
  color: #666;
}

.invite-section {
  width: 100%;
}

.selected-friends {
  margin-top: 12px;
}

.selected-count {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.friend-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.friend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: #f0f0f0;
  border-radius: 16px;
  font-size: 12px;
}

.friend-name {
  max-width: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remove-icon {
  cursor: pointer;
  color: #999;
  font-size: 14px;
}

.remove-icon:hover {
  color: #f56c6c;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 滚动条样式 */
.group-form::-webkit-scrollbar {
  width: 4px;
}

.group-form::-webkit-scrollbar-track {
  background: transparent;
}

.group-form::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.group-form::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>