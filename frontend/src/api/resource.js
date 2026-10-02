import request from '@/utils/request'

// 资源模块 /api/resource —— 对齐 03-接口契约 §四
// 资源检索（公开/登录均可）
export function searchResource(params) {
  return request({ url: '/resource/search', method: 'get', params })
}

// 资源详情
export function getResource(id) {
  return request({ url: `/resource/${id}`, method: 'get' })
}

// 在线预览（计浏览量）
export function previewResource(id) {
  return request({ url: `/resource/${id}/preview`, method: 'get' })
}

// 下载（记下载行为）
export function downloadResource(id) {
  return request({ url: `/resource/${id}/download`, method: 'get' })
}

// 评论列表
export function getComments(id, params) {
  return request({ url: `/resource/${id}/comments`, method: 'get', params })
}

// 发表评论
export function addComment(id, data) {
  return request({ url: `/resource/${id}/comments`, method: 'post', data })
}

// 收藏（切换/新增）
export function favoriteResource(id) {
  return request({ url: `/resource/${id}/favorite`, method: 'post' })
}

// 取消收藏
export function cancelFavorite(id) {
  return request({ url: `/resource/${id}/favorite`, method: 'delete' })
}

// 评分
export function rateResource(id, data) {
  return request({ url: `/resource/${id}/rating`, method: 'post', data })
}

// 学生上传资源（multipart/form-data）
export function uploadResource(formData) {
  return request({
    url: '/resource/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
