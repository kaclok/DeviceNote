<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import {exportContractExcel, downloadTemplate} from "../utils/ExcelX.js"
import {useRouter} from 'vue-router';
import ColumnHeader from "../components/ColumnHeader.vue";
import {METHOD_OPTIONS, PAY_METHOD_OPTIONS} from "../system/MockX.js";
import {useCache, ECacheType} from "@/framework/composable/use/useCache.ts";

const router = useRouter();

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const {wsCache} = useCache()

// 统计
const stats = ref({overdue: 0, soon: 0, normal: 0, paid: 0, total: 0})

// 筛选条件（顶部筛选栏）
const filters = ref({
    no: '',
    name: '',
    signer: '',
    method: '',
    supplier: '',
    payStatus: '',
    stock: '',
    dateFrom: '',
    dateTo: '',
})
// 表头列内搜索条件（作用于当前列）
const colFilters = ref({
    no: '', name: '', signer: '', method: '', supplier: '',
    amount: '', signDate: '', payMethod: '', planDate: '', payStatus: '', stock: '',
})
const signerOptions = ref([])
// 签订方式 / 付款方式 枚举为固定数据，统一来自 gd.json（经 MockX 导出）
const methodOptions = METHOD_OPTIONS
const payMethodOptions = PAY_METHOD_OPTIONS

// 排序（由表头组件触发，自管理）
const sortKey = ref('')
const sortOrder = ref('')

// 当前登录账号权限（空值兜底）
const auth = ref(wsCache.get(ECacheType.PERMS) || [])
function hasPerm(code) {
    return auth.value.includes(code)
}

const AC_list = new AbortController()
const AC_stats = new AbortController()

onMounted(() => {
    loadList()
    loadStats()
})

onUnmounted(() => {
    AC_list.abort()
    AC_stats.abort()
})

function loadList() {
    loading.value = true
    const paras = {...filters.value, ...colFilters.value, page: page.value, pageSize: pageSize.value}
    Singleton.getInstance(SysX).getContractList(paras, AC_list.signal, () => {
    }, (r, data) => {
        loading.value = false
        if (r) {
            let rows = data.data || []
            // 表头列内搜索（前端过滤，作用于当前列）
            rows = filterByColumns(rows)
            // 排序（前端排序，与筛选叠加）
            rows = sortRows(rows)
            list.value = rows
            total.value = rows.length
            // 收集签订人下拉
            signerOptions.value = [...new Set(rows.map(c => c.signer).filter(Boolean))]
        }
    })
}

/**
 * 表头列内搜索：对当前生效的列筛选条件做模糊匹配
 */
function filterByColumns(rows) {
    const active = Object.entries(colFilters.value).filter(([, v]) => v !== '' && v != null)
    if (active.length === 0) return rows
    return rows.filter(row => {
        return active.every(([key, kw]) => {
            let val = row[key]
            if (val == null) return false
            return String(val).toLowerCase().includes(String(kw).toLowerCase())
        })
    })
}

/**
 * 排序：根据 sortKey / sortOrder 对行排序
 */
function sortRows(rows) {
    if (!sortKey.value || sortOrder.value === 'none' || sortOrder.value === '') return rows
    const asc = sortOrder.value === 'ascending'
    return [...rows].sort((a, b) => {
        let va = a[sortKey.value], vb = b[sortKey.value]
        if (sortKey.value === 'amount' || sortKey.value === 'payCycleMonths') { va = Number(va); vb = Number(vb) }
        if (va == null) return 1
        if (vb == null) return -1
        return (va < vb ? -1 : va > vb ? 1 : 0) * (asc ? 1 : -1)
    })
}

// 表头排序点击（来自 ColumnHeader）
function handleHeaderSort(prop) {
    if (sortKey.value === prop) {
        // 升序 -> 降序 -> 取消
        sortOrder.value = sortOrder.value === 'ascending' ? 'descending' : (sortOrder.value === 'descending' ? '' : 'ascending')
        if (sortOrder.value === '') sortKey.value = ''
    } else {
        sortKey.value = prop
        sortOrder.value = 'ascending'
    }
    loadList()
}

// 表头列内搜索确认
function handleColSearch(prop, keyword) {
    colFilters.value[prop] = keyword
    loadList()
}

// 表头列内搜索清空
function handleColClear(prop) {
    colFilters.value[prop] = ''
    loadList()
}

function loadStats() {
    Singleton.getInstance(SysX).getStats({}, AC_stats.signal, () => {
    }, (r, data) => {
        if (r) stats.value = data.data || stats.value
    })
}

function applyFilters() {
    page.value = 1
    loadList()
}

function resetFilters() {
    filters.value = {no: '', name: '', signer: '', method: '', supplier: '', payStatus: '', stock: '', dateFrom: '', dateTo: ''}
    applyFilters()
}

// 付款状态标签
function statusTag(status) {
    const map = {
        '已超期': {type: 'danger', text: '已超期'},
        '即将超期': {type: 'warning', text: '即将超期'},
        '未到期': {type: 'primary', text: '未到期'},
        '已付款': {type: 'success', text: '已付款'},
    }
    return map[status] || {type: 'info', text: status}
}

function stockTag(stock) {
    const map = {
        '已入库': {type: 'success', text: '已入库'},
        '未入库': {type: 'info', text: '未入库'},
        '部分入库': {type: 'warning', text: '部分入库'},
    }
    return map[stock] || {type: 'info', text: stock}
}

function formatMoney(v) {
    return Number(v).toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2})
}

/* ---------------- 新增 / 编辑 ---------------- */
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const saving = ref(false)
const dupWarning = ref('')

const form = ref({
    no: '', name: '', signer: '', method: '网络询比价', supplier: '',
    amount: null, signDate: '', payMethod: '货到票到3个月付款', payCycleMonths: 3,
    plannedAmount: null, stock: '未入库', remark: '',
})

const rules = {
    no: [{required: true, message: '请输入合同编号', trigger: 'blur'}],
    name: [{required: true, message: '请输入合同名称', trigger: 'blur'}],
    signer: [{required: true, message: '请输入签订人', trigger: 'blur'}],
    supplier: [{required: true, message: '请输入供应商', trigger: 'blur'}],
    amount: [{required: true, message: '请输入合同金额', trigger: 'blur'}],
    signDate: [{required: true, message: '请选择签订时间', trigger: 'change'}],
}

// 编号唯一性实时校验
let AC_checkNo = new AbortController()
function checkNoDup() {
    const no = form.value.no.trim()
    dupWarning.value = ''
    if (!no || isEdit.value) return
    AC_checkNo.abort()
    AC_checkNo = new AbortController()
    Singleton.getInstance(SysX).checkNoExists({no}, AC_checkNo.signal, () => {
    }, (r, data) => {
        if (r && data.data?.data) {
            dupWarning.value = `合同编号 ${no} 已存在，禁止重复录入！`
        }
    })
}

function openCreate() {
    isEdit.value = false
    dupWarning.value = ''
    form.value = {
        no: '', name: '', signer: '', method: '网络询比价', supplier: '',
        amount: null, signDate: '', payMethod: '货到票到3个月付款', payCycleMonths: 3,
        plannedAmount: null, stock: '未入库', remark: '',
    }
    dialogVisible.value = true
}

function openEdit(row) {
    isEdit.value = true
    dupWarning.value = ''
    form.value = {...row, plannedAmount: row.plannedAmount ?? row.amount}
    dialogVisible.value = true
}

function saveContract() {
    formRef.value.validate(valid => {
        if (!valid) return
        if (dupWarning.value && !isEdit.value) {
            ElMessage.warning(dupWarning.value)
            return
        }
        saving.value = true
        const paras = {...form.value}
        const fn = isEdit.value ? Singleton.getInstance(SysX).updateContract : Singleton.getInstance(SysX).createContract
        fn(paras, new AbortController().signal, () => {
        }, (r, data) => {
            saving.value = false
            if (r) {
                if (data.data?.duplicate) {
                    ElMessage.error(data.msg)
                    return
                }
                ElMessage.success(isEdit.value ? '修改成功' : '录入成功')
                dialogVisible.value = false
                loadList()
                loadStats()
            } else {
                ElMessage.error(data?.msg || '操作失败')
            }
        })
    })
}

// 付款周期变化时自动重算计划付款日期（预览）
const planDatePreview = computed(() => {
    if (!form.value.signDate) return ''
    const d = new Date(form.value.signDate)
    d.setMonth(d.getMonth() + Number(form.value.payCycleMonths || 0))
    const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, '0'), day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
})

/* ---------------- 作废 ---------------- */
function removeContract(row) {
    ElMessageBox.confirm(`确定作废合同 ${row.no} 吗？作废后不可恢复。`, '作废确认', {type: 'warning'}).then(() => {
        Singleton.getInstance(SysX).deleteContract({no: row.no}, new AbortController().signal, () => {
        }, (r, data) => {
            if (r) {
                ElMessage.success('已作废')
                loadList()
                loadStats()
            }
        })
    }).catch(() => {
    })
}

/* ---------------- 导出 ---------------- */
function doExport() {
    exportContractExcel(list.value)
    ElMessage.success(`已导出 ${list.value.length} 条合同`)
}

/* ---------------- 跳转导入页 ---------------- */
function gotoImport() {
    router.push({name: 'home_import'})
}
</script>

<template>
    <div class="ledger-page">
        <!-- 统计卡 -->
        <div class="stats">
            <div class="stat stat-danger">
                <div class="stat-ic">⚠️</div>
                <div>
                    <div class="stat-val">{{ stats.overdue }}</div>
                    <div class="stat-lbl">已超期</div>
                    <div class="stat-sub">需立即处理</div>
                </div>
            </div>
            <div class="stat stat-warn">
                <div class="stat-ic">⏳</div>
                <div>
                    <div class="stat-val">{{ stats.soon }}</div>
                    <div class="stat-lbl">即将超期</div>
                    <div class="stat-sub">7 天内到期</div>
                </div>
            </div>
            <div class="stat stat-info">
                <div class="stat-ic">📅</div>
                <div>
                    <div class="stat-val">{{ stats.normal }}</div>
                    <div class="stat-lbl">未到期</div>
                    <div class="stat-sub">正常履约中</div>
                </div>
            </div>
            <div class="stat stat-ok">
                <div class="stat-ic">💰</div>
                <div>
                    <div class="stat-val">{{ stats.paid }}</div>
                    <div class="stat-lbl">已付款</div>
                    <div class="stat-sub">共 {{ stats.total }} 份合同</div>
                </div>
            </div>
        </div>

        <!-- 筛选卡片 -->
        <el-card shadow="never" class="filter-card">
            <el-form :inline="true" class="filter-form">
                <el-form-item label="合同编号">
                    <el-input v-model="filters.no" placeholder="如 SMLJ-CG-CL" clearable style="width:150px" @keyup.enter="applyFilters"/>
                </el-form-item>
                <el-form-item label="合同名称">
                    <el-input v-model="filters.name" placeholder="模糊搜索" clearable style="width:130px" @keyup.enter="applyFilters"/>
                </el-form-item>
                <el-form-item label="签订人">
                    <el-select v-model="filters.signer" placeholder="全部" clearable style="width:110px">
                        <el-option v-for="s in signerOptions" :key="s" :label="s" :value="s"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="签订方式">
                    <el-select v-model="filters.method" placeholder="全部" clearable style="width:130px">
                        <el-option v-for="m in methodOptions" :key="m" :label="m" :value="m"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="供应商">
                    <el-input v-model="filters.supplier" placeholder="模糊搜索" clearable style="width:130px" @keyup.enter="applyFilters"/>
                </el-form-item>
                <el-form-item label="付款状态">
                    <el-select v-model="filters.payStatus" placeholder="全部" clearable style="width:120px">
                        <el-option label="已超期" value="已超期"/>
                        <el-option label="即将超期" value="即将超期"/>
                        <el-option label="未到期" value="未到期"/>
                        <el-option label="已付款" value="已付款"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="入库状态">
                    <el-select v-model="filters.stock" placeholder="全部" clearable style="width:120px">
                        <el-option label="已入库" value="已入库"/>
                        <el-option label="未入库" value="未入库"/>
                        <el-option label="部分入库" value="部分入库"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="签订时间">
                    <el-date-picker v-model="filters.dateFrom" type="date" placeholder="开始" value-format="YYYY-MM-DD" style="width:130px"/>
                    <span style="margin:0 6px;color:#94a3b8">至</span>
                    <el-date-picker v-model="filters.dateTo" type="date" placeholder="结束" value-format="YYYY-MM-DD" style="width:130px"/>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="applyFilters">查询</el-button>
                    <el-button @click="resetFilters">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 工具栏 -->
        <div class="toolbar">
            <div class="toolbar-left">
                <el-button v-if="hasPerm('contract.create')" type="primary" @click="openCreate">＋ 新增合同</el-button>
                <el-button v-if="hasPerm('contract.import')" @click="gotoImport">📥 Excel 导入</el-button>
                <el-button v-if="hasPerm('contract.export')" @click="doExport">📤 导出 Excel</el-button>
                <el-button @click="downloadTemplate">⬇️ 下载模板</el-button>
            </div>
            <div class="toolbar-right">
                <span class="total-tip">共 {{ total }} 条</span>
            </div>
        </div>

        <!-- 表格 -->
        <el-card shadow="never" class="table-card">
            <el-table :data="list" v-loading="loading" border stripe
                      :row-class-name="({row}) => row.payStatus === '已超期' ? 'row-overdue' : (row.payStatus === '即将超期' ? 'row-soon' : '')"
                      style="width:100%">
                <el-table-column type="index" label="序号" width="60" align="center"/>
                <el-table-column prop="no" width="180" :sortable="false">
                    <template #header>
                        <ColumnHeader label="合同编号" sortable :order="sortKey === 'no' ? sortOrder : ''"
                                      :value="colFilters.no"
                                      @sort="handleHeaderSort('no')" @confirm="v => handleColSearch('no', v)" @clear="handleColClear('no')"/>
                    </template>
                    <template #default="{row}"><b style="color:#2563eb">{{ row.no }}</b></template>
                </el-table-column>
                <el-table-column prop="name" min-width="120" :sortable="false" show-overflow-tooltip>
                    <template #header>
                        <ColumnHeader label="合同名称" sortable :order="sortKey === 'name' ? sortOrder : ''"
                                      :value="colFilters.name"
                                      @sort="handleHeaderSort('name')" @confirm="v => handleColSearch('name', v)" @clear="handleColClear('name')"/>
                    </template>
                    <template #default="{row}">{{ row.name }}</template>
                </el-table-column>
                <el-table-column prop="signer" width="95" :sortable="false">
                    <template #header>
                        <ColumnHeader label="签订人" sortable :order="sortKey === 'signer' ? sortOrder : ''"
                                      :value="colFilters.signer"
                                      @sort="handleHeaderSort('signer')" @confirm="v => handleColSearch('signer', v)" @clear="handleColClear('signer')"/>
                    </template>
                    <template #default="{row}">{{ row.signer }}</template>
                </el-table-column>
                <el-table-column prop="method" width="115" :sortable="false">
                    <template #header>
                        <ColumnHeader label="签订方式" sortable :order="sortKey === 'method' ? sortOrder : ''"
                                      :value="colFilters.method"
                                      @sort="handleHeaderSort('method')" @confirm="v => handleColSearch('method', v)" @clear="handleColClear('method')"/>
                    </template>
                    <template #default="{row}">{{ row.method }}</template>
                </el-table-column>
                <el-table-column prop="supplier" min-width="180" :sortable="false" show-overflow-tooltip>
                    <template #header>
                        <ColumnHeader label="供应商" sortable :order="sortKey === 'supplier' ? sortOrder : ''"
                                      :value="colFilters.supplier"
                                      @sort="handleHeaderSort('supplier')" @confirm="v => handleColSearch('supplier', v)" @clear="handleColClear('supplier')"/>
                    </template>
                    <template #default="{row}">{{ row.supplier }}</template>
                </el-table-column>
                <el-table-column prop="amount" width="130" :sortable="false" align="right">
                    <template #header>
                        <ColumnHeader label="合同金额(元)" sortable :order="sortKey === 'amount' ? sortOrder : ''"
                                      :value="colFilters.amount"
                                      @sort="handleHeaderSort('amount')" @confirm="v => handleColSearch('amount', v)" @clear="handleColClear('amount')"/>
                    </template>
                    <template #default="{row}"><span class="money">{{ formatMoney(row.amount) }}</span></template>
                </el-table-column>
                <el-table-column prop="signDate" width="115" :sortable="false">
                    <template #header>
                        <ColumnHeader label="签订时间" sortable :order="sortKey === 'signDate' ? sortOrder : ''"
                                      :value="colFilters.signDate"
                                      @sort="handleHeaderSort('signDate')" @confirm="v => handleColSearch('signDate', v)" @clear="handleColClear('signDate')"/>
                    </template>
                    <template #default="{row}">{{ row.signDate }}</template>
                </el-table-column>
                <el-table-column prop="payMethod" min-width="160" :sortable="false" show-overflow-tooltip>
                    <template #header>
                        <ColumnHeader label="付款方式" sortable :order="sortKey === 'payMethod' ? sortOrder : ''"
                                      :value="colFilters.payMethod"
                                      @sort="handleHeaderSort('payMethod')" @confirm="v => handleColSearch('payMethod', v)" @clear="handleColClear('payMethod')"/>
                    </template>
                    <template #default="{row}">{{ row.payMethod }}</template>
                </el-table-column>
                <el-table-column prop="planDate" width="120" :sortable="false" align="center">
                    <template #header>
                        <ColumnHeader label="计划付款日期" sortable :order="sortKey === 'planDate' ? sortOrder : ''"
                                      :value="colFilters.planDate"
                                      @sort="handleHeaderSort('planDate')" @confirm="v => handleColSearch('planDate', v)" @clear="handleColClear('planDate')"/>
                    </template>
                    <template #default="{row}">
                        <div>{{ row.planDate || '-' }}</div>
                    </template>
                </el-table-column>
                <el-table-column prop="payStatus" width="110" :sortable="false" align="center">
                    <template #header>
                        <ColumnHeader label="付款状态" sortable :order="sortKey === 'payStatus' ? sortOrder : ''"
                                      :value="colFilters.payStatus"
                                      @sort="handleHeaderSort('payStatus')" @confirm="v => handleColSearch('payStatus', v)" @clear="handleColClear('payStatus')"/>
                    </template>
                    <template #default="{row}">
                        <el-tag :type="statusTag(row.payStatus).type" size="small" effect="light">
                            {{ statusTag(row.payStatus).text }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="stock" width="105" :sortable="false" align="center">
                    <template #header>
                        <ColumnHeader label="入库状态" sortable :order="sortKey === 'stock' ? sortOrder : ''"
                                      :value="colFilters.stock"
                                      @sort="handleHeaderSort('stock')" @confirm="v => handleColSearch('stock', v)" @clear="handleColClear('stock')"/>
                    </template>
                    <template #default="{row}">
                        <el-tag :type="stockTag(row.stock).type" size="small" effect="plain">
                            {{ stockTag(row.stock).text }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="140" fixed="right" align="center">
                    <template #default="{row}">
                        <el-button v-if="hasPerm('contract.update')" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                        <el-button v-if="hasPerm('contract.delete')" link type="danger" size="small" @click="removeContract(row)">作废</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑合同' : '新增合同'" width="680px" destroy-on-close>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="合同编号" prop="no">
                            <el-input v-model="form.no" placeholder="例：SMLJ-CG-CL-26330" :disabled="isEdit" @input="checkNoDup"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="合同名称" prop="name">
                            <el-input v-model="form.name" placeholder="例：螺栓"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="签订人" prop="signer">
                            <el-input v-model="form.signer" placeholder="签订人姓名"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="签订方式" prop="method">
                            <el-select v-model="form.method" style="width:100%">
                                <el-option v-for="m in methodOptions" :key="m" :label="m" :value="m"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="供应商" prop="supplier">
                            <el-input v-model="form.supplier" placeholder="供应商全称"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="合同金额(元)" prop="amount">
                            <el-input-number v-model="form.amount" :min="0" :precision="2" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="签订时间" prop="signDate">
                            <el-date-picker v-model="form.signDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="付款方式" prop="payMethod">
                            <el-select v-model="form.payMethod" style="width:100%" @change="() => {form.plannedAmount = form.amount}">
                                <el-option v-for="m in payMethodOptions" :key="m" :label="m" :value="m"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="付款周期(月)" prop="payCycleMonths">
                            <el-input-number v-model="form.payCycleMonths" :min="0" :max="60" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="计划付款金额">
                            <el-input-number v-model="form.plannedAmount" :min="0" :precision="2" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="是否已入库">
                            <el-select v-model="form.stock" style="width:100%">
                                <el-option label="已入库" value="已入库"/>
                                <el-option label="未入库" value="未入库"/>
                                <el-option label="部分入库" value="部分入库"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="备注">
                            <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填"/>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>

            <el-alert v-if="dupWarning" :title="dupWarning" type="error" :closable="false" show-icon style="margin-top:4px"/>
            <el-alert v-else-if="planDatePreview && form.signDate" :title="`计划付款日期预计为：${planDatePreview}（签订时间 + 付款周期）`"
                      type="info" :closable="false" show-icon style="margin-top:4px"/>

            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="saveContract">{{ isEdit ? '保存修改' : '保存' }}</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.ledger-page {
    .stats {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 14px;
        margin-bottom: 16px;

        .stat {
            background: #fff;
            border-radius: 10px;
            padding: 16px 18px;
            display: flex;
            align-items: center;
            gap: 14px;
            box-shadow: 0 1px 3px rgba(15, 23, 42, .06);

            .stat-ic {
                width: 46px;
                height: 46px;
                border-radius: 12px;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 22px;
            }

            .stat-val {
                font-size: 24px;
                font-weight: 700;
                line-height: 1.2;
            }

            .stat-lbl {
                font-size: 12px;
                color: #475569;
            }

            .stat-sub {
                font-size: 11px;
                color: #94a3b8;
            }
        }

        .stat-danger .stat-ic {background: #fef2f2}
        .stat-warn .stat-ic {background: #fff7ed}
        .stat-info .stat-ic {background: #eff6ff}
        .stat-ok .stat-ic {background: #f0fdf4}
    }

    .filter-card {
        margin-bottom: 14px;

        :deep(.el-card__body) {
            padding: 16px 16px 0;

            .filter-form .el-form-item {
                margin-bottom: 12px;
                margin-right: 14px;
            }
        }
    }

    .toolbar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 12px;

        .total-tip {
            font-size: 13px;
            color: #64748b;
        }
    }

    .table-card {
        :deep(.row-overdue) {
            td.el-table__cell {background: #fef2f2 !important}
        }

        :deep(.row-soon) {
            td.el-table__cell {background: #fff7ed !important}
        }

        .money {
            font-weight: 600;
            font-variant-numeric: tabular-nums;
        }
    }
}
</style>
