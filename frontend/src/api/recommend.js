import request from '@/utils/request'

// 推荐模块 /api/recommend（核心）—— 对齐 03-接口契约 §八
// 8.1 个性化推荐（三路混合 + 加权融合 + 可解释）
export function personalRecommend(params) {
  return request({ url: '/recommend/personal', method: 'get', params })
}

// 8.2 热门推荐
export function hotRecommend(params) {
  return request({ url: '/recommend/hot', method: 'get', params })
}

// 8.3 知识图谱语义推荐（单资源相关）
export function relatedRecommend(params) {
  return request({ url: '/recommend/related', method: 'get', params })
}

// 8.4 单条推荐解释
export function explainRecommend(params) {
  return request({ url: '/recommend/explain', method: 'get', params })
}
