<template>
  <el-dialog
    v-model="dialogVisible"
    title="修改群昵称"
    width="400px"
    :before-close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
    >
      <el-form-item label="群昵称" prop="nickname">
        <el-input
          v-model="form.nickname"
          placeholder="请输入群昵称"
          maxlength="20"
          show-word-limit
          clearable
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm" :loading="loading">
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { FormInstance } from 'element-plus'

// Props & Emits
const props = defineProps<{
  modelValue: boolean
  currentNickname: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [nickname: string]
}>()

// 响应式数据
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = ref({
  nickname: ''
})

// 计算属性
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 表单验证规则
const rules = {
  nickname: [
    { required: true, message: '请输入群昵称', trigger: 'blur' },
    { min: 1, max: 20, message: '昵称长度在 1 到 20 个字符', trigger: 'blur' }
  ]
}

// 方法
const handleClose = () => {
  emit('update:modelValue', false)
}

const handleConfirm = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true
    
    emit('confirm', form.value.nickname.trim())
    handleClose()
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    loading.value = false
  }
}

// 监听对话框显示状态
watch(dialogVisible, (visible) => {
  if (visible) {
    form.value.nickname = props.currentNickname
  }
})
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>