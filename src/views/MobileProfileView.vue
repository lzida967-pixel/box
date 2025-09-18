<template>
  <div class="mobile-profile-view">
    <!-- 顶部标题栏 -->
    <div class="header">
      <h2>我的</h2>
    </div>
    
    <!-- 用户信息区域 -->
    <div class="profile-header">
      <el-avatar :src="userAvatar" :size="80" class="user-avatar" />
      <div class="user-info">
        <div class="username">{{ userInfo?.username }}</div>
        <div class="nickname" v-if="userInfo?.nickname">{{ userInfo?.nickname }}</div>
        <div class="signature" v-if="userInfo?.signature">{{ userInfo?.signature }}</div>
      </div>
    </div>
    
    <!-- 功能菜单 -->
    <div class="menu-list">
      <div class="menu-item" @click="goToSettings">
        <el-icon class="menu-icon"><Setting /></el-icon>
        <span class="menu-text">设置</span>
        <el-icon class="arrow-icon"><ArrowRight /></el-icon>
      </div>
      
      <div class="menu-item" @click="goToAbout">
        <el-icon class="menu-icon"><InfoFilled /></el-icon>
        <span class="menu-text">关于</span>
        <el-icon class="arrow-icon"><ArrowRight /></el-icon>
      </div>
      
      <div class="menu-item" @click="handleLogout">
        <el-icon class="menu-icon"><SwitchButton /></el-icon>
        <span class="menu-text">退出登录</span>
        <el-icon class="arrow-icon"><ArrowRight /></el-icon>
      </div>
    </div>
    
    <!-- 设置对话框 -->
    <SettingsDialog v-model="showSettings" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getUserAvatarUrl } from '@/utils/avatar'
import SettingsDialog from '@/components/SettingsDialog.vue'
import { Setting, InfoFilled, SwitchButton, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const showSettings = ref(false)

// 用户信息
const userInfo = computed(() => authStore.userInfo)

// 用户头像
const userAvatar = computed(() => {
  return getUserAvatarUrl(userInfo.value)
})

// 跳转到设置页面
const goToSettings = () => {
  showSettings.value = true
}

// 跳转到关于页面
const goToAbout = () => {
  ElMessage.info('关于页面功能待实现')
}

// 处理退出登录
const handleLogout = async () => {
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
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.mobile-profile-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.header {
  padding: 16px;
  background: linear-gradient(180deg, #4A90E2 0%, #357ABD 100%);
  color: white;
  text-align: center;
}

.header h2 {
  margin: 0;
  font-size: 18px;
}

.profile-header {
  padding: 30px 20px;
  background: white;
  display: flex;
  align-items: center;
  gap: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.user-avatar {
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.username {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nickname {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.signature {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-list {
  margin-top: 20px;
  background: white;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.menu-item:hover {
  background-color: #f8f9fa;
}

.menu-icon {
  font-size: 18px;
  color: #4A90E2;
  margin-right: 12px;
}

.menu-text {
  flex: 1;
  font-size: 16px;
  color: #333;
}

.arrow-icon {
  color: #ccc;
  font-size: 16px;
}
</style>