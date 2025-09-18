import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import LoginView from './views/LoginView.vue'
import ChatView from './views/ChatView.vue'
import DebugView from './views/DebugView.vue'
import WebSocketTestView from './views/WebSocketTestView.vue'
import FriendChatTest from './components/FriendChatTest.vue'

// 移动端页面
import MobileMessagesView from './views/MobileMessagesView.vue'
import MobileContactsView from './views/MobileContactsView.vue'
import MobileGroupsView from './views/MobileGroupsView.vue'
import MobileProfileView from './views/MobileProfileView.vue'
import MobileContactDetailView from './views/MobileContactDetailView.vue'
import MobileGroupDetailView from './views/MobileGroupDetailView.vue'
import MobileChatView from './views/MobileChatView.vue'

import { useAuthStore } from './stores/auth'
import './style.css'

const routes = [
  {
    path: '/',
    redirect: () => {
      // 检查是否已登录，决定重定向到哪里
      const savedUser = localStorage.getItem('authUser')
      return savedUser ? '/chat' : '/login'
    }
  },
  {
    path: '/login',
    component: LoginView,
    meta: { requiresGuest: true }
  },
  {
    path: '/register', 
    component: LoginView,
    meta: { requiresGuest: true }
  },
  {
    path: '/chat',
    component: ChatView,
    meta: { requiresAuth: true }
  },
  // 移动端路由
  {
    path: '/mobile/messages',
    component: MobileMessagesView,
    meta: { requiresAuth: true }
  },
  {
    path: '/mobile/contacts',
    component: MobileContactsView,
    meta: { requiresAuth: true }
  },
  {
    path: '/mobile/groups',
    component: MobileGroupsView,
    meta: { requiresAuth: true }
  },
  {
    path: '/mobile/profile',
    component: MobileProfileView,
    meta: { requiresAuth: true }
  },
  {
    path: '/mobile/contact/:id',
    component: MobileContactDetailView,
    meta: { requiresAuth: true }
  },
  {
    path: '/mobile/group/:id',
    component: MobileGroupDetailView,
    meta: { requiresAuth: true }
  },
  {
    path: '/mobile/chat/:type/:id',
    component: MobileChatView,
    meta: { requiresAuth: true }
  },
  {
    path: '/debug',
    component: DebugView,
    meta: { requiresAuth: false }
  },
  {
    path: '/websocket-test',
    component: WebSocketTestView,
    meta: { requiresAuth: true }
  },
  {
    path: '/friend-chat-test',
    component: FriendChatTest,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to: any, from: any, next: any) => {
  // 从本地存储检查登录状态
  const savedUser = localStorage.getItem('authUser')
  const isLoggedIn = !!savedUser
  
  console.log('路由守卫检查:', {
    to: to.path,
    from: from.path,
    isLoggedIn,
    requiresAuth: to.meta?.requiresAuth,
    requiresGuest: to.meta?.requiresGuest
  })
  
  // 需要认证的页面
  if (to.meta?.requiresAuth && !isLoggedIn) {
    console.log('需要认证但未登录，重定向到登录页')
    next('/login')
    return
  }
  
  // 仅游客可访问的页面（如登录页）
  if (to.meta?.requiresGuest && isLoggedIn) {
    console.log('已登录用户访问游客页面，重定向到聊天页')
    next('/chat')
    return
  }
  
  next()
})

const app = createApp(App)
const pinia = createPinia()

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 应用启动后恢复认证状态
const authStore = useAuthStore()
const isRestored = authStore.restoreAuth()

// 如果成功恢复认证状态，初始化WebSocket连接
if (isRestored) {
  try {
    const { getWebSocketService } = await import('./services/websocket')
    const wsService = getWebSocketService()
    wsService.connect()
  } catch (error) {
    console.warn('WebSocket初始化失败:', error)
  }
}

app.mount('#app')