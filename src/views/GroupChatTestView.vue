gai'qate>
  <div class="group-chat-test">
    <div class="test-header">
      <h2>群聊功能测试</h2>
      <el-button @click="goBack" icon="ArrowLeft">返回</el-button>
    </div>

    <div class="test-content">
      <!-- 创建群聊测试 -->
      <el-card class="test-card">
        <template #header>
          <div class="card-header">
            <span>创建群聊测试</span>
          </div>
        </template>
        
        <el-form :model="createForm" label-width="100px">
          <el-form-item label="群名称">
            <el-input v-model="createForm.name" placeholder="请输入群名称" />
          </el-form-item>
          <el-form-item label="群描述">
            <el-input v-model="createForm.description" type="textarea" placeholder="请输入群描述" />
          </el-form-item>
          <el-form-item label="最大成员数">
            <el-input-number v-model="createForm.maxMembers" :min="3" :max="500" />
          </el-form-item>
          <el-form-item label="是否私有">
            <el-switch v-model="createForm.isPrivate" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="testCreateGroup" :loading="creating">
              创建群聊
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 群聊列表测试 -->
      <el-card class="test-card">
        <template #header>
          <div class="card-header">
            <span>群聊列表</span>
            <el-button @click="loadGroups" icon="Refresh" size="small">刷新</el-button>
          </div>
        </template>
        
        <div v-loading="loading">
          <div v-if="groups.length === 0" class="empty-state">
            <el-empty description="暂无群聊" />
          </div>
          <div v-else>
            <div 
              v-for="group in groups" 
              :key="group.id" 
              class="group-item"
              @click="selectGroup(group)"
              :class="{ active: selectedGroup?.id === group.id }"
            >
              <el-avatar :src="getGroupAvatar(group)" :size="40">
                {{ (group.remark || group.name).charAt(0) }}
              </el-avatar>
              <div class="group-info">
                <div class="group-name">{{ group.remark || group.name }}</div>
                <div class="group-desc">{{ group.description || '暂无描述' }}</div>
                <div class="group-meta">
                  成员: {{ group.memberCount || 0 }} / {{ group.maxMembers }}
                  <el-tag v-if="group.isPrivate" size="small" type="warning">私有</el-tag>
                </div>
              </div>
              <div class="group-actions">
                <el-button @click.stop="testGroupChat(group)" size="small" type="primary">
                  进入群聊
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 群聊详情测试 -->
      <el-card v-if="selectedGroup" class="test-card">
        <template #header>
          <div class="card-header">
            <span>群聊详情 - {{ selectedGroup.name }}</span>
          </div>
        </template>
        
        <GroupDetailPanel 
          :group="selectedGroup"
          @start-chat="handleStartGroupChat"
        />
      </el-card>

      <!-- WebSocket 消息测试 -->
      <el-card class="test-card">
        <template #header>
          <div class="card-header">
            <span>WebSocket 消息测试</span>
          </div>
        </template>
        
        <div class="websocket-test">
          <div class="connection-status">
            <span>连接状态: </span>
            <el-tag :type="wsConnected ? 'success' : 'danger'">
              {{ wsConnected ? '已连接' : '未连接' }}
            </el-tag>
            <el-button @click="toggleConnection" size="small" style="margin-left: 10px;">
              {{ wsConnected ? '断开连接' : '重新连接' }}
            </el-button>
          </div>
          
          <div v-if="selectedGroup" class="message-test">
            <el-input 
              v-model="testMessage" 
              placeholder="输入测试消息"
              @keyup.enter="sendTestMessage"
            />
            <el-button @click="sendTestMessage" type="primary" style="margin-top: 10px;">
              发送群聊消息
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- 日志输出 -->
      <el-card class="test-card">
        <template #header>
          <div class="card-header">
            <span>操作日志</span>
            <el-button @click="clearLogs" size="small">清空日志</el-button>
          </div>
        </template>
        
        <div class="logs">
          <div v-for="(log, index) in logs" :key="index" class="log-item">
            <span class="log-time">{{ formatTime(log.time) }}</span>
            <span class="log-level" :class="log.level">{{ log.level }}</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import { groupApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import GroupDetailPanel from '@/components/GroupDetailPanel.vue'
import type { ChatGroup } from '@/types'
import dayjs from 'dayjs'

const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const loading = ref(false)
const creating = ref(false)
const groups = ref<ChatGroup[]>([])
const selectedGroup = ref<ChatGroup | null>(null)
const wsConnected = ref(false)
const testMessage = ref('')
const logs = ref<Array<{ time: Date, level: string, message: string }>>([])

// 创建群聊表单
const createForm = ref({
  name: '测试群聊',
  description: '这是一个测试群聊',
  maxMembers: 100,
  isPrivate: false
})

// 方法
const goBack = () => {
  router.back()
}

const addLog = (level: string, message: string) => {
  logs.value.unshift({
    time: new Date(),
    level,
    message
  })
  
  // 限制日志数量
  if (logs.value.length > 50) {
    logs.value = logs.value.slice(0, 50)
  }
}

const clearLogs = () => {
  logs.value = []
}

const formatTime = (time: Date) => {
  return dayjs(time).format('HH:mm:ss')
}

const loadGroups = async () => {
  try {
    loading.value = true
    addLog('INFO', '开始加载群聊列表...')
    
    const response = await groupApi.getUserGroups()
    groups.value = response.data?.data || []
    
    addLog('SUCCESS', `成功加载 ${groups.value.length} 个群聊`)
  } catch (error: any) {
    console.error('加载群聊列表失败:', error)
    addLog('ERROR', `加载群聊列表失败: ${error.message}`)
    ElMessage.error(error.message || '加载群聊列表失败')
  } finally {
    loading.value = false
  }
}

const testCreateGroup = async () => {
  try {
    creating.value = true
    addLog('INFO', '开始创建群聊...')
    
    const groupData = {
      ...createForm.value,
      memberIds: [] // 暂时不添加其他成员
    }
    
    const response = await groupApi.createGroup(groupData)
    const newGroup = response.data?.data
    
    addLog('SUCCESS', `成功创建群聊: ${newGroup.name}`)
    ElMessage.success('群聊创建成功')
    
    // 刷新群聊列表
    await loadGroups()
    
    // 选中新创建的群聊
    selectedGroup.value = newGroup
    
  } catch (error: any) {
    console.error('创建群聊失败:', error)
    addLog('ERROR', `创建群聊失败: ${error.message}`)
    ElMessage.error(error.message || '创建群聊失败')
  } finally {
    creating.value = false
  }
}

const selectGroup = (group: ChatGroup) => {
  selectedGroup.value = group
  addLog('INFO', `选中群聊: ${group.remark || group.name}`)
}

// 存储已加载的群头像
const groupAvatarCache = ref<Map<number, string>>(new Map())

const getGroupAvatar = (group: ChatGroup) => {
  // 检查多个可能的头像字段
  const avatar = group.avatar || group.groupAvatar
  
  if (avatar) {
    // 如果是base64格式的图片数据
    if (avatar.startsWith('data:image/')) {
      return avatar
    }
  }
  
  // 检查缓存中是否已有该群的头像
  if (groupAvatarCache.value.has(group.id)) {
    const cachedAvatar = groupAvatarCache.value.get(group.id)
    return cachedAvatar || null
  }
  
  // 异步加载头像
  loadGroupAvatar(group.id)
  
  // 返回null让el-avatar显示fallback，直到头像加载完成
  return null
}

// 异步加载群头像
const loadGroupAvatar = async (groupId: number) => {
  try {
    const response = await fetch(`http://localhost:8080/api/group/${groupId}/avatar`, {
      headers: {
        'Authorization': `Bearer ${authStore.currentUser?.token || authStore.userInfo?.token || ''}`
      }
    })
    
    if (response.ok) {
      const blob = await response.blob()
      const reader = new FileReader()
      reader.onload = () => {
        const base64 = reader.result as string
        groupAvatarCache.value.set(groupId, base64)
      }
      reader.readAsDataURL(blob)
    } else {
      // 头像不存在或无权限访问，缓存空字符串避免重复请求
      groupAvatarCache.value.set(groupId, '')
    }
  } catch (error) {
    console.error('加载群头像失败:', error)
    groupAvatarCache.value.set(groupId, '')
  }
}

const testGroupChat = (group: ChatGroup) => {
  addLog('INFO', `测试进入群聊: ${group.remark || group.name}`)
  ElMessage.info(`进入群聊: ${group.remark || group.name}`)
  
  // 跳转到聊天界面并开始群聊
  router.push({
    name: 'chat',
    params: {
      type: 'group',
      id: group.id.toString()
    }
  })
}

const handleStartGroupChat = (group: ChatGroup) => {
  addLog('INFO', `开始群聊: ${group.remark || group.name}`)
  ElMessage.success(`开始与 ${group.remark || group.name} 的群聊`)
  
  // 跳转到聊天界面并开始群聊
  router.push({
    name: 'chat',
    params: {
      type: 'group',
      id: group.id.toString()
    }
  })
}

const toggleConnection = () => {
  if (wsConnected.value) {
    // 断开连接
    wsConnected.value = false
    addLog('INFO', 'WebSocket 连接已断开')
  } else {
    // 重新连接
    wsConnected.value = true
    addLog('INFO', 'WebSocket 连接已建立')
  }
}

const sendTestMessage = () => {
  if (!selectedGroup.value) {
    ElMessage.warning('请先选择一个群聊')
    return
  }
  
  if (!testMessage.value.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }
  
  addLog('INFO', `发送群聊消息到 ${selectedGroup.value.name}: ${testMessage.value}`)
  
  // 这里应该调用实际的发送消息API
  ElMessage.success('消息发送成功（模拟）')
  testMessage.value = ''
}

// 生命周期
onMounted(async () => {
  addLog('INFO', '群聊测试页面已加载')
  
  // 检查登录状态
  if (!authStore.isLoggedIn) {
    addLog('ERROR', '用户未登录')
    ElMessage.error('请先登录')
    router.push('/login')
    return
  }
  
  addLog('INFO', `当前用户: ${authStore.userInfo?.username}`)
  
  // 加载群聊列表
  await loadGroups()
  
  // 模拟WebSocket连接状态
  wsConnected.value = true
})

onUnmounted(() => {
  addLog('INFO', '群聊测试页面已卸载')
})
</script>

<style scoped>
.group-chat-test {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.test-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.test-header h2 {
  margin: 0;
  color: #333;
}

.test-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.test-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.group-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
  gap: 12px;
}

.group-item:hover {
  background-color: #f8f9fa;
  border-color: #1890ff;
}

.group-item.active {
  background-color: #e6f4ff;
  border-color: #1890ff;
}

.group-info {
  flex: 1;
}

.group-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.group-desc {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.group-meta {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 8px;
}

.group-actions {
  flex-shrink: 0;
}

.websocket-test {
  space-y: 16px;
}

.connection-status {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.message-test {
  margin-top: 16px;
}

.logs {
  max-height: 300px;
  overflow-y: auto;
  background: #f8f9fa;
  padding: 12px;
  border-radius: 4px;
}

.log-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
  font-family: monospace;
}

.log-time {
  color: #666;
  flex-shrink: 0;
}

.log-level {
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: 500;
  flex-shrink: 0;
  min-width: 60px;
  text-align: center;
}

.log-level.INFO {
  background: #e6f4ff;
  color: #1890ff;
}

.log-level.SUCCESS {
  background: #f6ffed;
  color: #52c41a;
}

.log-level.ERROR {
  background: #fff2f0;
  color: #f5222d;
}

.log-message {
  flex: 1;
  color: #333;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .test-content {
    grid-template-columns: 1fr;
  }
}
</style>