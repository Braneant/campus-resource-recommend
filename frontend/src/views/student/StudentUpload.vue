<template>
  <div class="page-container">
    <h2 class="page-title">上传学习资源</h2>
    <p class="page-subtitle">上传后进入审核流程，管理员审核通过即同步至知识图谱并对外可见</p>

    <div class="app-card upload-card">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" label-position="right">
        <el-form-item label="资源文件" prop="file">
          <el-upload
            class="up-load"
            drag
            :auto-upload="false"
            :limit="1"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或<em>点击选择</em></div>
            <template #tip><div class="el-upload__tip">支持文档 / 课件 / 视频，单文件不超过 200MB</div></template>
          </el-upload>
        </el-form-item>

        <el-form-item label="资源标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入资源标题" maxlength="80" show-word-limit />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="资源类型" prop="type">
              <el-select v-model="form.type" placeholder="选择类型" style="width:100%">
                <el-option v-for="(v, k) in typeMap" :key="k" :label="v.label" :value="Number(k)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学院" prop="collegeId">
              <el-select v-model="form.collegeId" placeholder="选择学院" style="width:100%">
                <el-option v-for="c in collegeOptions" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="专业" prop="majorId">
              <el-select v-model="form.majorId" placeholder="选择专业" style="width:100%">
                <el-option v-for="m in majorOptions" :key="m.id" :label="m.name" :value="m.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="课程" prop="courseId">
              <el-select v-model="form.courseId" placeholder="选择课程" style="width:100%">
                <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="选择分类" style="width:100%">
                <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="主知识点" prop="kpId">
              <el-input v-model.number="form.kpId" placeholder="知识点 ID，如 1003" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author" placeholder="资源作者 / 来源" />
        </el-form-item>

        <el-form-item label="资源简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="简要描述资源内容与适用场景" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" :icon="Promotion" @click="onSubmit">提交审核</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Promotion } from '@element-plus/icons-vue'
import { uploadResource } from '@/api/resource'
import { ResourceTypeMap } from '@/utils/constants'
import { categoryOptions, collegeOptions, courseOptions } from '@/utils/options'

const typeMap = ResourceTypeMap
const formRef = ref(null)
const loading = ref(false)
const fileRaw = ref(null)

const majorOptions = [
  { id: 1, name: '计算机科学与技术' },
  { id: 2, name: '软件工程' },
  { id: 3, name: '人工智能' }
]

const form = reactive({
  title: '', type: 3, collegeId: null, majorId: null, courseId: null,
  categoryId: null, kpId: null, author: '', description: '', file: null
})

const rules = {
  file: [{ required: true, message: '请选择资源文件', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  collegeId: [{ required: true, message: '请选择学院', trigger: 'change' }],
  majorId: [{ required: true, message: '请选择专业', trigger: 'change' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  kpId: [{ required: true, message: '请输入主知识点 ID', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }]
}

function onFileChange(file) {
  form.file = file.raw
  fileRaw.value = file.raw
}
function onFileRemove() {
  form.file = null
  fileRaw.value = null
}

function reset() {
  formRef.value.resetFields()
  form.file = null
  fileRaw.value = null
}

async function onSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid || !form.file) return
    loading.value = true
    const fd = new FormData()
    fd.append('file', form.file)
    fd.append('title', form.title)
    fd.append('type', form.type)
    fd.append('collegeId', form.collegeId)
    fd.append('majorId', form.majorId)
    fd.append('courseId', form.courseId)
    fd.append('categoryId', form.categoryId)
    fd.append('kpId', form.kpId)
    fd.append('author', form.author)
    fd.append('description', form.description)
    try {
      const d = await uploadResource(fd)
      ElMessage.success(`上传成功，资源编号 ${d.resourceNo || ''}，待管理员审核`)
      reset()
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.upload-card { padding: 26px 30px; max-width: 880px; }
.up-load { width: 100%; }
</style>
