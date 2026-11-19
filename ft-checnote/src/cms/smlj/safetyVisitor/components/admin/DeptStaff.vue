<template>
  <div>
    <el-row :gutter="16">
      <!-- 左：部门 -->
      <el-col :span="10">
        <el-card>
          <template #header>
            <div class="hd"><b>被访问部门</b><el-button size="small" type="primary" @click="openHostDialog()">新增部门</el-button></div>
          </template>
          <el-table :data="hosts" size="small" border>
            <el-table-column prop="name" label="部门名称" />
            <el-table-column prop="description" label="描述" show-overflow-tooltip />
            <el-table-column label="操作" width="130">
              <template #default="{ row }">
                <el-button link type="primary" @click="openHostDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="delHost(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右：人员 -->
      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="hd">
              <b>被访问人员</b>
              <span>
                <el-button size="small" type="primary" @click="openStaffDialog()">新增人员</el-button>
                <el-button size="small" @click="importStaff">Excel 导入</el-button>
                <input ref="staffFile" type="file" accept=".xlsx,.xls" style="display:none" @change="onStaffFile" />
              </span>
            </div>
          </template>
          <el-form inline size="small" class="filter">
            <el-form-item label="按部门筛选">
              <el-select v-model="staffHostFilter" placeholder="全部" clearable @change="loadStaff" style="width:160px">
                <el-option v-for="h in hosts" :key="h.id" :label="h.name" :value="h.id" />
              </el-select>
            </el-form-item>
          </el-form>
          <el-table :data="staff" size="small" border>
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column label="部门" width="120">
              <template #default="{ row }">{{ hostName(row.hostId) }}</template>
            </el-table-column>
            <el-table-column prop="phone" label="电话" width="130" />
            <el-table-column prop="position" label="岗位" />
            <el-table-column label="操作" width="130">
              <template #default="{ row }">
                <el-button link type="primary" @click="openStaffDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="delStaff(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 部门弹窗 -->
    <el-dialog v-model="hostVisible" :title="hostForm.id ? '编辑部门' : '新增部门'" width="420px">
      <el-form :model="hostForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="hostForm.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="hostForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="hostVisible = false">取消</el-button>
        <el-button type="primary" @click="saveHostHandler">保存</el-button>
      </template>
    </el-dialog>

    <!-- 人员弹窗 -->
    <el-dialog v-model="staffVisible" :title="staffForm.id ? '编辑人员' : '新增人员'" width="420px">
      <el-form :model="staffForm" label-width="80px">
        <el-form-item label="姓名"><el-input v-model="staffForm.name" /></el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="staffForm.hostId" placeholder="请选择" style="width:100%">
            <el-option v-for="h in hosts" :key="h.id" :label="h.name" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="电话"><el-input v-model="staffForm.phone" /></el-form-item>
        <el-form-item label="岗位"><el-input v-model="staffForm.position" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="staffVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStaffHandler">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listHostsAdmin, saveHost, updateHost, deleteHost, listStaffAdmin, saveStaff, updateStaff, deleteStaff } from '../../api'

const hosts = ref([])
const staff = ref([])
const staffHostFilter = ref('')

const hostVisible = ref(false)
const hostForm = reactive({ id: '', name: '', description: '' })

const staffVisible = ref(false)
const staffForm = reactive({ id: '', name: '', hostId: '', phone: '', position: '' })
const staffFile = ref(null)

const hostName = (id) => {
  const h = hosts.value.find((x) => x.id === id)
  return h ? h.name : '-'
}

async function loadHosts() {
  hosts.value = await listHostsAdmin()
}
async function loadStaff() {
  staff.value = await listStaffAdmin(staffHostFilter.value || undefined)
}

function openHostDialog(row) {
  hostForm.id = row ? row.id : ''
  hostForm.name = row ? row.name : ''
  hostForm.description = row ? row.description : ''
  hostVisible.value = true
}
async function saveHostHandler() {
  if (!hostForm.name) return ElMessage.warning('请输入部门名称')
  const payload = { name: hostForm.name, description: hostForm.description }
  if (hostForm.id) {
    await updateHost(hostForm.id, payload)
  } else {
    await saveHost(payload)
  }
  hostVisible.value = false
  await loadHosts()
  ElMessage.success('已保存')
}
async function delHost(row) {
  await ElMessageBox.confirm(`确认删除部门「${row.name}」？`, '提示', { type: 'warning' })
  await deleteHost(row.id)
  await loadHosts()
  await loadStaff()
}

function openStaffDialog(row) {
  staffForm.id = row ? row.id : ''
  staffForm.name = row ? row.name : ''
  staffForm.hostId = row ? row.hostId : ''
  staffForm.phone = row ? row.phone : ''
  staffForm.position = row ? row.position : ''
  staffVisible.value = true
}
async function saveStaffHandler() {
  if (!staffForm.name) return ElMessage.warning('请输入姓名')
  const payload = { name: staffForm.name, hostId: staffForm.hostId, phone: staffForm.phone, position: staffForm.position }
  if (staffForm.id) {
    await updateStaff(staffForm.id, payload)
  } else {
    await saveStaff(payload)
  }
  staffVisible.value = false
  await loadStaff()
  ElMessage.success('已保存')
}
async function delStaff(row) {
  await ElMessageBox.confirm(`确认删除人员「${row.name}」？`, '提示', { type: 'warning' })
  await deleteStaff(row.id)
  await loadStaff()
}

function importStaff() {
  staffFile.value.click()
}
async function onStaffFile(e) {
  const file = e.target.files[0]
  if (!file) return
  try {
    const XLSX = await import('xlsx')
    const buf = await file.arrayBuffer()
    const wb = XLSX.read(buf, { type: 'array' })
    const sheet = wb.Sheets[wb.SheetNames[0]]
    const rows = XLSX.utils.sheet_to_json(sheet, { header: 1 })
    const startIdx = rows.length && rows[0].some((c) => c && String(c).includes('姓名')) ? 1 : 0
    let ok = 0
    for (let i = startIdx; i < rows.length; i++) {
      const r = rows[i]
      if (!r || !r[0]) continue
      const name = String(r[0]).trim()
      const deptName = r[1] != null ? String(r[1]).trim() : ''
      const host = hosts.value.find((h) => h.name === deptName)
      const payload = {
        name,
        hostId: host ? host.id : '',
        phone: r[2] != null ? String(r[2]).trim() : '',
        position: r[3] != null ? String(r[3]).trim() : ''
      }
      await saveStaff(payload)
      ok++
    }
    ElMessage.success(`成功导入 ${ok} 条`)
    await loadStaff()
  } catch (err) {
    ElMessage.error('导入失败：' + (err.message || err))
  } finally {
    e.target.value = ''
  }
}

onMounted(async () => {
  await loadHosts()
  await loadStaff()
})
</script>

<style scoped>
.hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.filter {
  margin-bottom: 10px;
}
</style>
