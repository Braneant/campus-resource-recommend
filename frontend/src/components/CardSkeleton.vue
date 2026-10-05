<template>
  <!-- 卡片列表骨架屏：学生端列表（帖子/需求/推荐）首次加载时使用 -->
  <div class="card-skeleton" aria-hidden="true">
    <div v-for="r in rows" :key="r" class="cs-card">
      <div class="cs-block"></div>
      <div class="cs-body">
        <div class="cs-line" style="width:58%"></div>
        <div class="cs-line" style="width:88%"></div>
        <div class="cs-line" style="width:36%"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  rows: { type: Number, default: 3 }
})
</script>

<style scoped>
.card-skeleton { display: flex; flex-direction: column; gap: var(--sp-4); }
.cs-card { display: flex; gap: var(--sp-4); padding: var(--sp-4) var(--sp-5);
  background: var(--c-card); border: 1px solid var(--c-border); border-radius: var(--r-md); }
.cs-block { width: 72px; height: 72px; flex-shrink: 0; border-radius: var(--r-sm); background: #eef1f6; }
.cs-body { flex: 1; display: flex; flex-direction: column; gap: 10px; justify-content: center; }
.cs-line { height: 12px; border-radius: 4px; background: #eef1f6; position: relative; overflow: hidden; }
.cs-line::after, .cs-block::after {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(90deg, rgba(255,255,255,0) 0%, rgba(255,255,255,.75) 50%, rgba(255,255,255,0) 100%);
  transform: translateX(-100%);
  animation: cs-shimmer 1.4s ease-in-out infinite;
}
.cs-block { position: relative; overflow: hidden; }
@keyframes cs-shimmer { to { transform: translateX(100%); } }

@media (prefers-reduced-motion: reduce) {
  .cs-line::after, .cs-block::after { animation: none; }
}
</style>
