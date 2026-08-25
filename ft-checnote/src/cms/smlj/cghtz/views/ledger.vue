<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import {downloadTemplate, exportContractExcel} from "../utils/ExcelX.js"
import {useRouter} from 'vue-router';
import gd from "../data/gd.json"
import hd from "../data/hd.json"

const router = useRouter();

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

// 筛选条件（顶部筛选栏）— 与后端 contractList 的 @RequestParam 保持一致
const filters = ref({
    id: '',
    title: '',
    sign_person: '',
    sign_type: '',
    supplier: '',
    dateFrom: null,
    dateTo: null,
    finish_step: '',
    has_rk: '',
})
// 签订人列表（动态数据，由后端下发；）
const signerOptions = ref([])
// 签订方式枚举为固定数据，统一来自 gd.json（经 MockX 导出）
const methodOptions = gd.methodOptions

// 用 let：服务端分页每次翻页都要发请求，需取消上一次未完成的请求，避免旧响应覆盖新响应
let AC_list = new AbortController()
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
    // 取消上一次未完成的请求，避免快速翻页时旧响应覆盖新响应
    AC_list.abort()
    AC_list = new AbortController()

    loading.value = true
    // 筛选条件 -> 后端 contractList @RequestParam：id/title/sign_person/sign_type/supplier + date_sign 范围（queryBegin/queryEnd）
    const paras = {pageNum: page.value, pageSize: pageSize.value}
    const fm = {
        id: filters.value.id,
        title: filters.value.title,
        sign_person: filters.value.sign_person,
        sign_type: filters.value.sign_type,
        supplier: filters.value.supplier,
        queryBegin: filters.value.dateFrom,
        queryEnd: filters.value.dateTo,
        finish_step: filters.value.finish_step,
        has_rk: filters.value.has_rk,
    }

    // 过滤对象中的空值， 和...fm不一样
    for (const k in fm) {
        const v = fm[k]
        if (v !== '' && v != null) {
            paras[k] = v
        }
    }
    Singleton.getInstance(SysX).getContractList(paras, AC_list.signal, () => {
    }, (r, data) => {
        loading.value = false
        if (r) {
            list.value = data.data.list
            total.value = data.data.list.length
        }
    })
}

// 服务端分页：页码/每页条数变化需重新发请求
function onPageChange(p) {
    page.value = p
    loadList()
}

function onSizeChange(s) {
    pageSize.value = s
    page.value = 1
    loadList()
}

function loadSigners() {
    Singleton.getInstance(SysX).getSignerList(null, AC_signers.signal, () => {
    }, (r, data) => {
        if (r) {
            signerOptions.value = data.data
        }
    })
}

function applyFilters() {
    page.value = 1
    loadList()
}

function resetFilters() {
    filters.value = {
        id: '', title: '', sign_person: '', sign_type: '', supplier: '',
        dateFrom: null, dateTo: null,
        finish_step: '', has_rk: '',
    }
    applyFilters()
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
// 编辑模式：缓存原始合同数据，保存时 merge 避免空值覆盖未展示字段（Experience 718922）
const originalContract = ref({})

function emptyForm() {
    return {
        id: '',
        title: '',
        amount: null,
        date_sign: '',
        sign_person: '',
        sign_type: 0,
        supplier: '',
        pay_type: '',
        paycycle_dh: 0,
        paycycle_zb: 0,
        rate_yfk: 0,
        rate_dhk: 0,
        rate_zbj: 0,
        date_yfk: '',
        date_dhk: '',
        date_zbj: '',
        date_rk: '',
        bz: '',
        settle_amount: 0,
        hq: 0,
        date_htyj: '',
        date_fpyj: '',
        date_actual_dh: '',
        date_ruzlyj: '',
        finish_step: 0,
        has_rk: false,
    }
}

const form = ref(emptyForm())

const rules = {
    id: [{required: true, message: '请输入合同编号', trigger: 'blur'}],
    title: [{required: true, message: '请输入合同名称', trigger: 'blur'}],
    sign_person: [{required: true, message: '请选择签订人', trigger: 'change'}],
    sign_type: [{required: true, message: '请选择签订方式', trigger: 'change'}],
    supplier: [{required: true, message: '请输入供应商', trigger: 'blur'}],
    amount: [{required: true, message: '请输入合同金额', trigger: 'blur'}],
    date_sign: [{required: true, message: '请选择签订时间', trigger: 'change'}],
    paycycle_dh: [{required: true, message: '请输入到货周期', trigger: 'change'}],
    paycycle_zb: [{required: true, message: '请输入质保周期', trigger: 'change'}],
    rate_yfk: [{required: true, message: '请输入预付款比例', trigger: 'change'}],
    rate_dhk: [{required: true, message: '请输入到货款比例', trigger: 'change'}],
    rate_zbj: [{required: true, message: '请输入质保金比例', trigger: 'change'}],
}

// 编号唯一性实时校验
let AC_checkId = new AbortController()

function checkIdDup() {
    const id = String(form.value.id || '').trim()
    dupWarning.value = ''
    if (!id || isEdit.value) return
    AC_checkId.abort()
    AC_checkId = new AbortController()
    Singleton.getInstance(SysX).checkNoExists({id}, AC_checkId.signal, () => {
    }, (r, data) => {
        if (r) {
            // MockX.checkNoExists 返回 {code,msg,data: true/false}
            // 后端兜底也返回 {code,msg,data: succ.data.data != null}
            const exists = data?.data?.data === true || data?.data === true
            if (exists) dupWarning.value = `合同编号 ${id} 已存在，禁止重复录入！`
        }
    })
}

function openCreate() {
    isEdit.value = false
    dupWarning.value = ''
    originalContract.value = {}
    form.value = emptyForm()
    dialogVisible.value = true
}

function openEdit(row) {
    isEdit.value = true
    dupWarning.value = ''
    // 缓存原始数据（merge 用），避免对话框中未展示/未编辑字段保存为 null
    originalContract.value = {...(row || {})}
    const base = emptyForm()
    // sign_type 可能是字符串形式，统一切换到下拉选项对应的 index(int)
    const src = {...row}
    if (src.sign_type !== undefined && src.sign_type !== null && src.sign_type !== '') {
        if (typeof src.sign_type === 'number') {
            // 已是 int
        } else if (/^-?\d+$/.test(String(src.sign_type))) {
            src.sign_type = parseInt(src.sign_type, 10)
        } else {
            // 字符串文字 -> int 索引
            src.sign_type = STR_TO_SIGN_TYPE(String(src.sign_type))
        }
    } else {
        src.sign_type = 0
    }
    form.value = {...base, ...src}
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
        // 提交体：合并原数据（编辑）+ 当前表单字段；类型归一化：sign_type 为 int，数字字段为 Number
        const edited = {...form.value}
        // sign_type 强转 int（下拉 value 是 0-7 int）
        edited.sign_type = Number.isFinite(+edited.sign_type) ? parseInt(edited.sign_type, 10) : 0
        // 数字字段归一化
        const floatKeys = ['amount', 'paycycle_dh', 'paycycle_zb', 'rate_yfk', 'rate_dhk', 'rate_zbj', 'settle_amount']
        floatKeys.forEach(k => {
            const n = Number(edited[k])
            edited[k] = Number.isNaN(n) ? 0 : n
        })
        const intKeys = ['hq']
        intKeys.forEach(k => {
            const n = Number(edited[k])
            edited[k] = Number.isNaN(n) ? 0 : parseInt(n, 10)
        })
        // 布尔字段归一化；finish_step 为 int：0-未开始，1-预付款，2-到货款，3-质保款
        const fNum = Number(edited.finish_step)
        edited.finish_step = Number.isNaN(fNum) ? 0 : parseInt(fNum, 10)
        edited.has_rk = edited.has_rk === true || edited.has_rk === 'true'
        // Experience 718922：编辑模式 merge original，避免空覆盖
        const paras = isEdit.value ? {...originalContract.value, ...edited} : {...edited}

        const fn = isEdit.value
            ? Singleton.getInstance(SysX).updateContract
            : Singleton.getInstance(SysX).createContract
        fn(paras, new AbortController().signal, () => {
        }, (r, data) => {
            saving.value = false
            if (r) {
                if (data?.data?.duplicate) {
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
    ElMessageBox.confirm(`确定作废合同 ${row.id} 吗？作废后不可恢复。`, '作废确认', {type: 'warning'}).then(() => {
        Singleton.getInstance(SysX).deleteContract({id: row.id}, new AbortController().signal, () => {
        }, (r, data) => {
            if (r) {
                ElMessage.success('已作废')
                loadList()
            } else {
                ElMessage.error(data?.msg || '作废失败')
            }
        })
    }).catch(() => {
    })
}

/* ---------------- 导出 ---------------- */
function doExport() {
    exportContractExcel(list.value, '合同台账_导出', signerOptions.value)
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
                    <el-input v-model="filters.id" placeholder="如 SMLJ-CG-CL" clearable style="width:160px" @keyup.enter="applyFilters"/>
                </el-form-item>
                <el-form-item label="合同名称">
                    <el-input v-model="filters.title" placeholder="模糊搜索" clearable style="width:150px" @keyup.enter="applyFilters"/>
                </el-form-item>
                <el-form-item label="签订人">
                    <el-select v-model="filters.sign_person" placeholder="全部" clearable filterable style="width:130px">
                        <el-option v-for="s in signerOptions" :key="s.account" :label="s.username" :value="s.username"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="签订方式">
                    <el-select v-model="filters.sign_type" placeholder="全部" clearable style="width:130px">
                        <el-option v-for="(m, index) in methodOptions" :key="m.id" :label="m.desc" :value="m.id"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="供应商">
                    <el-input v-model="filters.supplier" placeholder="模糊搜索" clearable style="width:160px" @keyup.enter="applyFilters"/>
                </el-form-item>
                <el-form-item label="财务完结">
                    <el-select v-model="filters.finish_step" placeholder="全部" clearable style="width:110px">
                        <el-option v-for="(label, idx) in gd.finishedOptions" :key="idx" :label="label" :value="idx + 1"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="已入库">
                    <el-select v-model="filters.has_rk" placeholder="全部" clearable style="width:100px">
                        <el-option v-for="(label, idx) in gd.yesNoOptions" :key="idx" :label="label" :value="idx === 0"/>
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

        <!-- 表格：只读 9 列 -->
        <el-card shadow="never" class="table-card">
            <el-table :data="list" v-loading="loading" border stripe style="width:100%">
                <el-table-column prop="id" label="合同编号" width="160" fixed="left">
                    <template #default="{row}"><b style="color:#2563eb">{{ row.id }}</b></template>
                </el-table-column>
                <el-table-column prop="title" label="合同名称" min-width="150" show-overflow-tooltip/>
                <el-table-column prop="amount" label="合同金额(元)" width="110" align="right">
                    <template #default="{row}"><span class="money">{{ formatMoney(row.amount) }}</span></template>
                </el-table-column>
                <el-table-column prop="date_sign" label="签订时间" width="140">
                    <template #default="{row}">
                        <el-date-picker
                            format="YYYY-MM-DD"
                            :disabled="true"
                            type="date"
                            class="item"
                            v-model="row.date_sign"
                            style="width: 115px;">
                        </el-date-picker>
                    </template>
                </el-table-column>
                <el-table-column prop="sign_person" label="签订人" width="68">
                </el-table-column>
                <el-table-column prop="sign_type" label="签订方式" width="85">
                    <template #default="{row}">{{ methodOptions.find(item => item.id === row.sign_type).desc }}</template>
                </el-table-column>
                <el-table-column prop="supplier" label="供应商" min-width="200" show-overflow-tooltip/>
                <el-table-column prop="pay_type" label="付款方式" min-width="170" show-overflow-tooltip/>
                <el-table-column prop="finish_step" label="财务完结" width="92" align="center">
                    <template #default="{row}">
                        <el-tag v-if="!row.finish_step" type="info" size="small">未开始</el-tag>
                        <el-tag v-else-if="row.finish_step === 1" type="warning" size="small">预付款</el-tag>
                        <el-tag v-else-if="row.finish_step === 2" type="primary" size="small">到货款</el-tag>
                        <el-tag v-else type="success" size="small">质保款</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="has_rk" label="已入库" width="70" align="center">
                    <template #default="{row}">
                        <el-tag :type="row.has_rk ? 'success' : 'info'" size="small">{{ row.has_rk ? '是' : '否' }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="100" fixed="right" align="center">
                    <template #default="{row}">
                        <el-button v-hasPermission="['contract:update']" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                        <el-button v-hasPermission="['contract:delete']" link type="danger" size="small" @click="removeContract(row)">作废</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pager">
                <el-pagination
                    :current-page="page"
                    :page-size="pageSize"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="total"
                    layout="total, sizes, prev, pager, next, jumper"
                    background
                    @current-change="onPageChange"
                    @size-change="onSizeChange"
                />
            </div>
        </el-card>

        <!-- 新增/编辑弹窗：全部 24 字段 -->
        <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑合同' : '新增合同'" width="1080px" destroy-on-close>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="170px">
                <el-divider content-position="left">基本信息</el-divider>
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="合同编号" prop="id">
                            <el-input v-model="form.id" placeholder="例：SMLJ-CG-CL-26330" :disabled="isEdit" @input="checkIdDup"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="合同名称" prop="title">
                            <el-input v-model="form.title" placeholder="例：螺栓"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="合同金额(元)" prop="amount">
                            <el-input-number v-model="form.amount" :min="0" :precision="2" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="签订时间" prop="date_sign">
                            <el-date-picker v-model="form.date_sign" type="date" format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="签订人" prop="sign_person">
                            <el-select v-model="form.sign_person" placeholder="请选择签订人" filterable style="width:100%">
                                <el-option v-for="s in signerOptions" :key="s.account" :label="s.username" :value="s.account"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="合同签订方式" prop="sign_type">
                            <el-select v-model="form.sign_type" placeholder="请选择" style="width:100%">
                                <el-option v-for="(m, i) in methodOptions" :key="i" :label="m.desc" :value="i"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="供应商" prop="supplier">
                            <el-input v-model="form.supplier" placeholder="供应商全称"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="付款方式">
                            <el-input v-model="form.pay_type" placeholder="例：货到票到3个月付款"/>
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-divider content-position="left">付款周期</el-divider>
                <el-row :gutter="16">
                    <el-col :span="8">
                        <el-form-item label="到货付款周期(月)" prop="paycycle_dh">
                            <el-input-number v-model="form.paycycle_dh" :min="0" :precision="1" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="质保付款周期(月)" prop="paycycle_zb">
                            <el-input-number v-model="form.paycycle_zb" :min="0" :precision="1" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-divider content-position="left">付款比例</el-divider>
                <el-row :gutter="16">
                    <el-col :span="8">
                        <el-form-item label="预付款比例(%)" prop="rate_yfk">
                            <el-input-number v-model="form.rate_yfk" :min="0" :max="100" :precision="2" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="到货款比例(%)" prop="rate_dhk">
                            <el-input-number v-model="form.rate_dhk" :min="0" :max="100" :precision="2" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="质保金比例(%)" prop="rate_zbj">
                            <el-input-number v-model="form.rate_zbj" :min="0" :max="100" :precision="2" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-divider content-position="left">付款与入库日期</el-divider>
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="预付款日期">
                            <el-date-picker v-model="form.date_yfk" type="date" format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="到货款日期">
                            <el-date-picker v-model="form.date_dhk" type="date" format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="质保金付款日期">
                            <el-date-picker v-model="form.date_zbj" type="date" format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-divider content-position="left">货期、移交与备注</el-divider>
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="结算金额(元)">
                            <el-input-number v-model="form.settle_amount" :min="0" :precision="2" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="货期(天)">
                            <el-input-number v-model="form.hq" :min="0" :step="1" :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="合同移交日期">
                            <el-input v-model="form.date_htyj" placeholder="文字/日期均可"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="发票移交日期">
                            <el-input v-model="form.date_fpyj" placeholder="文字/日期均可"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="实际到货时间">
                            <el-input v-model="form.date_actual_dh" placeholder="文字/日期均可"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="入库资料移交物资日期">
                            <el-input v-model="form.date_ruzlyj" placeholder="文字/日期均可"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="挂账日期">
                            <el-date-picker v-model="form.date_rk" type="date" format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="备注">
                            <el-input v-model="form.bz" type="textarea" :rows="3" placeholder="可填写补充说明、技术要求等"/>
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-divider v-hasRole="'ADMIN'" content-position="left">入库、财务</el-divider>
                <el-row v-hasRole="'ADMIN'" :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="是否已入库">
                            <el-switch v-model="form.has_rk" active-text="是" inactive-text="否"/>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row v-hasRole="'ADMIN'" :gutter="16">
                    <el-col :span="24">
                        <el-form-item label="财务环节">
                            <el-steps :active="form.finish_step" finish-status="success" align-center @select="(idx) => form.finish_step = idx" style="max-width:480px">
                                <el-step title="预付款" description="已付预付款" :style="{cursor: 'pointer'}"/>
                                <el-step title="到货款" description="已付到货款" :style="{cursor: 'pointer'}"/>
                                <el-step title="质保款" description="已付质保款/完结" :style="{cursor: 'pointer'}"/>
                            </el-steps>
                            <div style="font-size:12px;color:#94a3b8;margin-top:4px;padding-left:0">点击对应步骤选中；0=未完结，1=预付款，2=到货款，3=质保款</div>
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

        .pager {
            margin-top: 14px;
            display: flex;
            justify-content: flex-end;
        }
    }
}
</style>
