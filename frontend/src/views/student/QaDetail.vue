<template>
  <div class="page-container" v-loading="loading">
    <el-button text :icon="ArrowLeft" @click="$router.push('/student/qa')" class="back-btn">返回问答广场</el-button>
    <div class="app-card detail" v-if="post">
      <div class="d-status" :class="post.status === 1 ? 'closed' : 'open'">{{ post.status === 1 ? '已结帖' : '正常' }}</div>
      <h1 class="d-title">{{ post.title }}</h1>
      <div class="d-meta">
        <span><el-icon><User /></el-icon>{{ post.studentName || post.author }}</span>
        <span v-if="post.tags" class="qa-tags"><el-tag v-for="t in post.tags.split(',')" :key="t" size="small" effect="plain">{{ t }}</el-tag></span>
        <span>{{ post.createTime }}</span>
        <span><el-icon><View /></el-icon>{{ post.viewCount || 0 }}</span>
      </div>
      <el-divider />
      <div class="d-content">{{ post.content }}</div>

      <div class="reply-head">全部回答（{{ (post.replies || []).length }}）</div>
      <div v-for="rp in post.replies" :key="rp.id" class="reply-item" :class="{ best: rp.isBest }">
        <el-avatar :size="36">{{ rp.studentName?.charAt(0) || '匿' }}</el-avatar>
        <div class="reply-body">
          <div class="reply-name">
            {{ rp.studentName }}
            <el-tag v-if="rp.isBest" type="success" size="small" effect="dark"><el-icon><Medal /></el-icon>最佳答案</el-tag>
            <span class="reply-time">{{ rp.createTime }}</span>
          </div>
          <div class="reply-text">{{ rp.content }}</div>
          <div class="reply-actions">
            <span class="reply-like"><el-icon><Pointer /></el-icon>{{ rp.likeCount || 0 }}</span>
            <el-button v-if="isOwner && !rp.isBest && post.status === 0" text type="success" size="small" :icon="Medal" @click="setBest(rp.id)">设为最佳</el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!(post.replies || []).length" description="等待第一个回答" />

      <el-divider />
      <div class="reply-box">
        <el-input v-model="replyText" type="textarea" :rows="3" placeholder="写下你的回答…" maxlength="500" show-word-limit />
        <div class="reply-actions">
          <el-button type="primary" :icon="Promotion" :disabled="!replyText.trim()" @click="submitReply">发表回答</el-button>
          <el-button v-if="isOwner && post.status === 0" type="success" :icon="CircleCheck" @click="markClose">结帖</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, User, View, Pointer, Promotion, CircleCheck, Medal } from '@element-plus/icons-vue'
import { getQa, replyQa, setBestAnswer } from '@/api/qa'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()
const id = route.params.id
const post = ref(null)
const loading = ref(false)
const replyText = ref('')

const isOwner = computed(() => post.value && post.value.studentId === userStore.userId)

function load() {
  loading.value = true
  getQa(id).then((d) => { post.value = d }).catch(() => {}).finally(() => { loading.value = false })
}

function submitReply() {
  if (!replyText.value.trim()) return
  replyQa(id, { content: replyText.value.trim(), parentId: null })
    .then(() => { ElMessage.success('回答成功'); replyText.value = ''; load() })
    .catch(() => {})
}

function setBest(replyId) {
  setBestAnswer(replyId).then(() => { ElMessage.success('已设为最佳答案'); load() }).catch(() => {})
}

function markClose() {
  // 结帖：复用“设为最佳”前的状态切换，这里通过审核语义近似——演示用 best 接口占位
  ElMessage.info('结帖功能需在后端新增 /api/qa/{id}/close 接口')
}

onMounted(load)
</script>

<style scoped>
.back-btn { margin-bottom: 12px; }
.detail { padding: 24px 28px; }
.d-status { display: inline-block; font-size: 12px; font-weight: 600; padding: 3px 12px; border-radius: 12px; margin-bottom: 10px; }
.d-status.open { color: var(--c-success); background: #e3f7ef; }
.d-status.closed { color: var(--c-info); background: #eef1f6; }
.d-title { font-size: 21px; margin: 0 0 10px; }
.d-meta { display: flex; gap: 16px; font-size: 13px; color: var(--c-text-sub); align-items: center; flex-wrap: wrap; }
.d-meta span { display: flex; align-items: center; gap: 4px; }
.qa-tags { gap: 6px !important; }
.d-content { line-height: 1.9; color: #314166; white-space: pre-wrap; }
.reply-head { font-weight: 700; margin: 18px 0 12px; }
.reply-item { display: flex; gap: 12px; padding: 12px 0; border-bottom: 1px dashed var(--c-border); }
.reply-item.best { background: #f3fbf6; border-radius: 10px; padding: 14px 12px; }
.reply-body { flex: 1; }
.reply-name { font-size: 13px; font-weight: 600; display: flex; align-items: center; gap: 10px; }
.reply-time { color: #aab4c5; font-weight: 400; }
.reply-text { font-size: 14px; line-height: 1.7; margin: 4px 0; }
.reply-actions { display: flex; align-items: center; gap: 12px; margin-top: 4px; }
.reply-like { font-size: 12px; color: var(--c-text-sub); display: flex; align-items: center; gap: 3px; }
.reply-box { margin-top: 8px; }
</style>
