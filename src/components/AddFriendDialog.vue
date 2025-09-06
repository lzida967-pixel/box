<template>
  <el-dialog
    v-model="visible"
    title="添加好友"
    width="500px"
    @close="handleClose"
  >
    <div class="add-friend-dialog">
      <!-- 搜索区域 -->
      <div class="search-section">
        <el-input
          v-model="searchKeyword"
          placeholder="输入用户名、昵称或邮箱搜索用户"
          prefix-icon="Search"
          clearable
          @keyup.enter="handleSearch"
          class="search-input"
        />
        <el-button 
          type="primary" 
          @click="handleSearch"
          :loading="searchLoading"
        >
          搜索
        </el-button>
      </div>

      <!-- 搜索结果 -->
      <div class="search-results" v-if="searchResults.length > 0">
        <div class="section-title">搜索结果</div>
        <div
          v-for="user in searchResults"
          :key="user.id"
          class="user-item"
        >
          <el-avatar :src="getUserAvatar(user)" :size="48" />
          <div class="user-info">
            <div class="user-name">{{ user.nickname || user.username }}</div>
            <div class="user-details">
              <span class="username">@{{ user.username }}</span>
              <span v-if="user.signature" class="signature">{{ user.signature }}</span>
            </div>
          </div>
          <el-button 
            type="primary" 
            size="small"
            @click="showAddFriendForm(user)"
            :disabled="addingFriends.has(user.id)"
          >
            {{ addingFriends.has(user.id) ? '请求中...' : '添加好友' }}
          </el-button>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="hasSearched && !searchLoading" class="empty-state">
        <el-empty description="没有找到相关用户" />
      </div>

      <!-- 提示信息 -->
      <div v-if="!hasSearched" class="tips">
        <el-icon><InfoFilled /></el-icon>
        <span>输入关键词搜索用户，可以通过用户名、昵称或邮箱进行查找</span>
      </div>
    </div>

    <!-- 添加好友确认对话框 -->
    <el-dialog
      v-model="showAddForm"
      title="发送好友请求"
      width="400px"
      append-to-body
    >
      <div class="add-friend-form" v-if="selectedUser">
        <div class="user-preview">
          <el-avatar :src="getUserAvatar(selectedUser)" :size="60" />
          <div class="user-info">
            <div class="user-name">{{ selectedUser.nickname || selectedUser.username }}</div>
            <div class="username">@{{ selectedUser.username }}</div>
          </div>
        </div>
        
        <el-form :model="addFriendForm" label-width="80px">
          <el-form-item label="验证消息">
            <el-input
              v-model="addFriendForm.message"
              type="textarea"
              :rows="3"
              placeholder="请输入验证消息"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <el-button @click="showAddForm = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="confirmAddFriend"
          :loading="addingFriend"
        >
          发送请求
        </el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { contactApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { User } from '@/types'

interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'friend-added'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const authStore = useAuthStore()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const searchKeyword = ref('')
const searchResults = ref<User[]>([])
const searchLoading = ref(false)
const hasSearched = ref(false)
const addingFriends = ref(new Set<number>())

const showAddForm = ref(false)
const selectedUser = ref<User | null>(null)
const addingFriend = ref(false)

const addFriendForm = reactive({
  message: ''
})

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  searchLoading.value = true
  try {
    const response = await contactApi.searchUsers(searchKeyword.value.trim())
    searchResults.value = response.data || []
    hasSearched.value = true
    
    if (searchResults.value.length === 0) {
      ElMessage.info('没有找到相关用户')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '搜索失败')
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

const showAddFriendForm = (user: User) => {
  selectedUser.value = user
  addFriendForm.message = `我是${authStore.userName}，请通过我的好友申请。`
  showAddForm.value = true
}

const confirmAddFriend = async () => {
  if (!selectedUser.value) return

  addingFriend.value = true
  try {
    await contactApi.addFriend(
      selectedUser.value.id.toString(), 
      addFriendForm.message
    )
    
    ElMessage.success('好友请求发送成功')
    addingFriends.value.add(selectedUser.value.id)
    showAddForm.value = false
    emit('friend-added')
    
    // 3秒后移除添加状态
    setTimeout(() => {
      if (selectedUser.value) {
        addingFriends.value.delete(selectedUser.value.id)
      }
    }, 3000)
    
  } catch (error: any) {
    ElMessage.error(error.message || '发送好友请求失败')
  } finally {
    addingFriend.value = false
  }
}

const getUserAvatar = (user: User) => {
  if (user.avatar && user.avatar.startsWith('avatar_')) {
    return `http://localhost:8080/api/user/avatar/${user.id}`
  }
  return user.avatar || 'https://avatars.githubusercontent.com/u/0?v=4'
}

const handleClose = () => {
  searchKeyword.value = ''
  searchResults.value = []
  hasSearched.value = false
  addingFriends.value.clear()
  showAddForm.value = false
  selectedUser.value = null
  visible.value = false
}
</script>

<style scoped>
.add-friend-dialog {
  min-height: 300px;
}

.search-section {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.search-input {
  flex: 1;
}

.section-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.search-results {
  max-height: 400px;
  overflow-y: auto;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  transition: background-color 0.2s;
  gap: 12px;
}

.user-item:hover {
  background-color: #f8f9fa;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-weight: 500;
  font-size: 16px;
  color: #333;
  margin-bottom: 4px;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.username {
  font-size: 14px;
  color: #666;
}

.signature {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.tips {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
  color: #666;
  font-size: 14px;
  margin-top: 20px;
}

.tips .el-icon {
  color: #1890ff;
}

/* 添加好友表单 */
.add-friend-form {
  padding: 16px 0;
}

.user-preview {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.user-preview .user-info {
  flex: 1;
}

.user-preview .user-name {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.user-preview .username {
  font-size: 14px;
  color: #666;
}

/* 滚动条样式 */
.search-results::-webkit-scrollbar {
  width: 4px;
}

.search-results::-webkit-scrollbar-track {
  background: transparent;
}

.search-results::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.search-results::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>