<template>
  <section class="panel table-panel">
    <div class="panel-heading"><h3 class="panel-title">设备列表</h3></div>
    <div class="panel-body">
      <div class="toolbar">
        <el-button type="primary" :icon="Refresh" @click="load" />
        <el-select v-model="status" clearable placeholder="设备状态" style="width: 160px" @change="load">
          <el-option label="正常运行" value="normal" />
          <el-option label="停机待修" value="repairing" />
          <el-option label="报废停用" value="scrapped" />
        </el-select>
      </div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="coding" label="唯一编码" width="120" />
        <el-table-column prop="equipmentCode" label="设备编号" width="130" />
        <el-table-column prop="archiveModel" label="设备型号" min-width="130" />
        <el-table-column prop="archiveName" label="设备名称" min-width="150" />
        <el-table-column prop="region" label="所在区域" min-width="130" />
        <el-table-column label="设备状态" width="120">
          <template #default="{ row }"><el-tag :type="tagType(row.workStatus)">{{ row.workStatusText }}</el-tag></template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getData, type PageResult } from '../api/client'

const rows = ref<any[]>([])
const status = ref('')

async function load() {
  const data = await getData<PageResult<any>>('/equipment/items', { status: status.value })
  rows.value = data.records
}

function tagType(statusName: string) {
  if (statusName === 'normal') return 'success'
  if (statusName === 'repairing') return 'danger'
  if (statusName === 'scrapped') return 'info'
  return 'warning'
}

onMounted(load)
</script>
