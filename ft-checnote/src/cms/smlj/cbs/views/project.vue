<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {ElMessage, ElMessageBox} from "element-plus";
import {Search} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const sys = Singleton.getInstance(SysX)

const activeTab = ref('proj')

/* ============ 项目列表（Tab1 项目管理） ============ */
const loading = ref(false)
const rows = ref([])
const keyword = ref('')
const contractorFilter = ref('')
let kwTimer = null

let AC = new AbortController()

function loadRows() {
    AC.abort()
    AC = new AbortController()
    loading.value = true
    const paras = {}
    const kw = keyword.value.trim()
    if (kw) paras.kw = kw
    if (contractorFilter.value) paras.contractorId = contractorFilter.value
    sys.biz('cbs/project/list', paras, AC.signal, () => {}, (r, data) => {
        loading.value = false
        if (r) rows.value = data.data || []
        else notifyError(data, '项目列表加载失败')
    })
}

watch(keyword, () => {
    clearTimeout(kwTimer)
    kwTimer = setTimeout(loadRows, 300)
})
watch(contractorFilter, loadRows)

/* 承包商下拉（列表 + 筛选共用） */
const contractorOptions = ref([])

function loadContractors() {
    sys.biz('cbs/contractor/list', {}, AC.signal, () => {}, (r, data) => {
        if (r) contractorOptions.value = (data.data || []).map(c => ({value: c.contractor_id, label: c.name}))
    })
}

function contractorName(id) {
    return contractorOptions.value.find(c => c.value === id)?.label || id
}

function statusMeta(s) {
    return gd.projStatus.find(x => x.id === Number(s)) || {desc: s, type: 'info'}
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

/* ============ 子档通用工厂：入场管理 / 开工准备 / 项目验收 ============
 * 三块结构一致（项目筛选 + 表格 + 弹窗），状态与增删改逻辑在此收敛。
 * 返回含 ref 的普通对象，调用侧用 reactive() 包裹（模板里嵌套 ref 才会自动解包）。
 * 工厂内部方法一律走原始对象 s（s.xxx.value），与 reactive 包装互不干扰。 */
function makeSub(cfg) {
    const s = {
        loading: ref(false),
        rows: ref([]),
        projectFilter: ref(''),
        dialogVisible: ref(false),
        saving: ref(false),
        formRef: ref(),
        form: ref({}),
        rules: cfg.rules,
        _loaded: false,
    }
    s.load = () => {
        AC.abort()
        AC = new AbortController()
        s.loading.value = true
        const paras = {}
        if (s.projectFilter.value) paras.projectId = s.projectFilter.value
        sys.biz(`cbs/project/${cfg.url}/list`, paras, AC.signal, () => {}, (r, data) => {
            s.loading.value = false
            if (r) s.rows.value = data.data || []
            else notifyError(data, `${cfg.title}加载失败`)
        })
    }
    s.open = (edit, row) => {
        s.form.value = cfg.toForm(row, edit, s)
        s.dialogVisible.value = true
    }
    s.save = () => {
        s.formRef.value.validate(valid => {
            if (!valid) return
            s.saving.value = true
            const f = cfg.fromForm(s.form.value)
            sys.biz(`cbs/project/${cfg.url}/save`, f, AC.signal, () => {}, (r, data) => {
                s.saving.value = false
                if (r) {
                    ElMessage.success('保存成功')
                    s.dialogVisible.value = false
                    s.load()
                } else {
                    notifyError(data, '保存失败')
                }
            })
        })
    }
    s.remove = row => {
        ElMessageBox.confirm(`确定删除这条${cfg.title}记录吗？`, '提示', {type: 'warning'}).then(() => {
            s.loading.value = true
            sys.biz(`cbs/project/${cfg.url}/delete`, {[cfg.idField]: row[cfg.idField]}, AC.signal, () => {}, (r, data) => {
                s.loading.value = false
                if (r) {
                    ElMessage.success('删除成功')
                    s.load()
                } else {
                    notifyError(data, '删除失败')
                }
            })
        }).catch(() => {})
    }
    return s
}

const _reqProject = {project_id: [{required: true, message: '请选择关联项目', trigger: 'change'}]}

/* 入场管理（清单第18项范围：不做关联培训/考试/进出场/设备管理） */
const enter = reactive(makeSub({
    title: '入场', url: 'enter', idField: 'enterId',
    rules: _reqProject,
    toForm: (row, edit, s) => edit
        ? {enter_id: row.enter_id, project_id: row.project_id, enter_date: row.enter_date || '',
           person_count: row.person_count ?? '', leader: row.leader || '', leader_phone: row.leader_phone || '',
           remark: row.remark || '', _edit: true}
        : {enter_id: '', project_id: s.projectFilter.value || '', enter_date: '', person_count: '',
           leader: '', leader_phone: '', remark: '', _edit: false},
    fromForm: f => {
        const o = {...f}
        delete o._edit
        if (!o.enter_date) o.enter_date = null
        if (o.person_count === '' || o.person_count == null) o.person_count = null
        return o
    },
}))

/* 开工准备 */
const prep = reactive(makeSub({
    title: '开工准备', url: 'prep', idField: 'prepId',
    rules: {..._reqProject, content: [{required: true, message: '请输入准备事项', trigger: 'blur'}]},
    toForm: (row, edit, s) => edit
        ? {prep_id: row.prep_id, project_id: row.project_id, prep_date: row.prep_date || '',
           content: row.content || '', status: row.status ?? 0, checker: row.checker || '',
           remark: row.remark || '', _edit: true}
        : {prep_id: '', project_id: s.projectFilter.value || '', prep_date: '', content: '',
           status: 0, checker: '', remark: '', _edit: false},
    fromForm: f => {
        const o = {...f}
        delete o._edit
        if (!o.prep_date) o.prep_date = null
        return o
    },
}))

/* 项目验收 */
const accept = reactive(makeSub({
    title: '验收', url: 'accept', idField: 'acceptId',
    rules: _reqProject,
    toForm: (row, edit, s) => edit
        ? {accept_id: row.accept_id, project_id: row.project_id, accept_date: row.accept_date || '',
           acceptor: row.acceptor || '', result: row.result ?? 1, opinion: row.opinion || '',
           remark: row.remark || '', _edit: true}
        : {accept_id: '', project_id: s.projectFilter.value || '', accept_date: '',
           acceptor: '', result: 1, opinion: '', remark: '', _edit: false},
    fromForm: f => {
        const o = {...f}
        delete o._edit
        if (!o.accept_date) o.accept_date = null
        return o
    },
}))

/* 子项目下拉：三块共用（项目列表已随 Tab1 拉好） */
const projOptions = computed(() => rows.value.map(p => ({value: p.project_id, label: p.proj_name})))

/* 首次切到子 Tab 时懒加载 */
watch(activeTab, t => {
    if (t === 'enter' && !enter._loaded) { enter.load(); enter._loaded = true }
    if (t === 'prep' && !prep._loaded) { prep.load(); prep._loaded = true }
    if (t === 'accept' && !accept._loaded) { accept.load(); accept._loaded = true }
})

function prepStatusMeta(s) {
    return gd.prepStatus.find(x => x.id === Number(s)) || {desc: s, type: 'info'}
}

function acceptResultMeta(s) {
    return gd.acceptResult.find(x => x.id === Number(s)) || {desc: s, type: 'info'}
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
        <el-tabs v-model="activeTab">
            <!-- ==================== 项目管理 ==================== -->
            <el-tab-pane label="项目管理" name="proj">
                <div class="card">
                    <div class="toolbar">
                        <el-input v-model="keyword" placeholder="搜索项目名称" clearable
                                  style="width:220px" :prefix-icon="Search"/>
                        <el-select v-model="contractorFilter" placeholder="承包商单位" clearable filterable
                                   style="width:200px">
                            <el-option v-for="c in contractorOptions" :key="c.value" :value="c.value" :label="c.label"/>
                        </el-select>
                        <el-button type="primary" @click="openDialog(false)">新增项目</el-button>
                    </div>

                    <el-table :data="rows" v-loading="loading" border stripe>
                        <el-table-column prop="proj_name" label="项目名称" min-width="180"/>
                        <el-table-column label="承包商单位" min-width="170">
                            <template #default="{row}">{{ row.contractor_name || contractorName(row.contractor_id) }}</template>
                        </el-table-column>
                        <el-table-column label="项目周期" width="220">
                            <template #default="{row}">
                                <span v-if="row.begin_date || row.end_date">
                                    {{ row.begin_date || '—' }} ~ {{ row.end_date || '—' }}
                                </span>
                                <span v-else class="dim">—</span>
                            </template>
                        </el-table-column>
                        <el-table-column label="状态" width="100" align="center">
                            <template #default="{row}">
                                <el-tag :type="statusMeta(row.proj_status).type" effect="light">
                                    {{ statusMeta(row.proj_status).desc }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="proj_content" label="项目内容" min-width="200" show-overflow-tooltip/>
                        <el-table-column label="操作" width="130" align="center" fixed="right">
                            <template #default="{row}">
                                <el-link type="primary" @click="openDialog(true, row)">编辑</el-link>
                                <el-link type="danger" style="margin-left:10px" @click="removeRow(row)">删除</el-link>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-empty v-if="!loading && rows.length === 0" description="暂无项目"/>
                </div>
            </el-tab-pane>

            <!-- ==================== 入场管理 ==================== -->
            <el-tab-pane label="入场管理" name="enter">
                <div class="card">
                    <div class="toolbar">
                        <el-select v-model="enter.projectFilter" placeholder="按项目筛选" clearable filterable
                                   style="width:240px" @change="enter.load()">
                            <el-option v-for="p in projOptions" :key="p.value" :value="p.value" :label="p.label"/>
                        </el-select>
                        <el-button type="primary" @click="enter.open(false)">新增入场</el-button>
                    </div>
                    <el-table :data="enter.rows" v-loading="enter.loading" border stripe>
                        <el-table-column label="项目名称" min-width="170">
                            <template #default="{row}">{{ row.proj_name || '—' }}</template>
                        </el-table-column>
                        <el-table-column prop="enter_date" label="入场日期" width="120" align="center">
                            <template #default="{row}">{{ row.enter_date || '—' }}</template>
                        </el-table-column>
                        <el-table-column prop="person_count" label="入场人数" width="100" align="center">
                            <template #default="{row}">{{ row.person_count ?? '—' }}</template>
                        </el-table-column>
                        <el-table-column prop="leader" label="入场负责人" width="120">
                            <template #default="{row}">{{ row.leader || '—' }}</template>
                        </el-table-column>
                        <el-table-column prop="leader_phone" label="联系电话" width="140">
                            <template #default="{row}">{{ row.leader_phone || '—' }}</template>
                        </el-table-column>
                        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip>
                            <template #default="{row}">
                                <span v-if="row.remark">{{ row.remark }}</span>
                                <span v-else class="dim">—</span>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="130" align="center" fixed="right">
                            <template #default="{row}">
                                <el-link type="primary" @click="enter.open(true, row)">编辑</el-link>
                                <el-link type="danger" style="margin-left:10px" @click="enter.remove(row)">删除</el-link>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-empty v-if="!enter.loading && enter.rows.length === 0" description="暂无入场记录"/>
                </div>
            </el-tab-pane>

            <!-- ==================== 开工准备 ==================== -->
            <el-tab-pane label="开工准备" name="prep">
                <div class="card">
                    <div class="toolbar">
                        <el-select v-model="prep.projectFilter" placeholder="按项目筛选" clearable filterable
                                   style="width:240px" @change="prep.load()">
                            <el-option v-for="p in projOptions" :key="p.value" :value="p.value" :label="p.label"/>
                        </el-select>
                        <el-button type="primary" @click="prep.open(false)">新增准备事项</el-button>
                    </div>
                    <el-table :data="prep.rows" v-loading="prep.loading" border stripe>
                        <el-table-column label="项目名称" min-width="170">
                            <template #default="{row}">{{ row.proj_name || '—' }}</template>
                        </el-table-column>
                        <el-table-column prop="prep_date" label="日期" width="120" align="center">
                            <template #default="{row}">{{ row.prep_date || '—' }}</template>
                        </el-table-column>
                        <el-table-column prop="content" label="准备事项" min-width="220" show-overflow-tooltip/>
                        <el-table-column label="状态" width="100" align="center">
                            <template #default="{row}">
                                <el-tag :type="prepStatusMeta(row.status).type" effect="light" size="small">
                                    {{ prepStatusMeta(row.status).desc }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="checker" label="确认人" width="110">
                            <template #default="{row}">{{ row.checker || '—' }}</template>
                        </el-table-column>
                        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip>
                            <template #default="{row}">
                                <span v-if="row.remark">{{ row.remark }}</span>
                                <span v-else class="dim">—</span>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="130" align="center" fixed="right">
                            <template #default="{row}">
                                <el-link type="primary" @click="prep.open(true, row)">编辑</el-link>
                                <el-link type="danger" style="margin-left:10px" @click="prep.remove(row)">删除</el-link>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-empty v-if="!prep.loading && prep.rows.length === 0" description="暂无开工准备记录"/>
                </div>
            </el-tab-pane>

            <!-- ==================== 项目验收 ==================== -->
            <el-tab-pane label="项目验收" name="accept">
                <div class="card">
                    <div class="toolbar">
                        <el-select v-model="accept.projectFilter" placeholder="按项目筛选" clearable filterable
                                   style="width:240px" @change="accept.load()">
                            <el-option v-for="p in projOptions" :key="p.value" :value="p.value" :label="p.label"/>
                        </el-select>
                        <el-button type="primary" @click="accept.open(false)">新增验收</el-button>
                    </div>
                    <el-table :data="accept.rows" v-loading="accept.loading" border stripe>
                        <el-table-column label="项目名称" min-width="170">
                            <template #default="{row}">{{ row.proj_name || '—' }}</template>
                        </el-table-column>
                        <el-table-column prop="accept_date" label="验收日期" width="120" align="center">
                            <template #default="{row}">{{ row.accept_date || '—' }}</template>
                        </el-table-column>
                        <el-table-column prop="acceptor" label="验收人" width="110">
                            <template #default="{row}">{{ row.acceptor || '—' }}</template>
                        </el-table-column>
                        <el-table-column label="结果" width="100" align="center">
                            <template #default="{row}">
                                <el-tag :type="acceptResultMeta(row.result).type" effect="light" size="small">
                                    {{ acceptResultMeta(row.result).desc }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="opinion" label="验收意见" min-width="200" show-overflow-tooltip>
                            <template #default="{row}">
                                <span v-if="row.opinion">{{ row.opinion }}</span>
                                <span v-else class="dim">—</span>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="130" align="center" fixed="right">
                            <template #default="{row}">
                                <el-link type="primary" @click="accept.open(true, row)">编辑</el-link>
                                <el-link type="danger" style="margin-left:10px" @click="accept.remove(row)">删除</el-link>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-empty v-if="!accept.loading && accept.rows.length === 0" description="暂无验收记录"/>
                </div>
            </el-tab-pane>
        </el-tabs>

        <!-- 新增/编辑项目弹窗 -->
        <el-dialog v-model="dialogVisible" :title="form._edit ? '编辑项目' : '新增项目'"
                   width="560px" destroy-on-close>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
                <el-form-item label="承包商单位" prop="contractor_id">
                    <el-select v-model="form.contractor_id" placeholder="请选择承包商单位" filterable
                               style="width:100%">
                        <el-option v-for="c in contractorOptions" :key="c.value" :value="c.value" :label="c.label"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="项目名称" prop="proj_name">
                    <el-input v-model="form.proj_name" placeholder="请输入项目名称"/>
                </el-form-item>
                <el-form-item label="项目周期">
                    <el-date-picker v-model="form.begin_date" type="date" value-format="YYYY-MM-DD"
                                    placeholder="开始日期" style="width:calc(50% - 8px)"/>
                    <span class="range-sep">~</span>
                    <el-date-picker v-model="form.end_date" type="date" value-format="YYYY-MM-DD"
                                    placeholder="结束日期" style="width:calc(50% - 8px)"/>
                </el-form-item>
                <el-form-item label="项目状态">
                    <el-select v-model="form.proj_status" style="width:100%">
                        <el-option v-for="s in gd.projStatus" :key="s.id" :value="s.id" :label="s.desc"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="项目内容">
                    <el-input v-model="form.proj_content" type="textarea" :rows="3" placeholder="项目内容概述"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
            </template>
        </el-dialog>

        <!-- 入场编辑弹窗 -->
        <el-dialog v-model="enter.dialogVisible" :title="enter.form._edit ? '编辑入场记录' : '新增入场记录'"
                   width="520px" destroy-on-close>
            <el-form :ref="el => (enter.formRef = el)" :model="enter.form" :rules="enter.rules" label-width="100px">
                <el-form-item label="关联项目" prop="project_id">
                    <el-select v-model="enter.form.project_id" placeholder="请选择项目" filterable style="width:100%">
                        <el-option v-for="p in projOptions" :key="p.value" :value="p.value" :label="p.label"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="入场日期">
                    <el-date-picker v-model="enter.form.enter_date" type="date" value-format="YYYY-MM-DD"
                                    placeholder="选择日期" style="width:100%"/>
                </el-form-item>
                <el-form-item label="入场人数">
                    <el-input-number v-model="enter.form.person_count" :min="0" :max="99999" style="width:100%"/>
                </el-form-item>
                <el-form-item label="入场负责人">
                    <el-input v-model="enter.form.leader" placeholder="姓名"/>
                </el-form-item>
                <el-form-item label="联系电话">
                    <el-input v-model="enter.form.leader_phone" placeholder="手机号"/>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="enter.form.remark" type="textarea" :rows="2" placeholder="选填"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="enter.dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="enter.saving" @click="enter.save()">保存</el-button>
            </template>
        </el-dialog>

        <!-- 开工准备编辑弹窗 -->
        <el-dialog v-model="prep.dialogVisible" :title="prep.form._edit ? '编辑开工准备' : '新增开工准备'"
                   width="520px" destroy-on-close>
            <el-form :ref="el => (prep.formRef = el)" :model="prep.form" :rules="prep.rules" label-width="100px">
                <el-form-item label="关联项目" prop="project_id">
                    <el-select v-model="prep.form.project_id" placeholder="请选择项目" filterable style="width:100%">
                        <el-option v-for="p in projOptions" :key="p.value" :value="p.value" :label="p.label"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="日期">
                    <el-date-picker v-model="prep.form.prep_date" type="date" value-format="YYYY-MM-DD"
                                    placeholder="选择日期" style="width:100%"/>
                </el-form-item>
                <el-form-item label="准备事项" prop="content">
                    <el-input v-model="prep.form.content" type="textarea" :rows="3"
                              placeholder="如：作业票办理、安全技术交底、现场隔离等"/>
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="prep.form.status">
                        <el-radio v-for="s in gd.prepStatus" :key="s.id" :value="s.id">{{ s.desc }}</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="确认人">
                    <el-input v-model="prep.form.checker" placeholder="姓名"/>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="prep.form.remark" type="textarea" :rows="2" placeholder="选填"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="prep.dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="prep.saving" @click="prep.save()">保存</el-button>
            </template>
        </el-dialog>

        <!-- 验收编辑弹窗 -->
        <el-dialog v-model="accept.dialogVisible" :title="accept.form._edit ? '编辑验收记录' : '新增验收记录'"
                   width="520px" destroy-on-close>
            <el-form :ref="el => (accept.formRef = el)" :model="accept.form" :rules="accept.rules" label-width="100px">
                <el-form-item label="关联项目" prop="project_id">
                    <el-select v-model="accept.form.project_id" placeholder="请选择项目" filterable style="width:100%">
                        <el-option v-for="p in projOptions" :key="p.value" :value="p.value" :label="p.label"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="验收日期">
                    <el-date-picker v-model="accept.form.accept_date" type="date" value-format="YYYY-MM-DD"
                                    placeholder="选择日期" style="width:100%"/>
                </el-form-item>
                <el-form-item label="验收人">
                    <el-input v-model="accept.form.acceptor" placeholder="姓名"/>
                </el-form-item>
                <el-form-item label="结果">
                    <el-radio-group v-model="accept.form.result">
                        <el-radio v-for="r in gd.acceptResult" :key="r.id" :value="r.id">{{ r.desc }}</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="验收意见">
                    <el-input v-model="accept.form.opinion" type="textarea" :rows="3" placeholder="选填"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="accept.dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="accept.saving" @click="accept.save()">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.project-page {
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
    }

    .range-sep {
        margin: 0 8px;
        color: #94a3b8;
    }

    .dim {
        color: #cbd5e1;
    }
}
</style>
