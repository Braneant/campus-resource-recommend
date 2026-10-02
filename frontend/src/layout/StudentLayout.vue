<template>
  <el-container class="student-layout">
    <!-- 顶部导航 -->
    <el-header class="sl-header">
      <div class="sl-brand" @click="goHome">
        <img src="/favicon.svg" class="sl-logo" alt="logo" />
        <span class="brand-gradient">智学推荐</span>
        <span class="sl-sub">校园学习资源推荐平台</span>
      </div>
      <el-menu :default-active="activeIndex" mode="horizontal" class="sl-menu" router :ellipsis="false">
        <el-menu-item index="/student/home"><el-icon><Compass /></el-icon>资源广场</el-menu-item>
        <el-menu-item index="/student/recommend"><el-icon><MagicStick /></el-icon>个性化推荐</el-menu-item>
        <el-menu-item index="/student/graph"><el-icon><Share /></el-icon>知识图谱</el-menu-item>
        <el-menu-item index="/student/requirement"><el-icon><Connection /></el-icon>资源互助</el-menu-item>
        <el-menu-item index="/student/qa"><el-icon><ChatDotRound /></el-icon>问答广场</el-menu-item>
        <el-menu-item index="/student/upload"><el-icon><UploadFilled /></el-icon>上传资源</el-menu-item>
      </el-menu>
      <div class="sl-user">
        <el-dropdown @command="onCommand">
          <span class="sl-user-trigger">
            <el-avatar :size="32" :src="userInfo?.avatar || ''">
              {{ nicknameInitial }}
            </el-avatar>
            <span class="sl-nick">{{ userStore.userInfo?.nickname || '同学' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo)
const activeIndex = computed(() => '/' + (route.path.split('/').slice(0, 3).join('/')))
const nicknameInitial = computed(() => {
  const n = userStore.userInfo?.nickname || userStore.userInfo?.username || '同'
  return n.charAt(0)
})

function goHome() {
  router.push('/student/home')
}

function onCommand(cmd) {
  if (cmd === 'profile') {
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
  display: flex; align-items: center; gap: 24px;
  background: #fff; border-bottom: 1px solid var(--c-border);
  height: 60px; padding: 0 24px; position: sticky; top: 0; z-index: 100;
  box-shadow: 0 2px 10px rgba(47, 109, 246, 0.05);
}
.sl-brand { display: flex; align-items: center; gap: 8px; cursor: pointer; flex-shrink: 0; }
.sl-logo { width: 30px; height: 30px; }
.sl-brand .brand-gradient { font-size: 19px; }
.sl-sub { font-size: 12px; color: var(--c-text-sub); margin-left: 4px; }
.sl-menu { flex: 1; border-bottom: none; background: transparent; }
.sl-menu :deep(.el-menu-item) { font-size: 14px; }
.sl-menu :deep(.el-menu-item.is-active) { color: var(--c-primary); font-weight: 600; }
.sl-user { flex-shrink: 0; }
.sl-user-trigger { display: flex; align-items: center; gap: 8px; cursor: pointer; outline: none; }
.sl-nick { font-size: 14px; color: var(--c-text); }
.sl-main { flex: 1; padding: 0; background: var(--c-bg); }
.sl-footer { text-align: center; color: var(--c-text-sub); font-size: 12px; background: var(--c-bg); height: 40px; line-height: 40px; }
</style>
