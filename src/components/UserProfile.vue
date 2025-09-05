<template>
  <el-dropdown trigger="click" @command="handleCommand" v-if="authStore.isLoggedIn && authStore.userInfo">
    <div class="user-profile">
      <el-avatar :src="authStore.userAvatar" :size="size" />
      <div v-if="showDetails" class="user-details">
        <div class="username">{{ authStore.userName }}</div>
        <div class="status">
          <el-tag :type="statusType" size="small">
            {{ statusText }}
          </el-tag>
        </div>
      </div>
      <el-icon v-if="showArrow" class="dropdown-arrow">
        <ArrowDown />
      </el-icon>
    </div>
    
    <template #dropdown>
      <el-dropdown-menu>
        <!-- 状态切换 -->
        <el-dropdown-item divided>
          <span class="menu-title">设置状态</span>
        </el-dropdown-item>
        <el-dropdown-item 
          command="status:online"
          :class="{ active: currentStatus === 'online' }"
        >
          <el-icon color="#52c41a"><CircleCheck /></el-icon>
          在线
        </el-dropdown-item>
        <el-dropdown-item 
          command="status:away"
          :class="{ active: currentStatus === 'away' }"
        >
          <el-icon color="#faad14"><Clock /></el-icon>
          离开
        </el-dropdown-item>
        <el-dropdown-item 
          command="status:offline"
          :class="{ active: currentStatus === 'offline' }"
        >
          <el-icon color="#999"><CircleClose /></el-icon>
          离线
        </el-dropdown-item>

        <!-- 账号切换 -->
        <el-dropdown-item divided v-if="switchableAccounts.length > 0">
          <span class="menu-title">切换账号</span>
        </el-dropdown-item>
        <el-dropdown-item
          v-for="account in switchableAccounts"
          :key="account.id"
          :command="`switch:${account.id}`"
          class="account-item"
        >
          <el-avatar :src="account.avatar" :size="24" />
          <div class="account-info">
            <div class="account-name">{{ account.name }}</div>
            <div class="account-username">@{{ account.username }}</div>
          </div>
        </el-dropdown-item>

        <!-- 添加账号 -->
        <el-dropdown-item command="add-account" divided>
          <el-icon><Plus /></el-icon>
          添加账号
        </el-dropdown-item>

        <!-- 退出登录 -->
        <el-dropdown-item command="logout" divided>
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>

  <!-- 切换账号确认对话框 -->
  <el-dialog
    v-model="showSwitchDialog"
    title="切换账号"
    width="400px"
    :before-close="handleDialogClose"
  >
    <div class="switch-dialog-content">
      <div class="current-account">
        <el-avatar :src="authStore.userAvatar" :size="48" />
        <div class="account-details">
          <div class="name">{{ authStore.userName }}</div>
          <div class="status-text">当前账号</div>
        </div>
      </div>
      
      <el-icon class="switch-arrow"><ArrowRight /></el-icon>
      
      <div class="target-account" v-if="targetAccount">
        <el-avatar :src="targetAccount.avatar" :size="48" />
        <div class="account-details">
          <div class="name">{{ targetAccount.name }}</div>
          <div class="status-text">切换到此账号</div>
        </div>
      </div>
    </div>
    
    <template #footer>
      <el-button @click="showSwitchDialog = false">取消</el-button>
      <el-button 
        type="primary" 
        @click="confirmSwitch"
        :loading="authStore.isLoading"
      >
        {{ authStore.isLoading ? '切换中...' : '确认切换' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'

interface Props {
  size?: number
  showDetails?: boolean
  showArrow?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 40,
  showDetails: true,
  showArrow: true
})

const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()

const showSwitchDialog = ref(false)
const targetAccount = ref<any>(null)

const currentStatus = computed(() => authStore.userInfo?.status || 'offline')

const statusType = computed(() => {
  switch (currentStatus.value) {
    case 'online': return 'success'
    case 'away': return 'warning'
    case 'offline': return 'danger'
    default: return 'info'
  }
})

const statusText = computed(() => {
  switch (currentStatus.value) {
    case 'online': return '在线'
    case 'away': return '离开'
    case 'offline': return '离线'
    default: return '未知'
  }
})

const switchableAccounts = computed(() => {
  return authStore.getSwitchableAccounts()
})

const handleCommand = async (command: string) => {
  if (command.startsWith('status:')) {
    const status = command.split(':')[1] as 'online' | 'offline' | 'away'
    authStore.updateUserStatus(status)
    ElMessage.success(`状态已切换为${statusText.value}`)
  } else if (command.startsWith('switch:')) {
    const accountId = command.split(':')[1]
    const account = switchableAccounts.value.find(acc => acc.id === accountId)
    if (account) {
      targetAccount.value = account
      showSwitchDialog.value = true
    }
  } else if (command === 'add-account') {
    await router.push('/login')
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm(
        '确定要退出当前账号吗？',
        '退出登录',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }
      )
      
      authStore.logout()
      chatStore.clearData()
      ElMessage.success('已退出登录')
      await router.push('/login')
    } catch {
      // 用户取消
    }
  }
}

const confirmSwitch = async () => {
  if (!targetAccount.value) return
  
  const success = await authStore.quickLogin({
    id: targetAccount.value.id,
    username: targetAccount.value.username,
    password: '123456',
    user: {
      id: targetAccount.value.id,
      name: targetAccount.value.name,
      avatar: targetAccount.value.avatar,
      status: 'online',
      email: ''
    }
  })
  
  if (success) {
    // 初始化新用户的聊天数据
    chatStore.initializeUserData(targetAccount.value.id)
    
    ElMessage.success(`已切换到 ${targetAccount.value.name}`)
    showSwitchDialog.value = false
    targetAccount.value = null
  } else if (authStore.error) {
    ElMessage.error(authStore.error)
  }
}

const handleDialogClose = () => {
  if (!authStore.isLoading) {
    showSwitchDialog.value = false
    targetAccount.value = null
  }
}
</script>

<style scoped>
.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 4px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.user-profile:hover {
  background-color: #f8f8f8;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.username {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  margin-top: 4px;
}

.dropdown-arrow {
  color: #999;
  transition: transform 0.2s;
}

.user-profile:hover .dropdown-arrow {
  color: #666;
}

:deep(.el-dropdown-menu) {
  min-width: 200px;
}

.menu-title {
  font-size: 12px;
  color: #999;
  font-weight: 500;
}

.account-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
}

.account-info {
  flex: 1;
  min-width: 0;
}

.account-name {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-username {
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.el-dropdown-menu__item.active) {
  background-color: #e6f4ff;
  color: #1890ff;
}

.switch-dialog-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 0;
}

.current-account,
.target-account {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.account-details {
  flex: 1;
}

.account-details .name {
  font-weight: 500;
  font-size: 16px;
  color: #333;
  margin-bottom: 4px;
}

.account-details .status-text {
  font-size: 12px;
  color: #666;
}

.switch-arrow {
  font-size: 20px;
  color: #ccc;
  margin: 0 16px;
}
</style>