<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/appStore'

const router = useRouter()
const appStore = useAppStore()

const form = reactive({
  name: '',
  password: '',
})

// ========== 忘记密码：弹窗与表单 ==========
const resetVisible = ref(false)
const resetForm = reactive({
  name: '',
  newPassword: '',
  confirmPassword: '',
})

const openReset = () => {
  // 默认把登录页里输入的 name 带过去，减少用户再输一遍
  resetForm.name = (form.name || '').trim()
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
  resetVisible.value = true
}

const submitReset = async () => {
  const name = (resetForm.name || '').trim()
  const p1 = (resetForm.newPassword || '').trim()
  const p2 = (resetForm.confirmPassword || '').trim()

  if (!name) {
    ElMessage.error('请输入姓名')
    return
  }
  if (!p1 || !p2) {
    ElMessage.error('请输入新密码并确认')
    return
  }
  if (p1 !== p2) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }

  try {
    // 这里调用 store：重置密码 + 直接登录
    // 我们下一步会在 appStore.js 里补上 resetUserPassword
    if (typeof appStore.resetUserPassword !== 'function') {
      ElMessage.error('当前 appStore 未实现 resetUserPassword，请先完善 store（下一步我来改）')
      return
    }

    const res = await appStore.resetUserPassword({ name, newPassword: p1 })

    // 兼容：返回 ok/message 或直接成功
    if (res && typeof res === 'object' && 'ok' in res) {
      if (!res.ok) {
        ElMessage.error(res.message || '重置失败')
        return
      }
    }

    ElMessage.success('密码已重置，已自动登录')
    resetVisible.value = false

    // 直接跳用户主页
    router.push('/user/profile')
  } catch (e) {
    ElMessage.error(e?.message || '重置失败')
  }
}

const submit = async () => {
  try {
    // 兼容两种 store 写法：
    // 1) loginUser 抛错，成功返回 user
    // 2) loginUser 返回 { ok, message, user }
    const res = await appStore.loginUser({
      name: form.name,
      password: form.password,
    })

    if (res && typeof res === 'object' && 'ok' in res) {
      if (!res.ok) {
        ElMessage.error(res.message || '登录失败')
        return
      }
      ElMessage.success(`登录成功：${res.user?.name || form.name}`)
    } else {
      // res 可能是 user
      ElMessage.success(`登录成功：${res?.name || form.name}`)
    }

    router.push('/user/profile')
  } catch (e) {
    ElMessage.error(e?.message || '登录失败')
  }
}

const goHome = () => {
  router.push('/')
}
</script>

<template>
  <main class="page">
    <el-card class="card" shadow="always">
      <template #header>
        <div class="head">
          <div class="title">用户登录</div>
          <el-button class="btn-ghost" @click="goHome">返回主页</el-button>
        </div>
      </template>

      <el-form label-width="72px" @submit.prevent>
        <el-form-item label="姓名">
          <el-input
            v-model="form.name"
            placeholder="请输入姓名"
            autocomplete="off"
            @keyup.enter="submit"
          />
        </el-form-item>

        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            placeholder="请输入密码"
            show-password
            autocomplete="off"
            @keyup.enter="submit"
          />
        </el-form-item>

        <!-- ✅ 忘记密码入口（密码栏下面） -->
        <div class="forgot-row">
          <el-button type="primary" link class="forgot-link" @click="openReset">
            忘记密码？
          </el-button>
        </div>

        <div class="actions">
          <el-button type="primary" size="large" @click="submit">登录</el-button>
          <el-button size="large" @click="goHome">去注册（主页）</el-button>
        </div>

        <div class="tip">
          说明：当前为前端演示版，用户数据存储在本地 localStorage 中；后续会接入后端接口。
        </div>
      </el-form>
    </el-card>

    <!-- ✅ 忘记密码：重置弹窗 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="520px" class="glass-dialog">
      <el-form label-width="92px">
        <el-form-item label="姓名">
          <el-input v-model="resetForm.name" placeholder="请输入姓名（用于找到账户）" />
        </el-form-item>

        <el-form-item label="新密码">
          <el-input v-model="resetForm.newPassword" placeholder="请输入新密码" show-password />
        </el-form-item>

        <el-form-item label="确认密码">
          <el-input v-model="resetForm.confirmPassword" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReset">确认重置</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<style scoped>
.page {
  min-height: calc(100vh - 64px);
  padding: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card {
  width: min(520px, 92vw);
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(10px);
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-size: 18px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.92);
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.tip {
  margin-top: 12px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.75);
  line-height: 1.6;
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(148, 163, 184, 0.25) !important;
  color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.el-form-item__label) {
  color: rgba(226, 232, 240, 0.9) !important;
}

/* 忘记密码那一行 */
.forgot-row {
  margin-top: -10px;
  margin-bottom: 8px;
  display: flex;
  justify-content: flex-end;
}
.forgot-link {
  padding: 0;
}

/* 弹窗玻璃风（和你项目风格一致） */
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