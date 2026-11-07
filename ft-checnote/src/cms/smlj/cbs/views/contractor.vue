<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {pagerOf} from "../system/Pager.js"
import {ElMessage, ElMessageBox} from "element-plus";
import {Search} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"
import {useDictNames} from "../system/DictX.js"

const sys = Singleton.getInstance(SysX)

/* ============ 两种页面：承包商单位管理 / 承包商审核 ============
 * 对齐参考站 contractorApproval：横向卡片切页 + 每类切页各自的行内操作。 */
const MODES = {
    unit: {
        title: '承包商单位管理',
        tabs: [
            {key: 'pass', label: '审批通过', params: {status: 1}},
            {key: 'wait', label: '待审批', params: {status: 0}},
            {key: 'reject', label: '未通过', params: {status: 2}},
            {key: 'recycle', label: '回收站', params: {recycled: true}},
        ],
        canCreate: true,
    },
    audit: {
        title: '承包商审核',
        tabs: [
            {key: 'coop', label: '合作中', params: {coop: 1, status: 1}},
            {key: 'ever', label: '曾合作', params: {coop: 2, status: 1}},
            {key: 'black', label: '黑名单', params: {coop: 3, status: 1}},
        ],
        canCreate: false,
    },
}

const props = defineProps({tab: String})
const mode = ref(MODES[props?.tab] ? props.tab : 'unit')
watch(() => props.tab, t => {
    if (MODES[t] && t !== mode.value) {
        mode.value = t
        tabKey.value = MODES[t].tabs[0].key
        keyword.value = ''
        ctypeFilter.value = ''
        pageNo.value = 1
        pageSize.value = pager.value.pageSize
        loadRows()
    }
})
const cfg = computed(() => MODES[mode.value])
const tabKey = ref(cfg.value.tabs[0].key)

/* ============ 列表 + 分页 ============ */
const loading = ref(false)
const rows = ref([])
const keyword = ref('')
const ctypeFilter = ref('')
// 每页独立分页配置：contractor.unit / contractor.audit
const pager = computed(() => pagerOf('contractor.' + mode.value))
const pageNo = ref(1)
const pageSize = ref(pager.value.pageSize)
const total = ref(0)
let kwTimer = null

let AC = new AbortController()
/* 审批流转独立通道：不能与列表共用 AC，否则查询/切页会取消审批请求，导致保存按钮状态卡死 */
let OPAC = new AbortController()

/** 当前切页 → 后端过滤参数（status 审批状态 / coop 合作状态 / recycled 回收站） */
const tabParams = computed(() => (cfg.value.tabs.find(t => t.key === tabKey.value) || {}).params || {})

/** 操作列宽随切页变化（按内容实测预留：6 个链接 400 / 审核 90 / 查看+删除 150 / 回收站 210） */
const opWidth = computed(() => {
    if (mode.value === 'audit') return 120
    return {pass: 400, wait: 90, reject: 150, recycle: 210}[tabKey.value] || 150
})

function loadRows() {
    AC.abort()
    AC = new AbortController()
    loading.value = true
    const paras = {pageNo: pageNo.value, pageSize: pageSize.value, ...tabParams.value}
    const kw = keyword.value.trim()
    if (kw) paras.kw = kw
    if (ctypeFilter.value) paras.ctype = ctypeFilter.value
    sys.biz('cbs/contractor/list', paras, AC.signal, () => {}, (r, data) => {
        loading.value = false
        if (r) {
            rows.value = data.data?.rows || []
            total.value = data.data?.total || 0
        } else {
            notifyError(data, '承包商列表加载失败')
        }
    })
}

watch(keyword, () => {
    clearTimeout(kwTimer)
    kwTimer = setTimeout(() => {
        pageNo.value = 1
        loadRows()
    }, 300)
})
watch([ctypeFilter, tabKey], () => {
    pageNo.value = 1
    loadRows()
})
watch([pageNo, pageSize], loadRows)

function statusMeta(s) {
    return gd.approveStatus.find(x => x.id === Number(s)) || {desc: s, type: 'info'}
}

function coopMeta(s) {
    return (gd.coopStatus || []).find(x => x.id === Number(s)) || {desc: s ?? '—', type: 'info'}
}

/* 承包商类型：字典项可在「基础设置 › 承包商类型」自行增减 ⇒ 选项一律现查后端，gd.json 不存选项 */
const ctypeOptions = useDictNames('contractor_type')

const today = () => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/* ============ 查看（只读详情，仿参考站 contractorCompanyModal 两段式） ============ */
const viewVisible = ref(false)
const viewRow = ref({})
/* 审批状态切换（0 待审批 / 1 审批通过 / 2 未通过）：未通过必须填原因，取消则回滚选中态 */
const viewApprove = ref(0)
const approveSaving = ref(false)

function openView(row) {
    viewRow.value = {...row}
    viewApprove.value = Number(row.approve_status ?? 0)
    viewVisible.value = true
}

async function onApproveChange(val) {
    const prev = Number(viewRow.value.approve_status ?? 0)
    if (val === prev) return
    let reason = ''
    try {
        if (val === 2) {
            const r = await ElMessageBox.prompt('切换为「未通过」需填写原因，原因会展示给承包商。', '审批状态', {
                confirmButtonText: '确定', cancelButtonText: '取消',
                inputPlaceholder: '请输入未通过原因',
                inputValidator: v => (v && v.trim() ? true : '未通过原因不能为空'),
            })
            reason = r.value.trim()
        } else {
            await ElMessageBox.confirm(`确认将审批状态切换为「${statusMeta(val).desc}」？`, '审批状态', {
                confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
            })
        }
    } catch (e) {
        viewApprove.value = prev
        return
    }
    approveSaving.value = true
    sys.biz('cbs/contractor/audit', {contractorId: viewRow.value.contractor_id, status: val, reason},
        OPAC.signal, () => {
            approveSaving.value = false
            viewApprove.value = prev
        }, (r, data) => {
            approveSaving.value = false
            if (r) {
                ElMessage.success('审批状态已更新')
                viewRow.value = {...viewRow.value, approve_status: val, reject_reason: reason || null}
                loadRows()
            } else {
                viewApprove.value = prev
                notifyError(data, '审批状态更新失败')
            }
        })
}

/* ============ 新增/编辑 ============ */
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
    name: [{required: true, message: '请输入承包商名称', trigger: 'blur'}],
    uscc: [{required: true, message: '请输入统一社会信用代码', trigger: 'blur'}],
    ctype: [{required: true, message: '请选择承包商类型', trigger: 'change'}],
    coop_status: [{required: true, message: '请选择状态', trigger: 'change'}],
    legal_person: [{required: true, message: '请输入法定代表人', trigger: 'blur'}],
    legal_phone: [{required: true, message: '请输入法定代表人联系电话', trigger: 'blur'}],
    safety_person: [{required: true, message: '请输入安全负责人', trigger: 'blur'}],
    safety_phone: [{required: true, message: '请输入安全负责人电话', trigger: 'blur'}],
}

function openDialog(edit, row) {
    form.value = edit
        ? {
            contractor_id: row.contractor_id, name: row.name, uscc: row.uscc || '',
            ctype: row.ctype || '', coop_status: row.coop_status ?? 1,
            legal_person: row.legal_person || '', legal_phone: row.legal_phone || '',
            safety_person: row.safety_person || '', safety_phone: row.safety_phone || '',
            register_address: row.register_address || '', register_capital: row.register_capital || '',
            deposit: row.deposit ?? undefined, login_account: row.login_account || '',
            phone: row.phone || '', id_card: row.id_card || '',
            post_name: row.post_name || '', role_name: row.role_name || '', _edit: true,
        }
        : {
            contractor_id: '', name: '', uscc: '', ctype: '', coop_status: 1, legal_person: '', legal_phone: '',
            safety_person: '', safety_phone: '', register_address: '', register_capital: '',
            deposit: undefined, login_account: '', phone: '', id_card: '', post_name: '', role_name: '',
            _edit: false,
        }
    dialogVisible.value = true
}

function saveForm() {
    formRef.value.validate(valid => {
        if (!valid) return
        saving.value = true
        const f = {...form.value}
        delete f._edit
        if (!f.deposit && f.deposit !== 0) f.deposit = null
        sys.biz('cbs/contractor/save', f, AC.signal, () => {}, (r, data) => {
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

/* ============ 审核 ============ */
const auditVisible = ref(false)
const auditSaving = ref(false)
const auditFormRef = ref()
const auditForm = ref({contractor_id: '', name: '', status: 1, reason: ''})
const auditRules = {
    reason: [{
        validator: (rule, value, callback) => {
            if (auditForm.value.status === 2 && !value) callback(new Error('驳回必须填写原因'))
            else callback()
        }, trigger: 'blur',
    }],
}

function openAudit(row) {
    auditForm.value = {contractor_id: row.contractor_id, name: row.name, status: 1, reason: ''}
    auditVisible.value = true
}

function submitAudit() {
    auditFormRef.value.validate(valid => {
        if (!valid) return
        auditSaving.value = true
        const f = {...auditForm.value}
        if (f.status === 1) f.reason = ''
        // 后端口径 contractorId/status/reason，表单内部键名 contractor_id 不直接透传
        sys.biz('cbs/contractor/audit',
            {contractorId: f.contractor_id, status: f.status, reason: f.reason || ''},
            OPAC.signal, () => {
                auditSaving.value = false
            }, (r, data) => {
                auditSaving.value = false
                if (r) {
                    ElMessage.success(f.status === 1 ? '已通过' : '已驳回')
                    auditVisible.value = false
                    loadRows()
                } else {
                    notifyError(data, '审核失败')
                }
            })
    })
}

/* ============ 删除 / 恢复 / 彻底删除 ============ */
function removeRow(row) {
    ElMessageBox.confirm(`确定删除承包商「${row.name}」吗？`, '提示', {type: 'warning'}).then(() => {
        loading.value = true
        sys.biz('cbs/contractor/delete', {contractorId: row.contractor_id}, AC.signal, () => {}, (r, data) => {
            loading.value = false
            if (r) {
                ElMessage.success('已删除，可在回收站恢复')
                loadRows()
            } else {
                notifyError(data, '删除失败')
            }
        })
    }).catch(() => {})
}

function restoreRow(row) {
    loading.value = true
    sys.biz('cbs/contractor/restore', {contractorId: row.contractor_id}, AC.signal, () => {}, (r, data) => {
        loading.value = false
        if (r) {
            ElMessage.success('已恢复')
            loadRows()
        } else {
            notifyError(data, '恢复失败')
        }
    })
}

function purgeRow(row) {
    ElMessageBox.confirm(`彻底删除后不可恢复，确定删除「${row.name}」吗？`, '警告', {type: 'warning'}).then(() => {
        loading.value = true
        sys.biz('cbs/contractor/purge', {contractorId: row.contractor_id}, AC.signal, () => {}, (r, data) => {
            loading.value = false
            if (r) {
                ElMessage.success('已彻底删除')
                loadRows()
            } else {
                notifyError(data, '删除失败')
            }
        })
    }).catch(() => {})
}

/* ============ 保证金变更 / 变更记录（仿参考站 addBondModal / bondLogModal） ============ */
const bondVisible = ref(false)
const bondReadonly = ref(false)
const bondSaving = ref(false)
const bondFormRef = ref()
const emptyBondForm = () => ({contractor_id: '', type: 1, money: undefined, describe: '', change_time: ''})
const bondForm = ref(emptyBondForm())
const bondRules = {
    type: [{required: true, message: '请选择变更类型', trigger: 'change'}],
    money: [{required: true, message: '请输入变更金额', trigger: 'blur'}],
    change_time: [{required: true, message: '请选择变更时间', trigger: 'change'}],
}

function openBond(row) {
    bondForm.value = {...emptyBondForm(), contractor_id: row.contractor_id, change_time: today()}
    bondReadonly.value = false
    bondVisible.value = true
}

function viewBond(row) {
    bondForm.value = {
        contractor_id: row.contractor_id, type: row.type,
        money: row.money, describe: row.describe || '', change_time: (row.change_time || '').slice(0, 10),
    }
    bondReadonly.value = true
    bondVisible.value = true
}

function saveBond() {
    bondFormRef.value.validate(valid => {
        if (!valid) return
        bondSaving.value = true
        sys.biz('cbs/contractor/changeLog/save', {...bondForm.value}, AC.signal, () => {}, (r, data) => {
            bondSaving.value = false
            if (r) {
                ElMessage.success('已登记变更')
                bondVisible.value = false
                loadRows()
                if (bondLogVisible.value) loadBondLog()
            } else {
                notifyError(data, '保证金变更失败')
            }
        })
    })
}

const bondLogVisible = ref(false)
const bondLogLoading = ref(false)
const bondLogRows = ref([])
const bondLogTotal = ref(0)
const bondLogPage = ref(1)
const bondLogSize = ref(10)
const bondLogOf = ref('')
const bondLogInfo = ref({name: '', deposit: null})

function openBondLog(row) {
    bondLogOf.value = row.contractor_id
    bondLogInfo.value = {name: row.name, deposit: row.deposit}
    bondLogPage.value = 1
    bondLogVisible.value = true
    loadBondLog()
}

function loadBondLog() {
    bondLogLoading.value = true
    sys.biz('cbs/contractor/changeLog/list', {
        contractorId: bondLogOf.value, pageNo: bondLogPage.value, pageSize: bondLogSize.value,
    }, AC.signal, () => {}, (r, data) => {
        bondLogLoading.value = false
        if (r) {
            bondLogRows.value = data.data?.rows || []
            bondLogTotal.value = data.data?.total || 0
            if (data.data?.contractor) {
                bondLogInfo.value = {name: data.data.contractor.name, deposit: data.data.contractor.deposit}
            }
        } else {
            notifyError(data, '变更记录加载失败')
        }
    })
}

watch([bondLogPage, bondLogSize], () => {
    if (bondLogVisible.value) loadBondLog()
})

/* ============ 处罚 / 处罚记录（仿参考站 addPunishModal / punishLogModal） ============ */
const punishVisible = ref(false)
const punishReadonly = ref(false)
const punishSaving = ref(false)
const punishFormRef = ref()
const emptyPunishForm = () => ({
    contractor_id: '', project_id: '', type_id: '', punish_user: '',
    money: undefined, describe: '', punish_time: '',
})
const punishForm = ref(emptyPunishForm())
const punishProjects = ref([])
const punishTypes = ref([])
const punishRules = {
    project_id: [{required: true, message: '请选择关联项目', trigger: 'change'}],
    type_id: [{required: true, message: '请选择处罚类型', trigger: 'change'}],
    punish_user: [{required: true, message: '请输入违规人', trigger: 'blur'}],
    money: [{required: true, message: '请输入处罚金额', trigger: 'blur'}],
    punish_time: [{required: true, message: '请选择处罚时间', trigger: 'change'}],
}

function loadPunishOptions(contractorId) {
    sys.biz('cbs/contractor/punish/options', {contractorId}, AC.signal, () => {}, (r, data) => {
        if (r) {
            punishProjects.value = data.data?.projects || []
            punishTypes.value = data.data?.punishTypes || []
        } else {
            notifyError(data, '处罚表单选项加载失败')
        }
    })
}

function openPunish(row) {
    punishForm.value = {...emptyPunishForm(), contractor_id: row.contractor_id, punish_time: today()}
    punishReadonly.value = false
    punishVisible.value = true
    loadPunishOptions(row.contractor_id)
}

function viewPunish(row) {
    punishForm.value = {
        contractor_id: row.contractor_id, project_id: row.project_id || '', type_id: row.type_id || '',
        punish_user: row.punish_user || '', money: row.money,
        describe: row.describe || '', punish_time: (row.punish_time || '').slice(0, 10),
    }
    punishReadonly.value = true
    punishVisible.value = true
}

function savePunish() {
    punishFormRef.value.validate(valid => {
        if (!valid) return
        punishSaving.value = true
        sys.biz('cbs/contractor/punish/save', {...punishForm.value}, AC.signal, () => {}, (r, data) => {
            punishSaving.value = false
            if (r) {
                ElMessage.success('已登记处罚')
                punishVisible.value = false
                if (punishLogVisible.value) loadPunishLog()
            } else {
                notifyError(data, '处罚登记失败')
            }
        })
    })
}

const punishLogVisible = ref(false)
const punishLogLoading = ref(false)
const punishLogRows = ref([])
const punishLogTotal = ref(0)
const punishLogPage = ref(1)
const punishLogSize = ref(10)
const punishLogOf = ref('')
const punishLogInfo = ref({name: '', deposit: null})

function openPunishLog(row) {
    punishLogOf.value = row.contractor_id
    punishLogInfo.value = {name: row.name, deposit: row.deposit}
    punishLogPage.value = 1
    punishLogVisible.value = true
    loadPunishLog()
}

function loadPunishLog() {
    punishLogLoading.value = true
    sys.biz('cbs/contractor/punish/list', {
        contractorId: punishLogOf.value, pageNo: punishLogPage.value, pageSize: punishLogSize.value,
    }, AC.signal, () => {}, (r, data) => {
        punishLogLoading.value = false
        if (r) {
            punishLogRows.value = data.data?.rows || []
            punishLogTotal.value = data.data?.total || 0
            if (data.data?.contractor) {
                punishLogInfo.value = {name: data.data.contractor.name, deposit: data.data.contractor.deposit}
            }
        } else {
            notifyError(data, '处罚记录加载失败')
        }
    })
}

watch([punishLogPage, punishLogSize], () => {
    if (punishLogVisible.value) loadPunishLog()
})

/* ============ 生命周期 ============ */
onMounted(loadRows)
onUnmounted(() => {
    AC.abort()
    OPAC.abort()
    clearTimeout(kwTimer)
})
</script>

<template>
    <div class="contractor-page">
        <div class="page-head">
            <h2>{{ cfg.title }}</h2>
        </div>

        <div class="card">
            <!-- 横向卡片切页（对齐参考站 a-tabs type=card） -->
            <el-tabs v-model="tabKey" type="card" class="state-tabs">
                <el-tab-pane v-for="t in cfg.tabs" :key="t.key" :label="t.label" :name="t.key"/>
            </el-tabs>

            <div class="toolbar">
                <el-input v-model="keyword" placeholder="承包商名称" clearable
                          style="width:200px" :prefix-icon="Search"/>
                <el-select v-model="ctypeFilter" placeholder="承包商类型" clearable style="width:170px">
                    <el-option v-for="t in ctypeOptions" :key="t" :value="t" :label="t"/>
                </el-select>
                <el-button type="primary" @click="loadRows">查询</el-button>
                <el-button @click="keyword = ''; ctypeFilter = ''; pageNo = 1">重置</el-button>
                <el-button v-if="cfg.canCreate" type="primary" plain class="add-btn"
                           @click="openDialog(false)">新增
                </el-button>
            </div>

            <el-table :data="rows" v-loading="loading" border stripe>
                <el-table-column type="index" label="序号" width="60" align="center"/>
                <el-table-column prop="name" label="承包商名称" min-width="200" show-overflow-tooltip/>
                <el-table-column prop="uscc" label="统一社会信用代码" min-width="180" show-overflow-tooltip/>
                <el-table-column prop="ctype" label="承包商类型" min-width="130" show-overflow-tooltip/>
                <el-table-column prop="legal_person" label="法定代表人" min-width="110" show-overflow-tooltip/>
                <el-table-column prop="safety_person" label="安全负责人" min-width="110" show-overflow-tooltip/>
                <el-table-column prop="safety_phone" label="安全负责人电话" min-width="140" show-overflow-tooltip/>
                <el-table-column v-if="mode === 'unit'" label="审批状态" width="100" align="center">
                    <template #default="{row}">
                        <el-tag :type="statusMeta(row.approve_status).type" effect="light">
                            {{ statusMeta(row.approve_status).desc }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column v-else label="合作状态" width="100" align="center">
                    <template #default="{row}">
                        <el-tag :type="coopMeta(row.coop_status).type" effect="light">
                            {{ coopMeta(row.coop_status).desc }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" :width="opWidth" align="center" fixed="right" class-name="op-cell">
                    <template #default="{row}">
                        <!-- 审批通过：查看｜保证金变更｜变更记录｜处罚｜处罚记录｜删除 -->
                        <template v-if="mode === 'unit' && tabKey === 'pass'">
                            <el-link type="primary" @click="openView(row)">查看</el-link>
                            <el-divider direction="vertical"/>
                            <el-link type="primary" @click="openBond(row)">保证金变更</el-link>
                            <el-divider direction="vertical"/>
                            <el-link type="primary" @click="openBondLog(row)">变更记录</el-link>
                            <el-divider direction="vertical"/>
                            <el-link type="primary" @click="openPunish(row)">处罚</el-link>
                            <el-divider direction="vertical"/>
                            <el-link type="primary" @click="openPunishLog(row)">处罚记录</el-link>
                            <el-divider direction="vertical"/>
                            <el-link type="danger" @click="removeRow(row)">删除</el-link>
                        </template>
                        <!-- 待审批：审核 -->
                        <el-link v-else-if="mode === 'unit' && tabKey === 'wait'" type="warning"
                                 @click="openAudit(row)">审核
                        </el-link>
                        <!-- 未通过：查看｜删除 -->
                        <template v-else-if="mode === 'unit' && tabKey === 'reject'">
                            <el-link type="primary" @click="openView(row)">查看</el-link>
                            <el-divider direction="vertical"/>
                            <el-link type="danger" @click="removeRow(row)">删除</el-link>
                        </template>
                        <!-- 回收站：查看｜恢复｜彻底删除 -->
                        <template v-else-if="mode === 'unit'">
                            <el-link type="primary" @click="openView(row)">查看</el-link>
                            <el-divider direction="vertical"/>
                            <el-link type="success" @click="restoreRow(row)">恢复</el-link>
                            <el-divider direction="vertical"/>
                            <el-link type="danger" @click="purgeRow(row)">彻底删除</el-link>
                        </template>
                        <!-- 审核页：查看｜编辑 -->
                        <template v-else>
                            <el-link type="primary" @click="openView(row)">查看</el-link>
                            <el-divider direction="vertical"/>
                            <el-link type="primary" @click="openDialog(true, row)">编辑</el-link>
                        </template>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!loading && rows.length === 0" description="暂无承包商"/>

            <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                           :total="total" v-model:current-page="pageNo" v-model:page-size="pageSize"
                           :page-sizes="pager.pageSizes"/>
        </div>

        <!-- 查看详情（只读，对齐参考站 contractorCompanyForm：1000px 双列 + 基本信息/注册信息 + 审批状态切换） -->
        <el-dialog v-model="viewVisible" :title="`查看 — ${viewRow.name || ''}`" width="1000px" top="6vh"
                   destroy-on-close class="view-dialog">
            <div class="approve-bar">
                <span class="lb">审批状态</span>
                <el-radio-group v-model="viewApprove" :disabled="approveSaving" @change="onApproveChange">
                    <el-radio-button v-for="s in gd.approveStatus" :key="s.id" :value="s.id">
                        {{ s.desc }}
                    </el-radio-button>
                </el-radio-group>
                <span v-if="viewRow.reject_reason" class="reason">未通过原因：{{ viewRow.reject_reason }}</span>
            </div>
            <el-form class="view-form" label-width="130px" label-suffix="：" disabled>
                <el-divider content-position="left">基本信息</el-divider>
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="承包商类型">
                            <el-input :model-value="viewRow.ctype || '—'"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="承包商名称">
                            <el-input :model-value="viewRow.name || '—'"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="统一社会信用代码">
                            <el-input :model-value="viewRow.uscc || '—'"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="法定代表人">
                            <el-input :model-value="viewRow.legal_person || '—'"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="安全负责人">
                            <el-input :model-value="viewRow.safety_person || '—'"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="安全负责人电话">
                            <el-input :model-value="viewRow.safety_phone || '—'"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="状态">
                            <el-radio-group :model-value="Number(viewRow.coop_status ?? 1)" disabled>
                                <el-radio v-for="c in gd.coopStatus" :key="c.id" :value="c.id">{{ c.desc }}
                                </el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="注册地址">
                            <el-input :model-value="viewRow.register_address || '—'"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="注册资本">
                            <el-input :model-value="viewRow.register_capital || '—'"/>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-divider content-position="left">注册信息</el-divider>
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="登录账号">
                            <el-input :model-value="viewRow.login_account || '—'"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="登录密码">
                            <el-input type="password" :model-value="viewRow.login_pwd || '—'" show-password/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="手机号码">
                            <el-input :model-value="viewRow.phone || '—'"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="身份证号">
                            <el-input :model-value="viewRow.id_card || '—'"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="岗位">
                            <el-input :model-value="viewRow.post_name || '—'"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="角色">
                            <el-input :model-value="viewRow.role_name || '—'"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="保证金(元)">
                            <el-input :model-value="viewRow.deposit ?? '—'"/>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <el-button @click="viewVisible = false">关闭</el-button>
            </template>
        </el-dialog>

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="dialogVisible" :title="form._edit ? '编辑承包商' : '新增承包商'"
                   width="680px" destroy-on-close>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="150px">
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="承包商名称" prop="name">
                            <el-input v-model="form.name" placeholder="请输入承包商名称"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="统一社会信用代码" prop="uscc">
                            <el-input v-model="form.uscc"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="承包商类型" prop="ctype">
                            <el-select v-model="form.ctype" placeholder="请选择类型" clearable style="width:100%">
                                <el-option v-for="t in ctypeOptions" :key="t" :value="t" :label="t"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="状态" prop="coop_status">
                            <el-radio-group v-model="form.coop_status">
                                <el-radio v-for="s in gd.coopStatus" :key="s.id" :value="s.id">{{ s.desc }}
                                </el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="法定代表人" prop="legal_person">
                            <el-input v-model="form.legal_person"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="法定代表人联系电话" prop="legal_phone">
                            <el-input v-model="form.legal_phone"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="安全负责人" prop="safety_person">
                            <el-input v-model="form.safety_person"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="安全负责人电话" prop="safety_phone">
                            <el-input v-model="form.safety_phone"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="注册地址">
                            <el-input v-model="form.register_address"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="注册资本">
                            <el-input v-model="form.register_capital" placeholder="文本口径，如 5000万元"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="登录账号">
                            <el-input v-model="form.login_account" placeholder="预留，一期不启用"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="手机号码">
                            <el-input v-model="form.phone" placeholder="请输入手机号码"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="身份证号">
                            <el-input v-model="form.id_card" placeholder="请输入身份证号"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="岗位">
                            <el-input v-model="form.post_name" placeholder="请输入岗位"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="角色">
                            <el-input v-model="form.role_name" placeholder="请输入角色"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="保证金(元)">
                            <el-input-number v-model="form.deposit" :min="0" :precision="2" :controls="false"
                                             style="width:100%"/>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
            </template>
        </el-dialog>

        <!-- 审核弹窗 -->
        <el-dialog v-model="auditVisible" :title="`审核 — ${auditForm.name}`" width="440px" destroy-on-close>
            <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="80px">
                <el-form-item label="审核结论">
                    <el-radio-group v-model="auditForm.status">
                        <el-radio :value="1">通过</el-radio>
                        <el-radio :value="2">驳回</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item v-if="auditForm.status === 2" label="驳回原因" prop="reason">
                    <el-input v-model="auditForm.reason" type="textarea" :rows="3" placeholder="请填写驳回原因"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="auditVisible = false">取消</el-button>
                <el-button type="primary" :loading="auditSaving" @click="submitAudit">确定</el-button>
            </template>
        </el-dialog>

        <!-- 保证金变更（新增 / 只读查看，仿 addBondModal 600px） -->
        <el-dialog v-model="bondVisible" :title="bondReadonly ? '变更详情' : '保证金变更'" width="600px" destroy-on-close>
            <el-form ref="bondFormRef" :model="bondForm" :rules="bondRules" label-width="110px"
                     :disabled="bondReadonly">
                <el-form-item label="变更类型" prop="type">
                    <el-radio-group v-model="bondForm.type">
                        <el-radio :value="1">扣款</el-radio>
                        <el-radio :value="2">增加</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="变更金额(元)" prop="money">
                    <el-input-number v-model="bondForm.money" :min="0.01" :precision="2" :controls="false"
                                     placeholder="请输入变更金额" style="width:100%"/>
                </el-form-item>
                <el-form-item label="描述">
                    <el-input v-model="bondForm.describe" type="textarea" :rows="3" placeholder="请输入描述"/>
                </el-form-item>
                <el-form-item label="变更时间" prop="change_time">
                    <el-date-picker v-model="bondForm.change_time" type="date" value-format="YYYY-MM-DD"
                                    placeholder="请选择变更时间" style="width:100%"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="bondVisible = false">关闭</el-button>
                <el-button v-if="!bondReadonly" type="primary" :loading="bondSaving" @click="saveBond">确定</el-button>
            </template>
        </el-dialog>

        <!-- 变更记录（仿 bondLogModal：80% 宽 + 带边框表格 + 头部余额） -->
        <el-dialog v-model="bondLogVisible" title="变更记录" width="80%" top="6vh" destroy-on-close>
            <div class="log-head">
                <span>承包商：{{ bondLogInfo.name }}</span>
                <span>保证金余额：{{ bondLogInfo.deposit ?? 0 }} 元</span>
            </div>
            <el-table :data="bondLogRows" v-loading="bondLogLoading" border max-height="480">
                <el-table-column type="index" label="序号" width="80" align="center"/>
                <el-table-column label="类型" width="90" align="center">
                    <template #default="{row}">
                        <span :class="row.type === 1 ? 't-cut' : 't-add'">{{ row.type === 1 ? '扣款' : '增加' }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="describe" label="描述" min-width="220" align="center">
                    <template #default="{row}">{{ row.describe || '—' }}</template>
                </el-table-column>
                <el-table-column prop="creator" label="操作人" width="120" align="center"/>
                <el-table-column prop="money" label="金额(元)" width="120" align="center"/>
                <el-table-column prop="change_time" label="时间" width="120" align="center"/>
                <el-table-column label="操作" width="80" align="center" fixed="right">
                    <template #default="{row}">
                        <el-link type="primary" @click="viewBond(row)">查看</el-link>
                    </template>
                </el-table-column>
                <template #empty>
                    <el-empty description="暂无变更记录"/>
                </template>
            </el-table>
            <el-pagination class="pager" small background layout="total, sizes, prev, pager, next"
                           :total="bondLogTotal" v-model:current-page="bondLogPage"
                           v-model:page-size="bondLogSize" :page-sizes="[10, 20, 50]"/>
            <template #footer>
                <el-button @click="bondLogVisible = false">关闭</el-button>
            </template>
        </el-dialog>

        <!-- 处罚（新增 / 只读查看，仿 addPunishModal 600px；附件/签字涉及文件服务，一期不做） -->
        <el-dialog v-model="punishVisible" :title="punishReadonly ? '处罚详情' : '处罚'" width="600px" destroy-on-close>
            <el-form ref="punishFormRef" :model="punishForm" :rules="punishRules" label-width="110px"
                     :disabled="punishReadonly">
                <el-form-item label="关联项目" prop="project_id">
                    <el-select v-model="punishForm.project_id" placeholder="请选择关联项目"
                               :loading="!punishReadonly && punishProjects.length === 0" style="width:100%">
                        <el-option v-for="p in punishProjects" :key="p.id" :value="p.id" :label="p.name"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="处罚类型" prop="type_id">
                    <el-select v-model="punishForm.type_id" placeholder="请选择处罚类型" style="width:100%">
                        <el-option v-for="t in punishTypes" :key="t.id" :value="t.id" :label="t.name"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="违规人" prop="punish_user">
                    <el-input v-model="punishForm.punish_user" placeholder="请输入违规人"/>
                </el-form-item>
                <el-form-item label="处罚金额(元)" prop="money">
                    <el-input-number v-model="punishForm.money" :min="0.01" :precision="2" :controls="false"
                                     placeholder="请输入处罚金额" style="width:100%"/>
                </el-form-item>
                <el-form-item label="描述">
                    <el-input v-model="punishForm.describe" type="textarea" :rows="3" placeholder="请输入描述"/>
                </el-form-item>
                <el-form-item label="处罚时间" prop="punish_time">
                    <el-date-picker v-model="punishForm.punish_time" type="date" value-format="YYYY-MM-DD"
                                    placeholder="请选择处罚时间" style="width:100%"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="punishVisible = false">关闭</el-button>
                <el-button v-if="!punishReadonly" type="primary" :loading="punishSaving" @click="savePunish">
                    确定
                </el-button>
            </template>
        </el-dialog>

        <!-- 处罚记录（仿 punishLogModal：80% 宽 + 带边框表格 + 头部余额） -->
        <el-dialog v-model="punishLogVisible" title="处罚记录" width="80%" top="6vh" destroy-on-close>
            <div class="log-head">
                <span>承包商：{{ punishLogInfo.name }}</span>
                <span>保证金余额：{{ punishLogInfo.deposit ?? 0 }} 元</span>
            </div>
            <el-table :data="punishLogRows" v-loading="punishLogLoading" border max-height="480">
                <el-table-column type="index" label="序号" width="80" align="center"/>
                <el-table-column prop="proj_name" label="所属项目" min-width="160" align="center">
                    <template #default="{row}">{{ row.proj_name || '—' }}</template>
                </el-table-column>
                <el-table-column prop="type_name" label="处罚类型" width="120" align="center">
                    <template #default="{row}">{{ row.type_name || '—' }}</template>
                </el-table-column>
                <el-table-column prop="describe" label="描述" min-width="180" align="center">
                    <template #default="{row}">{{ row.describe || '—' }}</template>
                </el-table-column>
                <el-table-column prop="punish_user" label="违规人" width="110" align="center"/>
                <el-table-column prop="money" label="处罚金额(元)" width="120" align="center"/>
                <el-table-column prop="punish_time" label="时间" width="120" align="center"/>
                <el-table-column label="操作" width="80" align="center" fixed="right">
                    <template #default="{row}">
                        <el-link type="primary" @click="viewPunish(row)">查看</el-link>
                    </template>
                </el-table-column>
                <template #empty>
                    <el-empty description="暂无处罚记录"/>
                </template>
            </el-table>
            <el-pagination class="pager" small background layout="total, sizes, prev, pager, next"
                           :total="punishLogTotal" v-model:current-page="punishLogPage"
                           v-model:page-size="punishLogSize" :page-sizes="[10, 20, 50]"/>
            <template #footer>
                <el-button @click="punishLogVisible = false">关闭</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.contractor-page {
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

    .state-tabs {
        margin-bottom: 12px;

        :deep(.el-tabs__header) {
            margin-bottom: 0;
        }
    }

    .toolbar {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 14px;

        .add-btn {
            margin-left: auto;
        }
    }

    /* 列表：单元格一律单行，超长用 tooltip 展示（对齐参考站 j-table-force-nowrap） */
    :deep(.el-table .cell) {
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    /* 操作列例外：链接必须完整显示，宁可横向滚动也不允许出省略号 */
    :deep(.el-table .op-cell .cell) {
        overflow: visible;
        text-overflow: clip;
    }

    :deep(.el-table .op-cell .el-divider--vertical) {
        margin: 0 6px;
    }

    :deep(.el-table .op-cell .el-link) {
        vertical-align: middle;
        font-weight: 400;
    }

    .approve-bar {
        display: flex;
        align-items: center;
        gap: 12px;
        margin: -4px 0 6px;
        padding: 10px 12px;
        border-radius: 8px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;

        .lb {
            font-weight: 700;
            color: #1e293b;
        }

        .reason {
            color: var(--el-color-danger);
            font-size: 12px;
        }
    }

    .view-form {
        :deep(.el-divider__text) {
            font-weight: 700;
            color: #1e293b;
            background: #ffffff;
        }

        :deep(.el-form-item) {
            margin-bottom: 14px;
        }
    }

    .log-head {
        display: flex;
        gap: 28px;
        margin-bottom: 10px;
        font-weight: 600;
        color: #1e293b;
    }

    .t-cut {
        color: var(--el-color-danger);
        font-weight: 600;
    }

    .t-add {
        color: var(--el-color-success);
        font-weight: 600;
    }

    .pager {
        margin-top: 12px;
        justify-content: flex-end;
    }
}
</style>
