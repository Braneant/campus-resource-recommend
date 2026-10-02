<template>
  <GenericCrud
    title="专业"
    subtitle="维护专业数据，需关联所属学院"
    :api="majorApi"
    :columns="columns"
    :table-columns="tableColumns"
    :filters="filters"
    add-text="新增专业"
  />
</template>

<script setup>
import GenericCrud from '@/components/GenericCrud.vue'
import { majorApi } from '@/api/admin'
import { collegeOptions } from '@/utils/options'

const columns = [
  { prop: 'collegeId', label: '所属学院', type: 'select', required: true, options: collegeOptions },
  { prop: 'name', label: '专业名称', type: 'text', required: true, placeholder: '如 软件工程' },
  { prop: 'code', label: '编码', type: 'text', placeholder: '如 SE' },
  { prop: 'description', label: '简介', type: 'textarea' },
  { prop: 'sort', label: '排序', type: 'number' }
]

const tableColumns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'collegeId', label: '学院', width: 140, formatter: (row) => collegeOptions.find((c) => c.id === row.collegeId)?.name || '—' },
  { prop: 'name', label: '专业名称', minWidth: 160 },
  { prop: 'code', label: '编码', width: 120 },
  { prop: 'description', label: '简介', minWidth: 200 }
]

const filters = [{ prop: 'collegeId', label: '学院', options: collegeOptions }]
</script>
