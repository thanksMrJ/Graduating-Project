// src/stores/appStore.js — 与后端 API 联调（JWT）
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api, apiPublic } from '@/api/http'

const USER_TOKEN_KEY = 'user_token'

export const useAppStore = defineStore('app', () => {
  const currentUser = ref(null)
  const records = ref([])

  const currentUserId = computed(() =>
    currentUser.value?.id != null ? String(currentUser.value.id) : '',
  )

  const currentUserComputed = computed(() => currentUser.value)

  const userRecords = computed(() => records.value)

  const userCount = ref(0)

  const isLoggedIn = computed(() => !!currentUser.value)

  const requireCurrentUser = () => {
    if (!currentUser.value) throw new Error('未登录或用户不存在')
    return currentUser.value
  }

  const setUserToken = (token) => {
    if (token) localStorage.setItem(USER_TOKEN_KEY, token)
    else localStorage.removeItem(USER_TOKEN_KEY)
  }

  const clearUserSession = () => {
    currentUser.value = null
    records.value = []
    setUserToken('')
  }

  const fetchMe = async () => {
    const token = localStorage.getItem(USER_TOKEN_KEY)
    if (!token) {
      currentUser.value = null
      return null
    }
    try {
      const u = await api('/api/v1/me', { method: 'GET' })
      currentUser.value = u
      return u
    } catch {
      clearUserSession()
      return null
    }
  }

  const fetchUserRecords = async () => {
    const list = await api('/api/v1/records', { method: 'GET' })
    records.value = Array.isArray(list) ? list : []
    return records.value
  }

  const fetchPublicUserCount = async () => {
    try {
      const data = await apiPublic('/api/v1/stats/user-count', { method: 'GET' })
      userCount.value = Number(data?.count ?? 0)
    } catch {
      userCount.value = 0
    }
  }

  const registerUser = async (payload) => {
    const name = (payload?.name || '').trim()
    const password = (payload?.password || '').trim()
    if (!name) throw new Error('请输入姓名')
    if (!password) throw new Error('请输入密码')

    const body = await apiPublic('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify({
        name,
        password,
        age: payload?.age ?? '',
        gender: payload?.gender ?? '',
        phone: payload?.phone ?? '',
      }),
    })
    setUserToken(body.token)
    currentUser.value = body.user
    return body.user
  }

  const loginUser = async ({ name, password }) => {
    const n = (name || '').trim()
    const p = (password || '').trim()
    if (!n || !p) throw new Error('请输入姓名和密码')

    const body = await apiPublic('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ name: n, password: p }),
    })
    setUserToken(body.token)
    currentUser.value = body.user
    return body.user
  }

  const logoutUser = () => {
    clearUserSession()
  }

  const resetUserPassword = async ({ name, newPassword }) => {
    const n = (name || '').trim()
    const p = (newPassword || '').trim()
    if (!n) throw new Error('请输入姓名')
    if (!p) throw new Error('请输入新密码')

    const body = await apiPublic('/api/v1/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ name: n, newPassword: p }),
    })
    setUserToken(body.token)
    currentUser.value = body.user
    return body.user
  }

  const updateCurrentUser = async (patch) => {
    requireCurrentUser()
    const u = await api('/api/v1/me', {
      method: 'PATCH',
      body: JSON.stringify({
        age: patch?.age,
        gender: patch?.gender,
        phone: patch?.phone,
      }),
    })
    currentUser.value = u
    return u
  }

  const createRecordForCurrentUser = async (payload) => {
    requireCurrentUser()
    const title = (payload?.title || '').trim() || '病历分析任务'
    const createdAt = payload?.createdAt || undefined
    const status = payload?.status || '待分析'
    const detail = payload?.detail || {}

    const rec = await api('/api/v1/records', {
      method: 'POST',
      body: JSON.stringify({ title, createdAt, status, detail }),
    })
    await fetchUserRecords()
    return rec
  }

  /**
   * 双表 Excel：后端解析、归并患者、生成综合文本；可选立即千问/占位分析。
   * @param {{ recordFile: File, indicatorFile: File, title?: string, notes?: string, createdAt?: string, autoAnalyze?: boolean }} p
   */
  const createRecordFromExcel = async (p) => {
    requireCurrentUser()
    if (!p?.recordFile || !p?.indicatorFile) {
      throw new Error('请选择病历表与指标表两个 Excel 文件')
    }
    const fd = new FormData()
    fd.append('recordExcel', p.recordFile)
    fd.append('indicatorExcel', p.indicatorFile)
    const title = (p.title || '').trim()
    if (title) fd.append('title', title)
    const notes = (p.notes || '').trim()
    if (notes) fd.append('notes', notes)
    if (p.createdAt) fd.append('createdAt', p.createdAt)
    fd.append('autoAnalyze', p.autoAnalyze === false ? 'false' : 'true')
    const rec = await api('/api/v1/records/from-excel', { method: 'POST', body: fd })
    await fetchUserRecords()
    return rec
  }

  const updateUserRecord = async (id, { title, detail } = {}) => {
    requireCurrentUser()
    const body = {}
    if (title != null && title !== undefined) body.title = title
    if (detail != null) body.detail = detail
    if (Object.keys(body).length === 0) throw new Error('没有可更新的内容')
    const rec = await api(`/api/v1/records/${encodeURIComponent(id)}`, {
      method: 'PATCH',
      body: JSON.stringify(body),
    })
    await fetchUserRecords()
    return rec
  }

  const runPlaceholderAnalyze = async (id) => {
    requireCurrentUser()
    return await api(`/api/v1/records/${encodeURIComponent(id)}/placeholder-analyze`, {
      method: 'POST',
    })
  }

  /** 文本病历推理（需后端 app.qwen.enabled + api-key） */
  const inferTextReasoning = async (body) => {
    requireCurrentUser()
    return await api('/api/v1/inference/text', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  }

  const boot = () => fetchMe()
  const loadFromLocalStorage = () => fetchMe()

  return {
    users: ref([]),
    records,
    currentUserId,
    currentUser: currentUserComputed,
    userRecords,
    userCount,
    isLoggedIn,
    requireCurrentUser,
    boot,
    loadFromLocalStorage,
    registerUser,
    loginUser,
    logoutUser,
    resetUserPassword,
    updateCurrentUser,
    createRecordForCurrentUser,
    createRecordFromExcel,
    updateUserRecord,
    runPlaceholderAnalyze,
    inferTextReasoning,
    fetchUserRecords,
    fetchPublicUserCount,
    fetchMe,
    clearUserSession,
  }
})
