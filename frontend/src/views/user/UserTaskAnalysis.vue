<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/appStore'

const router = useRouter()
const appStore = useAppStore()

onMounted(() => {
  appStore.fetchUserRecords().catch((e) => ElMessage.error(e?.message || '加载失败'))
})

const records = computed(() =>
  Array.isArray(appStore.userRecords) ? appStore.userRecords : [],
)

const completed = computed(() => records.value.filter((r) => r?.status === '分析完成'))

const recordsWithDisplayId = computed(() => {
  const counter = new Map()
  return completed.value.map((r) => {
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

const openResult = (row) => {
  current.value = row
  visible.value = true
}

const statusType = (s) => {
  const v = String(s || '')
  if (v.includes('完成')) return 'success'
  return 'info'
}

const copyText = async (text) => {
  try {
    await navigator.clipboard.writeText(String(text ?? ''))
    ElMessage.success('已复制')
  } catch {
    ElMessage.info('复制失败')
  }
}

/** 千问返回的 JSON 会存入 detail.result，此处解析为结构化展示 */
const parsedStructuredResult = computed(() => {
  const raw = current.value?.detail?.result
  if (raw == null || typeof raw !== 'string') return null
  try {
    const o = JSON.parse(raw)
    if (o && typeof o === 'object' && ('riskLevel' in o || 'riskScore' in o)) return o
    return null
  } catch {
    return null
  }
})
</script>

<template>
  <div class="page">
    <el-card class="card" shadow="always">
      <template #header>
        <div class="head">
          <div>
            <div class="title">任务分析</div>
            <div class="sub">此处展示「分析完成」的任务；结果为占位文案或千问结构化 JSON</div>
          </div>
          <el-button class="btn-ghost" @click="router.push('/user/records')">返回待分析任务</el-button>
        </div>
      </template>

      <template v-if="recordsWithDisplayId.length">
        <el-table :data="recordsWithDisplayId" class="table" border>
          <el-table-column label="记录编号" width="110" align="center">
            <template #default="scope">
              {{ scope.row.displayId || scope.row.id }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="日期" width="130" />
          <el-table-column prop="title" label="任务标题" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <el-tag :type="statusType(scope.row.status)">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button type="primary" link @click="openResult(scope.row)">查看结果</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else description="暂无已完成的任务" />
    </el-card>

    <el-dialog v-model="visible" title="分析结果" width="720px" class="glass-dialog" destroy-on-close>
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
        <div v-if="parsedStructuredResult" class="structured-result">
          <div class="sr-row">
            <span class="sr-label">风险等级</span>
            <el-tag type="warning">{{ parsedStructuredResult.riskLevel || '—' }}</el-tag>
            <span class="sr-score" v-if="parsedStructuredResult.riskScore != null">
              评分 {{ parsedStructuredResult.riskScore }}
            </span>
          </div>
          <div v-if="parsedStructuredResult.suspectedDiseases?.length" class="sr-block">
            <div class="sr-block-title">疑似病种</div>
            <el-table :data="parsedStructuredResult.suspectedDiseases" size="small" border>
              <el-table-column prop="name" label="名称" min-width="160" />
              <el-table-column prop="probability" label="概率" width="100" />
            </el-table>
          </div>
          <div v-if="parsedStructuredResult.keyEvidences?.length" class="sr-block">
            <div class="sr-block-title">关键证据</div>
            <ul class="sr-list">
              <li v-for="(ev, i) in parsedStructuredResult.keyEvidences" :key="i">{{ ev }}</li>
            </ul>
          </div>
          <div v-if="parsedStructuredResult.reasoning" class="sr-block">
            <div class="sr-block-title">推理说明</div>
            <p class="sr-text">{{ parsedStructuredResult.reasoning }}</p>
          </div>
          <div v-if="parsedStructuredResult.suggestions?.length" class="sr-block">
            <div class="sr-block-title">建议</div>
            <ul class="sr-list">
              <li v-for="(s, i) in parsedStructuredResult.suggestions" :key="i">{{ s }}</li>
            </ul>
          </div>
          <div v-if="parsedStructuredResult.disclaimer" class="sr-disclaimer">
            {{ parsedStructuredResult.disclaimer }}
          </div>
          <el-collapse v-if="parsedStructuredResult.rawModelResponse" class="sr-raw">
            <el-collapse-item title="模型原始回复（调试）" name="raw">
              <pre class="sr-pre">{{ parsedStructuredResult.rawModelResponse }}</pre>
            </el-collapse-item>
          </el-collapse>
        </div>
        <el-form v-else label-width="96px">
          <el-form-item label="分析结果">
            <el-input
              :model-value="current.detail?.result || '—'"
              type="textarea"
              :rows="8"
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
.table {
  border-radius: 12px;
  overflow: hidden;
}
.btn-ghost {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(148, 163, 184, 0.25) !important;
  color: rgba(255, 255, 255, 0.9) !important;
}
.kv {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
:deep(.glass-dialog .el-dialog) {
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(12px);
}
:deep(.glass-dialog .el-dialog__title) {
  color: rgba(255, 255, 255, 0.92);
}
:deep(.glass-dialog .el-textarea__inner) {
  color: rgba(17, 24, 39, 0.95) !important;
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

.structured-result {
  padding: 4px 0;
  color: rgba(17, 24, 39, 0.95);
}
.sr-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.sr-label {
  font-weight: 600;
  color: rgba(30, 41, 59, 0.9);
}
.sr-score {
  font-size: 13px;
  color: rgba(71, 85, 105, 0.95);
}
.sr-block {
  margin-bottom: 14px;
}
.sr-block-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
  color: rgba(30, 41, 59, 0.92);
}
.sr-list {
  margin: 0;
  padding-left: 1.25rem;
  font-size: 13px;
  line-height: 1.55;
}
.sr-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.sr-disclaimer {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(251, 191, 36, 0.15);
  border: 1px solid rgba(251, 191, 36, 0.45);
  font-size: 12px;
  color: rgba(120, 53, 15, 0.95);
}
.sr-raw {
  margin-top: 12px;
}
.sr-pre {
  margin: 0;
  max-height: 240px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
  color: rgba(51, 65, 85, 0.95);
}
</style>
