<template>
  <div class="debug-container">
    <h1>前端API调试页面</h1>
    
    <!-- 网络测试 -->
    <div class="section">
      <h3>🌐 网络连接测试</h3>
      <el-button @click="testConnection" :loading="testing.connection">
        测试后端连接
      </el-button>
      <div class="output" v-if="results.connection">
        <pre>{{ results.connection }}</pre>
      </div>
    </div>

    <!-- 登录测试 -->
    <div class="section">
      <h3>🔐 登录功能测试</h3>
      <div class="form-row">
        <el-input v-model="loginForm.username" placeholder="用户名" style="width: 200px; margin-right: 10px;" />
        <el-input v-model="loginForm.password" type="password" placeholder="密码" style="width: 200px; margin-right: 10px;" />
        <el-button @click="testLogin" :loading="testing.login">测试登录</el-button>
        <el-button @click="testQuickLogin" type="success">快速登录(zhangsan)</el-button>
      </div>
      <div class="output" v-if="results.login">
        <pre>{{ results.login }}</pre>
      </div>
    </div>

    <!-- 注册测试 -->
    <div class="section">
      <h3>📝 注册功能测试</h3>
      <div class="form-row">
        <el-input v-model="registerForm.username" placeholder="用户名" style="width: 150px; margin-right: 10px;" />
        <el-input v-model="registerForm.nickname" placeholder="昵称" style="width: 150px; margin-right: 10px;" />
        <el-input v-model="registerForm.password" type="password" placeholder="密码" style="width: 150px; margin-right: 10px;" />
        <el-button @click="testRegister" :loading="testing.register">测试注册</el-button>
      </div>
      <div class="output" v-if="results.register">
        <pre>{{ results.register }}</pre>
      </div>
    </div>

    <!-- Pinia Store测试 -->
    <div class="section">
      <h3>🏪 Pinia Store状态测试</h3>
      <el-button @click="testStore">检查Store状态</el-button>
      <el-button @click="testStoreLogin">使用Store登录</el-button>
      <div class="output" v-if="results.store">
        <pre>{{ results.store }}</pre>
      </div>
    </div>

    <!-- 网络请求监控 -->
    <div class="section">
      <h3>📡 网络请求监控</h3>
      <p>请打开浏览器开发者工具的 Network 面板来查看实际的网络请求</p>
      <el-button @click="openDevTools">打开开发者工具指南</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api'

const authStore = useAuthStore()

// 测试状态
const testing = reactive({
  connection: false,
  login: false,
  register: false
})

// 测试结果
const results = reactive({
  connection: '',
  login: '',
  register: '',
  store: ''
})

// 表单数据
const loginForm = reactive({
  username: 'zhangsan',
  password: '123456'
})

const registerForm = reactive({
  username: 'testuser_' + Date.now(),
  nickname: '测试用户',
  password: '123456'
})

// 测试后端连接
const testConnection = async () => {
  testing.connection = true
  results.connection = ''
  
  try {
    const startTime = Date.now()
    results.connection += `[${new Date().toLocaleTimeString()}] 开始测试连接到 http://localhost:8080/api\n`
    
    // 直接使用fetch测试
    const response = await fetch('http://localhost:8080/api/test/hello', {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      }
    })
    
    const endTime = Date.now()
    results.connection += `[${new Date().toLocaleTimeString()}] 响应状态: ${response.status} ${response.statusText}\n`
    results.connection += `[${new Date().toLocaleTimeString()}] 响应时间: ${endTime - startTime}ms\n`
    
    if (response.ok) {
      const text = await response.text()
      results.connection += `[${new Date().toLocaleTimeString()}] 响应内容: ${text}\n`
      results.connection += `[${new Date().toLocaleTimeString()}] ✅ 后端连接正常\n`
    } else {
      results.connection += `[${new Date().toLocaleTimeString()}] ❌ 连接失败\n`
    }
  } catch (error: any) {
    results.connection += `[${new Date().toLocaleTimeString()}] ❌ 网络错误: ${error.message}\n`
    results.connection += `[${new Date().toLocaleTimeString()}] 可能的原因:\n`
    results.connection += `   1. 后端服务没有启动\n`
    results.connection += `   2. 端口8080被占用\n`
    results.connection += `   3. CORS跨域问题\n`
  } finally {
    testing.connection = false
  }
}

// 测试登录API
const testLogin = async () => {
  testing.login = true
  results.login = ''
  
  try {
    results.login += `[${new Date().toLocaleTimeString()}] 开始测试登录API\n`
    results.login += `[${new Date().toLocaleTimeString()}] 用户名: ${loginForm.username}\n`
    results.login += `[${new Date().toLocaleTimeString()}] 请求数据: ${JSON.stringify({
      username: loginForm.username,
      password: loginForm.password
    }, null, 2)}\n`
    
    // 使用authApi调用
    const response = await authApi.login({
      username: loginForm.username,
      password: loginForm.password
    })
    
    results.login += `[${new Date().toLocaleTimeString()}] ✅ 登录成功!\n`
    results.login += `[${new Date().toLocaleTimeString()}] 响应数据: ${JSON.stringify(response, null, 2)}\n`
    
    ElMessage.success('登录测试成功!')
  } catch (error: any) {
    results.login += `[${new Date().toLocaleTimeString()}] ❌ 登录失败: ${error.message}\n`
    if (error.response) {
      results.login += `[${new Date().toLocaleTimeString()}] 响应状态: ${error.response.status}\n`
      results.login += `[${new Date().toLocaleTimeString()}] 响应数据: ${JSON.stringify(error.response.data, null, 2)}\n`
    }
    ElMessage.error('登录测试失败!')
  } finally {
    testing.login = false
  }
}

// 快速登录测试
const testQuickLogin = async () => {
  loginForm.username = 'zhangsan'
  loginForm.password = '123456'
  await testLogin()
}

// 测试注册API
const testRegister = async () => {
  testing.register = true
  results.register = ''
  
  try {
    results.register += `[${new Date().toLocaleTimeString()}] 开始测试注册API\n`
    results.register += `[${new Date().toLocaleTimeString()}] 请求数据: ${JSON.stringify({
      username: registerForm.username,
      nickname: registerForm.nickname,
      password: registerForm.password,
      confirmPassword: registerForm.password
    }, null, 2)}\n`
    
    // 使用authApi调用
    const response = await authApi.register({
      username: registerForm.username,
      nickname: registerForm.nickname,
      password: registerForm.password,
      confirmPassword: registerForm.password
    })
    
    results.register += `[${new Date().toLocaleTimeString()}] ✅ 注册成功!\n`
    results.register += `[${new Date().toLocaleTimeString()}] 响应数据: ${JSON.stringify(response, null, 2)}\n`
    
    ElMessage.success('注册测试成功!')
  } catch (error: any) {
    results.register += `[${new Date().toLocaleTimeString()}] ❌ 注册失败: ${error.message}\n`
    if (error.response) {
      results.register += `[${new Date().toLocaleTimeString()}] 响应状态: ${error.response.status}\n`
      results.register += `[${new Date().toLocaleTimeString()}] 响应数据: ${JSON.stringify(error.response.data, null, 2)}\n`
    }
    ElMessage.error('注册测试失败!')
  } finally {
    testing.register = false
  }
}

// 测试Store状态
const testStore = () => {
  results.store = ''
  results.store += `[${new Date().toLocaleTimeString()}] Pinia Auth Store 状态检查:\n`
  results.store += `当前用户: ${authStore.currentUser ? JSON.stringify(authStore.currentUser, null, 2) : 'null'}\n`
  results.store += `登录状态: ${authStore.isLoggedIn}\n`
  results.store += `加载状态: ${authStore.isLoading}\n`
  results.store += `错误信息: ${authStore.error || 'null'}\n`
  
  // 检查localStorage
  const savedUser = localStorage.getItem('authUser')
  results.store += `本地存储: ${savedUser || 'null'}\n`
}

// 使用Store登录
const testStoreLogin = async () => {
  results.store += `\n[${new Date().toLocaleTimeString()}] 开始使用 Auth Store 登录...\n`
  
  try {
    const success = await authStore.login({
      account: 'zhangsan',
      password: '123456'
    })
    
    results.store += `[${new Date().toLocaleTimeString()}] Store登录结果: ${success}\n`
    results.store += `[${new Date().toLocaleTimeString()}] Store状态更新后:\n`
    results.store += `  登录状态: ${authStore.isLoggedIn}\n`
    results.store += `  当前用户: ${authStore.currentUser?.displayName || 'null'}\n`
    results.store += `  错误信息: ${authStore.error || 'null'}\n`
    
    if (success) {
      ElMessage.success('Store登录成功!')
    } else {
      ElMessage.error('Store登录失败!')
    }
  } catch (error: any) {
    results.store += `[${new Date().toLocaleTimeString()}] Store登录异常: ${error.message}\n`
    ElMessage.error('Store登录异常!')
  }
}

// 开发者工具指南
const openDevTools = () => {
  ElMessage.info('请按 F12 或右键选择"检查"打开开发者工具，然后切换到 Network 面板查看网络请求')
}
</script>

<style scoped>
.debug-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.section {
  background: white;
  margin: 20px 0;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.form-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.output {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  padding: 15px;
  margin-top: 15px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  max-height: 300px;
  overflow-y: auto;
  white-space: pre-wrap;
}

h1 {
  color: #4A90E2;
  text-align: center;
  margin-bottom: 30px;
}

h3 {
  color: #333;
  margin-bottom: 15px;
}
</style>