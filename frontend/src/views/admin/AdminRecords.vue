<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/http'

const records = ref([])

const loadRecords = async () => {
  records.value = await adminApi('/api/v1/admin/records', { method: 'GET' })
}

onMounted(() => {
  loadRecords().catch((e) => ElMessage.error(e?.message || '加载记录失败'))
})

// 搜索关键字
const keyword = ref('')

// 将 records 统一成带 userName + displayTaskId 的展示数据
const recordsWithUser = computed(() => {
  const seqMap = new Map()
  const sortedForSeq = [...records.value].sort((a, b) => {
    const da = String(a.createdAt || '')
    const db = String(b.createdAt || '')
    if (da < db) return -1
    if (da > db) return 1
    return String(a.id).localeCompare(String(b.id))
  })

  const counter = new Map()
  for (const r of sortedForSeq) {
    const uid = String(r.userId ?? '')
    const next = (counter.get(uid) || 0) + 1
    counter.set(uid, next)
    seqMap.set(String(r.id), next)
  }

  return records.value.map((r) => {
    const seq = seqMap.get(String(r.id)) || 0
    return {
      ...r,
      userName: r.userName || '（未知用户）',
      displayTaskId: `${String(r.userId)}-${seq || 1}`,
    }
  })
})

// 搜索过滤
const filteredRecords = computed(() => {
  const k = keyword.value.trim()
  if (!k) return recordsWithUser.value

  return recordsWithUser.value.filter((r) => {
    return (
      String(r.displayTaskId || '').includes(k) ||
      String(r.id).includes(k) ||
      String(r.userId).includes(k) ||
      String(r.userName || '').includes(k) ||
      String(r.createdAt || '').includes(k) ||
      String(r.title || '').includes(k) ||
      String(r.status || '').includes(k)
    )
  })
})

// 详情弹窗
const detailVisible = ref(false)

const detail = reactive({
  id: '',
  displayTaskId: '',
  userId: '',
  userName: '',
  createdAt: '',
  title: '',
  status: '',
  // 以下字段先做占位/兼容 user 端 detail 结构
  reportText: '',
  indicators: '',
  imaging: '',
  explanation: '',
  result: '',
  remarks: '',
})

const openDetail = (row) => {
  detailVisible.value = true

  detail.id = row.id
  detail.displayTaskId = row.displayTaskId
  detail.userId = row.userId
  detail.userName = row.userName
  detail.createdAt = row.createdAt
  detail.title = row.title
  detail.status = row.status

  // 兼容 UserAnalyze：textReport + textReportFiles、indicatorFiles、imagingFiles、uploadedFiles、remarks
  const d = row.detail

  const linesTextReportFiles = Array.isArray(d?.textReportFiles)
    ? d.textReportFiles.map((f) => `${f?.name || ''} (${f?.objectKey || ''})`).join('\n')
    : ''
  const baseReport =
    d?.textReport || d?.reportText || (typeof d === 'string' ? d : '') || '（无文本）'
  detail.reportText = linesTextReportFiles ? `${baseReport}\n\n【病例报告文件】\n${linesTextReportFiles}` : baseReport

  const linesInd = Array.isArray(d?.indicatorFiles)
    ? d.indicatorFiles.map((f) => `${f?.name || ''} (${f?.objectKey || ''})`).join('\n')
    : ''
  detail.indicators = linesInd || d?.indicators || '（无）'

  const linesImg = Array.isArray(d?.imagingFiles)
    ? d.imagingFiles.map((f) => `${f?.name || ''} (${f?.objectKey || ''})`).join('\n')
    : ''
  detail.imaging = linesImg || d?.imagingDesc || d?.imaging || '（无）'

  const parts = []
  if (Array.isArray(d?.uploadedFiles) && d.uploadedFiles.length) {
    parts.push('【附件】\n' + JSON.stringify(d.uploadedFiles, null, 2))
  }
  if (d?.explanation) parts.push(String(d.explanation))
  detail.explanation = parts.length ? parts.join('\n\n') : '（无）'
  detail.result = d?.result || '（占位）等待模型接口返回'
  detail.remarks = d?.remarks || ''
}

const resetSearch = () => {
  keyword.value = ''
}

const deleteRecord = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该条病历分析记录？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await adminApi(`/api/v1/admin/records/${row.id}`, { method: 'DELETE' })
    ElMessage.success('已删除')
    await loadRecords()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

const statusType = (s) => {
  const v = String(s || '')
  if (v.includes('完成')) return 'success'
  if (v.includes('失败')) return 'danger'
  if (v.includes('分析') || v.includes('进行')) return 'warning'
  return 'info'
}
</script>

<template>
  <el-card class="card" shadow="always">
    <template #header>
      <div class="header">
        <div class="title">
          病历分析记录
          <el-tag type="info">共 {{ filteredRecords.length }} 条</el-tag>
        </div>

        <div class="search">
          <el-input
            v-model="keyword"
            placeholder="搜索 任务编号(用户ID-次数) / 用户ID / 姓名 / 时间 / 标题 / 状态"
            clearable
          />
          <el-button @click="resetSearch">重置</el-button>
        </div>
      </div>
    </template>

    <el-table
      :data="filteredRecords"
      border
      style="width: 100%"
      empty-text="暂无记录"
      :header-cell-style="{
        background: 'linear-gradient(90deg,#2563eb,#06b6d4)',
        color: '#fff',
        fontWeight: 700,
      }"
    >
      <!-- ✅ 这里显示用户ID-次数 -->
      <el-table-column prop="displayTaskId" label="任务编号" width="120" align="center" />

      <el-table-column prop="userId" label="用户ID" width="100" align="center" />
      <el-table-column prop="userName" label="姓名" width="120" />
      <el-table-column prop="createdAt" label="创建时间" width="140" />

      <el-table-column prop="title" label="任务标题" min-width="220" show-overflow-tooltip />

      <el-table-column label="状态" width="120" align="center">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)">{{ scope.row.status || '—' }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="200" align="center">
        <template #default="scope">
          <el-button type="primary" link @click="openDetail(scope.row)">查看详情</el-button>
          <el-button type="danger" link @click="deleteRecord(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="hint">说明：任务编号规则 = 用户ID-该用户第N次分析（按创建时间从早到晚计数）。</div>
  </el-card>

  <!-- 查看详情弹窗 -->
  <el-dialog v-model="detailVisible" title="记录详情" width="720px" class="glass-dialog">
    <el-form label-width="90px">
      <el-form-item label="任务编号">
        <el-input v-model="detail.displayTaskId" disabled />
      </el-form-item>

      <el-form-item label="用户">
        <el-input :value="`${detail.userName}（ID:${detail.userId}）`" disabled />
      </el-form-item>

      <el-form-item label="创建时间">
        <el-input v-model="detail.createdAt" disabled />
      </el-form-item>

      <el-form-item label="任务标题">
        <el-input v-model="detail.title" disabled />
      </el-form-item>

      <el-form-item label="状态">
        <el-input v-model="detail.status" disabled />
      </el-form-item>

      <el-form-item label="病例报告">
        <el-input v-model="detail.reportText" type="textarea" :rows="3" disabled />
      </el-form-item>

      <el-form-item label="指标摘要">
        <el-input v-model="detail.indicators" type="textarea" :rows="2" disabled />
      </el-form-item>

      <el-form-item label="影像描述">
        <el-input v-model="detail.imaging" type="textarea" :rows="2" disabled />
      </el-form-item>

      <el-form-item label="解释信息">
        <el-input v-model="detail.explanation" type="textarea" :rows="3" disabled />
      </el-form-item>

      <el-form-item v-if="detail.remarks" label="备注">
        <el-input v-model="detail.remarks" type="textarea" :rows="2" disabled />
      </el-form-item>

      <el-form-item label="分析结果">
        <el-input v-model="detail.result" type="textarea" :rows="3" disabled />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.card {
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(10px);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.title {
  font-size: 18px;
  font-weight: 800;
  display: flex;
  gap: 10px;
  align-items: center;
  color: rgba(255, 255, 255, 0.92);
}

.search {
  display: flex;
  gap: 10px;
  width: 360px;
  align-items: center;
}

.hint {
  margin-top: 12px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.75);
}

:deep(.el-tag) {
  background-color: #dbeafe !important;
  color: #1e3a8a !important;
  border-color: #93c5fd !important;
}

:deep(.el-table td) {
  background: #e0f2fe;
  color: #111827;
}

:deep(.el-table__row:hover > td) {
  background: rgba(6, 182, 212, 0.12) !important;
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
:deep(.glass-dialog .el-form-item__label) {
  color: rgba(226, 232, 240, 0.9) !important;
}
</style>