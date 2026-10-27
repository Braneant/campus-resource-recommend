<template>
  <div class="page-container">
    <h2 class="page-title">个人中心</h2>
    <p class="page-subtitle">管理你的资料、账号安全与学习足迹</p>

    <el-row :gutter="18">
      <el-col :xs="24" :sm="24" :md="9">
        <div class="app-card profile-card">
          <el-avatar :size="72" :src="avatarSrc">{{ initial }}</el-avatar>
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

      <el-col :xs="24" :sm="24" :md="15" class="profile-right">
        <div class="stat-row">
          <div class="app-card stat-box" v-for="s in stats" :key="s.label">
            <div class="stat-num">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>

        <div class="app-card foot-card">
          <el-tabs v-model="tab">
            <el-tab-pane v-for="pane in tabDefs" :key="pane.name" :label="pane.label" :name="pane.name">
              <div v-loading="pane.loading" class="foot-list">
                <div v-if="pane.list.length" class="foot-list-inner">
                  <div v-for="r in pane.list" :key="r.id" class="foot-item" @click="goResource(r.id)">
                    <div class="foot-main">
                      <div class="foot-title">{{ r.title }}</div>
                      <div class="foot-meta">
                        <el-tag size="small" effect="plain">{{ typeLabel(r.type) }}</el-tag>
                        <span>{{ r.author || '—' }}</span>
                        <span class="foot-no">{{ r.resourceNo }}</span>
                        <el-tag v-if="pane.name === 'up'" size="small" :type="statusTag(r.status)">{{ statusLabel(r.status) }}</el-tag>
                      </div>
                    </div>
                    <el-icon class="foot-arrow"><ArrowRight /></el-icon>
                  </div>
                </div>
                <el-empty v-else description="暂无记录" :image-size="70" />
              </div>
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Edit, Key, ArrowRight } from '@element-plus/icons-vue'
import {
  getProfile, updateProfile, changePassword,
  getUserOverview, getUserFavorites, getUserDownloads, getUserUploads
} from '@/api/user'
import { useUserStore } from '@/store/user'
import { GenderMap, ResourceTypeMap, ResourceStatusMap } from '@/utils/constants'
import { collegeOptions } from '@/utils/options'
import { usableMediaUrl } from '@/utils/media'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
/** 头像地址：占位域名视为无头像，自动回退为首字母 */
const avatarSrc = computed(() => usableMediaUrl(userInfo.value?.avatar))
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
const tabDefs = reactive([
  { name: 'fav', label: '我的收藏', list: [], loading: false },
  { name: 'dl', label: '我的下载', list: [], loading: false },
  { name: 'up', label: '我的上传', list: [], loading: false }
])
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

function typeLabel(t) { return ResourceTypeMap[t]?.label || '其他' }
function statusLabel(s) { return ResourceStatusMap[s]?.label || '未知' }
function statusTag(s) { return ResourceStatusMap[s]?.tag || 'info' }
function goResource(id) { router.push(`/student/resource/${id}`) }

function loadOverview() {
  getUserOverview()
    .then((d) => {
      stats[0].value = d.favoriteCount ?? 0
      stats[1].value = d.downloadCount ?? 0
      stats[2].value = d.uploadCount ?? 0
      stats[3].value = d.ratingCount ?? 0
    })
    .catch(() => {})
}

function loadTab(name) {
  const pane = tabDefs.find((p) => p.name === name)
  if (!pane) return
  pane.loading = true
  const api = name === 'fav' ? getUserFavorites : name === 'dl' ? getUserDownloads : getUserUploads
  api({ pageNum: 1, pageSize: 100 })
    .then((d) => { pane.list = d.list || [] })
    .catch(() => {})
    .finally(() => { pane.loading = false })
}

watch(tab, (name) => loadTab(name))

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
  loadOverview()
  loadTab('fav')
})
</script>

<style scoped>
.profile-card { padding: 26px; text-align: center; }
.p-name { font-size: 18px; font-weight: 700; margin: 12px 0 4px; }
.p-no { font-size: 12px; color: var(--c-text-sub); }
.p-list { list-style: none; padding: 0; margin: 0; text-align: left; }
.p-list li { display: flex; justify-content: space-between; padding: 10px 0; font-size: 13px; border-bottom: 1px dashed var(--c-border); }
.p-list span { color: var(--c-text-sub); }
.p-list b { max-width: 66%; text-align: right; word-break: break-all; }
.w100 { width: 100%; margin-top: 10px; }
.stat-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }
.stat-box { padding: 18px; text-align: center; }
.stat-num { font-size: 26px; font-weight: 800; color: var(--c-primary); }
.stat-label { font-size: 12px; color: var(--c-text-sub); margin-top: 4px; }
.foot-card { padding: 18px 22px; min-height: 280px; }
.foot-list { min-height: 220px; }
.foot-list-inner { max-height: 460px; overflow-y: auto; }
.foot-item { display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 12px 4px; border-bottom: 1px dashed var(--c-border); cursor: pointer; }
.foot-item:hover .foot-title { color: var(--c-primary); }
.foot-main { min-width: 0; }
.foot-title { font-size: 14px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.foot-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; row-gap: 4px; margin-top: 5px; font-size: 12px; color: var(--c-text-sub); }
.foot-no { font-family: var(--font-mono, monospace); font-size: 11px; }
.foot-arrow { color: var(--c-text-sub); flex-shrink: 0; }
@media (max-width: 991px) {
  .profile-right { margin-top: 16px; }
}
@media (max-width: 560px) {
  .stat-row { grid-template-columns: repeat(2, 1fr); }
  .profile-card { padding: 20px 18px; }
}
</style>
