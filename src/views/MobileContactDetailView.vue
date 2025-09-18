<template>
  <div class="mobile-contact-detail-view">
    <!-- 顶部标题栏 -->
    <div class="header">
      <el-icon class="back-icon" @click="goBack"><ArrowLeft /></el-icon>
      <h2>好友详情</h2>
    </div>
    
    <!-- 好友详情内容 -->
    <div class="detail-content" v-if="friend">
      <div class="avatar-section">
        <el-avatar :src="getUserAvatar(friend)" :size="80" />
      </div>
      
      <div class="info-section">
        <div class="info-item">
          <label>用户名:</label>
          <span>{{ friend.username }}</span>
        </div>
        
        <div class="info-item">
          <label>昵称:</label>
          <span>{{ friend.nickname || '' }}</span>
        </div>
        
        <div class="info-item">
          <label>备注:</label>
          <div class="remark-container">
            <span>{{ friend.remark || '' }}</span>
            <el-icon class="edit-icon" @click="showEditRemarkDialog(friend)">
              <Edit />
            </el-icon>
          </div>
        </div>
        
        <div class="info-item">
          <label>性别:</label>
          <span>{{ friend.gender ? getGenderText(friend.gender) : '' }}</span>
        </div>
        
        <div class="info-item">
          <label>签名:</label>
          <span class="signature">{{ friend.signature || '' }}</span>
        </div>
      </div>
      
      <div class="action-buttons">
        <el-button 
          type="primary" 
          @click="startChatWithContact(friend)"
          icon="ChatDotRound"
          size="large"
          class="action-button"
        >
          发送消息
        </el-button>
        <el-button 
          type="danger" 
          @click="showDeleteConfirm(friend)"
          icon="Delete"
          size="large"
          class="action-button"
        >
          删除好友
        </el-button>
      </div>
    </div>
    
    <div class="detail-content" v-else>
      <el-empty description="好友信息加载中..." />
    </div>
    
    <!-- 修改备注对话框 -->
    <el-dialog
      v-model="showEditRemarkDialogVisible"
      title="修改备注"
      width="90%"
      :before-close="handleDialogClose"
    >
      <el-form :model="remarkForm" label-width="80px">
        <el-form-item label="备注名称">
          <el-input 
            v-model="remarkForm.nickname" 
            placeholder="请输入备注名称"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showEditRemarkDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleUpdateRemark">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import ElMessage from 'element-plus/es/components/message/index.mjs'
import ElMessageBox from 'element-plus/es/components/message-box/index.mjs'
import { contactApi } from '@/api'
import { useChatStore } from '@/stores/chat'
import { getUserAvatarUrl } from '@/utils/avatar'
import type { User } from '@/types'
import { ArrowLeft, Edit } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const chatStore = useChatStore()
const friend = ref<User | null>(null)
const showEditRemarkDialogVisible = ref(false)
const remarkForm = ref({
  friendId: 0,
  nickname: ''
})

// 获取好友详情
const loadFriendDetail = async (friendId: string) => {
  try {
    // 使用contacts API获取联系人详情
    const response = await contactApi.getContacts()
    // 在联系人列表中找到指定的好友
    if (Array.isArray(response.data)) {
      const foundFriend = response.data.find((contact: any) => contact.id.toString() === friendId)
      if (foundFriend) {
        friend.value = foundFriend
      }
    }
  } catch (error: any) {
    console.error('加载好友详情失败:', error)
    ElMessage.error(error.message || '加载好友详情失败')
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 获取用户头像
const getUserAvatar = (user?: User) => {
  return getUserAvatarUrl(user)
}

// 开始与好友聊天
const startChatWithContact = async (contact: User) => {
  // 检查用户登录状态和必要信息
  if (!chatStore.currentUser?.id) {
    console.error('用户未登录或用户信息缺失')
    return
  }
  
  try {
    // 开始与选中联系人的对话
    const conversationId = chatStore.startConversation(contact.id.toString())
    
    if (conversationId) {
      // 设置活动对话
      await chatStore.setActiveConversation(conversationId)
      
      // 跳转到聊天页面
      router.push(`/mobile/chat/user/${contact.id}`)
    } else {
      console.error('无法创建或找到对话')
    }
  } catch (error) {
    console.error('开始聊天失败:', error)
    ElMessage.error('开始聊天失败')
  }
}

// 显示删除好友确认对话框
const showDeleteConfirm = (friend: User) => {
  ElMessageBox.confirm(
    `确定要删除好友 "${friend.nickname || friend.username}" 吗？`,
    '删除好友',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      // 调用API删除好友
      await contactApi.removeContact(friend.id.toString())
      ElMessage.success('好友删除成功')
      
      // 返回好友列表页面
      router.back()
    } catch (error: any) {
      ElMessage.error(error.message || '删除好友失败')
    }
  }).catch(() => {
    // 用户取消
  })
}

// 获取性别文本
const getGenderText = (gender: number) => {
  switch (gender) {
    case 1: return '男'
    case 2: return '女'
    case 0:
    default: return '未知'
  }
}

// 显示修改备注对话框
const showEditRemarkDialog = (friend: User) => {
  remarkForm.value = {
    friendId: friend.id,
    nickname: friend.remark || friend.nickname || ''
  }
  showEditRemarkDialogVisible.value = true
}

// 处理对话框关闭
const handleDialogClose = (done: () => void) => {
  remarkForm.value = {
    friendId: 0,
    nickname: ''
  }
  done()
}

// 更新备注
const handleUpdateRemark = async () => {
  if (!remarkForm.value.nickname.trim()) {
    ElMessage.warning('请输入备注名称')
    return
  }

  try {
    // 使用配置好的axios实例调用API更新备注
    await contactApi.updateFriendNickname(
      remarkForm.value.friendId.toString(),
      remarkForm.value.nickname.trim()
    )

    ElMessage.success('备注修改成功')
    showEditRemarkDialogVisible.value = false
    
    // 更新本地数据
    if (friend.value && friend.value.id === remarkForm.value.friendId) {
      friend.value.remark = remarkForm.value.nickname.trim()
    }
    
    // 通知聊天存储更新联系人信息
    await chatStore.loadContacts()
  } catch (error: any) {
    ElMessage.error(error.message || '修改备注失败')
  }
}

// 初始化数据
onMounted(() => {
  const friendId = route.params.id as string
  if (friendId) {
    loadFriendDetail(friendId)
  }
})
</script>

<style scoped>
.mobile-contact-detail-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.header {
  padding: 16px;
  background: linear-gradient(180deg, #4A90E2 0%, #357ABD 100%);
  color: white;
  display: flex;
  align-items: center;
  position: relative;
}

.header h2 {
  margin: 0;
  font-size: 18px;
  flex: 1;
  text-align: center;
}

.back-icon {
  font-size: 20px;
  cursor: pointer;
  position: absolute;
  left: 16px;
}

.detail-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  overflow-y: auto;
}

.avatar-section {
  margin-bottom: 24px;
}

.info-section {
  width: 100%;
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.info-item label {
  width: 80px;
  color: #666;
  font-weight: 500;
  flex-shrink: 0;
  text-align: right;
}

.info-item span {
  flex: 1;
  color: #333;
}

.remark-container {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.edit-icon {
  color: #1890ff;
  cursor: pointer;
  font-size: 16px;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.edit-icon:hover {
  opacity: 1;
}

.signature {
  color: #999;
  font-style: italic;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.action-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
}

/* 滚动条样式 */
.detail-content::-webkit-scrollbar {
  width: 4px;
}

.detail-content::-webkit-scrollbar-track {
  background: transparent;
}

.detail-content::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.detail-content::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>