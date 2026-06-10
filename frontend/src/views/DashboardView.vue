<template>
  <div>
    <div class="dashboard-grid">
      <div class="small-box bg-light-blue-gradient">
        <div class="inner"><h3>{{ summary.archiveCount }}</h3><p>设备档案</p></div>
        <div class="icon"><el-icon><Folder /></el-icon></div>
      </div>
      <div class="small-box bg-light-blue-gradient">
        <div class="inner"><h3>{{ summary.equipmentCount }}</h3><p>设备总数(台)</p></div>
        <div class="icon"><el-icon><List /></el-icon></div>
      </div>
      <div class="small-box bg-teal-gradient">
        <div class="inner"><h3>{{ summary.inspectionPendingCount }}</h3><p>待巡检任务</p></div>
        <div class="icon"><el-icon><Refresh /></el-icon></div>
      </div>
      <div class="small-box bg-teal-gradient">
        <div class="inner"><h3>{{ summary.maintenancePendingCount }}</h3><p>待保养任务</p></div>
        <div class="icon"><el-icon><Clock /></el-icon></div>
      </div>
    </div>

    <div class="dashboard-columns">
      <section class="panel">
        <div class="panel-heading"><h3 class="panel-title">设备运行情况</h3></div>
        <div class="panel-body info-grid">
          <div class="info-box"><span class="info-box-icon bg-green"><el-icon><TrendCharts /></el-icon></span><div class="info-box-content"><span class="info-box-text">正常运行</span><span class="info-box-number">{{ summary.equipmentNormalCount }}</span></div></div>
          <div class="info-box"><span class="info-box-icon bg-red"><el-icon><CircleClose /></el-icon></span><div class="info-box-content"><span class="info-box-text">停机待修</span><span class="info-box-number">{{ summary.equipmentRepairingCount }}</span></div></div>
          <div class="info-box"><span class="info-box-icon bg-maroon"><el-icon><Remove /></el-icon></span><div class="info-box-content"><span class="info-box-text">报废停用</span><span class="info-box-number">{{ summary.equipmentScrappedCount }}</span></div></div>
        </div>
      </section>
      <section class="panel">
        <div class="panel-heading"><h3 class="panel-title">设备维修工单</h3></div>
        <div class="panel-body info-grid">
          <div class="info-box"><span class="info-box-icon bg-maroon"><el-icon><Calendar /></el-icon></span><div class="info-box-content"><span class="info-box-text">待接单工单</span><span class="info-box-number">{{ summary.repairPendingCount }}</span></div></div>
          <div class="info-box"><span class="info-box-icon bg-maroon"><el-icon><Tools /></el-icon></span><div class="info-box-content"><span class="info-box-text">维修中工单</span><span class="info-box-number">{{ summary.repairRegisteredCount }}</span></div></div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { getData } from '../api/client'

const summary = reactive({
  archiveCount: 0,
  equipmentCount: 0,
  equipmentNormalCount: 0,
  equipmentRepairingCount: 0,
  equipmentScrappedCount: 0,
  repairPendingCount: 0,
  repairRegisteredCount: 0,
  inspectionPendingCount: 0,
  maintenancePendingCount: 0
})

onMounted(async () => {
  Object.assign(summary, await getData('/dashboard/summary'))
})
</script>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 15px;
}

.dashboard-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 15px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 15px;
}

@media (max-width: 1100px) {
  .dashboard-grid,
  .dashboard-columns,
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
