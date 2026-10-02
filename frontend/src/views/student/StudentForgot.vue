<template>
  <div class="auth-wrap">
    <div class="auth-card app-card" style="width:480px">
      <div class="auth-right" style="padding:44px 40px">
        <h2 class="auth-title">找回密码</h2>
        <el-steps :active="step" align-center finish-status="success" class="mb-24">
          <el-step title="校验身份" />
          <el-step title="重置密码" />
        </el-steps>

        <template v-if="step === 0">
          <el-form :model="form1" :rules="rules1" ref="f1" label-position="top">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="form1.studentNo" placeholder="请输入学号" :prefix-icon="Postcard" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form1.email" placeholder="请输入注册邮箱" :prefix-icon="Message" />
            </el-form-item>
            <el-button type="primary" class="auth-submit" :loading="loading" @click="onVerify">发送验证码</el-button>
          </el-form>
        </template>

        <template v-else>
          <el-form :model="form2" :rules="rules2" ref="f2" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form2.username" placeholder="请输入用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item label="验证码" prop="code">
              <el-input v-model="form2.code" placeholder="请输入邮箱收到的验证码" :prefix-icon="Key" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="form2.newPassword" type="password" show-password placeholder="至少 6 位" :prefix-icon="Lock" />
            </el-form-item>
            <el-button type="primary" class="auth-submit" :loading="loading" @click="onReset">重 置</el-button>
          </el-form>
        </template>

        <div class="auth-links" style="text-align:right">
          <span @click="go('StudentLogin')">返回登录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Postcard, Key } from '@element-plus/icons-vue'
import { forgotPassword, resetPassword } from '@/api/auth'

const router = useRouter()
const step = ref(0)
const loading = ref(false)
const f1 = ref(null)
const f2 = ref(null)

const form1 = reactive({ studentNo: '', email: '' })
const form2 = reactive({ username: '', code: '', newPassword: '' })

const rules1 = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}
const rules2 = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '至少 6 位', trigger: 'blur' }]
}

function go(name) { router.push({ name }) }

async function onVerify() {
  await f1.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await forgotPassword({ ...form1 })
      ElMessage.success('验证码已发送（演示环境请使用 123456）')
      step.value = 1
    } finally {
      loading.value = false
    }
  })
}

async function onReset() {
  await f2.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await resetPassword({ ...form2 })
      ElMessage.success('密码重置成功，请登录')
      router.replace('/student/login')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #eaf1ff 0%, #f4f7fc 55%, #eaf6ff 100%); padding: 24px; }
.auth-right { width: 100%; }
.auth-title { margin: 0 0 22px; font-size: 22px; }
.auth-submit { width: 100%; height: 42px; font-size: 15px; }
.mb-24 { margin-bottom: 24px; }
.auth-links { margin-top: 16px; font-size: 13px; color: var(--c-text-sub); }
.auth-links span { cursor: pointer; }
.auth-links span:hover { color: var(--c-primary); }
</style>
