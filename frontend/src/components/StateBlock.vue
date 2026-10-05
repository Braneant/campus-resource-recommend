<template>
  <!-- 统一状态块：空态 / 无结果 / 加载失败。替代默认的 el-empty（无行动指引） -->
  <div class="state-block" :class="'state-' + type" role="status">
    <div class="sb-icon" :class="'sb-icon--' + type">
      <el-icon>
        <WarningFilled v-if="type === 'error'" />
        <Search v-else-if="type === 'no-result'" />
        <Box v-else />
      </el-icon>
    </div>
    <div class="sb-title">{{ title || defaultTitle }}</div>
    <div v-if="description || defaultDescription" class="sb-desc">
      {{ description || defaultDescription }}
    </div>
    <div v-if="actionText" class="sb-action">
      <el-button :type="type === 'error' ? 'primary' : 'default'" @click="$emit('action')">
        {{ actionText }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** empty：确实没有数据；no-result：筛选后无匹配；error：加载失败 */
  type: { type: String, default: 'empty' },
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  actionText: { type: String, default: '' }
})
defineEmits(['action'])

const defaultTitle = computed(() => ({
  empty: '这里还没有数据',
  'no-result': '没有找到匹配的结果',
  error: '加载失败'
}[props.type] || '这里还没有数据'))

const defaultDescription = computed(() => ({
  empty: '',
  'no-result': '可以试试更换关键词，或清空筛选条件后重新查找',
  error: '请检查网络连接后重试；若持续失败，可稍后再试或联系管理员'
}[props.type] || ''))
</script>

<style scoped>
.state-block { padding: 40px 20px; text-align: center; }
.sb-icon { width: 52px; height: 52px; margin: 0 auto 12px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; font-size: 24px; }
.sb-icon--empty { background: #eef1f6; color: var(--badge-info-fg); }
.sb-icon--no-result { background: #eaf1ff; color: var(--badge-cf-fg); }
.sb-icon--error { background: #feeceb; color: var(--badge-err-fg); }
.sb-title { font-size: var(--fs-h3); font-weight: 500; color: var(--c-text); }
.sb-desc { margin-top: 6px; font-size: var(--fs-cap); color: var(--c-text-sub); line-height: 1.7; }
.sb-action { margin-top: 16px; }
</style>
