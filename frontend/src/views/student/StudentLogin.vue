<template>
  <div class="auth-wrap">
    <!-- 极光背景层：WebGL 不支持或动效关闭时自动隐藏，回退为静态渐变 -->
    <div v-if="showAurora" class="auth-bg" aria-hidden="true">
      <Aurora :color-stops="auroraStops" :speed="0.5" :amplitude="1.75" :blend="0.46" />
    </div>
    <div class="auth-veil" aria-hidden="true"></div>

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
import Aurora from '@/components/bits/Aurora.vue'
import { canUseAurora } from '@/utils/motion'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

/** 极光配色：浅蓝 → 品牌蓝 → 知识图谱紫，三色在白底上均可辨识 */
const auroraStops = ['#5b8def', '#2f6df6', '#8b5cf6']
/** 是否启用极光背景（关闭动效开关或浏览器不支持 WebGL 时自动降级） */
const showAurora = canUseAurora()

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
  position: relative; overflow: hidden;
  background: linear-gradient(135deg, #d9e6ff 0%, #e9eefb 55%, #e2e6ff 100%); padding: 24px; }

/* 极光背景层（置于最底，轻微放大让光带占据视觉重心） */
.auth-bg { position: absolute; inset: 0; z-index: 0; opacity: .95;
  transform: scale(1.15) translateY(2%); transform-origin: 50% 48%; }

/* 白色柔化层：仅在卡片区域加强，保证表单可读性，四周保留极光可见度 */
.auth-veil { position: absolute; inset: 0; z-index: 0; pointer-events: none;
  background: radial-gradient(ellipse 44% 38% at 50% 50%, rgba(255,255,255,.58) 0%, rgba(255,255,255,.2) 62%, rgba(255,255,255,0) 100%); }

.auth-card { position: relative; z-index: 1; display: flex; width: 820px; max-width: 100%;
  border-radius: 16px; overflow: hidden;
  animation: auth-card-in .5s cubic-bezier(.22,.8,.3,1) both; }

@keyframes auth-card-in {
  from { opacity: 0; transform: translateY(14px) scale(.985); }
  to { opacity: 1; transform: none; }
}

/* 动效尊重系统「减弱动态效果」偏好 */
@media (prefers-reduced-motion: reduce) {
  .auth-card { animation: none; }
  .auth-bg { display: none; }
}

.auth-left { width: 320px; padding: 40px 32px; color: #fff;
  background: linear-gradient(160deg, rgba(47,109,246,.95), rgba(91,141,239,.9)); }
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
