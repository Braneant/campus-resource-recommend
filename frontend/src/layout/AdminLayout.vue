<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="al-aside">
      <div class="al-brand" @click="router.push('/admin/dashboard')">
        <img src="/favicon.svg" class="al-logo" alt="logo" />
        <div>
          <div class="al-brand-title brand-gradient">智学推荐</div>
          <div class="al-brand-sub">管理控制台</div>
        </div>
      </div>
      <el-menu :default-active="activeIndex" class="al-menu" router background-color="#1f2a44" text-color="#c5d0e6" active-text-color="#ffffff">
        <el-menu-item index="/admin/dashboard"><el-icon><DataLine /></el-icon><span>数据大屏</span></el-menu-item>
        <el-menu-item index="/admin/college"><el-icon><School /></el-icon><span>学院管理</span></el-menu-item>
        <el-menu-item index="/admin/major"><el-icon><OfficeBuilding /></el-icon><span>专业管理</span></el-menu-item>
        <el-menu-item index="/admin/course"><el-icon><Reading /></el-icon><span>课程管理</span></el-menu-item>
        <el-menu-item index="/admin/category"><el-icon><Files /></el-icon><span>资源分类管理</span></el-menu-item>
        <el-menu-item index="/admin/audit"><el-icon><Stamp /></el-icon><span>资源审核</span></el-menu-item>
        <el-menu-item index="/admin/graph"><el-icon><Share /></el-icon><span>知识图谱运维</span></el-menu-item>
        <el-menu-item index="/admin/user"><el-icon><UserFilled /></el-icon><span>用户权限管控</span></el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="al-header">
        <div class="al-crumb">{{ route.meta.title || '管理后台' }}</div>
        <div class="al-user">
          <el-dropdown @command="onCommand">
            <span class="al-user-trigger">
              <el-icon><UserFilled /></el-icon>
              <span>{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员' }}</span>
              <el-tag size="small" type="warning" effect="dark">{{ roleLabel }}</el-tag>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="al-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { AdminRoleMap } from '@/utils/constants'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeIndex = computed(() => route.path)
const roleLabel = computed(() => {
  const r = userStore.userInfo?.adminRole || userStore.userInfo?.role
  return AdminRoleMap[r] || '管理员'
})

function onCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出登录？', '提示', {
      confirmButtonText: '退出', cancelButtonText: '取消', type: 'warning'
    }).then(() => {
      userStore.logout()
      router.replace('/admin/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.admin-layout { height: 100vh; }
.al-aside { background: #1f2a44; display: flex; flex-direction: column; }
.al-brand { display: flex; align-items: center; gap: 10px; padding: 18px 18px; cursor: pointer; }
.al-logo { width: 34px; height: 34px; }
.al-brand-title { font-size: 18px; }
.al-brand-sub { font-size: 11px; color: #8a9bb8; }
.al-menu { border-right: none; flex: 1; }
.al-menu :deep(.el-menu-item.is-active) { background: var(--c-primary) !important; }
.al-header {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-bottom: 1px solid var(--c-border); height: 56px; padding: 0 24px;
}
.al-crumb { font-size: 16px; font-weight: 600; color: var(--c-text); }
.al-user-trigger { display: flex; align-items: center; gap: 8px; cursor: pointer; color: var(--c-text); outline: none; }
.al-main { background: var(--c-bg); padding: 20px 24px; overflow-y: auto; }
</style>
