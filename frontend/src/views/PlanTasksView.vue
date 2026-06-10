<template>
  <section class="panel table-panel">
    <div class="panel-heading"><h3 class="panel-title">{{ title }}</h3></div>
    <div class="panel-body">
      <div class="toolbar">
        <el-button type="primary" :icon="Refresh" @click="load" />
      </div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="coding" label="唯一编码" width="120" />
        <el-table-column prop="equipmentCode" label="设备编号" width="130" />
        <el-table-column prop="archiveName" label="设备名称" min-width="150" />
        <el-table-column prop="taskUser" label="处理人" width="110" />
        <el-table-column prop="startTime" label="开始时间" min-width="170" />
        <el-table-column prop="dueTime" label="到期时间" min-width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === 'finish' ? 'success' : 'warning'">{{ row.statusText }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }"><el-button v-if="row.status === 'pending'" size="small" type="success" @click="submitTask(row.id)">完成</el-button></template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getData, postData, type PageResult } from '../api/client'

const props = defineProps<{ type: string; title: string }>()
const rows = ref<any[]>([])

async function load() {
  const data = await getData<PageResult<any>>('/plan-tasks', { type: props.type })
  rows.value = data.records
}

async function submitTask(id: number) {
  await postData(`/plan-tasks/${id}/submit`, { content: { work_status: '正常' } })
  ElMessage.success('操作完成')
  load()
}

watch(() => props.type, load)
onMounted(load)
</script>
