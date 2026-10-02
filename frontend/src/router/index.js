import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

// 学生端认证页
import StudentLogin from '@/views/student/StudentLogin.vue'
import StudentRegister from '@/views/student/StudentRegister.vue'
import StudentForgot from '@/views/student/StudentForgot.vue'
// 学生端布局 + 页面
import StudentLayout from '@/layout/StudentLayout.vue'
import StudentHome from '@/views/student/StudentHome.vue'
import ResourceDetail from '@/views/student/ResourceDetail.vue'
import StudentRecommend from '@/views/student/StudentRecommend.vue'
import StudentGraph from '@/views/student/StudentGraph.vue'
import RequirementList from '@/views/student/RequirementList.vue'
import RequirementDetail from '@/views/student/RequirementDetail.vue'
import QaList from '@/views/student/QaList.vue'
import QaDetail from '@/views/student/QaDetail.vue'
import StudentUpload from '@/views/student/StudentUpload.vue'
import StudentProfile from '@/views/student/StudentProfile.vue'

// 管理端
import AdminLogin from '@/views/admin/AdminLogin.vue'
import AdminLayout from '@/layout/AdminLayout.vue'
import AdminDashboard from '@/views/admin/AdminDashboard.vue'
import CollegeManage from '@/views/admin/CollegeManage.vue'
import MajorManage from '@/views/admin/MajorManage.vue'
import CourseManage from '@/views/admin/CourseManage.vue'
import CategoryManage from '@/views/admin/CategoryManage.vue'
import ResourceAudit from '@/views/admin/ResourceAudit.vue'
import GraphManage from '@/views/admin/GraphManage.vue'
import UserManage from '@/views/admin/UserManage.vue'

const routes = [
  { path: '/', redirect: '/student/home' },

  // ---------- 学生端认证 ----------
  { path: '/student/login', name: 'StudentLogin', component: StudentLogin, meta: { public: true, title: '学生登录' } },
  { path: '/student/register', name: 'StudentRegister', component: StudentRegister, meta: { public: true, title: '学生注册' } },
  { path: '/student/forgot', name: 'StudentForgot', component: StudentForgot, meta: { public: true, title: '找回密码' } },

  // ---------- 学生端应用 ----------
  {
    path: '/student',
    component: StudentLayout,
    redirect: '/student/home',
    children: [
      { path: 'home', name: 'StudentHome', component: StudentHome, meta: { requiresAuth: true, role: 'STUDENT', title: '资源广场' } },
      { path: 'resource/:id', name: 'ResourceDetail', component: ResourceDetail, meta: { requiresAuth: true, role: 'STUDENT', title: '资源详情' } },
      { path: 'recommend', name: 'StudentRecommend', component: StudentRecommend, meta: { requiresAuth: true, role: 'STUDENT', title: '个性化推荐' } },
      { path: 'graph', name: 'StudentGraph', component: StudentGraph, meta: { requiresAuth: true, role: 'STUDENT', title: '知识图谱' } },
      { path: 'requirement', name: 'RequirementList', component: RequirementList, meta: { requiresAuth: true, role: 'STUDENT', title: '资源互助' } },
      { path: 'requirement/:id', name: 'RequirementDetail', component: RequirementDetail, meta: { requiresAuth: true, role: 'STUDENT', title: '需求详情' } },
      { path: 'qa', name: 'QaList', component: QaList, meta: { requiresAuth: true, role: 'STUDENT', title: '问答广场' } },
      { path: 'qa/:id', name: 'QaDetail', component: QaDetail, meta: { requiresAuth: true, role: 'STUDENT', title: '帖子详情' } },
      { path: 'upload', name: 'StudentUpload', component: StudentUpload, meta: { requiresAuth: true, role: 'STUDENT', title: '上传资源' } },
      { path: 'profile', name: 'StudentProfile', component: StudentProfile, meta: { requiresAuth: true, role: 'STUDENT', title: '个人中心' } }
    ]
  },

  // ---------- 管理端 ----------
  { path: '/admin/login', name: 'AdminLogin', component: AdminLogin, meta: { public: true, title: '管理员登录' } },
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: AdminDashboard, meta: { requiresAuth: true, role: 'ADMIN', title: '数据大屏' } },
      { path: 'college', name: 'AdminCollege', component: CollegeManage, meta: { requiresAuth: true, role: 'ADMIN', title: '学院管理' } },
      { path: 'major', name: 'AdminMajor', component: MajorManage, meta: { requiresAuth: true, role: 'ADMIN', title: '专业管理' } },
      { path: 'course', name: 'AdminCourse', component: CourseManage, meta: { requiresAuth: true, role: 'ADMIN', title: '课程管理' } },
      { path: 'category', name: 'AdminCategory', component: CategoryManage, meta: { requiresAuth: true, role: 'ADMIN', title: '资源分类管理' } },
      { path: 'audit', name: 'AdminAudit', component: ResourceAudit, meta: { requiresAuth: true, role: 'ADMIN', title: '资源审核' } },
      { path: 'graph', name: 'AdminGraph', component: GraphManage, meta: { requiresAuth: true, role: 'ADMIN', title: '知识图谱运维' } },
      { path: 'user', name: 'AdminUser', component: UserManage, meta: { requiresAuth: true, role: 'ADMIN', title: '用户权限管控' } }
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/student/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：登录态 + 角色校验
router.beforeEach((to) => {
  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth

  if (!requiresAuth) return true

  if (!userStore.isLoggedIn) {
    // 未登录 → 跳到对应登录页
    return to.meta.role === 'ADMIN'
      ? { name: 'AdminLogin', query: { redirect: to.fullPath } }
      : { name: 'StudentLogin', query: { redirect: to.fullPath } }
  }

  // 已登录但角色不符
  if (to.meta.role && userStore.role !== to.meta.role) {
    return userStore.isStudent ? { name: 'StudentHome' } : { name: 'AdminDashboard' }
  }
  return true
})

router.afterEach((to) => {
  const base = '智学推荐 · 校园学习资源推荐平台'
  document.title = to.meta?.title ? `${to.meta.title} - ${base}` : base
})

export default router
