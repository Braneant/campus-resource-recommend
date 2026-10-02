<template>
  <div class="page-container" v-loading="loading">
    <el-button text :icon="ArrowLeft" @click="$router.push('/student/requirement')" class="back-btn">返回列表</el-button>
    <div class="app-card detail" v-if="req">
      <div class="d-status" :class="req.status === 1 ? 'done' : 'open'">{{ req.status === 1 ? '已解决' : '待解决' }}</div>
      <h1 class="d-title">{{ req.title }}</h1>
      <div class="d-meta">
        <span><el-icon><User /></el-icon>{{ req.studentName }}</span>
        <span><el-icon><School /></el-icon>{{ collegeName(req.collegeId) }}</span>
        <span>{{ req.createTime }}</span>
      </div>
      <el-divider />
      <div class="d-content">{{ req.content }}</div>

      <div class="reply-head">全部回复（{{ (req.replies || []).length }}）</div>
      <div v-for="rp in req.replies" :key="rp.id" class="reply-item">
        <el-avatar :size="34">{{ rp.studentName?.charAt(0) || '匿' }}</el-avatar>
        <div class="reply-body">
          <div class="reply-name">{{ rp.studentName }} <span class="reply-time">{{ rp.createTime }}</span></div>
          <div class="reply-text">{{ rp.content }}</div>
          <div class="reply-like"><el-icon><Pointer /></el-icon>{{ rp.likeCount || 0 }}</div>
        </div>
      </div>
      <el-empty v-if="!(req.replies || []).length" description="还没有人回复，伸出援手吧" />

      <el-divider />
      <div class="reply-box">
        <el-input v-model="replyText" type="textarea" :rows="3" placeholder="提供帮助或资源链接…" maxlength="300" show-word-limit />
        <div class="reply-actions">
          <el-button type="primary" :icon="Promotion" :disabled="!replyText.trim()" @click="submitReply">回复</el-button>
          <el-button v-if="isOwner && req.status === 0" type="success" :icon="CircleCheck" @click="markResolve">标记为已解决</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, User, School, Pointer, Promotion, CircleCheck } from '@element-plus/icons-vue'
import { getRequirement, replyRequirement, resolveRequirement } from '@/api/requirement'
import { useUserStore } from '@/store/user'
import { collegeOptions } from '@/utils/options'

const route = useRoute()
const userStore = useUserStore()
const id = route.params.id
const req = ref(null)
const loading = ref(false)
const replyText = ref('')

const isOwner = computed(() => req.value && req.value.studentId === userStore.userId)
function collegeName(cid) { return collegeOptions.find((c) => c.id === cid)?.name || '—' }

function load() {
  loading.value = true
  getRequirement(id).then((d) => { req.value = d }).catch(() => {}).finally(() => { loading.value = false })
}

function submitReply() {
  if (!replyText.value.trim()) return
  replyRequirement(id, { content: replyText.value.trim() })
    .then(() => { ElMessage.success('回复成功'); replyText.value = ''; load() })
    .catch(() => {})
}

function markResolve() {
  resolveRequirement(id).then(() => { ElMessage.success('已标记为解决'); load() }).catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.back-btn { margin-bottom: 12px; }
.detail { padding: 24px 28px; }
.d-status { display: inline-block; font-size: 12px; font-weight: 600; padding: 3px 12px; border-radius: 12px; margin-bottom: 10px; }
.d-status.open { color: var(--c-warning); background: #fff4e0; }
.d-status.done { color: var(--c-success); background: #e3f7ef; }
.d-title { font-size: 21px; margin: 0 0 10px; }
.d-meta { display: flex; gap: 16px; font-size: 13px; color: var(--c-text-sub); align-items: center; }
.d-meta span { display: flex; align-items: center; gap: 4px; }
.d-content { line-height: 1.9; color: #314166; white-space: pre-wrap; }
.reply-head { font-weight: 700; margin: 18px 0 12px; }
.reply-item { display: flex; gap: 12px; padding: 12px 0; border-bottom: 1px dashed var(--c-border); }
.reply-body { flex: 1; }
.reply-name { font-size: 13px; font-weight: 600; }
.reply-time { color: #aab4c5; font-weight: 400; margin-left: 8px; }
.reply-text { font-size: 14px; line-height: 1.7; margin: 4px 0; }
.reply-like { font-size: 12px; color: var(--c-text-sub); display: flex; align-items: center; gap: 3px; }
.reply-box { margin-top: 8px; }
.reply-actions { display: flex; gap: 10px; margin-top: 10px; }
</style>
