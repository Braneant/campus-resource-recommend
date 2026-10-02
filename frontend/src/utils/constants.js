// 枚举常量字典：前后端共用，严格对齐 03-接口契约 §1.4

export const ResourceTypeMap = {
  1: { label: '文档', tag: 'primary' },
  2: { label: '视频', tag: 'success' },
  3: { label: '课件', tag: 'warning' },
  4: { label: '其他', tag: 'info' }
}

export const ResourceStatusMap = {
  0: { label: '待审核', tag: 'info' },
  1: { label: '已通过', tag: 'success' },
  2: { label: '已驳回', tag: 'danger' }
}

export const GenderMap = {
  0: '未知',
  1: '男',
  2: '女'
}

export const AdminRoleMap = {
  1: '超级管理员',
  2: '普通管理员'
}

export const RequirementStatusMap = {
  0: { label: '待解决', tag: 'warning' },
  1: { label: '已解决', tag: 'success' }
}

export const QaStatusMap = {
  0: { label: '正常', tag: 'success' },
  1: { label: '已结帖', tag: 'info' }
}

export const GraphNodeTypeMap = {
  COLLEGE: '学院',
  MAJOR: '专业',
  COURSE: '课程',
  KNOWLEDGE_POINT: '知识点',
  RESOURCE: '资源'
}

// 推荐来源标识
export const RecommendSourceMap = {
  cf: { label: '协同过滤', color: '#2f6df6' },
  kg: { label: '知识图谱', color: '#8b5cf6' },
  hot: { label: '热度', color: '#f5a623' }
}

// 图谱关系类型 → 中文 + 颜色（用于可视化）
export const GraphRelMap = {
  BELONGS_TO_KP: { label: '归属知识点', color: '#5b8def' },
  TEACHES: { label: '讲授', color: '#2bbf8a' },
  PREREQUISITE: { label: '先修', color: '#f5a623' },
  SAME_COURSE: { label: '同课程', color: '#8b5cf6' },
  RELATED: { label: '相关', color: '#8a9bb8' },
  BELONGS_TO: { label: '属于', color: '#8a9bb8' }
}

// 图谱节点类型 → 颜色（ECharts 用）
export const GraphNodeColor = {
  COLLEGE: '#2f6df6',
  MAJOR: '#5b8def',
  COURSE: '#2bbf8a',
  KNOWLEDGE_POINT: '#8b5cf6',
  RESOURCE: '#f5a623'
}
