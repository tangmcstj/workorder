<template>
  <section class="panel table-panel">
    <div class="panel-heading"><h3 class="panel-title">设备档案</h3></div>
    <div class="panel-body">
      <div class="toolbar">
        <el-button type="primary" :icon="Refresh" @click="load" />
        <el-button :icon="Document" @click="downloadTemplate">下载模板</el-button>
        <el-button type="warning" :icon="Upload" @click="chooseImport">导入</el-button>
        <el-button type="success" :icon="Download" @click="exportCsv">导出</el-button>
        <el-button type="success" :icon="Tickets" @click="exportTags">导出标签</el-button>
        <input ref="fileInput" type="file" accept=".csv,text/csv" class="hidden-input" @change="importCsv" />
      </div>
      <el-table :data="rows" border stripe v-loading="loading || importing">
        <el-table-column prop="model" label="设备型号" min-width="130" />
        <el-table-column prop="name" label="设备名称" min-width="150" />
        <el-table-column prop="amount" label="设备数量" width="100" />
        <el-table-column prop="region" label="所在区域" min-width="130" />
        <el-table-column prop="supplier" label="供应商" min-width="150" />
        <el-table-column prop="responsibleName" label="负责人" width="110" />
      </el-table>
    </div>
    <el-dialog v-model="resultVisible" title="导入结果" width="520px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="总行数">{{ importResult.total }}</el-descriptions-item>
        <el-descriptions-item label="新增">{{ importResult.inserted }}</el-descriptions-item>
        <el-descriptions-item label="更新">{{ importResult.updated }}</el-descriptions-item>
        <el-descriptions-item label="跳过">{{ importResult.skipped }}</el-descriptions-item>
      </el-descriptions>
      <el-alert v-if="importResult.errors.length" class="import-errors" type="warning" :closable="false">
        <ul>
          <li v-for="error in importResult.errors" :key="error">{{ error }}</li>
        </ul>
      </el-alert>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Download, Refresh, Tickets, Upload } from '@element-plus/icons-vue'
import { client, getData, type PageResult } from '../api/client'

const rows = ref<any[]>([])
const fileInput = ref<HTMLInputElement>()
const loading = ref(false)
const importing = ref(false)
const resultVisible = ref(false)
const importResult = ref({ total: 0, inserted: 0, updated: 0, skipped: 0, errors: [] as string[] })

async function load() {
  loading.value = true
  try {
    const data = await getData<PageResult<any>>('/equipment/archives')
    rows.value = data.records
  } finally {
    loading.value = false
  }
}

function downloadTemplate() {
  window.location.href = '/api/admin/import/archives-template.csv'
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
  importing.value = true
  try {
    const response = await client.post('/admin/import/archives', form)
    const data = response.data.data
    importResult.value = data
    resultVisible.value = true
    ElMessage.success(`导入完成：新增 ${data.inserted}，更新 ${data.updated}，跳过 ${data.skipped}`)
    input.value = ''
    await load()
  } finally {
    importing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.hidden-input {
  display: none;
}

.import-errors {
  margin-top: 14px;
}

.import-errors ul {
  margin: 0;
  padding-left: 18px;
}
</style>
