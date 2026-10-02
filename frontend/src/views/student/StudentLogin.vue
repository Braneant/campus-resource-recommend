<template>
  <div class="auth-wrap">
    <div class="auth-card app-card">
      <div class="auth-left">
        <div class="brand-gradient auth-logo">智学推荐</div>
        <p class="auth-slogan">知识图谱 × 协同过滤<br />为你精准推荐校园学习资源</p>
        <ul class="auth-points">
          <li><el-icon><CircleCheck /></el-icon> 三路混合推荐，可解释</li>
          <li><el-icon><CircleCheck /></el-icon> 知识图谱可视化导航</li>
          <li><el-icon><CircleCheck /></el-icon> 资源互助与问答广场</li>
        </ul>
      </div>
      <div class="auth-right">
        <h2 class="auth-title">学生登录</h2>
        <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="onSubmit" label-position="top">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" :prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" class="auth-submit" :loading="loading" @click="onSubmit">登 录</el-button>
        </el-form>
        <div class="auth-links">
          <span @click="go('StudentRegister')">立即注册</span>
          <span @click="go('StudentForgot')">找回密码</span>
        </div>
        <div class="auth-admin">
          <el-link type="info" @click="go('AdminLogin')">我是管理员 →</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, CircleCheck } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function go(name) { router.push({ name }) }

async function onSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const data = await login(form)
      userStore.setAuth({ token: data.token, role: data.role, user: data.user })
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || (data.role === 'ADMIN' ? '/admin/dashboard' : '/student/home')
      router.replace(redirect)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #eaf1ff 0%, #f4f7fc 55%, #eaf6ff 100%); padding: 24px; }
.auth-card { display: flex; width: 820px; max-width: 100%; border-radius: 16px; overflow: hidden; }
.auth-left { width: 320px; padding: 40px 32px; color: #fff;
  background: linear-gradient(160deg, #2f6df6, #5b8def); }
.auth-logo { font-size: 26px; margin-bottom: 18px; -webkit-text-fill-color: #fff; }
.auth-slogan { font-size: 16px; line-height: 1.7; margin-bottom: 28px; }
.auth-points { list-style: none; padding: 0; margin: 0; font-size: 13px; }
.auth-points li { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.auth-right { flex: 1; padding: 44px 40px; }
.auth-title { margin: 0 0 26px; font-size: 22px; color: var(--c-text); }
.auth-submit { width: 100%; height: 42px; font-size: 15px; margin-top: 4px; }
.auth-links { display: flex; justify-content: space-between; margin-top: 16px; font-size: 13px; color: var(--c-text-sub); }
.auth-links span { cursor: pointer; }
.auth-links span:hover { color: var(--c-primary); }
.auth-admin { margin-top: 18px; text-align: center; }
</style>
