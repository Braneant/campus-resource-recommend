<template>
  <div class="page-container dash">
    <div class="dash-head">
      <div>
        <h2 class="page-title">运营数据大屏</h2>
        <p class="page-subtitle">校园学习资源平台实时运营概览</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadAll">刷新数据</el-button>
    </div>

    <!-- 概览卡片 -->
    <div class="stat-grid">
      <div class="app-card stat-card" v-for="s in statCards" :key="s.label">
        <div class="sc-icon" :style="{background: s.bg}"><el-icon><component :is="s.icon" /></el-icon></div>
        <div class="sc-body">
          <div class="sc-num">{{ stats[s.key] ?? '—' }}</div>
          <div class="sc-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="app-card chart-box span2">
        <div class="chart-title">资源增长趋势</div>
        <div ref="trendRef" class="chart"></div>
      </div>
      <div class="app-card chart-box">
        <div class="chart-title">资源分类占比</div>
        <div ref="catRef" class="chart"></div>
      </div>
      <div class="app-card chart-box">
        <div class="chart-title">知识点热度 Top</div>
        <div ref="kpRef" class="chart"></div>
      </div>
      <div class="app-card chart-box span2">
        <div class="chart-title">学院资源量排行</div>
        <div ref="collegeRef" class="chart"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Refresh, User, Document, Files, ChatLineSquare, Share, School } from '@element-plus/icons-vue'
import { dashboardStats, dashboardResourceTrend, dashboardCategoryDist, dashboardKpHeat, dashboardCollegeRank } from '@/api/admin'

const loading = ref(false)
const stats = reactive({})
const statCards = [
  { key: 'userCount', label: '学生用户', icon: User, bg: '#eaf1ff' },
  { key: 'resourceCount', label: '资源总数', icon: Document, bg: '#e3f7ef' },
  { key: 'resourcePassed', label: '已通过资源', icon: Files, bg: '#fff4e0' },
  { key: 'requirementCount', label: '互助需求', icon: ChatLineSquare, bg: '#f3edff' },
  { key: 'qaCount', label: '问答帖子', icon: Share, bg: '#e6f7ff' },
  { key: 'kpCount', label: '知识点', icon: School, bg: '#ffeef0' }
]

const trendRef = ref(null)
const catRef = ref(null)
const kpRef = ref(null)
const collegeRef = ref(null)
let trendChart, catChart, kpChart, collegeChart

const palette = ['#2f6df6', '#5b8def', '#2bbf8a', '#8b5cf6', '#f5a623', '#f5544c']

function loadAll() {
  loading.value = true
  Promise.all([
    dashboardStats().catch(() => null),
    dashboardResourceTrend().catch(() => null),
    dashboardCategoryDist().catch(() => null),
    dashboardKpHeat().catch(() => null),
    dashboardCollegeRank().catch(() => null)
  ]).then(([s, trend, cat, kp, college]) => {
    if (s) Object.assign(stats, s)
    renderTrend(trend)
    renderCat(cat)
    renderKp(kp)
    renderCollege(college)
  }).finally(() => { loading.value = false })
}

function renderTrend(d) {
  if (!trendChart || !d) return
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: d.dates || [], axisLabel: { rotate: 35 } },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: d.counts || [], areaStyle: { opacity: 0.12 },
      itemStyle: { color: palette[0] }, lineStyle: { width: 3 }, symbolSize: 6 }]
  })
}

function renderCat(d) {
  if (!catChart || !d) return
  catChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, type: 'scroll', icon: 'circle' },
    series: [{
      type: 'pie', radius: ['40%', '66%'], center: ['50%', '44%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{d}%' },
      data: (d.items || []).map((it, i) => ({ name: it.name || it.category, value: it.value, itemStyle: { color: palette[i % palette.length] } }))
    }]
  })
}

function renderKp(d) {
  if (!kpChart || !d) return
  const items = (d.items || []).slice().sort((a, b) => a.heat - b.heat)
  kpChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v) => (v * 100).toFixed(0) + '%' },
    grid: { left: 110, right: 30, top: 10, bottom: 20 },
    xAxis: { type: 'value', max: 1, axisLabel: { formatter: (v) => (v * 100).toFixed(0) + '%' } },
    yAxis: { type: 'category', data: items.map((i) => i.name), axisLabel: { fontSize: 12 } },
    series: [{ type: 'bar', barWidth: '56%', data: items.map((i) => ({ value: i.heat, itemStyle: { color: palette[2], borderRadius: [0, 6, 6, 0] } })) }]
  })
}

function renderCollege(d) {
  if (!collegeChart || !d) return
  collegeChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 80, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: (d.items || []).map((i) => i.name), axisLabel: { rotate: 20 } },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', barWidth: '46%', data: (d.items || []).map((i, idx) => ({ value: i.value, itemStyle: { color: palette[idx % palette.length], borderRadius: [6, 6, 0, 0] } })) }]
  })
}

function resizeAll() { [trendChart, catChart, kpChart, collegeChart].forEach((c) => c && c.resize()) }
window.addEventListener('resize', resizeAll)
onBeforeUnmount(() => { window.removeEventListener('resize', resizeAll); [trendChart, catChart, kpChart, collegeChart].forEach((c) => c && c.dispose()) })

onMounted(() => {
  nextTick(() => {
    trendChart = echarts.init(trendRef.value)
    catChart = echarts.init(catRef.value)
    kpChart = echarts.init(kpRef.value)
    collegeChart = echarts.init(collegeRef.value)
    loadAll()
  })
})
</script>

<style scoped>
.dash { max-width: 1280px; }
.dash-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 16px; }
.stat-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 14px; margin-bottom: 16px; }
.stat-card { display: flex; align-items: center; gap: 14px; padding: 18px; }
.sc-icon { width: 46px; height: 46px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 22px; color: var(--c-primary); }
.sc-num { font-size: 24px; font-weight: 800; color: var(--c-text); line-height: 1.1; }
.sc-label { font-size: 12px; color: var(--c-text-sub); margin-top: 4px; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-box { padding: 16px 18px 8px; }
.span2 { grid-column: span 2; }
.chart-title { font-weight: 700; font-size: 15px; margin-bottom: 6px; }
.chart { height: 280px; }
</style>
