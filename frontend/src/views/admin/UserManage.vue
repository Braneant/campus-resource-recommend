<template>
  <div class="page-container">
    <h2 class="page-title">用户权限管控</h2>
    <p class="page-subtitle">管理学生用户与管理员账号、角色与状态（扩展接口 /api/admin/user）</p>

    <div class="app-card table-card">
      <TableSkeleton v-if="loading && list.length === 0" :rows="6" :columns="5" />
      <el-table v-else-if="list.length > 0" :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="角色" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'ADMIN'" type="warning" effect="dark">{{ adminRoleLabel(row) }}</el-tag>
            <el-tag v-else type="primary" effect="plain">学生</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled !== 0" @change="(v) => toggleEnable(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.role !== 'ADMIN'" text type="warning" size="small" @click="promote(row)">设为管理员</el-button>
            <el-button text type="danger" size="small" :icon="Delete" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <StateBlock
        v-else
        type="empty"
        title="暂无用户数据"
        description="用户列表由 /api/admin/user 提供，若确实为空可刷新重试"
        action-text="刷新"
        @action="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { userApi } from '@/api/admin'
import { AdminRoleMap } from '@/utils/constants'

const list = ref([])
const loading = ref(false)

function adminRoleLabel(row) {
  return AdminRoleMap[row.adminRole] || '管理员'
}

function load() {
  loading.value = true
  userApi.list({ pageNum: 1, pageSize: 50 })
    .then((d) => { list.value = Array.isArray(d) ? d : (d.list || []) })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function toggleEnable(row, val) {
  ElMessage.info('启用/停用接口待后端提供（/api/admin/user/{id}/enable）')
}

function promote(row) {
  ElMessageBox.confirm(`将用户《${row.nickname || row.username}》设为管理员？`, '提示', { type: 'warning' })
    .then(() => userApi.setRole(row.id, { role: 'ADMIN', adminRole: 2 })
      .then(() => { ElMessage.success('已设为管理员'); load() })
      .catch(() => {}))
    .catch(() => {})
}

function remove(row) {
  ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' })
    .then(() => userApi.remove(row.id).then(() => { ElMessage.success('已删除'); load() }).catch(() => {}))
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.table-card { padding: 8px 14px 14px; }
</style>
