<template>
  <div class="page-container">
    <h2 class="page-title">知识图谱运维</h2>
    <p class="page-subtitle">可视化维护图谱节点与关系，支持一键同步 MySQL → Neo4j</p>

    <div class="gm-wrap app-card">
      <aside class="gm-side">
        <div class="gm-toolbar">
          <el-button type="primary" :icon="Plus" @click="nodeDlg = true">新增节点</el-button>
          <el-button :icon="Link" @click="relDlg = true">新增关系</el-button>
        </div>
        <div class="gm-sync">
          <span class="gm-sync-label">数据同步</span>
          <el-select v-model="syncScope" style="width:100%">
            <el-option value="all" label="全量同步" />
            <el-option value="resource" label="仅资源" />
            <el-option value="college" label="仅学院体系" />
          </el-select>
          <el-button type="success" :icon="Refresh" :loading="syncing" @click="doSync" class="w100">执行同步</el-button>
        </div>
        <el-divider />
        <div class="side-label">节点检索</div>
        <el-input v-model="kw" placeholder="关键词" :prefix-icon="Search" clearable @keyup.enter="loadNodes" @clear="loadNodes" />
        <el-select v-model="selType" placeholder="类型" clearable style="width:100%;margin-top:8px" @change="loadNodes">
          <el-option v-for="(v,k) in nodeTypeMap" :key="k" :label="v" :value="k" />
        </el-select>
        <el-button class="w100 mt8" :icon="Search" :loading="searching" @click="loadNodes">查询</el-button>

        <div v-if="selected" class="sel-node">
          <div class="side-label">已选节点</div>
          <div class="sel-info"><span class="nr-dot" :style="{background: colorOf(selected._type)}"></span>{{ selected.name }}</div>
          <div class="sel-type">{{ nodeTypeMap[selected._type] }} · {{ selected.id }}</div>
          <el-button type="danger" plain class="w100 mt8" :icon="Delete" :loading="acting" @click="delNode">删除该节点</el-button>
        </div>
      </aside>

      <section class="gm-main">
        <div ref="chartRef" class="gm-canvas"></div>
        <div class="gm-tip">提示：点击节点可选中；拖拽可调整布局，滚轮缩放</div>
      </section>
    </div>

    <!-- 新增节点 -->
    <el-dialog v-model="nodeDlg" title="新增图谱节点" width="440px">
      <el-form :model="nodeForm" label-width="90px">
        <el-form-item label="节点类型" required>
          <el-select v-model="nodeForm.type" style="width:100%">
            <el-option v-for="(v,k) in nodeTypeMap" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="nodeForm.name" placeholder="节点显示名称" />
        </el-form-item>
        <el-form-item label="附加属性">
          <el-input v-model="nodeForm.extra" type="textarea" :rows="2" placeholder="可选，JSON，如 {&quot;desc&quot;:&quot;...&quot;}" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeDlg = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="addNode">创建</el-button>
      </template>
    </el-dialog>

    <!-- 新增关系 -->
    <el-dialog v-model="relDlg" title="新增图谱关系" width="460px">
      <el-form :model="relForm" label-width="90px">
        <el-form-item label="起始节点">
          <el-input v-model="relForm.fromId" placeholder="节点 ID" style="width:60%;margin-right:6%" />
          <el-select v-model="relForm.fromType" placeholder="类型" style="width:34%">
            <el-option v-for="(v,k) in nodeTypeMap" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系类型">
          <el-select v-model="relForm.relType" style="width:100%">
            <el-option v-for="(v,k) in relMap" :key="k" :label="v.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标节点">
          <el-input v-model="relForm.toId" placeholder="节点 ID" style="width:60%;margin-right:6%" />
          <el-select v-model="relForm.toType" placeholder="类型" style="width:34%">
            <el-option v-for="(v,k) in nodeTypeMap" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="relDlg = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="addRel">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { Plus, Link, Refresh, Search, Delete } from '@element-plus/icons-vue'
import { addGraphNode, deleteGraphNode, addGraphRelation, syncGraph } from '@/api/admin'
import { listGraphNodes, getSubgraph } from '@/api/graph'
import { GraphNodeColor, GraphNodeTypeMap, GraphRelMap } from '@/utils/constants'

const nodeTypeMap = GraphNodeTypeMap
const relMap = GraphRelMap
const colorOf = (t) => GraphNodeColor[t] || '#8a9bb8'

const chartRef = ref(null)
let chart = null
const nodes = reactive([])
const edges = reactive([])
const selected = ref(null)

const kw = ref('')
const selType = ref('')
const searching = ref(false)
const acting = ref(false)
const syncing = ref(false)
const syncScope = ref('all')

const nodeDlg = ref(false)
const relDlg = ref(false)
const nodeForm = reactive({ type: 'KNOWLEDGE_POINT', name: '', extra: '' })
const relForm = reactive({ fromId: '', fromType: 'KNOWLEDGE_POINT', relType: 'RELATED', toId: '', toType: 'KNOWLEDGE_POINT' })

function symbolSizeOf(n) {
  const base = { COLLEGE: 16, MAJOR: 15, COURSE: 17, KNOWLEDGE_POINT: 19, RESOURCE: 22 }
  const h = typeof n.heat === 'number' ? n.heat : 0
  return (base[n.type] || 16) + h * 8
}
function categoryIndex(t) { return Object.keys(GraphNodeColor).indexOf(t) }

/** 后端节点类型为大驼峰（Resource/KnowledgePoint...），归一化为前端常量的全大写 key */
const TYPE_ALIAS = { College: 'COLLEGE', Major: 'MAJOR', Course: 'COURSE', KnowledgePoint: 'KNOWLEDGE_POINT', Resource: 'RESOURCE' }
function normType(t) {
  if (!t) return t
  return TYPE_ALIAS[t] || t
}

function addNodeObj(n) {
  const type = normType(n.type)
  if (nodes.find((x) => x.id === String(n.id) && x._type === type)) return
  nodes.push({ id: String(n.id), name: n.label, _type: type, symbolSize: symbolSizeOf({ type, heat: n.heat }), itemStyle: { color: colorOf(type) }, category: categoryIndex(type) })
}
function addEdgeObj(e) {
  const key = e.source + '->' + e.target + ':' + e.relType
  if (edges.find((x) => x._key === key)) return
  // 节点带 id 时，ECharts graph 的 links 必须用节点 id 匹配（实验验证）
  edges.push({ _key: key, source: String(e.source), target: String(e.target), lineStyle: { color: GraphRelMap[e.relType]?.color || '#c2ccdd', curveness: 0.1 } })
}

function render() {
  if (!chart) return
  chart.setOption({
    tooltip: { formatter: (p) => p.dataType === 'node' ? `<b>${p.data.name}</b><br/>${nodeTypeMap[p.data._type]}` : '' },
    legend: [{ data: Object.values(nodeTypeMap), top: 6, icon: 'circle', itemWidth: 10, itemHeight: 10 }],
    series: [{
      type: 'graph', layout: 'force', roam: true, draggable: true,
      categories: Object.keys(GraphNodeColor).map((k) => ({ name: nodeTypeMap[k], itemStyle: { color: GraphNodeColor[k] } })),
      force: { repulsion: 200, edgeLength: [60, 130], gravity: 0.06 },
      label: { show: true, position: 'right', fontSize: 11, color: '#2b3445' },
      emphasis: { focus: 'adjacency' },
      lineStyle: { color: 'source', curveness: 0.1, opacity: 0.7 },
      data: nodes, links: edges
    }]
  })
}

function onChartClick(params) {
  if (params.dataType === 'node') {
    selected.value = params.data
    // 展开其多跳子图
    const p = params.data._type === 'KNOWLEDGE_POINT'
      ? getSubgraph({ kpId: params.data.id, depth: 2 })
      : params.data._type === 'RESOURCE'
        ? getSubgraph({ resourceId: params.data.id, depth: 2 })
        : Promise.resolve({ nodes: [], edges: [] })
    p.then((d) => { (d.nodes || []).forEach(addNodeObj); (d.edges || []).forEach(addEdgeObj); render() }).catch(() => {})
  }
}

function loadNodes() {
  searching.value = true
  listGraphNodes({ keyword: kw.value || undefined, type: selType.value || undefined, limit: 200 })
    .then((d) => {
      nodes.length = 0; edges.length = 0; selected.value = null
      ;(d.nodes || []).forEach(addNodeObj)
      render()
    })
    .catch(() => {})
    .finally(() => { searching.value = false })
}

function addNode() {
  if (!nodeForm.name.trim()) { ElMessage.warning('请输入名称'); return }
  let extra = {}
  if (nodeForm.extra.trim()) {
    try { extra = JSON.parse(nodeForm.extra) } catch (e) { ElMessage.warning('附加属性需为合法 JSON'); return }
  }
  acting.value = true
  addGraphNode({ type: nodeForm.type, properties: { name: nodeForm.name.trim(), label: nodeForm.name.trim(), ...extra } })
    .then(() => { ElMessage.success('节点创建成功'); nodeDlg.value = false; nodeForm.name = ''; nodeForm.extra = ''; loadNodes() })
    .catch(() => {})
    .finally(() => { acting.value = false })
}

function delNode() {
  if (!selected.value) return
  acting.value = true
  deleteGraphNode(selected.value._type, selected.value.id)
    .then(() => { ElMessage.success('节点已删除'); selected.value = null; loadNodes() })
    .catch(() => {})
    .finally(() => { acting.value = false })
}

function addRel() {
  if (!relForm.fromId || !relForm.toId || !relForm.relType) { ElMessage.warning('请填写完整关系信息'); return }
  acting.value = true
  addGraphRelation({ ...relForm })
    .then(() => { ElMessage.success('关系创建成功'); relDlg.value = false; loadNodes() })
    .catch(() => {})
    .finally(() => { acting.value = false })
}

function doSync() {
  syncing.value = true
  syncGraph({ scope: syncScope.value })
    .then(() => { ElMessage.success(`同步完成（${syncScope.value}）`); loadNodes() })
    .catch(() => {})
    .finally(() => { syncing.value = false })
}

function onResize() { chart && chart.resize() }
window.addEventListener('resize', onResize)
onBeforeUnmount(() => { window.removeEventListener('resize', onResize); chart && chart.dispose() })

onMounted(() => {
  chart = echarts.init(chartRef.value)
  chart.on('click', onChartClick)
  loadNodes()
})
</script>

<style scoped>
.gm-wrap { display: flex; height: 72vh; min-height: 480px; overflow: hidden; padding: 0; }
.gm-side { width: 260px; flex-shrink: 0; border-right: 1px solid var(--c-border); padding: 16px; overflow-y: auto; }
.gm-toolbar { display: flex; gap: 10px; }
.gm-sync { margin-top: 16px; }
.gm-sync-label { font-size: 13px; font-weight: 700; color: var(--c-text); display: block; margin-bottom: 8px; }
.w100 { width: 100%; }
.mt8 { margin-top: 8px; }
.side-label { font-weight: 700; font-size: 13px; margin: 14px 0 8px; color: var(--c-text); }
.sel-node { margin-top: 8px; padding: 12px; background: var(--c-primary-lighter); border-radius: 10px; }
.sel-info { font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.nr-dot { width: 9px; height: 9px; border-radius: 50%; }
.sel-type { font-size: 12px; color: var(--c-text-sub); margin: 6px 0; }
.gm-main { flex: 1; position: relative; min-width: 0; }
.gm-canvas { position: absolute; inset: 0; }
.gm-tip { position: absolute; left: 16px; bottom: 12px; font-size: 12px; color: var(--c-text-sub); background: rgba(255,255,255,.8); padding: 4px 10px; border-radius: 8px; }
</style>
