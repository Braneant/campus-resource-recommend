<template>
  <div
    ref="wrap"
    class="kh"
    :class="{ 'is-ready': ready }"
    role="link"
    tabindex="0"
    :aria-label="ready ? `知识图谱，${nodeCount} 个节点，${edgeCount} 条关系，点击查看` : '知识图谱'"
    title="点击查看知识图谱"
    @pointermove="onMove"
    @pointerleave="onLeave"
    @click="openGraph"
    @keydown.enter="openGraph"
    @keydown.space.prevent="openGraph"
  >
    <canvas ref="canvas" aria-hidden="true" />
    <div v-if="ready" class="kh-meta">
      <span class="kh-dot" />
      <span>{{ hoverLabel || `${nodeCount} 节点 · ${edgeCount} 关系` }}</span>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { GraphNodeColor, GraphRelMap } from '@/utils/constants'

const router = useRouter()
const wrap = ref(null)
const canvas = ref(null)
const ready = ref(false)
const nodeCount = ref(0)
const edgeCount = ref(0)
const hoverLabel = ref('')

let ctx = null
let nodes = []
let edges = []
let width = 0
let height = 0
let dpr = 1
let raf = 0
let running = false
let hoverId = null
let resizeObserver = null
let reduced = false
let startAt = 0

const ring = {
  COLLEGE: 0.10,
  MAJOR: 0.28,
  COURSE: 0.48,
  KNOWLEDGE_POINT: 0.70,
  RESOURCE: 0.92
}

const radius = {
  COLLEGE: 7,
  MAJOR: 6,
  COURSE: 5,
  KNOWLEDGE_POINT: 4,
  RESOURCE: 2.8
}

const CACHE_KEY = 'hero-graph-v1'
const CACHE_TTL = 30 * 60 * 1000

function normType(t) {
  const s = String(t || '').toUpperCase().replace(/[^A-Z]/g, '')
  if (s === 'KNOWLEDGEPOINT') return 'KNOWLEDGE_POINT'
  if (s === 'RESOURCE' || s === 'COURSE' || s === 'MAJOR' || s === 'COLLEGE') return s
  return s || 'RESOURCE'
}

function readCache() {
  try {
    const raw = sessionStorage.getItem(CACHE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed || !parsed.t || Date.now() - parsed.t > CACHE_TTL) return null
    return parsed.data
  } catch (e) {
    return null
  }
}

function writeCache(data) {
  try {
    sessionStorage.setItem(CACHE_KEY, JSON.stringify({ t: Date.now(), data }))
  } catch (e) {
    // 隐私模式下 sessionStorage 可能不可用，忽略即可
  }
}

/**
 * 首页 Hero 是装饰性内容，失败时不该弹全局错误提示。
 * 所以这里直接用 fetch，不经过 axios 拦截器。
 */
async function fetchSubgraph() {
  try {
    const token = localStorage.getItem('token')
    if (!token) return null
    const res = await fetch('/api/graph/subgraph?kpId=1001&depth=2', {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (!res.ok) return null
    const json = await res.json()
    return json && json.code === 0 ? json.data : null
  } catch (e) {
    return null
  }
}

function setup(data) {
  const list = (data && data.nodes) || []
  const links = (data && data.edges) || []
  if (list.length < 4) return

  const map = new Map()
  nodes = list.map((n, i) => {
    const type = normType(n.type)
    const node = {
      id: String(n.id),
      type,
      label: n.label || '',
      color: GraphNodeColor[type] || '#8a9bb8',
      r: radius[type] || 3,
      x: 0,
      y: 0,
      bx: 0,
      by: 0,
      phase: i * 1.7 + Math.random() * 0.8
    }
    map.set(node.id, node)
    return node
  })

  edges = links
    .map((e) => ({
      source: String(e.source),
      target: String(e.target),
      color: (GraphRelMap[e.relType] && GraphRelMap[e.relType].color) || '#8a9bb8'
    }))
    .filter((e) => map.has(e.source) && map.has(e.target) && e.source !== e.target)

  nodeCount.value = nodes.length
  edgeCount.value = edges.length
  ready.value = true

  nextTick(() => {
    resize()
    layout()
    startAt = performance.now()
    draw(startAt)
    if (!reduced) start()
  })
}

function resize() {
  const el = wrap.value
  const c = canvas.value
  if (!el || !c) return
  const rect = el.getBoundingClientRect()
  width = Math.max(1, Math.floor(rect.width))
  height = Math.max(1, Math.floor(rect.height))
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  c.width = Math.floor(width * dpr)
  c.height = Math.floor(height * dpr)
  c.style.width = width + 'px'
  c.style.height = height + 'px'
  ctx = c.getContext('2d')
  if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
}

function layout() {
  if (!nodes.length || width < 20 || height < 20) return
  const cx = width / 2
  const cy = height / 2 + 2
  const R = Math.min(width, height) * 0.46

  const groups = {}
  for (const n of nodes) {
    if (!groups[n.type]) groups[n.type] = []
    groups[n.type].push(n)
  }

  for (const type of Object.keys(groups)) {
    const list = groups[type]
    const base = ring[type] || 0.7
    list.forEach((n, i) => {
      const a = (i / Math.max(1, list.length)) * Math.PI * 2 + (type === 'RESOURCE' ? 0.35 : 0)
      const rad = R * base
      n.x = cx + Math.cos(a) * rad + (Math.random() - 0.5) * 8
      n.y = cy + Math.sin(a) * rad + (Math.random() - 0.5) * 8
    })
  }

  for (let iter = 0; iter < 150; iter++) {
    for (let i = 0; i < nodes.length; i++) {
      const a = nodes[i]
      for (let j = i + 1; j < nodes.length; j++) {
        const b = nodes[j]
        let dx = b.x - a.x
        let dy = b.y - a.y
        let d2 = dx * dx + dy * dy
        if (d2 < 1) {
          dx = (Math.random() - 0.5) * 2
          dy = (Math.random() - 0.5) * 2
          d2 = dx * dx + dy * dy
        }
        if (d2 < 2500) {
          const d = Math.sqrt(d2)
          const f = 42 / d2
          const fx = (dx / d) * f
          const fy = (dy / d) * f
          a.x -= fx
          a.y -= fy
          b.x += fx
          b.y += fy
        }
      }
    }

    for (const e of edges) {
      const a = nodes.find((n) => n.id === e.source)
      const b = nodes.find((n) => n.id === e.target)
      if (!a || !b) continue
      const dx = b.x - a.x
      const dy = b.y - a.y
      const d = Math.max(1, Math.sqrt(dx * dx + dy * dy))
      const f = (d - 34) * 0.012
      const fx = (dx / d) * f
      const fy = (dy / d) * f
      a.x += fx
      a.y += fy
      b.x -= fx
      b.y -= fy
    }

    for (const n of nodes) {
      n.x += (cx - n.x) * 0.006
      n.y += (cy - n.y) * 0.006
      const target = R * (ring[n.type] || 0.7)
      const dx = n.x - cx
      const dy = n.y - cy
      const d = Math.max(1, Math.sqrt(dx * dx + dy * dy))
      const f = (target - d) * 0.012
      n.x += (dx / d) * f
      n.y += (dy / d) * f
      n.x = Math.min(width - 10, Math.max(10, n.x))
      n.y = Math.min(height - 14, Math.max(14, n.y))
    }
  }

  for (const n of nodes) {
    n.bx = n.x
    n.by = n.y
  }
}

function position(n, t) {
  const drift = reduced ? 0 : 1.1
  return {
    x: n.bx + Math.sin(t * 0.7 + n.phase) * drift,
    y: n.by + Math.cos(t * 0.55 + n.phase * 1.3) * drift
  }
}

function draw(now) {
  if (!ctx) return
  const t = reduced ? 0 : (now - startAt) / 1000
  ctx.clearRect(0, 0, width, height)

  const pos = new Map()
  for (const n of nodes) pos.set(n.id, position(n, t))

  const hovered = hoverId ? nodes.find((n) => n.id === hoverId) : null
  const neighbors = new Set()
  if (hovered) {
    neighbors.add(hovered.id)
    for (const e of edges) {
      if (e.source === hovered.id) neighbors.add(e.target)
      if (e.target === hovered.id) neighbors.add(e.source)
    }
  }

  for (const e of edges) {
    const a = pos.get(e.source)
    const b = pos.get(e.target)
    if (!a || !b) continue
    const active = hovered && (e.source === hovered.id || e.target === hovered.id)
    ctx.beginPath()
    ctx.moveTo(a.x, a.y)
    ctx.lineTo(b.x, b.y)
    ctx.strokeStyle = e.color
    ctx.globalAlpha = active ? 0.75 : hovered ? 0.08 : 0.20
    ctx.lineWidth = active ? 1.5 : 0.8
    ctx.stroke()
  }

  for (const n of nodes) {
    const p = pos.get(n.id)
    if (!p) continue
    const isHover = hoverId === n.id
    const isNear = neighbors.has(n.id)
    const dim = hovered && !isNear

    ctx.globalAlpha = dim ? 0.18 : 0.14
    ctx.beginPath()
    ctx.arc(p.x, p.y, n.r * 3.2, 0, Math.PI * 2)
    ctx.fillStyle = n.color
    ctx.fill()

    ctx.globalAlpha = dim ? 0.35 : 1
    ctx.beginPath()
    ctx.arc(p.x, p.y, n.r, 0, Math.PI * 2)
    ctx.fillStyle = n.color
    ctx.fill()

    if (isHover) {
      ctx.globalAlpha = 1
      ctx.beginPath()
      ctx.arc(p.x, p.y, n.r + 3.5, 0, Math.PI * 2)
      ctx.strokeStyle = '#ffffff'
      ctx.lineWidth = 1.2
      ctx.stroke()
    }
  }

  if (hovered) {
    const p = pos.get(hovered.id)
    if (p) {
      ctx.globalAlpha = 1
      ctx.font = '11px system-ui, -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'bottom'
      ctx.fillStyle = '#3c4a60'
      ctx.fillText(hovered.label, Math.min(width - 40, Math.max(40, p.x)), Math.max(12, p.y - hovered.r - 6))
    }
  }

  ctx.globalAlpha = 1
}

function tick(now) {
  if (!running) return
  raf = requestAnimationFrame(tick)
  draw(now)
}

function start() {
  if (running || reduced) return
  running = true
  startAt = performance.now()
  raf = requestAnimationFrame(tick)
}

function stop() {
  running = false
  if (raf) cancelAnimationFrame(raf)
  raf = 0
}

function onMove(e) {
  if (!ready.value) return
  const rect = wrap.value.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  let best = null
  let bestD = 16
  for (const n of nodes) {
    const p = position(n, reduced ? 0 : (performance.now() - startAt) / 1000)
    const d = Math.hypot(p.x - x, p.y - y)
    if (d < bestD) {
      bestD = d
      best = n
    }
  }
  hoverId = best ? best.id : null
  hoverLabel.value = best ? best.label : ''
  if (wrap.value) wrap.value.style.cursor = best ? 'pointer' : 'default'
  if (reduced) draw(performance.now())
}

function onLeave() {
  hoverId = null
  hoverLabel.value = ''
  if (wrap.value) wrap.value.style.cursor = 'default'
  if (reduced) draw(performance.now())
}

function openGraph() {
  router.push('/student/graph')
}

function onVisibility() {
  if (document.hidden) stop()
  else start()
}

onMounted(() => {
  reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches

  const cached = readCache()
  if (cached) setup(cached)
  else {
    fetchSubgraph()
      .then((data) => {
        if (!data || !data.nodes || data.nodes.length < 4) return
        writeCache(data)
        setup(data)
      })
      .catch(() => {})
  }

  if (wrap.value && 'ResizeObserver' in window) {
    resizeObserver = new ResizeObserver(() => {
      if (!ready.value) return
      resize()
      layout()
      draw(performance.now())
    })
    resizeObserver.observe(wrap.value)
  }
  document.addEventListener('visibilitychange', onVisibility)
})

onBeforeUnmount(() => {
  stop()
  if (resizeObserver) resizeObserver.disconnect()
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

<style scoped>
.kh {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  background:
    radial-gradient(circle at 50% 45%, rgba(139, 92, 246, 0.10), rgba(139, 92, 246, 0) 68%),
    radial-gradient(circle at 30% 70%, rgba(47, 109, 246, 0.10), rgba(47, 109, 246, 0) 62%);
  cursor: default;
}
.kh canvas {
  display: block;
  width: 100%;
  height: 100%;
}
.kh:focus-visible {
  outline: 2px solid var(--c-primary);
  outline-offset: 3px;
}
.kh-meta {
  position: absolute;
  left: 10px;
  bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 0.5px solid rgba(31, 42, 68, 0.08);
  color: #5f6b7e;
  font-size: 10px;
  line-height: 1.4;
  pointer-events: none;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}
.kh-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #8b5cf6;
  flex-shrink: 0;
}
.kh-meta span:last-child {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
