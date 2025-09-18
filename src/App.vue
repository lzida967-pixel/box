<template>
  <div class="app">
    <!-- 桌面端布局 -->
    <div v-if="!isMobile" class="desktop-layout">
      <router-view />
    </div>
    
    <!-- 移动端布局 -->
    <div v-else class="mobile-layout">
      <router-view />
      
      <!-- 底部导航栏 -->
      <div class="mobile-navbar">
        <div 
          class="nav-item" 
          :class="{ active: activeMobileTab === 'messages' }"
          @click="navigateTo('/mobile/messages')"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span>消息</span>
        </div>
        <div 
          class="nav-item" 
          :class="{ active: activeMobileTab === 'contacts' }"
          @click="navigateTo('/mobile/contacts')"
        >
          <el-icon><User /></el-icon>
          <span>好友</span>
        </div>
        <div 
          class="nav-item" 
          :class="{ active: activeMobileTab === 'groups' }"
          @click="navigateTo('/mobile/groups')"
        >
          <el-icon><UserFilled /></el-icon>
          <span>群聊</span>
        </div>
        <div 
          class="nav-item" 
          :class="{ active: activeMobileTab === 'profile' }"
          @click="navigateTo('/mobile/profile')"
        >
          <el-icon><User /></el-icon>
          <span>我的</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ChatDotRound, User, UserFilled } from '@element-plus/icons-vue'

// App 根组件
const router = useRouter()
const route = useRoute()
const isMobile = ref(false)
const activeMobileTab = ref('messages')

// 检测是否为移动端
const checkIsMobile = () => {
  const userAgent = navigator.userAgent
  const mobileRegex = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i
  isMobile.value = mobileRegex.test(userAgent) || window.innerWidth <= 768
}

// 导航到指定路径
const navigateTo = (path: string) => {
  router.push(path)
}

// 根据当前路由设置活动的导航项
const setActiveTabFromRoute = () => {
  if (route.path.includes('/mobile/messages')) {
    activeMobileTab.value = 'messages'
  } else if (route.path.includes('/mobile/contacts')) {
    activeMobileTab.value = 'contacts'
  } else if (route.path.includes('/mobile/groups')) {
    activeMobileTab.value = 'groups'
  } else if (route.path.includes('/mobile/profile')) {
    activeMobileTab.value = 'profile'
  }
}

onMounted(() => {
  checkIsMobile()
  setActiveTabFromRoute()
  
  // 监听路由变化
  router.afterEach(() => {
    setActiveTabFromRoute()
  })
  
  // 监听窗口大小变化
  window.addEventListener('resize', checkIsMobile)
})

// 清理事件监听器
// onUnmounted(() => {
//   window.removeEventListener('resize', checkIsMobile)
// })
</script>

<style scoped>
.app {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.desktop-layout {
  height: 100%;
}

.mobile-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding-bottom: 60px; /* 为底部导航栏留出空间 */
}

.mobile-navbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(180deg, #4A90E2 0%, #357ABD 100%);
  display: flex;
  justify-content: space-around;
  align-items: center;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  height: 100%;
  font-size: 12px;
  transition: all 0.2s;
}

.nav-item.active {
  color: white;
}

.nav-item .el-icon {
  font-size: 20px;
  margin-bottom: 4px;
}

.nav-item:hover {
  color: white;
}
</style>