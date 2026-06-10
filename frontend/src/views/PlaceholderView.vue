<template>
  <section class="panel">
    <div class="panel-heading"><h3 class="panel-title">{{ title }}</h3></div>
    <div class="panel-body">
      <el-empty description="该模块将在接入旧库权限与配置后完善" />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{ title: string }>()
</script>
