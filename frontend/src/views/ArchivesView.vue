<template>
  <section class="panel table-panel">
    <div class="panel-heading"><h3 class="panel-title">设备档案</h3></div>
    <div class="panel-body">
      <div class="toolbar">
        <el-button type="primary" :icon="Refresh" @click="load" />
        <el-button type="success" :icon="Plus">添加</el-button>
        <el-button type="warning" :icon="Upload">导入</el-button>
      </div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="model" label="设备型号" min-width="130" />
        <el-table-column prop="name" label="设备名称" min-width="150" />
        <el-table-column prop="amount" label="设备数量" width="100" />
        <el-table-column prop="region" label="所在区域" min-width="130" />
        <el-table-column prop="supplier" label="供应商" min-width="150" />
        <el-table-column prop="responsibleName" label="负责人" width="110" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default><el-button size="small" type="primary">编辑</el-button><el-button size="small" type="danger">删除</el-button></template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Plus, Refresh, Upload } from '@element-plus/icons-vue'
import { getData, type PageResult } from '../api/client'

const rows = ref<any[]>([])

async function load() {
  const data = await getData<PageResult<any>>('/equipment/archives')
  rows.value = data.records
}

onMounted(load)
</script>
