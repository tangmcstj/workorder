<template>
  <section class="panel">
    <div class="panel-heading">
      <h3 class="panel-title">系统配置</h3>
    </div>
    <div class="panel-body settings-page" v-loading="loading">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="微信小程序" name="wechat">
          <el-form :model="form" label-width="130px" class="settings-form">
            <el-form-item label="小程序 AppID">
              <el-input v-model="form.weappId" placeholder="请输入微信小程序 AppID" />
            </el-form-item>
            <el-form-item label="小程序 Secret">
              <el-input v-model="form.weappSecret" placeholder="留空表示不修改；保存后仅显示掩码" show-password />
              <div class="form-hint">当前：{{ currentSecret || '未配置' }}</div>
            </el-form-item>
            <el-form-item label="二维码域名">
              <el-input v-model="form.qrcodeDomain" placeholder="例如 https://wos.example.com" />
            </el-form-item>
            <el-form-item label="管理电话">
              <el-input v-model="form.managePhone" placeholder="小程序 getSystemInfo 返回的 manage_phone" />
            </el-form-item>
            <el-form-item label="自动派单">
              <el-switch v-model="form.repairAssignOneself" active-text="设备管理员报修时派给自己" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存配置</el-button>
              <el-button :icon="Refresh" @click="load">刷新</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="兼容接口" name="compat">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="小程序登录">/api/equipment/login, /api/equipment/weapplogin</el-descriptions-item>
            <el-descriptions-item label="工作台">/api/equipment/workbench</el-descriptions-item>
            <el-descriptions-item label="设备档案">/api/equipment/archives</el-descriptions-item>
            <el-descriptions-item label="设备列表">/api/equipment/list</el-descriptions-item>
            <el-descriptions-item label="设备详情">/api/equipment/info</el-descriptions-item>
            <el-descriptions-item label="维修与任务">/api/equipment/repairs, /api/equipment/submitPlanTasks</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="菜单权限" name="menus">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="旧后台菜单节点">{{ access.menus.length }}</el-descriptions-item>
            <el-descriptions-item label="旧设备权限节点">{{ access.equipmentRules.length }}</el-descriptions-item>
            <el-descriptions-item label="旧角色组">{{ access.roles.length }}</el-descriptions-item>
            <el-descriptions-item label="旧管理员">{{ access.admins.length }}</el-descriptions-item>
          </el-descriptions>
          <el-table :data="access.equipmentRules" border stripe height="360" class="settings-table">
            <el-table-column prop="title" label="名称" width="160" />
            <el-table-column prop="name" label="权限标识" min-width="260" show-overflow-tooltip />
            <el-table-column prop="ismenu" label="菜单" width="80" />
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <div class="settings-side">
        <div class="info-box">
          <span class="info-box-icon bg-light-blue-gradient"><el-icon><Connection /></el-icon></span>
          <div class="info-box-content">
            <span class="info-box-text">已绑定微信员工</span>
            <span class="info-box-number">{{ stats.staffBoundOpenidCount }}</span>
          </div>
        </div>
        <div class="info-box">
          <span class="info-box-icon bg-green"><el-icon><Bell /></el-icon></span>
          <div class="info-box-content">
            <span class="info-box-text">提醒人员</span>
            <span class="info-box-number">{{ stats.reminderUserCount }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Check, Connection, Refresh } from '@element-plus/icons-vue'
import { getData, putData } from '../api/client'

interface SystemConfig {
  managePhone: string
  weappId: string
  weappSecretMasked: string
  qrcodeDomain: string
  repairAssignOneself: boolean
  reminderUserCount: number
  staffBoundOpenidCount: number
}

const activeTab = ref('wechat')
const loading = ref(false)
const saving = ref(false)
const currentSecret = ref('')
const stats = reactive({ reminderUserCount: 0, staffBoundOpenidCount: 0 })
const access = reactive({ admins: [] as any[], roles: [] as any[], menus: [] as any[], equipmentRules: [] as any[] })
const form = reactive({
  managePhone: '',
  weappId: '',
  weappSecret: '',
  qrcodeDomain: '',
  repairAssignOneself: false
})

async function load() {
  loading.value = true
  try {
    const data = await getData<SystemConfig>('/config/system')
    form.managePhone = data.managePhone
    form.weappId = data.weappId
    form.weappSecret = ''
    form.qrcodeDomain = data.qrcodeDomain
    form.repairAssignOneself = data.repairAssignOneself
    currentSecret.value = data.weappSecretMasked
    stats.reminderUserCount = data.reminderUserCount
    stats.staffBoundOpenidCount = data.staffBoundOpenidCount
    const accessData = await getData<typeof access>('/admin/access-overview')
    access.admins = accessData.admins || []
    access.roles = accessData.roles || []
    access.menus = accessData.menus || []
    access.equipmentRules = accessData.equipmentRules || []
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    const data = await putData<SystemConfig>('/config/system', { ...form })
    form.weappSecret = ''
    currentSecret.value = data.weappSecretMasked
    stats.reminderUserCount = data.reminderUserCount
    stats.staffBoundOpenidCount = data.staffBoundOpenidCount
    ElMessage.success('配置已保存')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
