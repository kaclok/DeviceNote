import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '../store/auth'

import VisitorEntry from '../views/VisitorEntry.vue'
import Login from '../views/admin/Login.vue'
import Layout from '../views/admin/Layout.vue'
import SystemSettings from '../components/admin/SystemSettings.vue'
import DeptStaff from '../components/admin/DeptStaff.vue'
import VisitorLedger from '../components/admin/VisitorLedger.vue'
import QuestionBank from '../components/admin/QuestionBank.vue'
import AdminManage from '../components/admin/AdminManage.vue'

const routes = [
  { path: '/', name: 'VisitorEntry', component: VisitorEntry, meta: { public: true } },
  { path: '/admin/login', name: 'Login', component: Login, meta: { public: true } },
  {
    path: '/admin',
    component: Layout,
    redirect: '/admin/settings',
    children: [
      { path: 'settings', name: 'SystemSettings', component: SystemSettings, meta: { title: '系统设置' } },
      { path: 'dept-staff', name: 'DeptStaff', component: DeptStaff, meta: { title: '部门与人员' } },
      { path: 'visitors', name: 'VisitorLedger', component: VisitorLedger, meta: { title: '访客台账' } },
      { path: 'questions', name: 'QuestionBank', component: QuestionBank, meta: { title: '题库管理' } },
      { path: 'admins', name: 'AdminManage', component: AdminManage, meta: { title: '管理员管理' } }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.public) {
    next()
    return
  }
  const token = getToken()
  if (!token) {
    next({ path: '/admin/login' })
  } else {
    next()
  }
})

export default router
