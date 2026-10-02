<template>
  <GenericCrud
    title="学院"
    subtitle="维护学院基础数据，变更可同步至知识图谱"
    :api="collegeApi"
    :columns="columns"
    :table-columns="tableColumns"
    add-text="新增学院"
  />
</template>

<script setup>
import GenericCrud from '@/components/GenericCrud.vue'
import { collegeApi } from '@/api/admin'

const columns = [
  { prop: 'name', label: '学院名称', type: 'text', required: true, placeholder: '如 计算机学院' },
  { prop: 'code', label: '编码', type: 'text', placeholder: '如 CS' },
  { prop: 'description', label: '简介', type: 'textarea' },
  { prop: 'sort', label: '排序', type: 'number' }
]

const tableColumns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'name', label: '学院名称', minWidth: 160 },
  { prop: 'code', label: '编码', width: 120 },
  { prop: 'description', label: '简介', minWidth: 220 },
  { prop: 'sort', label: '排序', width: 90 }
]
</script>
