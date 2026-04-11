<script setup>
import { reactive, ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/appStore'

const router = useRouter()
const appStore = useAppStore()

const goAdmin = () => {
  router.push('/admin/login')
}

const goUserLogin = () => {
  router.push('/user/login')
}

const registerVisible = ref(false)

const registerForm = reactive({
  name: '',
  password: '',
  age: '',
  gender: '男',
  phone: '',
})

const resetRegisterForm = () => {
  registerForm.name = ''
  registerForm.password = ''
  registerForm.age = ''
  registerForm.gender = '男'
  registerForm.phone = ''
}

const openRegister = () => {
  resetRegisterForm()
  registerVisible.value = true
}

const userCount = computed(() => appStore.userCount)

onMounted(() => {
  appStore.fetchPublicUserCount()
})

const PAGE_TITLE = '医疗多模态主页'
const REGISTER_TITLE = '注册'

watch(registerVisible, (open) => {
  document.title = open ? REGISTER_TITLE : PAGE_TITLE
})

const submitRegister = async () => {
  const name = String(registerForm.name || '').trim()
  const password = String(registerForm.password || '').trim()

  if (!name) {
    ElMessage.warning('请输入姓名')
    return
  }
  if (!password) {
    ElMessage.warning('请输入密码')
    return
  }

  try {
    const res = await appStore.registerUser({
      name,
      password,
      age: registerForm.age,
      gender: registerForm.gender,
      phone: registerForm.phone,
    })
    ElMessage.success(`注册成功：${res?.name || name}`)
    registerVisible.value = false
    router.push('/user/profile')
  } catch (e) {
    ElMessage.error(e?.message || '注册失败')
  }
}
</script>

<template>
  <main class="home">
    <!-- 粒子光点层：纯装饰，不影响交互 -->
    <div class="particles" aria-hidden="true"></div>

    <div class="wrap">
      <!-- 顶部：大标题置顶居中 -->
      <header class="hero">
        <h1 class="hero-title art-title">多模态病历分析辅助系统</h1>
        <p class="hero-subtitle">基于影像、文本报告与结构化指标的综合分析展示平台（已对接后端 API）</p>

        <div class="chips">
          <span class="chip">病历任务管理</span>
          <span class="chip">结果展示与解释</span>
          <span class="chip">管理员后台</span>
        </div>
      </header>

      <!-- 中部：系统简介 -->
      <section class="intro-section">
        <el-card class="intro" shadow="always">
          <h3>系统简介</h3>
          <ul>
            <li>多模态信息录入（后续支持文件上传/校验）</li>
            <li>病历分析任务管理（创建、查询、记录追踪）</li>
            <li>分析结果可视化（风险等级、关键因素、解释信息）</li>
          </ul>
          <p class="note">说明：用户与病历任务数据由 Spring Boot 后端与数据库存储；推理结果占位，后续接模型接口。</p>
        </el-card>
      </section>

      <!-- 底部：入口按钮（中下方居中 + 竖排） -->
      <section class="entry-section">
        <el-card class="actions" shadow="always">
          <div class="actions-head">
            <div class="actions-title">请选择入口</div>
            <div class="count">当前已注册：{{ userCount }} 人</div>
          </div>

          <div class="btns-vertical">
            <el-button type="primary" size="large" class="full-btn" @click="goAdmin">管理员登录</el-button>
            <el-button size="large" class="full-btn" @click="goUserLogin">用户登录</el-button>
            <el-button size="large" class="full-btn" @click="openRegister">用户注册</el-button>
          </div>

          <div class="entry-tip">提示：登录态使用 JWT（user_token / admin_token），请先启动后端 localhost:8080。</div>
        </el-card>
      </section>
    </div>

    <!-- 用户注册：悬浮表单 -->
    <el-dialog v-model="registerVisible" title="用户注册" width="520px" class="glass-dialog">
      <el-form label-width="90px">
        <el-form-item label="姓名">
          <el-input v-model="registerForm.name" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="密码">
          <el-input v-model="registerForm.password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-form-item label="年龄">
          <el-input v-model="registerForm.age" placeholder="请输入年龄" />
        </el-form-item>

        <el-form-item label="性别">
          <el-select v-model="registerForm.gender" placeholder="请选择性别" style="width: 100%">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>

        <el-form-item label="联系方式">
          <el-input v-model="registerForm.phone" placeholder="手机号/邮箱（可选）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRegister">注册用户</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<style scoped>
/* 主背景 + 动态流动 */
.home {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  padding: 28px;
  background:
    radial-gradient(1200px 600px at 20% 10%, rgba(37, 99, 235, 0.25), transparent 60%),
    radial-gradient(1000px 500px at 80% 20%, rgba(6, 182, 212, 0.22), transparent 55%),
    linear-gradient(135deg, #0b1220, #0f172a 40%, #0b1220);
  background-size: 200% 200%;
  animation: bgMove 18s ease-in-out infinite;
}

/* 粒子光点层：纯 CSS（两层不同速度/大小） */
.particles {
  position: absolute;
  inset: -40px;
  pointer-events: none;
  z-index: 0;
  opacity: 0.95;
}

.particles::before,
.particles::after {
  content: '';
  position: absolute;
  inset: 0;
  background-repeat: no-repeat;
  mix-blend-mode: screen;
  filter: blur(0.2px);
  opacity: 0.8;
}

/* 第一层：小粒子多一点 */
.particles::before {
  background-image:
    radial-gradient(2px 2px at 10% 20%, rgba(147, 197, 253, 0.85) 60%, transparent 62%),
    radial-gradient(1.5px 1.5px at 25% 35%, rgba(34, 211, 238, 0.75) 60%, transparent 62%),
    radial-gradient(2px 2px at 40% 15%, rgba(59, 130, 246, 0.7) 60%, transparent 62%),
    radial-gradient(1.5px 1.5px at 55% 28%, rgba(56, 189, 248, 0.75) 60%, transparent 62%),
    radial-gradient(2px 2px at 70% 18%, rgba(125, 211, 252, 0.75) 60%, transparent 62%),
    radial-gradient(1.5px 1.5px at 82% 40%, rgba(34, 211, 238, 0.7) 60%, transparent 62%),
    radial-gradient(2px 2px at 18% 70%, rgba(59, 130, 246, 0.65) 60%, transparent 62%),
    radial-gradient(1.5px 1.5px at 35% 82%, rgba(34, 211, 238, 0.65) 60%, transparent 62%),
    radial-gradient(2px 2px at 62% 76%, rgba(147, 197, 253, 0.65) 60%, transparent 62%),
    radial-gradient(1.5px 1.5px at 78% 82%, rgba(34, 211, 238, 0.6) 60%, transparent 62%),
    radial-gradient(2px 2px at 90% 68%, rgba(59, 130, 246, 0.6) 60%, transparent 62%);
  animation: floatDots1 16s linear infinite;
}

/* 第二层：更大一点、少一点、慢一点 */
.particles::after {
  opacity: 0.6;
  background-image:
    radial-gradient(3px 3px at 15% 30%, rgba(59, 130, 246, 0.55) 55%, transparent 60%),
    radial-gradient(3px 3px at 48% 22%, rgba(34, 211, 238, 0.45) 55%, transparent 60%),
    radial-gradient(3px 3px at 72% 30%, rgba(147, 197, 253, 0.45) 55%, transparent 60%),
    radial-gradient(3px 3px at 30% 62%, rgba(34, 211, 238, 0.4) 55%, transparent 60%),
    radial-gradient(3px 3px at 62% 58%, rgba(59, 130, 246, 0.4) 55%, transparent 60%),
    radial-gradient(3px 3px at 85% 60%, rgba(34, 211, 238, 0.38) 55%, transparent 60%);
  animation: floatDots2 22s linear infinite;
}

/* 页面内容层要盖在粒子上面 */
.wrap {
  position: relative;
  z-index: 1;
  width: min(1100px, 94vw);
  margin: 0 auto;
  min-height: calc(100vh - 56px);
  display: flex;
  flex-direction: column;
}

/* 顶部标题区域：置顶居中 */
.hero {
  text-align: center;
  padding-top: 10px;
  padding-bottom: 6px;
}

.hero-title {
  margin: 0;
  font-size: clamp(34px, 4.2vw, 48px);
  letter-spacing: 1.2px;
  color: rgba(255, 255, 255, 0.96);

  font-family: "STKaiti", "KaiTi", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei",
    "Brush Script MT", "Segoe Script", cursive;
  font-weight: 900;
  text-shadow:
    0 8px 26px rgba(37, 99, 235, 0.38),
    0 3px 12px rgba(6, 182, 212, 0.22);
  animation: titleGlow 4s ease-in-out infinite alternate;
}

.hero-subtitle {
  margin: 10px auto 0;
  max-width: 820px;
  color: rgba(226, 232, 240, 0.82);
  line-height: 1.7;
  font-size: 13px;
}

/* chips */
.chips {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.chip {
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(255, 255, 255, 0.07);
  color: rgba(255, 255, 255, 0.88);
  font-size: 12px;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.chip:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(59, 130, 246, 0.35);
}

/* 系统简介 */
.intro-section {
  margin-top: 16px;
}

.intro {
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(10px);
  color: rgba(255, 255, 255, 0.88);
}

.intro h3 {
  margin: 0 0 10px;
}

.intro ul {
  margin: 0;
  padding-left: 18px;
}

.intro li {
  margin: 8px 0;
}

.note {
  margin: 12px 0 0;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.75);
}

/* 入口区域 */
.entry-section {
  margin-top: auto;
  padding-bottom: 12px;
  display: flex;
  justify-content: center;
}

.actions {
  width: min(520px, 92vw);
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(255, 255, 255, 0.07);
  backdrop-filter: blur(10px);
}

.actions-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.actions-title {
  font-size: 16px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.92);
}

.count {
  color: rgba(226, 232, 240, 0.78);
  font-size: 12px;
}

/* 按钮竖排 */
.btns-vertical {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
  width: 100%;
}

/* ✅ 修复 Element Plus 默认 .el-button + .el-button 的左外边距，导致竖排按钮不对齐 */
.btns-vertical :deep(.el-button + .el-button) {
  margin-left: 0 !important;
}

.full-btn {
  width: 100%;
  max-width: 360px;
  height: 44px;
  border-radius: 12px;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.full-btn:hover {
  transform: translateY(-2px) scale(1.01);
  box-shadow: 0 10px 28px rgba(37, 99, 235, 0.35);
}

.entry-tip {
  margin-top: 12px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.72);
  text-align: center;
}

/* 小屏优化 */
@media (max-width: 900px) {
  .home {
    padding: 18px;
  }
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
  color: rgba(226, 232, 240, 0.9);
}

/* 背景缓慢流动动画 */
@keyframes bgMove {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* 标题呼吸发光 */
@keyframes titleGlow {
  0% {
    text-shadow:
      0 6px 20px rgba(37, 99, 235, 0.35),
      0 2px 10px rgba(6, 182, 212, 0.18);
  }
  100% {
    text-shadow:
      0 10px 36px rgba(37, 99, 235, 0.55),
      0 4px 16px rgba(6, 182, 212, 0.35);
  }
}

/* 粒子漂浮：轻微平移 + 缩放 */
@keyframes floatDots1 {
  0% { transform: translate3d(0, 0, 0); }
  50% { transform: translate3d(-2%, 3%, 0); }
  100% { transform: translate3d(0, 0, 0); }
}

@keyframes floatDots2 {
  0% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(2%, -3%, 0) scale(1.02); }
  100% { transform: translate3d(0, 0, 0) scale(1); }
}
</style>