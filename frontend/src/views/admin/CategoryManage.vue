<template>
  <GenericCrud
    title="资源分类"
    subtitle="维护资源分类树（支持父级分类）"
    :api="categoryApi"
    :columns="columns"
    :table-columns="tableColumns"
    add-text="新增分类"
  />
</template>

<script setup>
import GenericCrud from '@/components/GenericCrud.vue'
import { categoryApi } from '@/api/admin'
import { categoryOptions } from '@/utils/options'

const columns = [
  { prop: 'name', label: '分类名称', type: 'text', required: true, placeholder: '如 课件讲义' },
  { prop: 'parentId', label: '父级分类', type: 'select', options: categoryOptions, placeholder: '不选则为顶级' },
  { prop: 'code', label: '编码', type: 'text', placeholder: '如 COURSEWARE' },
  { prop: 'description', label: '简介', type: 'textarea' },
  { prop: 'sort', label: '排序', type: 'number' }
]

const tableColumns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'name', label: '分类名称', minWidth: 160 },
  { prop: 'parentName', label: '父级', width: 140, formatter: (row) => (row.parentId ? categoryOptions.find((c) => c.id === row.parentId)?.name || '—' : '顶级') },
  { prop: 'code', label: '编码', width: 140 },
  { prop: 'description', label: '简介', minWidth: 200 }
]
</script>
