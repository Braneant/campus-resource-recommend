import request from '@/utils/request'

// ============ 管理端模块 /api/admin —— 对齐 03-接口契约 §十 ============

// ---- 学科数据 CRUD（学院/专业/课程/资源分类，各 GET/POST/PUT/DELETE）----
const ENTITY_PREFIX = {
  college: '/admin/college',
  major: '/admin/major',
  course: '/admin/course',
  category: '/admin/category'
}

function buildCrud(prefix) {
  return {
    list: (params) => request({ url: prefix, method: 'get', params }),
    create: (data) => request({ url: prefix, method: 'post', data }),
    update: (id, data) => request({ url: `${prefix}/${id}`, method: 'put', data }),
    remove: (id) => request({ url: `${prefix}/${id}`, method: 'delete' })
  }
}

export const collegeApi = buildCrud(ENTITY_PREFIX.college)
export const majorApi = buildCrud(ENTITY_PREFIX.major)
export const courseApi = buildCrud(ENTITY_PREFIX.course)
export const categoryApi = buildCrud(ENTITY_PREFIX.category)

// ---- 资源审核（管理端）----
export function auditList(params) {
  return request({ url: '/admin/resource/audit', method: 'get', params })
}

export function resourceList(params) {
  return request({ url: '/admin/resource', method: 'get', params })
}

export function approveResource(id, data) {
  return request({ url: `/admin/resource/audit/${id}/approve`, method: 'post', data })
}

export function rejectResource(id, data) {
  return request({ url: `/admin/resource/audit/${id}/reject`, method: 'post', data })
}

// ---- 图谱运维 ----
export function addGraphNode(data) {
  return request({ url: '/admin/graph/node', method: 'post', data })
}

export function deleteGraphNode(type, id) {
  return request({ url: `/admin/graph/node/${type}/${id}`, method: 'delete' })
}

export function addGraphRelation(data) {
  return request({ url: '/admin/graph/relation', method: 'post', data })
}

export function deleteGraphRelation(data) {
  return request({ url: '/admin/graph/relation', method: 'delete', data })
}

export function syncGraph(data) {
  return request({ url: '/admin/graph/sync', method: 'post', data })
}

// ---- 用户权限管控（扩展接口：契约未显式列出，按 /api/admin/user 约定）----
export const userApi = {
  list: (params) => request({ url: '/admin/user', method: 'get', params }),
  create: (data) => request({ url: '/admin/user', method: 'post', data }),
  update: (id, data) => request({ url: `/admin/user/${id}`, method: 'put', data }),
  remove: (id) => request({ url: `/admin/user/${id}`, method: 'delete' }),
  setRole: (id, data) => request({ url: `/admin/user/${id}/role`, method: 'put', data })
}

// ---- 数据大屏统计（ECharts）----
export function dashboardStats() {
  return request({ url: '/admin/dashboard/stats', method: 'get' })
}

export function dashboardResourceTrend() {
  return request({ url: '/admin/dashboard/resource-trend', method: 'get' })
}

export function dashboardCategoryDist() {
  return request({ url: '/admin/dashboard/category-dist', method: 'get' })
}

export function dashboardKpHeat() {
  return request({ url: '/admin/dashboard/kp-heat', method: 'get' })
}

export function dashboardCollegeRank() {
  return request({ url: '/admin/dashboard/college-rank', method: 'get' })
}
