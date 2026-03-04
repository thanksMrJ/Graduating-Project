

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/appStore'

const router = useRouter()
const appStore = useAppStore()

// 兼容两种 store：
// - userRecords 是 computed 数组
// - 或提供 userRecords(userId) 这种 getter

const records = computed(() => {
  if (Array.isArray(appStore.userRecords)) return appStore.userRecords
  // 如果是函数 getter：userRecords(userId)
  if (typeof appStore.userRecords === 'function') {
    const uid = appStore.currentUserId
    return uid ? appStore.userRecords(uid) : []
  }
  return []
})

// 给每条记录生成与管理员端一致的“记录编号”：用户ID-第N次分析
// 说明：按 records 当前顺序（通常是最新在前）从 1 开始编号
const recordsWithDisplayId = computed(() => {
  const list = Array.isArray(records.value) ? records.value : []
  const counter = new Map()

  return list.map((r) => {
    const uid = String(r?.userId ?? appStore.currentUserId ?? '')
    const next = (counter.get(uid) || 0) + 1
    counter.set(uid, next)

    return {
      ...r,
      displayId: uid ? `${uid}-${next}` : String(r?.id ?? ''),
    }
  })
})

const visible = ref(false)
const current = ref(null)

const openDetail = (row) => {
  current.value = row
  visible.value = true
}

const goAnalyze = () => router.push('/user/analyze')

const statusType = (s) => {
  const v = String(s || '')
  if (v.includes('完成')) return 'success'
  if (v.includes('失败')) return 'danger'
  if (v.includes('分析') || v.includes('进行')) return 'warning'
  return 'info'
}

const copyText = async (text) => {
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
            <div class="title">病历分析记录</div>
            <div class="sub">展示你创建的所有分析任务（当前为前端演示数据）</div>
          </div>

          <div class="head-actions">
            <el-button class="btn-ghost" @click="goAnalyze">新建分析任务</el-button>
          </div>
        </div>
      </template>

      <template v-if="records.length">
        <el-table :data="recordsWithDisplayId" class="table" border>
          <el-table-column label="记录编号" width="110" align="center">
            <template #default="scope">
              {{ scope.row.displayId || scope.row.id }}
            </template>
          </el-table-column>

          <el-table-column prop="createdAt" label="日期" width="130" />
          <el-table-column prop="title" label="任务标题" min-width="220" show-overflow-tooltip />

          <el-table-column label="状态" width="120">
            <template #default="scope">
              <el-tag :type="statusType(scope.row.status)">{{ scope.row.status || '—' }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="140" fixed="right">
            <template #default="scope">
              <el-button type="primary" link @click="openDetail(scope.row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="hint">
          小提示：点击“查看详情”可展开查看文本报告、指标、影像信息等占位字段。
        </div>
      </template>

      <template v-else>
        <el-empty description="暂无记录，先创建一个分析任务吧" />
        <div class="empty-actions">
          <el-button type="primary" @click="goAnalyze">去创建</el-button>
          <el-button class="btn-ghost" @click="$router.push('/user/profile')">返回个人信息</el-button>
        </div>
      </template>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="visible" title="记录详情" width="720px" class="glass-dialog">
      <template v-if="current">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="记录ID">
            <div class="kv">
              <span>{{ current.id }}</span>
              <el-button link type="primary" @click="copyText(current.id)">复制</el-button>
            </div>
          </el-descriptions-item>

          <el-descriptions-item label="日期">{{ current.createdAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="任务标题" :span="2">{{ current.title || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(current.status)">{{ current.status || '—' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="所属用户">{{ appStore.currentUser?.name || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <el-form label-width="110px">
          <el-form-item label="病例报告">
            <el-input
              :model-value="current.detail?.textReport || current.detail || '（无）'"
              type="textarea"
              :rows="4"
              readonly
            />
          </el-form-item>

          <el-form-item label="结构化指标">
            <el-input
              :model-value="current.detail?.indicators || '（无）'"
              type="textarea"
              :rows="3"
              readonly
            />
          </el-form-item>

          <el-form-item label="影像信息">
            <el-input
              :model-value="current.detail?.imagingDesc || '（无）'"
              type="textarea"
              :rows="3"
              readonly
            />
          </el-form-item>

          <el-form-item label="分析结果">
            <el-input
              :model-value="current.detail?.result || '（占位）等待后端返回'"
              type="textarea"
              :rows="3"
              readonly
            />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="visible = false">关闭</el-button>
      </template>
    </el-dialog>
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

.table {
  border-radius: 12px;
  overflow: hidden;
}

.hint {
  margin-top: 10px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.75);
}

.empty-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 10px;
}

.kv {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(148, 163, 184, 0.25) !important;
  color: rgba(255, 255, 255, 0.9) !important;
}

/* 弹窗玻璃风 */
:deep(.glass-dialog .el-dialog) {
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(12px);
}
:deep(.glass-dialog .el-dialog__title) {
  color: rgba(255, 255, 255, 0.92);
}

:deep(.el-form-item__label) {
  color: rgba(226, 232, 240, 0.9) !important;
}

:deep(.el-descriptions__label) {
  color: rgba(226, 232, 240, 0.85) !important;
}

:deep(.el-descriptions__content) {
  color: rgba(255, 255, 255, 0.9) !important;
}

/* 详情弹窗里表单内容加深（Element Plus 输入框） */
:deep(.glass-dialog .el-input__inner),
:deep(.glass-dialog .el-textarea__inner) {
  color: rgba(17, 24, 39, 0.95) !important;
}

:deep(.glass-dialog .el-textarea__inner) {
  background: rgba(255, 255, 255, 0.92) !important;
}

:deep(.glass-dialog .el-input__inner) {
  background: rgba(255, 255, 255, 0.92) !important;
}

:deep(.glass-dialog .el-form-item__label) {
  color: rgba(30, 41, 59, 0.92) !important;
}

:deep(.glass-dialog .el-descriptions__label) {
  color: rgba(30, 41, 59, 0.9) !important;
}

:deep(.glass-dialog .el-descriptions__content) {
  color: rgba(17, 24, 39, 0.95) !important;
}
</style>