<template>
  <el-dialog
    v-model="visible"
    title="设置"
    width="600px"
    @close="handleClose"
  >
    <div class="settings-content">
      <div class="settings-sidebar">
        <div 
          class="settings-menu-item"
          :class="{ active: activeMenu === 'profile' }"
          @click="activeMenu = 'profile'"
        >
          个人资料
        </div>
        <div 
          class="settings-menu-item"
          :class="{ active: activeMenu === 'account' }"
          @click="activeMenu = 'account'"
        >
          用户设置
        </div>
        <div 
          class="settings-menu-item"
          :class="{ active: activeMenu === 'password' }"
          @click="activeMenu = 'password'"
        >
          修改密码
        </div>
      </div>
      
      <div class="settings-main">
        <!-- 个人资料 -->
        <div v-if="activeMenu === 'profile'" class="settings-section">
          <div class="profile-header">
            <div class="avatar-section">
              <div class="avatar-label">头像</div>
              <div class="avatar-container">
                <el-avatar :src="currentAvatarUrl" :size="80" class="user-avatar" :key="authStore.currentUser?.avatarTimestamp || 0" />
                <div class="avatar-overlay" @click="changeAvatar">
                  <el-icon><Camera /></el-icon>
                  <span>更换</span>
                </div>
              </div>
              <el-button size="small" type="primary" @click="changeAvatar" :loading="loading">
                {{ loading ? '上传中...' : '更换头像' }}
              </el-button>
            </div>
          </div>
          
          <div class="profile-form">
            <div class="form-item">
              <label>用户名</label>
              <el-input v-model="profileForm.username" placeholder="用户名" disabled readonly />
              <span class="form-tip">用户名不可修改</span>
            </div>
            
            <div class="form-item">
              <label>昵称</label>
              <el-input 
                v-model="profileForm.nickname" 
                placeholder="请输入昵称" 
                maxlength="20" 
                show-word-limit 
                :class="{ 'is-error': !profileForm.nickname.trim() }"
              />
            </div>
            
            <div class="form-item">
              <label>性别</label>
              <el-radio-group v-model="profileForm.gender">
                <el-radio label="male">男</el-radio>
                <el-radio label="female">女</el-radio>
              </el-radio-group>
            </div>
            
            <div class="form-item">
              <label>个性签名</label>
              <el-input 
                v-model="profileForm.signature" 
                type="textarea" 
                :rows="3"
                placeholder="请输入个性签名"
                maxlength="64"
                show-word-limit
              />
            </div>
          </div>
        </div>
        
        <!-- 用户设置 -->
        <div v-else-if="activeMenu === 'account'" class="settings-section">
          <div class="setting-item">
            <label>消息通知</label>
            <el-switch v-model="accountSettings.notifications" />
          </div>
          <div class="setting-item">
            <label>声音提醒</label>
            <el-switch v-model="accountSettings.sound" />
          </div>
          <div class="setting-item">
            <label>自动登录</label>
            <el-switch v-model="accountSettings.autoLogin" />
          </div>
        </div>
        
        <!-- 修改密码 -->
        <div v-else-if="activeMenu === 'password'" class="settings-section">
          <div class="form-item">
            <label>原密码</label>
            <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" />
          </div>
          <div class="form-item">
            <label>新密码</label>
            <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" />
          </div>
          <div class="form-item">
            <label>确认密码</label>
            <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
          </div>
        </div>
      </div>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="saveSettings" :loading="loading">
          {{ loading ? '保存中...' : '提交' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { userApi } from '@/api'

interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const authStore = useAuthStore()
const router = useRouter()
const activeMenu = ref('profile')
const loading = ref(false)

// 响应式头像URL，确保时间戳更新时重新计算
const currentAvatarUrl = computed(() => {
  return authStore.userAvatar
})

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 监听对话框打开状态
watch(visible, async (newVal) => {
  if (newVal) {
    try {
      console.log('正在获取用户信息...')
      
      // 首先检查用户是否已登录
      if (!authStore.isLoggedIn) {
        console.warn('用户未登录，尝试恢复认证状态')
        const restored = authStore.restoreAuth()
        if (!restored) {
          ElMessage.error('请先登录后再访问设置页面')
          visible.value = false
          return
        }
      }
      
      // 从本地存储获取当前用户信息作为默认值
      const localUser = JSON.parse(localStorage.getItem('authUser') || '{}')
      if (localUser.username) {
        console.log('使用本地用户信息初始化表单')
        updateFormData(localUser)
      }
      
      // 尝试从 API 获取最新数据
      try {
        const response = await userApi.getProfile()
        console.log('API获取的用户信息:', response)
        
        if (response?.data) {
          // 更新表单数据
          updateFormData(response.data)
          
          // 更新本地存储
          authStore.updateUserInfo({
            displayName: response.data.nickname || response.data.username,
            signature: response.data.signature || ''
          })
          console.log('用户信息更新成功')
        }
      } catch (apiError: any) {
        console.warn('从 API 获取用户信息失败，使用本地数据:', apiError)
        
        // 如果是401错误，说明token已过期，需要重新登录
        if (apiError.response?.status === 401) {
          ElMessage.warning('登录状态已过期，请重新登录')
          authStore.logout()
          visible.value = false
          return
        }
        
        // 其他错误不影响设置页面的使用，只显示警告
        if (!localUser.username) {
          ElMessage.error('无法获取用户信息，请重新登录')
          visible.value = false
        }
      }
    } catch (error) {
      console.error('初始化设置页面失败:', error)
      ElMessage.error('初始化设置页面失败')
      visible.value = false
    }
  }
})

function updateFormData(userInfo: any) {
  profileForm.username = userInfo.username || ''
  profileForm.nickname = userInfo.nickname || userInfo.username || ''
  profileForm.gender = userInfo.gender === 1 ? 'male' : userInfo.gender === 2 ? 'female' : 'male'
  profileForm.signature = userInfo.signature || ''
  
  // 设置默认值以防undefined
  accountSettings.notifications = userInfo.notificationsEnabled ?? true
  accountSettings.sound = userInfo.soundEnabled ?? true
  accountSettings.autoLogin = userInfo.autoLoginEnabled ?? false
}

const profileForm = reactive({
  username: authStore.userName || '',
  nickname: authStore.userName || '',
  gender: 'male',
  signature: ''
})

const accountSettings = reactive({
  notifications: true,
  sound: true,
  autoLogin: false
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const changeAvatar = () => {
  // 创建文件输入元素
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.style.display = 'none'
  
  // 文件选择后的处理
  input.onchange = async (event: Event) => {
    const file = (event.target as HTMLInputElement).files?.[0]
    if (!file) return
    
    // 文件类型检查
    if (!file.type.startsWith('image/')) {
      ElMessage.error('请选择图片文件')
      return
    }
    
    // 文件大小检查（2MB）
    if (file.size > 2 * 1024 * 1024) {
      ElMessage.error('头像大小不能超过2MB')
      return
    }
    
    try {
      console.log('开始上传头像，文件名:', file.name, '大小:', (file.size / 1024).toFixed(2) + 'KB')
      
      loading.value = true
      ElMessage.info('正在上传头像...')
      
      // 调用API上传头像
      const response = await userApi.uploadAvatar(file)
      console.log('头像上传成功:', response)
      
      // 获取返回的头像URL
      const avatarUrl = response.data || response
      console.log('新头像URL:', avatarUrl)
      
      // 生成新的时间戳防止缓存
      const avatarTimestamp = Date.now()
      
      // 更新本地存储的用户头像
      authStore.updateUserInfo({
        avatar: avatarUrl,
        avatarTimestamp: avatarTimestamp
      })
      
      // 同步更新本地存储
      const authUser = JSON.parse(localStorage.getItem('authUser') || '{}')
      authUser.avatar = avatarUrl
      authUser.avatarTimestamp = avatarTimestamp
      localStorage.setItem('authUser', JSON.stringify(authUser))
      
      // 强制重新渲染头像组件
      await authStore.$patch({ currentUser: { ...authStore.currentUser, avatarTimestamp } })
      
      ElMessage.success('头像更新成功')
    } catch (error: any) {
      console.error('头像上传失败:', error)
      const errorMessage = error.response?.data?.message || error.message || '头像上传失败，请重试'
      ElMessage.error(errorMessage)
    } finally {
      loading.value = false
      // 清理文件输入元素
      input.remove()
    }
  }
  
  // 触发文件选择
  document.body.appendChild(input)
  input.click()
}

const saveSettings = async () => {
  loading.value = true
  
  try {
    // 验证当前用户是否已登录
    if (!authStore.isLoggedIn) {
      throw new Error('用户未登录，请重新登录')
    }

    console.log('开始保存设置，当前菜单:', activeMenu.value)
    console.log('当前用户:', authStore.currentUser?.username)

    if (activeMenu.value === 'profile') {
      // 验证个人资料表单
      if (!profileForm.nickname.trim()) {
        throw new Error('请输入昵称')
      }
      if (profileForm.nickname.length > 20) {
        throw new Error('昵称长度不能超过20个字符')
      }
      if (profileForm.signature && profileForm.signature.length > 64) {
        throw new Error('个性签名长度不能超过64个字符')
      }
      
      // 更新个人资料
      const updateData = {
        nickname: profileForm.nickname.trim(),
        signature: profileForm.signature?.trim() || '',
        gender: profileForm.gender === 'male' ? 1 : 2 // 转换为后端格式：1=男，2=女
      }
      
      console.log('提交的更新数据:', updateData)
      console.log('数据类型检查:', {
        nickname: typeof updateData.nickname,
        signature: typeof updateData.signature,
        gender: typeof updateData.gender
      })
      
      try {
        const response = await userApi.updateProfile(updateData)
        console.log('更新后的用户信息:', response)
        
        // 更新本地存储的用户信息
        authStore.updateUserInfo({
          displayName: response.data.nickname || response.data.username,
          signature: response.data.signature || '',
          avatar: response.data.avatar || authStore.userAvatar
        })
        
        // 同步更新本地存储
        const authUser = JSON.parse(localStorage.getItem('authUser') || '{}')
        authUser.displayName = response.data.nickname || response.data.username
        authUser.signature = response.data.signature || ''
        authUser.avatar = response.data.avatar || authUser.avatar
        localStorage.setItem('authUser', JSON.stringify(authUser))
        
        ElMessage.success('个人资料修改成功')
      } catch (apiError: any) {
        console.error('个人资料更新API错误:', apiError)
        throw apiError // 重新抛出错误，让外层catch处理
      }
      
    } else if (activeMenu.value === 'password') {
      // 验证密码表单
      if (!passwordForm.oldPassword) {
        throw new Error('请输入原密码')
      }
      if (!passwordForm.newPassword) {
        throw new Error('请输入新密码')
      }
      if (passwordForm.newPassword !== passwordForm.confirmPassword) {
        throw new Error('两次输入的密码不一致')
      }
      if (passwordForm.newPassword.length < 6) {
        throw new Error('密码长度至少6位')
      }
      
      await userApi.changePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      
      // 密码修改成功后建议重新登录
      ElMessage.success('密码修改成功，请重新登录')
      authStore.logout()
      handleClose() // 先关闭弹窗
      
      // 延迟跳转到登录页面，确保弹窗先关闭
      setTimeout(async () => {
        await router.push('/login')
      }, 100)
      
      return // 密码修改成功后不显示通用成功消息
    }
    
    ElMessage.success('设置保存成功')
    handleClose()
  } catch (error: any) {
    console.error('保存设置失败:', error)
    console.error('错误详情:', {
      message: error.message,
      response: error.response,
      status: error.response?.status,
      data: error.response?.data
    })
    
    const errorMessage = error.response?.data?.message || error.message || '保存失败，请重试'
    ElMessage.error(errorMessage)
    
    // 如果是认证错误，清除登录状态
    if (error.response?.status === 401) {
      authStore.logout()
    }
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  visible.value = false
  // 重置表单
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })
}
</script>

<style scoped>
.settings-content {
  display: flex;
  min-height: 400px;
}

.settings-sidebar {
  width: 150px;
  border-right: 1px solid #e8e8e8;
  padding-right: 20px;
}

.settings-menu-item {
  padding: 12px 16px;
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 0.2s;
  margin-bottom: 4px;
}

.settings-menu-item:hover {
  background-color: #f5f5f5;
}

.settings-menu-item.active {
  background-color: #e6f4ff;
  color: #1890ff;
}

.settings-main {
  flex: 1;
  padding-left: 20px;
}

.settings-section {
  min-height: 300px;
}

.profile-header {
  margin-bottom: 24px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-container {
  position: relative;
  display: inline-block;
}

.user-avatar {
  cursor: pointer;
  transition: filter 0.2s;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
  cursor: pointer;
}

.avatar-container:hover .avatar-overlay {
  opacity: 1;
}

.avatar-overlay .el-icon {
  font-size: 20px;
  margin-bottom: 4px;
}

.avatar-label {
  font-weight: 500;
  color: #333;
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.form-item label {
  width: 80px;
  padding-top: 8px;
  color: #333;
  font-weight: 500;
  flex-shrink: 0;
}

.form-item .el-input,
.form-item .el-radio-group,
.form-item .el-textarea {
  flex: 1;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
  line-height: 32px;
}

.form-item .el-input.is-disabled {
  background-color: #f5f5f5;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-item label {
  font-weight: 500;
  color: #333;
}

.dialog-footer {
  text-align: right;
}

.el-menu-item.active {
  background-color: #e6f4ff;
  color: #1890ff;
}

.settings-main {
  flex: 1;
  padding-left: 20px;
}

.settings-section {
  min-height: 300px;
}

.profile-header {
  margin-bottom: 24px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-label {
  font-weight: 500;
  color: #333;
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.form-item label {
  width: 80px;
  padding-top: 8px;
  color: #333;
  font-weight: 500;
  flex-shrink: 0;
}

.form-item .el-input,
.form-item .el-radio-group,
.form-item .el-textarea {
  flex: 1;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-item label {
  font-weight: 500;
  color: #333;
}

.dialog-footer {
  text-align: right;
}
</style>