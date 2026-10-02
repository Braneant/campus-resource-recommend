<template>
  <div class="page-container">
    <div class="head-row">
      <div>
        <h2 class="page-title">资源互助</h2>
        <p class="page-subtitle">发布你的学习资源需求，同学互助解答</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="dialog = true">发布需求</el-button>
    </div>

    <div class="filter-row app-card">
      <el-input v-model="filters.keyword" placeholder="搜索需求" :prefix-icon="Search" clearable style="width:260px" @keyup.enter="load" @clear="load" />
      <el-select v-model="filters.collegeId" placeholder="学院" clearable style="width:150px" @change="load">
        <el-option v-for="c in collegeOptions" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable style="width:130px" @change="load">
        <el-option :value="0" label="待解决" /><el-option :value="1" label="已解决" />
      </el-select>
    </div>

    <div v-loading="loading" class="req-list">
      <div v-for="r in list" :key="r.id" class="app-card req-item" @click="go(r.id)">
        <div class="req-status" :class="r.status === 1 ? 'done' : 'open'">
          <el-icon v-if="r.status === 1"><CircleCheck /></el-icon><el-icon v-else><Loading /></el-icon>
          {{ r.status === 1 ? '已解决' : '待解决' }}
        </div>
        <div class="req-main">
          <div class="req-title">{{ r.title }}</div>
          <div class="req-content">{{ r.content }}</div>
          <div class="req-meta">
            <span><el-icon><User /></el-icon>{{ r.studentName }}</span>
            <span><el-icon><School /></el-icon>{{ collegeName(r.collegeId) }}</span>
            <span><el-icon><ChatDotRound /></el-icon>{{ r.replyCount || 0 }} 条回复</span>
            <span class="req-time">{{ r.createTime }}</span>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && list.length === 0" description="暂无需求，快来发布第一条" />
    </div>

    <el-pagination v-if="total > 0" layout="prev,pager,next,total" :total="total" :page-size="pageSize"
      :current-page="pageNum" @current-change="onPage" background class="mt16 center" />

    <el-dialog v-model="dialog" title="发布资源需求" width="520px">
      <el-form :model="form" :rules="rules" ref="f" label-width="70px">
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" placeholder="一句话描述你的需求" /></el-form-item>
        <el-form-item label="学院" prop="collegeId">
          <el-select v-model="form.collegeId" style="width:100%">
            <el-option v-for="c in collegeOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" style="width:100%">
            <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="详情" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="具体描述你需要什么资源 / 帮助" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search, User, School, ChatDotRound, CircleCheck, Loading } from '@element-plus/icons-vue'
import { listRequirement, createRequirement } from '@/api/requirement'
import { collegeOptions, categoryOptions } from '@/utils/options'

const router = useRouter()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = reactive({ keyword: '', collegeId: null, status: null })
const dialog = ref(false)
const submitting = ref(false)
const f = ref(null)
const form = reactive({ title: '', content: '', categoryId: null, collegeId: null })
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  collegeId: [{ required: true, message: '请选择学院', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入详情', trigger: 'blur' }]
}

function collegeName(id) { return collegeOptions.find((c) => c.id === id)?.name || '—' }

function load() {
  loading.value = true
  listRequirement({ ...filters, pageNum: pageNum.value, pageSize: pageSize.value })
    .then((d) => { list.value = d.list || []; total.value = d.total || 0 })
    .catch(() => {})
    .finally(() => { loading.value = false })
}
function onPage(p) { pageNum.value = p; load() }
function go(id) { router.push(`/student/requirement/${id}`) }

function submit() {
  f.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    createRequirement({ ...form })
      .then(() => { ElMessage.success('发布成功'); dialog.value = false; f.value.resetFields(); load() })
      .catch(() => {})
      .finally(() => { submitting.value = false })
  })
}

onMounted(load)
</script>

<style scoped>
.head-row { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 14px; }
.filter-row { display: flex; gap: 12px; padding: 14px 18px; margin-bottom: 14px; flex-wrap: wrap; }
.req-list { display: flex; flex-direction: column; gap: 12px; }
.req-item { display: flex; gap: 14px; padding: 16px 18px; cursor: pointer; }
.req-item:hover { border-color: var(--c-primary-light); }
.req-status { display: flex; align-items: center; gap: 4px; font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 12px; height: fit-content; }
.req-status.open { color: var(--c-warning); background: #fff4e0; }
.req-status.done { color: var(--c-success); background: #e3f7ef; }
.req-main { flex: 1; min-width: 0; }
.req-title { font-size: 15px; font-weight: 700; }
.req-content { font-size: 13px; color: var(--c-text-sub); margin: 6px 0; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.req-meta { display: flex; gap: 16px; font-size: 12px; color: var(--c-text-sub); align-items: center; flex-wrap: wrap; }
.req-meta span { display: flex; align-items: center; gap: 3px; }
.req-time { margin-left: auto; }
.mt16 { margin-top: 16px; }
.center { justify-content: center; }
</style>
