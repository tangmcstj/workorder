<template>
  <div class="login-page">
    <div class="login-box">
      <div class="login-logo"><b>设备</b>工单系统</div>
      <div class="login-box-body">
        <p class="login-box-msg">Sign in to start your session</p>
        <el-form :model="form" @submit.prevent="submit">
          <el-form-item>
            <el-input v-model="form.username" placeholder="Username" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="Password" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-button type="primary" class="login-button" :loading="loading" @click="submit">登录</el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })

async function submit() {
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    router.push('/dashboard')
  } catch {
    ElMessage.error('登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-box-msg {
  margin: 0 0 15px;
  text-align: center;
}

.login-button {
  width: 100%;
}
</style>
