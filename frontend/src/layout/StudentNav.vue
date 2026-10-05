<template>
  <!-- 学生端导航：横向（顶栏）与纵向（抽屉）共用同一份配置，避免两处维护导致不一致 -->
  <el-menu
    :default-active="activeIndex"
    :mode="mode"
    class="app-nav"
    :class="mode === 'horizontal' ? 'app-nav--h' : 'app-nav--v'"
    router
    :ellipsis="false"
    @select="$emit('select')"
  >
    <el-menu-item index="/student/home">
      <el-icon><Compass /></el-icon>
      <template #title>探索</template>
    </el-menu-item>

    <el-menu-item index="/student/recommend">
      <el-icon><MagicStick /></el-icon>
      <template #title>为我推荐</template>
    </el-menu-item>

    <el-menu-item index="/student/graph">
      <el-icon><Share /></el-icon>
      <template #title>知识图谱</template>
    </el-menu-item>

    <el-sub-menu index="community">
      <template #title>
        <el-icon><ChatDotRound /></el-icon>
        <span>互动</span>
      </template>
      <el-menu-item index="/student/requirement">
        <el-icon><Connection /></el-icon>
        <template #title>资源互助</template>
      </el-menu-item>
      <el-menu-item index="/student/qa">
        <el-icon><ChatDotRound /></el-icon>
        <template #title>问答广场</template>
      </el-menu-item>
    </el-sub-menu>

    <!-- 上传资源是「动作」：顶栏以按钮呈现（includeUpload=false），抽屉里才作为菜单项 -->
    <el-menu-item v-if="includeUpload" index="/student/upload">
      <el-icon><UploadFilled /></el-icon>
      <template #title>上传资源</template>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
defineProps({
  activeIndex: { type: String, default: '' },
  mode: { type: String, default: 'horizontal' },
  /** 是否把「上传资源」作为导航项渲染（顶栏为 false，避免与上传按钮重复） */
  includeUpload: { type: Boolean, default: true }
})
defineEmits(['select'])
</script>

<style scoped>
/* 激活色统一为品牌蓝（Element Plus 默认是 #409eff 的蓝，与品牌不一致） */
.app-nav { --el-menu-active-color: var(--c-primary); }

.app-nav--h { flex: 1; border-bottom: none; background: transparent; }
.app-nav--h :deep(.el-menu-item) { font-size: 14px; }
.app-nav--h :deep(.el-menu-item.is-active),
.app-nav--h :deep(.el-sub-menu.is-active .el-sub-menu__title) { color: var(--c-primary); }

.app-nav--v { border-right: none; }
.app-nav--v :deep(.el-menu-item) { font-size: 14px; }
</style>
