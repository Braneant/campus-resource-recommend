<template>
  <!-- 表格骨架屏：首次加载时用占位行替代"转圈遮罩"，避免布局跳动 -->
  <div class="table-skeleton" aria-hidden="true">
    <div v-for="r in rows" :key="r" class="ts-row">
      <span
        v-for="c in columns"
        :key="c"
        class="ts-cell"
        :style="{ width: c === 1 ? '56px' : (c % 3 === 0 ? '96px' : 'auto'), flex: c === 1 ? '0 0 56px' : (c % 3 === 0 ? '0 0 96px' : '1 1 auto') }"
      ></span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  rows: { type: Number, default: 6 },
  columns: { type: Number, default: 5 }
})
</script>

<style scoped>
.table-skeleton { padding: 14px 14px 6px; }
.ts-row { display: flex; align-items: center; gap: 16px; padding: 13px 0; border-bottom: 1px solid var(--c-border); }
.ts-row:last-child { border-bottom: none; }
.ts-cell { height: 12px; border-radius: 4px; background: #eef1f6; position: relative; overflow: hidden; }
.ts-cell::after {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(90deg, rgba(255,255,255,0) 0%, rgba(255,255,255,.75) 50%, rgba(255,255,255,0) 100%);
  transform: translateX(-100%);
  animation: ts-shimmer 1.4s ease-in-out infinite;
}
@keyframes ts-shimmer { to { transform: translateX(100%); } }

/* 系统开启"减弱动态效果"时去掉微光，静态占位依然可用 */
@media (prefers-reduced-motion: reduce) {
  .ts-cell::after { animation: none; }
}
</style>
