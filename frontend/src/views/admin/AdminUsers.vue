<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/http'

const users = ref([])
const keyword = ref('')

const loadUsers = async () => {
  users.value = await adminApi('/api/v1/admin/users', { method: 'GET' })
}

onMounted(() => {
  loadUsers().catch((e) => ElMessage.error(e?.message || '加载用户失败'))
})

const filteredUsers = computed(() => {
  const k = keyword.value.trim()
  if (!k) return users.value
  return users.value.filter((u) => {
    return (
      String(u.id).includes(k) ||
      String(u.name || '').includes(k) ||
      String(u.age || '').includes(k) ||
      String(u.gender || '').includes(k) ||
      String(u.phone || '').includes(k)
    )
  })
})

const resetSearch = () => {
  keyword.value = ''
}

const deleteUser = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确认删除该用户？（会同时删除该用户的病历分析记录）',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await adminApi(`/api/v1/admin/users/${row.id}`, { method: 'DELETE' })
    ElMessage.success('删除成功')
    await loadUsers()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}
</script>

<template>
  <el-card class="card" shadow="always">
    <template #header>
      <div class="header">
        <div class="title">
          用户管理
          <el-tag type="info">共 {{ filteredUsers.length }} 条</el-tag>
        </div>

        <div class="search">
          <el-input v-model="keyword" placeholder="搜索 ID / 姓名 / 年龄 / 性别 / 联系方式" clearable />
          <el-button @click="resetSearch">重置</el-button>
        </div>
      </div>
    </template>

    <el-table
      :data="filteredUsers"
      border
      style="width: 100%"
      empty-text="暂无用户"
      :header-cell-style="{
        background: 'linear-gradient(90deg,#2563eb,#06b6d4)',
        color: '#fff',
        fontWeight: 700,
      }"
    >
      <el-table-column prop="id" label="用户ID" width="100" align="center" />
      <el-table-column prop="name" label="姓名" width="140" />
      <el-table-column prop="age" label="年龄" width="100" align="center" />
      <el-table-column prop="gender" label="性别" width="100" align="center" />
      <el-table-column prop="phone" label="联系方式" min-width="160" />

      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="scope">
          <el-button type="danger" size="small" @click="deleteUser(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="hint">说明：管理员端仅可删除用户，不提供新增功能（用户注册后会自动出现在这里）。</div>
  </el-card>
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
</style>
