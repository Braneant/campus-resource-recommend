// 演示用筛选选项（学生端未登录态下无法调用需鉴权的 /api/admin 列表接口；
// 生产环境应由后端提供公开的下拉数据接口，此处提供与种子数据一致的静态选项以便联调）

export const categoryOptions = [
  { id: 1, name: '课件讲义' },
  { id: 2, name: '习题试卷' },
  { id: 3, name: '实验指导' },
  { id: 4, name: '复习资料' },
  { id: 5, name: '读书笔记' },
  { id: 6, name: '项目案例' }
]

export const collegeOptions = [
  { id: 1, name: '计算机学院' },
  { id: 2, name: '数学学院' },
  { id: 3, name: '电子信息学院' },
  { id: 4, name: '机械工程学院' }
]

export const courseOptions = [
  { id: 101, name: '数据结构' },
  { id: 102, name: '算法设计与分析' },
  { id: 103, name: '操作系统' },
  { id: 104, name: '计算机网络' },
  { id: 105, name: '数据库系统' },
  { id: 106, name: '计算机组成原理' },
  { id: 201, name: '高等数学' },
  { id: 202, name: '线性代数' }
]
