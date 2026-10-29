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

/* ============ 新增/编辑 ============ */
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

/* ============ 删除 ============ */
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

        <!-- 新增/编辑弹窗 -->
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
