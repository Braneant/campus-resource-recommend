<template>
  <div class="page-container">
    <div class="hero app-card">
      <div class="hero-text">
        <h1 class="brand-gradient">发现最适合你的学习资源</h1>
        <p>基于知识图谱与协同过滤，为每位同学精准推荐 · 共 {{ total }} 份资源等你探索</p>
        <div class="hero-search">
          <el-input v-model="filters.keyword" placeholder="搜索资源标题 / 关键词" :prefix-icon="Search" clearable @keyup.enter="load" @clear="load" />
          <el-button type="primary" :icon="Search" @click="load">搜索</el-button>
        </div>
      </div>
      <img class="hero-img" src="/favicon.svg" alt="" />
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

        <div v-loading="loading" class="res-grid">
          <ResourceCard
            v-for="r in list"
            :key="r.id"
            :resource="r"
            @open="openDetail"
          />
          <el-empty v-if="!loading && list.length === 0" description="没有匹配的资源，换个条件试试~" />
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
.hero { display: flex; align-items: center; justify-content: space-between; padding: 26px 30px; margin-bottom: 18px;
  background: linear-gradient(120deg, #eef4ff, #e6f0ff); }
.hero-text h1 { font-size: 24px; margin: 0 0 8px; }
.hero-text p { color: var(--c-text-sub); margin: 0 0 16px; font-size: 13px; }
.hero-search { display: flex; gap: 10px; max-width: 460px; }
.hero-search .el-input { flex: 1; }
.hero-img { width: 64px; height: 64px; opacity: .9; }
.square { display: flex; gap: 18px; align-items: flex-start; }
.filters { width: 240px; flex-shrink: 0; padding: 18px 18px 22px; }
.filters-title { font-weight: 700; margin-bottom: 14px; font-size: 15px; }
.list-area { flex: 1; min-width: 0; }
.list-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.list-count { color: var(--c-text-sub); font-size: 13px; }
.list-count b { color: var(--c-primary); }
.res-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; min-height: 200px; }
.list-page { margin-top: 22px; justify-content: center; }
</style>
