<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import {exportContractExcel, downloadTemplate} from "../utils/ExcelX.js"
import {useRouter} from 'vue-router';
import ColumnHeader from "../components/ColumnHeader.vue";
import {METHOD_OPTIONS, YES_NO_OPTIONS} from "../system/MockX.js";

const router = useRouter();

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

// 筛选条件（顶部筛选栏）
const filters = ref({
    no: '',
    name: '',
    signer: '',
    method: '',
    supplier: '',
    stock: '',
    completed: '',
    dateFrom: '',
    dateTo: '',
})
// 签订人列表（动态数据，由后端下发；）
const signerOptions = ref([])
// 签订方式 / 是否 枚举为固定数据，统一来自 gd.json（经 MockX 导出）
const methodOptions = METHOD_OPTIONS
const yesNoOptions = YES_NO_OPTIONS

// 排序（由表头组件触发，自管理）
const sortKey = ref('')
const sortOrder = ref('')

const AC_list = new AbortController()
const AC_signers = new AbortController()

onMounted(() => {
    loadList()
    loadSigners()
})

onUnmounted(() => {
    AC_list.abort()
    AC_signers.abort()
})

function loadList() {
    loading.value = true
    const paras = {...filters.value, pageNum: page.value, pageSize: pageSize.value}
    Singleton.getInstance(SysX).getContractList(paras, AC_list.signal, () => {
    }, (r, data) => {
        loading.value = false
        if (r) {
            let rows = data.data || []
            // 排序（前端排序，与筛选叠加）
            rows = sortRows(rows)
            list.value = rows
            total.value = rows.length
        }
    })
}

function loadSigners() {
    Singleton.getInstance(SysX).getSignerList(null, AC_signers.signal, () => {
    }, (r, data) => {
        if (r) {
            signerOptions.value = data.data || []
        }
    })
}

/**
 * 排序：根据 sortKey / sortOrder 对行排序
 */
function sortRows(rows) {
    if (!sortKey.value || sortOrder.value === 'none' || sortOrder.value === '') return rows
    const asc = sortOrder.value === 'ascending'
    const numericKeys = ['amount', 'deliveryPayCycle', 'warrantyPayCycle', 'settlementAmount', 'deliveryDays']
    return [...rows].sort((a, b) => {
        let va = a[sortKey.value], vb = b[sortKey.value]
        if (numericKeys.includes(sortKey.value)) {
            va = Number(va);
            vb = Number(vb)
        }
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

function applyFilters() {
    page.value = 1
    loadList()
}

function resetFilters() {
    filters.value = {
        no: '', name: '', signer: '', method: '', supplier: '',
        stock: '', completed: '', dateFrom: '', dateTo: '',
    }
    applyFilters()
}

// 是否 标签：是=success / 否=info
function yesNoTag(val) {
    return val === '是'
        ? {type: 'success', text: '是'}
        : {type: 'info', text: '否'}
}

function formatMoney(v) {
    return Number(v || 0).toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2})
}

/* ---------------- 新增 / 编辑 ---------------- */
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const saving = ref(false)
const dupWarning = ref('')

function emptyForm() {
    return {
        no: '', name: '', signer: '', method: '询比价', supplier: '',
        amount: null, signDate: '',
        payMethod: '', deliveryPayCycle: 3, warrantyPayCycle: 12,
        prepayRatio: 0, deliveryPayRatio: 90, warrantyPayRatio: 10,
        prepayDate: '', deliveryPayDate: '', warrantyPayDate: '',
        stock: '否', completed: '否', settlementAmount: 0, deliveryDays: 30,
        contractTransferDate: '', invoiceTransferDate: '', accountDate: '',
        actualDeliveryDate: '', materialTransferDate: '',
    }
}

const form = ref(emptyForm())

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
    form.value = emptyForm()
    dialogVisible.value = true
}

function openEdit(row) {
    isEdit.value = true
    dupWarning.value = ''
    form.value = {...emptyForm(), ...row}
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
            } else {
                ElMessage.error(data?.msg || '操作失败')
            }
        })
    })
}

/* ---------------- 作废 ---------------- */
function removeContract(row) {
    ElMessageBox.confirm(`确定作废合同 ${row.no} 吗？作废后不可恢复。`, '作废确认', {type: 'warning'}).then(() => {
        Singleton.getInstance(SysX).deleteContract({no: row.no}, new AbortController().signal, () => {
        }, (r, data) => {
            if (r) {
                ElMessage.success('已作废')
                loadList()
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
                        <el-option v-for="s in signerOptions" :key="s.account" :label="s.username" :value="s.account"/>
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
                <el-form-item label="是否已入库">
                    <el-select v-model="filters.stock" placeholder="全部" clearable style="width:110px">
                        <el-option v-for="o in yesNoOptions" :key="o" :label="o" :value="o"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="是否完结">
                    <el-select v-model="filters.completed" placeholder="全部" clearable style="width:110px">
                        <el-option v-for="o in yesNoOptions" :key="o" :label="o" :value="o"/>
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
                <el-button v-hasPermission="['contract:create']" type="primary" @click="openCreate">＋ 新增合同</el-button>
                <el-button v-hasPermission="['contract:import']" @click="gotoImport">📥 Excel 导入</el-button>
                <el-button v-hasPermission="['contract:export']" @click="doExport">📤 导出 Excel</el-button>
                <el-button @click="downloadTemplate">⬇️ 下载模板</el-button>
            </div>
            <div class="toolbar-right">
                <span class="total-tip">共 {{ total }} 条</span>
            </div>
        </div>

        <!-- 表格 -->
        <el-card shadow="never" class="table-card">
            <el-table :data="list" v-loading="loading" border stripe style="width:100%">
                <el-table-column type="index" label="序号" width="60" align="center" fixed="left"/>
                <el-table-column prop="no" width="180" :sortable="false" fixed="left">
                    <template #header>
                        <ColumnHeader label="合同编号" sortable :order="sortKey === 'no' ? sortOrder : ''"
                                      @sort="handleHeaderSort('no')"/>
                    </template>
                    <template #default="{row}"><b style="color:#2563eb">{{ row.no }}</b></template>
                </el-table-column>
                <el-table-column prop="name" min-width="120" :sortable="false" show-overflow-tooltip>
                    <template #header>
                        <ColumnHeader label="合同名称" sortable :order="sortKey === 'name' ? sortOrder : ''"
                                      @sort="handleHeaderSort('name')"/>
                    </template>
                    <template #default="{row}">{{ row.name }}</template>
                </el-table-column>
                <el-table-column prop="signer" width="95" :sortable="false">
                    <template #header>
                        <ColumnHeader label="签订人" sortable :order="sortKey === 'signer' ? sortOrder : ''"
                                      @sort="handleHeaderSort('signer')"/>
                    </template>
                    <template #default="{row}">{{ row.signer }}</template>
                </el-table-column>
                <el-table-column prop="method" width="115" :sortable="false">
                    <template #header>
                        <ColumnHeader label="签订方式" sortable :order="sortKey === 'method' ? sortOrder : ''"
                                      @sort="handleHeaderSort('method')"/>
                    </template>
                    <template #default="{row}">{{ row.method }}</template>
                </el-table-column>
                <el-table-column prop="supplier" min-width="180" :sortable="false" show-overflow-tooltip>
                    <template #header>
                        <ColumnHeader label="供应商" sortable :order="sortKey === 'supplier' ? sortOrder : ''"
                                      @sort="handleHeaderSort('supplier')"/>
                    </template>
                    <template #default="{row}">{{ row.supplier }}</template>
                </el-table-column>
                <el-table-column prop="amount" width="130" :sortable="false" align="right">
                    <template #header>
                        <ColumnHeader label="合同金额(元)" sortable :order="sortKey === 'amount' ? sortOrder : ''"
                                      @sort="handleHeaderSort('amount')"/>
                    </template>
                    <template #default="{row}"><span class="money">{{ formatMoney(row.amount) }}</span></template>
                </el-table-column>
                <el-table-column prop="signDate" width="115" :sortable="false">
                    <template #header>
                        <ColumnHeader label="签订时间" sortable :order="sortKey === 'signDate' ? sortOrder : ''"
                                      @sort="handleHeaderSort('signDate')"/>
                    </template>
                    <template #default="{row}">{{ row.signDate }}</template>
                </el-table-column>
                <el-table-column prop="stock" width="105" :sortable="false" align="center">
                    <template #header>
                        <ColumnHeader label="是否已入库" sortable :order="sortKey === 'stock' ? sortOrder : ''"
                                      @sort="handleHeaderSort('stock')"/>
                    </template>
                    <template #default="{row}">
                        <el-tag :type="yesNoTag(row.stock).type" size="small" effect="plain">
                            {{ yesNoTag(row.stock).text }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="completed" width="105" :sortable="false" align="center">
                    <template #header>
                        <ColumnHeader label="是否完结" sortable :order="sortKey === 'completed' ? sortOrder : ''"
                                      @sort="handleHeaderSort('completed')"/>
                    </template>
                    <template #default="{row}">
                        <el-tag :type="yesNoTag(row.completed).type" size="small" effect="light">
                            {{ yesNoTag(row.completed).text }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="settlementAmount" width="130" :sortable="false" align="right">
                    <template #header>
                        <ColumnHeader label="结算金额(元)" sortable :order="sortKey === 'settlementAmount' ? sortOrder : ''"
                                      @sort="handleHeaderSort('settlementAmount')"/>
                    </template>
                    <template #default="{row}"><span class="money">{{ formatMoney(row.settlementAmount) }}</span></template>
                </el-table-column>
                <el-table-column label="操作" width="140" fixed="right" align="center">
                    <template #default="{row}">
                        <el-button v-hasPermission="['contract:update']" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                        <el-button v-hasPermission="['contract:delete']" link type="danger" size="small" @click="removeContract(row)">作废</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑合同' : '新增合同'" width="960px" destroy-on-close>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="160px">
                <el-divider content-position="left">基本信息</el-divider>
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
                            <el-select v-model="form.signer" placeholder="请选择签订人" filterable style="width:100%">
                                <el-option v-for="s in signerOptions" :key="s.account" :label="s.username" :value="s.account"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="合同签订方式" prop="method">
                            <el-select v-model="form.method" placeholder="请选择" style="width:100%">
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
                </el-row>

                <el-divider content-position="left">付款信息</el-divider>
                <el-row :gutter="16">
                    <el-col :span="24">
                        <el-form-item label="付款方式">
                            <el-input v-model="form.payMethod" placeholder="文字描述，例：货到票到3个月付款"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="到货付款周期(月)">
                            <el-input-number v-model="form.deliveryPayCycle" :min="0" :precision="1" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="质保付款周期(月)">
                            <el-input-number v-model="form.warrantyPayCycle" :min="0" :precision="1" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="预付款比例(%)">
                            <el-input-number v-model="form.prepayRatio" :min="0" :max="100" :precision="2" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="到货款比例(%)">
                            <el-input-number v-model="form.deliveryPayRatio" :min="0" :max="100" :precision="2" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="质保金比例(%)">
                            <el-input-number v-model="form.warrantyPayRatio" :min="0" :max="100" :precision="2" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="预付款日期">
                            <el-date-picker v-model="form.prepayDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="到货款日期">
                            <el-date-picker v-model="form.deliveryPayDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="质保金付款日期">
                            <el-date-picker v-model="form.warrantyPayDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-divider content-position="left">入库与完结</el-divider>
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="是否已入库">
                            <el-select v-model="form.stock" style="width:100%">
                                <el-option v-for="o in yesNoOptions" :key="o" :label="o" :value="o"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="是否完结">
                            <el-select v-model="form.completed" style="width:100%">
                                <el-option v-for="o in yesNoOptions" :key="o" :label="o" :value="o"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="结算金额(元)">
                            <el-input-number v-model="form.settlementAmount" :min="0" :precision="2" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="货期(天)">
                            <el-input-number v-model="form.deliveryDays" :min="0" :step="1" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-divider content-position="left">移交信息</el-divider>
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="合同移交日期">
                            <el-date-picker v-model="form.contractTransferDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="发票移交日期">
                            <el-date-picker v-model="form.invoiceTransferDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="挂账日期">
                            <el-date-picker v-model="form.accountDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="实际到货时间">
                            <el-date-picker v-model="form.actualDeliveryDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="入库资料移交物资日期">
                            <el-date-picker v-model="form.materialTransferDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>

            <el-alert v-if="dupWarning" :title="dupWarning" type="error" :closable="false" show-icon style="margin-top:4px"/>

            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="saveContract">{{ isEdit ? '保存修改' : '保存' }}</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.ledger-page {
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
        .money {
            font-weight: 600;
            font-variant-numeric: tabular-nums;
        }
    }
}
</style>
