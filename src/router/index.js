// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'

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

function getCurrentUserIdFromLS() {
  try {
    const raw = localStorage.getItem('gp_app_store_v1')
    if (!raw) return ''
    const data = JSON.parse(raw)
    return typeof data.currentUserId === 'string' ? data.currentUserId : ''
  } catch {
    return ''
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/table', name: 'table', component: TableView },

    // ===== Admin =====
    { path: '/admin/login', name: 'admin-login', component: AdminLogin },
    {
      path: '/admin',
      component: AdminLayout,
      children: [
        { path: '', redirect: '/admin/users' },
        { path: 'users', name: 'admin-users', component: AdminUsers },
        { path: 'records', name: 'admin-records', component: AdminRecords },
      ],
    },

    // ===== User =====
    { path: '/user/login', name: 'user-login', component: UserLogin },
    {
      path: '/user',
      component: UserLayout,
      children: [
        { path: '', redirect: '/user/profile' },
        { path: 'profile', name: 'user-profile', component: UserProfile },
        { path: 'analyze', name: 'user-analyze', component: UserAnalyze },
        { path: 'records', name: 'user-records', component: UserRecords },
      ],
    },

    { path: '/about', name: 'about', component: () => import('../views/AboutView.vue') },
  ],
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
    const uid = getCurrentUserIdFromLS()
    if (!uid) return { path: '/user/login' }
  }

  return true
})

export default router