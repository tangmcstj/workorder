<template>
  <section class="panel table-panel">
    <div class="panel-heading">
      <h3 class="panel-title">维修工单</h3>
      <el-tabs v-model="type" class="fast-tabs" @tab-change="load">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待接单" name="pending" />
        <el-tab-pane label="维修中" name="registered" />
        <el-tab-pane label="已完成" name="finish" />
      </el-tabs>
    </div>
    <div class="panel-body">
      <div class="toolbar">
        <el-button type="primary" :icon="Refresh" @click="load" />
        <el-button type="info" :icon="Warning">故障原因</el-button>
        <el-button type="warning" :icon="Bell">提醒人员</el-button>
      </div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="repairCode" label="工单编号" width="130" />
        <el-table-column prop="equipmentCode" label="设备编号" width="130" />
        <el-table-column prop="archiveName" label="设备名称" min-width="150" />
        <el-table-column prop="archiveModel" label="设备型号" min-width="120" />
        <el-table-column prop="content" label="报修登记" min-width="220" />
        <el-table-column prop="registerUser" label="报修人员" width="110" />
        <el-table-column label="维修状态" width="110">
          <template #default="{ row }"><el-tag :type="repairTag(row.status)">{{ row.statusText }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'pending'" size="small" type="primary" @click="receive(row.id)">接单</el-button>
            <el-button v-if="row.status === 'registered'" size="small" type="success" @click="finish(row.id)">登记</el-button>
            <el-button size="small">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Refresh, Warning } from '@element-plus/icons-vue'
import { getData, postData, type PageResult } from '../api/client'

const rows = ref<any[]>([])
const type = ref('')

async function load() {
  const data = await getData<PageResult<any>>('/repairs', { type: type.value })
  rows.value = data.records
}

async function receive(id: number) {
  await postData(`/repairs/${id}/receive`)
  ElMessage.success('操作完成')
  load()
}

async function finish(id: number) {
  await postData(`/repairs/${id}/finish`, { repairContent: '维修登记完成', repairStatus: 'repaired', failureCauseId: 1 })
  ElMessage.success('操作完成')
  load()
}

function repairTag(status: string) {
  if (status === 'pending') return 'warning'
  if (status === 'registered') return 'danger'
  if (status === 'repaired') return 'success'
  return 'info'
}

onMounted(load)
</script>

<style scoped>
.fast-tabs {
  margin-top: 8px;
}
</style>
