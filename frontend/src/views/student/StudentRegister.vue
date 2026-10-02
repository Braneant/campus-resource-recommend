<template>
  <div class="auth-wrap">
    <div class="auth-card app-card reg-card">
      <div class="auth-left">
        <div class="brand-gradient auth-logo">智学推荐</div>
        <p class="auth-slogan">加入平台<br />开启你的个性化学习之旅</p>
        <ul class="auth-points">
          <li><el-icon><CircleCheck /></el-icon> 智能推荐，省时高效</li>
          <li><el-icon><CircleCheck /></el-icon> 知识图谱，体系清晰</li>
          <li><el-icon><CircleCheck /></el-icon> 互助问答，共同进步</li>
        </ul>
      </div>
      <div class="auth-right">
        <h2 class="auth-title">学生注册</h2>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
          <el-row :gutter="14">
            <el-col :span="12">
              <el-form-item label="学号" prop="studentNo">
                <el-input v-model="form.studentNo" placeholder="如 2023001" :prefix-icon="Postcard" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="form.username" placeholder="登录用户名" :prefix-icon="User" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="14">
            <el-col :span="12">
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="form.nickname" placeholder="展示昵称" :prefix-icon="Avatar" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" placeholder="用于找回密码" :prefix-icon="Message" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="14">
            <el-col :span="12">
              <el-form-item label="学院" prop="collegeId">
                <el-select v-model="form.collegeId" placeholder="选择学院" style="width:100%" @change="onCollegeChange">
                  <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="专业" prop="majorId">
                <el-select v-model="form.majorId" placeholder="选择专业" style="width:100%">
                  <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="14">
            <el-col :span="12">
              <el-form-item label="密码" prop="password">
                <el-input v-model="form.password" type="password" show-password placeholder="设置密码" :prefix-icon="Lock" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="确认密码" prop="confirm">
                <el-input v-model="form.confirm" type="password" show-password placeholder="再次输入" :prefix-icon="Lock" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-button type="primary" class="auth-submit" :loading="loading" @click="onSubmit">注 册</el-button>
        </el-form>
        <div class="auth-links">
          <span @click="go('StudentLogin')">已有账号？去登录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Avatar, Message, Postcard, CircleCheck } from '@element-plus/icons-vue'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

// 学院/专业为演示用静态选项（注册页未登录，避免调用需鉴权的 /api/admin 接口）
const colleges = [
  { id: 1, name: '计算机学院' },
  { id: 2, name: '数学学院' },
  { id: 3, name: '电子信息学院' }
]
const majors = ref([
  { id: 1, name: '计算机科学与技术' },
  { id: 2, name: '软件工程' },
  { id: 3, name: '人工智能' }
])

function onCollegeChange() {
  form.majorId = null
  // 演示：按学院切换专业选项
  const map = {
    1: [{ id: 1, name: '计算机科学与技术' }, { id: 2, name: '软件工程' }, { id: 3, name: '人工智能' }],
    2: [{ id: 4, name: '数学与应用数学' }, { id: 5, name: '信息与计算科学' }],
    3: [{ id: 6, name: '电子信息工程' }, { id: 7, name: '通信工程' }]
  }
  majors.value = map[form.collegeId] || []
}

const form = reactive({
  studentNo: '', username: '', nickname: '', email: '',
  collegeId: null, majorId: null, password: '', confirm: ''
})

const rules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  collegeId: [{ required: true, message: '请选择学院', trigger: 'change' }],
  majorId: [{ required: true, message: '请选择专业', trigger: 'change' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '至少 6 位', trigger: 'blur' }],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: (r, v, cb) => v === form.password ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' }
  ]
}

function go(name) { router.push({ name }) }

async function onSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await register({ ...form })
      ElMessage.success('注册成功，请登录')
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
.auth-card { display: flex; width: 860px; max-width: 100%; border-radius: 16px; overflow: hidden; }
.reg-card { width: 900px; }
.auth-left { width: 300px; padding: 40px 30px; color: #fff; background: linear-gradient(160deg, #2f6df6, #5b8def); }
.auth-logo { font-size: 26px; margin-bottom: 18px; -webkit-text-fill-color: #fff; }
.auth-slogan { font-size: 16px; line-height: 1.7; margin-bottom: 28px; }
.auth-points { list-style: none; padding: 0; margin: 0; font-size: 13px; }
.auth-points li { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.auth-right { flex: 1; padding: 36px 40px; }
.auth-title { margin: 0 0 22px; font-size: 22px; color: var(--c-text); }
.auth-submit { width: 100%; height: 42px; font-size: 15px; }
.auth-links { margin-top: 16px; font-size: 13px; color: var(--c-text-sub); text-align: right; }
.auth-links span { cursor: pointer; }
.auth-links span:hover { color: var(--c-primary); }
</style>
