<template>
  <div class="group-detail-panel">
    <div class="detail-header">
      <h3>群聊详情</h3>
      <div class="header-actions">
        <el-dropdown @command="handleMoreAction" trigger="click">
          <el-button circle>
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="announcement" v-if="canManage">
                <el-icon><Bell /></el-icon>
                群公告
              </el-dropdown-item>
              <el-dropdown-item command="settings" v-if="canManage">
                <el-icon><Setting /></el-icon>
                群设置
              </el-dropdown-item>
              <el-dropdown-item command="dissolve" v-if="isOwner" divided>
                <el-icon><Delete /></el-icon>
                解散群聊
              </el-dropdown-item>
              <el-dropdown-item command="leave" v-else divided>
                <el-icon><Close /></el-icon>
                退出群聊
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div class="detail-content" v-if="group">
      <!-- 群基本信息 -->
      <div class="group-info-section">
        <div class="avatar-section">
          <div class="avatar-with-upload">
            <el-avatar :src="getGroupAvatar(group)" :size="80">
              {{ (group?.name || group?.groupName)?.charAt(0) }}
            </el-avatar>
            <el-upload
              v-if="canManage"
              class="avatar-upload-btn"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :on-success="handleAvatarSuccess"
              accept="image/*"
            >
              <el-button size="small" circle>
                <el-icon><Edit /></el-icon>
              </el-button>
            </el-upload>
          </div>
        </div>
        
        <div class="info-section">
          <div class="info-item">
            <label>群名称:</label>
            <span>{{ group.name || group.groupName }}</span>
          </div>
          
          <div class="info-item" v-if="group.description">
            <label>群描述:</label>
            <span class="description">{{ group.description }}</span>
          </div>
          
          <div class="info-item">
            <label>群主:</label>
            <span>{{ getOwnerName() }}</span>
          </div>
          
          <div class="info-item">
            <label>群备注:</label>
            <div class="remark-container">
              <el-input
                v-model="groupRemark"
                placeholder="请输入群备注"
                size="small"
                style="width: 200px"
              />
            </div>
          </div>
          
          <div class="info-item">
            <label>我在本群昵称:</label>
            <div class="nickname-container">
              <el-input
                v-model="myNickname"
                placeholder="请输入群内昵称"
                size="small"
                style="width: 200px"
              />
            </div>
          </div>
          
          <div class="info-item">
            <label>群公告:</label>
            <div class="announcement-container">
              <el-input
                v-model="groupAnnouncement"
                type="textarea"
                :rows="3"
                :placeholder="isOwner ? '请输入群公告' : '群主未设置'"
                :disabled="!isOwner"
                resize="none"
                show-word-limit
                maxlength="500"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button 
          type="primary" 
          @click="startGroupChat"
          icon="ChatDotRound"
        >
          开始聊天
        </el-button>
        <el-button 
          type="success" 
          @click="handleSaveChanges"
          icon="Check"
          :loading="saving"
        >
          保存修改
        </el-button>
      </div>

      <!-- 管理操作按钮 -->
      <div v-if="canManage" class="management-buttons">
        <el-button @click="showInviteDialog = true" icon="Plus">
          邀请成员
        </el-button>
        <el-button @click="showRemoveDialog = true" icon="Minus">
          移除成员
        </el-button>
        <el-button @click="showMuteDialog = true" icon="Mute">
          禁言成员
        </el-button>
        <el-button @click="showUnmuteDialog = true" icon="Microphone">
          解除禁言
        </el-button>
      </div>

      <!-- 普通成员操作 -->
      <div v-else class="member-buttons">
        <el-button @click="showInviteDialog = true" icon="Plus">
          邀请好友
        </el-button>
      </div>

      <!-- 群成员列表 -->
      <div class="members-section">
        <div class="section-title">
          群成员 ({{ members.length }}/{{ group.maxMembers }})
        </div>
        <div class="members-list">
          <div
            v-for="member in members"
            :key="member.id"
            class="member-item"
          >
            <el-avatar :src="getMemberAvatar(member)" :size="40" />
            <div class="member-info">
              <div class="member-name">
                {{ member.memberNickname || member.nickname || member.username }}
                <el-tag v-if="member.memberRole === 3" type="danger" size="small">群主</el-tag>
                <el-tag v-else-if="member.memberRole === 2" type="warning" size="small">管理员</el-tag>
              </div>
              <div class="member-status">
                <span v-if="member.muteUntil" class="muted-status">已禁言</span>
                <span v-else class="normal-status">正常</span>
              </div>
            </div>
            <div class="member-actions" v-if="canManageMember(member)">
              <el-dropdown @command="(cmd: string) => handleMemberAction(cmd, member)" trigger="click">
                <el-button size="small" circle>
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="promote" v-if="isOwner && member.memberRole === 1">
                      设为管理员
                    </el-dropdown-item>
                    <el-dropdown-item command="demote" v-if="isOwner && member.memberRole === 2">
                      取消管理员
                    </el-dropdown-item>
                    <el-dropdown-item command="mute" v-if="!member.muteUntil">
                      禁言
                    </el-dropdown-item>
                    <el-dropdown-item command="unmute" v-if="member.muteUntil">
                      解除禁言
                    </el-dropdown-item>
                    <el-dropdown-item command="remove" divided>
                      移除群聊
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="detail-content" v-else>
      <el-empty description="请选择群聊查看详情" />
    </div>

    <!-- 各种对话框 -->
    <EditNicknameDialog 
      v-model="showEditNicknameDialog"
      :current-nickname="currentMember?.nickname || ''"
      @confirm="handleNicknameUpdate"
    />

    <InviteMemberDialog
      v-model="showInviteDialog"
      :group-id="group?.id"
      @success="handleMembersUpdated"
    />

    <RemoveMemberDialog
      v-model="showRemoveDialog"
      :group-id="group?.id"
      :members="members"
      @success="handleMembersUpdated"
    />

    <MuteMemberDialog
      v-model="showMuteDialog"
      :group-id="group?.id"
      :members="members.filter(m => !m.isMuted)"
      @success="handleMembersUpdated"
    />

    <UnmuteMemberDialog
      v-model="showUnmuteDialog"
      :group-id="group?.id"
      :members="members.filter(m => m.isMuted)"
      @success="handleMembersUpdated"
    />

    <DissolveGroupDialog
      v-model="showDissolveDialog"
      :group="group"
      @success="handleGroupDissolved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Bell, Setting, Delete, Close, ChatDotRound, Plus, Minus, Mute, Microphone } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { groupApi } from '@/api'
import EditNicknameDialog from './groups/dialogs/EditNicknameDialog.vue'
import InviteMemberDialog from './groups/dialogs/InviteMemberDialog.vue'
import RemoveMemberDialog from './groups/dialogs/RemoveMemberDialog.vue'
import MuteMemberDialog from './groups/dialogs/MuteMemberDialog.vue'
import UnmuteMemberDialog from './groups/dialogs/UnmuteMemberDialog.vue'
import DissolveGroupDialog from './groups/dialogs/DissolveGroupDialog.vue'
import type { ChatGroup, GroupMember } from '@/types'

// Props & Emits
const props = defineProps<{
  group: ChatGroup | null
}>()

const emit = defineEmits<{
  startChat: [group: ChatGroup]
  groupUpdated: [group: ChatGroup]
  groupDissolved: [groupId: number]
}>()

// 响应式数据
const authStore = useAuthStore()
const members = ref<GroupMember[]>([])
const showEditNicknameDialog = ref(false)
const showInviteDialog = ref(false)
const showRemoveDialog = ref(false)
const showMuteDialog = ref(false)
const showUnmuteDialog = ref(false)
const showDissolveDialog = ref(false)
const saving = ref(false)
const groupRemark = ref('')
const myNickname = ref('')
const groupAnnouncement = ref('')

// 计算属性
const currentMember = computed(() => {
  const currentUserId = authStore.userInfo?.id
  console.log('查找当前成员 - 用户ID:', currentUserId, '类型:', typeof currentUserId)
  const member = members.value.find(m => {
    console.log('成员:', m.userId, '类型:', typeof m.userId, '匹配:', m.userId == currentUserId)
    return Number(m.userId) === Number(currentUserId) // 确保数字类型比较
  })
  console.log('找到的当前成员:', member)
  return member
})

// 头像上传相关
const uploadUrl = computed(() => `http://localhost:8080/api/group/${props.group?.id}/avatar`)
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${authStore.currentUser?.token || ''}`
}))

const beforeAvatarUpload = (file: any) => {
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

const handleAvatarSuccess = (response: any) => {
  if (response.code === 200 && response.data) {
    // 更新群组头像
    if (props.group) {
      props.group.avatar = response.data.url || response.data
    }
    ElMessage.success(response.message || '头像上传成功')
  } else {
    ElMessage.error(response.message || '头像上传失败')
  }
}

const isOwner = computed(() => {
  // 优先使用群组数据中的 ownerId 进行判断
  const groupOwnerId = props.group?.ownerId
  const currentUserId = authStore.userInfo?.id
  
  console.log('群主判断 - 群主ID:', groupOwnerId, '当前用户ID:', currentUserId, '类型:', typeof groupOwnerId, typeof currentUserId)
  
  // 使用宽松相等比较，避免类型不匹配问题
  const isGroupOwner = groupOwnerId == currentUserId
  
  // 如果群组数据中有ownerId，优先使用
  if (groupOwnerId !== undefined && groupOwnerId !== null) {
    console.log('基于群组ownerId判断是否为群主:', isGroupOwner)
    return isGroupOwner
  }
  
  // 备用方案：检查成员角色（3表示群主）
  const isMemberOwner = currentMember.value?.memberRole === 3
  console.log('基于成员角色判断是否为群主:', isMemberOwner)
  return isMemberOwner
})

const canManage = computed(() => {
  // 主要根据群组详情中的 ownerId 判断是否可以管理
  const currentUserId = authStore.userInfo?.id
  const groupOwnerId = props.group?.ownerId
  // 使用宽松比较，避免数据类型不匹配问题
  const isOwner = groupOwnerId == currentUserId
  
  // 如果是群主，可以直接管理
  if (isOwner) return true
  
  // 如果不是群主，再检查成员列表中的管理员权限（2表示管理员）
  if (currentMember.value) {
    const memberRole = currentMember.value?.memberRole
    const isAdmin = memberRole === 2
    return isAdmin
  }
  
  return false
})

// 方法
const loadMembers = async () => {
  if (!props.group) return
  
  try {
    const response = await groupApi.getGroupMembers(props.group.id)
    console.log('成员API响应:', response)
    members.value = response.data || []
    console.log('加载的成员列表:', members.value, '当前用户ID:', authStore.userInfo?.id)
    return true
  } catch (error: any) {
    console.error('加载群成员失败:', error)
    ElMessage.error(error.message || '加载群成员失败')
    return false
  }
}

const getGroupAvatar = (group: ChatGroup) => {
  if (group.avatar) {
    if (group.avatar.startsWith('data:image/')) {
      return group.avatar
    }
    return `http://localhost:8080/api/group/avatar/${group.id}`
  }
  return null
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

const getMemberAvatar = (member: GroupMember) => {
  if (!member?.avatar) return 'https://avatars.githubusercontent.com/u/0?v=4'
  
  if (member.avatar.startsWith('avatar_')) {
    return `http://localhost:8080/api/user/avatar/${member.userId}`
  } else if (member.avatar.startsWith('/api/user/avatar/')) {
    return `http://localhost:8080${member.avatar}`
  }
  
  return member.avatar
}

const getOwnerName = () => {
  // 直接从群组信息中获取群主名称
  return props.group?.ownerName || '未知'
}

const canManageMember = (member: GroupMember) => {
  if (!canManage.value) return false
  if (member.userId === authStore.userInfo?.id) return false
  
  if (isOwner.value) return true
  if (currentMember.value?.memberRole === 2 && member.memberRole === 1) return true
  
  return false
}

const startGroupChat = () => {
  if (props.group) {
    emit('startChat', props.group)
  }
}

const handleMoreAction = (command: string) => {
  switch (command) {
    case 'announcement':
      // TODO: 显示群公告对话框
      break
    case 'settings':
      // TODO: 显示群设置对话框
      break
    case 'dissolve':
      showDissolveDialog.value = true
      break
    case 'leave':
      handleLeaveGroup()
      break
  }
}

const handleMemberAction = async (command: string, member: GroupMember) => {
  if (!props.group) return
  
  try {
    switch (command) {
      case 'promote':
        await groupApi.updateMemberRole(props.group.id, member.userId, 2) // 2表示管理员
        ElMessage.success('设置管理员成功')
        break
      case 'demote':
        await groupApi.updateMemberRole(props.group.id, member.userId, 1) // 1表示普通成员
        ElMessage.success('取消管理员成功')
        break
      case 'mute':
        await groupApi.muteMember(props.group.id, member.userId, 24 * 60) // 禁言24小时
        ElMessage.success('禁言成功')
        break
      case 'unmute':
        await groupApi.unmuteMember(props.group.id, member.userId)
        ElMessage.success('解除禁言成功')
        break
      case 'remove':
        await ElMessageBox.confirm(
          `确定要移除成员 "${member.memberNickname || member.nickname || member.username}" 吗？`,
          '移除成员',
          { type: 'warning' }
        )
        await groupApi.removeMember(props.group.id, member.userId)
        ElMessage.success('移除成员成功')
        break
    }
    
    loadMembers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const handleLeaveGroup = async () => {
  if (!props.group) return
  
  try {
    await ElMessageBox.confirm(
      `确定要退出群聊 "${props.group.name || props.group.groupName}" 吗？`,
      '退出群聊',
      { type: 'warning' }
    )
    
    await groupApi.leaveGroup(props.group.id)
    ElMessage.success('已退出群聊')
    // TODO: 通知父组件刷新群聊列表
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '退出群聊失败')
    }
  }
}

const handleNicknameUpdate = async (nickname: string) => {
  if (!props.group) return
  
  try {
    await groupApi.updateMemberNickname(props.group.id, nickname)
    ElMessage.success('昵称修改成功')
    loadMembers()
  } catch (error: any) {
    ElMessage.error(error.message || '修改昵称失败')
  }
}

const handleMembersUpdated = () => {
  loadMembers()
}

const handleGroupDissolved = () => {
  if (props.group) {
    emit('groupDissolved', props.group.id)
  }
}

// 监听群聊变化
watch(() => props.group, async (newGroup) => {
  if (newGroup) {
    await loadMembers()
    // 初始化群备注、群内昵称和群公告
    groupRemark.value = newGroup.remark || newGroup.name || ''
    // 优先使用群组数据中的 myNickname，如果没有则使用当前成员的昵称
    myNickname.value = newGroup.myNickname || currentMember.value?.nickname || ''
    groupAnnouncement.value = newGroup.announcement || ''
  } else {
    members.value = []
    groupRemark.value = ''
    myNickname.value = ''
    groupAnnouncement.value = ''
  }
}, { immediate: true })

// 监听当前成员变化，更新昵称
watch(currentMember, (newMember) => {
  if (newMember) {
    myNickname.value = newMember.memberNickname || ''
  }
})


// 保存修改
const handleSaveChanges = async () => {
  if (!props.group) return
  
  saving.value = true
  try {
    // 准备更新数据
    const updateData: any = {}
    
    // 检查群备注是否有变化
    if (groupRemark.value !== props.group.remark) {
      updateData.remark = groupRemark.value
    }
    
    // 检查群内昵称是否有变化
    if (myNickname.value !== (props.group.myNickname || currentMember.value?.memberNickname)) {
      updateData.nickname = myNickname.value
    }
    
    // 检查群公告是否有变化（只有群主可以修改）
    if (isOwner.value && groupAnnouncement.value !== props.group.announcement) {
      updateData.announcement = groupAnnouncement.value
    }
    
    // 如果没有任何变化，直接返回
    if (Object.keys(updateData).length === 0) {
      ElMessage.info('没有需要保存的修改')
      return
    }
    
    console.log('保存修改:', updateData)
    
    // 调用统一的更新API
    const response = await groupApi.updateGroup(props.group.id, updateData)
    
    console.log('API响应:', response)
    console.log('响应数据:', response.data)
    console.log('响应code:', response.code)
    console.log('响应message:', response.message)
    
    // 检查响应是否成功 - 根据实际的响应结构
    const isSuccess = response.code === 200
    
    if (isSuccess) {
      // 使用响应中的消息
      const message = response.message || '保存成功'
      ElMessage.success(message)
      
      // 更新本地数据
      if (updateData.remark !== undefined) {
        props.group.remark = updateData.remark
      }
      if (updateData.nickname !== undefined) {
        props.group.myNickname = updateData.nickname
      }
      if (updateData.announcement !== undefined) {
        props.group.announcement = updateData.announcement
      }
      
      // 重新加载成员信息以获取最新的昵称
      loadMembers()
    } else {
      ElMessage.error(response.data?.message || '保存失败')
    }
  } catch (error: any) {
    console.error('保存失败:', error)
    ElMessage.error(error.response?.data?.message || error.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.group-detail-panel {
  padding: 20px;
  background: white;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-header h3 {
  margin: 0;
  color: #333;
  font-size: 20px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.detail-content {
  flex: 1;
}

.group-info-section {
  margin-bottom: 24px;
}

.avatar-section {
  text-align: center;
  margin-bottom: 24px;
}

.info-section {
  max-width: 400px;
  margin: 0 auto;
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

.description {
  color: #666;
  line-height: 1.5;
}

.nickname-container {
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

.announcement-container {
  flex: 1;
}

.announcement {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 8px;
  color: #666;
  line-height: 1.5;
  border-left: 4px solid #1890ff;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 24px;
}

.management-buttons,
.member-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.members-section {
  border-top: 1px solid #f0f0f0;
  padding-top: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 16px;
}

.members-list {
  max-height: 300px;
  overflow-y: auto;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  transition: background-color 0.2s;
  gap: 12px;
}

.member-item:hover {
  background-color: #f8f9fa;
}

.member-info {
  flex: 1;
}

.member-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
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

.member-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.member-item:hover .member-actions {
  opacity: 1;
}

/* 滚动条样式 */
.group-detail-panel::-webkit-scrollbar,
.members-list::-webkit-scrollbar {
  width: 4px;
}

.group-detail-panel::-webkit-scrollbar-track,
.members-list::-webkit-scrollbar-track {
  background: transparent;
}

.group-detail-panel::-webkit-scrollbar-thumb,
.members-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.group-detail-panel::-webkit-scrollbar-thumb:hover,
.members-list::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}

/* 头像上传样式 */
.avatar-with-upload {
  position: relative;
  display: inline-block;
}

.avatar-upload-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  transform: translate(50%, 50%);
}

.avatar-upload-btn .el-button {
  background-color: #409eff;
  color: white;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.avatar-upload-btn .el-button:hover {
  background-color: #66b1ff;
}
</style>