// src/stores/appStore.js
import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'

const LS_KEY = 'gp_app_store_v1'

function loadFromLS() {
  try {
    const raw = localStorage.getItem(LS_KEY)
    if (!raw) return null
    return JSON.parse(raw)
  } catch {
    return null
  }
}

function saveToLS(payload) {
  localStorage.setItem(LS_KEY, JSON.stringify(payload))
}

function uid(prefix = 'u') {
  return `${prefix}_${Date.now()}_${Math.random().toString(16).slice(2)}`
}

export const useAppStore = defineStore('app', () => {
  // ========== State ==========
  const users = ref([
    // 演示数据：可保留
    { id: '1', name: '张三', age: 28, gender: '男', phone: '13800000001', password: '123456' },
    { id: '2', name: '李四', age: 26, gender: '女', phone: '13800000002', password: '123456' },
    { id: '3', name: '王五', age: 30, gender: '男', phone: '13800000003', password: '123456' },
  ])

  const records = ref([
    // 演示记录：userId 绑定用户
    { id: uid('r'), userId: '1', createdAt: '2026-03-01', title: '病历分析任务 A', status: '已完成', detail: '（占位符）' },
    { id: uid('r'), userId: '2', createdAt: '2026-03-02', title: '病历分析任务 B', status: '分析中', detail: '（占位符）' },
  ])

  // 当前登录用户（用户端用）
  const currentUserId = ref('')

  // ========== Getters ==========
  const currentUser = computed(() => users.value.find(u => String(u.id) === String(currentUserId.value)) || null)

  const userRecords = computed(() => {
    if (!currentUserId.value) return []
    return records.value.filter(r => String(r.userId) === String(currentUserId.value))
  })

  const userCount = computed(() => users.value.length)

  const isLoggedIn = computed(() => !!currentUserId.value && !!currentUser.value)

  // 在页面里需要“必须登录”的场景用它，避免拿到 null 导致显示异常
  const requireCurrentUser = () => {
    if (!currentUser.value) throw new Error('未登录或用户不存在')
    return currentUser.value
  }

  // ========== Persistence ==========
  const boot = () => {
    const data = loadFromLS()
    if (!data) return

    if (Array.isArray(data.users)) users.value = data.users
    if (Array.isArray(data.records)) records.value = data.records
    if (typeof data.currentUserId === 'string') currentUserId.value = data.currentUserId
  }

  // 兼容你 HomeView 里调用的名字
  const loadFromLocalStorage = () => boot()

  watch(
    [users, records, currentUserId],
    () => {
      saveToLS({
        users: users.value,
        records: records.value,
        currentUserId: currentUserId.value,
      })
    },
    { deep: true }
  )

  // ========== Helpers ==========
  // 返回当前“最小可分配”的数字 ID（从 1 开始），例如已存在 1,2,4 则返回 3
  const nextNumericUserId = () => {
    const used = new Set(
      users.value
        .map(u => Number(u.id))
        .filter(n => Number.isFinite(n) && n > 0)
        .map(n => Math.trunc(n))
    )

    let id = 1
    while (used.has(id)) id += 1
    return String(id)
  }

  // ========== Actions - 用户端 ==========
  // 注册：成功返回 user；失败 throw（配合页面 try/catch）
  const registerUser = (payload) => {
    const name = (payload?.name || '').trim()
    const password = (payload?.password || '').trim()

    if (!name) throw new Error('请输入姓名')
    if (!password) throw new Error('请输入密码')

    // 简单：姓名唯一（你也可以换 phone 唯一）
    const exists = users.value.some(u => (u.name || '').trim() === name)
    if (exists) throw new Error('该姓名已注册，请直接登录')

    const user = {
      id: nextNumericUserId(),
      name,
      age: payload?.age ?? '',
      gender: payload?.gender ?? '',
      phone: payload?.phone ?? '',
      password,
    }

    users.value.push(user)
    users.value = [...users.value]
    currentUserId.value = user.id
    return user
  }

  // 登录：成功返回 user；失败 throw
  const loginUser = ({ name, password }) => {
    const n = (name || '').trim()
    const p = (password || '').trim()
    if (!n || !p) throw new Error('请输入姓名和密码')

    const u = users.value.find(x => (x.name || '').trim() === n && String(x.password) === p)
    if (!u) throw new Error('姓名或密码错误')

    currentUserId.value = u.id
    return u
  }

  const logoutUser = () => {
    currentUserId.value = ''
  }

  // ✅ 忘记密码：重置密码（覆盖旧密码，并直接登录）
  // 入参：{ name, newPassword }
  const resetUserPassword = ({ name, newPassword }) => {
    const n = (name || '').trim()
    const p = (newPassword || '').trim()

    if (!n) throw new Error('请输入姓名')
    if (!p) throw new Error('请输入新密码')

    const idx = users.value.findIndex(u => (u.name || '').trim() === n)
    if (idx === -1) throw new Error('未找到该用户，请确认姓名是否正确')

    // 覆盖旧密码
    users.value[idx] = {
      ...users.value[idx],
      password: p,
    }
    users.value = [...users.value]

    // 直接登录
    currentUserId.value = users.value[idx].id
    return users.value[idx]
  }

  // 更新当前登录用户的资料（后续“编辑个人信息”会用到）
  const updateCurrentUser = (patch) => {
    const u = requireCurrentUser()
    const idx = users.value.findIndex(x => String(x.id) === String(u.id))
    if (idx === -1) throw new Error('用户不存在')

    // 只允许更新部分字段，且不允许覆盖 id
    users.value[idx] = {
      ...users.value[idx],
      ...patch,
      id: users.value[idx].id,
    }

    users.value = [...users.value]
    return users.value[idx]
  }

  // 创建病历分析任务（先用文本代替上传）
  const createRecordForCurrentUser = (payload) => {
    if (!currentUserId.value) throw new Error('请先登录')

    const title = (payload?.title || '').trim() || '病历分析任务'
    const createdAt = payload?.createdAt || new Date().toISOString().slice(0, 10)

    const rec = {
      id: uid('r'),
      userId: String(currentUserId.value),
      createdAt,
      title,
      status: payload?.status || '待分析',
      detail: payload?.detail || '（此处为分析详情占位符，后续由后端返回）',
    }

    records.value.unshift(rec)
    return rec
  }

  // ========== Actions - 管理员端 ==========
  // 删除用户：级联删除 records
  const adminDeleteUser = (userId) => {
    const id = String(userId)
    users.value = users.value.filter(u => String(u.id) !== id)
    users.value = [...users.value]
    records.value = records.value.filter(r => String(r.userId) !== id)
    records.value = [...records.value]

    if (String(currentUserId.value) === id) currentUserId.value = ''
  }

  const adminDeleteRecord = (recordId) => {
    const rid = String(recordId)
    records.value = records.value.filter(r => String(r.id) !== rid)
    records.value = [...records.value]
  }

  // 启动恢复
  boot()

  return {
    // state
    users,
    records,
    currentUserId,

    // getters
    currentUser,
    userRecords,
    userCount,
    isLoggedIn,
    requireCurrentUser,

    // persistence
    boot,
    loadFromLocalStorage,

    // actions
    registerUser,
    loginUser,
    logoutUser,
    resetUserPassword,
    updateCurrentUser,
    createRecordForCurrentUser,
    adminDeleteUser,
    adminDeleteRecord,
  }
})