<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {pagerOf} from "../system/Pager.js"
import {ElMessage, ElMessageBox} from "element-plus";
import {Search} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const sys = Singleton.getInstance(SysX)

/* ============ 两种页面：承包商单位管理 / 承包商审核 ============
 * 一个菜单 = 一个页面，页内 tab 只是分类（对齐目标站），不再是多套功能挤在一起。 */
const MODES = {
    unit: {
        title: '承包商单位管理',
        tabs: [
            {key: 'pass', label: '审批通过', status: 1},
            {key: 'wait', label: '待审批', status: 0},
            {key: 'reject', label: '未通过', status: 2},
            {key: 'recycle', label: '回收站', status: -1},
        ],
        canCreate: true,
    },
    audit: {
        title: '承包商审核',
        tabs: [
            {key: 'coop', label: '合作中', status: 1},
            {key: 'ever', label: '曾合作', status: 2},
            {key: 'black', label: '黑名单', status: -2},
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

/** 分类 → 后端过滤参数。回收站 = 已软删行，后端用 status=-1 约定 */
const curStatus = computed(() => (cfg.value.tabs.find(t => t.key === tabKey.value) || {}).status)

function loadRows() {
    AC.abort()
    AC = new AbortController()
    loading.value = true
    const paras = {pageNo: pageNo.value, pageSize: pageSize.value}
    const kw = keyword.value.trim()
    if (kw) paras.kw = kw
    if (curStatus.value !== undefined && curStatus.value !== null) paras.status = curStatus.value
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

/* 承包商类型：后端字典（contractor_type）优先，gd.json 兜底（字典未维护时仍可选） */
const ctypeDict = ref([])

function loadCtypeDict() {
    // 下拉要全量：字典接口已分页，按上限取一页（≤200）
    sys.biz('cbs/dict/list', {dictType: 'contractor_type', onlyUsing: true, pageNo: 1, pageSize: 200},
        AC.signal, () => {}, (r, data) => {
            if (r) ctypeDict.value = (data.data?.rows || []).map(d => d.item_name)
        })
}

const ctypeOptions = computed(() => {
    const names = ctypeDict.value.filter(Boolean)
    return names.length ? names : gd.ctypeOptions
})

/* ============ 新增/编辑 ============ */
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
    name: [{required: true, message: '请输入承包商名称', trigger: 'blur'}],
    uscc: [{required: true, message: '请输入统一社会信用代码', trigger: 'blur'}],
    ctype: [{required: true, message: '请选择承包商类型', trigger: 'change'}],
    legal_person: [{required: true, message: '请输入法定代表人', trigger: 'blur'}],
    legal_phone: [{required: true, message: '请输入法定代表人联系电话', trigger: 'blur'}],
    safety_person: [{required: true, message: '请输入安全负责人', trigger: 'blur'}],
    safety_phone: [{required: true, message: '请输入安全负责人电话', trigger: 'blur'}],
}

function openDialog(edit, row) {
    form.value = edit
        ? {
            contractor_id: row.contractor_id, name: row.name, uscc: row.uscc || '',
            ctype: row.ctype || '', legal_person: row.legal_person || '', legal_phone: row.legal_phone || '',
            safety_person: row.safety_person || '', safety_phone: row.safety_phone || '',
            register_address: row.register_address || '', register_capital: row.register_capital || '',
            deposit: row.deposit ?? undefined, login_account: row.login_account || '', _edit: true,
        }
        : {
            contractor_id: '', name: '', uscc: '', ctype: '', legal_person: '', legal_phone: '',
            safety_person: '', safety_phone: '', register_address: '', register_capital: '',
            deposit: undefined, login_account: '', _edit: false,
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
        sys.biz('cbs/contractor/audit', f, AC.signal, () => {}, (r, data) => {
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

/* ============ 删除 ============ */
function removeRow(row) {
    ElMessageBox.confirm(`确定删除承包商「${row.name}」吗？`, '提示', {type: 'warning'}).then(() => {
        loading.value = true
        sys.biz('cbs/contractor/delete', {contractorId: row.contractor_id}, AC.signal, () => {}, (r, data) => {
            loading.value = false
            if (r) {
                ElMessage.success('删除成功')
                loadRows()
            } else {
                notifyError(data, '删除失败')
            }
        })
    }).catch(() => {})
}

/* ============ 生命周期 ============ */
onMounted(() => {
    loadRows()
    loadCtypeDict()
})
onUnmounted(() => {
    AC.abort()
    clearTimeout(kwTimer)
})
</script>

<template>
    <div class="contractor-page">
        <div class="page-head">
            <h2>{{ cfg.title }}</h2>
        </div>

        <div class="card">
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

            <el-tabs v-model="tabKey" class="cat-tabs">
                <el-tab-pane v-for="t in cfg.tabs" :key="t.key" :label="t.label" :name="t.key"/>
            </el-tabs>

            <el-table :data="rows" v-loading="loading" border stripe>
                <el-table-column type="index" label="序号" width="60" align="center"/>
                <el-table-column prop="name" label="承包商名称" min-width="190"/>
                <el-table-column prop="uscc" label="统一社会信用代码" min-width="180" show-overflow-tooltip/>
                <el-table-column prop="ctype" label="承包商类型" width="130"/>
                <el-table-column prop="legal_person" label="法定代表人" width="110"/>
                <el-table-column prop="safety_person" label="安全负责人" width="110"/>
                <el-table-column prop="safety_phone" label="安全负责人电话" width="140"/>
                <el-table-column v-if="mode === 'unit'" label="审批状态" width="100" align="center">
                    <template #default="{row}">
                        <el-tag :type="statusMeta(row.approve_status).type" effect="light">
                            {{ statusMeta(row.approve_status).desc }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="200" align="center" fixed="right">
                    <template #default="{row}">
                        <el-link type="primary" @click="openDialog(true, row)">查看</el-link>
                        <el-link v-if="row.approve_status === 0" type="warning" style="margin-left:10px"
                                 @click="openAudit(row)">审核</el-link>
                        <el-link v-if="mode === 'unit'" type="danger" style="margin-left:10px"
                                 @click="removeRow(row)">删除</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!loading && rows.length === 0" description="暂无承包商"/>

            <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                           :total="total" v-model:current-page="pageNo" v-model:page-size="pageSize"
                           :page-sizes="pager.pageSizes"/>
        </div>

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
                        <el-form-item label="登录账号">
                            <el-input v-model="form.login_account" placeholder="预留，一期不启用"/>
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

    .toolbar {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 6px;

        .add-btn {
            margin-left: auto;
        }
    }

    .cat-tabs {
        :deep(.el-tabs__header) {
            margin-bottom: 12px;
        }

        :deep(.el-tabs__item) {
            font-size: 13px;
        }
    }

    .pager {
        margin-top: 14px;
        justify-content: flex-end;
    }
}
</style>
