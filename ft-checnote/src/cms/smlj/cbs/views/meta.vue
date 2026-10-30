<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import {pagerOf} from "../system/Pager.js"
import {ElMessage, ElMessageBox} from "element-plus";
import {Search} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const sys = Singleton.getInstance(SysX)

/* 通用基础数据页：开工准备条件 / 资料清单（复刻目标站，简单清单：名称 + 说明） */
const META = {
    prep: {title: '开工准备条件', table: 't_prep_condition'},
    doc: {title: '资料清单', table: 't_doc_list'},
}
const props = defineProps({meta: String})
const cfg = computed(() => META[props?.meta] || META.prep)

const pager = computed(() => pagerOf('meta.' + (props?.meta || 'prep')))

const loading = ref(false)
const rows = ref([])
const keyword = ref('')
const pageNo = ref(1)
const pageSize = ref(pager.value.pageSize)
const total = ref(0)
let kwTimer = null
let AC = new AbortController()

function loadRows() {
    AC.abort()
    AC = new AbortController()
    loading.value = true
    const paras = {table: cfg.value.table, pageNo: pageNo.value, pageSize: pageSize.value}
    const kw = keyword.value.trim()
    if (kw) paras.kw = kw
    sys.biz('cbs/meta/list', paras, AC.signal, () => {}, (r, data) => {
        loading.value = false
        if (r) {
            rows.value = data.data?.rows || []
            total.value = data.data?.total || 0
        } else notifyError(data, '加载失败')
    })
}

watch(keyword, () => {
    clearTimeout(kwTimer)
    kwTimer = setTimeout(() => {pageNo.value = 1; loadRows()}, 300)
})
watch([pageNo, pageSize], loadRows)
watch(() => props.meta, () => {
    keyword.value = ''
    pageNo.value = 1
    pageSize.value = pager.value.pageSize
    loadRows()
})

/* ============ 新增/编辑 ============ */
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {name: [{required: true, message: '请输入名称', trigger: 'blur'}]}

function openDialog(edit, row) {
    form.value = edit
        ? {id: row.id, name: row.name, remark: row.remark || ''}
        : {id: '', name: '', remark: ''}
    dialogVisible.value = true
}

function saveForm() {
    formRef.value.validate(valid => {
        if (!valid) return
        saving.value = true
        const f = {...form.value, table: cfg.value.table}
        sys.biz('cbs/meta/save', f, AC.signal, () => {}, (r, data) => {
            saving.value = false
            if (r) {
                ElMessage.success('保存成功')
                dialogVisible.value = false
                loadRows()
            } else notifyError(data, '保存失败')
        })
    })
}

function removeRow(row) {
    ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '提示', {type: 'warning'}).then(() => {
        loading.value = true
        sys.biz('cbs/meta/delete', {table: cfg.value.table, id: row.id}, AC.signal, () => {}, (r, data) => {
            loading.value = false
            if (r) {
                ElMessage.success('已删除')
                loadRows()
            } else notifyError(data, '删除失败')
        })
    }).catch(() => {})
}

onMounted(loadRows)
onUnmounted(() => {
    AC.abort()
    clearTimeout(kwTimer)
})
</script>

<template>
    <div class="meta-page">
        <div class="page-head">
            <h2>{{ cfg.title }}</h2>
        </div>

        <div class="card">
            <div class="toolbar">
                <el-input v-model="keyword" placeholder="搜索名称" clearable
                          style="width:220px" :prefix-icon="Search"/>
                <el-button type="primary" plain class="add-btn" @click="openDialog(false)">新增</el-button>
            </div>

            <el-table :data="rows" v-loading="loading" border stripe>
                <el-table-column type="index" label="序号" width="70" align="center"
                                 :index="i => (pageNo - 1) * pageSize + i + 1"/>
                <el-table-column prop="name" label="名称" min-width="240"/>
                <el-table-column prop="remark" label="说明" min-width="300" show-overflow-tooltip>
                    <template #default="{row}">{{ row.remark || '—' }}</template>
                </el-table-column>
                <el-table-column label="操作" width="140" align="center" fixed="right">
                    <template #default="{row}">
                        <el-link type="primary" @click="openDialog(true, row)">编辑</el-link>
                        <el-link type="danger" style="margin-left:10px" @click="removeRow(row)">删除</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!loading && rows.length === 0" :description="`暂无${cfg.title}`"/>

            <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                           :total="total" v-model:current-page="pageNo" v-model:page-size="pageSize"
                           :page-sizes="pager.pageSizes"/>
        </div>

        <el-dialog v-model="dialogVisible" :title="form.id ? `编辑${cfg.title}` : `新增${cfg.title}`"
                   width="520px" destroy-on-close>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
                <el-form-item label="名称" prop="name">
                    <el-input v-model="form.name" placeholder="名称"/>
                </el-form-item>
                <el-form-item label="说明">
                    <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="补充说明（可选）"/>
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
.meta-page {
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
}
</style>
