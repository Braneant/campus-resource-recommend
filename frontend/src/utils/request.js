import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

// 基地址使用相对路径 /api，由 vite 代理到 http://localhost:8080
const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

/* ---------------- 错误文案映射 ----------------
   原则：给用户「原因 + 下一步做什么」，不要只抛“服务器内部错误”。
   后端返回具体业务文案时优先展示；返回无信息量的兜底文案时，改按状态码映射。 */
const GENERIC_MESSAGES = ['服务器内部错误', '系统异常', '未知错误', 'Error', 'Internal Server Error']

const HTTP_FRIENDLY = {
  400: '请求参数有误，请检查填写内容后重试',
  401: '登录已过期，请重新登录',
  403: '你没有权限执行该操作',
  404: '请求的内容不存在，可能已被删除',
  405: '当前操作方式不被支持，请刷新页面后重试',
  408: '请求超时，请检查网络后重试',
  413: '文件过大，请压缩后重新上传',
  500: '服务暂时不可用，请稍后重试',
  502: '服务暂时不可用，请稍后重试',
  503: '服务正在维护中，请稍后重试',
  504: '服务响应超时，请稍后重试'
}

/** 是否为无信息量的兜底文案 */
function isGenericMessage(msg) {
  if (!msg) return true
  return GENERIC_MESSAGES.includes(String(msg).trim())
}

/** 把任意异常转成一句用户看得懂、且知道下一步做什么的话 */
function toFriendlyMessage(error) {
  // 超时
  if (error && error.code === 'ECONNABORTED') return '请求超时，请检查网络后重试'
  // 断网 / 服务未启动
  if (!error || !error.response) return '网络连接失败，请检查网络后重试'
  const backendMsg = error.response.data && error.response.data.message
  if (!isGenericMessage(backendMsg)) return backendMsg
  return HTTP_FRIENDLY[error.response.status] || '操作失败，请稍后重试'
}

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
    ElMessage.error(isGenericMessage(res.message) ? '操作失败，请稍后重试' : res.message)
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
    ElMessage.error(toFriendlyMessage(error))
    return Promise.reject(error)
  }
)

export default service
