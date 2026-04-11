const API_BASE = (import.meta.env.VITE_API_BASE || 'http://localhost:8085').replace(/\/$/, '')

async function parseBody(res) {
  const text = await res.text()
  if (!text) return {}
  try {
    return JSON.parse(text)
  } catch {
    return {}
  }
}

/**
 * 普通用户接口（自动带 user_token）
 */
export async function api(path, options = {}) {
  const headers = { ...options.headers }
  if (!(options.body instanceof FormData)) {
    if (!headers['Content-Type']) headers['Content-Type'] = 'application/json'
  }
  const token = localStorage.getItem('user_token')
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers })
  const data = await parseBody(res)
  if (!res.ok || data.code !== 0) {
    const msg = data.message || res.statusText || '请求失败'
    const err = new Error(msg)
    err.status = res.status
    err.code = data.code
    throw err
  }
  return data.data
}

/**
 * 管理员接口（带 admin_token）
 */
export async function adminApi(path, options = {}) {
  const headers = { ...options.headers }
  if (!(options.body instanceof FormData)) {
    if (!headers['Content-Type']) headers['Content-Type'] = 'application/json'
  }
  const token = localStorage.getItem('admin_token')
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers })
  const data = await parseBody(res)
  if (!res.ok || data.code !== 0) {
    const msg = data.message || res.statusText || '请求失败'
    const err = new Error(msg)
    err.status = res.status
    err.code = data.code
    throw err
  }
  return data.data
}

export async function apiPublic(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  const res = await fetch(`${API_BASE}${path}`, { ...options, headers })
  const data = await parseBody(res)
  if (!res.ok || data.code !== 0) {
    throw new Error(data.message || res.statusText || '请求失败')
  }
  return data.data
}

/**
 * 用户登录状态下上传文件（multipart）
 */
export async function uploadFile(file, folder = 'files') {
  const fd = new FormData()
  fd.append('file', file)
  const token = localStorage.getItem('user_token')
  const headers = {}
  if (token) headers['Authorization'] = `Bearer ${token}`
  const q = folder ? `?folder=${encodeURIComponent(folder)}` : ''
  const res = await fetch(`${API_BASE}/api/v1/uploads/file${q}`, {
    method: 'POST',
    headers,
    body: fd,
  })
  const data = await parseBody(res)
  if (!res.ok || data.code !== 0) {
    throw new Error(data.message || '上传失败')
  }
  return data.data
}

export { API_BASE }
