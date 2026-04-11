<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/appStore'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

onMounted(async () => {
  try {
    await appStore.fetchMe()
    await appStore.fetchUserRecords()
  } catch {
    /* fetchMe 内会清理无效 token */
  }
})

const activeMenu = computed(() => {
  if (route.path.startsWith('/user/analyze')) return '/user/analyze'
  if (route.path.startsWith('/user/task-analysis')) return '/user/task-analysis'
  if (route.path.startsWith('/user/records')) return '/user/records'
  return '/user/profile'
})

const userName = computed(() => appStore.currentUser?.name || '用户')

const logout = () => {
  appStore.logoutUser?.() // 兼容你旧版 store（没有也不报错）
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<template>
  <el-container class="user">
    <el-header class="header">
      <div class="header-left">
        <span class="header-title">用户主页</span>
        <span class="header-sub">欢迎你，{{ userName }}</span>
      </div>
      <div class="header-right">
        <el-button class="btn-ghost" @click="logout">退出登录</el-button>
      </div>
    </el-header>

    <el-container>
      <el-aside class="aside" width="240px">
        <el-menu :default-active="activeMenu" router class="menu">
          <el-menu-item index="/user/profile">个人信息</el-menu-item>
          <el-menu-item index="/user/analyze">创建任务</el-menu-item>
          <el-menu-item index="/user/records">待分析任务</el-menu-item>
          <el-menu-item index="/user/task-analysis">任务分析</el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.user {
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

.header-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.header-title {
  font-size: 18px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.92);
  letter-spacing: 0.5px;
}

.header-sub {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.78);
}

.aside {
  border-right: 1px solid rgba(148, 163, 184, 0.18);
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.10),
    rgba(255, 255, 255, 0.07) 30%,
    rgba(255, 255, 255, 0.05)
  );
  backdrop-filter: blur(10px);
}

.menu {
  border-right: none;
  background: transparent;
  padding-top: 10px;
}

.main {
  padding: 18px;
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(148, 163, 184, 0.25) !important;
  color: rgba(255, 255, 255, 0.9) !important;
}

/* 左侧菜单整体 */
:deep(.el-menu) {
  background: rgba(255, 255, 255, 0.06) !important;
  border-right: none;
}

/* 菜单项 */
:deep(.el-menu-item) {
  position: relative;
  height: 52px;
  line-height: 52px;
  margin: 6px 10px;
  border-radius: 10px;
  font-size: 15px;
  letter-spacing: 0.5px;
  color: rgba(255, 255, 255, 0.92) !important;
  transition: all 0.25s ease;
}

/* hover */
:deep(.el-menu-item:hover) {
  background: rgba(59, 130, 246, 0.18) !important;
  transform: translateX(4px);
  color: #fff !important;
}

/* active */
:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #2563eb, #06b6d4) !important;
  color: #ffffff !important;
  font-weight: 700;
}

/* active 左侧指示条 */
:deep(.el-menu-item.is-active)::before {
  content: "";
  position: absolute;
  left: -10px;
  top: 11px;
  height: 30px;
  width: 4px;
  border-radius: 3px;
  background: linear-gradient(180deg, #2563eb, #06b6d4);
}
</style>