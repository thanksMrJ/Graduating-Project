<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/appStore'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const loading = ref(true)

onMounted(async () => {
  const id = route.params.id
  if (!id) {
    router.replace('/user/records')
    return
  }
  try {
    await appStore.runPlaceholderAnalyze(id)
    ElMessage.success('分析已完成')
    await appStore.fetchUserRecords()
    router.replace('/user/task-analysis')
  } catch (e) {
    ElMessage.error(e?.message || '分析失败')
    await appStore.fetchUserRecords()
    router.replace('/user/records')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <el-card class="card" shadow="always">
      <div class="center">
        <el-icon class="spin" :size="40"><Loading /></el-icon>
        <p class="title">正在进行病历文本推理</p>
        <p class="sub">已配置千问时将调用模型，可能需要数十秒；未配置时仍为演示占位（约 5 秒）。请勿关闭页面。</p>
      </div>
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
  min-height: 280px;
}
.center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}
.spin {
  color: var(--el-color-primary);
  animation: rot 1s linear infinite;
}
@keyframes rot {
  to {
    transform: rotate(360deg);
  }
}
.title {
  margin-top: 16px;
  font-size: 18px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.92);
}
.sub {
  margin-top: 8px;
  font-size: 13px;
  color: rgba(226, 232, 240, 0.75);
}
</style>
