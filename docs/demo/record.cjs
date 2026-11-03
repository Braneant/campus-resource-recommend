/* Frontend-only demonstration. All API calls are intercepted; no real backend is contacted. */
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const path = require('path')
const fs = require('fs')
const out = process.argv[2] || path.resolve('tmp/engineering/demo-recording')
fs.mkdirSync(out, { recursive: true })
const resources = [
  ['数据结构与算法基础', '模拟课程组', 1], ['数据库系统原理', '模拟课程组', 2],
  ['Java 面向对象程序设计', '模拟课程组', 1], ['计算机网络学习指南', '模拟课程组', 3],
  ['操作系统重点解析', '模拟课程组', 1], ['机器学习入门', '模拟课程组', 2]
].map(([title, author, type], i) => ({ id: i + 1, resourceId: i + 1, resourceNo: `DEMO-${i + 1}`,
  title, author, type, status: 1, avgRating: 4.5, downloadCount: 10 + i, heat: 0.7,
  recommendScore: 0.7, source: ['kg', 'cf', 'hot'][i % 3], reason: '模拟数据：兴趣知识点关联' }))
const nodes = [
  { id: '1001', label: '数据结构', type: 'KnowledgePoint' },
  { id: '1002', label: '算法设计', type: 'KnowledgePoint' },
  { id: 'DEMO-1', label: '数据结构与算法基础', type: 'Resource' }
]
const edges = [
  { source: '1001', target: '1002', sourceType: 'KnowledgePoint', targetType: 'KnowledgePoint', relType: 'RELATED_TO' },
  { source: 'DEMO-1', target: '1001', sourceType: 'Resource', targetType: 'KnowledgePoint', relType: 'BELONGS_TO_KP' }
]

async function main() {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, recordVideo: { dir: out, size: { width: 1440, height: 900 } } })
  await context.addInitScript(() => {
    const admin = location.pathname.startsWith('/admin')
    localStorage.setItem('token', 'isolated-demo-token')
    localStorage.setItem('role', admin ? 'ADMIN' : 'STUDENT')
    localStorage.setItem('userInfo', JSON.stringify({ id: 1, username: 'demo', nickname: '演示用户' }))
    addEventListener('DOMContentLoaded', () => {
      const banner = document.createElement('div')
      banner.textContent = '隔离前端演示 · 接口和账号均为模拟数据 · 未连接后端或数据库'
      banner.style.cssText = 'position:fixed;bottom:0;left:0;right:0;background:#09265c;color:white;text-align:center;padding:8px;z-index:99999;font:16px sans-serif'
      document.body.appendChild(banner)
    })
  })
  await context.route(url => url.pathname.startsWith('/api/'), async route => {
    const req = route.request(); const u = new URL(req.url()); let data = []
    if (req.method() !== 'GET') return route.fulfill({ status: 403, contentType: 'application/json', body: JSON.stringify({ code: 403, message: '演示只读', data: null }) })
    if (u.pathname.includes('/resource/search')) data = { list: resources, total: resources.length, pageNum: 1, pageSize: 12 }
    else if (u.pathname.includes('/recommend/personal')) data = resources
    else if (u.pathname.includes('/graph/nodes')) data = nodes
    else if (u.pathname.includes('/graph/edges')) data = edges
    else if (u.pathname.includes('/graph/subgraph')) data = { nodes, edges }
    else if (u.pathname.includes('/resource/audit')) data = { list: [{ ...resources[0], status: 0, submitterName: '模拟用户', submitTime: '2026-01-01 10:00' }], total: 1 }
    else if (u.pathname.includes('/admin/resource')) data = { list: resources, total: resources.length }
    else if (u.pathname.includes('/dashboard/stats')) data = { userCount: 30, resourceCount: 200, pendingCount: 1 }
    else if (u.pathname.includes('/recommend/explain')) data = { title: resources[0].title, reason: '模拟演示：知识点关联', sources: ['kg'], scoreBreakdown: { cf: 0.4, kg: 0.8, hot: 0.2, final: 0.52 } }
    await route.fulfill({ contentType: 'application/json; charset=utf-8', body: JSON.stringify({ code: 0, message: '模拟接口', data }) })
  })
  const page = await context.newPage()
  const pause = () => page.waitForTimeout(18000)
  await page.goto('http://127.0.0.1:3398/student/login'); await pause()
  await page.goto('http://127.0.0.1:3398/student/home'); await pause()
  const input = page.getByPlaceholder('搜索资源标题 / 关键词')
  if (await input.count()) { await input.fill('数据结构'); await page.waitForTimeout(1000); await input.clear() }
  await page.goto('http://127.0.0.1:3398/student/recommend'); await pause()
  await page.goto('http://127.0.0.1:3398/student/graph'); await pause()
  await page.goto('http://127.0.0.1:3398/admin/audit'); await pause()
  await page.goto('http://127.0.0.1:3398/admin/graph'); await pause()
  const video = page.video(); await page.close(); await context.close(); await browser.close()
  console.log(await video.path())
}
main().catch(err => { console.error(err); process.exitCode = 1 })
