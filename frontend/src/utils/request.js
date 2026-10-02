import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

// 基地址使用相对路径 /api，由 vite 代理到 http://localhost:8080
const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器：统一携带 JWT
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理 code
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 兼容非统一结构（如文件流/二进制），直接返回
    if (res === undefined || res === null || typeof res !== 'object' || !('code' in res)) {
      return res
    }
    if (res.code === 0) {
      return res.data
    }
    // 业务失败：按 code 处理
    if (res.code === 1002) {
      // 未登录 / 无权限 → 清除登录态并跳登录
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      ElMessageBox.alert(res.message || '登录已失效，请重新登录', '提示', {
        confirmButtonText: '去登录',
        callback: () => {
          if (router.currentRoute.value.name !== 'StudentLogin') {
            router.replace({ name: 'StudentLogin' })
          }
        }
      })
      return Promise.reject(new Error(res.message || '未登录'))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    const msg =
      error.response && error.response.data && error.response.data.message
        ? error.response.data.message
        : error.message
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
    ElMessage.error(msg || '网络异常')
    return Promise.reject(error)
  }
)

export default service
