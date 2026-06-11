<template>
  <section class="panel">
    <div class="panel-heading">
      <h3 class="panel-title">用户角色</h3>
      <el-button :icon="Refresh" size="small" @click="load">刷新</el-button>
    </div>
    <div class="panel-body">
      <div class="stats-row">
        <div class="info-box">
          <span class="info-box-text">用户</span>
          <span class="info-box-number">{{ users.length }}</span>
        </div>
        <div class="info-box">
          <span class="info-box-text">员工</span>
          <span class="info-box-number">{{ staff.length }}</span>
        </div>
        <div class="info-box">
          <span class="info-box-text">部门</span>
          <span class="info-box-number">{{ departments.length }}</span>
        </div>
        <div class="info-box">
          <span class="info-box-text">角色</span>
          <span class="info-box-number">{{ roles.length }}</span>
        </div>
        <div class="info-box">
          <span class="info-box-text">权限节点</span>
          <span class="info-box-number">{{ rules.length }}</span>
        </div>
      </div>

      <el-tabs v-model="active">
        <el-tab-pane label="管理员" name="admins">
          <el-table :data="admins" border stripe height="520" v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" width="140" />
            <el-table-column prop="nickname" label="昵称" width="150" />
            <el-table-column prop="mobile" label="手机号" width="140" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column prop="group_names" label="角色组" min-width="180" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="用户" name="users">
          <el-table :data="users" border stripe height="520" v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" width="130" />
            <el-table-column prop="nickname" label="昵称" width="150" />
            <el-table-column prop="mobile" label="手机号" width="140" />
            <el-table-column prop="department" label="部门" width="150" />
            <el-table-column prop="position" label="岗位" width="130" />
            <el-table-column prop="workno" label="工号" width="120" />
            <el-table-column label="微信绑定" width="100">
              <template #default="{ row }">
                <el-tag :type="row.openid ? 'success' : 'info'" effect="plain">{{ row.openid ? '已绑定' : '未绑定' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="员工" name="staff">
          <el-table :data="staff" border stripe height="520">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="nickname" label="姓名" width="150" />
            <el-table-column prop="department" label="部门" width="160" />
            <el-table-column prop="position" label="岗位" width="140" />
            <el-table-column prop="workno" label="工号" width="130" />
            <el-table-column label="微信绑定" width="110">
              <template #default="{ row }">
                <el-tag :type="row.openid ? 'success' : 'info'" effect="plain">{{ row.openid ? '已绑定' : '未绑定' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="部门" name="departments">
          <el-table :data="departments" border stripe height="520">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="部门名称" />
            <el-table-column label="设备管理" width="120">
              <template #default="{ row }">
                <el-tag :type="Number(row.equipment_manage) === 1 ? 'primary' : 'info'" effect="plain">
                  {{ Number(row.equipment_manage) === 1 ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="角色权限" name="roles">
          <el-table :data="roles" border stripe height="520">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="角色名称" width="180" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="rule_count" label="权限数量" width="110" />
            <el-table-column prop="rules" label="权限节点" min-width="360" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="后台菜单" name="menus">
          <el-table :data="menus" border stripe height="520" row-key="id">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="pid" label="PID" width="80" />
            <el-table-column prop="title" label="菜单名称" width="160" />
            <el-table-column prop="name" label="权限标识" min-width="220" show-overflow-tooltip />
            <el-table-column prop="icon" label="图标" width="120" />
            <el-table-column prop="ismenu" label="菜单" width="80" />
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="设备权限" name="equipmentRules">
          <el-table :data="equipmentRules" border stripe height="520">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="pid" label="PID" width="80" />
            <el-table-column prop="title" label="名称" width="160" />
            <el-table-column prop="name" label="权限标识" min-width="260" show-overflow-tooltip />
            <el-table-column prop="ismenu" label="菜单" width="80" />
            <el-table-column prop="weigh" label="权重" width="90" />
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getData } from '../api/client'

type Row = Record<string, unknown>

const active = ref('admins')
const loading = ref(false)
const admins = ref<Row[]>([])
const users = ref<Row[]>([])
const staff = ref<Row[]>([])
const departments = ref<Row[]>([])
const roles = ref<Row[]>([])
const rules = ref<Row[]>([])
const menus = ref<Row[]>([])
const equipmentRules = ref<Row[]>([])

async function load() {
  loading.value = true
  try {
    const data = await getData<{ admins: Row[]; users: Row[]; staff: Row[]; departments: Row[]; roles: Row[]; rules: Row[]; menus: Row[]; equipmentRules?: Row[] }>('/admin/users-overview')
    const access = await getData<{ equipmentRules: Row[] }>('/admin/access-overview')
    admins.value = data.admins || []
    users.value = data.users || []
    staff.value = data.staff || []
    departments.value = data.departments || []
    roles.value = data.roles || []
    rules.value = data.rules || []
    menus.value = data.menus || []
    equipmentRules.value = access.equipmentRules || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
