<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {ElMessage, ElMessageBox} from "element-plus";
import {Search} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const sys = Singleton.getInstance(SysX)

/* ============ 列表 ============ */
const loading = ref(false)
const rows = ref([])
const keyword = ref('')
const contractorFilter = ref('')
const workTypeFilter = ref('')
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
    if (workTypeFilter.value !== '') paras.workType = workTypeFilter.value
    sys.biz('cbs/spotcheck/list', paras, AC.signal, () => {}, (r, data) => {
        loading.value = false
        if (r) rows.value = data.data || []
        else notifyError(data, '抽查记录加载失败')
    })
}

watch(keyword, () => {
    clearTimeout(kwTimer)
    kwTimer = setTimeout(loadRows, 300)
})
watch([contractorFilter, workTypeFilter], loadRows)

/* 承包商下拉 */
const contractorOptions = ref([])

function loadContractors() {
    sys.biz('cbs/contractor/list', {}, AC.signal, () => {}, (r, data) => {
        if (r) contractorOptions.value = (data.data || []).map(c => ({value: c.contractor_id, label: c.name}))
    })
}

function contractorName(id) {
    return contractorOptions.value.find(c => c.value === id)?.label || id
}

function workTypeDesc(t) {
    return (gd.workTypes.find(x => x.id === Number(t)) || {}).desc || t
}

function resultMeta(s) {
    return gd.checkResult.find(x => x.id === Number(s)) || {desc: s, type: 'info'}
}

function rectifyMeta(s) {
    return gd.rectifyStatus.find(x => x.id === Number(s)) || {desc: s, type: 'info'}
}

/* ============ 新增/编辑 ============ */
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
    work_type: [{required: true, message: '请选择特殊作业类型', trigger: 'change'}],
    spot_content: [{required: true, message: '请输入抽查内容', trigger: 'blur'}],
}

function openDialog(edit, row) {
    form.value = edit
        ? {
            check_id: row.check_id, work_type: row.work_type, check_date: row.check_date || '',
            checker: row.checker || '', contractor_id: row.contractor_id || '',
            spot_content: row.spot_content || '', result: row.result ?? 1,
            problem_desc: row.problem_desc || '', rectify_status: row.rectify_status ?? 0, _edit: true,
        }
        : {
            check_id: '', work_type: '', check_date: '', checker: '', contractor_id: '',
            spot_content: '', result: 1, problem_desc: '', rectify_status: 0, _edit: false,
        }
    dialogVisible.value = true
}

/* 不合格默认进入待整改；合格固定无需整改 */
watch(() => form.value.result, v => {
    if (v === 0 && form.value.rectify_status === 0 && !form.value._edit) form.value.rectify_status = 1
    if (v === 1) form.value.rectify_status = 0
})

function saveForm() {
    formRef.value.validate(valid => {
        if (!valid) return
        saving.value = true
        const f = {...form.value}
        delete f._edit
        if (!f.check_date) f.check_date = null
        if (!f.contractor_id) f.contractor_id = null
        sys.biz('cbs/spotcheck/save', f, AC.signal, () => {}, (r, data) => {
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
    ElMessageBox.confirm('确定删除这条抽查记录吗？', '提示', {type: 'warning'}).then(() => {
        loading.value = true
        sys.biz('cbs/spotcheck/delete', {checkId: row.check_id}, AC.signal, () => {}, (r, data) => {
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
    loadContractors()
})
onUnmounted(() => {
    AC.abort()
    clearTimeout(kwTimer)
})
</script>

<template>
    <div class="spotcheck-page">
        <div class="card">
            <div class="toolbar">
                <el-input v-model="keyword" placeholder="搜索抽查内容/抽查人" clearable
                          style="width:220px" :prefix-icon="Search"/>
                <el-select v-model="workTypeFilter" placeholder="特殊作业类型" clearable style="width:150px">
                    <el-option v-for="t in gd.workTypes" :key="t.id" :value="t.id" :label="t.desc"/>
                </el-select>
                <el-select v-model="contractorFilter" placeholder="被抽查承包商" clearable filterable
                           style="width:190px">
                    <el-option v-for="c in contractorOptions" :key="c.value" :value="c.value" :label="c.label"/>
                </el-select>
                <el-button type="primary" @click="openDialog(false)">新增抽查</el-button>
            </div>

            <el-table :data="rows" v-loading="loading" border stripe>
                <el-table-column label="特殊作业类型" width="120" align="center">
                    <template #default="{row}">
                        <el-tag effect="plain" size="small">{{ workTypeDesc(row.work_type) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="check_date" label="抽查日期" width="115" align="center">
                    <template #default="{row}">{{ row.check_date || '—' }}</template>
                </el-table-column>
                <el-table-column prop="checker" label="抽查人" width="100">
                    <template #default="{row}">{{ row.checker || '—' }}</template>
                </el-table-column>
                <el-table-column label="被抽查承包商" min-width="150">
                    <template #default="{row}">{{ row.contractor_name || contractorName(row.contractor_id) || '厂内自查' }}</template>
                </el-table-column>
                <el-table-column prop="spot_content" label="抽查内容" min-width="200" show-overflow-tooltip/>
                <el-table-column label="结果" width="90" align="center">
                    <template #default="{row}">
                        <el-tag :type="resultMeta(row.result).type" effect="light" size="small">
                            {{ resultMeta(row.result).desc }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="problem_desc" label="问题描述" min-width="160" show-overflow-tooltip>
                    <template #default="{row}">
                        <span v-if="row.problem_desc">{{ row.problem_desc }}</span>
                        <span v-else class="dim">—</span>
                    </template>
                </el-table-column>
                <el-table-column label="整改状态" width="100" align="center">
                    <template #default="{row}">
                        <el-tag :type="rectifyMeta(row.rectify_status).type" effect="light" size="small">
                            {{ rectifyMeta(row.rectify_status).desc }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="130" align="center" fixed="right">
                    <template #default="{row}">
                        <el-link type="primary" @click="openDialog(true, row)">编辑</el-link>
                        <el-link type="danger" style="margin-left:10px" @click="removeRow(row)">删除</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!loading && rows.length === 0" description="暂无抽查记录"/>
        </div>

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="dialogVisible" :title="form._edit ? '编辑抽查记录' : '新增抽查记录'"
                   width="560px" destroy-on-close>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
                <el-form-item label="特殊作业类型" prop="work_type">
                    <el-select v-model="form.work_type" placeholder="请选择" style="width:100%">
                        <el-option v-for="t in gd.workTypes" :key="t.id" :value="t.id" :label="t.desc"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="抽查日期">
                    <el-date-picker v-model="form.check_date" type="date" value-format="YYYY-MM-DD"
                                    placeholder="选择日期" style="width:100%"/>
                </el-form-item>
                <el-form-item label="抽查人">
                    <el-input v-model="form.checker" placeholder="姓名"/>
                </el-form-item>
                <el-form-item label="被抽查承包商">
                    <el-select v-model="form.contractor_id" placeholder="不选=厂内自查" clearable filterable
                               style="width:100%">
                        <el-option v-for="c in contractorOptions" :key="c.value" :value="c.value" :label="c.label"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="抽查内容" prop="spot_content">
                    <el-input v-model="form.spot_content" type="textarea" :rows="3" placeholder="抽查内容描述"/>
                </el-form-item>
                <el-form-item label="抽查结果">
                    <el-radio-group v-model="form.result">
                        <el-radio v-for="r in gd.checkResult" :key="r.id" :value="r.id">{{ r.desc }}</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item v-if="form.result === 0" label="问题描述">
                    <el-input v-model="form.problem_desc" type="textarea" :rows="2" placeholder="不合格问题描述"/>
                </el-form-item>
                <el-form-item v-if="form.result === 0" label="整改状态">
                    <el-radio-group v-model="form.rectify_status">
                        <el-radio v-for="s in gd.rectifyStatus" :key="s.id" :value="s.id" :disabled="s.id === 0">
                            {{ s.desc }}
                        </el-radio>
                    </el-radio-group>
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

    .dim {
        color: #cbd5e1;
    }
}
</style>
