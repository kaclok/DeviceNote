<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {pagerOf} from "../system/Pager.js"
import {ElMessage, ElMessageBox} from "element-plus";
import {Plus, Search} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"
import {useDict} from "../system/DictX.js"

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

/** 操作列宽随切页变化（按内容实测预留：6 个链接 400 / 审核 90 / 查看+删除 150 / 回收站 220） */
const opWidth = computed(() => {
    if (mode.value === 'audit') return 340
    return {pass: 400, wait: 90, reject: 150, recycle: 220}[tabKey.value] || 150
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

/* 承包商类型：字典项可在「基础设置 › 承包商类型」自行增减 ⇒ 选项一律现查后端，gd.json 不存选项；
 * 表单/筛选存 item_id，列表与查看显示后端回填的 ctype_name */
const ctypeOptions = useDict('contractor_type')

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

/* ============ 承包商审核：资质证件 / 审核所需文件 ============
 * 清单来自「基础设置 › 承包商类型」里该类型维护的资质管理(qualType=1) / 审核所需文件(qualType=2)，
 * 本页只逐项记录这家承包商的审核结果（未审核 / 符合 / 不符合）与有效期，不改清单本身。 */
const QUAL_TITLE = {1: '资质证件', 2: '审核所需文件'}
const CHECK_OPTIONS = [
    {id: 0, desc: '未审核'},
    {id: 1, desc: '符合'},
    {id: 2, desc: '不符合'},
]
const qualVisible = ref(false)
const qualType = ref(1)
const qualCtx = ref({})
const qualRows = ref([])
const qualLoading = ref(false)
const qualSaving = ref(false)
const qualTitle = computed(() => `${QUAL_TITLE[qualType.value]} — ${qualCtx.value.name || ''}`)

function openQual(row, type) {
    qualCtx.value = {contractor_id: row.contractor_id, name: row.name}
    qualType.value = type
    qualRows.value = []
    qualVisible.value = true
    loadQual()
}

function loadQual() {
    const cid = qualCtx.value.contractor_id
    if (!cid) return
    qualLoading.value = true
    sys.biz('cbs/contractor/qualCheck/list', {contractorId: cid, qualType: qualType.value}, OPAC.signal,
        () => {
            qualLoading.value = false
        }, (r, data) => {
            qualLoading.value = false
            if (r) qualRows.value = data.data || []
            else notifyError(data, '审核清单加载失败')
        })
}

/** 整单提交（后端逐行校验 qual_id 属于该承包商类型，白名单外整单拒绝） */
function saveQual() {
    const cid = qualCtx.value.contractor_id
    if (!cid || !qualRows.value.length) return
    const rows = qualRows.value.map(x => ({
        qual_id: x.qual_id,
        check_status: Number(x.check_status ?? 0),
        valid_date: x.valid_date || null,
        remark: x.remark || null,
    }))
    qualSaving.value = true
    sys.biz('cbs/contractor/qualCheck/save',
        {contractorId: cid, qualType: qualType.value, rows}, OPAC.signal,
        () => {
            qualSaving.value = false
        }, (r, data) => {
            qualSaving.value = false
            if (r) {
                ElMessage.success('审核结果已保存')
                qualVisible.value = false
            } else {
                notifyError(data, '审核结果保存失败')
            }
        })
}

/* ============ 承包商审核：人员管理 ============
 * 人员库(t_person) 是全局台账，承包商人员走关联表(t_contractor_person)；
 * 「批量审核」= 勾选人员库里的人一次性写入关联（幂等：已在库跳过、软删过的恢复）。 */
const personPager = pagerOf('contractor.person')
const personVisible = ref(false)
const personCtx = ref({})
const personRows = ref([])
const personLoading = ref(false)
const personPageNo = ref(1)
const personSize = ref(personPager.pageSize)
const personTotal = ref(0)

const libraryPager = pagerOf('contractor.library')
const libraryVisible = ref(false)
const libraryRows = ref([])
const libraryLoading = ref(false)
const libraryKw = ref('')
const libraryPageNo = ref(1)
const librarySize = ref(libraryPager.pageSize)
const libraryTotal = ref(0)
const librarySel = ref([])
const librarySaving = ref(false)
let libKwTimer = null

const personFormVisible = ref(false)
const personFormRef = ref()
const personSaving = ref(false)
const emptyPersonForm = () => ({
    person_id: '', name: '', id_card: '', phone: '', gender: 1,
    post_name: '', cert_name: '', cert_no: '', entry_date: '', remark: '',
})
const personForm = ref(emptyPersonForm())
const personRules = {name: [{required: true, message: '请输入姓名', trigger: 'blur'}]}

function openPerson(row) {
    personCtx.value = {contractor_id: row.contractor_id, name: row.name}
    personRows.value = []
    personPageNo.value = 1
    personVisible.value = true
    loadPersons()
}

function loadPersons() {
    const cid = personCtx.value.contractor_id
    if (!cid) return
    personLoading.value = true
    sys.biz('cbs/contractor/person/list',
        {contractorId: cid, pageNo: personPageNo.value, pageSize: personSize.value}, OPAC.signal,
        () => {
            personLoading.value = false
        }, (r, data) => {
            personLoading.value = false
            if (r) {
                personRows.value = data.data?.rows || []
                personTotal.value = data.data?.total || 0
            } else {
                notifyError(data, '承包商人员加载失败')
            }
        })
}

watch([personPageNo, personSize], () => {
    if (personVisible.value) loadPersons()
})

function openLibrary() {
    libraryKw.value = ''
    libraryPageNo.value = 1
    librarySel.value = []
    libraryRows.value = []
    libraryVisible.value = true
    loadLibrary()
}

function loadLibrary() {
    libraryLoading.value = true
    sys.biz('cbs/person/library',
        {
            contractorId: personCtx.value.contractor_id,
            kw: libraryKw.value.trim(),
            pageNo: libraryPageNo.value,
            pageSize: librarySize.value,
        }, OPAC.signal, () => {
            libraryLoading.value = false
        }, (r, data) => {
            libraryLoading.value = false
            if (r) {
                libraryRows.value = data.data?.rows || []
                libraryTotal.value = data.data?.total || 0
            } else {
                notifyError(data, '人员库加载失败')
            }
        })
}

watch([libraryPageNo, librarySize], () => {
    if (libraryVisible.value) loadLibrary()
})
watch(libraryKw, () => {
    clearTimeout(libKwTimer)
    libKwTimer = setTimeout(() => {
        libraryPageNo.value = 1
        loadLibrary()
    }, 300)
})

function onLibrarySel(rows) {
    librarySel.value = rows.map(r => r.person_id)
}

/** 批量审核：多选人员 → 一次性加入该承包商人员库 */
function auditBatch() {
    const ids = librarySel.value.filter(Boolean)
    if (!ids.length) {
        ElMessage.warning('请先勾选人员')
        return
    }
    librarySaving.value = true
    sys.biz('cbs/contractor/person/auditBatch',
        {contractorId: personCtx.value.contractor_id, personIds: ids}, OPAC.signal,
        () => {
            librarySaving.value = false
        }, (r, data) => {
            librarySaving.value = false
            if (r) {
                const joined = data.data?.joined ?? ids.length
                const skipped = data.data?.skipped ?? 0
                ElMessage.success(skipped ? `已加入 ${joined} 人，${skipped} 人已在库中跳过` : `已加入 ${joined} 人`)
                libraryVisible.value = false
                personPageNo.value = 1
                loadPersons()
            } else {
                notifyError(data, '批量审核失败')
            }
        })
}

function openPersonForm() {
    personForm.value = emptyPersonForm()
    personFormVisible.value = true
}

function savePerson() {
    personFormRef.value.validate(valid => {
        if (!valid) return
        personSaving.value = true
        const f = {...personForm.value, contractor_id: personCtx.value.contractor_id}
        if (!f.entry_date) f.entry_date = null
        sys.biz('cbs/person/save', f, OPAC.signal, () => {
            personSaving.value = false
        }, (r, data) => {
            personSaving.value = false
            if (r) {
                ElMessage.success('人员已加入')
                personFormVisible.value = false
                personPageNo.value = 1
                loadPersons()
            } else {
                notifyError(data, '人员保存失败')
            }
        })
    })
}

function removePerson(row) {
    ElMessageBox.confirm(`确定将「${row.name}」移出该承包商人员库吗？`, '提示', {type: 'warning'}).then(() => {
        sys.biz('cbs/contractor/person/remove',
            {contractorId: personCtx.value.contractor_id, personId: row.person_id}, OPAC.signal,
            () => {
            }, (r, data) => {
                if (r) {
                    ElMessage.success('已移出')
                    loadPersons()
                } else {
                    notifyError(data, '移出失败')
                }
            })
    }).catch(() => {
    })
}

/* ============ 删除 / 恢复 / 彻底删除 ============ */
function removeRow(row) {
    ElMessageBox.confirm(`确定删除承包商「${row.name}」吗？`, '提示', {type: 'warning'}).then(() => {
        loading.value = true
        sys.biz('cbs/contractor/delete', {contractorId: row.contractor_id}, OPAC.signal,
            () => { loading.value = false }, (r, data) => {
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
    sys.biz('cbs/contractor/restore', {contractorId: row.contractor_id}, OPAC.signal,
        () => { loading.value = false }, (r, data) => {
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
        sys.biz('cbs/contractor/purge', {contractorId: row.contractor_id}, OPAC.signal,
            () => { loading.value = false }, (r, data) => {
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
                    <el-option v-for="t in ctypeOptions" :key="t.id" :value="t.id" :label="t.name"/>
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
                <el-table-column prop="ctype_name" label="承包商类型" min-width="130" show-overflow-tooltip/>
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
                <el-table-column v-if="mode === 'unit'" label="合作状态" width="100" align="center">
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
                        <template v-else-if="mode === 'unit' && tabKey === 'recycle'">
                            <el-link type="primary" @click="openView(row)">查看</el-link>
                            <el-divider direction="vertical"/>
                            <el-link type="success" @click="restoreRow(row)">恢复</el-link>
                            <el-divider direction="vertical"/>
                            <el-link type="danger" @click="purgeRow(row)">彻底删除</el-link>
                        </template>
                        <!-- 审核页：编辑｜资质证件｜审核所需文件｜人员管理（对齐参考站 contractorApproval） -->
                        <template v-else-if="mode === 'audit'">
                            <el-link type="primary" @click="openDialog(true, row)">编辑</el-link>
                            <el-divider direction="vertical"/>
                            <el-link type="primary" @click="openQual(row, 1)">资质证件</el-link>
                            <el-divider direction="vertical"/>
                            <el-link type="primary" @click="openQual(row, 2)">审核所需文件</el-link>
                            <el-divider direction="vertical"/>
                            <el-link type="primary" @click="openPerson(row)">人员管理</el-link>
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
                            <el-input :model-value="viewRow.ctype_name || '—'"/>
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

        <!-- 资质证件 / 审核所需文件：清单来自该承包商类型下的配置，这里只逐项审结果 -->
        <el-dialog v-model="qualVisible" :title="qualTitle" width="900px" top="6vh" destroy-on-close>
            <div class="qual-tip">
                清单取自「基础设置 › 承包商类型 › {{ qualType === 1 ? '资质管理' : '审核所需文件' }}」，
                要增删清单项请到该页维护。
            </div>
            <el-table :data="qualRows" v-loading="qualLoading" border max-height="460">
                <el-table-column type="index" label="序号" width="70" align="center"/>
                <el-table-column prop="qual_name" :label="qualType === 1 ? '资质名称' : '文件名称'" min-width="200"/>
                <el-table-column prop="qual_level" label="等级/类别" width="120">
                    <template #default="{row}">{{ row.qual_level || '—' }}</template>
                </el-table-column>
                <el-table-column label="审核结果" width="130" align="center">
                    <template #default="{row}">
                        <el-select v-model="row.check_status" size="small">
                            <el-option v-for="o in CHECK_OPTIONS" :key="o.id" :value="o.id" :label="o.desc"/>
                        </el-select>
                    </template>
                </el-table-column>
                <el-table-column label="有效期至" width="160" align="center">
                    <template #default="{row}">
                        <el-date-picker v-model="row.valid_date" type="date" size="small"
                                        value-format="YYYY-MM-DD" placeholder="长期有效"
                                        style="width:100%"/>
                    </template>
                </el-table-column>
                <el-table-column label="备注" min-width="170">
                    <template #default="{row}">
                        <el-input v-model="row.remark" size="small" placeholder="选填"/>
                    </template>
                </el-table-column>
                <template #empty>
                    <el-empty description="该承包商类型还没有配置清单，请先到「基础设置 › 承包商类型」维护"/>
                </template>
            </el-table>
            <template #footer>
                <el-button @click="qualVisible = false">关闭</el-button>
                <el-button type="primary" :loading="qualSaving" :disabled="!qualRows.length"
                           @click="saveQual">保存审核结果</el-button>
            </template>
        </el-dialog>

        <!-- 人员管理：该承包商人员库 + 从人员库多选批量审核 -->
        <el-dialog v-model="personVisible" :title="`人员管理 — ${personCtx.name || ''}`" width="960px"
                   top="6vh" destroy-on-close>
            <div class="table-operator">
                <el-button type="primary" :icon="Plus" @click="openPersonForm">新增人员</el-button>
                <el-button type="success" plain @click="openLibrary">从人员库添加</el-button>
                <span class="op-ctx">共 {{ personTotal }} 人</span>
            </div>
            <el-table :data="personRows" v-loading="personLoading" border stripe>
                <el-table-column type="index" label="序号" width="70" align="center"
                                 :index="i => (personPageNo - 1) * personSize + i + 1"/>
                <el-table-column prop="name" label="姓名" width="110"/>
                <el-table-column prop="id_card" label="身份证号" min-width="170" show-overflow-tooltip/>
                <el-table-column prop="phone" label="手机号码" width="130"/>
                <el-table-column label="性别" width="70" align="center">
                    <template #default="{row}">{{ row.gender === 2 ? '女' : (row.gender === 1 ? '男' : '—') }}</template>
                </el-table-column>
                <el-table-column prop="post_name" label="岗位/工种" width="120" show-overflow-tooltip/>
                <el-table-column prop="cert_name" label="证书名称" width="140" show-overflow-tooltip/>
                <el-table-column prop="audit_time" label="加入时间" width="150" align="center">
                    <template #default="{row}">{{ row.audit_time || '—' }}</template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="center" fixed="right">
                    <template #default="{row}">
                        <el-link type="danger" @click="removePerson(row)">移出</el-link>
                    </template>
                </el-table-column>
                <template #empty>
                    <el-empty description="该承包商还没有人员，点「从人员库添加」多选后批量审核"/>
                </template>
            </el-table>
            <el-pagination class="pager" small background layout="total, sizes, prev, pager, next"
                           :total="personTotal" v-model:current-page="personPageNo"
                           v-model:page-size="personSize" :page-sizes="personPager.pageSizes"/>
            <template #footer>
                <el-button @click="personVisible = false">关闭</el-button>
            </template>

            <!-- 二级弹窗：人员库多选 → 批量审核 -->
            <el-dialog v-model="libraryVisible" title="从人员库添加（多选后批量审核）" width="820px"
                       append-to-body destroy-on-close class="sub-dialog">
                <div class="lib-toolbar">
                    <el-input v-model="libraryKw" placeholder="姓名 / 身份证 / 手机号" clearable
                              style="width:250px" :prefix-icon="Search"/>
                    <span class="op-ctx">已选 {{ librarySel.length }} 人</span>
                </div>
                <el-table :data="libraryRows" v-loading="libraryLoading" border max-height="380"
                          row-key="person_id" @selection-change="onLibrarySel">
                    <el-table-column type="selection" width="50" align="center"/>
                    <el-table-column prop="name" label="姓名" width="110"/>
                    <el-table-column prop="id_card" label="身份证号" min-width="170" show-overflow-tooltip/>
                    <el-table-column prop="phone" label="手机号码" width="130"/>
                    <el-table-column prop="post_name" label="岗位/工种" width="120"/>
                    <el-table-column prop="cert_name" label="证书名称" min-width="150" show-overflow-tooltip/>
                    <template #empty>
                        <el-empty description="人员库为空，或该承包商已把人员全部加入"/>
                    </template>
                </el-table>
                <el-pagination class="pager" small background layout="total, prev, pager, next"
                               :total="libraryTotal" v-model:current-page="libraryPageNo"
                               v-model:page-size="librarySize"/>
                <template #footer>
                    <el-button @click="libraryVisible = false">关闭</el-button>
                    <el-button type="primary" :loading="librarySaving" :disabled="!librarySel.length"
                               @click="auditBatch">批量审核</el-button>
                </template>
            </el-dialog>
        </el-dialog>

        <!-- 新增人员（入人员库并直接加入本承包商） -->
        <el-dialog v-model="personFormVisible" title="新增人员" width="660px"
                   append-to-body destroy-on-close class="sub-dialog">
            <el-form ref="personFormRef" :model="personForm" :rules="personRules" label-width="110px">
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="姓名" prop="name">
                            <el-input v-model="personForm.name" placeholder="请输入姓名"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="身份证号">
                            <el-input v-model="personForm.id_card"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="手机号码">
                            <el-input v-model="personForm.phone"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="性别">
                            <el-radio-group v-model="personForm.gender">
                                <el-radio :value="1">男</el-radio>
                                <el-radio :value="2">女</el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="岗位/工种">
                            <el-input v-model="personForm.post_name"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="进场日期">
                            <el-date-picker v-model="personForm.entry_date" type="date" value-format="YYYY-MM-DD"
                                            style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="证书名称">
                            <el-input v-model="personForm.cert_name"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="证书编号">
                            <el-input v-model="personForm.cert_no"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="备注">
                            <el-input v-model="personForm.remark" type="textarea" :rows="2"/>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <el-button @click="personFormVisible = false">取消</el-button>
                <el-button type="primary" :loading="personSaving" @click="savePerson">保存并加入</el-button>
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
                                <el-option v-for="t in ctypeOptions" :key="t.id" :value="t.id" :label="t.name"/>
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

    /* 弹窗内工具条：左按钮、右上下文（与字典页口径一致） */
    .table-operator {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 12px;

        .op-ctx {
            margin-left: auto;
            color: #8a94a6;
            font-size: 13px;
        }
    }

    .lib-toolbar {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 12px;

        .op-ctx {
            margin-left: auto;
            color: #8a94a6;
            font-size: 13px;
        }
    }

    /* 审核清单来源说明 */
    .qual-tip {
        margin-bottom: 10px;
        padding: 8px 12px;
        border-radius: 6px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        color: #64748b;
        font-size: 13px;
    }

    .pager {
        margin-top: 12px;
        justify-content: flex-end;
    }
}
</style>
