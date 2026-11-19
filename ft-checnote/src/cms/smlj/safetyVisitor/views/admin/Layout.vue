<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">{{ appName }}</div>
      <el-menu :default-active="activeMenu" router class="menu" background-color="#001529" text-color="#fff" active-text-color="#409eff">
        <el-menu-item index="/admin/settings">
          <el-icon><Setting /></el-icon><span>系统设置</span>
        </el-menu-item>
        <el-menu-item index="/admin/dept-staff">
          <el-icon><UserFilled /></el-icon><span>部门与人员</span>
        </el-menu-item>
        <el-menu-item index="/admin/visitors">
          <el-icon><List /></el-icon><span>访客台账</span>
        </el-menu-item>
        <el-menu-item index="/admin/questions">
          <el-icon><EditPen /></el-icon><span>题库管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/admins">
          <el-icon><Key /></el-icon><span>管理员管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="cur-title">{{ currentTitle }}</span>
        <span class="right">
          <el-icon><User /></el-icon>
          <span class="uname">{{ username }}</span>
          <el-button text type="primary" @click="onLogout">退出</el-button>
        </span>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { getUsername, logout } from '../../store/auth'

const route = useRoute()
const router = useRouter()
const username = ref(getUsername())
const appName = ref('访客安全准入系统')

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')

function onLogout() {
  ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    .then(() => {
      logout()
      window.location.hash = '/admin/login'
    })
    .catch(() => {})
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: #001529;
  overflow: hidden;
}
.logo {
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  text-align: center;
  padding: 18px 0;
  border-bottom: 1px solid #1f2d3d;
}
.menu {
  border-right: none;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #eaeaea;
}
.cur-title {
  font-size: 16px;
  font-weight: 600;
}
.right {
  display: flex;
  align-items: center;
  gap: 6px;
}
.uname {
  margin-right: 8px;
}
.main {
  background: #f0f2f5;
  padding: 16px;
}
</style>
