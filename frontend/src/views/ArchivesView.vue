<template>
  <section class="panel table-panel">
    <div class="panel-heading">
      <h3 class="panel-title">设备档案</h3>
      <div class="panel-tools">
        <el-input v-model="keyword" size="small" placeholder="型号/名称/区域" clearable :prefix-icon="Search" @keyup.enter="load" />
        <el-switch v-model="recycle" size="small" active-text="回收站" @change="load" />
        <el-button type="primary" :icon="Refresh" size="small" @click="load">刷新</el-button>
        <el-button v-if="!recycle" type="primary" :icon="Plus" size="small" @click="openForm()">新增</el-button>
        <el-button v-if="!recycle" :icon="Document" size="small" @click="downloadTemplate">模板</el-button>
        <el-button v-if="!recycle" type="warning" :icon="Upload" size="small" @click="chooseImport">导入</el-button>
        <el-button v-if="!recycle" type="success" :icon="Download" size="small" @click="exportCsv">导出</el-button>
        <el-button v-if="!recycle" type="success" :icon="Tickets" size="small" @click="exportTags">标签</el-button>
        <input ref="fileInput" type="file" accept=".csv,text/csv" class="hidden-input" @change="importCsv" />
      </div>
    </div>
    <div class="panel-body">
      <el-table :data="rows" border stripe height="610" v-loading="loading || importing">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="model" label="设备型号" width="130" />
        <el-table-column prop="name" label="设备名称" width="150" />
        <el-table-column prop="parameter" label="设备参数" min-width="180" show-overflow-tooltip />
        <el-table-column prop="supplier" label="供应商" width="150" />
        <el-table-column prop="purchase_time" label="采购时间" width="160" />
        <el-table-column prop="amount" label="数量" width="80" />
        <el-table-column prop="region" label="所在区域" width="130" />
        <el-table-column label="负责人" width="160">
          <template #default="{ row }">{{ row.responsible }}<span v-if="row.responsible_mobile">，{{ row.responsible_mobile }}</span></template>
        </el-table-column>
        <el-table-column label="文档" width="90">
          <template #default="{ row }">
            <el-button v-if="row.document" link type="primary" @click="openDocument(row.document)">查看</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column v-if="!recycle" label="记录" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="go('/equipment', { archiveId: row.id })">设备</el-button>
            <el-button link type="primary" @click="go('/repairs', { archiveId: row.id })">维修</el-button>
            <el-button link type="primary" @click="openRecords('inspection', row.id)">巡检</el-button>
            <el-button link type="primary" @click="openRecords('maintenance', row.id)">保养</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <template v-if="!recycle">
              <el-button link type="primary" :icon="Edit" @click="openForm(row)">编辑</el-button>
              <el-button link type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
            </template>
            <template v-else>
              <el-button link type="success" @click="restore(row)">恢复</el-button>
              <el-button link type="danger" @click="destroy(row)">销毁</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="page" v-model:page-size="size" background layout="total, sizes, prev, pager, next" :total="total" @current-change="load" @size-change="load" />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑设备档案' : '新增设备档案'" width="720px">
      <el-form label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="设备型号"><el-input v-model="form.model" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="设备名称"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="供应商ID"><el-input-number v-model="form.supplier_id" :min="0" controls-position="right" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="数量"><el-input-number v-model="form.amount" :min="0" controls-position="right" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="所在区域"><el-input v-model="form.region" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="负责人ID"><el-input-number v-model="form.responsible_user_id" :min="0" controls-position="right" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="设备参数"><el-input v-model="form.parameter" type="textarea" :rows="3" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="文档路径"><el-input v-model="form.document" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recordsVisible" :title="recordTitle" width="860px">
      <el-table :data="recordRows" border stripe height="420">
        <el-table-column prop="equipment_code" label="设备编号" width="130" />
        <el-table-column prop="archive_name" label="设备名称" width="150" />
        <el-table-column prop="name" label="记录名称" min-width="180" />
        <el-table-column prop="add_user" label="记录人员" width="120" />
        <el-table-column prop="created_at" label="记录时间" width="160" />
        <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <el-dialog v-model="resultVisible" title="导入结果" width="520px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="总行数">{{ importResult.total }}</el-descriptions-item>
        <el-descriptions-item label="新增">{{ importResult.inserted }}</el-descriptions-item>
        <el-descriptions-item label="更新">{{ importResult.updated }}</el-descriptions-item>
        <el-descriptions-item label="跳过">{{ importResult.skipped }}</el-descriptions-item>
      </el-descriptions>
      <el-alert v-if="importResult.errors.length" class="import-errors" type="warning" :closable="false">
        <ul><li v-for="error in importResult.errors" :key="error">{{ error }}</li></ul>
      </el-alert>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Document, Download, Edit, Plus, Refresh, Search, Tickets, Upload } from '@element-plus/icons-vue'
import { client, deleteData, getData, postData, putData, type PageResult } from '../api/client'

const router = useRouter()
const rows = ref<any[]>([])
const recordRows = ref<any[]>([])
const fileInput = ref<HTMLInputElement>()
const loading = ref(false)
const importing = ref(false)
const saving = ref(false)
const resultVisible = ref(false)
const formVisible = ref(false)
const recordsVisible = ref(false)
const recordType = ref('')
const keyword = ref('')
const recycle = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const form = reactive<any>({})
const importResult = ref({ total: 0, inserted: 0, updated: 0, skipped: 0, errors: [] as string[] })
const recordTitle = computed(() => recordType.value === 'maintenance' ? '保养记录明细' : '巡检记录明细')

async function load() {
  loading.value = true
  try {
    const data = await getData<PageResult<any>>('/admin/legacy/archives', { page: page.value, size: size.value, keyword: keyword.value, recycle: recycle.value })
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openForm(row?: any) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, row || { model: '', name: '', amount: 0, supplier_id: 0, responsible_user_id: 0, status: 'normal' })
  formVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.id) await putData(`/admin/legacy/archives/${form.id}`, form)
    else await postData('/admin/legacy/archives', form)
    ElMessage.success('已保存')
    formVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该档案？', '提示', { type: 'warning' })
  await deleteData(`/admin/legacy/archives/${row.id}`)
  ElMessage.success('已删除')
  await load()
}

async function restore(row: any) {
  await postData(`/admin/legacy/archives/${row.id}/restore`)
  ElMessage.success('已恢复')
  await load()
}

async function destroy(row: any) {
  await ElMessageBox.confirm('确认永久销毁该档案？', '提示', { type: 'error' })
  await deleteData(`/admin/legacy/archives/${row.id}/destroy`)
  ElMessage.success('已销毁')
  await load()
}

function go(path: string, query: Record<string, string | number>) {
  router.push({ path, query })
}

async function openRecords(type: string, archiveId: number) {
  recordType.value = type
  const data = await getData<PageResult<any>>('/admin/legacy/records', { type, archiveId, page: 1, size: 100 })
  recordRows.value = data.records
  recordsVisible.value = true
}

function openDocument(path: string) {
  window.open(path, '_blank')
}

function downloadTemplate() {
  window.location.href = '/api/admin/import/archives-template.csv'
}

function exportCsv() {
  window.location.href = '/api/admin/export/archives.csv'
}

function exportTags() {
  window.location.href = '/api/admin/export/archive-tags.csv'
}

function chooseImport() {
  fileInput.value?.click()
}

async function importCsv(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  importing.value = true
  try {
    const response = await client.post('/admin/import/archives', formData)
    const data = response.data.data
    importResult.value = data
    resultVisible.value = true
    ElMessage.success(`导入完成：新增 ${data.inserted}，更新 ${data.updated}，跳过 ${data.skipped}`)
    input.value = ''
    await load()
  } finally {
    importing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.hidden-input { display: none; }
.import-errors { margin-top: 14px; }
.import-errors ul { margin: 0; padding-left: 18px; }
</style>
