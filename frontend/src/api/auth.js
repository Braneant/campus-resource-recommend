import request from '@/utils/request'

// 鉴权模块 /api/auth —— 对齐 03-接口契约 §二
export function register(data) {
  return request({ url: '/auth/register', method: 'post', data })
}

export function login(data) {
  return request({ url: '/auth/login', method: 'post', data })
}

export function forgotPassword(data) {
  return request({ url: '/auth/forgot-password', method: 'post', data })
}

export function resetPassword(data) {
  return request({ url: '/auth/reset-password', method: 'post', data })
}
