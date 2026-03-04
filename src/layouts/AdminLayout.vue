<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => {
  if (route.path.startsWith('/admin/records')) return '/admin/records'
  return '/admin/users'
})

const logout = () => {
  localStorage.removeItem('admin_token')
  router.push('/admin/login')
}
</script>

<template>
  <el-container class="admin">
    <el-header class="header">
      <div class="header-left">
        <span class="header-title">系统管理</span>
      </div>
      <div class="header-right">
        <el-button class="btn-ghost" @click="logout">退出登录</el-button>
      </div>
    </el-header>

    <el-container>
      <el-aside class="aside" width="220px">
        <el-menu :default-active="activeMenu" router class="menu">
          <el-menu-item index="/admin/users">用户管理</el-menu-item>
          <el-menu-item index="/admin/records">病历分析记录</el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin {
  min-height: 100vh;
  background: radial-gradient(1200px 600px at 20% 10%, rgba(37, 99, 235, 0.22), transparent 60%),
    radial-gradient(1000px 500px at 80% 20%, rgba(6, 182, 212, 0.18), transparent 55%),
    linear-gradient(135deg, #0b1220, #0f172a 40%, #0b1220);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(10px);
}

.header-title {
  font-size: 18px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.92);
  letter-spacing: 0.5px;
}

.aside {
  border-right: 1px solid rgba(148, 163, 184, 0.18);
  /* 背景稍微亮一点，让菜单下方不那么暗 */
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.08),
    rgba(255, 255, 255, 0.06) 40%,
    rgba(255, 255, 255, 0.05)
  );
  backdrop-filter: blur(10px);
}

.menu {
  border-right: none;
  background: transparent;
}

.main {
  padding: 18px;
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(148, 163, 184, 0.25) !important;
  color: rgba(255, 255, 255, 0.9) !important;
}

/* ========== Left Sidebar (brighter + clearer) ========== */

/* Aside background: slightly brighter than before */
.aside {
  border-right: 1px solid rgba(148, 163, 184, 0.18);
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.12),
    rgba(255, 255, 255, 0.09) 40%,
    rgba(255, 255, 255, 0.07)
  );
  backdrop-filter: blur(10px);
}

/* Element Plus menu container */
:deep(.el-menu) {
  background: transparent !important;
  border-right: none !important;
  padding: 12px 10px;
}

/* Menu item */
:deep(.el-menu-item) {
  position: relative;
  height: 54px;
  line-height: 54px;
  margin: 8px 8px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.6px;
  color: rgba(255, 255, 255, 0.94) !important;
  background: rgba(255, 255, 255, 0.06) !important;
  transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

/* Hover */
:deep(.el-menu-item:hover) {
  background: rgba(59, 130, 246, 0.22) !important;
  transform: translateX(4px);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.16);
  color: #ffffff !important;
}

/* Active */
:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #2563eb, #06b6d4) !important;
  color: #ffffff !important;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.25);
}

/* Active left indicator bar */
:deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 12px;
  height: 30px;
  width: 4px;
  border-radius: 4px;
  background: linear-gradient(180deg, #60a5fa, #22d3ee);
}
</style>