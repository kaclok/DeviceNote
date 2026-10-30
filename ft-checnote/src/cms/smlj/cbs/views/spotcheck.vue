<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {pagerOf} from "../system/Pager.js"
import {ElMessage, ElMessageBox} from "element-plus";
import {Search, Download, Upload, Document} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"
import {exportXlsx, exportTemplate, readSheet, cellText} from "../system/ExcelX.js"

const sys = Singleton.getInstance(SysX)

/* ============ 枚举 ============ */
function ticketDesc(t) {
    return (gd.jobTicketTypes.find(x => x.id === Number(t)) || {}).desc || (t ?? '')
}

function statusMeta(s) {
    return gd.reportStatus.find(x => x.id === Number(s)) || {desc: s ?? '', type: 'info'}
}

/** 后端 LocalDateTime 是 ISO（2026-10-28T08:45:00），表格与选择器都用空格分隔 */
function toInput(v) {
    return v ? String(v).replace('T', ' ').slice(0, 19) : ''
}

function fmtTime(v) {
    return v ? String(v).replace('T', ' ').slice(0, 16) : '—'
}

/** Excel 日期单元格 → yyyy-MM-dd（报备日期只取日期部分） */
function cellDate(v) {
    if (v instanceof Date) {
        const p = n => String(n).padStart(2, "0")
        return `${v.getFullYear()}-${p(v.getMonth() + 1)}-${p(v.getDate())}`
    }
    return String(v ?? '').trim().slice(0, 10)
}

/* ============ 列表 + 分页 ============ */
const loading = ref(false)
const rows = ref([])
const reporter = ref('')
const workType = ref('')
const status = ref('')
const reportRange = ref([])
const workRange = ref([])
const PAGER = pagerOf('spotcheck')
const pageNo = ref(1)
const pageSize = ref(PAGER.pageSize)
const total = ref(0)
let kwTimer = null

let AC = new AbortController()

function listParas() {
    const p = {}
    if (reporter.value.trim()) p.reporter = reporter.value.trim()
    if (workType.value !== '') p.workType = workType.value
    if (status.value !== '') p.status = status.value
    if (reportRange.value?.length === 2) {
        p.reportStart = reportRange.value[0]
        p.reportEnd = reportRange.value[1]
    }
    if (workRange.value?.length === 2) {
        p.beginStart = workRange.value[0]
        p.beginEnd = workRange.value[1]
    }
    return p
}

function loadRows() {
    AC.abort()
    AC = new AbortController()
    loading.value = true
    const paras = {pageNo: pageNo.value, pageSize: pageSize.value, ...listParas()}
    sys.biz('cbs/spotcheck/list', paras, AC.signal, () => {}, (r, data) => {
        loading.value = false
        if (r) {
            rows.value = data.data?.rows || []
            total.value = data.data?.total || 0
        } else {
            notifyError(data, '报备列表加载失败')
        }
    })
}

watch(reporter, () => {
    clearTimeout(kwTimer)
    kwTimer = setTimeout(() => {
        pageNo.value = 1
        loadRows()
    }, 300)
})
watch([workType, status, reportRange, workRange], () => {
    pageNo.value = 1
    loadRows()
})
watch([pageNo, pageSize], loadRows)

function search() {
    pageNo.value = 1
    loadRows()
}

function reset() {
    reporter.value = ''
    workType.value = ''
    status.value = ''
    reportRange.value = []
    workRange.value = []
    pageNo.value = 1
    loadRows()
}

/* ============ 新增 / 编辑 ============ */
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
    work_type: [{required: true, message: '请选择作业票类型', trigger: 'change'}],
    content: [{required: true, message: '请输入作业内容', trigger: 'blur'}],
    range: [{required: true, type: 'array', len: 2, message: '请选择作业时间', trigger: 'change'}],
    place: [{required: true, message: '请输入作业地点', trigger: 'blur'}],
    unit_name: [{required: true, message: '请输入施工单位/人员', trigger: 'blur'}],
    operation_person: [{required: true, message: '请输入项目负责人', trigger: 'blur'}],
    operation_phone: [
        {required: true, message: '请输入联系方式', trigger: 'blur'},
        {pattern: /^1[3-9]\d{9}$/, message: '联系方式格式不正确', trigger: 'blur'},
    ],
    report_date: [{required: true, message: '请选择报备日期', trigger: 'change'}],
}

function openDialog(row) {
    form.value = row
        ? {
            check_id: row.check_id, work_type: row.work_type, place: row.place || '',
            range: row.begin_time && row.end_time ? [toInput(row.begin_time), toInput(row.end_time)] : [],
            is_contractor_operate: !!row.is_contractor_operate, unit_name: row.unit_name || '',
            operation_person: row.operation_person || '', operation_phone: row.operation_phone || '',
            content: row.content || '', report_date: row.report_date || '', _edit: true,
        }
        : {
            check_id: '', work_type: '', place: '', range: [], is_contractor_operate: false,
            unit_name: '', operation_person: '', operation_phone: '', content: '',
            report_date: fmtToday(), _edit: false,
        }
    dialogVisible.value = true
}

function fmtToday() {
    const d = new Date()
    const p = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function saveForm() {
    formRef.value.validate(valid => {
        if (!valid) return
        saving.value = true
        const f = form.value
        const payload = {
            check_id: f.check_id || null,
            work_type: f.work_type,
            place: f.place,
            begin_time: f.range?.[0]?.replace(' ', 'T') || null,
            end_time: f.range?.[1]?.replace(' ', 'T') || null,
            is_contractor_operate: !!f.is_contractor_operate,
            unit_name: f.unit_name,
            operation_person: f.operation_person,
            operation_phone: f.operation_phone,
            content: f.content,
            report_date: f.report_date,
        }
        sys.biz('cbs/spotcheck/save', payload, AC.signal, () => {}, (r, data) => {
            saving.value = false
            if (r) {
                ElMessage.success('保存成功')
                dialogVisible.value = false
                loadRows()
            } else {
                notifyError(data, '保存失败')
            }
        })
    })
}

function removeRow(row) {
    ElMessageBox.confirm(`确定删除「${ticketDesc(row.work_type)}」这条报备吗？删除后不可恢复。`, '提示', {type: 'warning'}).then(() => {
        loading.value = true
        sys.biz('cbs/spotcheck/delete', {checkId: row.check_id}, AC.signal, () => {}, (r, data) => {
            loading.value = false
            if (r) {
                ElMessage.success('已删除')
                loadRows()
            } else {
                notifyError(data, '删除失败')
            }
        })
    }).catch(() => {})
}

/* ============ Excel：导出 / 模板 / 批量报备 ============ */
const EXCEL_COLS = [
    {label: '作业票类型', key: 'work_type_name', width: 20},
    {label: '作业地点', key: 'place', width: 22},
    {label: '作业开始时间', key: 'begin_time', width: 20},
    {label: '作业结束时间', key: 'end_time', width: 20},
    {label: '是否承包商作业', key: 'is_contractor_text', width: 16},
    {label: '施工单位/人员', key: 'unit_name', width: 26},
    {label: '项目负责人', key: 'operation_person', width: 14},
    {label: '联系方式', key: 'operation_phone', width: 16},
    {label: '作业内容', key: 'content', width: 36},
    {label: '报备状态', key: 'status_text', width: 12},
    {label: '报备日期', key: 'report_date', width: 14},
]

const SAMPLE_ROW = {
    '作业票类型': '一级动火作业', '作业地点': '常减压装置区',
    '作业开始时间': '2026-10-30 08:00', '作业结束时间': '2026-10-30 17:00',
    '是否承包商作业': '是', '施工单位/人员': '陕西化建工程有限责任公司',
    '项目负责人': '张伟', '联系方式': '13800001234',
    '作业内容': '一级动火作业：管线焊接作业', '报备状态': '进行中', '报备日期': '2026-10-30',
}

function exportExcel() {
    loading.value = true
    sys.biz('cbs/spotcheck/export', listParas(), AC.signal, () => {}, (r, data) => {
        loading.value = false
        if (!r) {
            notifyError(data, '导出失败')
            return
        }
        const list = data.data || []
        if (!list.length) {
            ElMessage.warning('当前筛选条件下没有可导出的数据')
            return
        }
        exportXlsx('特殊作业报备', '报备清单',
            EXCEL_COLS.map(c => ({
                label: c.label, width: c.width, value: row => {
                    if (c.key === 'work_type_name') return ticketDesc(row.work_type)
                    if (c.key === 'is_contractor_text') return row.is_contractor_operate ? '是' : '否'
                    if (c.key === 'status_text') return statusMeta(row.status).desc
                    if (c.key === 'begin_time' || c.key === 'end_time') return fmtTime(row[c.key])
                    return row[c.key] ?? ''
                },
            })), list)
        ElMessage.success(`已导出 ${list.length} 条`)
    })
}

function exportTpl() {
    exportTemplate('特殊作业报备模板', '报备模板', EXCEL_COLS, SAMPLE_ROW)
}

const fileInput = ref()
const importing = ref(false)

function pickFile() {
    fileInput.value?.click()
}

function onFileChange(e) {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    importing.value = true
    readSheet(file).then(aoa => {
        const header = (aoa[0] || []).map(cellText)
        const idx = {}
        EXCEL_COLS.forEach(c => {
            idx[c.label] = header.indexOf(c.label)
        })
        const missing = EXCEL_COLS.filter(c => idx[c.label] < 0).map(c => c.label)
        if (missing.length) {
            importing.value = false
            ElMessage.error(`模板不匹配，缺少列：${missing.join('、')}`)
            return
        }
        const nameToId = name => (gd.jobTicketTypes.find(t => t.desc === name) || {}).id
        const dataRows = aoa.slice(1).filter(r => r.some(v => cellText(v) !== ''))
        const payload = []
        const bad = []
        dataRows.forEach((r, i) => {
            const get = label => cellText(r[idx[label]])
            const ticketName = get('作业票类型').replace(/\s/g, '')
            const wt = nameToId(ticketName)
            if (!wt) {
                bad.push(`第 ${i + 2} 行：作业票类型「${ticketName}」不在可选范围`)
                return
            }
            const stName = get('报备状态') || '进行中'
            const st = (gd.reportStatus.find(s => s.desc === stName) || {}).id || 1
            payload.push({
                work_type: wt,
                place: get('作业地点'),
                begin_time: get('作业开始时间'),
                end_time: get('作业结束时间'),
                is_contractor_operate: get('是否承包商作业') === '是',
                unit_name: get('施工单位/人员'),
                operation_person: get('项目负责人'),
                operation_phone: get('联系方式'),
                content: get('作业内容'),
                status: st,
                report_date: cellDate(r[idx['报备日期']]),
            })
        })
        if (bad.length) {
            importing.value = false
            ElMessageBox.alert(bad.slice(0, 10).join('<br/>'), 'Excel 内容有误，未提交', {dangerouslyUseHTMLString: true})
            return
        }
        if (!payload.length) {
            importing.value = false
            ElMessage.warning('Excel 中没有可导入的数据行')
            return
        }
        sys.biz('cbs/spotcheck/importBatch', {rows: payload}, AC.signal, () => {}, (r2, data2) => {
            importing.value = false
            if (!r2) {
                notifyError(data2, '批量报备失败')
                return
            }
            const ok = data2.data?.inserted || 0
            const failed = data2.data?.failed || []
            loadRows()
            if (failed.length) {
                const msg = failed.slice(0, 10).map(f => `第 ${f.row} 行：${f.msg}`).join('<br/>')
                ElMessageBox.alert(`成功 ${ok} 条，失败 ${failed.length} 条<br/>${msg}`,
                    '批量报备结果', {dangerouslyUseHTMLString: true})
            } else {
                ElMessage.success(`批量报备成功，共导入 ${ok} 条`)
            }
        })
    }).catch(() => {
        importing.value = false
        ElMessage.error('Excel 解析失败，请确认文件格式为 .xlsx')
    })
}

/* ============ 生命周期 ============ */
onMounted(loadRows)
onUnmounted(() => {
    AC.abort()
    clearTimeout(kwTimer)
})
</script>

<template>
    <div class="spotcheck-page">
        <div class="page-head">
            <h2>特殊作业报备</h2>
        </div>
        <div class="card">
            <div class="filters">
                <el-input v-model="reporter" placeholder="报备人员" clearable
                          style="width:170px" :prefix-icon="Search"/>
                <el-select v-model="workType" placeholder="作业票类型" clearable filterable style="width:190px">
                    <el-option v-for="t in gd.jobTicketTypes" :key="t.id" :value="t.id" :label="t.desc"/>
                </el-select>
                <el-select v-model="status" placeholder="报备状态" clearable style="width:140px">
                    <el-option v-for="s in gd.reportStatus" :key="s.id" :value="s.id" :label="s.desc"/>
                </el-select>
                <el-date-picker v-model="reportRange" type="daterange" value-format="YYYY-MM-DD"
                                range-separator="至" start-placeholder="报备开始日期"
                                end-placeholder="报备结束日期" style="width:260px"/>
                <el-date-picker v-model="workRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"
                                range-separator="至" start-placeholder="作业开始时间"
                                end-placeholder="作业结束时间" style="width:360px"/>
                <el-button type="primary" @click="search">查询</el-button>
                <el-button @click="reset">重置</el-button>
            </div>

            <div class="toolbar">
                <el-button type="primary" @click="openDialog(null)">新增</el-button>
                <el-button :icon="Download" @click="exportExcel">导出</el-button>
                <el-button :icon="Document" @click="exportTpl">导出模板</el-button>
                <el-button :icon="Upload" :loading="importing" @click="pickFile">批量报备</el-button>
                <input ref="fileInput" type="file" accept=".xlsx,.xls" class="file-input"
                       @change="onFileChange"/>
                <span class="hint">批量报备：下载模板填好后选择 Excel 一次性导入</span>
            </div>

            <el-table :data="rows" v-loading="loading" border stripe>
                <el-table-column label="作业类型" width="128" align="center">
                    <template #default="{row}">
                        <el-tag effect="plain" size="small">{{ ticketDesc(row.work_type) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="place" label="作业地点" min-width="125" show-overflow-tooltip>
                    <template #default="{row}">{{ row.place || '—' }}</template>
                </el-table-column>
                <el-table-column label="作业时间" min-width="250" align="center">
                    <template #default="{row}">
                        <span v-if="row.begin_time || row.end_time" class="time-cell">
                            {{ fmtTime(row.begin_time) }} ~ {{ fmtTime(row.end_time) }}
                        </span>
                        <span v-else class="dim">—</span>
                    </template>
                </el-table-column>
                <el-table-column label="是否承包商作业" width="126" align="center">
                    <template #default="{row}">{{ row.is_contractor_operate ? '是' : '否' }}</template>
                </el-table-column>
                <el-table-column prop="unit_name" label="施工单位/人员" min-width="158" show-overflow-tooltip>
                    <template #default="{row}">{{ row.unit_name || '—' }}</template>
                </el-table-column>
                <el-table-column prop="operation_person" label="项目负责人" width="95">
                    <template #default="{row}">{{ row.operation_person || '—' }}</template>
                </el-table-column>
                <el-table-column prop="operation_phone" label="联系方式" width="130">
                    <template #default="{row}">{{ row.operation_phone || '—' }}</template>
                </el-table-column>
                <el-table-column prop="content" label="作业内容" min-width="160" show-overflow-tooltip/>
                <el-table-column label="报备状态" width="96" align="center">
                    <template #default="{row}">
                        <el-tag :type="statusMeta(row.status).type" effect="light" size="small">
                            {{ statusMeta(row.status).desc }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="130" align="center" fixed="right">
                    <template #default="{row}">
                        <el-link type="primary" @click="openDialog(row)">编辑</el-link>
                        <el-link type="danger" style="margin-left:10px" @click="removeRow(row)">删除</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!loading && rows.length === 0" description="暂无报备记录"/>

            <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                           :total="total" v-model:current-page="pageNo" v-model:page-size="pageSize"
                           :page-sizes="PAGER.pageSizes"/>
        </div>

        <!-- 新增/编辑弹窗（字段与目标站报备表单一致，单列） -->
        <el-dialog v-model="dialogVisible" :title="form._edit ? '编辑报备' : '新增报备'"
                   width="640px" destroy-on-close>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
                <el-form-item label="作业内容" prop="content">
                    <el-input v-model="form.content" type="textarea" :rows="3" placeholder="请输入作业内容"/>
                </el-form-item>
                <el-form-item label="作业时间" prop="range">
                    <el-date-picker v-model="form.range" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"
                                    range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间"
                                    style="width:100%"/>
                </el-form-item>
                <el-form-item label="作业地点" prop="place">
                    <el-input v-model="form.place" placeholder="请输入作业地点"/>
                </el-form-item>
                <el-form-item label="是否承包商作业" prop="is_contractor_operate">
                    <el-radio-group v-model="form.is_contractor_operate">
                        <el-radio :value="true">是</el-radio>
                        <el-radio :value="false">否</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="施工单位/人员" prop="unit_name">
                    <el-input v-model="form.unit_name" placeholder="请输入施工单位/人员"/>
                </el-form-item>
                <el-form-item label="项目负责人" prop="operation_person">
                    <el-input v-model="form.operation_person" placeholder="请输入项目负责人"/>
                </el-form-item>
                <el-form-item label="联系方式" prop="operation_phone">
                    <el-input v-model="form.operation_phone" maxlength="11" placeholder="请输入联系方式"/>
                </el-form-item>
                <el-form-item label="相关作业票" prop="work_type">
                    <el-select v-model="form.work_type" placeholder="请选择作业票" filterable style="width:100%">
                        <el-option v-for="t in gd.jobTicketTypes" :key="t.id" :value="t.id" :label="t.desc"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="报备日期" prop="report_date">
                    <el-date-picker v-model="form.report_date" type="date" value-format="YYYY-MM-DD"
                                    placeholder="选择日期" style="width:100%"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.spotcheck-page {
    .page-head {
        margin: 4px 0 18px;
        text-align: center;

        h2 {
            margin: 0;
            font-size: 22px;
            font-weight: 700;
            color: #1e293b;
            letter-spacing: 2px;
        }
    }

    .card {
        background: #ffffff;
        border-radius: 10px;
        padding: 16px;
        border: 1px solid #e2e8f0;
    }

    .filters {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 10px;
        padding-bottom: 12px;
        margin-bottom: 12px;
        border-bottom: 1px dashed #e2e8f0;
    }

    .toolbar {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 14px;

        .file-input {
            display: none;
        }

        .hint {
            margin-left: auto;
            font-size: 12px;
            color: #94a3b8;
        }
    }

    .pager {
        margin-top: 14px;
        justify-content: flex-end;
    }

    .dim {
        color: #cbd5e1;
    }

    .time-cell {
        font-size: 12px;
        white-space: nowrap;
    }
}
</style>
