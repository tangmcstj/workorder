<template>
  <section class="panel table-panel">
    <div class="panel-heading">
      <h3 class="panel-title">维修工单</h3>
      <div class="panel-tools">
        <el-input v-model="keyword" size="small" placeholder="工单/设备/内容" clearable :prefix-icon="Search" @keyup.enter="load" />
        <el-select v-model="repairStatus" size="small" clearable placeholder="维修状态" style="width: 130px" @change="load">
          <el-option label="待接单" value="pending" />
          <el-option label="维修中" value="registered" />
          <el-option label="已修复" value="repaired" />
          <el-option label="已报废" value="scrapped" />
        </el-select>
        <el-switch v-model="recycle" size="small" active-text="回收站" @change="load" />
        <el-button type="primary" :icon="Refresh" size="small" @click="load">刷新</el-button>
        <el-button v-if="!recycle" type="primary" :icon="Plus" size="small" @click="openForm()">新增</el-button>
        <el-button v-if="!recycle" type="info" :icon="Warning" size="small" @click="goBase('failure-causes')">故障原因</el-button>
        <el-button v-if="!recycle" type="warning" :icon="Bell" size="small" @click="goBase('reminder-users')">提醒人员</el-button>
      </div>
    </div>
    <div class="panel-body">
      <el-table :data="rows" border stripe height="610" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="repair_code" label="工单编号" width="130" />
        <el-table-column prop="archive_model" label="设备型号" width="120" />
        <el-table-column prop="archive_name" label="设备名称" width="150" />
        <el-table-column prop="equipment_code" label="设备编号" width="130" />
        <el-table-column prop="register_user" label="报修人员" width="110" />
        <el-table-column prop="register_time" label="报修时间" width="160" />
        <el-table-column prop="assign_time" label="指派时间" width="160" />
        <el-table-column prop="failure_cause" label="故障原因" width="130" />
        <el-table-column prop="repair_user" label="维修人员" width="110" />
        <el-table-column prop="repair_time" label="维修时间" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="repairTag(row.status)" effect="plain">{{ repairText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <template v-if="!recycle">
              <el-button v-if="row.status === 'pending' || row.status === 'registered'" link type="warning" @click="openAssign(row)">指派</el-button>
              <el-button v-if="row.status === 'registered'" link type="success" @click="openRegister(row)">登记</el-button>
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑维修工单' : '新增维修工单'" width="660px">
      <el-form label-width="110px">
        <el-form-item label="档案ID"><el-input-number v-model="form.archive_id" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="设备ID"><el-input-number v-model="form.equipment_id" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="工单编号"><el-input v-model="form.repair_code" /></el-form-item>
        <el-form-item label="报修人员ID"><el-input-number v-model="form.register_user_id" :min="0" controls-position="right" /></el-form-item>
        <el-form-item label="报修内容"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="报修图片"><el-input v-model="form.register_image" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="待接单" value="pending" />
            <el-option label="维修中" value="registered" />
            <el-option label="已修复" value="repaired" />
            <el-option label="已报废" value="scrapped" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="指派维修人员" width="460px">
      <el-form label-width="100px">
        <el-form-item label="维修人员ID"><el-input-number v-model="assignForm.repair_user_id" :min="1" controls-position="right" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="assign">确认指派</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="registerVisible" title="维修登记" width="620px">
      <el-form label-width="110px">
        <el-form-item label="维修结果">
          <el-radio-group v-model="registerForm.repair_status">
            <el-radio-button label="repaired">已修复</el-radio-button>
            <el-radio-button label="scrapped">已报废</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="故障原因ID"><el-input-number v-model="registerForm.failure_cause_id" :min="0" controls-position="right" /></el-form-item>
        <el-form-item label="维修内容"><el-input v-model="registerForm.repair_content" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="维修图片"><el-input v-model="registerForm.repair_image" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="registerRepair">保存登记</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="维修详情" width="760px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单编号">{{ detail.repair_code }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ repairText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="设备">{{ detail.archive_model }} {{ detail.archive_name }}</el-descriptions-item>
        <el-descriptions-item label="设备编号">{{ detail.equipment_code }}</el-descriptions-item>
        <el-descriptions-item label="报修人员">{{ detail.register_user }}</el-descriptions-item>
        <el-descriptions-item label="维修人员">{{ detail.repair_user }}</el-descriptions-item>
        <el-descriptions-item label="故障原因">{{ detail.failure_cause || '-' }}</el-descriptions-item>
        <el-descriptions-item label="维修时间">{{ detail.repair_time || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修内容" :span="2">{{ detail.content }}</el-descriptions-item>
        <el-descriptions-item label="维修内容" :span="2">{{ detail.repair_content || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Delete, Edit, Plus, Refresh, Search, Warning } from '@element-plus/icons-vue'
import { deleteData, getData, postData, putData, type PageResult } from '../api/client'

const route = useRoute()
const router = useRouter()
const rows = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const assignVisible = ref(false)
const registerVisible = ref(false)
const detailVisible = ref(false)
const keyword = ref('')
const repairStatus = ref('')
const recycle = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const form = reactive<any>({})
const assignForm = reactive<any>({})
const registerForm = reactive<any>({})
const detail = ref<any>({})

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value, keyword: keyword.value, recycle: recycle.value, repairStatus: repairStatus.value }
    if (route.query.archiveId) params.archiveId = route.query.archiveId
    if (route.query.equipmentId) params.equipmentId = route.query.equipmentId
    const data = await getData<PageResult<any>>('/admin/legacy/repairs', params)
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openForm(row?: any) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, row || { archive_id: Number(route.query.archiveId || 0), equipment_id: Number(route.query.equipmentId || 0), repair_code: '', content: '', register_image: '', status: 'pending' })
  formVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.id) await putData(`/admin/legacy/repairs/${form.id}`, form)
    else await postData('/admin/legacy/repairs', form)
    ElMessage.success('已保存')
    formVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function openAssign(row: any) {
  Object.assign(assignForm, { id: row.id, repair_user_id: row.repair_user_id || 0 })
  assignVisible.value = true
}

async function assign() {
  saving.value = true
  try {
    await postData(`/admin/legacy/repairs/${assignForm.id}/assignment`, assignForm)
    ElMessage.success('已指派')
    assignVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function openRegister(row: any) {
  Object.assign(registerForm, { id: row.id, repair_status: 'repaired', failure_cause_id: row.failure_cause_id || 0, repair_content: row.repair_content || '', repair_image: row.repair_image || '' })
  registerVisible.value = true
}

async function registerRepair() {
  saving.value = true
  try {
    await postData(`/admin/legacy/repairs/${registerForm.id}/register`, registerForm)
    ElMessage.success('已登记')
    registerVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function openDetail(row: any) {
  detail.value = await getData(`/admin/legacy/repairs/${row.id}`)
  detailVisible.value = true
}

async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该维修工单？', '提示', { type: 'warning' })
  await deleteData(`/admin/legacy/repairs/${row.id}`)
  ElMessage.success('已删除')
  await load()
}

async function restore(row: any) {
  await postData(`/admin/legacy/repairs/${row.id}/restore`)
  ElMessage.success('已恢复')
  await load()
}

async function destroy(row: any) {
  await ElMessageBox.confirm('确认永久销毁该维修工单？', '提示', { type: 'error' })
  await deleteData(`/admin/legacy/repairs/${row.id}/destroy`)
  ElMessage.success('已销毁')
  await load()
}

function goBase(tab: string) {
  router.push({ path: '/legacy-manage', query: { tab } })
}

function repairText(status: string) {
  if (status === 'pending') return '待接单'
  if (status === 'registered') return '维修中'
  if (status === 'repaired') return '已修复'
  if (status === 'scrapped') return '已报废'
  return status || '-'
}

function repairTag(status: string) {
  if (status === 'pending') return 'warning'
  if (status === 'registered') return 'info'
  if (status === 'repaired') return 'success'
  return 'danger'
}

onMounted(load)
</script>
