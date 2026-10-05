<template>
  <el-container class="admin-layout">
    <!-- 侧边栏：≥1100px 常驻，可折叠为 64px 图标栏 -->
    <el-aside v-if="!isNarrow" :width="collapsed ? '64px' : '220px'" class="al-aside">
      <div class="al-brand" @click="router.push('/admin/dashboard')">
        <img src="/favicon.svg" class="al-logo" alt="logo" />
        <div v-show="!collapsed" class="al-brand-text">
          <div class="al-brand-title brand-gradient">智学推荐</div>
          <div class="al-brand-sub">管理控制台</div>
        </div>
      </div>
      <AdminNav :active-index="activeIndex" :collapsed="collapsed" />
    </el-aside>

    <!-- 窄屏：侧栏改为抽屉 -->
    <el-drawer v-model="drawerOpen" direction="ltr" size="248px" :with-header="false" class="al-drawer">
      <div class="al-brand" @click="router.push('/admin/dashboard')">
        <img src="/favicon.svg" class="al-logo" alt="logo" />
        <div class="al-brand-text">
          <div class="al-brand-title brand-gradient">智学推荐</div>
          <div class="al-brand-sub">管理控制台</div>
        </div>
      </div>
      <AdminNav :active-index="activeIndex" :collapsed="false" @select="drawerOpen = false" />
    </el-drawer>

    <el-container>
      <el-header class="al-header">
        <button
          v-if="isNarrow"
          class="al-icon-btn"
          type="button"
          aria-label="打开导航菜单"
          @click="drawerOpen = true"
        >
          <el-icon><Menu /></el-icon>
        </button>
        <button
          v-else
          class="al-icon-btn"
          type="button"
          :aria-label="collapsed ? '展开侧栏' : '收起侧栏'"
          @click="toggleCollapse"
        >
          <el-icon v-if="collapsed"><Expand /></el-icon>
          <el-icon v-else><Fold /></el-icon>
        </button>

        <div class="al-crumb">{{ route.meta.title || '管理后台' }}</div>

        <div class="al-user">
          <el-dropdown @command="onCommand">
            <span class="al-user-trigger">
              <el-icon><UserFilled /></el-icon>
              <span class="al-uname">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员' }}</span>
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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { AdminRoleMap } from '@/utils/constants'
import AdminNav from './AdminNav.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const COLLAPSE_KEY = 'adminAsideCollapsed'
const collapsed = ref(localStorage.getItem(COLLAPSE_KEY) === '1')
const drawerOpen = ref(false)
/** 窄屏判定：<1100px 时侧栏改为抽屉，避免表格与内容被挤压 */
const isNarrow = ref(typeof window !== 'undefined' ? window.innerWidth < 1100 : false)

function onResize() {
  isNarrow.value = window.innerWidth < 1100
}
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

function toggleCollapse() {
  collapsed.value = !collapsed.value
  localStorage.setItem(COLLAPSE_KEY, collapsed.value ? '1' : '0')
}

const activeIndex = computed(() => route.path)
const roleLabel = computed(() => {
  const r = userStore.userInfo?.adminRole || userStore.userInfo?.role
  return AdminRoleMap[r] || '管理员'
})

watch(() => route.fullPath, () => { drawerOpen.value = false })

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

.al-aside { background: #1f2a44; display: flex; flex-direction: column; transition: width var(--du-base) var(--ease); overflow: hidden; }
.al-brand { display: flex; align-items: center; gap: var(--sp-3); padding: 18px; cursor: pointer; flex-shrink: 0; }
.al-logo { width: 34px; height: 34px; flex-shrink: 0; }
.al-brand-text { min-width: 0; }
.al-brand-title { font-size: 18px; white-space: nowrap; }
.al-brand-sub { font-size: 11px; color: #8a9bb8; white-space: nowrap; }

.al-header {
  display: flex; align-items: center; gap: var(--sp-3);
  background: #fff; border-bottom: 1px solid var(--c-border);
  height: 56px; padding: 0 var(--sp-6); flex-shrink: 0;
}
.al-icon-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; flex-shrink: 0;
  border: 1px solid var(--c-border); background: #fff;
  border-radius: var(--r-sm); cursor: pointer; color: var(--c-text-sub);
}
.al-icon-btn:hover { border-color: var(--c-border-strong); color: var(--c-primary); }
.al-crumb { font-size: var(--fs-h2); font-weight: 500; color: var(--c-text); }
.al-user { margin-left: auto; }
.al-user-trigger { display: flex; align-items: center; gap: var(--sp-2); cursor: pointer; color: var(--c-text); outline: none; }
.al-uname { font-size: var(--fs-body); }

.al-main { background: var(--c-bg); padding: var(--sp-5) var(--sp-6); overflow-y: auto; }

.al-drawer :deep(.el-drawer__body) { padding: 0; background: #1f2a44; }

@media (max-width: 1199px) {
  .al-header { padding: 0 var(--sp-4); }
  .al-main { padding: var(--sp-4); }
}
@media (max-width: 640px) {
  .al-uname { display: none; }
}
</style>
