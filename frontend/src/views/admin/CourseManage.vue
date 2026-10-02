<template>
  <GenericCrud
    title="课程"
    subtitle="维护课程数据，关联学院与专业"
    :api="courseApi"
    :columns="columns"
    :table-columns="tableColumns"
    :filters="filters"
    add-text="新增课程"
  />
</template>

<script setup>
import GenericCrud from '@/components/GenericCrud.vue'
import { courseApi } from '@/api/admin'
import { collegeOptions } from '@/utils/options'

const majorOptions = [
  { id: 1, name: '计算机科学与技术' },
  { id: 2, name: '软件工程' },
  { id: 3, name: '人工智能' }
]

const columns = [
  { prop: 'collegeId', label: '学院', type: 'select', required: true, options: collegeOptions },
  { prop: 'majorId', label: '专业', type: 'select', required: true, options: majorOptions },
  { prop: 'name', label: '课程名称', type: 'text', required: true, placeholder: '如 数据结构' },
  { prop: 'code', label: '课程编号', type: 'text', placeholder: '如 CS101' },
  { prop: 'description', label: '简介', type: 'textarea' },
  { prop: 'sort', label: '排序', type: 'number' }
]

const tableColumns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'collegeId', label: '学院', width: 130, formatter: (row) => collegeOptions.find((c) => c.id === row.collegeId)?.name || '—' },
  { prop: 'majorId', label: '专业', width: 150, formatter: (row) => majorOptions.find((m) => m.id === row.majorId)?.name || '—' },
  { prop: 'name', label: '课程名称', minWidth: 160 },
  { prop: 'code', label: '课程编号', width: 120 },
  { prop: 'description', label: '简介', minWidth: 180 }
]

const filters = [
  { prop: 'collegeId', label: '学院', options: collegeOptions },
  { prop: 'majorId', label: '专业', options: majorOptions }
]
</script>
