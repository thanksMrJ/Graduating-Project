// src/mock/adminStore.js
// 统一数据源：AdminUsers/AdminRecords 只要从这里读写，就能保证联动

const USERS_KEY = 'demo_users_v1'
const RECORDS_KEY = 'demo_records_v1'

// ====== 种子数据（首次进入时写入）======
const seedUsers = [
  { id: 1, name: '张三', age: 28, gender: '男', phone: '13800000001' },
  { id: 2, name: '李四', age: 35, gender: '女', phone: '13800000002' },
  { id: 3, name: '王五', age: 41, gender: '男', phone: '13800000003' },
]

const seedRecords = [
  {
    id: 1001,
    userId: 1,
    userName: '张三',
    createdAt: '2026-03-01 10:20',
    riskLevel: '中',
    conclusion: '风险提示（占位）',
    detail: {
      reportText: '病例报告内容（占位）：患者主诉……',
      indicators: '关键指标摘要（占位）：血压/血糖/血脂……',
      imaging: '影像描述（占位）：CT 显示……',
      explanation: '推理解释（占位）：模型关注点……',
    },
  },
  {
    id: 1002,
    userId: 2,
    userName: '李四',
    createdAt: '2026-03-02 14:05',
    riskLevel: '低',
    conclusion: '风险提示（占位）',
    detail: {
      reportText: '病例报告内容（占位）：患者既往史……',
      indicators: '关键指标摘要（占位）：心率/体温……',
      imaging: '影像描述（占位）：X 光提示……',
      explanation: '推理解释（占位）：特征贡献……',
    },
  },
  {
    id: 1003,
    userId: 1,
    userName: '张三',
    createdAt: '2026-03-03 09:10',
    riskLevel: '高',
    conclusion: '风险提示（占位）',
    detail: {
      reportText: '病例报告内容（占位）：症状加重……',
      indicators: '关键指标摘要（占位）：指标异常……',
      imaging: '影像描述（占位）：MRI 显示……',
      explanation: '推理解释（占位）：模型判定依据……',
    },
  },
]

function safeParse(text, fallback) {
  try {
    const v = JSON.parse(text)
    return v ?? fallback
  } catch {
    return fallback
  }
}

// ✅ 更稳：如果 localStorage 数据被污染/不是数组，会自动重置为种子数据
export function ensureSeed() {
  const usersText = localStorage.getItem(USERS_KEY)
  const recordsText = localStorage.getItem(RECORDS_KEY)

  const users = safeParse(usersText, null)
  const records = safeParse(recordsText, null)

  if (!Array.isArray(users)) {
    localStorage.setItem(USERS_KEY, JSON.stringify(seedUsers))
  }
  if (!Array.isArray(records)) {
    localStorage.setItem(RECORDS_KEY, JSON.stringify(seedRecords))
  }
}

// ===== Users =====
export function getUsers() {
  ensureSeed()
  return safeParse(localStorage.getItem(USERS_KEY), [])
}

export function setUsers(users) {
  localStorage.setItem(USERS_KEY, JSON.stringify(users))
}

// ===== Records =====
export function getRecords() {
  ensureSeed()
  return safeParse(localStorage.getItem(RECORDS_KEY), [])
}

export function setRecords(records) {
  localStorage.setItem(RECORDS_KEY, JSON.stringify(records))
}

export function getUserById(id) {
  return getUsers().find((u) => u.id === id)
}

export function getUserName(userId) {
  return getUserById(userId)?.name ?? `用户#${userId}`
}

// ✅ 关键：删除用户时，级联删除该用户所有 records
export function deleteUserById(id) {
  const users = getUsers().filter((u) => u.id !== id)
  setUsers(users)

  const records = getRecords().filter((r) => r.userId !== id)
  setRecords(records)
}

// 调试用：一键恢复演示数据
export function resetDemoData() {
  localStorage.setItem(USERS_KEY, JSON.stringify(seedUsers))
  localStorage.setItem(RECORDS_KEY, JSON.stringify(seedRecords))
}