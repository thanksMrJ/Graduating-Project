

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/appStore'

const router = useRouter()
const appStore = useAppStore()

const submitting = ref(false)

// 先用文本字段代替“上传文件”
const form = reactive({
  createdAt: '',
  title: '病历分析任务',
  textReport: '',
  indicators: '',
  imagingDesc: '',
})

const reset = () => {
  form.createdAt = ''
  form.title = '病历分析任务'
  form.textReport = ''
  form.indicators = ''
  form.imagingDesc = ''
}

const submit = async () => {
  try {
    submitting.value = true

    // 基本校验（入门友好）
    if (!String(form.title || '').trim()) {
      ElMessage.warning('请填写任务标题')
      return
    }
    if (!String(form.textReport || '').trim()) {
      ElMessage.warning('请填写病例报告（文本）')
      return
    }

    const payload = {
      createdAt: form.createdAt || undefined,
      title: form.title,
      status: '待分析',
      detail: {
        textReport: form.textReport,
        indicators: form.indicators,
        imagingDesc: form.imagingDesc,
        // 结果先占位
        result: '（占位）分析结果将由后端生成并返回',
      },
    }

    // 兼容两种 store 写法：
    // 1) createRecordForCurrentUser 抛错，成功返回 record
    // 2) createRecordForCurrentUser 返回 { ok, message, record }
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

    // 创建成功后跳到记录页
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
            <div class="title">病历分析</div>
            <div class="sub">当前为前端演示：先用文字字段模拟上传的报告/指标/影像信息</div>
          </div>
          <div class="head-actions">
            <el-button class="btn-ghost" @click="reset">重置</el-button>
            <el-button type="primary" :loading="submitting" @click="submit">创建分析任务</el-button>
          </div>
        </div>
      </template>

      <el-form label-width="96px" class="form">
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
          <el-input
            v-model="form.textReport"
            type="textarea"
            :rows="5"
            placeholder="请输入病例报告内容（后续可替换为文件上传）"
          />
        </el-form-item>

        <el-form-item label="结构化指标">
          <el-input
            v-model="form.indicators"
            type="textarea"
            :rows="4"
            placeholder="例如：WBC=xx, CRP=xx（后续可替换为表单/上传）"
          />
        </el-form-item>

        <el-form-item label="影像信息">
          <el-input
            v-model="form.imagingDesc"
            type="textarea"
            :rows="4"
            placeholder="例如：CT 显示……（后续可替换为影像上传）"
          />
        </el-form-item>

        <div class="footer-actions">
          <el-button class="btn-ghost" @click="$router.push('/user/profile')">返回个人信息</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">创建分析任务</el-button>
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
</style>