<template>
  <div class="page-container">
    <h2 class="page-title">个人中心</h2>
    <p class="page-subtitle">管理你的资料、账号安全与学习足迹</p>

    <el-row :gutter="18">
      <el-col :span="9">
        <div class="app-card profile-card">
          <el-avatar :size="72" :src="userInfo?.avatar || ''">{{ initial }}</el-avatar>
          <div class="p-name">{{ userInfo?.nickname || userInfo?.username }}</div>
          <div class="p-no">@{{ userInfo?.username }} · 学号 {{ userInfo?.studentNo || '—' }}</div>
          <el-divider />
          <ul class="p-list">
            <li><span>性别</span><b>{{ genderLabel }}</b></li>
            <li><span>邮箱</span><b>{{ userInfo?.email || '—' }}</b></li>
            <li><span>手机</span><b>{{ userInfo?.phone || '—' }}</b></li>
            <li><span>学院</span><b>{{ collegeName(userInfo?.collegeId) }}</b></li>
            <li><span>专业</span><b>{{ majorName(userInfo?.majorId) }}</b></li>
          </ul>
          <el-button type="primary" plain class="w100" :icon="Edit" @click="editVisible = true">编辑资料</el-button>
          <el-button class="w100" :icon="Key" @click="pwdVisible = true">修改密码</el-button>
        </div>
      </el-col>

      <el-col :span="15">
        <div class="stat-row">
          <div class="app-card stat-box" v-for="s in stats" :key="s.label">
            <div class="stat-num">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>

        <div class="app-card foot-card">
          <el-tabs v-model="tab">
            <el-tab-pane label="我的收藏" name="fav">
              <el-empty description="收藏列表接口待后端提供（/api/user/favorites）" :image-size="70" />
            </el-tab-pane>
            <el-tab-pane label="我的下载" name="dl">
              <el-empty description="下载记录接口待后端提供（/api/user/downloads）" :image-size="70" />
            </el-tab-pane>
            <el-tab-pane label="我的上传" name="up">
              <el-empty description="我的上传接口待后端提供（/api/user/uploads）" :image-size="70" />
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>

    <!-- 编辑资料 -->
    <el-dialog v-model="editVisible" title="编辑资料" width="460px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="昵称"><el-input v-model="editForm.nickname" /></el-form-item>
        <el-form-item label="真实姓名"><el-input v-model="editForm.realName" /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="editForm.gender" style="width:100%">
            <el-option :value="0" label="未知" /><el-option :value="1" label="男" /><el-option :value="2" label="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="editForm.phone" /></el-form-item>
        <el-form-item label="头像"><el-input v-model="editForm.avatar" placeholder="头像 URL" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="420px">
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdRef" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码" prop="newPassword"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="savePwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Key } from '@element-plus/icons-vue'
import { getProfile, updateProfile, changePassword } from '@/api/user'
import { useUserStore } from '@/store/user'
import { GenderMap } from '@/utils/constants'
import { collegeOptions } from '@/utils/options'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const initial = computed(() => (userInfo.value?.nickname || userInfo.value?.username || '同').charAt(0))
const genderLabel = computed(() => GenderMap[userInfo.value?.gender] || '未知')

function collegeName(id) { return collegeOptions.find((c) => c.id === id)?.name || '—' }
function majorName(id) {
  const map = { 1: '计算机科学与技术', 2: '软件工程', 3: '人工智能' }
  return map[id] || '—'
}

const stats = reactive([
  { label: '我的收藏', value: '—' },
  { label: '我的下载', value: '—' },
  { label: '我的上传', value: '—' },
  { label: '累计评分', value: '—' }
])

const tab = ref('fav')
const editVisible = ref(false)
const pwdVisible = ref(false)
const loading = ref(false)

const editForm = reactive({ nickname: '', realName: '', gender: 0, email: '', phone: '', avatar: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '' })
const pwdRef = ref(null)
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '至少 6 位', trigger: 'blur' }]
}

function fillEdit() {
  const u = userInfo.value || {}
  editForm.nickname = u.nickname || ''
  editForm.realName = u.realName || ''
  editForm.gender = u.gender ?? 0
  editForm.email = u.email || ''
  editForm.phone = u.phone || ''
  editForm.avatar = u.avatar || ''
}

function saveProfile() {
  loading.value = true
  updateProfile({ ...editForm })
    .then(() => {
      userStore.setUserInfo({ ...editForm })
      ElMessage.success('资料已更新')
      editVisible.value = false
    })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function savePwd() {
  pwdRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    changePassword({ ...pwdForm })
      .then(() => { ElMessage.success('密码修改成功'); pwdVisible.value = false; pwdForm.oldPassword = ''; pwdForm.newPassword = '' })
      .catch(() => {})
      .finally(() => { loading.value = false })
  })
}

onMounted(() => {
  getProfile()
    .then((u) => { userStore.setUserInfo(u); fillEdit() })
    .catch(() => {})
  fillEdit()
})
</script>

<style scoped>
.profile-card { padding: 26px; text-align: center; }
.p-name { font-size: 18px; font-weight: 700; margin: 12px 0 4px; }
.p-no { font-size: 12px; color: var(--c-text-sub); }
.p-list { list-style: none; padding: 0; margin: 0; text-align: left; }
.p-list li { display: flex; justify-content: space-between; padding: 10px 0; font-size: 13px; border-bottom: 1px dashed var(--c-border); }
.p-list span { color: var(--c-text-sub); }
.w100 { width: 100%; margin-top: 10px; }
.stat-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }
.stat-box { padding: 18px; text-align: center; }
.stat-num { font-size: 26px; font-weight: 800; color: var(--c-primary); }
.stat-label { font-size: 12px; color: var(--c-text-sub); margin-top: 4px; }
.foot-card { padding: 18px 22px; min-height: 280px; }
</style>
