<template>
  <div class="page-container">
    <div class="head-row">
      <div>
        <h2 class="page-title">问答广场</h2>
        <p class="page-subtitle">提问、解答、沉淀学习经验</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="dialog = true">发布帖子</el-button>
    </div>

    <div class="filter-row app-card">
      <el-input v-model="filters.keyword" placeholder="搜索问题" :prefix-icon="Search" clearable style="width:260px" @keyup.enter="load" @clear="load" />
      <el-select v-model="filters.categoryId" placeholder="分类" clearable style="width:150px" @change="load">
        <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable style="width:130px" @change="load">
        <el-option :value="0" label="正常" /><el-option :value="1" label="已结帖" />
      </el-select>
    </div>

    <div v-loading="loading && list.length > 0" class="qa-list">
      <CardSkeleton v-if="loading && list.length === 0" :rows="4" />
      <template v-else>
        <div v-for="p in list" :key="p.id" class="app-card qa-item" @click="go(p.id)">
          <div class="qa-stats">
            <div class="qs-num">{{ p.replyCount || 0 }}</div><div class="qs-lbl">回答</div>
            <div class="qs-num">{{ p.likeCount || 0 }}</div><div class="qs-lbl">赞</div>
          </div>
          <div class="qa-main">
            <div class="qa-title">{{ p.title }}</div>
            <div class="qa-content">{{ p.content }}</div>
            <div class="qa-meta">
              <span><el-icon><User /></el-icon>{{ p.studentName || p.author }}</span>
              <span v-if="p.tags" class="qa-tags"><el-tag v-for="t in p.tags.split(',')" :key="t" size="small" effect="plain">{{ t }}</el-tag></span>
              <span class="qa-time">{{ p.createTime }}</span>
              <el-tag v-if="p.status === 1" size="small" type="info" effect="plain">已结帖</el-tag>
            </div>
          </div>
        </div>
      </template>
      <StateBlock
        v-if="!loading && list.length === 0"
        :type="filters.keyword || filters.categoryId != null || filters.status != null ? 'no-result' : 'empty'"
        :title="filters.keyword || filters.categoryId != null || filters.status != null ? '没有匹配的帖子' : '还没有人提问'"
        :description="filters.keyword || filters.categoryId != null || filters.status != null ? '可以更换关键词，或清空筛选条件后重新查找' : '把你的学习疑问抛出来，同学和老师都能看到'"
        :action-text="filters.keyword || filters.categoryId != null || filters.status != null ? '清空筛选' : '发布帖子'"
        @action="filters.keyword || filters.categoryId != null || filters.status != null ? resetFilters() : (dialog = true)"
      />
    </div>

    <el-pagination v-if="total > 0" layout="prev,pager,next,total" :total="total" :page-size="pageSize"
      :current-page="pageNum" @current-change="onPage" background class="mt16 center" />

    <el-dialog v-model="dialog" title="发布提问" width="540px">
      <el-form :model="form" :rules="rules" ref="f" label-width="70px">
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" placeholder="清晰描述你的问题" /></el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" style="width:100%">
            <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="tags"><el-input v-model="form.tags" placeholder="用逗号分隔，如 算法,DP" /></el-form-item>
        <el-form-item label="内容" prop="content"><el-input v-model="form.content" type="textarea" :rows="4" placeholder="补充问题背景与你的思考" /></el-form-item>
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
import { Plus, Search, User } from '@element-plus/icons-vue'
import { listQa, createQa } from '@/api/qa'
import { categoryOptions } from '@/utils/options'

const router = useRouter()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const filters = reactive({ keyword: '', categoryId: null, status: null })
const dialog = ref(false)
const submitting = ref(false)
const f = ref(null)
const form = reactive({ title: '', content: '', categoryId: null, tags: '' })
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

/** 清空筛选条件（供空态的行动按钮调用） */
function resetFilters() {
  filters.keyword = ''
  filters.categoryId = null
  filters.status = null
  pageNum.value = 1
  load()
}

function load() {
  loading.value = true
  listQa({ ...filters, pageNum: pageNum.value, pageSize: pageSize.value })
    .then((d) => { list.value = d.list || []; total.value = d.total || 0 })
    .catch(() => {})
    .finally(() => { loading.value = false })
}
function onPage(p) { pageNum.value = p; load() }
function go(id) { router.push(`/student/qa/${id}`) }

function submit() {
  f.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    createQa({ ...form })
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
.qa-list { display: flex; flex-direction: column; gap: 12px; }
.qa-item { display: flex; gap: 16px; padding: 16px 18px; cursor: pointer; }
.qa-item:hover { border-color: var(--c-primary-light); }
.qa-stats { width: 70px; flex-shrink: 0; text-align: center; border-right: 1px dashed var(--c-border); }
.qs-num { font-size: 18px; font-weight: 800; color: var(--c-primary); }
.qs-lbl { font-size: 11px; color: var(--c-text-sub); margin-bottom: 8px; }
.qa-main { flex: 1; min-width: 0; }
.qa-title { font-size: 15px; font-weight: 700; }
.qa-content { font-size: 13px; color: var(--c-text-sub); margin: 6px 0; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.qa-meta { display: flex; gap: 14px; font-size: 12px; color: var(--c-text-sub); align-items: center; flex-wrap: wrap; }
.qa-meta span { display: flex; align-items: center; gap: 4px; }
.qa-tags { gap: 6px !important; }
.qa-time { margin-left: auto; }
.mt16 { margin-top: 16px; }
.center { justify-content: center; }
</style>
