<template>
  <section class="panel">
    <div class="panel-heading">
      <h3 class="panel-title">基础资料</h3>
      <div class="panel-tools">
        <el-input v-model="keyword" size="small" placeholder="搜索" clearable :prefix-icon="Search" @keyup.enter="load" />
        <el-switch v-model="recycle" size="small" active-text="回收站" @change="load" />
        <el-button size="small" :icon="Refresh" @click="load">刷新</el-button>
        <el-button v-if="current.resource" size="small" type="primary" :icon="Plus" @click="openForm()">新增</el-button>
      </div>
    </div>
    <div class="panel-body">
      <el-tabs v-model="active" @tab-change="load">
        <el-tab-pane v-for="tab in tabs" :key="tab.name" :label="tab.label" :name="tab.name" />
      </el-tabs>

      <el-table :data="rows" border stripe height="560" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column v-for="column in current.columns" :key="column.prop" :prop="column.prop" :label="column.label" :min-width="column.width || 130" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="column.prop === 'status'" :type="row.status === 'normal' ? 'success' : 'info'" effect="plain">{{ row.status }}</el-tag>
            <el-tag v-else-if="column.prop === 'openid'" :type="row.openid ? 'success' : 'info'" effect="plain">{{ row.openid ? '已绑定' : '未绑定' }}</el-tag>
            <span v-else>{{ row[column.prop] }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button v-if="current.resource && !recycle" link type="primary" :icon="Edit" @click="openForm(row)">编辑</el-button>
            <el-button v-if="active === 'staff' && row.openid && !recycle" link type="warning" @click="unbind(row)">解绑</el-button>
            <el-button v-if="current.resource && !recycle" link type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
            <el-button v-if="current.resource && recycle" link type="success" @click="restore(row)">恢复</el-button>
            <el-button v-if="current.resource && recycle" link type="danger" @click="destroy(row)">销毁</el-button>
            <el-button v-if="active === 'plans'" link type="primary" @click="loadPlanFields(row)">检查项</el-button>
            <el-button v-if="active === 'plans'" link type="warning" @click="stopPlan(row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination v-model:current-page="page" v-model:page-size="size" background layout="total, sizes, prev, pager, next" :total="total" @current-change="load" @size-change="load" />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑' : '新增'" width="560px">
      <el-form label-width="110px">
        <el-form-item v-for="field in current.fields" :key="field.prop" :label="field.label">
          <el-input v-if="field.type === 'textarea'" v-model="form[field.prop]" type="textarea" :rows="3" />
          <el-select v-else-if="field.type === 'status'" v-model="form[field.prop]">
            <el-option label="normal" value="normal" />
            <el-option label="hidden" value="hidden" />
          </el-select>
          <el-switch v-else-if="field.type === 'switch'" v-model="form[field.prop]" :active-value="1" :inactive-value="0" />
          <el-input-number v-else-if="field.type === 'number'" v-model="form[field.prop]" :min="0" controls-position="right" />
          <el-input v-else v-model="form[field.prop]" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fieldsVisible" title="计划检查项" width="720px">
      <el-table :data="planFields" border stripe height="420">
        <el-table-column prop="label" label="名称" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="required" label="必填" width="90" />
        <el-table-column prop="options" label="选项" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { deleteData, getData, postData, putData, type PageResult } from '../api/client'

type Row = Record<string, any>
type Field = { prop: string; label: string; type?: 'text' | 'textarea' | 'status' | 'switch' | 'number' }
type Tab = { name: string; label: string; resource?: string; columns: { prop: string; label: string; width?: number }[]; fields: Field[] }

const tabs: Tab[] = [
  { name: 'departments', label: '部门', resource: 'departments', columns: [{ prop: 'name', label: '部门名称' }, { prop: 'equipment_manage', label: '设备管理' }, { prop: 'status', label: '状态' }], fields: [{ prop: 'name', label: '部门名称' }, { prop: 'equipment_manage', label: '设备管理', type: 'switch' }, { prop: 'status', label: '状态', type: 'status' }] },
  { name: 'staff', label: '员工', resource: 'staff', columns: [{ prop: 'nickname', label: '姓名' }, { prop: 'department', label: '部门' }, { prop: 'workno', label: '工号' }, { prop: 'position', label: '岗位' }, { prop: 'openid', label: '微信绑定' }, { prop: 'status', label: '状态' }], fields: [{ prop: 'user_id', label: '用户ID', type: 'number' }, { prop: 'department_id', label: '部门ID', type: 'number' }, { prop: 'workno', label: '工号' }, { prop: 'position', label: '岗位' }, { prop: 'openid', label: 'OpenID' }, { prop: 'status', label: '状态', type: 'status' }] },
  { name: 'suppliers', label: '供应商', resource: 'suppliers', columns: [{ prop: 'supplier_code', label: '编号' }, { prop: 'name', label: '名称' }, { prop: 'contact', label: '联系人' }, { prop: 'contact_mobile', label: '电话' }, { prop: 'relationship', label: '合作关系' }, { prop: 'status', label: '状态' }], fields: [{ prop: 'supplier_code', label: '编号' }, { prop: 'name', label: '名称' }, { prop: 'relationship', label: '合作关系' }, { prop: 'bank', label: '开户行' }, { prop: 'bank_account', label: '银行账号' }, { prop: 'contact', label: '联系人' }, { prop: 'contact_mobile', label: '电话' }, { prop: 'remark', label: '备注', type: 'textarea' }, { prop: 'status', label: '状态', type: 'status' }] },
  { name: 'failure-causes', label: '故障原因', resource: 'failure-causes', columns: [{ prop: 'name', label: '原因' }, { prop: 'status', label: '状态' }], fields: [{ prop: 'name', label: '原因' }, { prop: 'status', label: '状态', type: 'status' }] },
  { name: 'reminder-users', label: '提醒人员', resource: 'reminder-users', columns: [{ prop: 'nickname', label: '员工' }, { prop: 'department', label: '部门' }, { prop: 'type', label: '提醒类型' }, { prop: 'status', label: '状态' }], fields: [{ prop: 'staff_id', label: '员工ID', type: 'number' }, { prop: 'type', label: '提醒类型' }, { prop: 'status', label: '状态', type: 'status' }] },
  { name: 'plans', label: '设备计划', columns: [{ prop: 'coding', label: '编号' }, { prop: 'name', label: '计划名称' }, { prop: 'type', label: '类型' }, { prop: 'periodicity', label: '周期' }, { prop: 'equipment_count', label: '设备数' }, { prop: 'pending_count', label: '待处理' }, { prop: 'status', label: '状态' }], fields: [] },
  { name: 'records', label: '设备记录', columns: [{ prop: 'equipment_code', label: '设备编号' }, { prop: 'archive_name', label: '设备名称' }, { prop: 'name', label: '记录名称' }, { prop: 'type', label: '类型' }, { prop: 'add_user', label: '操作人' }, { prop: 'status', label: '状态' }], fields: [] }
]

const route = useRoute()
const pathTabs: Record<string, string> = {
  '/departments': 'departments',
  '/staff': 'staff',
  '/suppliers': 'suppliers',
  '/failure-causes': 'failure-causes',
  '/reminder-users': 'reminder-users',
  '/records': 'records'
}
const active = ref(String(route.query.tab || pathTabs[route.path] || 'departments'))
const keyword = ref('')
const recycle = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const rows = ref<Row[]>([])
const formVisible = ref(false)
const fieldsVisible = ref(false)
const form = reactive<Row>({})
const planFields = ref<Row[]>([])
const current = computed(() => tabs.find((tab) => tab.name === active.value) || tabs[0])

async function load() {
  loading.value = true
  try {
    const tab = current.value
    const url = tab.name === 'plans' ? '/admin/legacy/plans' : tab.name === 'records' ? '/admin/legacy/records' : `/admin/legacy/${tab.resource}`
    const data = await getData<PageResult<Row>>(url, { page: page.value, size: size.value, keyword: keyword.value, recycle: recycle.value })
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openForm(row?: Row) {
  Object.keys(form).forEach((key) => delete form[key])
  for (const field of current.value.fields) form[field.prop] = row?.[field.prop] ?? (field.type === 'status' ? 'normal' : field.type === 'number' || field.type === 'switch' ? 0 : '')
  if (row?.id) form.id = row.id
  formVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const resource = current.value.resource
    if (!resource) return
    if (form.id) await putData(`/admin/legacy/${resource}/${form.id}`, form)
    else await postData(`/admin/legacy/${resource}`, form)
    formVisible.value = false
    ElMessage.success('已保存')
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Row) {
  await ElMessageBox.confirm('确认删除到回收站？', '提示', { type: 'warning' })
  await deleteData(`/admin/legacy/${current.value.resource}/${row.id}`)
  ElMessage.success('已删除')
  await load()
}

async function restore(row: Row) {
  await postData(`/admin/legacy/${current.value.resource}/${row.id}/restore`)
  ElMessage.success('已恢复')
  await load()
}

async function destroy(row: Row) {
  await ElMessageBox.confirm('确认永久销毁？', '提示', { type: 'error' })
  await deleteData(`/admin/legacy/${current.value.resource}/${row.id}/destroy`)
  ElMessage.success('已销毁')
  await load()
}

async function unbind(row: Row) {
  await postData(`/admin/legacy/staff/${row.id}/unbind`)
  ElMessage.success('已解绑')
  await load()
}

async function stopPlan(row: Row) {
  await ElMessageBox.confirm('确认停用该计划？', '提示', { type: 'warning' })
  await postData(`/admin/legacy/plans/${row.id}/stop`)
  ElMessage.success('已停用')
  await load()
}

async function loadPlanFields(row: Row) {
  planFields.value = await getData<Row[]>(`/admin/legacy/plans/${row.id}/fields`)
  fieldsVisible.value = true
}

onMounted(load)
watch(() => route.fullPath, () => {
  active.value = String(route.query.tab || pathTabs[route.path] || active.value)
  load()
})
</script>
