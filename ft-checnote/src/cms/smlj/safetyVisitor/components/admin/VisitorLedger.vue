<template>
  <el-card>
    <template #header><b>访客台账</b></template>

    <el-form inline size="small" class="filter">
      <el-form-item label="部门">
        <el-select v-model="filters.hostId" placeholder="全部" clearable @change="load" style="width:150px">
          <el-option v-for="h in hosts" :key="h.id" :label="h.name" :value="h.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" placeholder="全部" clearable @change="load" style="width:130px">
          <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker v-model="filters.date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" @change="load" style="width:150px" />
      </el-form-item>
      <el-form-item label="关键字">
        <el-input v-model="filters.keyword" placeholder="姓名/手机/证件/车牌/编号" clearable @keyup.enter="load" style="width:200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="success" @click="onExport">导出 Excel(CSV)</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" border size="small" v-loading="loading">
      <el-table-column prop="code" label="访客编号" width="130" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column label="部门" width="120">
        <template #default="{ row }">{{ hostName(row.hostId) }}</template>
      </el-table-column>
      <el-table-column label="被访问人" width="100">
        <template #default="{ row }">{{ staffName(row.staffId) }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="120" />
      <el-table-column prop="idCard" label="身份证" width="160" />
      <el-table-column prop="plate" label="车牌" width="100" />
      <el-table-column prop="examScore" label="成绩" width="70" />
      <el-table-column label="通过" width="70">
        <template #default="{ row }">
          <el-tag :type="row.passed ? 'success' : 'info'" size="small">{{ row.passed ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-select v-model="row.status" size="small" @change="(v) => changeStatus(row, v)">
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listVisitors, updateVisitor, deleteVisitor, exportVisitors, listHostsAdmin, listStaffAdmin } from '../../api'

const statusOptions = ['PENDING', 'DONE', 'PASSED', 'REJECTED', 'LEFT']
const list = ref([])
const hosts = ref([])
const staff = ref([])
const loading = ref(false)
const filters = reactive({ hostId: '', staffId: '', status: '', date: '', keyword: '' })

const hostName = (id) => (hosts.value.find((h) => h.id === id) || {}).name || '-'
const staffName = (id) => (staff.value.find((s) => s.id === id) || {}).name || '-'

async function loadHostsStaff() {
  hosts.value = await listHostsAdmin()
  staff.value = await listStaffAdmin()
}

async function load() {
  loading.value = true
  try {
    const params = {}
    if (filters.hostId) params.hostId = filters.hostId
    if (filters.status) params.status = filters.status
    if (filters.date) params.date = filters.date
    if (filters.keyword) params.keyword = filters.keyword
    list.value = await listVisitors(params)
  } finally {
    loading.value = false
  }
}

function reset() {
  filters.hostId = ''
  filters.status = ''
  filters.date = ''
  filters.keyword = ''
  load()
}

async function changeStatus(row, val) {
  try {
    await updateVisitor(row.id, { status: val })
    ElMessage.success('状态已更新')
  } catch (e) {
    ElMessage.error('更新失败')
    load()
  }
}

async function del(row) {
  await ElMessageBox.confirm(`确认删除访客「${row.name}」的记录？`, '提示', { type: 'warning' })
  await deleteVisitor(row.id)
  await load()
}

async function onExport() {
  const params = {}
  if (filters.hostId) params.hostId = filters.hostId
  if (filters.status) params.status = filters.status
  if (filters.date) params.date = filters.date
  if (filters.keyword) params.keyword = filters.keyword
  const data = await exportVisitors(params)
  if (!data || !data.length) {
    return ElMessage.warning('无数据可导出')
  }
  const header = ['访客编号', '姓名', '部门', '被访问人', '手机号', '身份证', '车牌', '成绩', '通过', '状态', '创建时间']
  const rows = data.map((v) => [
    v.code, v.name, hostName(v.hostId), staffName(v.staffId), v.phone, v.idCard, v.plate,
    v.examScore, v.passed ? '是' : '否', v.status, v.createTime
  ])
  const csv = [header, ...rows].map((r) => r.map((c) => `"${c == null ? '' : String(c).replace(/"/g, '""')}"`).join(',')).join('\r\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '访客台账_' + new Date().toISOString().slice(0, 10) + '.csv'
  a.click()
  URL.revokeObjectURL(a.href)
}

onMounted(async () => {
  await loadHostsStaff()
  await load()
})
</script>

<style scoped>
.filter {
  margin-bottom: 12px;
}
</style>
