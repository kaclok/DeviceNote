import axios from 'axios'

// 接口前缀统一在此维护：需与后端 server.servlet.context-path、
// nginx location、vite 代理三处保持一致，改前缀只改这一行。
export const API_PREFIX = '/api_safety_visitor'

const instance = axios.create({
  baseURL: API_PREFIX,
  timeout: 15000
})

instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('vs_token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

instance.interceptors.response.use(
  (response) => {
    const data = response.data
    // 统一返回结构；401 由后端 AuthInterceptor 直接返回
    return data
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('vs_token')
      localStorage.removeItem('vs_username')
      if (!window.location.hash.includes('/admin/login')) {
        window.location.hash = '/admin/login'
      }
    }
    return Promise.reject(error)
  }
)

// ===== 公共接口（无需登录）=====
export function getHosts() {
  return instance.get('/config/hosts')
}
export function getStaff(hostId) {
  return instance.get('/config/staff', { params: hostId ? { hostId } : {} })
}
export function getPublicSettings() {
  return instance.get('/config/settings')
}
export function createVisitor(payload) {
  return instance.post('/visitors', payload)
}
export function getQuestions() {
  return instance.get('/questions')
}
export function submitExam(id, answers) {
  return instance.post(`/visitors/${id}/exam`, { answers })
}

// ===== 鉴权 =====
export function login(username, password) {
  return instance.post('/auth/login', { username, password })
}
export function getMe() {
  return instance.get('/auth/me')
}

// ===== 后台：部门 =====
export function listHostsAdmin() {
  return instance.get('/admin/hosts').then((r) => r.data)
}
export function saveHost(payload) {
  return instance.post('/admin/hosts', payload)
}
export function updateHost(id, payload) {
  return instance.put(`/admin/hosts/${id}`, payload)
}
export function deleteHost(id) {
  return instance.delete(`/admin/hosts/${id}`)
}

// ===== 后台：人员 =====
export function listStaffAdmin(hostId) {
  return instance.get('/admin/staff', { params: hostId ? { hostId } : {} }).then((r) => r.data)
}
export function saveStaff(payload) {
  return instance.post('/admin/staff', payload)
}
export function updateStaff(id, payload) {
  return instance.put(`/admin/staff/${id}`, payload)
}
export function deleteStaff(id) {
  return instance.delete(`/admin/staff/${id}`)
}

// ===== 后台：访客台账 =====
export function listVisitors(params) {
  return instance.get('/admin/visitors', { params }).then((r) => r.data)
}
export function updateVisitor(id, payload) {
  return instance.put(`/admin/visitors/${id}`, payload)
}
export function deleteVisitor(id) {
  return instance.delete(`/admin/visitors/${id}`)
}
export function exportVisitors(params) {
  return instance.get('/admin/visitors/export', { params }).then((r) => r.data)
}

// ===== 后台：题库 =====
export function listQuestionsAdmin() {
  return instance.get('/admin/questions').then((r) => r.data)
}
export function saveQuestion(payload) {
  return instance.post('/admin/questions', payload)
}
export function updateQuestion(id, payload) {
  return instance.put(`/admin/questions/${id}`, payload)
}
export function deleteQuestion(id) {
  return instance.delete(`/admin/questions/${id}`)
}
// 客户端读取 Excel 解析题目并逐条保存
export async function importQuestions(file) {
  const XLSX = await import('xlsx')
  const buf = await file.arrayBuffer()
  const wb = XLSX.read(buf, { type: 'array' })
  const sheet = wb.Sheets[wb.SheetNames[0]]
  const rows = XLSX.utils.sheet_to_json(sheet, { header: 1 })

  const LETTERS = ['A', 'B', 'C', 'D', 'E', 'F']
  let success = 0
  let fail = 0
  // 第一行若为表头则跳过（含"题干"字样）
  const startIdx = rows.length && rows[0].some((c) => c && String(c).includes('题干')) ? 1 : 0
  for (let i = startIdx; i < rows.length; i++) {
    const r = rows[i]
    if (!r || !r[0]) continue
    const title = String(r[0]).trim()
    const options = LETTERS.map((l, idx) => (r[idx + 1] != null ? String(r[idx + 1]).trim() : `${l}`)).filter(
      (o, idx) => r[idx + 1] != null && String(r[idx + 1]).trim() !== ''
    )
    const answerRaw = r[6] != null ? String(r[6]).trim() : ''
    // 正确答案列可能为 "B" 或 "A,C" 或 "1,2"
    const answerIdx = answerRaw
      .split(/[,，]/)
      .map((s) => s.trim())
      .filter((s) => s !== '')
      .map((s) => {
        if (/^[0-9]+$/.test(s)) return parseInt(s, 10)
        const up = s.toUpperCase()
        const li = LETTERS.indexOf(up)
        return li >= 0 ? li : -1
      })
      .filter((n) => n >= 0)
    const type = r[5] != null && String(r[5]).includes('多') ? 'MULTIPLE' : 'SINGLE'
    try {
      await saveQuestion({
        title,
        type,
        options: JSON.stringify(options),
        answer: JSON.stringify(answerIdx),
        orderNo: i
      })
      success++
    } catch (e) {
      fail++
    }
  }
  return { success, fail }
}

// ===== 后台：系统设置 =====
export function getSettings() {
  return instance.get('/admin/settings').then((r) => r.data)
}
export function saveSettings(payload) {
  return instance.put('/admin/settings', payload).then((r) => r.data)
}

// ===== 后台：管理员 =====
export function listAdmins() {
  return instance.get('/admin/admins').then((r) => r.data)
}
export function createAdmin(payload) {
  return instance.post('/admin/admins', payload)
}
export function changePassword(id, payload) {
  return instance.put(`/admin/admins/${id}/password`, payload)
}
export function deleteAdmin(id) {
  return instance.delete(`/admin/admins/${id}`)
}

export default instance
