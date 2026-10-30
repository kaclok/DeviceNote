<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {pagerOf} from "../system/Pager.js"
import {ElMessage, ElMessageBox} from "element-plus";
import {Search} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const sys = Singleton.getInstance(SysX)

/* ============ 五个菜单 = 五个页面（同一份项目列表，行内操作不同） ============ */
const PAGE_TITLE = {
    proj: '项目管理',
    state: '项目状态',
    enter: '入场管理',
    prep: '开工准备',
    accept: '项目验收',
}
const KEYS = Object.keys(PAGE_TITLE)
const props = defineProps({tab: String})
const activeTab = ref(KEYS.includes(props?.tab) ? props.tab : 'proj')
watch(() => props.tab, t => {
    if (!KEYS.includes(t) || t === activeTab.value) return
    activeTab.value = t
    // 每个菜单是独立页面，分页参数各用各的
    pageNo.value = 1
    pageSize.value = pager.value.pageSize
    loadRows()
})
const pageTitle = computed(() => PAGE_TITLE[activeTab.value])

/* ============ 项目列表 + 分页 ============ */
const loading = ref(false)
const rows = ref([])
const keyword = ref('')
const contractorFilter = ref('')
const statusFilter = ref('')
// 每页独立分页配置：project.proj / state / enter / prep / accept
const pager = computed(() => pagerOf('project.' + activeTab.value))
const pageNo = ref(1)
const pageSize = ref(pager.value.pageSize)
const total = ref(0)
let kwTimer = null

let AC = new AbortController()

function loadRows() {
    AC.abort()
    AC = new AbortController()
    loading.value = true
    const paras = {pageNo: pageNo.value, pageSize: pageSize.value}
    const kw = keyword.value.trim()
    if (kw) paras.kw = kw
    if (contractorFilter.value) paras.contractorId = contractorFilter.value
    if (statusFilter.value !== '') paras.projStatus = statusFilter.value
    sys.biz('cbs/project/list', paras, AC.signal, () => {}, (r, data) => {
        loading.value = false
        if (r) {
            rows.value = data.data?.rows || []
            total.value = data.data?.total || 0
        } else {
            notifyError(data, '项目列表加载失败')
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
watch([contractorFilter, statusFilter], () => {
    pageNo.value = 1
    loadRows()
})
watch([pageNo, pageSize], loadRows)

/* 承包商下拉（筛选 + 表单共用，走 options 接口，不受列表分页影响） */
const contractorOptions = ref([])

function loadContractors() {
    sys.biz('cbs/contractor/options', {}, AC.signal, () => {}, (r, data) => {
        if (r) contractorOptions.value = (data.data || []).map(c => ({value: c.contractor_id, label: c.name}))
    })
}

function contractorName(id) {
    return contractorOptions.value.find(c => c.value === id)?.label || id || '—'
}

function statusMeta(s) {
    return gd.projStatus.find(x => x.id === Number(s)) || {desc: s ?? '—', type: 'info'}
}

/* 所属单位：t_project.dept_code（组织树解析待 cbs:org 字典导出后接入，先原样显示） */
function deptName(code) {
    return code || '—'
}

/* ============ 新增/编辑项目 ============ */
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
    proj_name: [{required: true, message: '请输入项目名称', trigger: 'blur'}],
    contractor_id: [{required: true, message: '请选择承包商单位', trigger: 'change'}],
}

function openDialog(edit, row) {
    form.value = edit
        ? {
            project_id: row.project_id, contractor_id: row.contractor_id, proj_name: row.proj_name,
            proj_content: row.proj_content || '', begin_date: row.begin_date || '',
            end_date: row.end_date || '', proj_status: row.proj_status ?? 0, _edit: true,
        }
        : {
            project_id: '', contractor_id: contractorFilter.value || '', proj_name: '', proj_content: '',
            begin_date: '', end_date: '', proj_status: 0, _edit: false,
        }
    dialogVisible.value = true
}

function saveForm() {
    formRef.value.validate(valid => {
        if (!valid) return
        saving.value = true
        const f = {...form.value}
        delete f._edit
        if (!f.begin_date) f.begin_date = null
        if (!f.end_date) f.end_date = null
        sys.biz('cbs/project/save', f, AC.signal, () => {}, (r, data) => {
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
    ElMessageBox.confirm(`确定删除项目「${row.proj_name}」吗？`, '提示', {type: 'warning'}).then(() => {
        loading.value = true
        sys.biz('cbs/project/delete', {projectId: row.project_id}, AC.signal, () => {}, (r, data) => {
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

/* ============ 项目状态：只改状态，整行其它字段原样回传（后端 update 全列覆盖） ============ */
const stateVisible = ref(false)
const stateSaving = ref(false)
const stateRow = ref({})
const stateValue = ref(0)

function openState(row) {
    stateRow.value = row
    stateValue.value = row.proj_status ?? 0
    stateVisible.value = true
}

function saveState() {
    const r = stateRow.value
    stateSaving.value = true
    const payload = {
        project_id: r.project_id,
        contractor_id: r.contractor_id,
        proj_name: r.proj_name,
        proj_content: r.proj_content || null,
        begin_date: r.begin_date || null,
        end_date: r.end_date || null,
        proj_status: stateValue.value,
        dept_code: r.dept_code || null,
    }
    sys.biz('cbs/project/save', payload, AC.signal, () => {}, (r2, data) => {
        stateSaving.value = false
        if (r2) {
            ElMessage.success('状态已更新')
            stateVisible.value = false
            loadRows()
        } else {
            notifyError(data, '更新失败')
        }
    })
}

/* ============ 子档（入场/开工准备/验收）：一个弹窗内 列表 + 内联表单 ============
 * 三块结构一致，字段差异用 SUB_CFG 描述，避免三份重复实现。 */
const SUB_CFG = {
    enter: {
        url: 'enter', title: '入场记录', idField: 'enter_id', idParam: 'enterId',
        cols: [
            {prop: 'enter_date', label: '入场日期', width: 120},
            {prop: 'person_count', label: '入场人数', width: 100},
            {prop: 'leader', label: '入场负责人', width: 120},
            {prop: 'leader_phone', label: '联系电话', width: 140},
            {prop: 'remark', label: '备注', min: 160},
        ],
        fields: [
            {key: 'enter_date', label: '入场日期', kind: 'date'},
            {key: 'person_count', label: '入场人数', kind: 'number'},
            {key: 'leader', label: '入场负责人'},
            {key: 'leader_phone', label: '联系电话'},
            {key: 'remark', label: '备注', kind: 'textarea'},
        ],
        blank: {enter_date: '', person_count: null, leader: '', leader_phone: '', remark: ''},
        rules: {enter_date: [{required: true, message: '请选择入场日期', trigger: 'change'}]},
    },
    prep: {
        url: 'prep', title: '开工准备', idField: 'prep_id', idParam: 'prepId',
        cols: [
            {prop: 'prep_date', label: '日期', width: 120},
            {prop: 'content', label: '准备事项', min: 220},
            {prop: 'checker', label: '确认人', width: 110},
            {prop: 'remark', label: '备注', min: 140},
        ],
        fields: [
            {key: 'prep_date', label: '日期', kind: 'date'},
            {key: 'content', label: '准备事项', kind: 'textarea'},
            {key: 'checker', label: '确认人'},
            {key: 'remark', label: '备注', kind: 'textarea'},
        ],
        blank: {prep_date: '', content: '', checker: '', remark: ''},
        rules: {content: [{required: true, message: '请输入准备事项', trigger: 'blur'}]},
    },
    accept: {
        url: 'accept', title: '验收记录', idField: 'accept_id', idParam: 'acceptId',
        cols: [
            {prop: 'accept_date', label: '验收日期', width: 120},
            {prop: 'acceptor', label: '验收人', width: 110},
            {prop: 'opinion', label: '验收意见', min: 200},
            {prop: 'remark', label: '备注', min: 140},
        ],
        fields: [
            {key: 'accept_date', label: '验收日期', kind: 'date'},
            {key: 'acceptor', label: '验收人'},
            {key: 'opinion', label: '验收意见', kind: 'textarea'},
            {key: 'remark', label: '备注', kind: 'textarea'},
        ],
        blank: {accept_date: '', acceptor: '', opinion: '', remark: ''},
        rules: {accept_date: [{required: true, message: '请选择验收日期', trigger: 'change'}]},
    },
}

const subVisible = ref(false)
const subProject = ref({})
const subCfg = computed(() => SUB_CFG[activeTab.value] || null)
const subRows = ref([])
const subLoading = ref(false)
const subFormRef = ref()
const subForm = ref({})
const subSaving = ref(false)

function openSub(row) {
    subProject.value = row
    subForm.value = {...subCfg.value.blank}
    subVisible.value = true
    loadSubRows()
}

function loadSubRows() {
    const c = subCfg.value
    if (!c) return
    subLoading.value = true
    sys.biz(`cbs/project/${c.url}/list`, {projectId: subProject.value.project_id, pageNo: 1, pageSize: 100},
        AC.signal, () => {}, (r, data) => {
            subLoading.value = false
            if (r) subRows.value = data.data?.rows || []
            else notifyError(data, `${c.title}加载失败`)
        })
}

function editSub(row) {
    const c = subCfg.value
    const f = {...c.blank}
    c.fields.forEach(x => {
        f[x.key] = row[x.key] ?? f[x.key]
    })
    f[c.idField] = row[c.idField]
    subForm.value = f
}

function saveSub() {
    subFormRef.value.validate(valid => {
        if (!valid) return
        const c = subCfg.value
        subSaving.value = true
        const f = {project_id: subProject.value.project_id, ...subForm.value}
        c.fields.forEach(x => {
            if (x.kind === 'date' && !f[x.key]) f[x.key] = null
            if (x.kind === 'number' && (f[x.key] === '' || f[x.key] == null)) f[x.key] = null
        })
        sys.biz(`cbs/project/${c.url}/save`, f, AC.signal, () => {}, (r, data) => {
            subSaving.value = false
            if (r) {
                ElMessage.success('保存成功')
                subForm.value = {...c.blank}
                loadSubRows()
            } else {
                notifyError(data, '保存失败')
            }
        })
    })
}

function removeSub(row) {
    const c = subCfg.value
    ElMessageBox.confirm(`确定删除这条${c.title}吗？`, '提示', {type: 'warning'}).then(() => {
        subLoading.value = true
        sys.biz(`cbs/project/${c.url}/delete`, {[c.idParam]: row[c.idField]}, AC.signal, () => {}, (r, data) => {
            subLoading.value = false
            if (r) {
                ElMessage.success('删除成功')
                loadSubRows()
            } else {
                notifyError(data, '删除失败')
            }
        })
    }).catch(() => {})
}

/* ============ 生命周期 ============ */
onMounted(() => {
    loadRows()
    loadContractors()
})
onUnmounted(() => {
    AC.abort()
    clearTimeout(kwTimer)
})
</script>

<template>
    <div class="project-page">
        <div class="page-head">
            <h2>{{ pageTitle }}</h2>
        </div>

        <div class="card">
            <div class="toolbar">
                <el-input v-model="keyword" placeholder="项目名称" clearable
                          style="width:200px" :prefix-icon="Search"/>
                <el-select v-model="contractorFilter" placeholder="承包商" clearable filterable
                           style="width:200px">
                    <el-option v-for="c in contractorOptions" :key="c.value" :value="c.value" :label="c.label"/>
                </el-select>
                <el-select v-model="statusFilter" placeholder="项目状态" clearable style="width:130px">
                    <el-option v-for="s in gd.projStatus" :key="s.id" :value="s.id" :label="s.desc"/>
                </el-select>
                <el-button type="primary" @click="pageNo = 1; loadRows()">查询</el-button>
                <el-button @click="keyword = ''; contractorFilter = ''; statusFilter = ''; pageNo = 1">重置</el-button>
                <el-button v-if="activeTab === 'proj'" type="primary" plain class="add-btn"
                           @click="openDialog(false)">新增
                </el-button>
            </div>

            <el-table :data="rows" v-loading="loading" border stripe>
                <el-table-column prop="proj_name" label="项目名称" min-width="180"/>
                <el-table-column label="所属单位" width="140">
                    <template #default="{row}">{{ deptName(row.dept_code) }}</template>
                </el-table-column>
                <el-table-column label="承包商" min-width="170">
                    <template #default="{row}">{{ row.contractor_name || contractorName(row.contractor_id) }}</template>
                </el-table-column>
                <el-table-column label="项目周期" width="210">
                    <template #default="{row}">
                        <span v-if="row.begin_date || row.end_date">
                            {{ row.begin_date || '—' }} ~ {{ row.end_date || '—' }}
                        </span>
                        <span v-else class="dim">—</span>
                    </template>
                </el-table-column>
                <el-table-column label="项目状态" width="100" align="center">
                    <template #default="{row}">
                        <el-tag :type="statusMeta(row.proj_status).type" effect="light">
                            {{ statusMeta(row.proj_status).desc }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="190" align="center" fixed="right">
                    <template #default="{row}">
                        <template v-if="activeTab === 'proj'">
                            <el-link type="primary" @click="openDialog(true, row)">编辑</el-link>
                            <el-link type="danger" style="margin-left:10px" @click="removeRow(row)">删除</el-link>
                        </template>
                        <el-link v-else-if="activeTab === 'state'" type="primary" @click="openState(row)">
                            变更状态
                        </el-link>
                        <el-link v-else type="primary" @click="openSub(row)">
                            {{ subCfg.title }}
                        </el-link>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!loading && rows.length === 0" description="暂无项目"/>

            <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                           :total="total" v-model:current-page="pageNo" v-model:page-size="pageSize"
                           :page-sizes="pager.pageSizes"/>
        </div>

        <!-- 新增/编辑项目 -->
        <el-dialog v-model="dialogVisible" :title="form._edit ? '编辑项目' : '新增项目'" width="620px" destroy-on-close>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
                <el-form-item label="项目名称" prop="proj_name">
                    <el-input v-model="form.proj_name"/>
                </el-form-item>
                <el-form-item label="承包商" prop="contractor_id">
                    <el-select v-model="form.contractor_id" filterable style="width:100%" placeholder="请选择承包商">
                        <el-option v-for="c in contractorOptions" :key="c.value" :value="c.value" :label="c.label"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="开始日期">
                    <el-date-picker v-model="form.begin_date" type="date" value-format="YYYY-MM-DD"
                                    style="width:100%"/>
                </el-form-item>
                <el-form-item label="结束日期">
                    <el-date-picker v-model="form.end_date" type="date" value-format="YYYY-MM-DD"
                                    style="width:100%"/>
                </el-form-item>
                <el-form-item label="项目状态">
                    <el-radio-group v-model="form.proj_status">
                        <el-radio v-for="s in gd.projStatus" :key="s.id" :value="s.id">{{ s.desc }}</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="项目内容">
                    <el-input v-model="form.proj_content" type="textarea" :rows="3"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
            </template>
        </el-dialog>

        <!-- 变更项目状态 -->
        <el-dialog v-model="stateVisible" :title="`变更状态 — ${stateRow.proj_name || ''}`"
                   width="420px" destroy-on-close>
            <el-radio-group v-model="stateValue">
                <el-radio v-for="s in gd.projStatus" :key="s.id" :value="s.id">{{ s.desc }}</el-radio>
            </el-radio-group>
            <template #footer>
                <el-button @click="stateVisible = false">取消</el-button>
                <el-button type="primary" :loading="stateSaving" @click="saveState">确定</el-button>
            </template>
        </el-dialog>

        <!-- 子档：入场 / 开工准备 / 验收 -->
        <el-dialog v-model="subVisible" :title="`${subCfg?.title} — ${subProject.proj_name || ''}`"
                   width="900px" destroy-on-close>
            <el-table :data="subRows" v-loading="subLoading" size="small" border>
                <el-table-column v-for="c in subCfg.cols" :key="c.prop" :prop="c.prop" :label="c.label"
                                 :width="c.width" :min-width="c.min" show-overflow-tooltip>
                    <template #default="{row}">
                        <span v-if="row[c.prop] != null && row[c.prop] !== ''">{{ row[c.prop] }}</span>
                        <span v-else class="dim">—</span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
                    <template #default="{row}">
                        <el-link type="primary" @click="editSub(row)">编辑</el-link>
                        <el-link type="danger" style="margin-left:10px" @click="removeSub(row)">删除</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!subLoading && subRows.length === 0" :description="`暂无${subCfg?.title || ''}`"/>

            <el-divider content-position="left">{{ subForm[subCfg?.idField] ? '编辑记录' : '新增记录' }}</el-divider>
            <el-form ref="subFormRef" :model="subForm" :rules="subCfg?.rules" label-width="110px">
                <el-row :gutter="16">
                    <el-col v-for="f in (subCfg?.fields || [])" :key="f.key" :span="f.kind === 'textarea' ? 24 : 12">
                        <el-form-item :label="f.label" :prop="f.key">
                            <el-date-picker v-if="f.kind === 'date'" v-model="subForm[f.key]" type="date"
                                            value-format="YYYY-MM-DD" style="width:100%"/>
                            <el-input-number v-else-if="f.kind === 'number'" v-model="subForm[f.key]"
                                             :min="0" :precision="0" :controls="false" style="width:100%"/>
                            <el-input v-else-if="f.kind === 'textarea'" v-model="subForm[f.key]"
                                      type="textarea" :rows="2"/>
                            <el-input v-else v-model="subForm[f.key]"/>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <el-button @click="subVisible = false">关闭</el-button>
                <el-button type="primary" :loading="subSaving" @click="saveSub">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.project-page {
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
        margin-bottom: 14px;

        .add-btn {
            margin-left: auto;
        }
    }

    .pager {
        margin-top: 14px;
        justify-content: flex-end;
    }

    .dim {
        color: #cbd5e1;
    }
}
</style>
