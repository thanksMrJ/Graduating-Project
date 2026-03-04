<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/appStore'

const router = useRouter()
const appStore = useAppStore()

// 用 store 新增的 isLoggedIn / requireCurrentUser，避免出现“已登录但页面拿到空对象”
const isLoggedIn = computed(() => {
  // 兼容旧 store：没有 isLoggedIn 时退化成 currentUser 判断
  return typeof appStore.isLoggedIn === 'boolean' ? appStore.isLoggedIn : !!appStore.currentUser
})

const user = computed(() => {
  try {
    // 新 store：强制拿当前用户
    if (typeof appStore.requireCurrentUser === 'function') return appStore.requireCurrentUser()
    return appStore.currentUser
  } catch {
    return null
  }
})

const goAnalyze = () => router.push('/user/analyze')
const goRecords = () => router.push('/user/records')

const quickCopy = async (text) => {
  try {
    await navigator.clipboard.writeText(String(text ?? ''))
    ElMessage.success('已复制')
  } catch {
    ElMessage.info('复制失败（可能未授权）')
  }
}
</script>

<template>
  <div class="page">
    <el-card class="card" shadow="always">
      <template #header>
        <div class="head">
          <div>
            <div class="title">个人信息</div>
            <div class="sub">查看你的基本信息与账户状态</div>
          </div>

          <div class="head-actions">
            <el-button class="btn-ghost" @click="goAnalyze">开始病历分析</el-button>
            <el-button type="primary" @click="goRecords">查看记录</el-button>
          </div>
        </div>
      </template>

      <template v-if="isLoggedIn && user">
        <el-descriptions :column="2" border class="desc">
          <el-descriptions-item label="用户ID">
            <div class="kv">
              <span>{{ user?.id }}</span>
              <el-button link type="primary" @click="quickCopy(user?.id)">复制</el-button>
            </div>
          </el-descriptions-item>

          <el-descriptions-item label="姓名">
            <div class="kv">
              <span>{{ user?.name }}</span>
              <el-button link type="primary" @click="quickCopy(user?.name)">复制</el-button>
            </div>
          </el-descriptions-item>

          <el-descriptions-item label="性别">{{ user?.gender || '—' }}</el-descriptions-item>
          <el-descriptions-item label="年龄">{{ user?.age || '—' }}</el-descriptions-item>

          <el-descriptions-item label="联系方式">
            {{ user?.phone || '—' }}
          </el-descriptions-item>

          <el-descriptions-item label="账户状态">
            <el-tag type="success">已登录</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <el-alert
          title="提示"
          type="info"
          show-icon
          :closable="false"
          description="当前为前端演示版：个人信息与病历记录暂存于本地 localStorage；后续会接入后端与数据库。"
        />
      </template>

      <template v-else>
        <el-empty description="未检测到登录用户，请先登录" />
        <div class="empty-actions">
          <el-button type="primary" @click="$router.push('/user/login')">去登录</el-button>
          <el-button class="btn-ghost" @click="$router.push('/')">返回主页</el-button>
        </div>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  padding: 8px 4px;
}

.card {
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(10px);
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.title {
  font-size: 18px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.92);
}

.sub {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.75);
}

.head-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.desc {
  margin-top: 4px;
}

.kv {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.empty-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 10px;
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(148, 163, 184, 0.25) !important;
  color: rgba(255, 255, 255, 0.9) !important;
}

/* Descriptions：把默认的浅色单元格背景改成玻璃深色，避免内容看不见 */
:deep(.el-descriptions__body) {
  background: transparent !important;
}

:deep(.el-descriptions__table) {
  border-color: rgba(59, 130, 246, 0.45) !important;
}

:deep(.el-descriptions__cell) {
  border-color: rgba(59, 130, 246, 0.45) !important;
}

:deep(.el-descriptions__label) {
  background: rgba(255, 255, 255, 0.06) !important;
  color: rgba(226, 232, 240, 0.92) !important;
  font-weight: 600;
}

:deep(.el-descriptions__content) {
  background: rgba(255, 255, 255, 0.04) !important;
  color: rgba(255, 255, 255, 0.92) !important;
}

:deep(.el-descriptions__label.is-bordered-label) {
  background: rgba(255, 255, 255, 0.06) !important;
}

:deep(.el-descriptions__content.is-bordered-content) {
  background: rgba(255, 255, 255, 0.04) !important;
}

/* 复制按钮在表格里更像“链接” */
:deep(.el-button.is-link) {
  font-weight: 600;
}
</style>