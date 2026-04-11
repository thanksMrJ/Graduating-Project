

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/appStore'
import { uploadFile } from '@/api/http'

const router = useRouter()
const appStore = useAppStore()

const uploadRefAttachment = ref(null)
const uploadRefReport = ref(null)
const uploadRefIndicators = ref(null)
const uploadRefImaging = ref(null)

const submitting = ref(false)
const uploadedFiles = ref([])
const textReportFiles = ref([])
const indicatorFiles = ref([])
const imagingFiles = ref([])

const uploadingCount = ref(0)
const uploadsBusy = computed(() => uploadingCount.value > 0)

const form = reactive({
  createdAt: '',
  title: '病历分析任务',
  textReport: '',
  indicators: '',
  remarks: '',
})

function makeUploadHandler(filesRef, folder) {
  return async (opt) => {
    uploadingCount.value += 1
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
      uploadingCount.value -= 1
    }
  }
}

const handleUploadAttachment = makeUploadHandler(uploadedFiles, 'medical')
const handleUploadReport = makeUploadHandler(textReportFiles, 'medical/report')
const handleUploadIndicators = makeUploadHandler(indicatorFiles, 'medical/indicators')
const handleUploadImaging = makeUploadHandler(imagingFiles, 'medical/imaging')

function onRemoveFrom(filesRef, file) {
  const key = file.response?.objectKey
  if (key) {
    filesRef.value = filesRef.value.filter((f) => f.objectKey !== key)
  }
}

const reset = () => {
  form.createdAt = ''
  form.title = '病历分析任务'
  form.textReport = ''
  form.indicators = ''
  form.remarks = ''
  uploadedFiles.value = []
  textReportFiles.value = []
  indicatorFiles.value = []
  imagingFiles.value = []
  uploadRefAttachment.value?.clearFiles()
  uploadRefReport.value?.clearFiles()
  uploadRefIndicators.value?.clearFiles()
  uploadRefImaging.value?.clearFiles()
}

const submit = async () => {
  if (uploadsBusy.value) {
    ElMessage.warning('文件尚未上传完成，请等待全部上传结束后再创建任务')
    return
  }
  try {
    submitting.value = true

    if (!String(form.title || '').trim()) {
      ElMessage.warning('请填写任务标题')
      return
    }
    const hasReportText = String(form.textReport || '').trim().length > 0
    const hasReportFiles = textReportFiles.value.length > 0
    const hasIndicatorsText = String(form.indicators || '').trim().length > 0
    const hasIndicatorsFiles = indicatorFiles.value.length > 0
    if (!hasReportText && !hasReportFiles && !hasIndicatorsText && !hasIndicatorsFiles) {
      ElMessage.warning('请在「病例报告」或「结构化指标」中至少填写文本或上传文件')
      return
    }

    const payload = {
      createdAt: form.createdAt || undefined,
      title: form.title,
      status: '待分析',
      detail: {
        textReport: form.textReport,
        indicators: String(form.indicators || '').trim() || undefined,
        textReportFiles: textReportFiles.value.length ? [...textReportFiles.value] : undefined,
        indicatorFiles: indicatorFiles.value.length ? [...indicatorFiles.value] : undefined,
        imagingFiles: imagingFiles.value.length ? [...imagingFiles.value] : undefined,
        remarks: String(form.remarks || '').trim() || undefined,
        uploadedFiles: uploadedFiles.value.length ? [...uploadedFiles.value] : undefined,
        result: '（占位）多模态推理将由后续模型接口写入',
      },
    }

    const res = await appStore.createRecordForCurrentUser(payload)

    if (res && typeof res === 'object' && 'ok' in res) {
      if (!res.ok) {
        ElMessage.error(res.message || '创建任务失败')
        return
      }
      ElMessage.success('已创建病历分析任务')
    } else {
      ElMessage.success('已创建病历分析任务')
    }

    router.push('/user/records')
  } catch (e) {
    ElMessage.error(e?.message || '创建任务失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <el-card class="card" shadow="always">
      <template #header>
        <div class="head">
          <div>
            <div class="title">创建任务</div>
            <div class="sub">
              病例报告、结构化指标均支持<strong>文本</strong>与<strong>文件</strong>（.xls/.xlsx
              会在点击「开始分析」时自动解析并与文本合并为综合文本再送大模型）。影像与其它附件仍按文件上传。
            </div>
          </div>
          <div class="head-actions">
            <el-button class="btn-ghost" @click="reset">重置</el-button>
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="uploadsBusy"
              @click="submit"
            >
              创建分析任务
            </el-button>
          </div>
        </div>
      </template>

      <el-form label-width="110px" class="form">
        <el-form-item label="任务日期">
          <el-date-picker
            v-model="form.createdAt"
            type="date"
            placeholder="选择日期（可选）"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="任务标题">
          <el-input v-model="form.title" placeholder="例如：胸部 CT + 血常规" />
        </el-form-item>

        <el-form-item label="病例报告">
          <div class="report-stack">
            <el-input
              v-model="form.textReport"
              type="textarea"
              :rows="4"
              placeholder="可在此输入病例报告文本（可与下方文件同时使用）"
            />
            <div class="report-upload-label">或上传病例报告文件（可多选，支持 Excel 病历表）</div>
            <el-upload
              ref="uploadRefReport"
              class="upload-block upload-block--compact"
              drag
              multiple
              :http-request="handleUploadReport"
              :show-file-list="true"
              :auto-upload="true"
              :disabled="submitting"
              @remove="(f) => onRemoveFrom(textReportFiles, f)"
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
                    v-if="!submitting"
                    type="danger"
                    link
                    size="small"
                    class="upload-file-custom__rm"
                    @click.stop="uploadRefReport?.handleRemove(file)"
                  >
                    移除
                  </el-button>
                </div>
              </template>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item label="结构化指标">
          <div class="report-stack">
            <el-input
              v-model="form.indicators"
              type="textarea"
              :rows="3"
              placeholder="可在此直接输入检验/指标说明（可与下方 Excel 或文件同时使用）"
            />
            <div class="report-upload-label">或上传检验单、表格等（可多选，支持 Excel 指标表）</div>
          <el-upload
            ref="uploadRefIndicators"
            class="upload-block upload-block--compact"
            drag
            multiple
            :http-request="handleUploadIndicators"
            :show-file-list="true"
            :auto-upload="true"
            :disabled="submitting"
            @remove="(f) => onRemoveFrom(indicatorFiles, f)"
          >
            <div class="el-upload__text compact-drag">拖拽到此处或点击选择</div>
            <template #tip>
              <div class="el-upload__tip">血常规、生化、血清学指标表等；与病历侧 Excel 将按患者号归并</div>
            </template>
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
                  v-if="!submitting"
                  type="danger"
                  link
                  size="small"
                  class="upload-file-custom__rm"
                  @click.stop="uploadRefIndicators?.handleRemove(file)"
                >
                  移除
                </el-button>
              </div>
            </template>
          </el-upload>
          </div>
        </el-form-item>

        <el-form-item label="影像信息">
          <el-upload
            ref="uploadRefImaging"
            class="upload-block upload-block--compact"
            drag
            multiple
            :http-request="handleUploadImaging"
            :show-file-list="true"
            :auto-upload="true"
            :disabled="submitting"
            @remove="(f) => onRemoveFrom(imagingFiles, f)"
          >
            <div class="el-upload__text compact-drag">上传影像文件（DICOM、NIfTI 等，可多选）</div>
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
                  v-if="!submitting"
                  type="danger"
                  link
                  size="small"
                  class="upload-file-custom__rm"
                  @click.stop="uploadRefImaging?.handleRemove(file)"
                >
                  移除
                </el-button>
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="附件上传">
          <el-upload
            ref="uploadRefAttachment"
            class="upload-block"
            drag
            multiple
            :http-request="handleUploadAttachment"
            :show-file-list="true"
            :auto-upload="true"
            :disabled="submitting"
            @remove="(f) => onRemoveFrom(uploadedFiles, f)"
          >
            <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">需先登录用户账号；单文件最大由后端限制（默认 50MB）</div>
            </template>
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
                  v-if="!submitting"
                  type="danger"
                  link
                  size="small"
                  class="upload-file-custom__rm"
                  @click.stop="uploadRefAttachment?.handleRemove(file)"
                >
                  移除
                </el-button>
              </div>
            </template>
          </el-upload>
          <div v-if="uploadedFiles.length" class="uploaded-summary">
            已上传 {{ uploadedFiles.length }} 个附件，将随任务一并保存到记录详情。
          </div>
          <div v-if="uploadsBusy" class="upload-hint">文件上传中，请稍候…</div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="form.remarks"
            type="textarea"
            :rows="3"
            placeholder="选填：其它需要说明的信息"
          />
        </el-form-item>

        <div class="footer-actions">
          <el-button class="btn-ghost" @click="$router.push('/user/profile')">返回个人信息</el-button>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="uploadsBusy"
            @click="submit"
          >
            创建分析任务
          </el-button>
        </div>
      </el-form>
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

.form {
  margin-top: 6px;
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

.compact-drag {
  font-size: 13px;
}

.uploaded-summary {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.8);
}

.upload-hint {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(250, 204, 21, 0.95);
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

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(148, 163, 184, 0.25) !important;
  color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.el-form-item__label) {
  color: rgba(226, 232, 240, 0.9) !important;
}

:deep(.el-upload-list__item) {
  display: flex;
  align-items: center;
  margin-top: 8px;
}
</style>
