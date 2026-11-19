<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <div class="login-title">后台管理登录</div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" size="large" show-password @keyup.enter="onLogin" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onLogin">登录</el-button>
      </el-form>
      <div class="back">
        <el-link type="primary" @click="goHome">返回访客入口</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '../../store/auth'

const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onLogin() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await login(form.username, form.password)
      window.location.hash = '/admin/settings'
    } catch (e) {
      ElMessage.error('用户名或密码错误')
    } finally {
      loading.value = false
    }
  })
}

function goHome() {
  window.location.hash = '/'
}
</script>

<style scoped>
.login-wrap {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
}
.login-card {
  width: 360px;
  padding: 10px 16px;
}
.login-title {
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  margin: 12px 0 24px;
}
.back {
  text-align: center;
  margin-top: 12px;
}
</style>
