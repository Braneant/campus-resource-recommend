<template>
  <div class="page-container" v-loading="loading">
    <h2 class="page-title">个性化推荐</h2>
    <p class="page-subtitle">
      基于 <b style="color:var(--c-cf)">协同过滤</b> +
      <b style="color:var(--c-kg)">知识图谱</b> +
      <b style="color:var(--c-hot)">热度加权</b> 三路混合融合，为你量身推荐
    </p>

    <!-- 概览：三路来源可视化 -->
    <div class="overview">
      <div class="app-card ov-stat">
        <div class="ov-stat-title">三路来源构成</div>
        <div class="ov-stat-row">
          <div class="ov-pill cf"><span class="dot"></span>协同过滤 <b>{{ sourceCount.cf }}</b></div>
          <div class="ov-pill kg"><span class="dot"></span>知识图谱 <b>{{ sourceCount.kg }}</b></div>
          <div class="ov-pill hot"><span class="dot"></span>热度 <b>{{ sourceCount.hot }}</b></div>
        </div>
        <div ref="pieRef" class="ov-pie"></div>
        <div class="ov-hint">融合公式：score = 0.5·CF + 0.3·KG + 0.2·Hot（min-max 归一化后加权）</div>
      </div>

      <div class="app-card ov-stat">
        <div class="ov-stat-title">平均匹配度</div>
        <div ref="barRef" class="ov-bar"></div>
        <div class="ov-hint">各来源推荐项的平均融合得分，帮助理解推荐偏向</div>
      </div>
    </div>

    <!-- 工具条 -->
    <div class="tool-bar app-card">
      <div class="tb-left">
        <span>来源筛选：</span>
        <el-radio-group v-model="sourceFilter" @change="applyFilter">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="cf">协同过滤</el-radio-button>
          <el-radio-button value="kg">知识图谱</el-radio-button>
          <el-radio-button value="hot">热度</el-radio-button>
        </el-radio-group>
      </div>
      <div class="tb-right">
        <span>推荐数量：</span>
        <el-select v-model="topN" style="width:96px" @change="load">
          <el-option :value="8" label="8" /><el-option :value="10" label="10" />
          <el-option :value="15" label="15" /><el-option :value="20" label="20" />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="load">刷新推荐</el-button>
      </div>
    </div>

    <!-- 推荐列表 -->
    <div class="rec-list">
      <div v-for="(item, idx) in filtered" :key="item.resourceId" class="app-card rec-item">
        <div class="rec-rank" :class="{ top: idx < 3 }">{{ idx + 1 }}</div>
        <div class="rec-cover" @click="openDetail(item)">
          <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" />
          <div v-else class="rec-cover-ph">{{ srcMap[item.source]?.label }}</div>
        </div>
        <div class="rec-info" @click="openDetail(item)">
          <div class="rec-title-row">
            <span class="rec-title" :title="item.title">{{ item.title }}</span>
            <span :class="['src-tag', item.source]">{{ srcMap[item.source]?.label }}</span>
          </div>
          <div class="rec-author"><el-icon><User /></el-icon>{{ item.author || '佚名' }} · 评分 {{ item.avgRating ? item.avgRating.toFixed(1) : '—' }}</div>
          <div class="rec-reason">
            <el-icon class="rec-reason-ic"><MagicStick /></el-icon>
            <span>{{ item.reason }}</span>
          </div>
        </div>
        <div class="rec-right">
          <div class="rec-score-num">{{ (item.recommendScore * 100).toFixed(0) }}<small>%</small></div>
          <el-progress type="circle" :width="56" :percentage="Math.round(item.recommendScore * 100)"
            :color="srcMap[item.source]?.color" :stroke-width="6" />
          <el-button text type="primary" :icon="QuestionFilled" @click="openExplain(item)">为什么推荐我</el-button>
        </div>
      </div>
      <el-empty v-if="filtered.length === 0" description="暂无推荐结果" />
    </div>

    <!-- 推荐解释抽屉 -->
    <el-drawer v-model="explainVisible" title="为什么推荐给你" size="420px">
      <div v-if="explain" v-loading="explainLoading">
        <div class="ex-title">{{ explain.title }}</div>
        <div class="block-label"><el-icon><MagicStick /></el-icon> 推荐理由</div>
        <p class="ex-reason">{{ explain.reason }}</p>
        <div class="block-label">命中来源</div>
        <div class="ex-sources">
          <el-tag v-for="s in explain.sources" :key="s" :class="['src-tag', s]" effect="dark">{{ srcMap[s]?.label }}</el-tag>
        </div>
        <div class="block-label" style="margin-top:18px">融合得分拆解</div>
        <div class="ex-bar">
          <div class="eb-item"><span>协同过滤</span><div class="eb-track"><i :style="{width: pct(explain.scoreBreakdown?.cf)}"></i></div><b>{{ fmt(explain.scoreBreakdown?.cf) }}</b></div>
          <div class="eb-item"><span>知识图谱</span><div class="eb-track"><i class="kg" :style="{width: pct(explain.scoreBreakdown?.kg)}"></i></div><b>{{ fmt(explain.scoreBreakdown?.kg) }}</b></div>
          <div class="eb-item"><span>热度</span><div class="eb-track"><i class="hot" :style="{width: pct(explain.scoreBreakdown?.hot)}"></i></div><b>{{ fmt(explain.scoreBreakdown?.hot) }}</b></div>
          <div class="eb-item final"><span>综合</span><div class="eb-track"><i class="final-bar" :style="{width: pct(explain.scoreBreakdown?.final)}"></i></div><b>{{ fmt(explain.scoreBreakdown?.final) }}</b></div>
        </div>
        <el-alert type="info" :closable="false" class="ex-alert" title="可解释机制" description="推荐理由由规则模板基于你的收藏 / 下载 / 评分行为与知识图谱多跳关系自动拼装，不依赖大模型。" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { User, MagicStick, QuestionFilled, Refresh } from '@element-plus/icons-vue'
import { personalRecommend, explainRecommend } from '@/api/recommend'
import { useUserStore } from '@/store/user'
import { RecommendSourceMap } from '@/utils/constants'

const router = useRouter()
const userStore = useUserStore()
const srcMap = RecommendSourceMap

const list = ref([])
const loading = ref(false)
const topN = ref(10)
const sourceFilter = ref('all')
const filtered = computed(() =>
  sourceFilter.value === 'all' ? list.value : list.value.filter((i) => i.source === sourceFilter.value)
)

const sourceCount = reactive({ cf: 0, kg: 0, hot: 0 })

// 图表
const pieRef = ref(null)
const barRef = ref(null)
let pieChart = null
let barChart = null

function load() {
  if (!userStore.userId) {
    ElMessage.warning('请先登录')
    return
  }
  loading.value = true
  personalRecommend({ userId: userStore.userId, topN: topN.value })
    .then((data) => {
      list.value = data || []
      sourceCount.cf = list.value.filter((i) => i.source === 'cf').length
      sourceCount.kg = list.value.filter((i) => i.source === 'kg').length
      sourceCount.hot = list.value.filter((i) => i.source === 'hot').length
      renderCharts()
    })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function applyFilter() {}

function renderCharts() {
  nextTick(() => {
    // 饼图：来源构成
    if (pieRef.value) {
      pieChart = pieChart || echarts.init(pieRef.value)
      pieChart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0, icon: 'circle', itemWidth: 10, itemHeight: 10 },
        series: [{
          type: 'pie', radius: ['42%', '66%'], center: ['50%', '44%'],
          avoidLabelOverlap: true,
          itemStyle: { borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          data: [
            { name: '协同过滤', value: sourceCount.cf, itemStyle: { color: srcMap.cf.color } },
            { name: '知识图谱', value: sourceCount.kg, itemStyle: { color: srcMap.kg.color } },
            { name: '热度', value: sourceCount.hot, itemStyle: { color: srcMap.hot.color } }
          ]
        }]
      })
    }
    // 柱状图：平均融合得分
    if (barRef.value) {
      barChart = barChart || echarts.init(barRef.value)
      const avg = (src) => {
        const arr = list.value.filter((i) => i.source === src)
        if (!arr.length) return 0
        return arr.reduce((s, i) => s + i.recommendScore, 0) / arr.length
      }
      barChart.setOption({
        grid: { left: 60, right: 20, top: 20, bottom: 30 },
        tooltip: { trigger: 'axis', valueFormatter: (v) => (v * 100).toFixed(0) + '%' },
        xAxis: { type: 'category', data: ['协同过滤', '知识图谱', '热度'] },
        yAxis: { type: 'value', max: 1, axisLabel: { formatter: (v) => (v * 100).toFixed(0) + '%' } },
        series: [{
          type: 'bar', barWidth: '46%',
          data: [
            { value: avg('cf'), itemStyle: { color: srcMap.cf.color, borderRadius: [6, 6, 0, 0] } },
            { value: avg('kg'), itemStyle: { color: srcMap.kg.color, borderRadius: [6, 6, 0, 0] } },
            { value: avg('hot'), itemStyle: { color: srcMap.hot.color, borderRadius: [6, 6, 0, 0] } }
          ]
        }]
      })
    }
  })
}

function onResize() { pieChart && pieChart.resize(); barChart && barChart.resize() }
window.addEventListener('resize', onResize)
onBeforeUnmount(() => { window.removeEventListener('resize', onResize); pieChart && pieChart.dispose(); barChart && barChart.dispose() })

function pct(v) { return (Math.min(1, v || 0) * 100).toFixed(0) + '%' }
function fmt(v) { return v == null ? '—' : v.toFixed(2) }

function openDetail(item) { router.push(`/student/resource/${item.resourceId}`) }

// 解释抽屉
const explainVisible = ref(false)
const explainLoading = ref(false)
const explain = ref(null)
function openExplain(item) {
  explainVisible.value = true
  explainLoading.value = true
  explain.value = null
  explainRecommend({ userId: userStore.userId, resourceId: item.resourceId })
    .then((d) => { explain.value = d })
    .catch(() => {})
    .finally(() => { explainLoading.value = false })
}

onMounted(load)
</script>

<style scoped>
.overview { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.ov-stat { padding: 18px 20px; }
.ov-stat-title { font-weight: 700; font-size: 15px; margin-bottom: 12px; }
.ov-stat-row { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 6px; }
.ov-pill { display: flex; align-items: center; gap: 6px; font-size: 13px; padding: 4px 10px; border-radius: 14px; background: #f4f7fc; }
.ov-pill b { color: var(--c-text); }
.ov-pill .dot { width: 8px; height: 8px; border-radius: 50%; }
.ov-pill.cf .dot { background: var(--c-cf); }
.ov-pill.kg .dot { background: var(--c-kg); }
.ov-pill.hot .dot { background: var(--c-hot); }
.ov-pie { height: 220px; }
.ov-bar { height: 220px; }
.ov-hint { font-size: 12px; color: var(--c-text-sub); line-height: 1.6; margin-top: 4px; }
.tool-bar { display: flex; justify-content: space-between; align-items: center; padding: 14px 18px; margin-bottom: 16px; flex-wrap: wrap; gap: 10px; }
.tb-left, .tb-right { display: flex; align-items: center; gap: 10px; font-size: 13px; color: var(--c-text-sub); }
.rec-list { display: flex; flex-direction: column; gap: 14px; }
.rec-item { display: flex; align-items: center; gap: 16px; padding: 16px 18px; }
.rec-rank { width: 30px; height: 30px; flex-shrink: 0; border-radius: 8px; background: #eef2f9; color: var(--c-text-sub);
  display: flex; align-items: center; justify-content: center; font-weight: 700; }
.rec-rank.top { background: linear-gradient(135deg, #2f6df6, #5b8def); color: #fff; }
.rec-cover { width: 84px; height: 84px; flex-shrink: 0; border-radius: 10px; overflow: hidden; cursor: pointer;
  background: linear-gradient(135deg, #eaf1ff, #dfe9ff); display: flex; align-items: center; justify-content: center; }
.rec-cover img { width: 100%; height: 100%; object-fit: cover; }
.rec-cover-ph { font-size: 13px; color: var(--c-primary); font-weight: 600; padding: 6px; text-align: center; }
.rec-info { flex: 1; min-width: 0; cursor: pointer; }
.rec-title-row { display: flex; align-items: center; gap: 10px; }
.rec-title { font-size: 16px; font-weight: 700; color: var(--c-text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rec-author { font-size: 13px; color: var(--c-text-sub); margin: 6px 0; display: flex; align-items: center; gap: 4px; }
.rec-reason { font-size: 13px; color: #44506b; line-height: 1.6; display: flex; gap: 6px; align-items: flex-start; }
.rec-reason-ic { color: var(--c-primary); margin-top: 2px; flex-shrink: 0; }
.rec-right { display: flex; flex-direction: column; align-items: center; gap: 4px; flex-shrink: 0; width: 110px; }
.rec-score-num { font-size: 22px; font-weight: 800; color: var(--c-primary); }
.rec-score-num small { font-size: 12px; font-weight: 500; }
.ex-title { font-size: 16px; font-weight: 700; margin-bottom: 14px; }
.ex-reason { line-height: 1.8; color: #314166; background: #f3f7ff; padding: 12px 14px; border-radius: 10px; }
.ex-sources { display: flex; gap: 8px; }
.ex-bar { display: flex; flex-direction: column; gap: 10px; }
.eb-item { display: flex; align-items: center; gap: 10px; font-size: 13px; }
.eb-item > span { width: 64px; color: var(--c-text-sub); }
.eb-track { flex: 1; height: 9px; background: #e6ecf5; border-radius: 5px; overflow: hidden; }
.eb-track i { display: block; height: 100%; background: var(--c-cf); border-radius: 5px; }
.eb-track i.kg { background: var(--c-kg); }
.eb-track i.hot { background: var(--c-hot); }
.eb-track i.final-bar { background: linear-gradient(90deg, #2f6df6, #8b5cf6); }
.eb-item b { width: 38px; text-align: right; }
.eb-item.final { font-weight: 700; }
.ex-alert { margin-top: 18px; }
</style>
