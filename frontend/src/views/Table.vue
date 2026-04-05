<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 本地存储 key
const STORAGE_KEY = 'employees_rawData_v1'

// 表格数据：后续你可以替换成 axios 请求到的数据
const rawData = ref([
  { id: 1, date: '2024-03-03', name: '张三', address: '北京市朝阳区' },
  { id: 2, date: '2024-03-04', name: '李四', address: '上海市浦东新区' },
  { id: 3, date: '2024-03-05', name: '王五', address: '广州市天河区' }
])

// 让新增/删除的数据在刷新页面后依然存在（localStorage 持久化）
onMounted(() => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      const parsed = JSON.parse(saved)
      if (Array.isArray(parsed)) rawData.value = parsed
    }
  } catch {
    // 忽略解析错误
  }
})

watch(
  rawData,
  (val) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(val))
    } catch {
      // 忽略存储错误（例如隐私模式/容量限制）
    }
  },
  { deep: true }
)

// 搜索关键字（演示：按姓名/地址过滤）
const keyword = ref('')

const filteredData = computed(() => {
  const k = keyword.value.trim()
  if (!k) return rawData.value
  return rawData.value.filter(
    (row) => row.name.includes(k) || row.address.includes(k) || row.date.includes(k)
  )
})

const total = computed(() => filteredData.value.length)

// ===== 主表格分页 =====
const pageSize = ref(5)
const currentPage = ref(1)

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page) => {
  currentPage.value = page
}

// 主表格“全局序号”（分页后不从 1 重新开始）
// 第 2 页：5条/页 从 6 开始；10条/页 从 11 开始
const indexMethod = (index) => {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

// 当筛选结果变少/变多时，防止出现“空页”
watch(total, (t) => {
  const maxPage = Math.max(1, Math.ceil(t / pageSize.value))
  if (currentPage.value > maxPage) currentPage.value = maxPage
})

// ===== 主表格高度：不留空白；10条/页时最多显示5条，其余滚动 =====
const TABLE_HEADER_HEIGHT = 48
const TABLE_ROW_HEIGHT = 48
const TABLE_BORDER_EXTRA = 2

const visibleRowCount = computed(() => {
  const len = pagedData.value.length

  // 10条/页：最多只显示 5 条；如果当前页不足 5 条就收缩
  if (pageSize.value === 10) return Math.max(1, Math.min(5, len))

  // 5条/页：跟随当前页实际条数（不满 5 条就收缩）
  return Math.max(1, len)
})

const tableHeight = computed(() => {
  return TABLE_HEADER_HEIGHT + visibleRowCount.value * TABLE_ROW_HEIGHT + TABLE_BORDER_EXTRA
})

const reset = () => {
  keyword.value = ''
  currentPage.value = 1
}

// ===== 添加员工（弹窗表单） =====
const dialogVisible = ref(false)

const formRef = ref()
const form = reactive({
  date: '',
  name: '',
  address: ''
})

const rules = {
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
}

const openAdd = () => {
  dialogVisible.value = true
}

const closeAdd = () => {
  dialogVisible.value = false
  // 重置表单内容与校验
  form.date = ''
  form.name = ''
  form.address = ''
  formRef.value?.clearValidate?.()
}

const formatDate = (d) => {
  if (!d) return ''
  const dateObj = d instanceof Date ? d : new Date(d)
  const y = dateObj.getFullYear()
  const m = String(dateObj.getMonth() + 1).padStart(2, '0')
  const day = String(dateObj.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const addEmployee = async () => {
  // 表单校验
  const ok = await formRef.value?.validate?.().catch(() => false)
  if (!ok) return

  // 自动生成下一条 id（编号顺延）
  const nextId = rawData.value.length ? Math.max(...rawData.value.map((r) => r.id)) + 1 : 1

  rawData.value.push({
    id: nextId,
    date: formatDate(form.date),
    name: form.name.trim(),
    address: form.address.trim()
  })

  ElMessage.success('添加成功')
  closeAdd()
}

// ===== 删除员工（列表弹窗 + 分页 + 二次确认） =====
const deleteVisible = ref(false)

const deletePageSize = ref(5)
const deleteCurrentPage = ref(1)

const deleteTotal = computed(() => rawData.value.length)

const deletePageData = computed(() => {
  const start = (deleteCurrentPage.value - 1) * deletePageSize.value
  return rawData.value.slice(start, start + deletePageSize.value)
})

const openDelete = () => {
  deleteVisible.value = true
  // 打开时如果当前页超出范围，纠正一下
  const maxPage = Math.max(1, Math.ceil(deleteTotal.value / deletePageSize.value))
  if (deleteCurrentPage.value > maxPage) deleteCurrentPage.value = maxPage
}

const onDeleteSizeChange = (size) => {
  deletePageSize.value = size
  deleteCurrentPage.value = 1
}

const onDeleteCurrentChange = (page) => {
  deleteCurrentPage.value = page
}

const confirmDeleteRow = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    rawData.value = rawData.value.filter((r) => r.id !== id)

    // 删除后如果当前页空了，回退一页
    const maxPage = Math.max(1, Math.ceil(rawData.value.length / deletePageSize.value))
    if (deleteCurrentPage.value > maxPage) deleteCurrentPage.value = maxPage

    ElMessage.success('删除成功')
  } catch {
    // 用户点了取消，不做任何事
  }
}
</script>

<template>
  <main class="page">
    <el-card class="card" shadow="always">
      <template #header>
        <div class="card-header">
          <div class="title">
            <span class="title-text">员工表格</span>
            <el-tag type="info" effect="plain">共 {{ total }} 条</el-tag>
          </div>

          <div class="actions">
            <el-input
              v-model="keyword"
              clearable
              placeholder="搜索：姓名 / 地址 / 日期"
              class="search"
              @clear="reset"
            />
            <el-button @click="reset">重置</el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="pagedData"
        row-key="id"
        border
        highlight-current-row
        style="width: 100%"
        :header-cell-style="{ background: 'linear-gradient(90deg, #2563eb, #06b6d4)', color: '#ffffff', fontWeight: 700 }"
        :cell-style="{ padding: '12px 0' }"
        empty-text="暂无数据"
        :height="tableHeight"
      >
        <el-table-column type="index" label="#" width="60" align="center" :index="indexMethod" />
        <el-table-column prop="date" label="日期" width="150" align="center" />
        <el-table-column prop="name" label="姓名" width="140" />
        <el-table-column prop="address" label="地址" min-width="260" show-overflow-tooltip />
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[5, 10]"
          :page-size="pageSize"
          :current-page="currentPage"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>

      <div class="toolbar">
        <el-button class="btn-danger" @click="openDelete">删除员工</el-button>
        <el-button type="primary" @click="openAdd">添加员工</el-button>
      </div>
      <el-dialog v-model="deleteVisible" title="删除员工" width="760px">
        <el-table
          :data="deletePageData"
          row-key="id"
          border
          style="width: 100%"
          :header-cell-style="{
            background: 'linear-gradient(90deg, #2563eb, #06b6d4)',
            color: '#ffffff',
            fontWeight: 700
          }"
          :cell-style="{ padding: '12px 0' }"
          empty-text="暂无数据"
        >
          <el-table-column label="操作" width="110" align="center">
            <template #default="{ row }">
              <el-button class="btn-danger" size="small" @click="confirmDeleteRow(row.id)">
                删除
              </el-button>
            </template>
          </el-table-column>

          <el-table-column type="index" label="#" width="60" align="center" />
          <el-table-column prop="date" label="日期" width="150" align="center" />
          <el-table-column prop="name" label="姓名" width="140" />
          <el-table-column prop="address" label="地址" min-width="260" show-overflow-tooltip />
        </el-table>

        <div class="delete-footer">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next"
            :total="deleteTotal"
            :page-sizes="[5, 10, 20, 50]"
            :page-size="deletePageSize"
            :current-page="deleteCurrentPage"
            @size-change="onDeleteSizeChange"
            @current-change="onDeleteCurrentChange"
          />
        </div>
      </el-dialog>

      <div class="footer">
        <div class="hint">提示：当前是示例数据；后续你可以用 axios 请求接口替换 rawData。</div>
      </div>

      <el-dialog v-model="dialogVisible" title="添加员工" width="520px" @close="closeAdd">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
          <el-form-item label="日期" prop="date">
            <el-date-picker
              v-model="form.date"
              type="date"
              placeholder="请选择日期"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" placeholder="请输入姓名" />
          </el-form-item>

          <el-form-item label="地址" prop="address">
            <el-input v-model="form.address" placeholder="请输入地址" />
          </el-form-item>
        </el-form>

        <template #footer>
          <span class="dialog-footer">
            <el-button @click="closeAdd">取消</el-button>
            <el-button type="primary" @click="addEmployee">添加</el-button>
          </span>
        </template>
      </el-dialog>
    </el-card>
  </main>
</template>

<style scoped>
.page {
  padding: 28px;
  min-height: 100vh;
  /* 更明显的蓝色渐变背景 */
  background: radial-gradient(1200px 600px at 20% 10%, rgba(37, 99, 235, 0.35), transparent 60%),
    radial-gradient(1000px 500px at 80% 20%, rgba(6, 182, 212, 0.28), transparent 55%),
    linear-gradient(135deg, #0b1220, #0f172a 40%, #0b1220);
}

.card {
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(10px);
}

/* 让 el-card 内部也更“玻璃态” */
:deep(.el-card__header) {
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.04);
}

:deep(.el-card__body) {
  background: rgba(255, 255, 255, 0.02);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-text {
  font-size: 20px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.92);
  letter-spacing: 0.5px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search {
  width: 280px;
}

.footer {
  margin-top: 14px;
}

.hint {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.8);
}

.toolbar {
  /* 与表格隔开更明显（约两行） */
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

/* 分页整体偏蓝，匹配当前卡片/表格风格 */
:deep(.el-pagination.is-background .el-pager li) {
  background: rgba(219, 234, 254, 0.9) !important;
  color: #1e3a8a !important;
  border: 1px solid #93c5fd !important;
}

:deep(.el-pagination.is-background .el-pager li.is-active) {
  background: linear-gradient(90deg, #2563eb, #06b6d4) !important;
  color: #ffffff !important;
  border-color: transparent !important;
}

:deep(.el-pagination.is-background .btn-prev),
:deep(.el-pagination.is-background .btn-next) {
  background: rgba(219, 234, 254, 0.9) !important;
  border: 1px solid #93c5fd !important;
  color: #1e3a8a !important;
}

:deep(.el-pagination__total) {
  color: rgba(255, 255, 255, 0.8) !important;
}

:deep(.el-pagination__jump) {
  color: rgba(255, 255, 255, 0.8) !important;
}

:deep(.el-pagination__sizes .el-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(148, 163, 184, 0.25) !important;
}

:deep(.el-pagination__editor.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(148, 163, 184, 0.25) !important;
}

:deep(.el-pagination__editor .el-input__inner) {
  color: rgba(255, 255, 255, 0.9) !important;
}

/* 表格整体更“蓝色系” */
:deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid #3b82f6;
}

:deep(.el-table__inner-wrapper),
:deep(.el-table th),
:deep(.el-table td) {
  border-color: #3b82f6 !important;
}

:deep(.el-table__inner-wrapper) {
  background: rgba(255, 255, 255, 0.03);
}

:deep(.el-table th) {
  /* 表头用深蓝底（实际渐变在 header-cell-style 里） */
  background-color: #1e3a8a !important;
}

:deep(.el-table td) {
  background: #e0f2fe; /* 浅蓝色 */
  color: #1f2937;
  font-weight: 500;
}

:deep(.el-table__row:hover > td) {
  background: rgba(6, 182, 212, 0.12) !important;
}

/* 输入框/按钮在深色背景下更协调 */
:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  box-shadow: none;
  border: 1px solid rgba(148, 163, 184, 0.25);
}

:deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.9);
}

:deep(.el-input__inner::placeholder) {
  color: rgba(226, 232, 240, 0.55);
}

:deep(.el-button) {
  background-color: #dbeafe !important;
  color: #1e3a8a !important;
  border-color: #93c5fd !important;
}

:deep(.el-tag) {
  background-color: #dbeafe !important;
  color: #1e3a8a !important;
  border-color: #93c5fd !important;
}

:deep(.el-form-item__error) {
  color: #000000 !important;
  font-weight: 500;
}

/* 对话框内 placeholder 改成灰色 */
:deep(.el-dialog .el-input__inner::placeholder),
:deep(.el-dialog .el-textarea__inner::placeholder),
:deep(.el-dialog .el-date-editor .el-input__inner::placeholder) {
  color: #9ca3af !important; /* 柔和灰色 */
  opacity: 1;
}

/* 对话框内输入内容改成黑色 */
:deep(.el-dialog .el-input__inner),
:deep(.el-dialog .el-textarea__inner) {
  color: #000000 !important;
}

/* 删除员工按钮：浅红色，和当前浅色按钮风格一致 */
:deep(.el-button.btn-danger) {
  background-color: #fee2e2 !important;
  color: #991b1b !important;
  border-color: #fca5a5 !important;
}

/* 删除弹窗底部分页区 */
.delete-footer {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

</style>