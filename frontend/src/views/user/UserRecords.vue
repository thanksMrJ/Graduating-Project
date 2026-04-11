

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/appStore'
import { uploadFile } from '@/api/http'

const router = useRouter()
const appStore = useAppStore()

onMounted(() => {
  appStore.fetchUserRecords().catch((e) => ElMessage.error(e?.message || '加载记录失败'))
})

const records = computed(() => {
  if (Array.isArray(appStore.userRecords)) return appStore.userRecords
  if (typeof appStore.userRecords === 'function') {
    const uid = appStore.currentUserId
    return uid ? appStore.userRecords(uid) : []
  }
  return []
})

/** 分析完成后从本页移除，仅在「任务分析」中展示 */
const activeRecords = computed(() =>
  (Array.isArray(records.value) ? records.value : []).filter((r) => r?.status !== '分析完成'),
)

const recordsWithDisplayId = computed(() => {
  const list = activeRecords.value
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
const saving = ref(false)

const uploadRefEditAttachment = ref(null)
const uploadRefEditReport = ref(null)
const uploadRefEditIndicators = ref(null)
const uploadRefEditImaging = ref(null)

const editTextReportFiles = ref([])
const editIndicatorFiles = ref([])
const editImagingFiles = ref([])
const editUploadedFiles = ref([])

const editUploadingCount = ref(0)
const uploadsBusy = computed(() => editUploadingCount.value > 0)

const draft = reactive({
  title: '',
  textReport: '',
  remarks: '',
  indicators: '',
  imagingDesc: '',
  result: '',
})

const editDisabled = computed(() => current.value?.status === '分析中')

function makeEditUploadHandler(filesRef, folder) {
  return async (opt) => {
    editUploadingCount.value += 1
    let progressTimer = null
    try {
      let fakePct = 0
      progressTimer = setInterval(() => {
        fakePct = Math.min(fakePct + 6, 90)
        opt.onProgress?.({ percent: fakePct })
      }, 450)

      const data = await uploadFile(opt.file, folder)

      clearInterval(progressTimer)
      progressTimer = null
      opt.onProgress?.({ percent: 100 })

      filesRef.value.push({
        name: opt.file.name,
        objectKey: data.objectKey,
        url: data.url || '',
      })
      opt.onSuccess(data)
      ElMessage.success(`已上传：${opt.file.name}`)
    } catch (e) {
      opt.onError(e)
      ElMessage.error(e?.message || '上传失败')
    } finally {
      if (progressTimer) clearInterval(progressTimer)
      editUploadingCount.value -= 1
    }
  }
}

const handleEditUploadAttachment = makeEditUploadHandler(editUploadedFiles, 'medical')
const handleEditUploadReport = makeEditUploadHandler(editTextReportFiles, 'medical/report')
const handleEditUploadIndicators = makeEditUploadHandler(editIndicatorFiles, 'medical/indicators')
const handleEditUploadImaging = makeEditUploadHandler(editImagingFiles, 'medical/imaging')

function onRemoveEditReport(file) {
  const key = file.response?.objectKey
  if (key) {
    editTextReportFiles.value = editTextReportFiles.value.filter((f) => f.objectKey !== key)
  }
}
function onRemoveEditIndicator(file) {
  const key = file.response?.objectKey
  if (key) {
    editIndicatorFiles.value = editIndicatorFiles.value.filter((f) => f.objectKey !== key)
  }
}
function onRemoveEditImaging(file) {
  const key = file.response?.objectKey
  if (key) {
    editImagingFiles.value = editImagingFiles.value.filter((f) => f.objectKey !== key)
  }
}
function onRemoveEditAttachment(file) {
  const key = file.response?.objectKey
  if (key) {
    editUploadedFiles.value = editUploadedFiles.value.filter((f) => f.objectKey !== key)
  }
}

/** 模板中列表为 ref 解包后的数组，直接 splice 即可同步 ref */
function removeExistingFile(list, index) {
  if (!Array.isArray(list)) return
  list.splice(index, 1)
}

function initDraftFromRow(row) {
  const d = row?.detail && typeof row.detail === 'object' ? row.detail : {}
  draft.title = row?.title || ''
  draft.textReport = d.textReport ?? ''
  draft.remarks = d.remarks ?? ''
  draft.indicators = d.indicators ?? ''
  draft.imagingDesc = d.imagingDesc ?? ''
  draft.result = d.result ?? ''
  editTextReportFiles.value = Array.isArray(d.textReportFiles) ? [...d.textReportFiles] : []
  editIndicatorFiles.value = Array.isArray(d.indicatorFiles) ? [...d.indicatorFiles] : []
  editImagingFiles.value = Array.isArray(d.imagingFiles) ? [...d.imagingFiles] : []
  editUploadedFiles.value = Array.isArray(d.uploadedFiles) ? [...d.uploadedFiles] : []
  nextTick(() => {
    uploadRefEditAttachment.value?.clearFiles()
    uploadRefEditReport.value?.clearFiles()
    uploadRefEditIndicators.value?.clearFiles()
    uploadRefEditImaging.value?.clearFiles()
  })
}

watch(
  () => [visible.value, current.value?.id],
  ([open]) => {
    if (open && current.value) initDraftFromRow(current.value)
  },
)

const openDetail = (row) => {
  current.value = row
  visible.value = true
}

const goAnalyze = () => router.push('/user/analyze')

const startAnalyze = (row) => {
  if (row?.status !== '待分析') return
  router.push(`/user/task-analysis/run/${encodeURIComponent(row.id)}`)
}

const statusType = (s) => {
  const v = String(s || '')
  if (v.includes('完成')) return 'success'
  if (v.includes('失败')) return 'danger'
  if (v.includes('中')) return 'warning'
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

async function saveRecord() {
  if (!current.value?.id) return
  if (editDisabled.value) {
    ElMessage.warning('分析进行中，暂不可保存修改')
    return
  }
  if (uploadsBusy.value) {
    ElMessage.warning('请等待文件上传完成后再保存')
    return
  }
  const title = String(draft.title || '').trim()
  if (!title) {
    ElMessage.warning('请填写任务标题')
    return
  }
  const hasReportText = String(draft.textReport || '').trim().length > 0
  const hasReportFiles = editTextReportFiles.value.length > 0
  if (!hasReportText && !hasReportFiles) {
    ElMessage.warning('请填写病例报告文本或保留至少一个病例报告文件')
    return
  }

  const detail = {
    textReport: draft.textReport,
    remarks: String(draft.remarks || '').trim() || undefined,
    indicators: String(draft.indicators || '').trim() || undefined,
    imagingDesc: String(draft.imagingDesc || '').trim() || undefined,
    result: draft.result != null && String(draft.result).length ? String(draft.result) : undefined,
    textReportFiles: [...editTextReportFiles.value],
    indicatorFiles:
      editIndicatorFiles.value.length > 0 ? [...editIndicatorFiles.value] : undefined,
    imagingFiles: editImagingFiles.value.length > 0 ? [...editImagingFiles.value] : undefined,
    uploadedFiles: editUploadedFiles.value.length > 0 ? [...editUploadedFiles.value] : undefined,
  }

  try {
    saving.value = true
    await appStore.updateUserRecord(current.value.id, { title, detail })
    ElMessage.success('已保存')
    visible.value = false
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page">
    <el-card class="card" shadow="always">
      <template #header>
        <div class="head">
          <div>
            <div class="title">待分析任务</div>
            <div class="sub">展示未完成分析的任务；分析完成后将移至「任务分析」</div>
          </div>

          <div class="head-actions">
            <el-button class="btn-ghost" @click="goAnalyze">新建分析任务</el-button>
          </div>
        </div>
      </template>

      <template v-if="activeRecords.length">
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
              <el-tag :type="statusType(scope.row.status)">{{ scope.row.status || '—' }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button type="primary" link @click="openDetail(scope.row)">查看详情</el-button>
            </template>
          </el-table-column>

          <el-table-column label="开始分析" width="120" fixed="right">
            <template #default="scope">
              <el-button
                v-if="scope.row.status === '待分析'"
                type="primary"
                link
                @click="startAnalyze(scope.row)"
              >
                开始分析
              </el-button>
              <span v-else-if="scope.row.status === '分析中'" class="muted">分析中…</span>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="hint">
          小提示：「查看详情」可编辑病例报告、上传文件等；「开始分析」将进入占位分析流程（约 5 秒）。
        </div>
      </template>

      <template v-else>
        <el-empty description="暂无进行中的任务（已完成任务请在「任务分析」查看）" />
        <div class="empty-actions">
          <el-button type="primary" @click="goAnalyze">去创建</el-button>
          <el-button class="btn-ghost" @click="$router.push('/user/task-analysis')">任务分析</el-button>
        </div>
      </template>
    </el-card>

    <el-dialog
      v-model="visible"
      title="编辑记录"
      width="720px"
      class="glass-dialog"
      destroy-on-close
      @closed="current = null"
    >
      <template v-if="current">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="记录ID">
            <div class="kv">
              <span>{{ current.id }}</span>
              <el-button link type="primary" @click="copyText(current.id)">复制</el-button>
            </div>
          </el-descriptions-item>

          <el-descriptions-item label="日期">{{ current.createdAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="任务标题" :span="2">
            <el-input v-model="draft.title" :disabled="editDisabled" placeholder="任务标题" />
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(current.status)">{{ current.status || '—' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="所属用户">{{ appStore.currentUser?.name || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <p v-if="editDisabled" class="warn-banner">分析进行中，暂不可修改或保存；请稍后在「任务分析」查看结果。</p>

        <el-form label-width="110px">
          <el-form-item label="病例报告">
            <div class="report-stack">
              <el-input
                v-model="draft.textReport"
                type="textarea"
                :rows="4"
                :disabled="editDisabled"
                placeholder="病例报告文本"
              />
              <div class="report-upload-label">已关联文件（可移除）</div>
              <ul v-if="editTextReportFiles.length" class="edit-file-list">
                <li v-for="(f, i) in editTextReportFiles" :key="f.objectKey || `${i}-${f.name}`">
                  <el-link v-if="f.url" :href="f.url" target="_blank" type="primary">
                    {{ f.name || f.objectKey || '文件' }}
                  </el-link>
                  <span v-else>{{ f.name || f.objectKey || '—' }}</span>
                  <el-button
                    v-if="!editDisabled"
                    type="danger"
                    link
                    size="small"
                    @click="removeExistingFile(editTextReportFiles, i)"
                  >
                    移除
                  </el-button>
                </li>
              </ul>
              <div class="report-upload-label">或上传病例报告文件（可多选）</div>
              <el-upload
                ref="uploadRefEditReport"
                class="upload-block upload-block--compact"
                drag
                multiple
                :http-request="handleEditUploadReport"
                :show-file-list="true"
                :auto-upload="true"
                :disabled="editDisabled || saving"
                @remove="onRemoveEditReport"
              >
                <div class="el-upload__text compact-drag">拖拽到此处或 <em>点击选择</em></div>
                <template #file="{ file }">
                  <div class="upload-file-custom">
                    <span class="upload-file-custom__name" :title="file.name">{{ file.name }}</span>
                    <span class="upload-file-custom__meta">
                      <template v-if="file.status === 'uploading'">
                        <el-icon class="is-loading upload-file-custom__spin"><Loading /></el-icon>
                        <span
                          v-if="file.percentage != null && file.percentage < 100"
                          class="upload-file-custom__pct"
                        >
                          {{ Math.round(file.percentage) }}%
                        </span>
                      </template>
                      <el-icon v-else-if="file.status === 'success'" class="upload-file-custom__ok">
                        <CircleCheck />
                      </el-icon>
                      <el-icon v-else-if="file.status === 'fail'" class="upload-file-custom__fail">
                        <CircleClose />
                      </el-icon>
                    </span>
                    <el-button
                      v-if="!editDisabled && !saving"
                      type="danger"
                      link
                      size="small"
                      class="upload-file-custom__rm"
                      @click.stop="uploadRefEditReport?.handleRemove(file)"
                    >
                      移除
                    </el-button>
                  </div>
                </template>
              </el-upload>
            </div>
          </el-form-item>

          <el-form-item label="结构化指标">
            <ul v-if="editIndicatorFiles.length" class="edit-file-list">
              <li v-for="(f, i) in editIndicatorFiles" :key="f.objectKey || `${i}-${f.name}`">
                <el-link v-if="f.url" :href="f.url" target="_blank" type="primary">
                  {{ f.name || f.objectKey || '文件' }}
                </el-link>
                <span v-else>{{ f.name || f.objectKey || '—' }}</span>
                <el-button
                  v-if="!editDisabled"
                  type="danger"
                  link
                  size="small"
                  @click="removeExistingFile(editIndicatorFiles, i)"
                >
                  移除
                </el-button>
              </li>
            </ul>
            <el-input
              v-else
              v-model="draft.indicators"
              type="textarea"
              :rows="2"
              :disabled="editDisabled"
              placeholder="无文件时可填写文本说明（选填）"
            />
            <el-upload
              ref="uploadRefEditIndicators"
              class="upload-block upload-block--compact upload-gap"
              drag
              multiple
              :http-request="handleEditUploadIndicators"
              :show-file-list="true"
              :auto-upload="true"
              :disabled="editDisabled || saving"
              @remove="onRemoveEditIndicator"
            >
              <div class="el-upload__text compact-drag">上传检验单等（可多选）</div>
              <template #file="{ file }">
                <div class="upload-file-custom">
                  <span class="upload-file-custom__name" :title="file.name">{{ file.name }}</span>
                  <span class="upload-file-custom__meta">
                    <template v-if="file.status === 'uploading'">
                      <el-icon class="is-loading upload-file-custom__spin"><Loading /></el-icon>
                    </template>
                    <el-icon v-else-if="file.status === 'success'" class="upload-file-custom__ok">
                      <CircleCheck />
                    </el-icon>
                    <el-icon v-else-if="file.status === 'fail'" class="upload-file-custom__fail">
                      <CircleClose />
                    </el-icon>
                  </span>
                  <el-button
                    v-if="!editDisabled && !saving"
                    type="danger"
                    link
                    size="small"
                    class="upload-file-custom__rm"
                    @click.stop="uploadRefEditIndicators?.handleRemove(file)"
                  >
                    移除
                  </el-button>
                </div>
              </template>
            </el-upload>
          </el-form-item>

          <el-form-item label="影像信息">
            <ul v-if="editImagingFiles.length" class="edit-file-list">
              <li v-for="(f, i) in editImagingFiles" :key="f.objectKey || `${i}-${f.name}`">
                <el-link v-if="f.url" :href="f.url" target="_blank" type="primary">
                  {{ f.name || f.objectKey || '文件' }}
                </el-link>
                <span v-else>{{ f.name || f.objectKey || '—' }}</span>
                <el-button
                  v-if="!editDisabled"
                  type="danger"
                  link
                  size="small"
                  @click="removeExistingFile(editImagingFiles, i)"
                >
                  移除
                </el-button>
              </li>
            </ul>
            <el-input
              v-else
              v-model="draft.imagingDesc"
              type="textarea"
              :rows="2"
              :disabled="editDisabled"
              placeholder="无文件时可填写影像说明（选填）"
            />
            <el-upload
              ref="uploadRefEditImaging"
              class="upload-block upload-block--compact upload-gap"
              drag
              multiple
              :http-request="handleEditUploadImaging"
              :show-file-list="true"
              :auto-upload="true"
              :disabled="editDisabled || saving"
              @remove="onRemoveEditImaging"
            >
              <div class="el-upload__text compact-drag">上传影像文件（可多选）</div>
              <template #file="{ file }">
                <div class="upload-file-custom">
                  <span class="upload-file-custom__name" :title="file.name">{{ file.name }}</span>
                  <span class="upload-file-custom__meta">
                    <template v-if="file.status === 'uploading'">
                      <el-icon class="is-loading upload-file-custom__spin"><Loading /></el-icon>
                    </template>
                    <el-icon v-else-if="file.status === 'success'" class="upload-file-custom__ok">
                      <CircleCheck />
                    </el-icon>
                    <el-icon v-else-if="file.status === 'fail'" class="upload-file-custom__fail">
                      <CircleClose />
                    </el-icon>
                  </span>
                  <el-button
                    v-if="!editDisabled && !saving"
                    type="danger"
                    link
                    size="small"
                    class="upload-file-custom__rm"
                    @click.stop="uploadRefEditImaging?.handleRemove(file)"
                  >
                    移除
                  </el-button>
                </div>
              </template>
            </el-upload>
          </el-form-item>

          <el-form-item label="其它附件">
            <ul v-if="editUploadedFiles.length" class="edit-file-list">
              <li v-for="(f, i) in editUploadedFiles" :key="f.objectKey || `${i}-${f.name}`">
                <el-link v-if="f.url" :href="f.url" target="_blank" type="primary">
                  {{ f.name || f.objectKey || '文件' }}
                </el-link>
                <span v-else>{{ f.name || f.objectKey || '—' }}</span>
                <el-button
                  v-if="!editDisabled"
                  type="danger"
                  link
                  size="small"
                  @click="removeExistingFile(editUploadedFiles, i)"
                >
                  移除
                </el-button>
              </li>
            </ul>
            <el-upload
              ref="uploadRefEditAttachment"
              class="upload-block upload-block--compact upload-gap"
              drag
              multiple
              :http-request="handleEditUploadAttachment"
              :show-file-list="true"
              :auto-upload="true"
              :disabled="editDisabled || saving"
              @remove="onRemoveEditAttachment"
            >
              <div class="el-upload__text compact-drag">拖拽或点击添加附件</div>
              <template #file="{ file }">
                <div class="upload-file-custom">
                  <span class="upload-file-custom__name" :title="file.name">{{ file.name }}</span>
                  <span class="upload-file-custom__meta">
                    <template v-if="file.status === 'uploading'">
                      <el-icon class="is-loading upload-file-custom__spin"><Loading /></el-icon>
                    </template>
                    <el-icon v-else-if="file.status === 'success'" class="upload-file-custom__ok">
                      <CircleCheck />
                    </el-icon>
                    <el-icon v-else-if="file.status === 'fail'" class="upload-file-custom__fail">
                      <CircleClose />
                    </el-icon>
                  </span>
                  <el-button
                    v-if="!editDisabled && !saving"
                    type="danger"
                    link
                    size="small"
                    class="upload-file-custom__rm"
                    @click.stop="uploadRefEditAttachment?.handleRemove(file)"
                  >
                    移除
                  </el-button>
                </div>
              </template>
            </el-upload>
          </el-form-item>

          <el-form-item label="备注">
            <el-input
              v-model="draft.remarks"
              type="textarea"
              :rows="2"
              :disabled="editDisabled"
              placeholder="选填"
            />
          </el-form-item>

          <el-form-item label="分析结果">
            <el-input
              v-model="draft.result"
              type="textarea"
              :rows="2"
              readonly
              placeholder="占位：分析完成后由后端写入"
            />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="visible = false">关闭</el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="editDisabled || uploadsBusy"
          @click="saveRecord"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.edit-file-list {
  margin: 0 0 8px;
  padding-left: 1.25rem;
  color: rgba(226, 232, 240, 0.9);
  font-size: 13px;
}
.edit-file-list li {
  margin: 4px 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.muted {
  font-size: 13px;
  color: rgba(226, 232, 240, 0.55);
}

.warn-banner {
  margin: 0 0 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(250, 204, 21, 0.12);
  border: 1px solid rgba(250, 204, 21, 0.35);
  color: rgba(254, 243, 199, 0.95);
  font-size: 13px;
}

.report-stack {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.report-upload-label {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.7);
}

.upload-block {
  width: 100%;
}

.upload-block--compact :deep(.el-upload-dragger) {
  padding: 16px 12px;
  min-height: auto;
}

.upload-gap {
  margin-top: 8px;
}

.compact-drag {
  font-size: 13px;
}

.upload-file-custom {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 28px;
}

.upload-file-custom__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: rgba(226, 232, 240, 0.92);
}

.upload-file-custom__meta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.upload-file-custom__spin {
  font-size: 18px;
  color: var(--el-color-primary);
}

.upload-file-custom__pct {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.75);
  min-width: 2.5rem;
}

.upload-file-custom__ok {
  font-size: 18px;
  color: var(--el-color-success);
}

.upload-file-custom__fail {
  font-size: 18px;
  color: var(--el-color-danger);
}

.upload-file-custom__rm {
  flex-shrink: 0;
}

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

:deep(.el-upload-list__item) {
  display: flex;
  align-items: center;
  margin-top: 8px;
}
</style>
