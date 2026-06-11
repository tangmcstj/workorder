<template>
  <section class="panel table-panel">
    <div class="panel-heading">
      <h3 class="panel-title">设备列表</h3>
      <div class="panel-tools">
        <el-input v-model="keyword" size="small" placeholder="编号/二维码/档案" clearable :prefix-icon="Search" @keyup.enter="load" />
        <el-select v-model="status" size="small" clearable placeholder="运行状态" style="width: 130px" @change="load">
          <el-option label="正常运行" value="normal" />
          <el-option label="停机待修" value="repairing" />
          <el-option label="报废停用" value="scrapped" />
        </el-select>
        <el-switch v-model="recycle" size="small" active-text="回收站" @change="load" />
        <el-button type="primary" :icon="Refresh" size="small" @click="load">刷新</el-button>
        <el-button v-if="!recycle" type="primary" :icon="Plus" size="small" @click="openForm()">新增</el-button>
      </div>
    </div>
    <div class="panel-body">
      <el-table :data="rows" border stripe height="610" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="equipment_code" label="设备编号" width="130" />
        <el-table-column prop="archive_model" label="设备型号" width="130" />
        <el-table-column prop="archive_name" label="设备名称" width="150" />
        <el-table-column prop="supplier" label="供应商" width="150" />
        <el-table-column prop="region" label="区域" width="120" />
        <el-table-column label="负责人" width="160">
          <template #default="{ row }">{{ row.responsible }}<span v-if="row.responsible_mobile">，{{ row.responsible_mobile }}</span></template>
        </el-table-column>
        <el-table-column label="二维码标签" width="110">
          <template #default="{ row }"><el-button link type="primary" :icon="Tickets" @click="showQrcode(row)">查看</el-button></template>
        </el-table-column>
        <el-table-column label="运行状态" width="110">
          <template #default="{ row }"><el-tag :type="tagType(row.work_status)" effect="plain">{{ workStatusText(row.work_status) }}</el-tag></template>
        </el-table-column>
        <el-table-column v-if="!recycle" label="记录" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goRepairs(row)">维修</el-button>
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑设备' : '新增设备'" width="560px">
      <el-form label-width="100px">
        <el-form-item label="档案ID"><el-input-number v-model="form.archive_id" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="二维码编码"><el-input v-model="form.coding" /></el-form-item>
        <el-form-item label="设备编号"><el-input v-model="form.equipment_code" /></el-form-item>
        <el-form-item label="运行状态">
          <el-select v-model="form.work_status">
            <el-option label="正常运行" value="normal" />
            <el-option label="停机待修" value="repairing" />
            <el-option label="报废停用" value="scrapped" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status"><el-option label="normal" value="normal" /><el-option label="hidden" value="hidden" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="qrcodeVisible" title="二维码标签" width="360px">
      <div class="qrcode-box">
        <div class="qrcode-card">{{ qrcodeRow.coding }}</div>
        <div class="qrcode-code">{{ qrcodeRow.equipment_code }}</div>
        <el-button type="primary" :icon="Download" @click="downloadQrcode">下载标签数据</el-button>
      </div>
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
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, Edit, Plus, Refresh, Search, Tickets } from '@element-plus/icons-vue'
import { deleteData, getData, postData, putData, type PageResult } from '../api/client'

const route = useRoute()
const router = useRouter()
const rows = ref<any[]>([])
const recordRows = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const qrcodeVisible = ref(false)
const recordsVisible = ref(false)
const qrcodeRow = ref<any>({})
const recordType = ref('')
const keyword = ref('')
const status = ref('')
const recycle = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const form = reactive<any>({})
const recordTitle = computed(() => recordType.value === 'maintenance' ? '保养记录明细' : '巡检记录明细')

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value, keyword: keyword.value, recycle: recycle.value }
    if (status.value) params.workStatus = status.value
    if (route.query.archiveId) params.archiveId = route.query.archiveId
    const data = await getData<PageResult<any>>('/admin/legacy/equipment-items', params)
    rows.value = status.value ? data.records.filter((row) => row.work_status === status.value) : data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openForm(row?: any) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, row || { archive_id: Number(route.query.archiveId || 0), coding: '', equipment_code: '', work_status: 'normal', status: 'normal' })
  formVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.id) await putData(`/admin/legacy/equipment-items/${form.id}`, form)
    else await postData('/admin/legacy/equipment-items', form)
    ElMessage.success('已保存')
    formVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该设备？', '提示', { type: 'warning' })
  await deleteData(`/admin/legacy/equipment-items/${row.id}`)
  ElMessage.success('已删除')
  await load()
}

async function restore(row: any) {
  await postData(`/admin/legacy/equipment-items/${row.id}/restore`)
  ElMessage.success('已恢复')
  await load()
}

async function destroy(row: any) {
  await ElMessageBox.confirm('确认永久销毁该设备？', '提示', { type: 'error' })
  await deleteData(`/admin/legacy/equipment-items/${row.id}/destroy`)
  ElMessage.success('已销毁')
  await load()
}

function showQrcode(row: any) {
  qrcodeRow.value = row
  qrcodeVisible.value = true
}

function downloadQrcode() {
  window.location.href = '/api/admin/export/archive-tags.csv'
}

function goRepairs(row: any) {
  router.push({ path: '/repairs', query: { equipmentId: row.id } })
}

async function openRecords(type: string, equipmentId: number) {
  recordType.value = type
  const data = await getData<PageResult<any>>('/admin/legacy/records', { type, equipmentId, page: 1, size: 100 })
  recordRows.value = data.records
  recordsVisible.value = true
}

function tagType(value: string) {
  if (value === 'normal') return 'success'
  if (value === 'repairing') return 'warning'
  return 'danger'
}

function workStatusText(value: string) {
  if (value === 'repairing') return '停机待修'
  if (value === 'scrapped') return '停机报废'
  return '正常'
}

onMounted(load)
</script>

<style scoped>
.qrcode-box {
  display: grid;
  justify-items: center;
  gap: 14px;
}
.qrcode-card {
  width: 180px;
  height: 180px;
  border: 8px solid #222;
  display: grid;
  place-items: center;
  font-weight: 700;
  word-break: break-all;
  text-align: center;
  padding: 12px;
}
.qrcode-code {
  font-size: 18px;
  font-weight: 700;
}
</style>
