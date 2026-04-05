<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入管理员密码', trigger: 'blur' }]
}

const login = async () => {
  const ok = await formRef.value?.validate?.().catch(() => false)
  if (!ok) return

  loading.value = true
  try {
    // ✅ 演示版账号密码（后续接后端接口再替换）
    if (form.username !== 'admin' || form.password !== '123456') {
      ElMessage.error('账号或密码错误（演示账号：admin / 123456）')
      return
    }

    // ✅ 写入 token（配合 router 守卫使用）
    localStorage.setItem('admin_token', 'demo-token')

    ElMessage.success('登录成功')
    router.push('/admin/users')
  } finally {
    loading.value = false
  }
}

const goHome = () => router.push('/')
</script>

<template>
  <main class="page">
    <div class="bg-glow" />

    <el-card class="card" shadow="always">
      <div class="header">
        <div class="badge">ADMIN</div>
        <h2 class="title">管理员登录</h2>
        <p class="sub">进入系统管理后台（当前为前端演示版）</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="86px"
        @keyup.enter="login"
      >
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入管理员账号"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            placeholder="请输入管理员密码"
            type="password"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item>
          <div class="btns">
            <el-button type="primary" size="large" :loading="loading" @click="login">
              登录
            </el-button>
            <el-button size="large" :disabled="loading" @click="goHome">返回主页</el-button>
          </div>
        </el-form-item>
      </el-form>

      <div class="hint">
        演示账号：admin / 123456（后续接后端后将改为真实校验）
      </div>
    </el-card>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 28px;
  position: relative;
  overflow: hidden;

  background: radial-gradient(1200px 600px at 20% 10%, rgba(37, 99, 235, 0.22), transparent 60%),
    radial-gradient(1000px 500px at 80% 20%, rgba(6, 182, 212, 0.18), transparent 55%),
    linear-gradient(135deg, #0b1220, #0f172a 40%, #0b1220);
}

.bg-glow {
  position: absolute;
  inset: -200px;
  background:
    radial-gradient(600px 300px at 30% 30%, rgba(37, 99, 235, 0.25), transparent 60%),
    radial-gradient(520px 280px at 70% 40%, rgba(6, 182, 212, 0.18), transparent 60%);
  filter: blur(10px);
  pointer-events: none;
}

.card {
  width: 540px;
  max-width: calc(100vw - 24px);
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(10px);
}

.header {
  margin-bottom: 10px;
}

.badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.86);
  font-size: 12px;
  letter-spacing: 1px;
}

.title {
  margin: 10px 0 6px;
  color: rgba(255, 255, 255, 0.92);
  font-weight: 900;
}

.sub {
  margin: 0 0 14px;
  color: rgba(226, 232, 240, 0.8);
  font-size: 13px;
}

.btns {
  display: flex;
  gap: 12px;
}

.hint {
  margin-top: 10px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.75);
}

/* label 变亮一点 */
:deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.9) !important;
}

/* 输入框半透明 */
:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(148, 163, 184, 0.25) !important;
  box-shadow: none !important;
}

/* 输入内容白色 */
:deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.92) !important;
}

/* placeholder 灰白 */
:deep(.el-input__inner::placeholder) {
  color: rgba(226, 232, 240, 0.55) !important;
}
</style>