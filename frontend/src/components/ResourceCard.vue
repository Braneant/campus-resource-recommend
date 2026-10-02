<template>
  <el-card class="res-card" shadow="hover" @click="emit('open', resource.id)">
    <div class="res-cover">
      <img v-if="resource.coverUrl" :src="resource.coverUrl" :alt="resource.title" />
      <div v-else class="res-cover-ph">{{ typeLabel }}</div>
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
import { computed } from 'vue'
import { User, View, Download, Star } from '@element-plus/icons-vue'
import { ResourceTypeMap } from '@/utils/constants'

const props = defineProps({ resource: { type: Object, required: true } })
const emit = defineEmits(['open'])

const typeLabel = computed(() => ResourceTypeMap[props.resource.type]?.label || '资源')

function formatHeat(h) {
  return typeof h === 'number' ? (h * 100).toFixed(0) + '%' : h
}
</script>

<style scoped>
.res-card { cursor: pointer; border-radius: 12px; overflow: hidden; border: 1px solid var(--c-border); transition: transform .15s; }
.res-card:hover { transform: translateY(-3px); }
.res-cover { position: relative; height: 132px; background: linear-gradient(135deg, #eaf1ff, #dfe9ff); display: flex; align-items: center; justify-content: center; }
.res-cover img { width: 100%; height: 100%; object-fit: cover; }
.res-cover-ph { font-size: 15px; color: var(--c-primary); font-weight: 600; }
.res-type { position: absolute; left: 8px; top: 8px; font-size: 12px; color: #fff; padding: 1px 8px; border-radius: 9px; background: var(--c-primary); }
.res-type.t2 { background: var(--c-success); }
.res-type.t3 { background: var(--c-warning); }
.res-type.t4 { background: var(--c-info); }
.res-heat { position: absolute; right: 8px; top: 8px; font-size: 11px; color: #fff; background: rgba(0,0,0,.4); padding: 1px 7px; border-radius: 9px; }
.res-body { padding: 10px 12px 12px; }
.res-title { font-size: 14px; font-weight: 600; color: var(--c-text); line-height: 1.4; height: 38px; overflow: hidden;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.res-author { font-size: 12px; color: var(--c-text-sub); margin: 8px 0 6px; display: flex; align-items: center; gap: 4px; }
.res-no { margin-left: auto; color: #aab4c5; font-size: 11px; }
.res-meta { display: flex; gap: 14px; font-size: 12px; color: var(--c-text-sub); align-items: center; }
.res-meta span { display: flex; align-items: center; gap: 3px; }
.res-rate { color: var(--c-warning); }
</style>
