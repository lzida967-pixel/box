<template>
  <div class="mobile-group-detail-view">
    <!-- 顶部标题栏 -->
    <div class="header">
      <el-icon class="back-icon" @click="goBack"><ArrowLeft /></el-icon>
      <h2>群聊详情</h2>
      <el-icon class="add-icon" @click="showCreateGroupDialog = true"><Plus /></el-icon>
    </div>
    
    <!-- 群聊详情内容 -->
    <div class="detail-content" v-if="group">
      <div class="avatar-section">
        <el-avatar :src="getGroupAvatar(group)" :size="80">
          {{ (group.remark || group.name).charAt(0) }}
        </el-avatar>
      </div>
      
      <div class="info-section">
        <div class="info-item">
          <label>群名称:</label>
          <span>{{ group.name }}</span>
        </div>
        
        <div class="info-item">
          <label>群描述:</label>
          <span>{{ group.description || '暂无描述' }}</span>
        </div>
        
        <div class="info-item">
          <label>群主:</label>
          <span>{{ group.ownerName || '未知' }}</span>
        </div>
        
        <div class="info-item">
          <label>成员数:</label>
          <span>{{ group.memberCount }}人</span>
        </div>
        
        <div class="info-item" v-if="group.announcement">
          <label>公告:</label>
          <span class="announcement">{{ group.announcement }}</span>
        </div>
      </div>
      
      <div class="action-buttons">
        <!-- 顶部两个按钮：开始聊天 + 保存修改 -->
        <el-button 
          type="primary" 
          @click="startGroupChat(group)"
          icon="ChatDotRound"
          size="small"
          class="action-button"
        >
          开始聊天
        </el-button>
        <el-button 
          type="success" 
          @click="handleTopSave"
          icon="Check"
          size="small"
          class="action-button"
        >
          保存修改
        </el-button>

        <!-- 群主管理区：仅群主显示下面四个按钮 -->
        <template v-if="isGroupOwner">
          <div class="owner-actions">
            <el-button plain @click="showInviteDialog = true" icon="Plus" size="small" class="action-button">
              邀请成员
            </el-button>
            <el-button plain @click="showRemoveDialog = true" icon="Remove" size="small" class="action-button">
              移除成员
            </el-button>
            <el-button plain @click="showMuteDialog = true" icon="Mute" size="small" class="action-button">
              禁言成员
            </el-button>
            <el-button plain @click="showUnmuteDialog = true" icon="Microphone" size="small" class="action-button">
              解除禁言
            </el-button>
          </div>

          <el-button 
            type="danger" 
            @click="showDissolveGroupDialog"
            icon="Delete"
            size="small"
            class="action-button"
          >
            解散群聊
          </el-button>
        </template>

        <!-- 非群主：邀请好友 -->
        <el-button 
          v-else
          type="primary" 
          @click="showInviteDialog = true"
          icon="Plus"
          size="small"
          class="action-button"
        >
          邀请好友
        </el-button>
      </div>
    </div>
    
    <div class="detail-content" v-else>
      <el-empty description="群聊信息加载中..." />
    </div>
    
    <!-- 编辑群信息对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑群信息"
      width="90%"
    >
      <el-form :model="groupForm" label-width="80px">
        <el-form-item label="群名称">
          <el-input 
            v-model="groupForm.name" 
            placeholder="请输入群名称"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="群描述">
          <el-input 
            v-model="groupForm.description" 
            type="textarea"
            placeholder="请输入群描述"
            maxlength="200"
            show-word-limit
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" @click="handleUpdateGroup">确定</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 解散群聊确认对话框 -->
    <el-dialog
      v-model="showDissolveDialog"
      title="解散群聊"
      width="90%"
    >
      <div class="dialog-content">
        <p>确定要解散群聊 "{{ group?.name }}" 吗？</p>
        <p class="warning-text">解散后所有成员将被移出群聊，聊天记录将被清空且无法恢复。</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showDissolveDialog = false">取消</el-button>
          <el-button type="danger" @click="handleDissolveGroup">确定解散</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 退出群聊确认对话框 -->
    <el-dialog
      v-model="showLeaveDialog"
      title="退出群聊"
      width="90%"
    >
      <div class="dialog-content">
        <p>确定要退出群聊 "{{ group?.name }}" 吗？</p>
        <p class="warning-text">退出后你将不再接收该群聊的消息，需要群成员重新邀请才能加入。</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showLeaveDialog = false">取消</el-button>
          <el-button type="danger" @click="handleLeaveGroup">确定退出</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 群主管理对话框，仅在按钮触发时展示 -->
    <InviteMemberDialog
      v-if="group"
      v-model="showInviteDialog"
      :group-id="group.id"
    />
    <RemoveMemberDialog
      v-if="group"
      v-model="showRemoveDialog"
      :group-id="group.id"
    />
    <MuteMemberDialog
      v-if="group"
      v-model="showMuteDialog"
      :group-id="group.id"
      :members="[]"
    />
    <UnmuteMemberDialog
      v-if="group"
      v-model="showUnmuteDialog"
      :group-id="group.id"
      :members="[]"
    />

    <!-- 创建群聊对话框 -->
    <CreateGroupDialog
      v-model="showCreateGroupDialog"
      @created="handleGroupCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { groupApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import type { ChatGroup } from '@/types'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import InviteMemberDialog from '@/components/groups/dialogs/InviteMemberDialog.vue'
import RemoveMemberDialog from '@/components/groups/dialogs/RemoveMemberDialog.vue'
import MuteMemberDialog from '@/components/groups/dialogs/MuteMemberDialog.vue'
import UnmuteMemberDialog from '@/components/groups/dialogs/UnmuteMemberDialog.vue'
import CreateGroupDialog from '@/components/groups/dialogs/CreateGroupDialog.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const chatStore = useChatStore()
const group = ref<ChatGroup | null>(null)
const showEditDialog = ref(false)
const showDissolveDialog = ref(false)
const showLeaveDialog = ref(false)
const showCreateGroupDialog = ref(false)

// 群主管理弹窗
const showInviteDialog = ref(false)
const showRemoveDialog = ref(false)
const showMuteDialog = ref(false)
const showUnmuteDialog = ref(false)

const groupForm = ref({
  name: '',
  description: ''
})

// 判断当前用户是否为群主
const isGroupOwner = computed(() => {
  return group.value?.ownerId === authStore.userId
})

// 获取群聊详情
const loadGroupDetail = async (groupId: string) => {
  try {
    const response = await groupApi.getGroupDetail(parseInt(groupId))
    const groupDTO = response.data || response
    
    // 转换为前端需要的 ChatGroup 格式
    group.value = {
      id: groupDTO.id,
      name: groupDTO.groupName,
      groupName: groupDTO.groupName,
      description: groupDTO.groupDescription,
      avatar: groupDTO.groupAvatar,
      ownerId: groupDTO.ownerId,
      ownerName: groupDTO.ownerName,
      maxMembers: groupDTO.maxMembers || 200,
      isPrivate: groupDTO.status === 2,
      memberCount: groupDTO.memberCount || 1,
      remark: groupDTO.myRemark || groupDTO.groupName,
      announcement: groupDTO.latestAnnouncement,
      createdAt: groupDTO.createTime ? new Date(groupDTO.createTime).toISOString() : new Date().toISOString(),
      updatedAt: groupDTO.createTime ? new Date(groupDTO.createTime).toISOString() : new Date().toISOString(),
      myNickname: groupDTO.myNickname
    }
    
    // 初始化表单数据
    groupForm.value.name = group.value.name
    groupForm.value.description = group.value.description || ''
  } catch (error: any) {
    console.error('加载群聊详情失败:', error)
    ElMessage.error(error.message || '加载群聊详情失败')
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 获取群头像
const getGroupAvatar = (group: ChatGroup) => {
  // 检查多个可能的头像字段
  const avatar = group.avatar || group.groupAvatar
  
  if (avatar) {
    // 如果是base64格式的图片数据
    if (avatar.startsWith('data:image/')) {
      return avatar
    }
  }
  
  // 返回null让el-avatar显示fallback
  return null
}

// 开始群聊
const startGroupChat = async (group: ChatGroup) => {
  // 检查用户登录状态
  if (!authStore.isLoggedIn || !authStore.userId) {
    ElMessage.error('请先登录')
    return
  }
  
  try {
    // 创建群聊会话ID（使用group_前缀区分群聊和私聊）
    const conversationId = `group_${group.id}`
    
    // 检查是否已存在该群聊会话
    let conversation = chatStore.conversations.find(c => c.id === conversationId)
    
    if (!conversation) {
      // 创建新的群聊会话
      conversation = {
        id: conversationId,
        type: 'group',
        participantIds: [authStore.userId.toString()],
        name: group.name || group.groupName || `群聊 ${group.id}`,
        avatar: group.avatar || group.groupAvatar || '',
        description: group.description || group.groupDescription || '',
        memberCount: group.memberCount || 1,
        ownerId: group.ownerId,
        lastMessage: undefined,
        unreadCount: 0,
        timestamp: new Date(),
        // 保存完整的群组信息，以便在界面上显示
        groupInfo: group
      }
      
      // 添加到会话列表
      chatStore.conversations.unshift(conversation)
      chatStore.messages[conversationId] = [] // 初始化消息数组
    }
    
    // 设置为活动会话
    await chatStore.setActiveConversation(conversationId)
    
    // 跳转到聊天页面
    router.push(`/mobile/chat/group/${group.id}`)
  } catch (error) {
    console.error('开始群聊失败:', error)
    ElMessage.error('开始群聊失败')
  }
}

/** 顶部“保存修改”：群主打开编辑，非群主提示无权限 */
const handleTopSave = () => {
  if (isGroupOwner.value) {
    showEditDialog.value = true
  } else {
    ElMessage.warning('仅群主可修改群信息')
  }
}

// 显示编辑群信息对话框
const showEditGroupDialog = () => {
  showEditDialog.value = true
}

// 显示解散群聊对话框
const showDissolveGroupDialog = () => {
  showDissolveDialog.value = true
}

// 显示退出群聊对话框
const showLeaveGroupDialog = () => {
  showLeaveDialog.value = true
}

// 处理解散群聊
const handleDissolveGroup = async () => {
  if (!group.value) return
  
  try {
    await groupApi.dissolveGroup(group.value.id)
    ElMessage.success('群聊已解散')
    showDissolveDialog.value = false
    // 返回群聊列表页面
    router.back()
  } catch (error: any) {
    ElMessage.error(error.message || '解散群聊失败')
  }
}

// 处理退出群聊
const handleLeaveGroup = async () => {
  if (!group.value) return
  
  try {
    await groupApi.leaveGroup(group.value.id)
    ElMessage.success('已退出群聊')
    showLeaveDialog.value = false
    // 返回群聊列表页面
    router.back()
  } catch (error: any) {
    ElMessage.error(error.message || '退出群聊失败')
  }
}

// 处理更新群信息
const handleUpdateGroup = async () => {
  if (!group.value) return
  
  try {
    await groupApi.updateGroup(group.value.id, {
      groupName: groupForm.value.name,
      groupDescription: groupForm.value.description
    })
    
    ElMessage.success('群信息更新成功')
    showEditDialog.value = false
    
    // 更新本地数据
    if (group.value) {
      group.value.name = groupForm.value.name
      group.value.description = groupForm.value.description
    }
  } catch (error: any) {
    ElMessage.error(error.message || '更新群信息失败')
  }
}

// 处理群聊创建成功
const handleGroupCreated = (newGroup: any) => {
  ElMessage.success('群聊创建成功')
  // 可以选择跳转到新创建的群聊详情页
  // router.push(`/mobile/group/${newGroup.id}`)
}

// 初始化数据
onMounted(() => {
  const groupId = route.params.id as string
  if (groupId) {
    loadGroupDetail(groupId)
  }
})
</script>

<style scoped>
.mobile-group-detail-view {
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

.add-icon {
  font-size: 20px;
  cursor: pointer;
  position: absolute;
  right: 16px;
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
  align-items: flex-start;
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

.announcement {
  color: #999;
  font-style: italic;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.owner-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.action-button {
  width: 100%;
  height: 40px;
  font-size: 14px;
}

.dialog-content {
  padding: 20px;
}

.warning-text {
  color: #f56c6c;
  font-size: 14px;
  margin-top: 10px;
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