<template>
  <div class="page-container">
    <div class="head-row">
      <div>
        <h2 class="page-title">{{ title }}</h2>
        <p class="page-subtitle">{{ subtitle }}</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">{{ addText }}</el-button>
    </div>

    <!-- 筛选 -->
    <div v-if="filters.length" class="filter-row app-card">
      <el-form :inline="true">
        <el-form-item v-for="fl in filters" :key="fl.prop" :label="fl.label">
          <el-select v-model="filterForm[fl.prop]" clearable :placeholder="fl.label" style="width:160px" @change="load">
            <el-option v-for="o in fl.options" :key="o.id" :label="o.name" :value="o.id" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <div class="app-card table-card">
      <!-- 首次加载用骨架屏，避免"转圈遮罩"造成的布局跳动 -->
      <TableSkeleton v-if="loading && tableData.length === 0" :rows="6" :columns="tableColumns.length + 1" />
      <el-table v-else-if="tableData.length > 0" :data="tableData" border stripe v-loading="loading">
        <el-table-column v-for="col in tableColumns" :key="col.prop" :prop="col.prop" :label="col.label" :width="col.width" :min-width="col.minWidth">
          <template v-if="col.formatter" #default="{ row }">{{ col.formatter(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" :icon="Delete" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 空态：一句说明 + 一个可执行的下一步 -->
      <StateBlock
        v-else
        type="empty"
        :title="'还没有' + title + '数据'"
        description="点击下方按钮添加第一条记录"
        :action-text="addText"
        @action="openCreate"
      />
    </div>

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dialog" :title="editing ? '编辑' + title : addText" width="480px">
      <el-form :model="form" :rules="formRules" ref="f" label-width="88px">
        <el-form-item v-for="c in columns" :key="c.prop" :label="c.label" :prop="c.prop" :rules="c.required ? [{required:true,message:'请输入'+c.label,trigger:'blur'}] : []">
          <el-input v-if="c.type === 'text'" v-model="form[c.prop]" :placeholder="c.placeholder" />
          <el-input v-else-if="c.type === 'textarea'" v-model="form[c.prop]" type="textarea" :rows="3" />
          <el-input v-else-if="c.type === 'number'" v-model.number="form[c.prop]" type="number" />
          <el-select v-else-if="c.type === 'select'" v-model="form[c.prop]" style="width:100%" :placeholder="c.placeholder">
            <el-option v-for="o in c.options" :key="o.id" :label="o.name" :value="o.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  api: { type: Object, required: true },
  columns: { type: Array, required: true },
  tableColumns: { type: Array, required: true },
  filters: { type: Array, default: () => [] },
  extraListParams: { type: Object, default: () => ({}) },
  addText: { type: String, default: '新增' },
  idKey: { type: String, default: 'id' }
})

const tableData = ref([])
const loading = ref(false)
const dialog = ref(false)
const editing = ref(false)
const saving = ref(false)
const f = ref(null)

const filterForm = reactive({})
props.filters.forEach((fl) => { filterForm[fl.prop] = null })

const form = reactive({})
const formRules = {}

function load() {
  loading.value = true
  const params = { ...props.extraListParams }
  props.filters.forEach((fl) => { if (filterForm[fl.prop] != null) params[fl.prop] = filterForm[fl.prop] })
  props.api.list(params)
    .then((d) => { tableData.value = Array.isArray(d) ? d : (d.list || d.records || []) })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function resetForm() {
  Object.keys(form).forEach((k) => delete form[k])
  props.columns.forEach((c) => { form[c.prop] = c.type === 'number' ? null : '' })
}

function openCreate() {
  resetForm()
  editing.value = false
  dialog.value = true
}

function openEdit(row) {
  resetForm()
  props.columns.forEach((c) => { form[c.prop] = row[c.prop] })
  form[props.idKey] = row[props.idKey]
  editing.value = true
  dialog.value = true
}

function save() {
  f.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    const payload = { ...form }
    try {
      if (editing.value) {
        const id = form[props.idKey]
        delete payload[props.idKey]
        await props.api.update(id, payload)
        ElMessage.success('更新成功')
      } else {
        await props.api.create(payload)
        ElMessage.success('创建成功')
      }
      dialog.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

function remove(row) {
  ElMessageBox.confirm(`确认删除该${props.title}？`, '提示', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    .then(async () => {
      await props.api.remove(row[props.idKey])
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.head-row { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 14px; }
.filter-row { padding: 14px 18px; margin-bottom: 14px; }
.table-card { padding: 8px 14px 14px; }
</style>
