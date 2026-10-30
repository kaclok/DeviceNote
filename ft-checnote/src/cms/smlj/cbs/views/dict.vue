<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {pagerOf} from "../system/Pager.js"
import {invalidateDict} from "../system/DictX.js"
import {ElMessage, ElMessageBox} from "element-plus";
import {Search} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const sys = Singleton.getInstance(SysX)

/* 一个菜单 = 一个字典类型（承包商类型 / 承包商处罚类型 / 项目类型），由路由 :type 决定 */
const TYPES = gd.dictTypes.map(t => t.id)
const props = defineProps({type: String})
const activeType = ref(TYPES.includes(props?.type) ? props.type : TYPES[0])
const pageTitle = computed(() => (gd.dictTypes.find(t => t.id === activeType.value) || {}).desc || '')

// 每页独立分页配置：dict.<类型>
const pager = computed(() => pagerOf('dict.' + activeType.value))

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
    const paras = {dictType: activeType.value, pageNo: pageNo.value, pageSize: pageSize.value}
    const kw = keyword.value.trim()
    if (kw) paras.kw = kw
    sys.biz('cbs/dict/list', paras, AC.signal, () => {}, (r, data) => {
        loading.value = false
        if (r) {
            rows.value = data.data?.rows || []
            total.value = data.data?.total || 0
        } else {
            notifyError(data, '字典加载失败')
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
watch([pageNo, pageSize], loadRows)

// 切换类型（同一组件实例换路由参数）：重置到第一页并换成该页自己的每页条数
watch(() => props.type, t => {
    if (!TYPES.includes(t) || t === activeType.value) return
    activeType.value = t
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
const rules = {item_name: [{required: true, message: '请输入名称', trigger: 'blur'}]}

function openDialog(edit, row) {
    form.value = edit
        ? {
            item_id: row.item_id, dict_type: row.dict_type, item_name: row.item_name,
            sort_no: row.sort_no ?? 0, is_using: row.is_using !== false,
        }
        : {item_id: '', dict_type: activeType.value, item_name: '', sort_no: 0, is_using: true}
    dialogVisible.value = true
}

function saveForm() {
    formRef.value.validate(valid => {
        if (!valid) return
        saving.value = true
        const f = {...form.value, dict_type: activeType.value}
        sys.biz('cbs/dict/save', f, AC.signal, () => {}, (r, data) => {
            saving.value = false
            if (r) {
                ElMessage.success('保存成功')
                dialogVisible.value = false
                invalidateDict(activeType.value)   // 业务页下拉的缓存一并失效
                loadRows()
            } else {
                notifyError(data, '保存失败')
            }
        })
    })
}

function removeRow(row) {
    ElMessageBox.confirm(`确定删除字典项「${row.item_name}」吗？`, '提示', {type: 'warning'}).then(() => {
        loading.value = true
        sys.biz('cbs/dict/delete', {itemId: row.item_id}, AC.signal, () => {}, (r, data) => {
            loading.value = false
            if (r) {
                ElMessage.success('删除成功')
                invalidateDict(activeType.value)
                loadRows()
            } else {
                notifyError(data, '删除失败')
            }
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
    <div class="dict-page">
        <div class="page-head">
            <h2>{{ pageTitle }}</h2>
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
                <el-table-column prop="item_name" label="名称" min-width="240"/>
                <el-table-column prop="sort_no" label="排序" width="90" align="center"/>
                <el-table-column label="状态" width="100" align="center">
                    <template #default="{row}">
                        <el-tag :type="row.is_using ? 'success' : 'info'" effect="light" size="small">
                            {{ row.is_using ? '启用' : '停用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="140" align="center" fixed="right">
                    <template #default="{row}">
                        <el-link type="primary" @click="openDialog(true, row)">编辑</el-link>
                        <el-link type="danger" style="margin-left:10px" @click="removeRow(row)">删除</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!loading && rows.length === 0" :description="`暂无${pageTitle}`"/>

            <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                           :total="total" v-model:current-page="pageNo" v-model:page-size="pageSize"
                           :page-sizes="pager.pageSizes"/>
        </div>

        <el-dialog v-model="dialogVisible" :title="form.item_id ? `编辑${pageTitle}` : `新增${pageTitle}`"
                   width="460px" destroy-on-close>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
                <el-form-item label="名称" prop="item_name">
                    <el-input v-model="form.item_name" placeholder="字典项名称"/>
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="form.sort_no" :min="0" :precision="0" :controls="false"/>
                    <span class="form-tip" style="margin-left:8px">数值越小越靠前</span>
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="form.is_using" active-text="启用"/>
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
.dict-page {
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

    .form-tip {
        font-size: 11px;
        color: #94a3b8;
    }
}
</style>
