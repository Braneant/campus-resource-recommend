import request from '@/utils/request'

// 问答广场模块 /api/qa —— 对齐 03-接口契约 §七
export function listQa(params) {
  return request({ url: '/qa/list', method: 'get', params })
}

export function createQa(data) {
  return request({ url: '/qa', method: 'post', data })
}

export function getQa(id) {
  return request({ url: `/qa/${id}`, method: 'get' })
}

export function replyQa(id, data) {
  return request({ url: `/qa/${id}/reply`, method: 'post', data })
}

export function setBestAnswer(replyId) {
  return request({ url: `/qa/reply/${replyId}/best`, method: 'post' })
}
