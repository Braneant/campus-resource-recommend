import request from '@/utils/request'

// 知识图谱可视化模块 /api/graph —— 对齐 03-接口契约 §九
// 9.1 节点列表/搜索
export function listGraphNodes(params) {
  return request({ url: '/graph/nodes', method: 'get', params })
}

// 9.2 关系列表
export function listGraphEdges(params) {
  return request({ url: '/graph/edges', method: 'get', params })
}

// 9.3 多跳子图（力导向图渲染）
export function getSubgraph(params) {
  return request({ url: '/graph/subgraph', method: 'get', params })
}

// 9.4 节点详情 + 邻居
export function getGraphNode(type, id) {
  return request({ url: `/graph/node/${type}/${id}`, method: 'get' })
}
