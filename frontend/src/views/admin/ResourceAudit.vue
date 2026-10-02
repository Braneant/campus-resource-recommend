<template>
  <div class="page-container">
    <h2 class="page-title">资源审核</h2>
    <p class="page-subtitle">审核学生上传资源，通过后同步至知识图谱并对外可见</p>

    <el-tabs v-model="tab" class="app-card audit-tabs" @tab-change="switchTab">
      <el-tab-pane label="待审核" name="0" />
      <el-tab-pane label="已驳回" name="2" />
      <el-tab-pane label="全部资源" name="all" />
    </el-tabs>

    <div class="app-card table-card">
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="resourceNo" label="资源编号" width="140" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }"><el-tag :type="typeMap[row.type]?.tag">{{ typeMap[row.type]?.label }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="submitterName" label="提交人" width="120" />
        <el-table-column prop="submitTime" label="提交时间" width="170" v-if="tab !== 'all'" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.tag">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="view(row)">查看</el-button>
            <el-button v-if="row.status === 0" text type="success" size="small" :icon="Select" @click="openApprove(row)">通过</el-button>
            <el-button v-if="row.status === 0" text type="danger" size="small" :icon="Close" @click="openReject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无数据" />
      <el-pagination v-if="total > 0" layout="prev,pager,next,total" :total="total" :page-size="pageSize"
        :current-page="pageNum" @current-change="onPage" background class="center mt16" />
    </div>

    <el-dialog v-model="approveDlg" title="审核通过" width="420px">
      <p>将为资源《{{ current.title }}》建立知识图谱关联并对外发布。</p>
      <el-form label-width="90px">
        <el-form-item label="主知识点ID">
          <el-input v-model.number="kpId" placeholder="可选，默认使用上传时填写的 kpId" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDlg = false">取消</el-button>
        <el-button type="success" :loading="acting" @click="doApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDlg" title="审核驳回" width="420px">
      <el-form label-width="80px">
        <el-form-item label="驳回原因">
          <el-input v-model="reviewComment" type="textarea" :rows="3" placeholder="请填写驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDlg = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="doReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Select, Close } from '@element-plus/icons-vue'
import { auditList, resourceList, approveResource, rejectResource } from '@/api/admin'
import { ResourceTypeMap, ResourceStatusMap } from '@/utils/constants'

const router = useRouter()
const typeMap = ResourceTypeMap
const statusMap = ResourceStatusMap
const list = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const tab = ref('0')

const approveDlg = ref(false)
const rejectDlg = ref(false)
const acting = ref(false)
const current = reactive({})
const kpId = ref(null)
const reviewComment = ref('')

function load() {
  loading.value = true
  const params = { pageNum: pageNum.value, pageSize: pageSize.value }
  const req = tab.value === 'all' ? resourceList({ ...params, status: null }) : auditList({ ...params, status: Number(tab.value) })
  req.then((d) => { list.value = d.list || []; total.value = d.total || 0 })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function switchTab() { pageNum.value = 1; load() }
function onPage(p) { pageNum.value = p; load() }
function view(row) { router.push(`/student/resource/${row.id}`) }

function openApprove(row) { Object.assign(current, row); kpId.value = null; approveDlg.value = true }
function openReject(row) { Object.assign(current, row); reviewComment.value = ''; rejectDlg.value = true }

function doApprove() {
  acting.value = true
  approveResource(current.id, kpId.value != null ? { kpId: kpId.value } : {})
    .then(() => { ElMessage.success('已通过，已同步知识图谱'); approveDlg.value = false; load() })
    .catch(() => {})
    .finally(() => { acting.value = false })
}

function doReject() {
  if (!reviewComment.value.trim()) { ElMessage.warning('请填写驳回原因'); return }
  acting.value = true
  rejectResource(current.id, { reviewComment: reviewComment.value.trim() })
    .then(() => { ElMessage.success('已驳回'); rejectDlg.value = false; load() })
    .catch(() => {})
    .finally(() => { acting.value = false })
}

onMounted(load)
</script>

<style scoped>
.audit-tabs { padding: 4px 18px 0; margin-bottom: 14px; }
.table-card { padding: 8px 14px 14px; }
.center { justify-content: center; }
.mt16 { margin-top: 16px; }
</style>
