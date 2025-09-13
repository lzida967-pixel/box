<template>
  <el-dialog
    v-model="dialogVisible"
    title="解散群聊"
    width="400px"
    :before-close="handleClose"
    class="dissolve-dialog"
  >
    <div class="warning-content">
      <div class="warning-icon">
        <el-icon size="48" color="#f56c6c">
          <WarningFilled />
        </el-icon>
      </div>
      
      <div class="warning-text">
        <h3>确定要解散群聊吗？</h3>
        <p v-if="group">
          群聊 <strong>"{{ group.remark || group.name }}"</strong> 解散后将无法恢复，所有聊天记录将被清空，群成员将被移除。
        </p>
      </div>
      
      <div class="confirmation-section">
        <div class="confirmation-text">
          请输入群名称以确认解散：
        </div>
        <el-input
          v-model="confirmText"
          :placeholder="group?.remark || group?.name || ''"
          clearable
          class="confirm-input"
        />
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="danger" 
          @click="handleDissolve" 
          :loading="loading"
          :disabled="!isConfirmed"
        >
          确定解散
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import ElMessage from 'element-plus/es/components/message/index.mjs'
import { WarningFilled } from '@element-plus/icons-vue'
import { groupApi } from '@/api'
import type { ChatGroup } from '@/types'

// Props & Emits
const props = defineProps<{
  modelValue: boolean
  group?: ChatGroup | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

// 响应式数据
const confirmText = ref('')
const loading = ref(false)

// 计算属性
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const isConfirmed = computed(() => {
  return confirmText.value === (props.group?.remark || props.group?.name)
})

// 方法
const handleClose = () => {
  confirmText.value = ''
  emit('update:modelValue', false)
}

const handleDissolve = async () => {
  if (!props.group || !isConfirmed.value) return

  try {
    loading.value = true
    
    await groupApi.dissolveGroup(props.group.id)
    
    ElMessage.success('群聊已解散')
    emit('success')
    handleClose()
  } catch (error: any) {
    console.error('解散群聊失败:', error)
    ElMessage.error(error.message || '解散群聊失败')
  } finally {
    loading.value = false
  }
}

// 监听对话框显示状态
watch(dialogVisible, (visible) => {
  if (!visible) {
    confirmText.value = ''
  }
})
</script>

<style scoped>
.dissolve-dialog :deep(.el-dialog__body) {
  padding: 30px 20px 20px;
}

.warning-content {
  text-align: center;
}

.warning-icon {
  margin-bottom: 20px;
}

.warning-text {
  margin-bottom: 24px;
}

.warning-text h3 {
  margin: 0 0 12px 0;
  color: #333;
  font-size: 18px;
  font-weight: 600;
}

.warning-text p {
  margin: 0;
  color: #666;
  line-height: 1.5;
  font-size: 14px;
}

.warning-text strong {
  color: #f56c6c;
}

.confirmation-section {
  text-align: left;
  margin-bottom: 20px;
}

.confirmation-text {
  margin-bottom: 8px;
  color: #333;
  font-size: 14px;
  font-weight: 500;
}

.confirm-input {
  width: 100%;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>