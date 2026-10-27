<template>
  <div class="page-container graph-page">
    <h2 class="page-title">知识图谱可视化</h2>
    <p class="page-subtitle">学院 → 专业 → 课程 → 知识点 → 资源，拖拽节点、滚轮缩放、点击查看详情与知识脉络</p>

    <div class="graph-wrap app-card">
      <!-- 左：检索与图例 -->
      <aside class="graph-side">
        <div class="side-block">
          <div class="side-label">快速探索</div>
          <div class="chip-row">
            <el-tag v-for="c in seeds" :key="c.key" class="seed-chip" @click="loadSeed(c)">{{ c.label }}</el-tag>
          </div>
        </div>

        <div class="side-block">
          <div class="side-label">节点检索</div>
          <el-input v-model="kw" placeholder="关键词搜索节点" :prefix-icon="Search" clearable @keyup.enter="searchNodes" @clear="searchNodes" />
          <el-select v-model="nodeType" placeholder="节点类型" clearable style="width:100%;margin-top:8px" @change="searchNodes">
            <el-option v-for="(v, k) in nodeTypeMap" :key="k" :label="v" :value="k" />
          </el-select>
          <el-button type="primary" class="w100 mt8" :icon="Search" :loading="searching" @click="searchNodes">搜索</el-button>
          <div class="node-result">
            <div v-for="n in searchRes" :key="n.id + n.type" class="node-res-item" @click="expandNode({ id: String(n.id), _type: normType(n.type), name: n.label })">
              <span class="nr-dot" :style="{background: colorOf(normType(n.type))}"></span>
              <span class="nr-label">{{ n.label }}</span>
              <span class="nr-type">{{ nodeTypeMap[normType(n.type)] }}</span>
            </div>
            <el-empty v-if="searchRes.length === 0" description="输入关键词检索" :image-size="50" />
          </div>
        </div>

        <div class="side-block">
          <div class="side-label">图例</div>
          <div class="legend">
            <span v-for="(c, k) in nodeColor" :key="k" class="legend-item">
              <i :style="{background:c}"></i>{{ nodeTypeMap[k] }}
            </span>
          </div>
          <div class="side-label mt12">关联深度</div>
          <el-slider v-model="depth" :min="1" :max="3" :marks="{1:'1跳',2:'2跳',3:'3跳'}" @change="reloadSeed" />
        </div>
      </aside>

      <!-- 中：图谱 -->
      <section class="graph-main">
        <div class="graph-toolbar">
          <span class="gt-info">节点 <b>{{ graphNodes.length }}</b> · 关系 <b>{{ graphEdges.length }}</b></span>
          <el-button text :icon="Refresh" @click="clearGraph">清空画布</el-button>
        </div>
        <div ref="chartRef" class="graph-canvas"></div>
      </section>

      <!-- 右：节点详情 -->
      <aside class="graph-detail" v-if="detail">
        <div class="side-label">节点详情</div>
        <div class="detail-type"><span class="nr-dot" :style="{background: colorOf(normType(detail.type))}"></span>{{ nodeTypeMap[normType(detail.type)] }}</div>
        <div class="detail-name">{{ detail.label }}</div>
        <el-descriptions :column="1" size="small" border class="mt10" v-if="detail.properties">
          <el-descriptions-item v-for="(val, key) in detail.properties" :key="key" :label="key">{{ val }}</el-descriptions-item>
        </el-descriptions>
        <div class="side-label mt14">相邻节点（{{ (detail.neighbors||[]).length }}）</div>
        <div class="nb-list">
          <div v-for="nb in detail.neighbors" :key="nb.id + nb.type" class="nb-item" @click="expandNode({ id: String(nb.id), _type: normType(nb.type), name: nb.label })">
            <span class="nr-dot" :style="{background: colorOf(normType(nb.type))}"></span>
            <span class="nr-label">{{ nb.label }}</span>
            <span class="nb-rel">{{ relLabel(nb.relType) }}</span>
          </div>
          <el-empty v-if="!(detail.neighbors||[]).length" description="无相邻节点" :image-size="50" />
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { listGraphNodes, getSubgraph, getGraphNode, listGraphEdges } from '@/api/graph'
import { GraphNodeColor, GraphNodeTypeMap, GraphRelMap } from '@/utils/constants'

const nodeTypeMap = GraphNodeTypeMap
const nodeColor = GraphNodeColor
const colorOf = (t) => nodeColor[t] || '#8a9bb8'
const relLabel = (r) => GraphRelMap[r]?.label || r || ''

const chartRef = ref(null)
let chart = null

const kw = ref('')
const nodeType = ref('')
const searching = ref(false)
const searchRes = ref([])
const depth = ref(2)

/** 后端节点类型为大驼峰（Resource/KnowledgePoint...），统一归一化为前端常量的全大写 key */
const TYPE_ALIAS = { College: 'COLLEGE', Major: 'MAJOR', Course: 'COURSE', KnowledgePoint: 'KNOWLEDGE_POINT', Resource: 'RESOURCE' }
function normType(t) {
  if (!t) return t
  return TYPE_ALIAS[t] || (nodeColor[t] ? t : t)
}

const seeds = [
  { key: 'res', label: '二叉树遍历动画演示', params: { resourceId: 'RES20260002' } },
  { key: 'kp', label: '树与二叉树(知识点)', params: { kpId: 1003 } }
]

const graphNodes = reactive([])
const graphEdges = reactive([])
const detail = ref(null)

function symbolSizeOf(n) {
  const base = { COLLEGE: 16, MAJOR: 15, COURSE: 17, KNOWLEDGE_POINT: 19, RESOURCE: 22 }
  const h = typeof n.heat === 'number' ? n.heat : 0
  return (base[n.type] || 16) + h * 10
}

function categoryIndexOf(t) { return Object.keys(nodeColor).indexOf(t) }

function addNode(n) {
  const type = normType(n.type)
  if (graphNodes.find((x) => x.id === String(n.id) && x._type === type)) return
  graphNodes.push({
    id: String(n.id),
    name: n.label,
    _type: type,
    heat: n.heat,
    category: categoryIndexOf(type),
    symbolSize: symbolSizeOf({ type, heat: n.heat }),
    itemStyle: { color: colorOf(type) },
    label: { show: true }
  })
}
function addEdge(e) {
  const key = e.source + '->' + e.target + ':' + e.relType
  if (graphEdges.find((x) => x._key === key)) return
  // 节点带 id 时，ECharts graph 的 links 必须用节点 id 匹配（实验验证）
  graphEdges.push({
    _key: key,
    source: String(e.source),
    target: String(e.target),
    relType: e.relType,
    lineStyle: { color: GraphRelMap[e.relType]?.color || '#c2ccdd', width: 1.4, curveness: 0.08, opacity: 0.75 },
    label: { show: false, formatter: relLabel(e.relType) }
  })
}

function renderGraph() {
  if (!chart) return
  chart.setOption({
    tooltip: {
      formatter: (p) => p.dataType === 'node'
        ? `<b>${p.data.name}</b><br/>类型：${nodeTypeMap[p.data._type] || ''}`
        : `${relLabel(p.data.relType)}`
    },
    legend: [{ data: Object.values(nodeTypeMap), top: 6, icon: 'circle', itemWidth: 10, itemHeight: 10 }],
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      zoom: 1,
      categories: Object.keys(nodeColor).map((k) => ({ name: nodeTypeMap[k], itemStyle: { color: nodeColor[k] } })),
      force: { repulsion: 220, edgeLength: [60, 140], gravity: 0.06, friction: 0.18 },
      label: { show: true, position: 'right', fontSize: 11, color: '#2b3445' },
      emphasis: { focus: 'adjacency', label: { show: true }, lineStyle: { width: 3 } },
      lineStyle: { color: 'source', curveness: 0.08, opacity: 0.7 },
      data: graphNodes,
      links: graphEdges
    }]
  })
}

function onChartClick(params) {
  if (params.dataType === 'node') {
    showDetail(params.data)
    expandNode(params.data)
  }
}

async function showDetail(node) {
  detail.value = null
  getGraphNode(node._type, node.id)
    .then((d) => { detail.value = d })
    .catch(() => {})
}

function expandNode(node) {
  // 知识点 / 资源 → 多跳子图；其他类型 → 取直接关系
  let p
  if (node._type === 'KNOWLEDGE_POINT') p = getSubgraph({ kpId: node.id, depth: depth.value })
  else if (node._type === 'RESOURCE') p = getSubgraph({ resourceId: node.id, depth: depth.value })
  else p = listGraphEdges({ fromId: node.id, fromType: node._type }).then((e) => ({
    nodes: (e || []).map((x) => ({ id: x.to, type: neighborType(node._type, x.relType), label: String(x.to) })),
    edges: (e || []).map((x) => ({ source: x.from, target: x.to, relType: x.type }))
  }))
  p.then((data) => {
    ;(data.nodes || []).forEach(addNode)
    ;(data.edges || []).forEach(addEdge)
    renderGraph()
    if ((data.nodes || []).length === 0) ElMessage.info('该节点暂无更深层关联')
  }).catch(() => {})
}

function neighborType(fromType, relType) {
  // 粗略推断邻居类型（用于非子图的关系展开）
  if (fromType === 'COURSE') return 'KNOWLEDGE_POINT'
  if (fromType === 'COLLEGE') return 'MAJOR'
  if (fromType === 'MAJOR') return 'COURSE'
  return 'RESOURCE'
}

function searchNodes() {
  searching.value = true
  listGraphNodes({ keyword: kw.value || undefined, type: nodeType.value || undefined, limit: 30 })
    .then((d) => { searchRes.value = d.nodes || [] })
    .catch(() => {})
    .finally(() => { searching.value = false })
}

function loadSeed(c) {
  graphNodes.length = 0
  graphEdges.length = 0
  detail.value = null
  getSubgraph({ ...c.params, depth: depth.value })
    .then((d) => {
      ;(d.nodes || []).forEach(addNode)
      ;(d.edges || []).forEach(addEdge)
      renderGraph()
    })
    .catch(() => {})
}
function reloadSeed() {
  if (seeds.length) loadSeed(seeds[0])
}

function clearGraph() {
  graphNodes.length = 0
  graphEdges.length = 0
  detail.value = null
  renderGraph()
}

function onResize() { chart && chart.resize() }
window.addEventListener('resize', onResize)
onBeforeUnmount(() => { window.removeEventListener('resize', onResize); chart && chart.dispose() })

onMounted(() => {
  chart = echarts.init(chartRef.value)
  chart.on('click', onChartClick)
  loadSeed(seeds[0])
})
</script>

<style scoped>
.graph-page { max-width: 100%; }
.graph-wrap { display: flex; height: 76vh; min-height: 520px; padding: 0; overflow: hidden; }
.graph-side { width: 250px; flex-shrink: 0; border-right: 1px solid var(--c-border); padding: 16px; overflow-y: auto; }
.graph-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.graph-detail { width: 270px; flex-shrink: 0; border-left: 1px solid var(--c-border); padding: 16px; overflow-y: auto; }
.side-block { margin-bottom: 18px; }
.side-label { font-weight: 700; font-size: 13px; margin-bottom: 8px; color: var(--c-text); }
.mt8 { margin-top: 8px; }
.mt10 { margin-top: 10px; }
.mt12 { margin-top: 12px; }
.mt14 { margin-top: 14px; }
.w100 { width: 100%; }
.chip-row { display: flex; flex-wrap: wrap; gap: 8px; }
.seed-chip { cursor: pointer; }
.node-result { margin-top: 12px; }
.node-res-item { display: flex; align-items: center; gap: 8px; padding: 7px 6px; border-radius: 8px; cursor: pointer; font-size: 13px; }
.node-res-item:hover { background: var(--c-primary-lighter); }
.nr-dot { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }
.nr-label { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.nr-type { font-size: 11px; color: var(--c-text-sub); }
.legend { display: flex; flex-wrap: wrap; gap: 8px 14px; font-size: 12px; color: var(--c-text-sub); }
.legend-item { display: flex; align-items: center; gap: 5px; }
.legend-item i { width: 10px; height: 10px; border-radius: 50%; }
.graph-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; border-bottom: 1px solid var(--c-border); }
.gt-info { font-size: 13px; color: var(--c-text-sub); }
.gt-info b { color: var(--c-primary); }
.graph-canvas { flex: 1; min-height: 0; }

@media (max-width: 900px) {
  .graph-wrap { flex-direction: column; height: auto; min-height: 0; }
  .graph-side { width: 100%; border-right: none; border-bottom: 1px solid var(--c-border); }
  .graph-main { width: 100%; }
  .graph-canvas { height: 58vh; min-height: 360px; flex: none; }
  .graph-detail { width: 100%; border-left: none; border-top: 1px solid var(--c-border); }
}
.detail-type { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--c-text-sub); }
.detail-name { font-size: 17px; font-weight: 700; margin: 8px 0; }
.nb-list { display: flex; flex-direction: column; }
.nb-item { display: flex; align-items: center; gap: 8px; padding: 7px 6px; border-radius: 8px; cursor: pointer; font-size: 13px; }
.nb-item:hover { background: var(--c-primary-lighter); }
.nb-rel { font-size: 11px; color: var(--c-info); margin-left: auto; }
</style>
