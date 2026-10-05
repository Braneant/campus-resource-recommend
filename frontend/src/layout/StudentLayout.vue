<template>
  <el-container class="student-layout">
    <!-- 顶部导航：≥1100px 显示板块导航；更窄时收进抽屉，避免挤压换行 -->
    <el-header class="sl-header">
      <button class="sl-burger" type="button" aria-label="打开导航菜单" @click="drawerOpen = true">
        <el-icon><Menu /></el-icon>
      </button>

      <div class="sl-brand" @click="goHome">
        <img src="/favicon.svg" class="sl-logo" alt="logo" />
        <span class="brand-gradient">智学推荐</span>
        <span class="sl-sub">校园学习资源推荐平台</span>
      </div>

      <StudentNav :active-index="activeIndex" mode="horizontal" :include-upload="false" class="sl-nav" />

      <div class="sl-actions">
        <!-- 上传是「动作」，以按钮呈现，不再混进板块导航 -->
        <el-button type="primary" class="sl-upload" @click="router.push('/student/upload')">
          <el-icon class="sl-btn-icon"><UploadFilled /></el-icon>上传资源
        </el-button>

        <el-dropdown @command="onCommand">
          <span class="sl-user-trigger">
            <el-avatar :size="32" :src="avatarSrc">
              {{ nicknameInitial }}
            </el-avatar>
            <span class="sl-nick">{{ userStore.userInfo?.nickname || '同学' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="upload"><el-icon><UploadFilled /></el-icon>上传资源</el-dropdown-item>
              <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <!-- 窄屏导航抽屉 -->
    <el-drawer v-model="drawerOpen" direction="ltr" size="252px" :with-header="false" class="sl-drawer">
      <div class="sl-drawer-brand">
        <img src="/favicon.svg" class="sl-logo" alt="logo" />
        <span class="brand-gradient">智学推荐</span>
      </div>
      <StudentNav :active-index="activeIndex" mode="vertical" @select="drawerOpen = false" />
    </el-drawer>

    <el-main class="sl-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>

    <el-footer class="sl-footer">
      基于知识图谱 + 协同过滤的校园学习资源推荐平台 · 毕业设计第六套
    </el-footer>
  </el-container>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { usableMediaUrl } from '@/utils/media'
import StudentNav from './StudentNav.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const drawerOpen = ref(false)

const userInfo = computed(() => userStore.userInfo)
/** 头像地址：占位域名视为无头像，el-avatar 自动回退为首字母 */
const avatarSrc = computed(() => usableMediaUrl(userStore.userInfo?.avatar))

/** 当前激活的板块：把详情页与二级页归到所属板块，保证导航高亮不丢 */
const activeIndex = computed(() => {
  const p = route.path
  if (p.startsWith('/student/resource')) return '/student/home'
  if (p.startsWith('/student/requirement')) return '/student/requirement'
  if (p.startsWith('/student/qa')) return '/student/qa'
  return p
})

const nicknameInitial = computed(() => {
  const n = userStore.userInfo?.nickname || userStore.userInfo?.username || '同'
  return n.charAt(0)
})

// 路由变化时自动收起抽屉，避免遮挡内容
watch(() => route.fullPath, () => { drawerOpen.value = false })

function goHome() {
  router.push('/student/home')
}

function onCommand(cmd) {
  if (cmd === 'upload') {
    router.push('/student/upload')
  } else if (cmd === 'profile') {
    router.push('/student/profile')
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出登录？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      router.replace('/student/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.student-layout { min-height: 100vh; display: flex; flex-direction: column; }

.sl-header {
  display: flex; align-items: center; gap: var(--sp-5);
  background: #fff; border-bottom: 1px solid var(--c-border);
  height: 60px; padding: 0 var(--sp-6); position: sticky; top: 0; z-index: var(--z-header);
  box-shadow: var(--e1);
}
.sl-brand { display: flex; align-items: center; gap: var(--sp-2); cursor: pointer; flex-shrink: 0; }
.sl-logo { width: 30px; height: 30px; }
.sl-brand .brand-gradient { font-size: 19px; }
.sl-sub { font-size: var(--fs-micro); color: var(--c-text-sub); margin-left: var(--sp-1); }

.sl-actions { display: flex; align-items: center; gap: var(--sp-4); flex-shrink: 0; margin-left: auto; }
.sl-btn-icon { margin-right: 4px; }
.sl-user-trigger { display: flex; align-items: center; gap: var(--sp-2); cursor: pointer; outline: none; }
.sl-nick { font-size: var(--fs-body); color: var(--c-text); }

/* 汉堡按钮：仅窄屏出现 */
.sl-burger {
  display: none; align-items: center; justify-content: center;
  width: 36px; height: 36px; border: 1px solid var(--c-border);
  background: #fff; border-radius: var(--r-sm); cursor: pointer;
  color: var(--c-text); flex-shrink: 0;
}
.sl-burger:hover { border-color: var(--c-border-strong); }

.sl-main { flex: 1; padding: 0; background: var(--c-bg); }
.sl-footer { text-align: center; color: var(--c-text-sub); font-size: var(--fs-micro); background: var(--c-bg); height: 40px; line-height: 40px; }

.sl-drawer-brand { display: flex; align-items: center; gap: var(--sp-2); padding: 4px 16px 16px; }
.sl-drawer-brand .brand-gradient { font-size: 18px; }

/* ---------- 响应式：<1200px 收进抽屉。
   断点取 1200 而非 1100：实测 1100px 时顶栏仍会溢出 29px，1200 留出安全余量 ---------- */
@media (max-width: 1199px) {
  .sl-header { gap: var(--sp-3); padding: 0 var(--sp-4); }
  .sl-nav { display: none; }
  .sl-upload { display: none; }
  .sl-burger { display: inline-flex; }
}
@media (max-width: 640px) {
  .sl-sub { display: none; }
  .sl-nick { display: none; }
  .sl-brand .brand-gradient { font-size: 17px; }
}
</style>
