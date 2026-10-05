<template>
  <!-- 管理端导航：按职能分为四组；折叠态（64px）下由 Element Plus 自动显示 tooltip -->
  <el-menu
    :default-active="activeIndex"
    :collapse="collapsed"
    :collapse-transition="false"
    class="admin-nav"
    router
    background-color="#1f2a44"
    text-color="#c5d0e6"
    active-text-color="#ffffff"
    @select="$emit('select')"
  >
    <el-menu-item-group>
      <template #title><span class="al-nav-group">概览</span></template>
      <el-menu-item index="/admin/dashboard">
        <el-icon><DataLine /></el-icon>
        <template #title>数据大屏</template>
      </el-menu-item>
    </el-menu-item-group>

    <el-menu-item-group>
      <template #title><span class="al-nav-group">学科数据</span></template>
      <el-menu-item index="/admin/college">
        <el-icon><School /></el-icon>
        <template #title>学院管理</template>
      </el-menu-item>
      <el-menu-item index="/admin/major">
        <el-icon><OfficeBuilding /></el-icon>
        <template #title>专业管理</template>
      </el-menu-item>
      <el-menu-item index="/admin/course">
        <el-icon><Reading /></el-icon>
        <template #title>课程管理</template>
      </el-menu-item>
      <el-menu-item index="/admin/category">
        <el-icon><Files /></el-icon>
        <template #title>资源分类管理</template>
      </el-menu-item>
    </el-menu-item-group>

    <el-menu-item-group>
      <template #title><span class="al-nav-group">内容与知识治理</span></template>
      <el-menu-item index="/admin/audit">
        <el-icon><Stamp /></el-icon>
        <template #title>
          <span class="al-item-title">资源审核</span>
          <span v-if="pendingCount > 0" class="al-badge" :title="pendingCount + ' 条待审核'">{{ pendingCount }}</span>
        </template>
      </el-menu-item>
      <el-menu-item index="/admin/graph">
        <el-icon><Share /></el-icon>
        <template #title>知识图谱运维</template>
      </el-menu-item>
    </el-menu-item-group>

    <el-menu-item-group>
      <template #title><span class="al-nav-group">系统</span></template>
      <el-menu-item index="/admin/user">
        <el-icon><UserFilled /></el-icon>
        <template #title>用户权限管控</template>
      </el-menu-item>
    </el-menu-item-group>
  </el-menu>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { auditList } from '@/api/admin'

defineProps({
  activeIndex: { type: String, default: '' },
  collapsed: { type: Boolean, default: false }
})
defineEmits(['select'])

/** 待审核数量角标：复用审核列表接口，只取总数（pageSize=1），失败静默不影响导航 */
const pendingCount = ref(0)
onMounted(() => {
  auditList({ pageNum: 1, pageSize: 1, status: 0 })
    .then((d) => { pendingCount.value = (d && d.total) || 0 })
    .catch(() => {})
})
</script>

<style scoped>
.admin-nav { border-right: none; flex: 1; }
.admin-nav :deep(.el-menu-item.is-active) { background: var(--c-primary) !important; }
.al-item-title { display: inline-flex; align-items: center; }
.al-badge {
  display: inline-block; min-width: 18px; height: 18px; line-height: 18px;
  margin-left: 8px; padding: 0 5px; border-radius: var(--r-full);
  background: var(--badge-err-fg); color: #fff;
  font-size: 11px; text-align: center;
}
.al-nav-group {
  display: block;
  padding: 10px 20px 4px;
  font-size: 11px;
  letter-spacing: .5px;
  color: #7c8bab;
}
.admin-nav.el-menu--collapse :deep(.al-nav-group) { display: none; }
</style>
