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

    <section class="app-card" style="margin-top:20px;padding:20px">
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:12px">
        <h3 style="margin:0">同步记录</h3>
        <el-select v-model="logStatus" clearable placeholder="全部状态" style="width:150px" @change="loadLogs">
          <el-option :value="0" label="待同步" /><el-option :value="1" label="成功" /><el-option :value="2" label="失败" />
        </el-select>
        <el-button :loading="logsLoading" @click="loadLogs">刷新记录</el-button>
      </div>
      <p style="color:var(--text-secondary);font-size:13px">审核提交后同步图谱；失败不撤销审核，可排查后重试。显示最近 100 条。</p>
      <el-table :data="syncLogs" v-loading="logsLoading" max-height="320" empty-text="暂无同步记录">
        <el-table-column prop="id" label="任务" width="80" />
        <el-table-column prop="resourceId" label="资源 ID" width="100" />
        <el-table-column label="状态" width="100"><template #default="{row}">{{ ['待同步','成功','失败'][row.syncStatus] || '未知' }}</template></el-table-column>
        <el-table-column prop="errorMsg" label="说明" min-width="220" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="100"><template #default="{row}"><el-button v-if="row.syncStatus !== 1" link type="primary" :loading="retrying === row.id" @click="retryLog(row.id)">重试</el-button></template></el-table-column>
      </el-table>
    </section>

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
        <el-form-item :label="nodeForm.type === 'RESOURCE' ? '资源编号' : '节点 ID'" required>
          <el-input
            v-model="nodeForm.nid"
            :placeholder="nodeForm.type === 'RESOURCE' ? '如 RES20260201，需与 MySQL resource_no 一致' : '如 2001，建关系时要用它来引用'"
          />
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
import { addGraphNode, deleteGraphNode, addGraphRelation, syncGraph, listSyncLogs, retrySyncLog } from '@/api/admin'
import { listGraphNodes, listGraphEdges, getSubgraph } from '@/api/graph'
import { GraphNodeColor, GraphNodeTypeMap, GraphRelMap } from '@/utils/constants'

const nodeTypeMap = GraphNodeTypeMap
// 新建关系时只允许选 Neo4j 里真实存在的六种类型。
// 这里没用 GraphRelMap 直接当选项：那里面还包含 SAME_COURSE / RELATED 两个
// 只用于推荐理由文案的“虚拟类型”，选了会建出孤儿关系类型。
const relMap = {
  BELONGS_TO_KP: { label: '归属知识点' },
  TEACHES: { label: '讲授' },
  PREREQUISITE: { label: '先修' },
  RELATED_TO: { label: '相关' },
  BELONGS_TO: { label: '属于' },
  CONTAINS: { label: '包含' }
}
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
const syncLogs = ref([])
const logStatus = ref()
const logsLoading = ref(false)
const retrying = ref(null)
async function loadLogs() {
  logsLoading.value = true
  try { syncLogs.value = await listSyncLogs({ status: logStatus.value }) }
  finally { logsLoading.value = false }
}
async function retryLog(id) {
  retrying.value = id
  try { await retrySyncLog(id); await loadLogs() }
  finally { retrying.value = null }
}
onMounted(loadLogs)

const nodeDlg = ref(false)
const relDlg = ref(false)
const nodeForm = reactive({ type: 'KNOWLEDGE_POINT', nid: '', name: '', extra: '' })
const relForm = reactive({ fromId: '', fromType: 'KNOWLEDGE_POINT', relType: 'RELATED_TO', toId: '', toType: 'KNOWLEDGE_POINT' })

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

// 节点 id 只在自己那一类里唯一（学院 1 和专业 1 都是 1），
// ECharts 的 graph 要求全局唯一，所以画布上统一用「类型:id」当键，
// 原始 id 存在 _rid 里，调接口时仍用它。
function nodeKey(type, rid) { return type + ':' + String(rid) }

function addNodeObj(n) {
  const type = normType(n.type)
  const rid = String(n.id)
  const key = nodeKey(type, rid)
  if (nodes.find((x) => x.id === key)) return
  nodes.push({ id: key, _rid: rid, name: n.label, _type: type, symbolSize: symbolSizeOf({ type, heat: n.heat }), itemStyle: { color: colorOf(type) }, category: categoryIndex(type) })
}
function addEdgeObj(e) {
  const st = normType(e.sourceType)
  const tt = normType(e.targetType)
  const s = st ? nodeKey(st, e.source) : String(e.source)
  const t = tt ? nodeKey(tt, e.target) : String(e.target)
  const key = s + '->' + t + ':' + e.relType
  if (edges.find((x) => x._key === key)) return
  edges.push({ _key: key, source: s, target: t, relType: e.relType, lineStyle: { color: GraphRelMap[e.relType]?.color || '#c2ccdd', curveness: 0.1 } })
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
      ? getSubgraph({ kpId: params.data._rid, depth: 2 })
      : params.data._type === 'RESOURCE'
        ? getSubgraph({ resourceId: params.data._rid, depth: 2 })
        : Promise.resolve({ nodes: [], edges: [] })
    p.then((d) => { (d.nodes || []).forEach(addNodeObj); (d.edges || []).forEach(addEdgeObj); render() }).catch(() => {})
  }
}

function loadNodes() {
  searching.value = true
  const params = { keyword: kw.value || undefined, type: selType.value || undefined, limit: 500 }
  // /graph/nodes 与 /graph/edges 返回的是数组（不是 {nodes,edges}），这里两种形状都兼容。
  // 之前的写法把数组当对象取 d.nodes，永远得到 undefined，所以画布一直是空的。
  Promise.all([
    listGraphNodes(params),
    selType.value ? Promise.resolve([]) : listGraphEdges({})
  ])
    .then(([ns, es]) => {
      nodes.length = 0; edges.length = 0; selected.value = null
      const nodeList = Array.isArray(ns) ? ns : (ns && ns.nodes) || []
      const edgeList = Array.isArray(es) ? es : (es && es.edges) || []
      nodeList.forEach(addNodeObj)
      // 只保留两端都在画布上的边，避免 ECharts 出现悬空连线
      const ids = new Set(nodes.map((n) => n.id))
      const onCanvas = (e) => {
        const st = normType(e.sourceType)
        const tt = normType(e.targetType)
        const s = st ? nodeKey(st, e.source) : String(e.source)
        const t = tt ? nodeKey(tt, e.target) : String(e.target)
        return ids.has(s) && ids.has(t)
      }
      edgeList.filter(onCanvas).forEach(addEdgeObj)
      render()
    })
    .catch((e) => { console.error('[图谱运维] 加载失败', e) })
    .finally(() => { searching.value = false })
}

function addNode() {
  if (!nodeForm.name.trim()) { ElMessage.warning('请输入名称'); return }
  const nid = nodeForm.nid.trim()
  if (!nid) {
    ElMessage.warning(nodeForm.type === 'RESOURCE' ? '请填写资源编号（与 MySQL 的 resource_no 一致）' : '请填写节点 ID，后续建关系要用它引用')
    return
  }
  let extra = {}
  if (nodeForm.extra.trim()) {
    try { extra = JSON.parse(nodeForm.extra) } catch (e) { ElMessage.warning('附加属性需为合法 JSON'); return }
  }
  // 显式字段放在 extra 之后，避免附加属性把 id / 名称覆盖掉
  const props = { ...extra, name: nodeForm.name.trim(), label: nodeForm.name.trim() }
  if (nodeForm.type === 'RESOURCE') props.resource_id = nid
  else props.id = /^\d+$/.test(nid) ? Number(nid) : nid
  acting.value = true
  addGraphNode({ type: nodeForm.type, properties: props })
    .then(() => {
      ElMessage.success('节点创建成功')
      nodeDlg.value = false
      nodeForm.name = ''; nodeForm.nid = ''; nodeForm.extra = ''
      loadNodes()
    })
    .catch((e) => { console.error('[图谱运维] 创建节点失败', e) })
    .finally(() => { acting.value = false })
}

function delNode() {
  if (!selected.value) return
  acting.value = true
  deleteGraphNode(selected.value._type, selected.value._rid)
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
    .then((result) => { ElMessage.success(`已提交 ${result.queued} 条资源同步任务，请查看同步记录`); loadNodes(); loadLogs() })
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
