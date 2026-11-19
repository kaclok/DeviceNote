<template>
  <el-card>
    <template #header>
      <div class="hd">
        <b>管理员管理</b>
        <el-button size="small" type="primary" @click="openAdd">新增管理员</el-button>
      </div>
    </template>

    <el-table :data="list" border size="small">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="role" label="角色" width="100" />
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openPwd(row)">改密码</el-button>
          <el-button link type="danger" :disabled="row.username === currentUser" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="addVisible" title="新增管理员" width="400px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="addForm.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="addForm.password" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAdd">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdVisible" title="修改密码" width="400px">
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="账号"><el-input :model-value="pwdForm.username" disabled /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwdForm.password" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="savePwd">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAdmins, createAdmin, changePassword, deleteAdmin } from '../../api'
import { getUsername } from '../../store/auth'

const list = ref([])
const currentUser = getUsername()

const addVisible = ref(false)
const addForm = reactive({ username: '', password: '' })

const pwdVisible = ref(false)
const pwdForm = reactive({ id: '', username: '', password: '' })

async function load() {
  list.value = await listAdmins()
}

function openAdd() {
  addForm.username = ''
  addForm.password = ''
  addVisible.value = true
}
async function saveAdd() {
  if (!addForm.username.trim()) return ElMessage.warning('请输入用户名')
  if (!addForm.password || addForm.password.length < 6) return ElMessage.warning('密码至少 6 位')
  await createAdmin({ username: addForm.username, password: addForm.password })
  addVisible.value = false
  await load()
  ElMessage.success('已创建')
}

function openPwd(row) {
  pwdForm.id = row.id
  pwdForm.username = row.username
  pwdForm.password = ''
  pwdVisible.value = true
}
async function savePwd() {
  if (!pwdForm.password || pwdForm.password.length < 6) return ElMessage.warning('密码至少 6 位')
  await changePassword(pwdForm.id, { password: pwdForm.password })
  pwdVisible.value = false
  ElMessage.success('密码已修改')
}

async function del(row) {
  if (row.username === currentUser) return ElMessage.warning('不能删除当前登录账号')
  await ElMessageBox.confirm(`确认删除管理员「${row.username}」？`, '提示', { type: 'warning' })
  await deleteAdmin(row.id)
  await load()
}

onMounted(load)
</script>

<style scoped>
.hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
