<template>
  <section class="panel table-panel">
    <div class="panel-heading">
      <h3 class="panel-title">{{ title }}</h3>
      <div class="panel-tools">
        <el-input v-model="keyword" size="small" placeholder="计划名称" clearable :prefix-icon="Search" @keyup.enter="load" />
        <el-switch v-model="deactivated" size="small" active-text="已停用" @change="load" />
        <el-button type="primary" :icon="Refresh" size="small" @click="load">刷新</el-button>
        <el-button v-if="!deactivated" type="primary" :icon="Plus" size="small" @click="openForm()">新增计划</el-button>
        <el-button type="warning" :icon="Delete" size="small" @click="clearInvalid">清理无效任务</el-button>
      </div>
    </div>
    <div class="panel-body">
      <el-table :data="rows" border stripe height="610" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="计划名称" min-width="160" />
        <el-table-column prop="equipment_count" label="设备范围" width="110">
          <template #default="{ row }"><el-button link type="primary" @click="loadEquipment(row)">{{ row.equipment_count || 0 }} 台设备</el-button></template>
        </el-table-column>
        <el-table-column label="检查项" width="120">
          <template #default="{ row }"><el-button link type="primary" @click="loadFields(row)">查看检查项</el-button></template>
        </el-table-column>
        <el-table-column prop="first_due_time" label="首次执行" width="160" />
        <el-table-column prop="last_due_time" label="结束时间" width="160" />
        <el-table-column label="周期" width="100">
          <template #default="{ row }">{{ row.periodicity }} 天一次</template>
        </el-table-column>
        <el-table-column prop="pending_count" label="待处理" width="90" />
        <el-table-column prop="finish_count" label="已完成" width="90" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!deactivated" link type="danger" @click="stopPlan(row)">停用</el-button>
            <el-button link type="primary" @click="loadEquipment(row)">设备明细</el-button>
            <el-button link type="primary" @click="loadRecords">记录明细</el-button>
            <el-button v-if="!deactivated" link type="primary" :icon="Edit" @click="openForm(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="page" v-model:page-size="size" background layout="total, sizes, prev, pager, next" :total="total" @current-change="load" @size-change="load" />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑计划' : '新增计划'" width="620px">
      <el-form label-width="110px">
        <el-form-item label="计划编号"><el-input v-model="form.coding" /></el-form-item>
        <el-form-item label="计划名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="周期天数"><el-input-number v-model="form.periodicity" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status"><el-option label="normal" value="normal" /><el-option label="hidden" value="hidden" /></el-select></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fieldsVisible" title="检查项" width="820px">
      <el-table :data="fields" border stripe height="420">
        <el-table-column prop="label" label="检查项名称" min-width="180" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="required" label="必填" width="90" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="options" label="选项" min-width="240" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <el-dialog v-model="equipmentVisible" title="设备明细" width="820px">
      <el-table :data="equipmentRows" border stripe height="420">
        <el-table-column prop="coding" label="二维码编码" width="130" />
        <el-table-column prop="equipment_code" label="设备编号" width="130" />
        <el-table-column prop="archive_model" label="型号" width="130" />
        <el-table-column prop="archive_name" label="名称" min-width="160" />
        <el-table-column prop="work_status" label="状态" width="110" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="recordsVisible" title="记录明细" width="860px">
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
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { getData, postData, putData, type PageResult } from '../api/client'

const props = defineProps<{ type: string; title: string }>()
const rows = ref<any[]>([])
const fields = ref<any[]>([])
const equipmentRows = ref<any[]>([])
const recordRows = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const fieldsVisible = ref(false)
const equipmentVisible = ref(false)
const recordsVisible = ref(false)
const keyword = ref('')
const deactivated = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const form = reactive<any>({})

async function load() {
  loading.value = true
  try {
    const data = await getData<PageResult<any>>('/admin/legacy/plans', { type: props.type, page: page.value, size: size.value, deactivated: deactivated.value, keyword: keyword.value })
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openForm(row?: any) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, row || { coding: '', name: '', type: props.type, periodicity: 1, status: 'normal' })
  formVisible.value = true
}

async function save() {
  saving.value = true
  try {
    form.type = props.type
    if (form.id) await putData(`/admin/legacy/plans/${form.id}`, form)
    else await postData('/admin/legacy/plans', form)
    ElMessage.success('已保存')
    formVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function loadFields(row: any) {
  fields.value = await getData<any[]>(`/admin/legacy/plans/${row.id}/fields`)
  fieldsVisible.value = true
}

async function loadEquipment(row: any) {
  equipmentRows.value = await getData<any[]>(`/admin/legacy/plans/${row.id}/equipment`)
  equipmentVisible.value = true
}

async function loadRecords() {
  const data = await getData<PageResult<any>>('/admin/legacy/records', { type: props.type, page: 1, size: 100 })
  recordRows.value = data.records
  recordsVisible.value = true
}

async function stopPlan(row: any) {
  await ElMessageBox.confirm('停用后下一周期任务不再有效，确认停用？', '提示', { type: 'warning' })
  await postData(`/admin/legacy/plans/${row.id}/stop`)
  ElMessage.success('已停用')
  await load()
}

async function clearInvalid() {
  const preview = await postData<any>('/admin/legacy/plans/clear-invalid-task?dryRun=true')
  await ElMessageBox.confirm(`检测到 ${preview.invalid} 条无效任务，确认清理？`, '提示', { type: 'warning' })
  const result = await postData<any>('/admin/legacy/plans/clear-invalid-task?dryRun=false')
  ElMessage.success(`已清理 ${result.cleared} 条`)
  await load()
}

watch(() => props.type, load)
onMounted(load)
</script>
