// src/router/index.js
import { createRouter, createWebHashHistory } from 'vue-router'

import HomeView from '../views/HomeView.vue'
import TableView from '../views/Table.vue'

// Admin
import AdminLayout from '../layouts/AdminLayout.vue'
import AdminLogin from '../views/admin/AdminLogin.vue'
import AdminUsers from '../views/admin/AdminUsers.vue'
import AdminRecords from '../views/admin/AdminRecords.vue'

// User
import UserLayout from '../layouts/UserLayout.vue'
import UserLogin from '../views/user/UserLogin.vue'
import UserProfile from '../views/user/UserProfile.vue'
import UserAnalyze from '../views/user/UserAnalyze.vue'
import UserRecords from '../views/user/UserRecords.vue'
import UserTaskAnalysis from '../views/user/UserTaskAnalysis.vue'
import UserTaskAnalysisRun from '../views/user/UserTaskAnalysisRun.vue'

function hasUserToken() {
  return !!localStorage.getItem('user_token')
}

const DEFAULT_TITLE = '医疗多模态主页'

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView, meta: { title: DEFAULT_TITLE } },
    { path: '/table', name: 'table', component: TableView, meta: { title: DEFAULT_TITLE } },

    // ===== Admin =====
    { path: '/admin/login', name: 'admin-login', component: AdminLogin, meta: { title: '管理员登录' } },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { title: '管理端' },
      children: [
        { path: '', redirect: '/admin/users' },
        { path: 'users', name: 'admin-users', component: AdminUsers },
        { path: 'records', name: 'admin-records', component: AdminRecords },
      ],
    },

    // ===== User =====
    { path: '/user/login', name: 'user-login', component: UserLogin, meta: { title: '用户登录' } },
    {
      path: '/user',
      component: UserLayout,
      meta: { title: '用户端' },
      children: [
        { path: '', redirect: '/user/profile' },
        { path: 'profile', name: 'user-profile', component: UserProfile },
        { path: 'analyze', name: 'user-analyze', component: UserAnalyze, meta: { title: '创建任务' } },
        { path: 'records', name: 'user-records', component: UserRecords, meta: { title: '待分析任务' } },
        {
          path: 'task-analysis',
          name: 'user-task-analysis',
          component: UserTaskAnalysis,
          meta: { title: '任务分析' },
        },
        {
          path: 'task-analysis/run/:id',
          name: 'user-task-analysis-run',
          component: UserTaskAnalysisRun,
          meta: { title: '分析进行中' },
        },
      ],
    },

    { path: '/about', name: 'about', component: () => import('../views/AboutView.vue'), meta: { title: DEFAULT_TITLE } },
  ],
})

function applyDocumentTitle(to) {
  const hit = [...to.matched].reverse().find((r) => r.meta?.title)
  document.title = hit?.meta?.title || DEFAULT_TITLE
}

router.afterEach((to) => {
  applyDocumentTitle(to)
})

router.beforeEach((to) => {
  // ---- Admin guard ----
  if (to.path === '/admin/login') return true
  if (to.path.startsWith('/admin')) {
    const token = localStorage.getItem('admin_token')
    if (!token) return { path: '/admin/login' }
  }

  // ---- User guard ----
  if (to.path === '/user/login') return true
  if (to.path.startsWith('/user')) {
    if (!hasUserToken()) return { path: '/user/login' }
  }

  return true
})

export default router