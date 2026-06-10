<template>
  <router-view v-if="$route.path === '/login'" />
  <div v-else class="skin-blue app-shell">
    <aside class="main-sidebar">
      <a class="logo">
        <span class="logo-mini">WOS</span>
        <span class="logo-lg">设备工单系统</span>
      </a>
      <section class="sidebar">
        <div class="user-panel hidden-xs">
          <div class="pull-left image avatar">{{ initials }}</div>
          <div class="pull-left info">
            <p>{{ auth.user?.displayName }}</p>
            <span><i class="online-dot"></i> Online</span>
          </div>
        </div>
        <div class="sidebar-form">
          <el-input size="small" placeholder="Search menu" :prefix-icon="Search" />
        </div>
        <ul class="sidebar-menu">
          <li v-for="menu in auth.menus" :key="menu.path" :class="{ active: $route.path === menu.path }">
            <router-link :to="menu.path">
              <component :is="menu.icon" class="menu-icon" />
              <span>{{ menu.title }}</span>
            </router-link>
          </li>
        </ul>
      </section>
    </aside>
    <main class="content-wrapper">
      <header class="main-header">
        <nav class="navbar">
          <button class="sidebar-toggle" type="button"><el-icon><Fold /></el-icon></button>
          <div class="navbar-title">{{ routeTitle }}</div>
          <div class="navbar-user">
            <span>{{ auth.user?.username }}</span>
            <el-button size="small" @click="logout">退出</el-button>
          </div>
        </nav>
      </header>
      <section class="content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const initials = computed(() => auth.user?.displayName?.slice(0, 1) || 'A')
const routeTitle = computed(() => auth.menus.find((item) => item.path === route.path)?.title || '工作台')

function logout() {
  auth.logout()
  router.push('/login')
}
</script>
