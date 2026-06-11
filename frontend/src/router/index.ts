import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import ArchivesView from '../views/ArchivesView.vue'
import EquipmentView from '../views/EquipmentView.vue'
import RepairsView from '../views/RepairsView.vue'
import PlanTasksView from '../views/PlanTasksView.vue'
import UsersView from '../views/UsersView.vue'
import SettingsView from '../views/SettingsView.vue'
import LegacyManageView from '../views/LegacyManageView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    { path: '/', redirect: '/dashboard' },
    { path: '/dashboard', component: DashboardView },
    { path: '/archives', component: ArchivesView },
    { path: '/equipment', component: EquipmentView },
    { path: '/repairs', component: RepairsView },
    { path: '/inspection', component: PlanTasksView, props: { type: 'inspection', title: '巡检任务' } },
    { path: '/maintenance', component: PlanTasksView, props: { type: 'maintenance', title: '保养任务' } },
    { path: '/users', component: UsersView },
    { path: '/legacy-manage', component: LegacyManageView },
    { path: '/settings', component: SettingsView }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.user) return '/login'
  if (to.path === '/login' && auth.user) return '/dashboard'
})

export default router
