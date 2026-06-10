<template>
  <section class="panel table-panel">
    <div class="panel-heading"><h3 class="panel-title">设备档案</h3></div>
    <div class="panel-body">
      <div class="toolbar">
        <el-button type="primary" :icon="Refresh" @click="load" />
        <el-button type="warning" :icon="Upload" @click="chooseImport">导入</el-button>
        <el-button type="success" :icon="Download" @click="exportCsv">导出</el-button>
        <el-button type="success" :icon="Tickets" @click="exportTags">导出标签</el-button>
        <input ref="fileInput" type="file" accept=".csv,text/csv" class="hidden-input" @change="importCsv" />
      </div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="model" label="设备型号" min-width="130" />
        <el-table-column prop="name" label="设备名称" min-width="150" />
        <el-table-column prop="amount" label="设备数量" width="100" />
        <el-table-column prop="region" label="所在区域" min-width="130" />
        <el-table-column prop="supplier" label="供应商" min-width="150" />
        <el-table-column prop="responsibleName" label="负责人" width="110" />
      </el-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh, Tickets, Upload } from '@element-plus/icons-vue'
import { client, getData, type PageResult } from '../api/client'

const rows = ref<any[]>([])
const fileInput = ref<HTMLInputElement>()

async function load() {
  const data = await getData<PageResult<any>>('/equipment/archives')
  rows.value = data.records
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
  const form = new FormData()
  form.append('file', file)
  const response = await client.post('/admin/import/archives', form)
  const data = response.data.data
  ElMessage.success(`导入完成：新增 ${data.inserted}，更新 ${data.updated}，跳过 ${data.skipped}`)
  input.value = ''
  await load()
}

onMounted(load)
</script>

<style scoped>
.hidden-input {
  display: none;
}
</style>
