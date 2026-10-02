import request from '@/utils/request'

// 资源需求互助模块 /api/requirement —— 对齐 03-接口契约 §六
export function listRequirement(params) {
  return request({ url: '/requirement/list', method: 'get', params })
}

export function createRequirement(data) {
  return request({ url: '/requirement', method: 'post', data })
}

export function getRequirement(id) {
  return request({ url: `/requirement/${id}`, method: 'get' })
}

export function replyRequirement(id, data) {
  return request({ url: `/requirement/${id}/reply`, method: 'post', data })
}

export function resolveRequirement(id) {
  return request({ url: `/requirement/${id}/resolve`, method: 'post' })
}
