import request from '@/utils/request'

// 用户模块 /api/user —— 对齐 03-接口契约 §三
export function getProfile() {
  return request({ url: '/user/profile', method: 'get' })
}

export function updateProfile(data) {
  return request({ url: '/user/profile', method: 'put', data })
}

export function changePassword(data) {
  return request({ url: '/user/password', method: 'put', data })
}

// 个人中心的学习足迹
export function getUserOverview() {
  return request({ url: '/user/overview', method: 'get' })
}

export function getUserFavorites(params) {
  return request({ url: '/user/favorites', method: 'get', params })
}

export function getUserDownloads(params) {
  return request({ url: '/user/downloads', method: 'get', params })
}

export function getUserUploads(params) {
  return request({ url: '/user/uploads', method: 'get', params })
}
