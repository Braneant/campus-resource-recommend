<template>
  <div class="page-container" v-loading="loading">
    <el-row :gutter="18" v-if="res">
      <!-- 主内容 -->
      <el-col :span="17">
        <div class="app-card detail-card">
          <div class="detail-head">
            <div>
              <el-tag :type="typeTag">{{ typeLabel }}</el-tag>
              <el-tag v-if="res.status === 1" type="success" effect="light" class="ml6">已通过</el-tag>
              <h1 class="detail-title">{{ res.title }}</h1>
              <div class="detail-sub">
                <span><el-icon><User /></el-icon>{{ res.author || '佚名' }}</span>
                <span><el-icon><View /></el-icon>浏览 {{ res.viewCount || 0 }}</span>
                <span><el-icon><Download /></el-icon>下载 {{ res.downloadCount || 0 }}</span>
                <span class="rate"><el-icon><Star /></el-icon>评分 {{ res.avgRating ? res.avgRating.toFixed(1) : '—' }}</span>
                <span v-if="res.heat != null">热度 {{ (res.heat * 100).toFixed(0) }}%</span>
              </div>
            </div>
          </div>

          <div class="detail-actions">
            <el-button type="primary" :icon="View" @click="onPreview">在线预览</el-button>
            <el-button type="success" :icon="Download" @click="onDownload">下载资源</el-button>
            <el-button :type="isFav ? 'warning' : 'default'" :icon="Star" @click="toggleFav">
              {{ isFav ? '已收藏' : '收藏' }}
            </el-button>
            <span class="rate-self">我的评分：
              <el-rate v-model="myRate" :max="5" @change="onRate" />
            </span>
          </div>

          <el-divider />
          <div class="detail-desc">
            <div class="block-label">资源简介</div>
            <p>{{ res.description || '该资源暂无简介。' }}</p>
          </div>

          <!-- 可解释推荐卡片 -->
          <div class="explain-box" v-if="explain">
            <div class="block-label"><el-icon><MagicStick /></el-icon> 为什么向你推荐它？</div>
            <p class="explain-reason">{{ explain.reason }}</p>
            <div class="explain-sources">
              <el-tag v-for="s in explain.sources" :key="s" :class="['src-tag', s]" effect="dark">
                {{ srcMap[s]?.label }}
              </el-tag>
            </div>
            <div class="explain-bar" v-if="explain.scoreBreakdown">
              <div class="eb-item"><span>协同过滤</span><div class="eb-track"><i :style="{width: pct(explain.scoreBreakdown.cf)}"></i></div></div>
              <div class="eb-item"><span>知识图谱</span><div class="eb-track"><i class="kg" :style="{width: pct(explain.scoreBreakdown.kg)}"></i></div></div>
              <div class="eb-item"><span>热度</span><div class="eb-track"><i class="hot" :style="{width: pct(explain.scoreBreakdown.hot)}"></i></div></div>
              <div class="eb-item final"><span>综合得分</span><b>{{ explain.scoreBreakdown.final?.toFixed(2) }}</b></div>
            </div>
          </div>
        </div>

        <!-- 评论区 -->
        <div class="app-card comment-card">
          <div class="block-label">评论（{{ res.commentCount || comments.length }}）</div>
          <div class="comment-input">
            <el-input v-model="commentText" type="textarea" :rows="3" placeholder="说点什么吧~" maxlength="200" show-word-limit />
            <el-button type="primary" :icon="Promotion" @click="submitComment" :disabled="!commentText.trim()">发表评论</el-button>
          </div>
          <el-divider />
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <el-avatar :size="34">{{ c.nickname?.charAt(0) || '匿' }}</el-avatar>
            <div class="comment-body">
              <div class="comment-name">{{ c.nickname || '匿名' }} <span class="comment-time">{{ c.createTime }}</span></div>
              <div class="comment-text">{{ c.content }}</div>
              <div class="comment-like"><el-icon><Pointer /></el-icon>{{ c.likeCount || 0 }}</div>
            </div>
          </div>
          <el-empty v-if="comments.length === 0" description="还没有评论，快来抢沙发" />
        </div>
      </el-col>

      <!-- 侧栏：相关推荐 -->
      <el-col :span="7">
        <div class="app-card side-card">
          <div class="block-label"><el-icon><Connection /></el-icon> 相关知识资源</div>
          <div v-for="r in related" :key="r.resourceId" class="rel-item" @click="goResource(r.resourceId)">
            <div class="rel-info">
              <div class="rel-title">{{ r.title }}</div>
              <div class="rel-meta">
                <span :class="['src-tag', r.source]">{{ srcMap[r.source]?.label }}</span>
                <span class="rel-score">匹配 {{ (r.recommendScore * 100).toFixed(0) }}%</span>
              </div>
            </div>
          </div>
          <el-empty v-if="related.length === 0" description="暂无相关推荐" :image-size="60" />
        </div>

        <div class="app-card side-card">
          <div class="block-label">资源信息</div>
          <ul class="info-list">
            <li><span>资源编号</span><b>{{ res.resourceNo || '—' }}</b></li>
            <li><span>所属课程</span><b>{{ courseName(res.courseId) }}</b></li>
            <li><span>资源分类</span><b>{{ categoryName(res.categoryId) }}</b></li>
            <li><span>主知识点</span><b>KP#{{ res.kpId || '—' }}</b></li>
            <li><span>评论数</span><b>{{ res.commentCount || 0 }}</b></li>
          </ul>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, View, Download, Star, Promotion, Pointer, MagicStick, Connection } from '@element-plus/icons-vue'
import { getResource, previewResource, downloadResource, getComments, addComment, favoriteResource, cancelFavorite, rateResource } from '@/api/resource'
import { relatedRecommend, explainRecommend } from '@/api/recommend'
import { useUserStore } from '@/store/user'
import { ResourceTypeMap, RecommendSourceMap } from '@/utils/constants'
import { categoryOptions, courseOptions } from '@/utils/options'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const id = route.params.id

const res = ref(null)
const loading = ref(false)
const isFav = ref(false)
const myRate = ref(0)
const comments = ref([])
const commentText = ref('')
const related = ref([])
const explain = ref(null)
const srcMap = RecommendSourceMap

const typeLabel = computed(() => ResourceTypeMap[res.value?.type]?.label || '资源')
const typeTag = computed(() => ResourceTypeMap[res.value?.type]?.tag || 'info')

function courseName(cid) { return courseOptions.find((c) => c.id === cid)?.name || '—' }
function categoryName(cid) { return categoryOptions.find((c) => c.id === cid)?.name || '—' }

function pct(v) { return (Math.min(1, v || 0) * 100).toFixed(0) + '%' }

function load() {
  loading.value = true
  getResource(id)
    .then((data) => { res.value = data; myRate.value = data.avgRating ? Math.round(data.avgRating) : 0 })
    .catch(() => {})
    .finally(() => { loading.value = false })
  loadComments()
  loadRelated()
  loadExplain()
}

function loadComments() {
  getComments(id, { pageNum: 1, pageSize: 20 })
    .then((d) => { comments.value = d.list || [] })
    .catch(() => {})
}

function loadRelated() {
  relatedRecommend({ resourceId: id, topN: 6 })
    .then((d) => { related.value = d || [] })
    .catch(() => {})
}

function loadExplain() {
  if (!userStore.userId) return
  explainRecommend({ userId: userStore.userId, resourceId: id })
    .then((d) => { explain.value = d })
    .catch(() => {})
}

function onPreview() {
  previewResource(id)
    .then((d) => {
      if (d.fileUrl) window.open(d.fileUrl, '_blank')
      else ElMessage.info('该资源暂不支持在线预览')
    })
    .catch(() => {})
}

function onDownload() {
  downloadResource(id)
    .then((d) => {
      if (d.fileUrl) window.open(d.fileUrl, '_blank')
      else ElMessage.info('下载地址获取失败')
    })
    .catch(() => {})
}

async function toggleFav() {
  try {
    if (isFav.value) {
      await cancelFavorite(id)
      isFav.value = false
      ElMessage.success('已取消收藏')
    } else {
      await favoriteResource(id)
      isFav.value = true
      ElMessage.success('收藏成功')
    }
  } catch (e) {}
}

function onRate(score) {
  rateResource(id, { score })
    .then(() => ElMessage.success('评分成功'))
    .catch(() => {})
}

function submitComment() {
  if (!commentText.value.trim()) return
  addComment(id, { content: commentText.value.trim(), parentId: null })
    .then(() => { ElMessage.success('评论成功'); commentText.value = ''; loadComments() })
    .catch(() => {})
}

function goResource(rid) { router.push(`/student/resource/${rid}`) }

onMounted(load)
</script>

<style scoped>
.detail-card { padding: 24px 26px; }
.detail-title { font-size: 22px; margin: 10px 0 12px; }
.detail-sub { display: flex; flex-wrap: wrap; gap: 16px; color: var(--c-text-sub); font-size: 13px; align-items: center; }
.detail-sub span { display: flex; align-items: center; gap: 4px; }
.detail-sub .rate { color: var(--c-warning); }
.detail-actions { display: flex; align-items: center; gap: 12px; margin-top: 18px; flex-wrap: wrap; }
.rate-self { display: flex; align-items: center; gap: 8px; color: var(--c-text-sub); font-size: 13px; }
.block-label { font-weight: 700; font-size: 15px; margin-bottom: 12px; display: flex; align-items: center; gap: 6px; color: var(--c-text); }
.detail-desc p { line-height: 1.8; color: var(--c-text-sub); white-space: pre-wrap; margin: 0; }
.explain-box { margin-top: 18px; padding: 16px 18px; background: linear-gradient(135deg, #f3f7ff, #eef3ff); border-radius: 12px; border: 1px solid #e0e9ff; }
.explain-reason { line-height: 1.8; margin: 0 0 10px; color: #314166; }
.explain-sources { display: flex; gap: 8px; margin-bottom: 12px; }
.explain-bar { display: flex; flex-direction: column; gap: 8px; }
.eb-item { display: flex; align-items: center; gap: 10px; font-size: 13px; }
.eb-item > span { width: 64px; color: var(--c-text-sub); }
.eb-track { flex: 1; height: 8px; background: #e6ecf5; border-radius: 5px; overflow: hidden; }
.eb-track i { display: block; height: 100%; background: var(--c-cf); border-radius: 5px; }
.eb-track i.kg { background: var(--c-kg); }
.eb-track i.hot { background: var(--c-hot); }
.eb-item.final { font-weight: 700; }
.comment-card { padding: 22px 26px; margin-top: 16px; }
.comment-input { display: flex; gap: 12px; align-items: flex-start; }
.comment-input .el-input { flex: 1; }
.comment-item { display: flex; gap: 12px; padding: 14px 0; border-bottom: 1px dashed var(--c-border); }
.comment-body { flex: 1; }
.comment-name { font-size: 13px; font-weight: 600; }
.comment-time { color: #aab4c5; font-weight: 400; margin-left: 8px; }
.comment-text { font-size: 14px; color: var(--c-text); line-height: 1.7; margin: 4px 0; }
.comment-like { font-size: 12px; color: var(--c-text-sub); display: flex; align-items: center; gap: 3px; }
.side-card { padding: 18px 20px; margin-bottom: 16px; }
.rel-item { display: flex; gap: 10px; padding: 10px 0; cursor: pointer; border-bottom: 1px dashed var(--c-border); }
.rel-item:hover .rel-title { color: var(--c-primary); }
.rel-title { font-size: 14px; font-weight: 600; line-height: 1.4; }
.rel-meta { display: flex; align-items: center; gap: 8px; margin-top: 6px; }
.rel-score { font-size: 12px; color: var(--c-text-sub); }
.info-list { list-style: none; padding: 0; margin: 0; }
.info-list li { display: flex; justify-content: space-between; padding: 9px 0; font-size: 13px; border-bottom: 1px dashed var(--c-border); }
.info-list span { color: var(--c-text-sub); }
.ml6 { margin-left: 6px; }
</style>
