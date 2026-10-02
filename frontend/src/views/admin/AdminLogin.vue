<template>
  <div class="auth-wrap">
    <div class="auth-card app-card" style="width:440px">
      <div class="admin-login">
        <img src="/favicon.svg" class="al-logo" alt="logo" />
        <h2 class="brand-gradient">管理控制台</h2>
        <p class="al-sub">校园学习资源推荐平台 · 管理员入口</p>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @keyup.enter="onSubmit">
          <el-form-item label="管理员账号" prop="username">
            <el-input v-model="form.username" placeholder="请输入管理员用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" :prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" class="auth-submit" :loading="loading" @click="onSubmit">登 录</el-button>
        </el-form>
        <div class="al-back"><el-link type="info" @click="go('StudentLogin')">返回学生端 →</el-link></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function go(name) { router.push({ name }) }

async function onSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const data = await login(form)
      if (data.role !== 'ADMIN') {
        ElMessage.warning('该账号不是管理员，请使用学生端登录')
        return
      }
      userStore.setAuth({ token: data.token, role: data.role, user: data.user })
      ElMessage.success('管理员登录成功')
      const redirect = route.query.redirect || '/admin/dashboard'
      router.replace(redirect)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1f2a44 0%, #2f6df6 100%); padding: 24px; }
.admin-login { padding: 44px 40px; text-align: center; }
.al-logo { width: 56px; height: 56px; }
.brand-gradient { font-size: 24px; margin: 12px 0 4px; }
.al-sub { color: var(--c-text-sub); font-size: 13px; margin: 0 0 24px; }
.auth-submit { width: 100%; height: 42px; font-size: 15px; }
.al-back { margin-top: 16px; text-align: center; }
</style>
