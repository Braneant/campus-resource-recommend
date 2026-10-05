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
  { key: 'resourcePassed', label: '已通过', icon: Files, bg: '#fff4e0' },
  { key: 'requirementCount', label: '互助需求', icon: ChatLineSquare, bg: '#f3edff' },
  { key: 'qaCount', label: '问答帖子', icon: Share, bg: '#e6f7ff' },
  { key: 'kpCount', label: '知识点', icon: School, bg: '#ffeef0' }
]

const trendRef = ref(null)
const catRef = ref(null)
const kpRef = ref(null)
const collegeRef = ref(null)
let trendChart, catChart, kpChart, collegeChart

const palette = ['#2f6df6', '#5b8def', '#2bbf8a', '#8b5cf6', '#f5a623', '#8a9bb8']

/**
 * 图表统一"降噪"样式：坐标轴与网格用中性浅色，避免与数据争夺注意力；
 * 提示框统一为白底细边（去掉 ECharts 默认的重阴影）。
 */
const AXIS_LABEL = { color: '#606b7e', fontSize: 12 }
const AXIS_LINE = { lineStyle: { color: '#e6ecf5' } }
const SPLIT_LINE = { lineStyle: { color: '#eef1f6' } }
const TOOLTIP_STYLE = {
  backgroundColor: '#fff',
  borderColor: '#e6ecf5',
  borderWidth: 1,
  padding: [8, 12],
  textStyle: { color: '#2b3445', fontSize: 12 },
  extraCssText: 'box-shadow: 0 4px 12px rgba(31,42,68,.08); border-radius: 8px;'
}

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
    tooltip: { trigger: 'axis', ...TOOLTIP_STYLE },
    grid: { left: 50, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: d.dates || [], axisLabel: { rotate: 35, ...AXIS_LABEL }, axisLine: AXIS_LINE, axisTick: { show: false } },
    yAxis: { type: 'value', axisLabel: AXIS_LABEL, splitLine: SPLIT_LINE, axisLine: { show: false } },
    series: [{ type: 'line', smooth: true, data: d.counts || [], areaStyle: { opacity: 0.12 },
      itemStyle: { color: palette[0] }, lineStyle: { width: 3 }, symbolSize: 6 }]
  })
}

function renderCat(d) {
  if (!catChart || !d) return
  catChart.setOption({
    tooltip: { trigger: 'item', ...TOOLTIP_STYLE },
    legend: { bottom: 0, type: 'scroll', icon: 'circle', textStyle: { color: '#606b7e', fontSize: 12 } },
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
    tooltip: { trigger: 'axis', valueFormatter: (v) => (v * 100).toFixed(0) + '%', ...TOOLTIP_STYLE },
    grid: { left: 110, right: 30, top: 10, bottom: 20 },
    xAxis: { type: 'value', max: 1, axisLabel: { formatter: (v) => (v * 100).toFixed(0) + '%', ...AXIS_LABEL }, splitLine: SPLIT_LINE, axisLine: { show: false } },
    yAxis: { type: 'category', data: items.map((i) => i.name), axisLabel: AXIS_LABEL, axisLine: AXIS_LINE, axisTick: { show: false } },
    series: [{ type: 'bar', barWidth: '56%', data: items.map((i) => ({ value: i.heat, itemStyle: { color: palette[2], borderRadius: [0, 6, 6, 0] } })) }]
  })
}

function renderCollege(d) {
  if (!collegeChart || !d) return
  collegeChart.setOption({
    tooltip: { trigger: 'axis', ...TOOLTIP_STYLE },
    grid: { left: 80, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: (d.items || []).map((i) => i.name), axisLabel: { rotate: 20, ...AXIS_LABEL }, axisLine: AXIS_LINE, axisTick: { show: false } },
    yAxis: { type: 'value', axisLabel: AXIS_LABEL, splitLine: SPLIT_LINE, axisLine: { show: false } },
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
.dash-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: var(--sp-4); }
.stat-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 14px; margin-bottom: var(--sp-4); }
.stat-card { display: flex; align-items: center; gap: 12px; padding: var(--sp-4); }
.sc-icon { width: 46px; height: 46px; border-radius: var(--r-md); display: flex; align-items: center; justify-content: center; font-size: 22px; color: var(--c-primary); flex-shrink: 0; }
.sc-body { min-width: 0; }
/* KPI 数字：统一字号阶梯 + 等宽数字（避免数值变化时宽度跳动） */
.sc-num { font-size: var(--fs-display); font-weight: 500; color: var(--c-text); line-height: 1.15;
  font-variant-numeric: tabular-nums; }
/* 标签不换行：数字加大后标签列变窄，换行会破坏卡片的整行对齐 */
.sc-label { font-size: var(--fs-micro); color: var(--c-text-sub); margin-top: 2px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.chart-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: var(--sp-4); }
.chart-box { padding: var(--sp-4) 18px 8px; }
.span2 { grid-column: span 2; }
.chart-title { font-weight: 500; font-size: var(--fs-h3); color: var(--c-text); margin-bottom: 6px; }
.chart { height: 280px; }

/* 响应式：窄屏下 KPI 与图表由 6 列/2 列退化为 3 列/1 列，避免挤压 */
@media (max-width: 1199px) {
  .stat-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .chart-grid { grid-template-columns: minmax(0, 1fr); }
  .span2 { grid-column: auto; }
}
@media (max-width: 700px) {
  .stat-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .sc-num { font-size: 22px; }
}
</style>
