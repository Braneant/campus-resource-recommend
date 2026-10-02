<template>
  <div class="auth-wrap">
    <!-- 深色底 + 明亮极光：管理端专属的沉稳深邃气质（不支持 WebGL 时自动回退为静态深色渐变） -->
    <div v-if="showAurora" class="auth-bg" aria-hidden="true">
      <Aurora :color-stops="auroraStops" :speed="0.6" :amplitude="1.2" :blend="0.55" />
    </div>

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
import Aurora from '@/components/bits/Aurora.vue'
import { canUseAurora } from '@/utils/motion'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

/** 极光配色：深色底上使用高饱和三色，形成真正的光带效果 */
const auroraStops = ['#2f6df6', '#8b5cf6', '#2bbf8a']
/** 是否启用极光背景（关闭动效开关或浏览器不支持 WebGL 时自动降级） */
const showAurora = canUseAurora()
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
  position: relative; overflow: hidden;
  background: radial-gradient(ellipse 85% 65% at 50% 42%, #1b2a4d 0%, #0d1526 72%);
  padding: 24px; }

/* 极光背景层 */
.auth-bg { position: absolute; inset: 0; z-index: 0; opacity: .85;
  transform: scale(1.12) translateY(2%); transform-origin: 50% 48%; }

/* 卡片居中浮于极光之上 */
.auth-card { position: relative; z-index: 1;
  animation: auth-card-in .5s cubic-bezier(.22, .8, .3, 1) both; }

@keyframes auth-card-in {
  from { opacity: 0; transform: translateY(14px) scale(.985); }
  to { opacity: 1; transform: none; }
}

.admin-login { padding: 44px 40px; text-align: center; }
.al-logo { width: 56px; height: 56px; }
.brand-gradient { font-size: 24px; margin: 12px 0 4px; }
.al-sub { color: var(--c-text-sub); font-size: 13px; margin: 0 0 24px; }
.auth-submit { width: 100%; height: 42px; font-size: 15px; }
.al-back { margin-top: 16px; text-align: center; }

/* 尊重系统「减弱动态效果」偏好 */
@media (prefers-reduced-motion: reduce) {
  .auth-card { animation: none; }
  .auth-bg { display: none; }
}
</style>
