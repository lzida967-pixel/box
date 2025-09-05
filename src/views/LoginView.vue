<template>
  <div class="login-container">
    <div class="login-background">
      <div class="bg-pattern"></div>
    </div>
    
    <div class="login-box">
      <!-- Logo和标题 -->
      <div class="login-header">
        <div class="logo">
          <el-icon size="48" color="#4A90E2">
            <ChatDotRound />
          </el-icon>
        </div>
        <h1 class="title">聊天应用</h1>
        <p class="subtitle">连接你我，沟通无界</p>
      </div>

      <!-- 登录/注册切换 -->
      <div class="auth-tabs">
        <div 
          class="auth-tab"
          :class="{ active: authMode === 'login' }"
          @click="authMode = 'login'"
        >
          登录
        </div>
        <div 
          class="auth-tab"
          :class="{ active: authMode === 'register' }"
          @click="authMode = 'register'"
        >
          注册
        </div>
      </div>

      <!-- 登录表单 -->
      <el-form
        v-if="authMode === 'login'"
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="auth-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="account">
          <el-input
            v-model="loginForm.account"
            placeholder="用户名/邮箱/手机号"
            size="large"
            prefix-icon="User"
            :disabled="authStore.isLoading"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            size="large"
            prefix-icon="Lock"
            show-password
            :disabled="authStore.isLoading"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <div class="form-options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <el-button text type="primary" @click="showForgotPassword = true">
              忘记密码？
            </el-button>
          </div>
        </el-form-item>

        <el-form-item v-if="authStore.error">
          <el-alert
            :title="authStore.error"
            type="error"
            :closable="false"
            show-icon
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="authStore.isLoading"
            @click="handleLogin"
            class="auth-button"
          >
            {{ authStore.isLoading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 注册表单 -->
      <el-form
        v-else
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="auth-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="用户名"
            size="large"
            prefix-icon="User"
            :disabled="isRegistering"
          />
        </el-form-item>
        
        <el-form-item prop="displayName">
          <el-input
            v-model="registerForm.displayName"
            placeholder="显示名称"
            size="large"
            prefix-icon="UserFilled"
            :disabled="isRegistering"
          />
        </el-form-item>
        
        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="邮箱(可选)"
            size="large"
            prefix-icon="Message"
            :disabled="isRegistering"
          />
        </el-form-item>
        
        <el-form-item>
          <el-input
            v-model="registerForm.phone"
            placeholder="手机号(可选)"
            size="large"
            prefix-icon="Phone"
            :disabled="isRegistering"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="密码"
            size="large"
            prefix-icon="Lock"
            show-password
            :disabled="isRegistering"
          />
        </el-form-item>
        
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="确认密码"
            size="large"
            prefix-icon="Lock"
            show-password
            :disabled="isRegistering"
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="agreeTerms">
            我已阅读并同意
            <el-button text type="primary">《用户协议》</el-button>
            和
            <el-button text type="primary">《隐私政策》</el-button>
          </el-checkbox>
        </el-form-item>

        <el-form-item v-if="registerError">
          <el-alert
            :title="registerError"
            type="error"
            :closable="false"
            show-icon
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="isRegistering"
            @click="handleRegister"
            class="auth-button"
          >
            {{ isRegistering ? '注册中...' : '注册' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 快速登录 -->
      <div class="quick-login" v-if="authMode === 'login'">
        <div class="divider">
          <span>或</span>
        </div>
        
        <div class="quick-accounts">
          <div class="quick-title">快速登录</div>
          <div class="account-avatars">
            <div
              v-for="account in availableAccounts.slice(0, 4)"
              :key="account.id"
              class="quick-account"
              @click="quickLogin(account)"
              :title="account.name"
            >
              <el-avatar :src="account.avatar" :size="48" />
              <div class="account-name">{{ account.name }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 忘记密码对话框 -->
    <el-dialog v-model="showForgotPassword" title="找回密码" width="400px">
      <el-form :model="forgotForm">
        <el-form-item label="邮箱">
          <el-input v-model="forgotForm.email" placeholder="请输入注册邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForgotPassword = false">取消</el-button>
        <el-button type="primary" @click="handleForgotPassword">发送验证码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { LoginCredentials, RegisterCredentials } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const authMode = ref('login')
const isRegistering = ref(false)
const registerError = ref('')
const rememberMe = ref(false)
const agreeTerms = ref(false)
const showForgotPassword = ref(false)

const loginForm = reactive<LoginCredentials>({
  account: '',
  password: ''
})

const registerForm = reactive<RegisterCredentials>({
  username: '',
  displayName: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

const forgotForm = reactive({
  email: ''
})

const loginRules = {
  account: [
    { required: true, message: '请输入用户名/邮箱/手机号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20位', trigger: 'blur' }
  ],
  displayName: [
    { required: true, message: '请输入显示名称', trigger: 'blur' },
    { min: 1, max: 50, message: '显示名称长度1-50位', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const availableAccounts = ref(authStore.getAvailableAccounts())

// 账号密码登录
const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      const success = await authStore.login(loginForm)
      if (success) {
        ElMessage.success('登录成功！')
        await router.push('/chat')
      }
    }
  })
}

// 注册
const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  if (!agreeTerms.value) {
    ElMessage.warning('请先同意用户协议和隐私政策')
    return
  }

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      isRegistering.value = true
      registerError.value = ''
      
      try {
        // 调用真实的注册API
        const success = await authStore.register(registerForm)
        
        if (success) {
          // 注册成功，切换到登录模式
          authMode.value = 'login'
          loginForm.account = registerForm.username
          
          // 清空注册表单
          Object.assign(registerForm, {
            username: '',
            displayName: '',
            email: '',
            phone: '',
            password: '',
            confirmPassword: ''
          })
          agreeTerms.value = false
        }
      } catch (error) {
        console.error('注册错误:', error)
        registerError.value = authStore.error || '注册失败，请重试'
      } finally {
        isRegistering.value = false
      }
    }
  })
}

// 快速登录
const quickLogin = async (account: any) => {
  try {
    console.log('快速登录:', account)
    
    // 使用统一的登录方法，确保密码一致
    const credentials: LoginCredentials = {
      account: account.username,
      password: '123456' // 数据库中的统一测试密码
    }

    const success = await authStore.login(credentials)
    if (success) {
      ElMessage.success(`欢迎回来，${account.name}！`)
      await router.push('/chat')
    }
  } catch (error) {
    console.error('快速登录失败:', error)
    ElMessage.error('快速登录失败，请尝试手动登录')
  }
}

// 忘记密码
const handleForgotPassword = () => {
  if (!forgotForm.email) {
    ElMessage.warning('请输入邮箱地址')
    return
  }
  
  ElMessage.success('验证码已发送到您的邮箱')
  showForgotPassword.value = false
  forgotForm.email = ''
}

onMounted(() => {
  authStore.clearError()
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  z-index: 1;
}

.bg-pattern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 20% 80%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255, 119, 198, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 40% 40%, rgba(120, 219, 255, 0.3) 0%, transparent 50%);
}

.login-box {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 420px;
  padding: 20px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  margin-bottom: 16px;
}

.title {
  color: white;
  font-size: 32px;
  font-weight: 600;
  margin: 0 0 8px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.subtitle {
  color: rgba(255, 255, 255, 0.8);
  font-size: 16px;
  margin: 0;
}

.auth-tabs {
  display: flex;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 24px;
  backdrop-filter: blur(10px);
}

.auth-tab {
  flex: 1;
  text-align: center;
  padding: 12px;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 500;
}

.auth-tab.active {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.auth-form {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 32px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.auth-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
}

.quick-login {
  margin-top: 24px;
}

.divider {
  position: relative;
  text-align: center;
  margin: 24px 0;
}

.divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: rgba(255, 255, 255, 0.3);
}

.divider span {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 0 16px;
  font-size: 14px;
}

.quick-accounts {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 24px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.quick-title {
  text-align: center;
  font-weight: 500;
  color: #333;
  margin-bottom: 20px;
}

.account-avatars {
  display: flex;
  justify-content: space-around;
  gap: 16px;
}

.quick-account {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: transform 0.2s;
  padding: 8px;
  border-radius: 12px;
}

.quick-account:hover {
  transform: translateY(-4px);
  background: rgba(116, 75, 162, 0.1);
}

.account-name {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
  text-align: center;
  max-width: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-box {
    padding: 16px;
    max-width: 100%;
  }
  
  .title {
    font-size: 28px;
  }
  
  .subtitle {
    font-size: 14px;
  }
  
  .auth-form {
    padding: 24px;
  }
  
  .quick-accounts {
    padding: 20px;
  }
  
  .account-avatars {
    gap: 12px;
  }
}

/* 动画效果 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-box {
  animation: fadeInUp 0.6s ease-out;
}

/* Element Plus 样式覆盖 */
.auth-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  border: 1px solid #e0e6ed;
  box-shadow: none;
}

.auth-form :deep(.el-input__wrapper:hover) {
  border-color: #4A90E2;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  border-color: #4A90E2;
  box-shadow: 0 0 0 2px rgba(74, 144, 226, 0.2);
}

.auth-form :deep(.el-checkbox__label) {
  color: #666;
  font-size: 14px;
}

.auth-form :deep(.el-button--text) {
  color: #4A90E2;
  padding: 0;
}

.auth-form :deep(.el-button--text:hover) {
  color: #357ABD;
}
</style>