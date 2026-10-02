<template>
  <el-card class="res-card" shadow="hover" @click="emit('open', resource.id)">
    <div class="res-cover" :class="'ct' + (resource.type || 4)">
      <!-- 有真实封面时显示图片；占位域名或加载失败均回退为类型图标 -->
      <img
        v-if="hasCover"
        :src="coverSrc"
        :alt="resource.title"
        @error="imgFailed = true"
      />
      <component :is="typeIcon" v-else class="res-cover-icon" />
      <span class="res-type" :class="'t' + resource.type">{{ typeLabel }}</span>
      <span v-if="resource.heat != null" class="res-heat">热度 {{ formatHeat(resource.heat) }}</span>
    </div>
    <div class="res-body">
      <div class="res-title" :title="resource.title">{{ resource.title }}</div>
      <div class="res-author">
        <el-icon><User /></el-icon> {{ resource.author || '佚名' }}
        <span v-if="resource.resourceNo" class="res-no">{{ resource.resourceNo }}</span>
      </div>
      <div class="res-meta">
        <span><el-icon><View /></el-icon>{{ resource.viewCount || 0 }}</span>
        <span><el-icon><Download /></el-icon>{{ resource.downloadCount || 0 }}</span>
        <span class="res-rate"><el-icon><Star /></el-icon>{{ resource.avgRating ? resource.avgRating.toFixed(1) : '—' }}</span>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed, ref } from 'vue'
import { User, View, Download, Star, Document, VideoPlay, Notebook, Files } from '@element-plus/icons-vue'
import { ResourceTypeMap } from '@/utils/constants'
import { usableMediaUrl } from '@/utils/media'

const props = defineProps({ resource: { type: Object, required: true } })
const emit = defineEmits(['open'])

/** 封面图加载失败标记（真实封面加载失败时回退为类型图标） */
const imgFailed = ref(false)

/** 可用封面地址：占位域名直接视为无封面，避免无效请求与 alt 文本残留 */
const coverSrc = computed(() => usableMediaUrl(props.resource.coverUrl))
const hasCover = computed(() => !!coverSrc.value && !imgFailed.value)

const typeLabel = computed(() => ResourceTypeMap[props.resource.type]?.label || '资源')

/** 按资源类型展示对应图标，替代原先居中重复的类型文字 */
const TYPE_ICONS = { 1: Document, 2: VideoPlay, 3: Notebook, 4: Files }
const typeIcon = computed(() => TYPE_ICONS[props.resource.type] || Files)

function formatHeat(h) {
  return typeof h === 'number' ? (h * 100).toFixed(0) + '%' : h
}
</script>

<style scoped>
.res-card { cursor: pointer; border-radius: 12px; overflow: hidden; border: 1px solid var(--c-border);
  transition: transform .18s ease, box-shadow .18s ease, border-color .18s ease; }
.res-card:hover { transform: translateY(-4px); border-color: var(--c-primary-light);
  box-shadow: 0 10px 26px rgba(47, 109, 246, .14); }

.res-cover { position: relative; height: 132px; overflow: hidden;
  display: flex; align-items: center; justify-content: center; }
.res-cover img { width: 100%; height: 100%; object-fit: cover; }

/* 按资源类型着色：文档蓝 / 视频绿 / 课件琥珀 / 其他灰 */
.res-cover.ct1 { background: linear-gradient(135deg, #e9f1ff, #d3e2ff); }
.res-cover.ct2 { background: linear-gradient(135deg, #e2f7ee, #c7ebdc); }
.res-cover.ct3 { background: linear-gradient(135deg, #fff4e2, #ffe5bd); }
.res-cover.ct4 { background: linear-gradient(135deg, #eef2f8, #dfe6f1); }

/* 点阵纹理：消除纯色扁平感 */
.res-cover::before { content: ''; position: absolute; inset: 0; opacity: .55;
  background-image: radial-gradient(circle, rgba(255, 255, 255, .85) 1px, transparent 1.2px);
  background-size: 14px 14px; }

/* 悬停微光扫过 */
.res-cover::after { content: ''; position: absolute; top: 0; bottom: 0; width: 55%; left: -60%;
  transform: skewX(-18deg);
  background: linear-gradient(90deg, rgba(255,255,255,0), rgba(255,255,255,.6), rgba(255,255,255,0));
  transition: left .55s ease; }
.res-card:hover .res-cover::after { left: 110%; }

.res-cover-icon { position: relative; z-index: 1; font-size: 46px;
  transition: transform .25s ease; }
.res-cover.ct1 .res-cover-icon { color: #6f9dee; }
.res-cover.ct2 .res-cover-icon { color: #4fc79b; }
.res-cover.ct3 .res-cover-icon { color: #e8a63a; }
.res-cover.ct4 .res-cover-icon { color: #9aa8bd; }
.res-card:hover .res-cover-icon { transform: scale(1.12); }

.res-type { position: absolute; left: 8px; top: 8px; z-index: 2; font-size: 12px; color: #fff;
  padding: 1px 8px; border-radius: 9px; background: var(--c-primary); }
.res-type.t2 { background: var(--c-success); }
.res-type.t3 { background: var(--c-warning); }
.res-type.t4 { background: var(--c-info); }

.res-heat { position: absolute; right: 8px; top: 8px; z-index: 2; font-size: 11px; color: #fff;
  background: rgba(0, 0, 0, .4); padding: 1px 7px; border-radius: 9px; }

.res-body { padding: 10px 12px 12px; }
.res-title { font-size: 14px; font-weight: 600; color: var(--c-text); line-height: 1.4; height: 38px; overflow: hidden;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; transition: color .18s ease; }
.res-card:hover .res-title { color: var(--c-primary); }
.res-author { font-size: 12px; color: var(--c-text-sub); margin: 8px 0 6px; display: flex; align-items: center; gap: 4px; }
.res-no { margin-left: auto; color: #aab4c5; font-size: 11px; }
.res-meta { display: flex; gap: 14px; font-size: 12px; color: var(--c-text-sub); align-items: center; }
.res-meta span { display: flex; align-items: center; gap: 3px; }
.res-rate { color: var(--c-warning); }

/* 尊重系统「减弱动态效果」偏好 */
@media (prefers-reduced-motion: reduce) {
  .res-card, .res-card:hover { transition: none; transform: none; }
  .res-cover::after { display: none; }
  .res-card:hover .res-cover-icon { transform: none; }
}
</style>
