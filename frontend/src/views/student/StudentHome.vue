<template>
  <div class="page-container">
    <div class="hero app-card">
      <div class="hero-text">
        <h1 class="hero-title">
          <span
            v-for="(c, i) in heroChars"
            :key="i"
            class="hero-ch"
            :style="{ color: c.color, animationDelay: i * 45 + 'ms' }"
          >{{ c.ch }}</span>
        </h1>
        <p>基于知识图谱与协同过滤，为每位同学精准推荐 · 共 {{ total }} 份资源等你探索</p>
        <div class="hero-search">
          <el-input v-model="filters.keyword" placeholder="搜索资源标题 / 关键词" :prefix-icon="Search" clearable @keyup.enter="load" @clear="load" />
          <el-button type="primary" :icon="Search" @click="load">搜索</el-button>
        </div>
      </div>
      <KnowledgeHero class="hero-graph" />
    </div>

    <div class="square">
      <!-- 左侧筛选 -->
      <aside class="filters app-card">
        <div class="filters-title">筛选条件</div>
        <el-form label-position="top" size="default">
          <el-form-item label="资源类型">
            <el-select v-model="filters.type" placeholder="全部类型" clearable style="width:100%" @change="load">
              <el-option v-for="(v, k) in typeMap" :key="k" :label="v.label" :value="Number(k)" />
            </el-select>
          </el-form-item>
          <el-form-item label="学院">
            <el-select v-model="filters.collegeId" placeholder="全部学院" clearable style="width:100%" @change="load">
              <el-option v-for="c in collegeOptions" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="课程">
            <el-select v-model="filters.courseId" placeholder="全部课程" clearable style="width:100%" @change="load">
              <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="filters.categoryId" placeholder="全部分类" clearable style="width:100%" @change="load">
              <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序">
            <el-radio-group v-model="filters.sort" @change="load">
              <el-radio-button value="hot">最热</el-radio-button>
              <el-radio-button value="new">最新</el-radio-button>
              <el-radio-button value="rating">评分</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-button text type="info" :icon="RefreshLeft" @click="resetFilters">重置筛选</el-button>
        </el-form>
      </aside>

      <!-- 右侧资源列表 -->
      <section class="list-area">
        <div class="list-bar">
          <span class="list-count">共 <b>{{ total }}</b> 条结果</span>
          <el-button type="primary" plain :icon="Upload" @click="$router.push('/student/upload')">上传资源</el-button>
        </div>

        <!-- 首次加载显示骨架屏，后续筛选加载保留列表并用遮罩 -->
        <div v-if="loading && list.length === 0" class="res-grid">
          <div v-for="n in pageSize" :key="'sk' + n" class="sk-card app-card">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="image" class="sk-cover" />
                <div class="sk-body">
                  <el-skeleton-item variant="h3" style="width:82%" />
                  <el-skeleton-item variant="text" style="margin-top:10px;width:55%" />
                  <el-skeleton-item variant="text" style="margin-top:8px;width:70%" />
                </div>
              </template>
            </el-skeleton>
          </div>
        </div>

        <div v-else v-loading="loading" class="res-grid">
          <ResourceCard
            v-for="r in list"
            :key="r.id"
            :resource="r"
            @open="openDetail"
          />
          <el-empty v-if="!loading && list.length === 0" description="没有匹配的资源，换个条件试试~">
            <el-button plain @click="resetFilters">重置筛选条件</el-button>
          </el-empty>
        </div>

        <el-pagination
          v-if="total > 0"
          class="list-page"
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="onPage"
          background
        />
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Upload, RefreshLeft, User } from '@element-plus/icons-vue'
import ResourceCard from '@/components/ResourceCard.vue'
import KnowledgeHero from '@/components/bits/KnowledgeHero.vue'
import { searchResource } from '@/api/resource'
import { ResourceTypeMap } from '@/utils/constants'
import { categoryOptions, collegeOptions, courseOptions } from '@/utils/options'

const router = useRouter()
const typeMap = ResourceTypeMap
const list = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(12)

const filters = reactive({
  keyword: '', type: null, collegeId: null, courseId: null,
  categoryId: null, sort: 'hot'
})

/** hero 标题：逐字着色（品牌蓝 → 知识图谱紫线性过渡）+ 逐字入场 */
const heroChars = (() => {
  const text = '发现最适合你的学习资源'
  const from = [47, 109, 246]
  const to = [139, 92, 246]
  const chars = [...text]
  return chars.map((ch, i) => {
    const t = chars.length > 1 ? i / (chars.length - 1) : 0
    const rgb = from.map((v, k) => Math.round(v + (to[k] - v) * t))
    return { ch, color: `rgb(${rgb[0]}, ${rgb[1]}, ${rgb[2]})` }
  })
})()

function load() {
  loading.value = true
  const params = {
    keyword: filters.keyword || undefined,
    type: filters.type || undefined,
    collegeId: filters.collegeId || undefined,
    courseId: filters.courseId || undefined,
    categoryId: filters.categoryId || undefined,
    sort: filters.sort,
    pageNum: pageNum.value,
    pageSize: pageSize.value
  }
  searchResource(params)
    .then((data) => {
      list.value = data.list || []
      total.value = data.total || 0
    })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function onPage(p) { pageNum.value = p; load() }

function resetFilters() {
  filters.keyword = ''
  filters.type = null
  filters.collegeId = null
  filters.courseId = null
  filters.categoryId = null
  filters.sort = 'hot'
  pageNum.value = 1
  load()
}

function openDetail(id) { router.push(`/student/resource/${id}`) }

onMounted(load)
</script>

<style scoped>
.hero { position: relative; display: flex; align-items: center; justify-content: space-between; padding: 26px 30px; margin-bottom: 18px;
  background: linear-gradient(120deg, #eef4ff, #e6f0ff); overflow: hidden; }
.hero-text { flex: 1; min-width: 0; }
.hero-text h1 { margin: 0 0 8px; }
.hero-title { font-size: 24px; font-weight: 800; line-height: 1.3; }
.hero-ch { display: inline-block; animation: ch-in .5s cubic-bezier(.22, .8, .3, 1) both; }
@keyframes ch-in {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: none; }
}
.hero-text p { color: var(--c-text-sub); margin: 0 0 16px; font-size: 13px; }
.hero-search { display: flex; gap: 10px; max-width: 460px; }
.hero-search .el-input { flex: 1; }
.hero-graph { width: 320px; height: 160px; flex-shrink: 0; margin-left: 14px; }
@media (max-width: 1100px) {
  .hero-graph { width: 260px; height: 150px; margin-left: 10px; }
}
@media (max-width: 900px) {
  .square { flex-direction: column; }
  .square .filters { width: 100%; }
  .list-area { width: 100%; }
}
@media (max-width: 900px) {
  .hero-graph { display: none; }
}
.square { display: flex; gap: 18px; align-items: flex-start; }
.filters { width: 240px; flex-shrink: 0; padding: 18px 18px 22px; }
.filters-title { font-weight: 700; margin-bottom: 14px; font-size: 15px; }
.list-area { flex: 1; min-width: 0; }
.list-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.list-count { color: var(--c-text-sub); font-size: 13px; }
.list-count b { color: var(--c-primary); }
.res-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; min-height: 200px; }
.list-page { margin-top: 22px; justify-content: center; }

/* 骨架屏卡片（与真实卡片同尺寸，避免加载时跳动） */
.sk-card { border-radius: 12px; overflow: hidden; }
.sk-cover { width: 100%; height: 132px; display: block; }
.sk-body { padding: 12px; }

/* 尊重系统「减弱动态效果」偏好 */
@media (prefers-reduced-motion: reduce) {
  .hero-ch { animation: none; }
}
</style>
